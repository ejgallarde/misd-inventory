package ph.gov.phlpost.inventory.misddashboard.service;

import ph.gov.phlpost.inventory.misddashboard.model.FleetVehicle;
import ph.gov.phlpost.inventory.misddashboard.repository.FleetVehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        vehicle.setCurrentStatus("Deployed");
        fleetRepo.save(vehicle);
        auditService.logAssignment(vehicle.getPlateNumber(), employeeId, "Vehicle Checkout", notes);
    }

    @Transactional
    public void returnVehicle(Integer vehicleId, String notes) {
        FleetVehicle vehicle = fleetRepo.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found."));

        String previousDriver = vehicle.getAssignedDriverID();
        vehicle.setAssignedDriverID(null);
        vehicle.setCurrentStatus("Active");
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
        vehicle.setCurrentStatus("Retired");
        fleetRepo.save(vehicle);
        auditService.logLifecycleEvent(vehicle.getPlateNumber(), "SYSTEM", "Vehicle Retired", notes);
    }
}