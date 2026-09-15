package ph.gov.phlpost.inventory.misddashboard.service;

import ph.gov.phlpost.inventory.misddashboard.model.FleetVehicle;
import ph.gov.phlpost.inventory.misddashboard.repository.FleetVehicleRepository;
import ph.gov.phlpost.inventory.misddashboard.util.TextUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;

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
        vehicle.setOperationalStatus("Dispatched");
        fleetRepo.save(vehicle);
        auditService.logAssignment("FLEETVEHICLE", auditReferenceId(vehicle), employeeId, vehicle.getEndUserID(),
                "Vehicle Checkout", null, notes);
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
            auditService.logAssignment("FLEETVEHICLE", auditReferenceId(vehicle), previousDriver,
                    vehicle.getEndUserID(), "Vehicle Returned", null, notes);
        } else {
            auditService.logLifecycleEvent(auditReferenceId(vehicle), "MOTORPOOL", "Vehicle Returned", notes);
        }
    }

    @Transactional
    public void markVehicleUnderMaintenance(Integer vehicleId, String notes) {
        FleetVehicle vehicle = fleetRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));

        vehicle.setOperationalStatus("Grounded");
        vehicle.setMaintenanceStatus("Under Repair");
        fleetRepo.save(vehicle);
        auditService.logLifecycleEvent(auditReferenceId(vehicle), "SYSTEM", "Marked Under Maintenance", notes);
    }

    @Transactional
    public void markVehicleImpounded(Integer vehicleId, String notes) {
        FleetVehicle vehicle = fleetRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));

        vehicle.setAssignedDriverID(null);
        vehicle.setAdminLegaltionalStatus("Impounded");
        vehicle.setOperationalStatus("Grounded");
        vehicle.setMaintenanceStatus("Not Applicable");
        fleetRepo.save(vehicle);
        auditService.logLifecycleEvent(auditReferenceId(vehicle), "SYSTEM", "Marked Impounded", notes);
    }

    @Transactional
    public void markVehicleBeyondEconomicRepair(Integer vehicleId, String notes) {
        FleetVehicle vehicle = fleetRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));

        vehicle.setOperationalStatus("Slated for Disposal");
        vehicle.setMaintenanceStatus("Beyond Economic Repair (BER)");
        fleetRepo.save(vehicle);
        auditService.logLifecycleEvent(auditReferenceId(vehicle), "SYSTEM", "Marked BER", notes);
    }

    @Transactional
    public void markVehicleStolen(Integer vehicleId, String notes) {
        FleetVehicle vehicle = fleetRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));

        vehicle.setAssignedDriverID(null);
        vehicle.setAdminLegaltionalStatus("Under Investigation");
        vehicle.setOperationalStatus("Stolen");
        vehicle.setMaintenanceStatus("Not Applicable");
        fleetRepo.save(vehicle);
        auditService.logLifecycleEvent(auditReferenceId(vehicle), "SYSTEM", "Marked Stolen", notes);
    }

    @Transactional
    public void markVehicleMissing(Integer vehicleId, String notes) {
        FleetVehicle vehicle = fleetRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));

        vehicle.setAssignedDriverID(null);
        vehicle.setAdminLegaltionalStatus("Under Investigation");
        vehicle.setOperationalStatus("Missing");
        vehicle.setMaintenanceStatus("Not Applicable");
        fleetRepo.save(vehicle);
        auditService.logLifecycleEvent(auditReferenceId(vehicle), "SYSTEM", "Marked Missing", notes);
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
        auditService.logLifecycleEvent(auditReferenceId(vehicle), "SYSTEM", "Vehicle Retired", notes);
    }

    /**
     * Audit reference for a vehicle: always the immutable primary key.
     *
     * <p>
     * History used to be filed under the plate number. That broke two ways.
     * PlateNumber is nullable and registration does not require it, while
     * AssetAssignments.AssetTag and LifecycleAuditLog.ReferenceID are both
     * NOT NULL — so acting on a plate-less vehicle threw a constraint violation
     * that slipped past the controllers' IllegalArgumentException catch. And
     * because updateVehicleDetails lets a blank plate be filled in later, and a
     * vehicle can be re-plated, every entry written under the old value became
     * unreachable from the detail panel.
     *
     * <p>
     * Existing rows are moved onto this key by
     * db/migration_2026-09-01_blank-to-null.sql.
     */
    public static String auditReferenceId(FleetVehicle vehicle) {
        return "VEHICLE-" + vehicle.getVehicleID();
    }

    /**
     * Applies an edit from the detail panel and records it in the audit log.
     *
     * <p>
     * The seventeen-parameter signature this replaces was assembled field by
     * field in the controller from an object it already had. More importantly it
     * wrote nothing to the audit log, so a status change made through the
     * slideout left no trace while the same change through the action modals did
     * &mdash; the history tab under-reported who changed what.
     */
    @Transactional
    public void updateVehicleDetails(FleetVehicle submitted, String performedBy) {
        FleetVehicle vehicle = fleetRepo.findById(submitted.getVehicleID())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));

        String previousStatuses = statusSummary(vehicle);

        // Always-editable fields
        vehicle.setRegistrationExpiry(submitted.getRegistrationExpiry());
        vehicle.setInsuranceExpiry(submitted.getInsuranceExpiry());
        vehicle.setAdminLegaltionalStatus(submitted.getAdminLegaltionalStatus());
        vehicle.setOperationalStatus(submitted.getOperationalStatus());
        vehicle.setMaintenanceStatus(submitted.getMaintenanceStatus());
        vehicle.setRemarks(submitted.getRemarks());
        // Unlike AssignedDriverID (a formal checkout/return lifecycle action), the
        // end user is day-to-day information and freely editable here.
        vehicle.setEndUserID(submitted.getEndUserID());

        // Lock-once fields: only applied when the current database value is blank
        if (TextUtils.isBlank(vehicle.getPropertyNumber()) && !TextUtils.isBlank(submitted.getPropertyNumber())) {
            vehicle.setPropertyNumber(submitted.getPropertyNumber());
        }
        if (TextUtils.isBlank(vehicle.getPlateNumber()) && !TextUtils.isBlank(submitted.getPlateNumber())) {
            vehicle.setPlateNumber(submitted.getPlateNumber());
        }
        // CatalogID is intentionally never touched here: this method mutates the
        // managed entity field-by-field rather than overwriting it wholesale, so
        // simply never setting it keeps a registered vehicle's catalog
        // assignment permanent with no extra guard needed.
        if (vehicle.getAcquisitionYear() == null && submitted.getAcquisitionYear() != null) {
            vehicle.setAcquisitionYear(submitted.getAcquisitionYear());
        }
        if (TextUtils.isBlank(vehicle.getBodyNumber()) && !TextUtils.isBlank(submitted.getBodyNumber())) {
            vehicle.setBodyNumber(submitted.getBodyNumber());
        }
        if (TextUtils.isBlank(vehicle.getEngineNumber()) && !TextUtils.isBlank(submitted.getEngineNumber())) {
            vehicle.setEngineNumber(submitted.getEngineNumber());
        }
        if (TextUtils.isBlank(vehicle.getChassisNumberVIN()) && !TextUtils.isBlank(submitted.getChassisNumberVIN())) {
            vehicle.setChassisNumberVIN(submitted.getChassisNumberVIN());
        }
        if (vehicle.getCost() == null && submitted.getCost() != null) {
            vehicle.setCost(submitted.getCost());
        }

        fleetRepo.save(vehicle);

        String currentStatuses = statusSummary(vehicle);
        String actionType = previousStatuses.equals(currentStatuses)
                ? "Vehicle Details Updated"
                : "Vehicle Status Updated";
        auditService.logLifecycleEvent(auditReferenceId(vehicle), performedBy, actionType,
                previousStatuses + " -> " + currentStatuses);
    }

    private static String statusSummary(FleetVehicle vehicle) {
        return "Legal: " + displayValue(vehicle.getAdminLegaltionalStatus())
                + "; Operational: " + displayValue(vehicle.getOperationalStatus())
                + "; Maintenance: " + displayValue(vehicle.getMaintenanceStatus());
    }

    private static String displayValue(String value) {
        return TextUtils.isBlank(value) ? "None" : value;
    }

    /**
     * Loads a vehicle for display. Strictly read-only.
     *
     * <p>
     * This replaces {@code reviewAndUpdateVehicleStatus}, which ran on every
     * {@code GET /fleet/{id}} and wrote to the database: it overwrote
     * AdminLegaltionalStatus with "Registration Expired" — erasing Impounded,
     * Under Investigation, Decommissioned or Sold — and forced MaintenanceStatus
     * to BER for any vehicle ten or more years past its acquisition year,
     * regardless of its actual condition, with no audit entry. Simply opening the
     * detail panel silently rewrote the record.
     *
     * <p>
     * The same two conditions are now reported as derived flags for the panel to
     * display, so the information survives without the record being changed
     * behind the user's back. Both remain available as explicit lifecycle
     * actions the user takes deliberately.
     */
    @Transactional(readOnly = true)
    public FleetVehicle findVehicle(Integer vehicleId) {
        return fleetRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));
    }

    /**
     * Number of years an asset is depreciated over, measured from acquisition.
     * Also the age at which a vehicle is surfaced on the dashboard as needing
     * attention, so the detail panel and the "requires attention" list agree.
     * COA Circular 2003-007, Annex A: Motor Vehicles = 7 years. The native
     * queries in FleetVehicleRepository can't reference this constant and must
     * be kept in sync with it by hand.
     */
    public static final int USEFUL_LIFE_YEARS = DepreciationService.FLEET_USEFUL_LIFE_YEARS;

    /** Display-only conclusions drawn from a vehicle's stored values. */
    public record VehicleStatusFlags(boolean fullyDepreciated, boolean registrationExpired) {
    }

    public static VehicleStatusFlags deriveStatusFlags(FleetVehicle vehicle) {
        boolean fullyDepreciated = false;
        if (vehicle.getCost() != null
                && vehicle.getCost().signum() > 0
                && vehicle.getAcquisitionYear() != null
                && vehicle.getAcquisitionYear() > 0) {
            fullyDepreciated = Year.now().getValue() - vehicle.getAcquisitionYear() >= USEFUL_LIFE_YEARS;
        }

        boolean registrationExpired = vehicle.getRegistrationExpiry() != null
                && vehicle.getRegistrationExpiry().isBefore(LocalDate.now());

        return new VehicleStatusFlags(fullyDepreciated, registrationExpired);
    }

}