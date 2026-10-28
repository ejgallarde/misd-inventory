package ph.gov.phlpost.inventory.misddashboard.service;

import ph.gov.phlpost.inventory.misddashboard.model.FleetVehicle;
import ph.gov.phlpost.inventory.misddashboard.repository.FleetVehicleRepository;
import ph.gov.phlpost.inventory.misddashboard.util.Amounts;
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
    private final FleetWorkflowHelper workflow;

    public FleetService(FleetVehicleRepository fleetRepo, AuditLogService auditService,
            FleetWorkflowHelper workflow) {
        this.fleetRepo = fleetRepo;
        this.auditService = auditService;
        this.workflow = workflow;
    }

    /**
     * Every lifecycle action re-checks, server side, the same predicate the
     * Actions menu uses to show it. The menu was the only gate before, so a
     * stale page or a hand-made POST could dispatch a stolen or decommissioned
     * vehicle to a driver.
     */
    private static void requireAllowed(boolean allowed, FleetVehicle vehicle, String action) {
        if (!allowed) {
            throw new IllegalArgumentException("Cannot " + action + ": vehicle "
                    + (TextUtils.isBlank(vehicle.getPlateNumber()) ? "#" + vehicle.getVehicleID()
                            : vehicle.getPlateNumber())
                    + " is " + statusSummary(vehicle) + ".");
        }
    }

    @Transactional
    public void assignVehicle(Integer vehicleId, String employeeId, String notes, String performedBy) {
        FleetVehicle vehicle = fleetRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));
        requireAllowed(workflow.canAssignDriver(vehicle.getOperationalStatus())
                && workflow.hasActions(vehicle.getAdminLegaltionalStatus()), vehicle, "assign a driver");

        vehicle.setAssignedDriverID(employeeId);
        vehicle.setOperationalStatus("Dispatched");
        fleetRepo.save(vehicle);
        // The assignment row names the driver; only the lifecycle row can record
        // who dispatched the vehicle.
        auditService.logAssignment(auditReferenceId(vehicle), employeeId, "Vehicle Checkout", notes);
        auditService.logLifecycleEvent(auditReferenceId(vehicle), performedBy, "Vehicle Checkout",
                "Driver: " + employeeId + appendNotes(notes));
    }

    private static String appendNotes(String notes) {
        return TextUtils.isBlank(notes) ? "" : "; Notes: " + notes.trim();
    }

    @Transactional
    public void returnVehicle(Integer vehicleId, String notes, String performedBy) {
        FleetVehicle vehicle = fleetRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));
        requireAllowed(workflow.canReturnToMotorpool(vehicle.getOperationalStatus()),
                vehicle, "return it to the motorpool");

        String previousDriver = vehicle.getAssignedDriverID();
        vehicle.setAssignedDriverID(null);
        vehicle.setOperationalStatus("Available/Idle");
        fleetRepo.save(vehicle);

        if (previousDriver != null) {
            auditService.logAssignment(auditReferenceId(vehicle), previousDriver, "Vehicle Returned", notes);
        }
        auditService.logLifecycleEvent(auditReferenceId(vehicle), performedBy, "Vehicle Returned",
                (previousDriver == null ? "No driver on record" : "Driver: " + previousDriver) + appendNotes(notes));
    }

    @Transactional
    public void markVehicleUnderMaintenance(Integer vehicleId, String notes, String performedBy) {
        FleetVehicle vehicle = fleetRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));
        requireAllowed(workflow.hasActions(vehicle.getAdminLegaltionalStatus())
                && workflow.canMarkUnderMaintenance(vehicle.getMaintenanceStatus()),
                vehicle, "mark it under maintenance");

        vehicle.setOperationalStatus("Grounded");
        vehicle.setMaintenanceStatus("Under Repair");
        fleetRepo.save(vehicle);
        auditService.logLifecycleEvent(auditReferenceId(vehicle), performedBy, "Marked Under Maintenance", notes);
    }

    @Transactional
    public void markVehicleImpounded(Integer vehicleId, String notes, String performedBy) {
        FleetVehicle vehicle = fleetRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));
        requireAllowed(workflow.canMarkImpounded(vehicle.getAdminLegaltionalStatus()),
                vehicle, "mark it impounded");

        vehicle.setAssignedDriverID(null);
        vehicle.setAdminLegaltionalStatus("Impounded");
        vehicle.setOperationalStatus("Grounded");
        vehicle.setMaintenanceStatus("Not Applicable");
        fleetRepo.save(vehicle);
        auditService.logLifecycleEvent(auditReferenceId(vehicle), performedBy, "Marked Impounded", notes);
    }

    @Transactional
    public void markVehicleBeyondEconomicRepair(Integer vehicleId, String notes, String performedBy) {
        FleetVehicle vehicle = fleetRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));
        requireAllowed(workflow.hasActions(vehicle.getAdminLegaltionalStatus())
                && workflow.canMarkBeyondEconomicRepair(vehicle.getMaintenanceStatus()),
                vehicle, "mark it beyond economic repair");

        vehicle.setOperationalStatus("Slated for Disposal");
        vehicle.setMaintenanceStatus("Beyond Economic Repair (BER)");
        fleetRepo.save(vehicle);
        auditService.logLifecycleEvent(auditReferenceId(vehicle), performedBy, "Marked BER", notes);
    }

    @Transactional
    public void markVehicleStolen(Integer vehicleId, String notes, String performedBy) {
        FleetVehicle vehicle = fleetRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));
        requireAllowed(workflow.canMarkStolen(vehicle.getAdminLegaltionalStatus()),
                vehicle, "mark it stolen");

        vehicle.setAssignedDriverID(null);
        vehicle.setAdminLegaltionalStatus("Under Investigation");
        vehicle.setOperationalStatus("Stolen");
        vehicle.setMaintenanceStatus("Not Applicable");
        fleetRepo.save(vehicle);
        auditService.logLifecycleEvent(auditReferenceId(vehicle), performedBy, "Marked Stolen", notes);
    }

    @Transactional
    public void markVehicleMissing(Integer vehicleId, String notes, String performedBy) {
        FleetVehicle vehicle = fleetRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));
        requireAllowed(workflow.canMarkMissing(vehicle.getAdminLegaltionalStatus()),
                vehicle, "mark it missing");

        vehicle.setAssignedDriverID(null);
        vehicle.setAdminLegaltionalStatus("Under Investigation");
        vehicle.setOperationalStatus("Missing");
        vehicle.setMaintenanceStatus("Not Applicable");
        fleetRepo.save(vehicle);
        auditService.logLifecycleEvent(auditReferenceId(vehicle), performedBy, "Marked Missing", notes);
    }

    @Transactional
    public void retireVehicle(Integer vehicleId, String notes, String performedBy) {
        FleetVehicle vehicle = fleetRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));
        requireAllowed(workflow.hasActions(vehicle.getAdminLegaltionalStatus()),
                vehicle, "retire it");

        vehicle.setAssignedDriverID(null);
        vehicle.setAdminLegaltionalStatus("Decommissioned");
        vehicle.setOperationalStatus("Slated for Disposal");
        vehicle.setMaintenanceStatus("Beyond Economic Repair (BER)");
        fleetRepo.save(vehicle);
        auditService.logLifecycleEvent(auditReferenceId(vehicle), performedBy, "Vehicle Retired", notes);
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
    public void updateVehicleDetails(FleetVehicle submitted, String performedBy, boolean operational,
            boolean financial) {
        FleetVehicle vehicle = fleetRepo.findById(submitted.getVehicleID())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));

        String previousStatuses = statusSummary(vehicle);
        java.math.BigDecimal previousCost = vehicle.getCost();

        // Role split (see config/AppRole): cost belongs to FINANCE and, unlike the
        // identifiers below, may be corrected after it is recorded.
        if (operational) {
            // Always-editable fields
            vehicle.setRegistrationExpiry(submitted.getRegistrationExpiry());
            vehicle.setInsuranceExpiry(submitted.getInsuranceExpiry());
            vehicle.setAdminLegaltionalStatus(submitted.getAdminLegaltionalStatus());
            vehicle.setOperationalStatus(submitted.getOperationalStatus());
            vehicle.setMaintenanceStatus(submitted.getMaintenanceStatus());
            vehicle.setRemarks(submitted.getRemarks());

            // Lock-once fields: only applied when the current database value is blank
            if (TextUtils.isBlank(vehicle.getPlateNumber()) && !TextUtils.isBlank(submitted.getPlateNumber())) {
                vehicle.setPlateNumber(submitted.getPlateNumber());
            }
            if (TextUtils.isBlank(vehicle.getMake()) && !TextUtils.isBlank(submitted.getMake())) {
                vehicle.setMake(submitted.getMake());
            }
            if (TextUtils.isBlank(vehicle.getModel()) && !TextUtils.isBlank(submitted.getModel())) {
                vehicle.setModel(submitted.getModel());
            }
            if (vehicle.getManufactureYear() == null && submitted.getManufactureYear() != null) {
                vehicle.setManufactureYear(submitted.getManufactureYear());
            }
            if (vehicle.getAcquisitionYear() == null && submitted.getAcquisitionYear() != null) {
                vehicle.setAcquisitionYear(submitted.getAcquisitionYear());
            }
            if (TextUtils.isBlank(vehicle.getBodyNumber()) && !TextUtils.isBlank(submitted.getBodyNumber())) {
                vehicle.setBodyNumber(submitted.getBodyNumber());
            }
            if (TextUtils.isBlank(vehicle.getFuelType()) && !TextUtils.isBlank(submitted.getFuelType())) {
                vehicle.setFuelType(submitted.getFuelType());
            }
            if (TextUtils.isBlank(vehicle.getEngineNumber()) && !TextUtils.isBlank(submitted.getEngineNumber())) {
                vehicle.setEngineNumber(submitted.getEngineNumber());
            }
            if (TextUtils.isBlank(vehicle.getChassisNumberVIN()) && !TextUtils.isBlank(submitted.getChassisNumberVIN())) {
                vehicle.setChassisNumberVIN(submitted.getChassisNumberVIN());
            }
        }
        if (financial) {
            vehicle.setCost(submitted.getCost());
        }

        fleetRepo.save(vehicle);
        String costNote = Amounts.changeNote("Cost", previousCost, vehicle.getCost());

        String currentStatuses = statusSummary(vehicle);
        String actionType = !previousStatuses.equals(currentStatuses) ? "Vehicle Status Updated"
                : !costNote.isEmpty() && !operational ? "Vehicle Cost Updated"
                        : "Vehicle Details Updated";
        auditService.logLifecycleEvent(auditReferenceId(vehicle), performedBy, actionType,
                previousStatuses + " -> " + currentStatuses + costNote);
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
     */
    public static final int USEFUL_LIFE_YEARS = 10;

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