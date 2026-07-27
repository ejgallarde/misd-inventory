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
        if (isBlank(vehicle.getPlateNumber()) && !isBlank(plateNumber)) {
            vehicle.setPlateNumber(plateNumber);
        }
        if (isBlank(vehicle.getMake()) && !isBlank(make)) {
            vehicle.setMake(make);
        }
        if (isBlank(vehicle.getModel()) && !isBlank(model)) {
            vehicle.setModel(model);
        }
        if (vehicle.getManufactureYear() == null && manufactureYear != null) {
            vehicle.setManufactureYear(manufactureYear);
        }
        if (vehicle.getAcquisitionYear() == null && acquisitionYear != null) {
            vehicle.setAcquisitionYear(acquisitionYear);
        }
        if (isBlank(vehicle.getBodyNumber()) && !isBlank(bodyNumber)) {
            vehicle.setBodyNumber(bodyNumber);
        }
        if (isBlank(vehicle.getFuelType()) && !isBlank(fuelType)) {
            vehicle.setFuelType(fuelType);
        }
        if (isBlank(vehicle.getEngineNumber()) && !isBlank(engineNumber)) {
            vehicle.setEngineNumber(engineNumber);
        }
        if (isBlank(vehicle.getChassisNumberVIN()) && !isBlank(chassisNumberVIN)) {
            vehicle.setChassisNumberVIN(chassisNumberVIN);
        }
        if (isBlank(vehicle.getCost()) && !isBlank(cost)) {
            vehicle.setCost(cost);
        }

        fleetRepo.save(vehicle);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Transactional
    public void updateVehicleStatusIfNeeded(FleetVehicle vehicle) {
        boolean updated = false;

        // Check if fully depreciated; if so, update maintenance status
        if (vehicle.getCost() != null && vehicle.getAcquisitionYear() != null) {
            double cost = parseDouble(vehicle.getCost());
            int acqYear = vehicle.getAcquisitionYear();
            if (cost > 0 && acqYear > 0) {
                int yearsUsed = java.time.Year.now().getValue() - acqYear;
                if (yearsUsed >= 10) {
                    if (!isBlank(vehicle.getMaintenanceStatus()) &&
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
                if (!isBlank(vehicle.getAdminLegaltionalStatus()) &&
                        !vehicle.getAdminLegaltionalStatus().equals("Registration Expired")) {
                    vehicle.setAdminLegaltionalStatus("Registration Expired");
                    updated = true;
                }
            }
        }

        if (updated) {
            fleetRepo.save(vehicle);
        }
    }

    private double parseDouble(String value) {
        if (value == null || value.trim().isEmpty())
            return 0;
        try {
            return Double.parseDouble(value.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}