package ph.gov.phlpost.inventory.misddashboard.service;

import ph.gov.phlpost.inventory.misddashboard.model.Asset;
import ph.gov.phlpost.inventory.misddashboard.model.Personnel;
import ph.gov.phlpost.inventory.misddashboard.repository.AssetRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.PersonnelRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ITAssetService {

    private final AssetRepository assetRepo;
    private final PersonnelRepository personnelRepo;
    private final AuditLogService auditService;
    private final String supplierOwnerId;
    private final String maintenanceTechnicianJobTitle;
    private final int maxReceiveQuantity;

    private static final DateTimeFormatter ASSET_TAG_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ITAssetService(AssetRepository assetRepo, PersonnelRepository personnelRepo, AuditLogService auditService,
            @Value("${asset.workflow.supplier-owner-id:SUPPLIER}") String supplierOwnerId,
            @Value("${asset.workflow.maintenance-technician-job-title:Computer Maintenance Technologist}") String maintenanceTechnicianJobTitle,
            @Value("${asset.receive.max-quantity:100}") int maxReceiveQuantity) {
        this.assetRepo = assetRepo;
        this.personnelRepo = personnelRepo;
        this.auditService = auditService;
        this.supplierOwnerId = supplierOwnerId;
        this.maintenanceTechnicianJobTitle = maintenanceTechnicianJobTitle;
        this.maxReceiveQuantity = maxReceiveQuantity;
    }

    @Transactional
    public void assignAsset(String assetTag, String employeeId, String notes) {
        Asset asset = assetRepo.findById(assetTag).orElseThrow(() -> new IllegalArgumentException("Asset not found."));
        String previousOwner = asset.getCurrentOwnerID();
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
        String actionType = previousOwner == null || previousOwner.isBlank() ? "Checkout" : "Reassignment";
        auditService.logAssignment(assetTag, employeeId, actionType, notes);
        auditService.logLifecycleEvent(assetTag, "SYSTEM", actionType,
                statusSummary(asset) + appendNotes(notes));
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
        logAssignmentForExistingOwner(assetTag, prevOwner, "Return", notes);
        auditService.logLifecycleEvent(assetTag, "SYSTEM", "Returned to MISD",
                statusSummary(asset) + appendNotes(notes));
    }

    @Transactional
    public void sendForWarranty(String assetTag, String notes) {
        Asset asset = findAsset(assetTag);
        ensureNotUnderRepair(asset);
        ensureSupplierOwnerExists();
        asset.setCurrentOwnerID(supplierOwnerId);
        applyRepairState(asset, "With Service Center");
        assetRepo.save(asset);
        auditService.logAssignment(assetTag, supplierOwnerId, "Warranty Repair", notes);
        auditService.logLifecycleEvent(assetTag, "SYSTEM", "Sent for Warranty Repair", notes);
    }

    @Transactional
    public void sendForMisdMaintenance(String assetTag, String technicianId, String notes) {
        Personnel technician = personnelRepo.findById(technicianId)
                .orElseThrow(() -> new IllegalArgumentException("Selected technician was not found."));
        if (!isMaintenanceTechnician(technician.getJobTitle())) {
            throw new IllegalArgumentException(
                    "Selected employee must be a " + maintenanceTechnicianJobTitle + " I, II, or III.");
        }

        Asset asset = findAsset(assetTag);
        ensureNotUnderRepair(asset);
        asset.setCurrentOwnerID(technician.getEmployeeID());
        applyRepairState(asset, "With MISD Technician");
        assetRepo.save(asset);
        auditService.logAssignment(assetTag, technician.getEmployeeID(), "MISD Maintenance", notes);
        auditService.logLifecycleEvent(assetTag, technician.getEmployeeID(), "Sent for MISD Maintenance", notes);
    }

    @Transactional
    public void markRepaired(String assetTag, String notes) {
        Asset asset = findAsset(assetTag);
        if (!"Under Repair".equals(asset.getMaintenanceHealthStatus())
                || !("With Service Center".equals(asset.getDeploymentStatus())
                        || "With MISD Technician".equals(asset.getDeploymentStatus()))) {
            throw new IllegalArgumentException("Only assets currently assigned for repair can be marked repaired.");
        }

        String previousOwner = asset.getCurrentOwnerID();
        asset.setCurrentOwnerID(null);
        asset.setDeploymentStatus("In Storage");
        asset.setMaintenanceHealthStatus("Operational");
        asset.setLifecycleStatus("Active");
        assetRepo.save(asset);
        logAssignmentForExistingOwner(assetTag, previousOwner, "Repair Completed", notes);
        auditService.logLifecycleEvent(assetTag, "SYSTEM", "Asset Repaired", notes);
    }

    @Transactional
    public void updateLifecycle(String assetTag, String status, String actionType, String notes) {
        Asset asset = findAsset(assetTag);
        String previousOwner = asset.getCurrentOwnerID();

        if ("Unserviceable".equals(status)) {
            // Declaring an asset beyond repair also returns it to MISD custody.
            // It previously stayed "Deployed" and accountable to its holder, and
            // AssetWorkflowHelper.isUnserviceable then hid every action that
            // could have corrected that — leaving the asset stuck on someone's
            // accountability with no way off it.
            asset.setCurrentOwnerID(null);
            asset.setDeploymentStatus("Unavailable");
            asset.setMaintenanceHealthStatus("Beyond Economic Repair (BER)");
            asset.setLifecycleStatus("End of Life (EOL)");
        } else if ("Retired".equals(status)) {
            asset.setCurrentOwnerID(null);
            asset.setDeploymentStatus("Unavailable");
            asset.setMaintenanceHealthStatus("Not Applicable");
            asset.setLifecycleStatus("Decommissioned / Retired");
        }

        assetRepo.save(asset);
        if ("Retired".equals(status)) {
            logAssignmentForExistingOwner(assetTag, previousOwner, "Asset Retired", notes);
        } else if ("Unserviceable".equals(status)) {
            logAssignmentForExistingOwner(assetTag, previousOwner, "Returned - Unserviceable", notes);
        }
        auditService.logLifecycleEvent(assetTag, "SYSTEM", actionType,
                statusSummary(asset) + appendNotes(notes));
    }

    @Transactional
    public void recordReceived(Asset asset, String performedBy) {
        auditService.logLifecycleEvent(asset.getAssetTag(), performedBy, "Asset Received",
                statusSummary(asset) + appendNotes(asset.getRemarks()));
    }

    /**
     * Receives one or more units of the same catalog item into storage and
     * returns the asset tags created.
     *
     * <p>
     * The loop used to live in the controller with no transaction, so a failure
     * part-way through a batch left the first few assets created while the user
     * was told the whole receipt had failed. The quantity was also an unchecked
     * {@code int} — the form's {@code min="1"} is client-side only, so a
     * mistyped 1000 created a thousand rows without confirmation and a 0 reported
     * "Successfully received 0 asset(s)".
     */
    @Transactional
    public List<String> receiveAssets(Asset baseAsset, int quantity, String performedBy) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }
        if (quantity > maxReceiveQuantity) {
            throw new IllegalArgumentException(
                    "A maximum of " + maxReceiveQuantity + " assets can be received at once.");
        }

        String datePrefix = "PPC-" + LocalDate.now().format(ASSET_TAG_DATE_FORMAT) + "-";
        List<String> createdAssetTags = new ArrayList<>();

        for (int index = 0; index < quantity; index++) {
            Asset newAsset = new Asset();
            newAsset.setCatalogID(baseAsset.getCatalogID());
            newAsset.setPurchaseDate(baseAsset.getPurchaseDate());
            newAsset.setPurchasePrice(baseAsset.getPurchasePrice());
            newAsset.setRemarks(baseAsset.getRemarks());

            newAsset.setDeploymentStatus("In Storage");
            newAsset.setMaintenanceHealthStatus("Operational");
            newAsset.setLifecycleStatus("Procured / Pre-Deployment");
            newAsset.setCurrentOwnerID(null);

            // A serial number identifies one physical unit, so it is only carried
            // over for a single-unit receipt.
            if (quantity == 1) {
                newAsset.setSerialNumber(baseAsset.getSerialNumber());
                String requestedTag = baseAsset.getAssetTag() == null ? "" : baseAsset.getAssetTag().trim();
                newAsset.setAssetTag(requestedTag.isEmpty() ? generateNextAssetTag(datePrefix) : requestedTag);
            } else {
                newAsset.setSerialNumber(null);
                newAsset.setAssetTag(generateNextAssetTag(datePrefix));
            }

            assetRepo.saveAndFlush(newAsset);
            recordReceived(newAsset, performedBy);
            createdAssetTags.add(newAsset.getAssetTag());
        }

        return createdAssetTags;
    }

    /**
     * Next tag in the PPC-yyyy-MM-dd-NNNNN sequence for the given day.
     * Synchronized against concurrent receipts within this instance.
     */
    private synchronized String generateNextAssetTag(String datePrefix) {
        Optional<Asset> lastAsset = assetRepo.findTopByAssetTagStartingWithOrderByAssetTagDesc(datePrefix);

        if (lastAsset.isEmpty() || lastAsset.get().getAssetTag() == null) {
            return datePrefix + "00001";
        }

        String lastTag = lastAsset.get().getAssetTag();
        try {
            int sequence = Integer.parseInt(lastTag.substring(datePrefix.length()));
            return datePrefix + String.format("%05d", sequence + 1);
        } catch (RuntimeException ex) {
            // Previously fell back to "99999", which silently collided with itself
            // on the next receipt. Failing here rolls the whole batch back instead.
            throw new IllegalArgumentException(
                    "Could not determine the next asset tag after '" + lastTag + "'. "
                            + "Enter an asset tag manually or correct the existing record.");
        }
    }

    @Transactional
    public void updateAsset(Asset updatedAsset, String performedBy) {
        Asset existingAsset = findAsset(updatedAsset.getAssetTag());
        String previousOwner = existingAsset.getCurrentOwnerID();
        String previousStatuses = statusSummary(existingAsset);

        assetRepo.save(updatedAsset);

        if (!java.util.Objects.equals(previousOwner, updatedAsset.getCurrentOwnerID())) {
            String auditOwner = updatedAsset.getCurrentOwnerID() != null
                    ? updatedAsset.getCurrentOwnerID()
                    : previousOwner;
            logAssignmentForExistingOwner(updatedAsset.getAssetTag(), auditOwner, "Assignment Updated",
                    "Owner changed from " + displayValue(previousOwner) + " to "
                            + displayValue(updatedAsset.getCurrentOwnerID()) + ".");
        }

        String currentStatuses = statusSummary(updatedAsset);
        String actionType = previousStatuses.equals(currentStatuses) ? "Asset Details Updated" : "Asset Status Updated";
        auditService.logLifecycleEvent(updatedAsset.getAssetTag(), performedBy, actionType,
                previousStatuses + " -> " + currentStatuses);
    }

    private Asset findAsset(String assetTag) {
        return assetRepo.findById(assetTag)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found."));
    }

    private void applyRepairState(Asset asset, String deploymentStatus) {
        asset.setDeploymentStatus(deploymentStatus);
        asset.setMaintenanceHealthStatus("Under Repair");
        asset.setLifecycleStatus("Inactive");
    }

    private void ensureNotUnderRepair(Asset asset) {
        if ("Under Repair".equals(asset.getMaintenanceHealthStatus())) {
            throw new IllegalArgumentException("Asset is already under repair.");
        }
    }

    private boolean isMaintenanceTechnician(String jobTitle) {
        if (jobTitle == null || jobTitle.isBlank()) {
            return false;
        }

        String normalizedJobTitle = jobTitle.trim();
        return normalizedJobTitle.equalsIgnoreCase(maintenanceTechnicianJobTitle)
                || normalizedJobTitle.equalsIgnoreCase(maintenanceTechnicianJobTitle + " I")
                || normalizedJobTitle.equalsIgnoreCase(maintenanceTechnicianJobTitle + " II")
                || normalizedJobTitle.equalsIgnoreCase(maintenanceTechnicianJobTitle + " III");
    }

    private void logAssignmentForExistingOwner(String assetTag, String ownerId, String actionType, String notes) {
        if (ownerId != null && !ownerId.isBlank() && personnelRepo.existsById(ownerId)) {
            auditService.logAssignment(assetTag, ownerId, actionType, notes);
        }
    }

    private String statusSummary(Asset asset) {
        return "Deployment: " + displayValue(asset.getDeploymentStatus())
                + "; Health: " + displayValue(asset.getMaintenanceHealthStatus())
                + "; Lifecycle: " + displayValue(asset.getLifecycleStatus());
    }

    private String appendNotes(String notes) {
        return notes == null || notes.isBlank() ? "" : "; Notes: " + notes.trim();
    }

    private String displayValue(String value) {
        return value == null || value.isBlank() ? "None" : value;
    }

    private void ensureSupplierOwnerExists() {
        if (personnelRepo.existsById(supplierOwnerId)) {
            return;
        }

        Personnel supplier = new Personnel();
        supplier.setEmployeeID(supplierOwnerId);
        supplier.setFirstName("External");
        supplier.setLastName("Supplier");
        supplier.setJobTitle("External Supplier");
        supplier.setDepartment("External Supplier");
        supplier.setDivision("Warranty Service");
        personnelRepo.saveAndFlush(supplier);
    }
}