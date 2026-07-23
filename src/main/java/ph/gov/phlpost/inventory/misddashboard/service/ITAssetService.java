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
        asset.setCurrentStatus("Deployed");
        assetRepo.save(asset);
        auditService.logAssignment(assetTag, employeeId, "Checkout", notes);
    }

    @Transactional
    public void returnAsset(String assetTag, String notes) {
        Asset asset = assetRepo.findById(assetTag).orElseThrow(() -> new IllegalArgumentException("Asset not found."));
        String prevOwner = asset.getCurrentOwnerID();
        asset.setCurrentOwnerID(null);
        asset.setCurrentStatus("In Storage");
        assetRepo.save(asset);
        auditService.logAssignment(assetTag, prevOwner != null ? prevOwner : "MISD", "Return", notes);
    }

    @Transactional
    public void updateLifecycle(String assetTag, String status, String actionType, String notes) {
        Asset asset = assetRepo.findById(assetTag).orElseThrow(() -> new IllegalArgumentException("Asset not found."));
        asset.setCurrentOwnerID(null);
        asset.setCurrentStatus(status);
        assetRepo.save(asset);
        auditService.logLifecycleEvent(assetTag, "SYSTEM", actionType, notes);
    }
}