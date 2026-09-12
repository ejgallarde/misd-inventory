package ph.gov.phlpost.inventory.misddashboard.service;

import ph.gov.phlpost.inventory.misddashboard.model.SurveyAsset;
import ph.gov.phlpost.inventory.misddashboard.repository.SurveyAssetRepository;
import ph.gov.phlpost.inventory.misddashboard.util.TextUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SurveyAssetService {

    private final SurveyAssetRepository surveyAssetRepo;
    private final AuditLogService auditService;
    private final int maxReceiveQuantity;

    private static final DateTimeFormatter ASSET_TAG_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public SurveyAssetService(SurveyAssetRepository surveyAssetRepo, AuditLogService auditService,
            @Value("${surveyasset.receive.max-quantity:100}") int maxReceiveQuantity) {
        this.surveyAssetRepo = surveyAssetRepo;
        this.auditService = auditService;
        this.maxReceiveQuantity = maxReceiveQuantity;
    }

    @Transactional
    public void assignCustodian(Integer surveyAssetId, String employeeId, String notes) {
        SurveyAsset asset = surveyAssetRepo.findById(surveyAssetId)
                .orElseThrow(() -> new IllegalArgumentException("Survey asset not found."));

        asset.setAssignedCustodianID(employeeId);
        asset.setOperationalStatus("Deployed/Field Use");
        surveyAssetRepo.save(asset);
        auditService.logAssignment(auditReferenceId(asset), employeeId, "Survey Asset Checkout", notes);
    }

    @Transactional
    public void returnAsset(Integer surveyAssetId, String notes) {
        SurveyAsset asset = surveyAssetRepo.findById(surveyAssetId)
                .orElseThrow(() -> new IllegalArgumentException("Survey asset not found."));

        String previousCustodian = asset.getAssignedCustodianID();
        asset.setAssignedCustodianID(null);
        asset.setOperationalStatus("Available/Idle");
        surveyAssetRepo.save(asset);

        if (previousCustodian != null) {
            auditService.logAssignment(auditReferenceId(asset), previousCustodian, "Survey Asset Returned", notes);
        } else {
            auditService.logLifecycleEvent(auditReferenceId(asset), "EQUIPMENT ROOM", "Survey Asset Returned", notes);
        }
    }

    @Transactional
    public void markUnderCalibration(Integer surveyAssetId, String notes) {
        SurveyAsset asset = surveyAssetRepo.findById(surveyAssetId)
                .orElseThrow(() -> new IllegalArgumentException("Survey asset not found."));

        asset.setConditionStatus("Needs Calibration");
        surveyAssetRepo.save(asset);
        auditService.logLifecycleEvent(auditReferenceId(asset), "SYSTEM", "Marked Needs Calibration", notes);
    }

    @Transactional
    public void markUnderRepair(Integer surveyAssetId, String notes) {
        SurveyAsset asset = surveyAssetRepo.findById(surveyAssetId)
                .orElseThrow(() -> new IllegalArgumentException("Survey asset not found."));

        asset.setConditionStatus("Under Repair");
        surveyAssetRepo.save(asset);
        auditService.logLifecycleEvent(auditReferenceId(asset), "SYSTEM", "Marked Under Repair", notes);
    }

    @Transactional
    public void markBeyondEconomicRepair(Integer surveyAssetId, String notes) {
        SurveyAsset asset = surveyAssetRepo.findById(surveyAssetId)
                .orElseThrow(() -> new IllegalArgumentException("Survey asset not found."));

        asset.setOperationalStatus("Slated for Disposal");
        asset.setConditionStatus("Beyond Economic Repair (BER)");
        surveyAssetRepo.save(asset);
        auditService.logLifecycleEvent(auditReferenceId(asset), "SYSTEM", "Marked BER", notes);
    }

    @Transactional
    public void markStolen(Integer surveyAssetId, String notes) {
        SurveyAsset asset = surveyAssetRepo.findById(surveyAssetId)
                .orElseThrow(() -> new IllegalArgumentException("Survey asset not found."));

        asset.setAssignedCustodianID(null);
        asset.setAdminLegalStatus("Under Investigation");
        asset.setOperationalStatus("Stolen");
        asset.setConditionStatus("Not Applicable");
        surveyAssetRepo.save(asset);
        auditService.logLifecycleEvent(auditReferenceId(asset), "SYSTEM", "Marked Stolen", notes);
    }

    @Transactional
    public void markMissing(Integer surveyAssetId, String notes) {
        SurveyAsset asset = surveyAssetRepo.findById(surveyAssetId)
                .orElseThrow(() -> new IllegalArgumentException("Survey asset not found."));

        asset.setAssignedCustodianID(null);
        asset.setAdminLegalStatus("Under Investigation");
        asset.setOperationalStatus("Missing");
        asset.setConditionStatus("Not Applicable");
        surveyAssetRepo.save(asset);
        auditService.logLifecycleEvent(auditReferenceId(asset), "SYSTEM", "Marked Missing", notes);
    }

    @Transactional
    public void retire(Integer surveyAssetId, String notes) {
        SurveyAsset asset = surveyAssetRepo.findById(surveyAssetId)
                .orElseThrow(() -> new IllegalArgumentException("Survey asset not found."));

        asset.setAssignedCustodianID(null);
        asset.setAdminLegalStatus("Decommissioned");
        asset.setOperationalStatus("Slated for Disposal");
        asset.setConditionStatus("Beyond Economic Repair (BER)");
        surveyAssetRepo.save(asset);
        auditService.logLifecycleEvent(auditReferenceId(asset), "SYSTEM", "Survey Asset Retired", notes);
    }

    /**
     * Receives one or more units of the same cataloged survey equipment model
     * into storage, mirroring {@link ITAssetService#receiveAssets}. Quantity
     * bounds are validated here (the form's {@code min="1"} is client-side
     * only), and each unit is saved and audited individually.
     */
    @Transactional
    public List<SurveyAsset> receiveSurveyAssets(SurveyAsset baseAsset, int quantity, String performedBy) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }
        if (quantity > maxReceiveQuantity) {
            throw new IllegalArgumentException(
                    "A maximum of " + maxReceiveQuantity + " survey assets can be received at once.");
        }

        String datePrefix = "SVY-" + LocalDate.now().format(ASSET_TAG_DATE_FORMAT) + "-";
        List<SurveyAsset> created = new ArrayList<>();

        for (int index = 0; index < quantity; index++) {
            SurveyAsset newAsset = new SurveyAsset();
            newAsset.setCatalogID(baseAsset.getCatalogID());
            newAsset.setAcquisitionDate(baseAsset.getAcquisitionDate());
            newAsset.setCost(baseAsset.getCost());
            newAsset.setLastCalibrationDate(baseAsset.getLastCalibrationDate());
            newAsset.setCalibrationDueDate(baseAsset.getCalibrationDueDate());
            newAsset.setRemarks(baseAsset.getRemarks());

            newAsset.setAssignedCustodianID(null);
            newAsset.setAdminLegalStatus("Registered/Accountable");
            newAsset.setOperationalStatus("Available/Idle");
            newAsset.setConditionStatus("Operational");

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

            surveyAssetRepo.saveAndFlush(newAsset);
            auditService.logLifecycleEvent(auditReferenceId(newAsset), performedBy, "Survey Asset Received",
                    statusSummary(newAsset) + appendNotes(newAsset.getRemarks()));
            created.add(newAsset);
        }

        return created;
    }

    /**
     * Next tag in the SVY-yyyy-MM-dd-NNNNN sequence for the given day.
     * Synchronized against concurrent receipts within this instance.
     */
    private synchronized String generateNextAssetTag(String datePrefix) {
        Optional<SurveyAsset> lastAsset = surveyAssetRepo.findTopByAssetTagStartingWithOrderByAssetTagDesc(datePrefix);

        if (lastAsset.isEmpty() || lastAsset.get().getAssetTag() == null) {
            return datePrefix + "00001";
        }

        String lastTag = lastAsset.get().getAssetTag();
        try {
            int sequence = Integer.parseInt(lastTag.substring(datePrefix.length()));
            return datePrefix + String.format("%05d", sequence + 1);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(
                    "Could not determine the next asset tag after '" + lastTag + "'. "
                            + "Enter an asset tag manually or correct the existing record.");
        }
    }

    private String appendNotes(String notes) {
        return notes == null || notes.isBlank() ? "" : "; Notes: " + notes.trim();
    }

    /**
     * Audit reference for a survey asset: always the immutable primary key, the
     * same convention used for fleet vehicles (see {@link FleetService#auditReferenceId}).
     * Asset tag and serial number are both optional-unique columns, so neither is
     * safe to key history on.
     */
    public static String auditReferenceId(SurveyAsset asset) {
        return "SURVEYASSET-" + asset.getSurveyAssetID();
    }

    /**
     * Applies an edit from the detail panel and records it in the audit log.
     */
    @Transactional
    public void updateSurveyAssetDetails(SurveyAsset submitted, String performedBy) {
        SurveyAsset asset = surveyAssetRepo.findById(submitted.getSurveyAssetID())
                .orElseThrow(() -> new IllegalArgumentException("Survey asset not found."));

        String previousStatuses = statusSummary(asset);

        // Always-editable fields
        asset.setCalibrationDueDate(submitted.getCalibrationDueDate());
        asset.setLastCalibrationDate(submitted.getLastCalibrationDate());
        asset.setAdminLegalStatus(submitted.getAdminLegalStatus());
        asset.setOperationalStatus(submitted.getOperationalStatus());
        asset.setConditionStatus(submitted.getConditionStatus());
        asset.setRemarks(submitted.getRemarks());

        // Lock-once fields: only applied when the current database value is blank
        if (TextUtils.isBlank(asset.getAssetTag()) && !TextUtils.isBlank(submitted.getAssetTag())) {
            asset.setAssetTag(submitted.getAssetTag());
        }
        if (TextUtils.isBlank(asset.getSerialNumber()) && !TextUtils.isBlank(submitted.getSerialNumber())) {
            asset.setSerialNumber(submitted.getSerialNumber());
        }
        // CatalogID is intentionally never touched here: this method mutates the
        // managed entity field-by-field rather than overwriting it wholesale, so
        // simply never setting it keeps a received unit's catalog assignment
        // permanent with no extra guard needed.
        if (asset.getAcquisitionDate() == null && submitted.getAcquisitionDate() != null) {
            asset.setAcquisitionDate(submitted.getAcquisitionDate());
        }
        if (asset.getCost() == null && submitted.getCost() != null) {
            asset.setCost(submitted.getCost());
        }

        surveyAssetRepo.save(asset);

        String currentStatuses = statusSummary(asset);
        String actionType = previousStatuses.equals(currentStatuses)
                ? "Survey Asset Details Updated"
                : "Survey Asset Status Updated";
        auditService.logLifecycleEvent(auditReferenceId(asset), performedBy, actionType,
                previousStatuses + " -> " + currentStatuses);
    }

    private static String statusSummary(SurveyAsset asset) {
        return "Legal: " + displayValue(asset.getAdminLegalStatus())
                + "; Operational: " + displayValue(asset.getOperationalStatus())
                + "; Condition: " + displayValue(asset.getConditionStatus());
    }

    private static String displayValue(String value) {
        return TextUtils.isBlank(value) ? "None" : value;
    }

    /** Loads a survey asset for display. Strictly read-only. */
    @Transactional(readOnly = true)
    public SurveyAsset findSurveyAsset(Integer surveyAssetId) {
        return surveyAssetRepo.findById(surveyAssetId)
                .orElseThrow(() -> new IllegalArgumentException("Survey asset not found."));
    }

    /**
     * Number of years an asset is depreciated over, measured from acquisition.
     * Also the age at which a survey asset is surfaced on the dashboard as
     * needing attention, so the detail panel and the "requires attention" list
     * agree.
     */
    public static final int USEFUL_LIFE_YEARS = 10;

    /** Display-only conclusions drawn from a survey asset's stored values. */
    public record SurveyAssetStatusFlags(boolean fullyDepreciated, boolean calibrationOverdue) {
    }

    public static SurveyAssetStatusFlags deriveStatusFlags(SurveyAsset asset) {
        boolean fullyDepreciated = false;
        if (asset.getCost() != null
                && asset.getCost().signum() > 0
                && asset.getAcquisitionDate() != null) {
            fullyDepreciated = Year.now().getValue() - asset.getAcquisitionDate().getYear() >= USEFUL_LIFE_YEARS;
        }

        boolean calibrationOverdue = asset.getCalibrationDueDate() != null
                && asset.getCalibrationDueDate().isBefore(LocalDate.now());

        return new SurveyAssetStatusFlags(fullyDepreciated, calibrationOverdue);
    }

}
