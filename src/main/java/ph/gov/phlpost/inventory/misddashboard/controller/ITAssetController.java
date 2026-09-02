package ph.gov.phlpost.inventory.misddashboard.controller;

import ph.gov.phlpost.inventory.misddashboard.model.Asset;
import ph.gov.phlpost.inventory.misddashboard.model.EquipmentCatalog;
import ph.gov.phlpost.inventory.misddashboard.model.Personnel;
import ph.gov.phlpost.inventory.misddashboard.repository.AssetRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.EquipmentCatalogRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.PersonnelRepository;
import ph.gov.phlpost.inventory.misddashboard.service.DocumentService;
import ph.gov.phlpost.inventory.misddashboard.service.AssetHistoryService;
import ph.gov.phlpost.inventory.misddashboard.service.ITAssetService;
import ph.gov.phlpost.inventory.misddashboard.service.RegistryService;
import ph.gov.phlpost.inventory.misddashboard.util.TextUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.springframework.web.util.UriUtils;

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
public class ITAssetController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ITAssetController.class);

    private final AssetRepository assetRepo;
    private final EquipmentCatalogRepository catalogRepo;
    private final PersonnelRepository personnelRepo;
    private final ITAssetService assetService;
    private final RegistryService registryService;
    private final DocumentService documentService;
    private final AssetHistoryService assetHistoryService;
    private final JsonMapper jsonMapper;

    @Value("${document.upload.max-size-mb:15}")
    private int documentUploadMaxSizeMb;

    @Value("${document.upload.allowed-extensions:pdf,jpg,jpeg,png,doc,docx,xls,xlsx}")
    private String documentUploadAllowedExtensions;

    @Value("${document.upload.categories.it}")
    private String itDocumentUploadCategoriesCsv;

    @Value("${asset.workflow.maintenance-technician-job-title:Computer Maintenance Technologist}")
    private String maintenanceTechnicianJobTitle;

    @Value("#{'${dropdown.asset-deployment-statuses}'.split(',')}")
    private List<String> assetDeploymentStatusOptions;

    @Value("#{'${dropdown.asset-maintenance-health-statuses}'.split(',')}")
    private List<String> assetMaintenanceHealthStatusOptions;

    @Value("#{'${dropdown.asset-lifecycle-statuses}'.split(',')}")
    private List<String> assetLifecycleStatusOptions;

    public ITAssetController(AssetRepository assetRepo,
            EquipmentCatalogRepository catalogRepo, ITAssetService assetService,
            RegistryService registryService,
            DocumentService documentService,
            AssetHistoryService assetHistoryService,
            PersonnelRepository personnelRepo,
            JsonMapper jsonMapper) {
        this.assetRepo = assetRepo;
        this.catalogRepo = catalogRepo;
        this.assetService = assetService;
        this.registryService = registryService;
        this.documentService = documentService;
        this.assetHistoryService = assetHistoryService;
        this.personnelRepo = personnelRepo;
        this.jsonMapper = jsonMapper;
    }

    @GetMapping("/assets")
    public String viewAllAssets(Model model,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) String openAsset) {
        model.addAttribute("allAssets", assetRepo.findAll());
        model.addAttribute("employeeMap", registryService.getEmployeeNameMap());
        model.addAttribute("catalogMap", registryService.getCatalogMap());
        model.addAttribute("departmentMap", registryService.getDepartmentMap());
        model.addAttribute("divisionMap", registryService.getDivisionMap());
        model.addAttribute("personnelLocationMap", registryService.getPersonnelLocationMap());
        model.addAttribute("managerNameMap", registryService.getManagerNameMap());
        model.addAttribute("documentUploadMaxSizeMb", documentUploadMaxSizeMb);
        model.addAttribute("documentUploadAllowedExtensions", documentUploadAllowedExtensions);
        model.addAttribute("itDocumentUploadCategories", TextUtils.splitCsv(itDocumentUploadCategoriesCsv).stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList());
        model.addAttribute("maintenanceTechnicianJobTitle", maintenanceTechnicianJobTitle);
        model.addAttribute("assetDeploymentStatusOptions", assetDeploymentStatusOptions);
        model.addAttribute("assetMaintenanceHealthStatusOptions", assetMaintenanceHealthStatusOptions);
        model.addAttribute("assetLifecycleStatusOptions", assetLifecycleStatusOptions);
        model.addAttribute("filter", filter);
        model.addAttribute("openAsset", openAsset);
        return "assets";
    }

    @PostMapping("/catalog/add")
    @CacheEvict(value = "catalogMap", allEntries = true)
    public String addCatalog(@ModelAttribute EquipmentCatalog newCatalog, RedirectAttributes redirectAttributes) {
        catalogRepo.save(newCatalog);
        redirectAttributes.addFlashAttribute("successMessage", "Catalog updated.");
        return "redirect:/";
    }

    @PostMapping("/assets/receive")
    public String receiveAsset(
            @ModelAttribute Asset baseAsset,
            @RequestParam(defaultValue = "1") int quantity,
            @RequestParam(value = "documentFiles", required = false) MultipartFile[] documentFiles,
            @RequestParam(value = "documentCategories", required = false) String[] documentCategories,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String performedBy = authentication != null ? authentication.getName() : "SYSTEM";
        List<String> createdAssetTags;

        // Creation and its audit entries are one transaction in the service, so a
        // failure part-way through a batch leaves no partial receipt behind.
        try {
            createdAssetTags = assetService.receiveAssets(baseAsset, quantity, performedBy);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/";
        }

        String successMessage = "Successfully received " + createdAssetTags.size() + " asset(s) into storage.";

        // Document storage is deliberately outside that transaction: it writes to
        // the filesystem or MinIO, which a rollback could not undo anyway.
        if (documentService.hasFiles(documentFiles)) {
            String uploadedBy = authentication != null ? authentication.getName() : "SystemUser";
            try {
                for (String assetTag : createdAssetTags) {
                    documentService.uploadAndSaveDocuments(
                            documentFiles,
                            "IT_EQUIPMENT",
                            assetTag,
                            documentCategories,
                            uploadedBy);
                }
                if (createdAssetTags.size() > 1) {
                    successMessage += " Documents attached to all asset tags: "
                            + String.join(", ", createdAssetTags) + ".";
                }
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Assets were saved, but document upload failed: " + e.getMessage());
                return "redirect:/";
            }
        }

        redirectAttributes.addFlashAttribute("successMessage", successMessage);
        return "redirect:/";
    }

    @PostMapping("/assets/assign")
    public String assignAsset(@RequestParam String assetTag, @RequestParam String employeeID,
            @RequestParam(required = false) String conditionNotes, @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", required = false) Integer page, RedirectAttributes redirectAttributes) {
        try {
            assetService.assignAsset(assetTag, employeeID, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Asset assigned successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + buildAssetsRedirectUrl(search, page);
    }

    @PostMapping("/assets/return")
    public String returnAsset(@RequestParam String assetTag, @RequestParam(required = false) String conditionNotes,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", required = false) Integer page, RedirectAttributes redirectAttributes) {
        try {
            assetService.returnAsset(assetTag, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Asset returned to MISD.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + buildAssetsRedirectUrl(search, page);
    }

    @PostMapping("/assets/unserviceable")
    public String markUnserviceable(@RequestParam String assetTag, @RequestParam(required = false) String conditionNotes,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", required = false) Integer page, RedirectAttributes redirectAttributes) {
        try {
            assetService.updateLifecycle(assetTag, "Unserviceable", "Marked Unserviceable", conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Asset marked unserviceable.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + buildAssetsRedirectUrl(search, page);
    }

    @PostMapping("/assets/warranty")
    public String markForWarranty(@RequestParam String assetTag, @RequestParam(required = false) String conditionNotes,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", required = false) Integer page, RedirectAttributes redirectAttributes) {
        try {
            assetService.sendForWarranty(assetTag, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Asset flagged for warranty.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + buildAssetsRedirectUrl(search, page);
    }

    @PostMapping("/assets/misd-maintenance")
    public String sendForMisdMaintenance(@RequestParam String assetTag, @RequestParam String technicianID,
            @RequestParam(required = false) String conditionNotes, @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", required = false) Integer page, RedirectAttributes redirectAttributes) {
        try {
            assetService.sendForMisdMaintenance(assetTag, technicianID, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Asset sent for MISD maintenance.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + buildAssetsRedirectUrl(search, page);
    }

    @PostMapping("/assets/repaired")
    public String markAssetRepaired(@RequestParam String assetTag, @RequestParam(required = false) String conditionNotes,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", required = false) Integer page, RedirectAttributes redirectAttributes) {
        try {
            assetService.markRepaired(assetTag, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Asset marked as repaired and returned to storage.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + buildAssetsRedirectUrl(search, page);
    }

    @PostMapping("/assets/retire")
    public String retireAsset(@RequestParam String assetTag, @RequestParam(required = false) String conditionNotes,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", required = false) Integer page, RedirectAttributes redirectAttributes) {
        try {
            assetService.updateLifecycle(assetTag, "Retired", "Asset Retired", conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Asset retired.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + buildAssetsRedirectUrl(search, page);
    }

    private String buildAssetsRedirectUrl(String search, Integer page) {
        StringBuilder redirectUrl = new StringBuilder("/assets");
        boolean hasQuery = false;

        if (search != null && !search.isBlank()) {
            redirectUrl.append("?search=").append(UriUtils.encode(search, StandardCharsets.UTF_8));
            hasQuery = true;
        }

        if (page != null) {
            redirectUrl.append(hasQuery ? '&' : '?').append("page=").append(page);
        }

        return redirectUrl.toString();
    }

    @GetMapping("/assets/{id}")
    public ResponseEntity<AssetDetailResponse> getITAssetDetails(@PathVariable String id) {
        return assetRepo.findById(id)
                .map(this::toAssetDetailResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/assets/{assetTag}/history")
    @ResponseBody
    public ResponseEntity<List<AssetHistoryService.AssetHistoryEntry>> getAssetHistory(
            @PathVariable String assetTag) {
        if (!assetRepo.existsById(assetTag)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(assetHistoryService.getHistory(assetTag));
    }

    @PostMapping("/assets/update")
    public ResponseEntity<String> updateITAsset(@RequestBody Asset updatedAsset, Authentication authentication) {
        normalizeBlankOptionalFields(updatedAsset);
        applyStatusTransitions(updatedAsset);
        String performedBy = authentication != null ? authentication.getName() : "SYSTEM";
        assetService.updateAsset(updatedAsset, performedBy);
        return ResponseEntity.ok("Asset updated successfully");
    }

    /**
     * The detail slideout serializes its whole form, so cleared fields arrive as
     * empty strings rather than nulls — and Jackson bypasses the global
     * {@code StringTrimmerEditor} that handles this for form posts.
     *
     * <p>
     * Both columns below reject an empty string: SerialNumber is UNIQUE (so the
     * second blank one collides) and CurrentOwnerID is a foreign key to
     * Personnel (which has no employee with an empty ID). Leaving them as ""
     * failed the save and discarded the user's entire edit.
     */
    private void normalizeBlankOptionalFields(Asset asset) {
        if (TextUtils.normalizeBlank(asset.getSerialNumber()).isEmpty()) {
            asset.setSerialNumber(null);
        }
        if (TextUtils.normalizeBlank(asset.getCurrentOwnerID()).isEmpty()) {
            asset.setCurrentOwnerID(null);
        }
        if (TextUtils.normalizeBlank(asset.getRemarks()).isEmpty()) {
            asset.setRemarks(null);
        }
    }

    private AssetDetailResponse toAssetDetailResponse(Asset asset) {
        EquipmentCatalog catalog = registryService.getCatalogMap().get(asset.getCatalogID());
        String ownerId = asset.getCurrentOwnerID();
        Personnel personnel = ownerId == null ? null : personnelRepo.findById(ownerId).orElse(null);

        return new AssetDetailResponse(
                asset.getAssetTag(),
                asset.getCatalogID(),
                asset.getSerialNumber(),
                asset.getPurchaseDate(),
                asset.getPurchasePrice(),
                asset.getCurrentOwnerID(),
                asset.getDeploymentStatus(),
                asset.getMaintenanceHealthStatus(),
                asset.getLifecycleStatus(),
                asset.getRemarks(),
                catalog == null ? null : catalog.getCategory(),
                catalog == null ? null : catalog.getManufacturer(),
                catalog == null ? null : catalog.getModelName(),
                formatSpecifications(catalog == null ? null : catalog.getSpecifications()),
                ownerId,
                ownerId == null ? null : registryService.getEmployeeNameMap().get(ownerId),
                ownerId == null ? null : registryService.getDepartmentMap().get(ownerId),
                ownerId == null ? null : registryService.getDivisionMap().get(ownerId),
                resolveManagerId(personnel),
                resolveManagerFullName(personnel));
    }

    private void applyStatusTransitions(Asset asset) {
        String deployment = TextUtils.normalizeBlank(asset.getDeploymentStatus());
        String maintenance = TextUtils.normalizeBlank(asset.getMaintenanceHealthStatus());
        String lifecycle = TextUtils.normalizeBlank(asset.getLifecycleStatus());

        // BER always implies end of life.
        if ("Beyond Economic Repair (BER)".equals(maintenance)) {
            lifecycle = "End of Life (EOL)";
            asset.setLifecycleStatus(lifecycle);
        }

        // End states should not remain assigned/deployed.
        if ("Decommissioned / Retired".equals(lifecycle)
                || "Disposed".equals(lifecycle)
                || "Sold".equals(lifecycle)) {
            asset.setCurrentOwnerID(null);
            if (deployment.isBlank() || "Deployed".equals(deployment)) {
                asset.setDeploymentStatus("In Storage");
                deployment = "In Storage";
            }
        }

        // Active assignment/repair states promote lifecycle out of pre-deployment.
        if (("Deployed".equals(deployment) || "Under Repair".equals(maintenance))
                && (lifecycle.isBlank() || "Procured / Pre-Deployment".equals(lifecycle))) {
            asset.setLifecycleStatus("Active");
        }
    }

    private String buildFullName(Personnel personnel) {
        String lastName = TextUtils.normalizeBlank(personnel.getLastName());
        String firstName = TextUtils.normalizeBlank(personnel.getFirstName());

        if (lastName.isBlank() && firstName.isBlank()) {
            return null;
        }

        if (lastName.isBlank()) {
            return firstName;
        }

        if (firstName.isBlank()) {
            return lastName;
        }

        return firstName + " " + lastName;
    }

    private String resolveManagerId(Personnel personnel) {
        if (personnel == null) {
            return null;
        }

        String managerId = TextUtils.normalizeBlank(personnel.getManagerID());
        return managerId.isBlank() ? null : managerId;
    }

    private String resolveManagerFullName(Personnel personnel) {
        String managerId = resolveManagerId(personnel);
        if (managerId == null) {
            return null;
        }

        Personnel manager = personnelRepo.findById(managerId).orElse(null);
        if (manager == null) {
            return managerId;
        }

        String managerName = buildFullName(manager);
        return managerName == null || managerName.isBlank() ? managerId : managerName;
    }

    private String formatSpecifications(String specifications) {
        if (specifications == null || specifications.isBlank()) {
            return null;
        }

        try {
            JsonNode jsonNode = jsonMapper.readTree(specifications);
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        } catch (Exception ex) {
            log.warn("Failed to pretty-print catalog specifications JSON, returning raw value", ex);
            return specifications;
        }
    }

    private record AssetDetailResponse(
            String assetTag,
            Integer catalogID,
            String serialNumber,
            LocalDate purchaseDate,
            java.math.BigDecimal purchasePrice,
            String currentOwnerID,
            String deploymentStatus,
            String maintenanceHealthStatus,
            String lifecycleStatus,
            String remarks,
            String catalogCategory,
            String catalogManufacturer,
            String catalogModelName,
            String catalogSpecifications,
            String assigneeEmployeeID,
            String assigneeFullName,
            String assigneeDepartment,
            String assigneeDivision,
            String assigneeManagerID,
            String assigneeManagerFullName) {
    }
}