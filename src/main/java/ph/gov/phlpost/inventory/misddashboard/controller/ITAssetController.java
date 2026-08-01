package ph.gov.phlpost.inventory.misddashboard.controller;

import ph.gov.phlpost.inventory.misddashboard.model.Asset;
import ph.gov.phlpost.inventory.misddashboard.model.EquipmentCatalog;
import ph.gov.phlpost.inventory.misddashboard.model.Personnel;
import ph.gov.phlpost.inventory.misddashboard.repository.AssetRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.EquipmentCatalogRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.PersonnelRepository;
import ph.gov.phlpost.inventory.misddashboard.service.DocumentService;
import ph.gov.phlpost.inventory.misddashboard.service.ITAssetService;
import ph.gov.phlpost.inventory.misddashboard.service.RegistryService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
public class ITAssetController {

    private final AssetRepository assetRepo;
    private final EquipmentCatalogRepository catalogRepo;
    private final PersonnelRepository personnelRepo;
    private final ITAssetService assetService;
    private final RegistryService registryService;
    private final DocumentService documentService;
    private final ObjectMapper objectMapper;

    @Value("${document.upload.max-size-mb:15}")
    private int documentUploadMaxSizeMb;

    @Value("${document.upload.allowed-extensions:pdf,jpg,jpeg,png,doc,docx,xls,xlsx}")
    private String documentUploadAllowedExtensions;

