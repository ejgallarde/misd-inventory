package ph.gov.phlpost.inventory.misddashboard.service;

import ph.gov.phlpost.inventory.misddashboard.model.Asset;
import ph.gov.phlpost.inventory.misddashboard.repository.AssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ITAssetService {

    private final AssetRepository assetRepo;
    private final AuditLogService auditService;

    public ITAssetService(AssetRepository assetRepo, AuditLogService auditService) {
        this.assetRepo = assetRepo;
        this.auditService = auditService;
    }

    @Transactional
    public void assignAsset(String assetTag, String employeeId, String notes) {
        Asset asset = assetRepo.findById(assetTag).orElseThrow(() -> new IllegalArgumentException("Asset not found."));
        asset.setCurrentOwnerID(employeeId);
        asset.setDeploymentStatus("Deployed");
        if (asset.getLifecycleStatus() == null || asset.getLifecycleStatus().isBlank()
                || "Procured / Pre-Deployment".equals(asset.getLifecycleStatus())) {
            asset.setLifecycleStatus("Active");
        }
        if (asset.getMaintenanceHealthStatus() == null || asset.getMaintenanceHealthStatus().isBlank()
                || "Under Repair".equals(asset.getMaintenanceHealthStatus())) {
            asset.setMaintenanceHealthStatus("Operational");
        }
        assetRepo.save(asset);
        auditService.logAssignment(assetTag, employeeId, "Checkout", notes);
    }

    @Transactional
    public void returnAsset(String assetTag, String notes) {
        Asset asset = assetRepo.findById(assetTag).orElseThrow(() -> new IllegalArgumentException("Asset not found."));
        String prevOwner = asset.getCurrentOwnerID();
        asset.setCurrentOwnerID(null);
        asset.setDeploymentStatus("In Storage");
        if (asset.getLifecycleStatus() == null || asset.getLifecycleStatus().isBlank()) {
            asset.setLifecycleStatus("Active");
        }
        assetRepo.save(asset);
        auditService.logAssignment(assetTag, prevOwner != null ? prevOwner : "MISD", "Return", notes);
    }

    @Transactional
    public void updateLifecycle(String assetTag, String status, String actionType, String notes) {
        Asset asset = assetRepo.findById(assetTag).orElseThrow(() -> new IllegalArgumentException("Asset not found."));

        if ("In Warranty Repair".equals(status)) {
            asset.setDeploymentStatus("With Service Center");
            asset.setMaintenanceHealthStatus("Under Repair");
            if (asset.getLifecycleStatus() == null || asset.getLifecycleStatus().isBlank()
                    || "Procured / Pre-Deployment".equals(asset.getLifecycleStatus())) {
                asset.setLifecycleStatus("Active");
            }
        } else if ("Unserviceable".equals(status)) {
            asset.setMaintenanceHealthStatus("Beyond Economic Repair (BER)");
            asset.setLifecycleStatus("End of Life (EOL)");
        } else if ("Retired".equals(status)) {
            asset.setCurrentOwnerID(null);
            asset.setDeploymentStatus("Unavailable");
            asset.setMaintenanceHealthStatus("Not Applicable");
            asset.setLifecycleStatus("Decommissioned / Retired");
        }

        assetRepo.save(asset);
        auditService.logLifecycleEvent(assetTag, "SYSTEM", actionType, notes);
    }
}