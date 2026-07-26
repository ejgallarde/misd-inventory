package ph.gov.phlpost.inventory.misddashboard.service;

import ph.gov.phlpost.inventory.misddashboard.model.FleetVehicle;
import ph.gov.phlpost.inventory.misddashboard.repository.FleetVehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class FleetService {

    private final FleetVehicleRepository fleetRepo;
    private final AuditLogService auditService;

    public FleetService(FleetVehicleRepository fleetRepo, AuditLogService auditService) {
        this.fleetRepo = fleetRepo;
        this.auditService = auditService;
    }

    @Transactional
    public void assignVehicle(Integer vehicleId, String employeeId, String notes) {
        FleetVehicle vehicle = fleetRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));

        vehicle.setAssignedDriverID(employeeId);
        vehicle.setOperationalStatus("Dispatched/In Transit");
        fleetRepo.save(vehicle);
        auditService.logAssignment(vehicle.getPlateNumber(), employeeId, "Vehicle Checkout", notes);
    }

    @Transactional
    public void returnVehicle(Integer vehicleId, String notes) {
        FleetVehicle vehicle = fleetRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));

        String previousDriver = vehicle.getAssignedDriverID();
        vehicle.setAssignedDriverID(null);
        vehicle.setOperationalStatus("Available/Idle");
        fleetRepo.save(vehicle);

        if (previousDriver != null) {
            auditService.logAssignment(vehicle.getPlateNumber(), previousDriver, "Vehicle Returned", notes);
        } else {
            auditService.logLifecycleEvent(vehicle.getPlateNumber(), "MOTORPOOL", "Vehicle Returned", notes);
        }
    }

    @Transactional
    public void retireVehicle(Integer vehicleId, String notes) {
        FleetVehicle vehicle = fleetRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));

        vehicle.setAssignedDriverID(null);
        vehicle.setAdminLegaltionalStatus("Decommissioned");
        vehicle.setOperationalStatus("Slated for Disposal");
        vehicle.setMaintenanceStatus("Beyond Economic Repair (BER)");
        fleetRepo.save(vehicle);
        auditService.logLifecycleEvent(vehicle.getPlateNumber(), "SYSTEM", "Vehicle Retired", notes);
    }

    @Transactional
    public void updateVehicleDetails(
            Integer vehicleId,
            LocalDate registrationExpiry,
            LocalDate insuranceExpiry,
            String adminLegalStatus,
            String operationalStatus,
            String maintenanceStatus,
            String remarks) {
        FleetVehicle vehicle = fleetRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));

        vehicle.setRegistrationExpiry(registrationExpiry);
        vehicle.setInsuranceExpiry(insuranceExpiry);
        vehicle.setAdminLegaltionalStatus(adminLegalStatus);
        vehicle.setOperationalStatus(operationalStatus);
        vehicle.setMaintenanceStatus(maintenanceStatus);
        vehicle.setRemarks(remarks);
        fleetRepo.save(vehicle);
    }
}