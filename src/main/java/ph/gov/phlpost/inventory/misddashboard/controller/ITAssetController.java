package ph.gov.phlpost.inventory.misddashboard.controller;

import ph.gov.phlpost.inventory.misddashboard.model.Asset;
import ph.gov.phlpost.inventory.misddashboard.model.EquipmentCatalog;
import ph.gov.phlpost.inventory.misddashboard.repository.AssetRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.EquipmentCatalogRepository;
import ph.gov.phlpost.inventory.misddashboard.service.ITAssetService;
import ph.gov.phlpost.inventory.misddashboard.service.RegistryService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ITAssetController {

    private final AssetRepository assetRepo;
    private final EquipmentCatalogRepository catalogRepo;
    private final ITAssetService assetService;
    private final RegistryService registryService;

    public ITAssetController(AssetRepository assetRepo,
            EquipmentCatalogRepository catalogRepo, ITAssetService assetService,
            RegistryService registryService) {
        this.assetRepo = assetRepo;
        this.catalogRepo = catalogRepo;
        this.assetService = assetService;
        this.registryService = registryService;
    }

    @GetMapping("/assets")
    public String viewAllAssets(Model model) {
        model.addAttribute("allAssets", assetRepo.findAll());
        model.addAttribute("employeeMap", registryService.getEmployeeNameMap());
        model.addAttribute("catalogMap", registryService.getCatalogMap());
        model.addAttribute("departmentMap", registryService.getDepartmentMap());
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
            RedirectAttributes redirectAttributes) {

        // 1. Setup the Date Prefix (e.g., PPC-2026-07-15-)
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String datePrefix = "PPC-" + today.format(formatter) + "-";

        // 2. Loop through the quantity and save each asset
        for (int i = 0; i < quantity; i++) {
            Asset newAsset = new Asset();

            // Copy standard fields
            newAsset.setCatalogID(baseAsset.getCatalogID());
            newAsset.setPurchaseDate(baseAsset.getPurchaseDate());
            newAsset.setPurchasePrice(baseAsset.getPurchasePrice());
            newAsset.setRemarks(baseAsset.getRemarks());

            // Apply Business Rules
            newAsset.setCurrentStatus("In Storage");
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
            @RequestParam String conditionNotes, RedirectAttributes redirectAttributes) {
        try {
            assetService.assignAsset(assetTag, employeeID, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Asset assigned successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/assets";
    }

    @PostMapping("/assets/return")
    public String returnAsset(@RequestParam String assetTag, @RequestParam String conditionNotes,
            RedirectAttributes redirectAttributes) {
        try {
            assetService.returnAsset(assetTag, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Asset returned to MISD.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/assets";
    }

    @PostMapping("/assets/unserviceable")
    public String markUnserviceable(@RequestParam String assetTag, @RequestParam String conditionNotes,
            RedirectAttributes redirectAttributes) {
        try {
            assetService.updateLifecycle(assetTag, "Unserviceable", "Marked Unserviceable", conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Asset marked unserviceable.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/assets";
    }

    @PostMapping("/assets/warranty")
    public String markForWarranty(@RequestParam String assetTag, @RequestParam String conditionNotes,
            RedirectAttributes redirectAttributes) {
        try {
            assetService.updateLifecycle(assetTag, "In Warranty Repair", "Sent for Warranty Repair", conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Asset flagged for warranty.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/assets";
    }

    @PostMapping("/assets/retire")
    public String retireAsset(@RequestParam String assetTag, @RequestParam String conditionNotes,
            RedirectAttributes redirectAttributes) {
        try {
            assetService.updateLifecycle(assetTag, "Retired", "Asset Retired", conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Asset retired.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/assets";
    }

    @GetMapping("/assets/{id}")
    public ResponseEntity<Asset> getITAssetDetails(@PathVariable String id) {
        return assetRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/assets/update")
    public ResponseEntity<String> updateITAsset(@RequestBody Asset updatedAsset) {
        assetRepo.save(updatedAsset);
        return ResponseEntity.ok("Asset updated successfully");
    }
}