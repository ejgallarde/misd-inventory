package ph.gov.phlpost.inventory.misddashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ph.gov.phlpost.inventory.misddashboard.model.FleetVehicle;
import ph.gov.phlpost.inventory.misddashboard.repository.FleetVehicleRepository;

@ExtendWith(MockitoExtension.class)
class FleetServiceTest {

    @Mock
    private FleetVehicleRepository fleetRepo;

    @Mock
    private AuditLogService auditService;

    private FleetService service;

    @BeforeEach
    void setUp() {
        service = new FleetService(fleetRepo, auditService, new FleetWorkflowHelper());
    }

    @Test
    void assignVehicleDispatchesItToTheDriver() {
        FleetVehicle vehicle = vehicle(1);
        when(fleetRepo.findById(1)).thenReturn(Optional.of(vehicle));

        service.assignVehicle(1, "E-2", "Trip ticket 55", "clerk");

        assertThat(vehicle.getAssignedDriverID()).isEqualTo("E-2");
        assertThat(vehicle.getOperationalStatus()).isEqualTo("Dispatched");
        verify(auditService).logAssignment("VEHICLE-1", "E-2", "Vehicle Checkout", "Trip ticket 55");
        // Who dispatched it lives only in the lifecycle log.
        verify(auditService).logLifecycleEvent("VEHICLE-1", "clerk", "Vehicle Checkout",
                "Driver: E-2; Notes: Trip ticket 55");
    }

    @Test
    void returnVehicleCreditsThePreviousDriver() {
        FleetVehicle vehicle = vehicle(2);
        vehicle.setAssignedDriverID("E-2");
        vehicle.setOperationalStatus("Dispatched");
        when(fleetRepo.findById(2)).thenReturn(Optional.of(vehicle));

        service.returnVehicle(2, "Full tank", "clerk");

        assertThat(vehicle.getAssignedDriverID()).isNull();
        assertThat(vehicle.getOperationalStatus()).isEqualTo("Available/Idle");
        verify(auditService).logAssignment("VEHICLE-2", "E-2", "Vehicle Returned", "Full tank");
        verify(auditService).logLifecycleEvent("VEHICLE-2", "clerk", "Vehicle Returned",
                "Driver: E-2; Notes: Full tank");
    }

    @Test
    void returningAnUnassignedVehicleIsAttributedToTheUser() {
        // Dispatched with no recorded driver: rows from before drivers were required.
        FleetVehicle vehicle = vehicle(3);
        vehicle.setOperationalStatus("Dispatched");
        when(fleetRepo.findById(3)).thenReturn(Optional.of(vehicle));

        service.returnVehicle(3, null, "clerk");

        // AssetAssignments.EmployeeID is a NOT NULL foreign key, so a driverless
        // return must not be written there.
        verify(auditService, never()).logAssignment(anyString(), anyString(), anyString(), anyString());
        verify(auditService).logLifecycleEvent("VEHICLE-3", "clerk", "Vehicle Returned", "No driver on record");
    }

    @Test
    void impoundStolenAndMissingAllReleaseTheDriver() {
        FleetVehicle impounded = dispatched(4);
        FleetVehicle stolen = dispatched(5);
        FleetVehicle missing = dispatched(6);
        when(fleetRepo.findById(4)).thenReturn(Optional.of(impounded));
        when(fleetRepo.findById(5)).thenReturn(Optional.of(stolen));
        when(fleetRepo.findById(6)).thenReturn(Optional.of(missing));

        service.markVehicleImpounded(4, null, "clerk");
        service.markVehicleStolen(5, null, "clerk");
        service.markVehicleMissing(6, null, "clerk");

        assertThat(impounded.getAssignedDriverID()).isNull();
        assertThat(impounded.getAdminLegaltionalStatus()).isEqualTo("Impounded");
        assertThat(stolen.getAssignedDriverID()).isNull();
        assertThat(stolen.getOperationalStatus()).isEqualTo("Stolen");
        assertThat(missing.getAssignedDriverID()).isNull();
        assertThat(missing.getOperationalStatus()).isEqualTo("Missing");
    }

