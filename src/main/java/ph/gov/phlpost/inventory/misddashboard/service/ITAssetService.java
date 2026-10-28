package ph.gov.phlpost.inventory.misddashboard.service;

import ph.gov.phlpost.inventory.misddashboard.model.Asset;
import ph.gov.phlpost.inventory.misddashboard.model.Personnel;
import ph.gov.phlpost.inventory.misddashboard.repository.AssetRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.PersonnelRepository;
import ph.gov.phlpost.inventory.misddashboard.util.Amounts;
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
    private final AssetWorkflowHelper workflow;
    private final AccountabilityService accountabilityService;

    private static final DateTimeFormatter ASSET_TAG_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ITAssetService(AssetRepository assetRepo, PersonnelRepository personnelRepo, AuditLogService auditService,
            AssetWorkflowHelper workflow, AccountabilityService accountabilityService,
            @Value("${asset.workflow.supplier-owner-id:SUPPLIER}") String supplierOwnerId,
            @Value("${asset.workflow.maintenance-technician-job-title:Computer Maintenance Technologist}") String maintenanceTechnicianJobTitle,
            @Value("${asset.receive.max-quantity:100}") int maxReceiveQuantity) {
        this.assetRepo = assetRepo;
        this.personnelRepo = personnelRepo;
        this.auditService = auditService;
        this.workflow = workflow;
        this.accountabilityService = accountabilityService;
        this.supplierOwnerId = supplierOwnerId;
        this.maintenanceTechnicianJobTitle = maintenanceTechnicianJobTitle;
        this.maxReceiveQuantity = maxReceiveQuantity;
    }

    /**
     * Server-side copy of the Actions-menu rules in asset-table.html. The menu
     * used to be the only gate, so a stale page or a hand-made POST could, for
     * example, deploy a retired or BER asset to an employee.
     */
    private void requireAllowed(boolean allowed, Asset asset, String action) {
        if (!allowed) {
            throw new IllegalArgumentException("Cannot " + action + ": asset " + asset.getAssetTag()
                    + " is " + statusSummary(asset) + ".");
        }
    }

    /** The guard every workflow action except retirement shares. */
    private boolean isWorkable(Asset asset) {
        return workflow.hasActions(asset.getLifecycleStatus())
                && !workflow.isUnserviceable(asset.getMaintenanceHealthStatus());
    }

    /**
     * Deploys an asset to {@code employeeId} (the user). The accountable owner is
     * {@code accountableId} when given, otherwise the user if their employment
     * status allows it, otherwise their nearest eligible manager
     * ({@link AccountabilityService}).
     */
    @Transactional
    public AccountabilityService.Accountability assignAsset(String assetTag, String employeeId, String accountableId,
            String notes, String performedBy) {
        Asset asset = assetRepo.findById(assetTag).orElseThrow(() -> new IllegalArgumentException("Asset not found."));
        requireAllowed(isWorkable(asset) && (workflow.canAssign(asset.getDeploymentStatus())
                || workflow.canReassign(asset.getDeploymentStatus())), asset, "assign it");
        AccountabilityService.Accountability accountability = accountabilityService.resolve(employeeId, accountableId);
        String previousUser = asset.getAssignedUserID() != null ? asset.getAssignedUserID() : asset.getCurrentOwnerID();
        asset.setAssignedUserID(accountability.assignedId());
        asset.setCurrentOwnerID(accountability.accountableId());
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
        String actionType = previousUser == null || previousUser.isBlank() ? "Checkout" : "Reassignment";
        String accountabilityNote = accountabilityNote(accountability);
        auditService.logAssignment(assetTag, accountability.assignedId(), actionType,
                accountabilityNote + appendNotes(notes));
        if (!accountability.accountableId().equals(accountability.assignedId())) {
            // So the accountable owner's own history shows what they signed for.
            auditService.logAssignment(assetTag, accountability.accountableId(), "Accountability Assigned",
                    accountabilityNote + appendNotes(notes));
        }
        auditService.logLifecycleEvent(assetTag, performedBy, actionType,
                statusSummary(asset) + "; " + accountabilityNote + appendNotes(notes));
        return accountability;
    }

    static String accountabilityNote(AccountabilityService.Accountability accountability) {
        String basis = switch (accountability.basis()) {
            case SELF -> "";
            case MANAGER -> " (manager; user is not eligible)";
            case CHOSEN -> " (chosen)";
        };
        return "User: " + accountability.assignedId() + "; Accountable: " + accountability.accountableId() + basis;
    }

    @Transactional
    public void returnAsset(String assetTag, String notes, String performedBy) {
        Asset asset = assetRepo.findById(assetTag).orElseThrow(() -> new IllegalArgumentException("Asset not found."));
        requireAllowed(isWorkable(asset) && workflow.canReturn(asset.getDeploymentStatus()), asset,
                "return it to MISD");
        String prevOwner = asset.getAssignedUserID() != null ? asset.getAssignedUserID() : asset.getCurrentOwnerID();
        asset.setCurrentOwnerID(null);
        asset.setAssignedUserID(null);
        asset.setDeploymentStatus("In Storage");
        if (asset.getLifecycleStatus() == null || asset.getLifecycleStatus().isBlank()) {
            asset.setLifecycleStatus("Active");
        }
        assetRepo.save(asset);
        logAssignmentForExistingOwner(assetTag, prevOwner, "Return", notes);
        auditService.logLifecycleEvent(assetTag, performedBy, "Returned to MISD",
                statusSummary(asset) + appendNotes(notes));
    }

    @Transactional
    public void sendForWarranty(String assetTag, String notes, String performedBy) {
        Asset asset = findAsset(assetTag);
        requireAllowed(isWorkable(asset), asset, "send it for warranty repair");
        ensureNotUnderRepair(asset);
        ensureSupplierOwnerExists();
        // The supplier holds custody during warranty repair; nobody uses it.
        asset.setCurrentOwnerID(supplierOwnerId);
        asset.setAssignedUserID(null);
        applyRepairState(asset, "With Service Center");
        assetRepo.save(asset);
        auditService.logAssignment(assetTag, supplierOwnerId, "Warranty Repair", notes);
        auditService.logLifecycleEvent(assetTag, performedBy, "Sent for Warranty Repair", notes);
    }

    @Transactional
    public void sendForMisdMaintenance(String assetTag, String technicianId, String notes, String performedBy) {
        Personnel technician = personnelRepo.findById(technicianId)
                .orElseThrow(() -> new IllegalArgumentException("Selected technician was not found."));
        if (!isMaintenanceTechnician(technician.getJobTitle())) {
            throw new IllegalArgumentException(
                    "Selected employee must be a " + maintenanceTechnicianJobTitle + " I, II, or III.");
        }

        Asset asset = findAsset(assetTag);
        requireAllowed(isWorkable(asset), asset, "send it for MISD maintenance");
        ensureNotUnderRepair(asset);
        // The technician works on it; if they are not eligible to be accountable
        // (e.g. a contractual technologist), their manager is.
        AccountabilityService.Accountability accountability =
                accountabilityService.resolve(technician.getEmployeeID(), null);
        asset.setAssignedUserID(accountability.assignedId());
        asset.setCurrentOwnerID(accountability.accountableId());
        applyRepairState(asset, "With MISD Technician");
        assetRepo.save(asset);
        auditService.logAssignment(assetTag, technician.getEmployeeID(), "MISD Maintenance", notes);
        auditService.logLifecycleEvent(assetTag, performedBy, "Sent for MISD Maintenance",
                "Technician: " + technician.getEmployeeID() + "; " + accountabilityNote(accountability)
                        + appendNotes(notes));
    }

    @Transactional
    public void markRepaired(String assetTag, String notes, String performedBy) {
        Asset asset = findAsset(assetTag);
        if (!workflow.hasActions(asset.getLifecycleStatus())
                || !"Under Repair".equals(asset.getMaintenanceHealthStatus())
                || !("With Service Center".equals(asset.getDeploymentStatus())
                        || "With MISD Technician".equals(asset.getDeploymentStatus()))) {
            throw new IllegalArgumentException("Only assets currently assigned for repair can be marked repaired.");
        }

        String previousOwner = asset.getAssignedUserID() != null ? asset.getAssignedUserID() : asset.getCurrentOwnerID();
        asset.setCurrentOwnerID(null);
        asset.setAssignedUserID(null);
        asset.setDeploymentStatus("In Storage");
        asset.setMaintenanceHealthStatus("Operational");
        asset.setLifecycleStatus("Active");
        assetRepo.save(asset);
        logAssignmentForExistingOwner(assetTag, previousOwner, "Repair Completed", notes);
        auditService.logLifecycleEvent(assetTag, performedBy, "Asset Repaired", notes);
    }

    @Transactional
    public void updateLifecycle(String assetTag, String status, String actionType, String notes,
            String performedBy) {
        Asset asset = findAsset(assetTag);
        if ("Unserviceable".equals(status)) {
            requireAllowed(isWorkable(asset), asset, "mark it unserviceable");
        } else if ("Retired".equals(status)) {
            requireAllowed(workflow.hasActions(asset.getLifecycleStatus()), asset, "retire it");
        }
        String previousOwner = asset.getAssignedUserID() != null ? asset.getAssignedUserID() : asset.getCurrentOwnerID();

        if ("Unserviceable".equals(status)) {
            // Declaring an asset beyond repair also returns it to MISD custody.
            // It previously stayed "Deployed" and accountable to its holder, and
            // AssetWorkflowHelper.isUnserviceable then hid every action that
            // could have corrected that — leaving the asset stuck on someone's
            // accountability with no way off it.
            asset.setCurrentOwnerID(null);
            asset.setAssignedUserID(null);
            asset.setDeploymentStatus("Unavailable");
            asset.setMaintenanceHealthStatus("Beyond Economic Repair (BER)");
            asset.setLifecycleStatus("End of Life (EOL)");
        } else if ("Retired".equals(status)) {
            asset.setCurrentOwnerID(null);
            asset.setAssignedUserID(null);
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
        auditService.logLifecycleEvent(assetTag, performedBy, actionType,
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

    /**
     * Applies an edit from the detail slideout.
     *
     * <p>
     * Only the fields the slideout offers are copied onto the stored record.
     * This used to save the submitted object as-is, so any property the client
     * left out was written back as NULL, and any it added was accepted. The
     * serial number is lock-once, as the slideout presents it: it can be filled
     * in when blank but not changed afterwards.
     */
    @Transactional
    public void updateAsset(Asset submitted, String performedBy, boolean operational, boolean financial) {
        Asset existingAsset = findAsset(submitted.getAssetTag());
        String previousOwner = existingAsset.getCurrentOwnerID();
        String previousStatuses = statusSummary(existingAsset);
        java.math.BigDecimal previousPrice = existingAsset.getPurchasePrice();

        // Role split (see config/AppRole): the purchase price belongs to FINANCE,
        // everything else in the slideout to CLERK. ADMIN passes both flags.
        if (operational) {
            existingAsset.setCatalogID(submitted.getCatalogID());
            existingAsset.setPurchaseDate(submitted.getPurchaseDate());
            existingAsset.setDeploymentStatus(submitted.getDeploymentStatus());
            existingAsset.setMaintenanceHealthStatus(submitted.getMaintenanceHealthStatus());
            existingAsset.setLifecycleStatus(submitted.getLifecycleStatus());
            // The slideout cannot assign anyone (that goes through assignAsset and
            // its accountability rule); it can only release the asset, which it
            // signals by submitting no owner. Releasing clears user and owner.
            if (submitted.getCurrentOwnerID() == null) {
                existingAsset.setCurrentOwnerID(null);
                existingAsset.setAssignedUserID(null);
            }
            existingAsset.setRemarks(submitted.getRemarks());
            if (existingAsset.getSerialNumber() == null || existingAsset.getSerialNumber().isBlank()) {
                existingAsset.setSerialNumber(submitted.getSerialNumber());
            }
        }
        if (financial) {
            existingAsset.setPurchasePrice(submitted.getPurchasePrice());
        }
        assetRepo.save(existingAsset);
        String costNote = Amounts.changeNote("Purchase price", previousPrice, existingAsset.getPurchasePrice());

        if (!java.util.Objects.equals(previousOwner, existingAsset.getCurrentOwnerID())) {
            String auditOwner = existingAsset.getCurrentOwnerID() != null
                    ? existingAsset.getCurrentOwnerID()
                    : previousOwner;
            logAssignmentForExistingOwner(existingAsset.getAssetTag(), auditOwner, "Assignment Updated",
                    "Owner changed from " + displayValue(previousOwner) + " to "
                            + displayValue(existingAsset.getCurrentOwnerID()) + ".");
        }

        String currentStatuses = statusSummary(existingAsset);
        String actionType = !previousStatuses.equals(currentStatuses) ? "Asset Status Updated"
                : !costNote.isEmpty() && !operational ? "Asset Cost Updated"
                        : "Asset Details Updated";
        auditService.logLifecycleEvent(existingAsset.getAssetTag(), performedBy, actionType,
                previousStatuses + " -> " + currentStatuses + costNote);
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