    @Value("#{'${document.upload.categories}'.split(',')}")
    private List<String> documentUploadCategories;

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
            PersonnelRepository personnelRepo,
            ObjectMapper objectMapper) {
        this.assetRepo = assetRepo;
        this.catalogRepo = catalogRepo;
        this.assetService = assetService;
        this.registryService = registryService;
        this.documentService = documentService;
        this.personnelRepo = personnelRepo;
        this.objectMapper = objectMapper;
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
        model.addAttribute("documentUploadMaxSizeMb", documentUploadMaxSizeMb);
        model.addAttribute("documentUploadAllowedExtensions", documentUploadAllowedExtensions);
        model.addAttribute("documentUploadCategories", documentUploadCategories.stream()
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

        // 1. Setup the Date Prefix (e.g., PPC-2026-07-15-)
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String datePrefix = "PPC-" + today.format(formatter) + "-";

        // 2. Loop through the quantity and save each asset
        List<String> createdAssetTags = new ArrayList<>();

        for (int i = 0; i < quantity; i++) {
            Asset newAsset = new Asset();

            // Copy standard fields
            newAsset.setCatalogID(baseAsset.getCatalogID());
            newAsset.setPurchaseDate(baseAsset.getPurchaseDate());
            newAsset.setPurchasePrice(baseAsset.getPurchasePrice());
            newAsset.setRemarks(baseAsset.getRemarks());

            // Apply Business Rules for initial receipt
            newAsset.setDeploymentStatus("In Storage");
            newAsset.setMaintenanceHealthStatus("Operational");
            newAsset.setLifecycleStatus("Procured / Pre-Deployment");
            newAsset.setCurrentOwnerID(null);

            // Handle Tag and Serial Logic
            if (quantity == 1) {
                newAsset.setSerialNumber(baseAsset.getSerialNumber());
                // Use manual tag if provided, otherwise auto-generate
                if (baseAsset.getAssetTag() != null && !baseAsset.getAssetTag().trim().isEmpty()) {
                    newAsset.setAssetTag(baseAsset.getAssetTag().trim());
                } else {
                    newAsset.setAssetTag(generateNextAssetTag(datePrefix));
                }
            } else {
                // Bulk Rules: Disable serial number, force auto-generation
                newAsset.setSerialNumber(null);
                newAsset.setAssetTag(generateNextAssetTag(datePrefix));
            }

            assetRepo.save(newAsset);
            createdAssetTags.add(newAsset.getAssetTag());
        }

        // 3. Attach documents to ALL created assets
        if (documentService.hasFiles(documentFiles) && !createdAssetTags.isEmpty()) {
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

                if (quantity > 1) {
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Successfully received " + quantity
                                    + " asset(s) into storage. Documents attached to all asset tags: "
                                    + String.join(", ", createdAssetTags) + ".");
                    return "redirect:/";
                }
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Assets were saved, but document upload failed: " + e.getMessage());
                return "redirect:/";
            }
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "Successfully received " + quantity + " asset(s) into storage.");
        return "redirect:/"; // Update this to redirect to your assets page if preferred
    }

    /**
     * Helper method to query the database for today's highest tag and increment by
     * 1.
     */
    private synchronized String generateNextAssetTag(String datePrefix) {
        Optional<Asset> lastAsset = assetRepo.findTopByAssetTagStartingWithOrderByAssetTagDesc(datePrefix);

        if (lastAsset.isEmpty() || lastAsset.get().getAssetTag() == null) {
            return datePrefix + "00001"; // First asset of the day
        }

        try {
            // Extract the last 5 digits and increment
            String lastTag = lastAsset.get().getAssetTag();
            String sequenceStr = lastTag.substring(datePrefix.length());
            int sequence = Integer.parseInt(sequenceStr);
            return datePrefix + String.format("%05d", sequence + 1);
        } catch (Exception e) {
            // Fallback safety net in case of a corrupted tag
            return datePrefix + "99999";
        }
    }

    @PostMapping("/assets/assign")
    public String assignAsset(@RequestParam String assetTag, @RequestParam String employeeID,
            @RequestParam String conditionNotes, @RequestParam(value = "search", required = false) String search,
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
    public String returnAsset(@RequestParam String assetTag, @RequestParam String conditionNotes,
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
    public String markUnserviceable(@RequestParam String assetTag, @RequestParam String conditionNotes,
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
    public String markForWarranty(@RequestParam String assetTag, @RequestParam String conditionNotes,
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
            @RequestParam String conditionNotes, @RequestParam(value = "search", required = false) String search,
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
    public String markAssetRepaired(@RequestParam String assetTag, @RequestParam String conditionNotes,
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
    public String retireAsset(@RequestParam String assetTag, @RequestParam String conditionNotes,
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

    @PostMapping("/assets/update")
    public ResponseEntity<String> updateITAsset(@RequestBody Asset updatedAsset) {
        applyStatusTransitions(updatedAsset);
        assetRepo.save(updatedAsset);
        return ResponseEntity.ok("Asset updated successfully");
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
        String deployment = normalizeBlank(asset.getDeploymentStatus());
        String maintenance = normalizeBlank(asset.getMaintenanceHealthStatus());
        String lifecycle = normalizeBlank(asset.getLifecycleStatus());

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
            if (deployment.isBlank() || "Deployed".equals(deployment) || "On Loan".equals(deployment)) {
                asset.setDeploymentStatus("In Storage");
                deployment = "In Storage";
            }
        }

        // Active assignment/repair states promote lifecycle out of pre-deployment.
        if (("Deployed".equals(deployment) || "On Loan".equals(deployment) || "Under Repair".equals(maintenance))
                && (lifecycle.isBlank() || "Procured / Pre-Deployment".equals(lifecycle))) {
            asset.setLifecycleStatus("Active");
        }
    }

    private String buildFullName(Personnel personnel) {
        String lastName = normalizeBlank(personnel.getLastName());
        String firstName = normalizeBlank(personnel.getFirstName());

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

    private String normalizeBlank(String value) {
        return value == null ? "" : value.trim();
    }

    private String resolveManagerId(Personnel personnel) {
        if (personnel == null) {
            return null;
        }

        String managerId = normalizeBlank(personnel.getManagerID());
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
            JsonNode jsonNode = objectMapper.readTree(specifications);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        } catch (Exception ex) {
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