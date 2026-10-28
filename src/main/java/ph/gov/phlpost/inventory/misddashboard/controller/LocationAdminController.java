package ph.gov.phlpost.inventory.misddashboard.controller;

import ph.gov.phlpost.inventory.misddashboard.model.PsgcImportResult;
import ph.gov.phlpost.inventory.misddashboard.service.LocationImportService;
import ph.gov.phlpost.inventory.misddashboard.service.ReferenceDataRefresher;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/locations")
public class LocationAdminController {

    private final LocationImportService locationImportService;
    private final ReferenceDataRefresher referenceDataRefresher;

    public LocationAdminController(LocationImportService locationImportService,
            ReferenceDataRefresher referenceDataRefresher) {
        this.locationImportService = locationImportService;
        this.referenceDataRefresher = referenceDataRefresher;
    }

    @GetMapping
    public String viewLocationAdminPage() {
        return "location-admin";
    }

    /**
     * Reloads cached personnel names, departments and managers now rather than
     * at the next scheduled refresh (service/ReferenceDataRefresher). Lives on
     * this page because it is the admin's reference-data page.
     */
    @PostMapping("/refresh-reference-data")
    public String refreshReferenceData(RedirectAttributes redirectAttributes) {
        referenceDataRefresher.refresh();
        redirectAttributes.addFlashAttribute("successMessage",
                "Personnel reference data reloaded. Pages now show current names and managers.");
        return "redirect:/admin/locations";
    }

    @PostMapping("/import-psgc")
    public String importPsgcSingleCsv(
            @RequestParam("psgcFile") MultipartFile psgcFile,
            RedirectAttributes redirectAttributes) {
        if (psgcFile == null || psgcFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a PSGC CSV file.");
            return "redirect:/admin/locations";
        }

        try {
            PsgcImportResult result = locationImportService.importFromSinglePsgcCsv(psgcFile);
            redirectAttributes.addFlashAttribute("successMessage",
                    "PSGC import completed. Provinces: " + result.provinces()
                            + ", Cities/Municipalities: " + result.citiesMunicipalities()
                            + ", Barangays: " + result.barangays() + ".");
            redirectAttributes.addFlashAttribute("psgcImported", true);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "PSGC import failed: " + ex.getMessage());
        }

        return "redirect:/admin/locations";
    }
}
