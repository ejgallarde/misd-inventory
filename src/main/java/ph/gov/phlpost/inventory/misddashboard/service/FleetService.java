package ph.gov.phlpost.inventory.misddashboard.service;

import ph.gov.phlpost.inventory.misddashboard.model.FleetVehicle;
import ph.gov.phlpost.inventory.misddashboard.repository.FleetVehicleRepository;
import ph.gov.phlpost.inventory.misddashboard.util.TextUtils;
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
        vehicle.setOperationalStatus("Dispatched");
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
    public void markVehicleUnderMaintenance(Integer vehicleId, String notes) {
        FleetVehicle vehicle = fleetRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));

        vehicle.setOperationalStatus("Grounded");
        vehicle.setMaintenanceStatus("Under Repair");
        fleetRepo.save(vehicle);
        auditService.logLifecycleEvent(vehicle.getPlateNumber(), "SYSTEM", "Marked Under Maintenance", notes);
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
        auditService.logLifecycleEvent(vehicle.getPlateNumber(), "SYSTEM", "Marked Impounded", notes);
    }

    @Transactional
    public void markVehicleBeyondEconomicRepair(Integer vehicleId, String notes) {
        FleetVehicle vehicle = fleetRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));

        vehicle.setOperationalStatus("Slated for Disposal");
        vehicle.setMaintenanceStatus("Beyond Economic Repair (BER)");
        fleetRepo.save(vehicle);
        auditService.logLifecycleEvent(vehicle.getPlateNumber(), "SYSTEM", "Marked BER", notes);
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
        auditService.logLifecycleEvent(vehicle.getPlateNumber(), "SYSTEM", "Marked Stolen", notes);
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
        auditService.logLifecycleEvent(vehicle.getPlateNumber(), "SYSTEM", "Marked Missing", notes);
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
            String remarks,
            String plateNumber,
            String make,
            String model,
            Integer manufactureYear,
            Integer acquisitionYear,
            String bodyNumber,
            String fuelType,
            String engineNumber,
            String chassisNumberVIN,
            String cost) {
        FleetVehicle vehicle = fleetRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));

        // Always-editable fields
        vehicle.setRegistrationExpiry(registrationExpiry);
        vehicle.setInsuranceExpiry(insuranceExpiry);
        vehicle.setAdminLegaltionalStatus(adminLegalStatus);
        vehicle.setOperationalStatus(operationalStatus);
        vehicle.setMaintenanceStatus(maintenanceStatus);
        vehicle.setRemarks(remarks);

        // Lock-once fields: only applied when the current database value is blank
        if (TextUtils.isBlank(vehicle.getPlateNumber()) && !TextUtils.isBlank(plateNumber)) {
            vehicle.setPlateNumber(plateNumber);
        }
        if (TextUtils.isBlank(vehicle.getMake()) && !TextUtils.isBlank(make)) {
            vehicle.setMake(make);
        }
        if (TextUtils.isBlank(vehicle.getModel()) && !TextUtils.isBlank(model)) {
            vehicle.setModel(model);
        }
        if (vehicle.getManufactureYear() == null && manufactureYear != null) {
            vehicle.setManufactureYear(manufactureYear);
        }
        if (vehicle.getAcquisitionYear() == null && acquisitionYear != null) {
            vehicle.setAcquisitionYear(acquisitionYear);
        }
        if (TextUtils.isBlank(vehicle.getBodyNumber()) && !TextUtils.isBlank(bodyNumber)) {
            vehicle.setBodyNumber(bodyNumber);
        }
        if (TextUtils.isBlank(vehicle.getFuelType()) && !TextUtils.isBlank(fuelType)) {
            vehicle.setFuelType(fuelType);
        }
        if (TextUtils.isBlank(vehicle.getEngineNumber()) && !TextUtils.isBlank(engineNumber)) {
            vehicle.setEngineNumber(engineNumber);
        }
        if (TextUtils.isBlank(vehicle.getChassisNumberVIN()) && !TextUtils.isBlank(chassisNumberVIN)) {
            vehicle.setChassisNumberVIN(chassisNumberVIN);
        }
        if (TextUtils.isBlank(vehicle.getCost()) && !TextUtils.isBlank(cost)) {
            vehicle.setCost(cost);
        }

        fleetRepo.save(vehicle);
    }

    @Transactional
    public FleetVehicle reviewAndUpdateVehicleStatus(Integer vehicleId) {
        FleetVehicle vehicle = fleetRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));

        boolean updated = false;

        // Check if fully depreciated; if so, update maintenance status
        if (vehicle.getCost() != null && vehicle.getAcquisitionYear() != null) {
            double cost = TextUtils.parseLenientDouble(vehicle.getCost());
            int acqYear = vehicle.getAcquisitionYear();
            if (cost > 0 && acqYear > 0) {
                int yearsUsed = java.time.Year.now().getValue() - acqYear;
                if (yearsUsed >= 10) {
                    if (!TextUtils.isBlank(vehicle.getMaintenanceStatus()) &&
                            !vehicle.getMaintenanceStatus().equals("Beyond Economic Repair (BER)")) {
                        vehicle.setMaintenanceStatus("Beyond Economic Repair (BER)");
                        updated = true;
                    }
                }
            }
        }

        // Check if registration is expired; if so, update admin/legal status
        if (vehicle.getRegistrationExpiry() != null) {
            if (vehicle.getRegistrationExpiry().isBefore(LocalDate.now())) {
                if (!TextUtils.isBlank(vehicle.getAdminLegaltionalStatus()) &&
                        !vehicle.getAdminLegaltionalStatus().equals("Registration Expired")) {
                    vehicle.setAdminLegaltionalStatus("Registration Expired");
                    updated = true;
                }
            }
        }

        if (updated) {
            fleetRepo.save(vehicle);
        }

        return vehicle;
    }

}