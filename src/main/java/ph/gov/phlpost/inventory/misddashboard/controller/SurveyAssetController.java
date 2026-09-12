package ph.gov.phlpost.inventory.misddashboard.controller;

import ph.gov.phlpost.inventory.misddashboard.model.SurveyAsset;
import ph.gov.phlpost.inventory.misddashboard.model.SurveyEquipmentCatalog;
import ph.gov.phlpost.inventory.misddashboard.repository.SurveyAssetRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.SurveyEquipmentCatalogRepository;
import ph.gov.phlpost.inventory.misddashboard.service.AssetHistoryService;
import ph.gov.phlpost.inventory.misddashboard.service.DocumentService;
import ph.gov.phlpost.inventory.misddashboard.service.RegistryService;
import ph.gov.phlpost.inventory.misddashboard.service.SurveyAssetService;
import ph.gov.phlpost.inventory.misddashboard.util.TextUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Controller
@RequestMapping("/survey-assets")
public class SurveyAssetController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SurveyAssetController.class);

    private final SurveyAssetRepository surveyAssetRepo;
    private final SurveyEquipmentCatalogRepository surveyCatalogRepo;
    private final SurveyAssetService surveyAssetService;
    private final RegistryService registryService;
    private final DocumentService documentService;
    private final AssetHistoryService assetHistoryService;
    private final JsonMapper jsonMapper;

    @Value("${document.upload.max-size-mb:15}")
    private int documentUploadMaxSizeMb;

    @Value("${document.upload.allowed-extensions:pdf,jpg,jpeg,png,doc,docx,xls,xlsx}")
    private String documentUploadAllowedExtensions;

    @Value("${document.upload.categories.surveyasset}")
    private String surveyAssetDocumentUploadCategoriesCsv;

    @Value("#{'${dropdown.surveyasset-admin-legal-statuses}'.split(',')}")
    private List<String> surveyAssetAdminLegalStatuses;

    @Value("#{'${dropdown.surveyasset-operational-statuses}'.split(',')}")
    private List<String> surveyAssetOperationalStatuses;

    @Value("#{'${dropdown.surveyasset-condition-statuses}'.split(',')}")
    private List<String> surveyAssetConditionStatuses;

    public SurveyAssetController(SurveyAssetRepository surveyAssetRepo,
            SurveyEquipmentCatalogRepository surveyCatalogRepo,
            SurveyAssetService surveyAssetService,
            RegistryService registryService,
            DocumentService documentService,
            AssetHistoryService assetHistoryService,
            JsonMapper jsonMapper) {
        this.surveyAssetRepo = surveyAssetRepo;
        this.surveyCatalogRepo = surveyCatalogRepo;
        this.surveyAssetService = surveyAssetService;
        this.registryService = registryService;
        this.documentService = documentService;
        this.assetHistoryService = assetHistoryService;
        this.jsonMapper = jsonMapper;
    }

    @GetMapping
    public String viewAllSurveyAssets(@RequestParam(required = false) String filter, Model model) {
        model.addAttribute("allSurveyAssets", surveyAssetRepo.findAll());
        model.addAttribute("filter", filter);
        model.addAttribute("employeeMap", registryService.getEmployeeNameMap());
        model.addAttribute("managerNameMap", registryService.getManagerNameMap());
        model.addAttribute("documentUploadMaxSizeMb", documentUploadMaxSizeMb);
        model.addAttribute("documentUploadAllowedExtensions", documentUploadAllowedExtensions);
        model.addAttribute("surveyAssetDocumentUploadCategories",
                TextUtils.splitCsv(surveyAssetDocumentUploadCategoriesCsv).stream()
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList());
        model.addAttribute("surveyCatalogMap", registryService.getSurveyCatalogMap());
        model.addAttribute("surveyAssetAdminLegalStatuses", surveyAssetAdminLegalStatuses);
        model.addAttribute("surveyAssetOperationalStatuses", surveyAssetOperationalStatuses);
        model.addAttribute("surveyAssetConditionStatuses", surveyAssetConditionStatuses);
        return "survey-assets";
    }

    @PostMapping("/catalog/add")
    @CacheEvict(value = "surveyCatalogMap", allEntries = true)
    public String addSurveyCatalog(@ModelAttribute SurveyEquipmentCatalog newCatalog,
            RedirectAttributes redirectAttributes) {
        surveyCatalogRepo.save(newCatalog);
        redirectAttributes.addFlashAttribute("successMessage", "Survey equipment catalog updated.");
        return "redirect:/";
    }

    @PostMapping("/receive")
    public String receiveSurveyAsset(
            @ModelAttribute SurveyAsset baseAsset,
            @RequestParam(defaultValue = "1") int quantity,
            @RequestParam(value = "documentFiles", required = false) MultipartFile[] documentFiles,
            @RequestParam(value = "documentCategories", required = false) String[] documentCategories,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String performedBy = authentication != null ? authentication.getName() : "SYSTEM";
        List<SurveyAsset> created;

        try {
            created = surveyAssetService.receiveSurveyAssets(baseAsset, quantity, performedBy);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/";
        }

        String successMessage = "Successfully received " + created.size() + " survey asset(s) into storage.";

        if (documentService.hasFiles(documentFiles)) {
            String uploadedBy = authentication != null ? authentication.getName() : "SystemUser";
            try {
                for (SurveyAsset asset : created) {
                    documentService.uploadAndSaveDocuments(
                            documentFiles,
                            "SURVEY_ASSET",
                            String.valueOf(asset.getSurveyAssetID()),
                            documentCategories,
                            uploadedBy);
                }
                if (created.size() > 1) {
                    successMessage += " Documents attached to all created records: "
                            + created.stream().map(SurveyAsset::getAssetTag).collect(Collectors.joining(", ")) + ".";
                }
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Survey assets were saved, but document upload failed: " + e.getMessage());
                return "redirect:/";
            }
        }

        redirectAttributes.addFlashAttribute("successMessage", successMessage);
        return "redirect:/";
    }

    @PostMapping("/assign")
    public String assignSurveyAsset(@RequestParam Integer surveyAssetID, @RequestParam String employeeID,
            @RequestParam(required = false) String conditionNotes, RedirectAttributes redirectAttributes) {
        try {
            surveyAssetService.assignCustodian(surveyAssetID, employeeID, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Success! Survey asset assigned.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/survey-assets";
    }

    @PostMapping("/{surveyAssetID}/under-calibration")
    public String markUnderCalibration(@PathVariable Integer surveyAssetID,
            @RequestParam(required = false) String conditionNotes,
            RedirectAttributes redirectAttributes) {
        try {
            surveyAssetService.markUnderCalibration(surveyAssetID, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Survey asset marked as needing calibration.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/survey-assets";
    }

    @PostMapping("/{surveyAssetID}/under-repair")
    public String markUnderRepair(@PathVariable Integer surveyAssetID,
            @RequestParam(required = false) String conditionNotes,
            RedirectAttributes redirectAttributes) {
        try {
            surveyAssetService.markUnderRepair(surveyAssetID, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Survey asset marked under repair.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/survey-assets";
    }

    @PostMapping("/{surveyAssetID}/ber")
    public String markBeyondEconomicRepair(@PathVariable Integer surveyAssetID,
            @RequestParam(required = false) String conditionNotes,
            RedirectAttributes redirectAttributes) {
        try {
            surveyAssetService.markBeyondEconomicRepair(surveyAssetID, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Survey asset marked beyond economic repair.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/survey-assets";
    }

    @PostMapping("/{surveyAssetID}/stolen")
    public String markStolen(@PathVariable Integer surveyAssetID,
            @RequestParam(required = false) String conditionNotes,
            RedirectAttributes redirectAttributes) {
        try {
            surveyAssetService.markStolen(surveyAssetID, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Survey asset marked stolen.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/survey-assets";
    }

    @PostMapping("/{surveyAssetID}/missing")
    public String markMissing(@PathVariable Integer surveyAssetID,
            @RequestParam(required = false) String conditionNotes,
            RedirectAttributes redirectAttributes) {
        try {
            surveyAssetService.markMissing(surveyAssetID, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Survey asset marked missing.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/survey-assets";
    }

    @PostMapping("/return")
    public String returnSurveyAsset(@RequestParam Integer surveyAssetID,
            @RequestParam(required = false) String conditionNotes,
            RedirectAttributes redirectAttributes) {
        try {
            surveyAssetService.returnAsset(surveyAssetID, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Survey asset returned to storage.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/survey-assets";
    }

    @PostMapping("/retire")
    public String retireSurveyAsset(@RequestParam Integer surveyAssetID,
            @RequestParam(required = false) String conditionNotes,
            RedirectAttributes redirectAttributes) {
        try {
            surveyAssetService.retire(surveyAssetID, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Survey asset retired.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/survey-assets";
    }

    @GetMapping("/{id}/history")
    @ResponseBody
    public ResponseEntity<List<AssetHistoryService.AssetHistoryEntry>> getSurveyAssetHistory(@PathVariable Integer id) {
        return surveyAssetRepo.findById(id)
                .map(SurveyAssetService::auditReferenceId)
                .map(assetHistoryService::getHistory)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getSurveyAssetDetails(@PathVariable Integer id) {
        try {
            // Read-only: opening the detail panel must not change the record.
            SurveyAsset asset = surveyAssetService.findSurveyAsset(id);
            SurveyAssetService.SurveyAssetStatusFlags statusFlags = SurveyAssetService.deriveStatusFlags(asset);
            SurveyEquipmentCatalog catalog = registryService.getSurveyCatalogMap().get(asset.getCatalogID());

            String assignedCustodianId = asset.getAssignedCustodianID();
            String assignedCustodianName = registryService.resolveDisplayName(assignedCustodianId);
            String assignedCustodianManagerName = assignedCustodianId == null || assignedCustodianId.isBlank()
                    ? "N/A"
                    : registryService.getManagerNameByEmployeeId(assignedCustodianId);

            Map<String, Object> response = Map.ofEntries(
                    Map.entry("surveyAssetID", asset.getSurveyAssetID()),
                    Map.entry("catalogID", asset.getCatalogID() == null ? "" : asset.getCatalogID()),
                    Map.entry("catalogCategory", catalog == null || catalog.getCategory() == null ? "" : catalog.getCategory()),
                    Map.entry("catalogManufacturer",
                            catalog == null || catalog.getManufacturer() == null ? "" : catalog.getManufacturer()),
                    Map.entry("catalogModelName",
                            catalog == null || catalog.getModelName() == null ? "" : catalog.getModelName()),
                    Map.entry("catalogSpecifications",
                            formatSpecifications(catalog == null ? null : catalog.getSpecifications())),
                    Map.entry("assetTag", asset.getAssetTag() == null ? "" : asset.getAssetTag()),
                    Map.entry("serialNumber", asset.getSerialNumber() == null ? "" : asset.getSerialNumber()),
                    Map.entry("acquisitionDate",
                            asset.getAcquisitionDate() == null ? "" : asset.getAcquisitionDate()),
                    Map.entry("cost", asset.getCost() == null ? "" : asset.getCost()),
                    Map.entry("calibrationDueDate",
                            asset.getCalibrationDueDate() == null ? "" : asset.getCalibrationDueDate()),
                    Map.entry("lastCalibrationDate",
                            asset.getLastCalibrationDate() == null ? "" : asset.getLastCalibrationDate()),
                    Map.entry("assignedCustodianID", assignedCustodianId == null ? "" : assignedCustodianId),
                    Map.entry("assignedCustodianName", assignedCustodianName),
                    Map.entry("assignedCustodianManagerName", assignedCustodianManagerName),
                    Map.entry("adminLegalStatus",
                            asset.getAdminLegalStatus() == null ? "" : asset.getAdminLegalStatus()),
                    Map.entry("operationalStatus",
                            asset.getOperationalStatus() == null ? "" : asset.getOperationalStatus()),
                    Map.entry("conditionStatus",
                            asset.getConditionStatus() == null ? "" : asset.getConditionStatus()),
                    Map.entry("isFullyDepreciated", statusFlags.fullyDepreciated()),
                    Map.entry("isCalibrationOverdue", statusFlags.calibrationOverdue()),
                    Map.entry("remarks", asset.getRemarks() == null ? "" : asset.getRemarks()));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/update")
    public ResponseEntity<String> updateSurveyAsset(@RequestBody SurveyAsset updatedSurveyAsset,
            Authentication authentication) {
        Integer surveyAssetId = updatedSurveyAsset.getSurveyAssetID();
        if (surveyAssetId == null || !surveyAssetRepo.existsById(surveyAssetId)) {
            return ResponseEntity.notFound().build();
        }

        normalizeBlankOptionalFields(updatedSurveyAsset);

        String performedBy = authentication != null ? authentication.getName() : "SYSTEM";
        surveyAssetService.updateSurveyAssetDetails(updatedSurveyAsset, performedBy);
        return ResponseEntity.ok("Survey asset details updated successfully");
    }

    /**
     * JSON request bodies bypass the global {@code StringTrimmerEditor}, so a
     * blank optional-unique field (AssetTag, SerialNumber) must be normalized to
     * {@code null} here the same way {@code /assets/update} does — otherwise an
     * empty string collides with any other row's empty string under the unique
     * index.
     */
    private void normalizeBlankOptionalFields(SurveyAsset asset) {
        if (TextUtils.isBlank(asset.getAssetTag())) {
            asset.setAssetTag(null);
        }
        if (TextUtils.isBlank(asset.getSerialNumber())) {
            asset.setSerialNumber(null);
        }
    }

    private String formatSpecifications(String specifications) {
        if (specifications == null || specifications.isBlank()) {
            return null;
        }

        try {
            JsonNode jsonNode = jsonMapper.readTree(specifications);
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        } catch (Exception ex) {
            log.warn("Failed to pretty-print survey catalog specifications JSON, returning raw value", ex);
            return specifications;
        }
    }
}