    @Test
    void underMaintenanceGroundsTheVehicle() {
        FleetVehicle vehicle = vehicle(7);
        when(fleetRepo.findById(7)).thenReturn(Optional.of(vehicle));

        service.markVehicleUnderMaintenance(7, "Brake pads", "clerk");

        assertThat(vehicle.getOperationalStatus()).isEqualTo("Grounded");
        assertThat(vehicle.getMaintenanceStatus()).isEqualTo("Under Repair");
        verify(auditService).logLifecycleEvent("VEHICLE-7", "clerk", "Marked Under Maintenance", "Brake pads");
    }

    @Test
    void retireDecommissionsAndReleasesTheDriver() {
        FleetVehicle vehicle = dispatched(8);
        when(fleetRepo.findById(8)).thenReturn(Optional.of(vehicle));

        service.retireVehicle(8, "Board resolution 12", "clerk");

        assertThat(vehicle.getAssignedDriverID()).isNull();
        assertThat(vehicle.getAdminLegaltionalStatus()).isEqualTo("Decommissioned");
        assertThat(vehicle.getOperationalStatus()).isEqualTo("Slated for Disposal");
    }

    @Test
    void stolenVehicleCannotBeDispatchedEvenByADirectRequest() {
        FleetVehicle vehicle = vehicle(13);
        vehicle.setAdminLegaltionalStatus("Under Investigation");
        vehicle.setOperationalStatus("Stolen");
        when(fleetRepo.findById(13)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> service.assignVehicle(13, "E-2", null, "clerk"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Cannot assign a driver: vehicle PLT-13 is ");
        assertThat(vehicle.getAssignedDriverID()).isNull();
        verify(fleetRepo, never()).save(vehicle);
    }

    @Test
    void decommissionedVehicleIsClosedToEveryAction() {
        FleetVehicle vehicle = vehicle(14);
        vehicle.setAdminLegaltionalStatus("Decommissioned");
        when(fleetRepo.findById(14)).thenReturn(Optional.of(vehicle));

        // Idle but decommissioned: the operational status alone would allow it.
        assertThatThrownBy(() -> service.assignVehicle(14, "E-2", null, "clerk")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.retireVehicle(14, null, "clerk")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.markVehicleStolen(14, null, "clerk")).isInstanceOf(IllegalArgumentException.class);
        verify(fleetRepo, never()).save(vehicle);
    }

    @Test
    void idleVehicleCannotBeReturnedAndRepairCannotBeDoubleBooked() {
        FleetVehicle vehicle = vehicle(15);
        vehicle.setMaintenanceStatus("Under Repair");
        when(fleetRepo.findById(15)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> service.returnVehicle(15, null, "clerk"))
                .hasMessageContaining("return it to the motorpool");
        assertThatThrownBy(() -> service.markVehicleUnderMaintenance(15, null, "clerk"))
                .hasMessageContaining("mark it under maintenance");
    }

    @Test
    void lifecycleActionsOnUnknownVehicleRaiseNotFound() {
        when(fleetRepo.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markVehicleBeyondEconomicRepair(404, null, "clerk"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Vehicle not found.");
    }

    @Test
    void updateDetailsFillsBlankIdentifiersButNeverOverwritesRecordedOnes() {
        FleetVehicle stored = vehicle(9);
        stored.setPlateNumber(null);
        stored.setEngineNumber("ENG-ORIGINAL");
        stored.setCost(null);
        when(fleetRepo.findById(9)).thenReturn(Optional.of(stored));

        FleetVehicle submitted = vehicle(9);
        submitted.setPlateNumber("NEW-123");
        submitted.setEngineNumber("ENG-TAMPERED");
        submitted.setCost(new BigDecimal("1500000.00"));
        submitted.setRemarks("Updated");

        service.updateVehicleDetails(submitted, "clerk@phlpost.gov.ph", true, false);

        assertThat(stored.getPlateNumber()).isEqualTo("NEW-123");
        assertThat(stored.getEngineNumber()).isEqualTo("ENG-ORIGINAL");
        assertThat(stored.getCost()).as("cost is FINANCE's, even when blank").isNull();
        assertThat(stored.getRemarks()).isEqualTo("Updated");
        verify(auditService).logLifecycleEvent(eq("VEHICLE-9"), eq("clerk@phlpost.gov.ph"),
                eq("Vehicle Details Updated"), anyString());
    }

    @Test
    void financeCorrectsARecordedCostAndChangesNothingElse() {
        FleetVehicle stored = vehicle(16);
        stored.setCost(new BigDecimal("1500000.00"));
        when(fleetRepo.findById(16)).thenReturn(Optional.of(stored));

        FleetVehicle submitted = vehicle(16);
        submitted.setCost(new BigDecimal("1450000.00"));
        submitted.setMaintenanceStatus("Under Repair");
        submitted.setRemarks("Should not apply");

        service.updateVehicleDetails(submitted, "finance", false, true);

        assertThat(stored.getCost()).isEqualByComparingTo("1450000.00");
        assertThat(stored.getMaintenanceStatus()).isEqualTo("Roadworthy");
        assertThat(stored.getRemarks()).isNull();
        verify(auditService).logLifecycleEvent(eq("VEHICLE-16"), eq("finance"), eq("Vehicle Cost Updated"),
                org.mockito.ArgumentMatchers.endsWith("; Cost: 1500000.00 -> 1450000.00"));
    }

    @Test
    void updateDetailsReportsAStatusChange() {
        FleetVehicle stored = vehicle(10);
        when(fleetRepo.findById(10)).thenReturn(Optional.of(stored));
        FleetVehicle submitted = vehicle(10);
        submitted.setMaintenanceStatus("Under Repair");

        service.updateVehicleDetails(submitted, "clerk", true, false);

        verify(auditService).logLifecycleEvent(eq("VEHICLE-10"), eq("clerk"), eq("Vehicle Status Updated"),
                org.mockito.ArgumentMatchers.contains("Maintenance: Roadworthy"));
    }

    @Test
    void statusFlagsDeriveFromCostAgeAndRegistrationWithoutChangingTheRecord() {
        FleetVehicle old = vehicle(11);
        old.setCost(new BigDecimal("900000"));
        old.setAcquisitionYear(Year.now().getValue() - FleetService.USEFUL_LIFE_YEARS);
        old.setRegistrationExpiry(LocalDate.now().minusDays(1));

        FleetService.VehicleStatusFlags flags = FleetService.deriveStatusFlags(old);

        assertThat(flags.fullyDepreciated()).isTrue();
        assertThat(flags.registrationExpired()).isTrue();
        assertThat(old.getAdminLegaltionalStatus()).isEqualTo("Registered");
    }

    @Test
    void vehicleWithoutCostIsNeverReportedFullyDepreciated() {
        FleetVehicle vehicle = vehicle(12);
        vehicle.setAcquisitionYear(1990);
        vehicle.setRegistrationExpiry(LocalDate.now().plusYears(1));

        FleetService.VehicleStatusFlags flags = FleetService.deriveStatusFlags(vehicle);

        assertThat(flags.fullyDepreciated()).isFalse();
        assertThat(flags.registrationExpired()).isFalse();
    }

    private static FleetVehicle dispatched(int id) {
        FleetVehicle vehicle = vehicle(id);
        vehicle.setAssignedDriverID("E-2");
        vehicle.setOperationalStatus("Dispatched");
        return vehicle;
    }

    private static FleetVehicle vehicle(int id) {
        FleetVehicle vehicle = new FleetVehicle();
        vehicle.setVehicleID(id);
        vehicle.setPlateNumber("PLT-" + id);
        vehicle.setAdminLegaltionalStatus("Registered");
        vehicle.setOperationalStatus("Available/Idle");
        vehicle.setMaintenanceStatus("Roadworthy");
        return vehicle;
    }
}
