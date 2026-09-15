package ph.gov.phlpost.inventory.misddashboard.controller;

import ph.gov.phlpost.inventory.misddashboard.service.DepreciationService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/depreciation")
public class DepreciationController {

    private final DepreciationService depreciationService;

    public DepreciationController(DepreciationService depreciationService) {
        this.depreciationService = depreciationService;
    }

    @GetMapping
    public String viewDepreciationAdminPage() {
        return "depreciation-admin";
    }

    @PostMapping("/recompute")
    public String recompute(RedirectAttributes redirectAttributes) {
        try {
            DepreciationService.RecomputeSummary summary = depreciationService.recomputeAll();
            redirectAttributes.addFlashAttribute("successMessage",
                    "Depreciation recomputed. Assets: " + summary.assetsUpdated()
                            + ", Fleet vehicles: " + summary.fleetVehiclesUpdated()
                            + ", Survey assets: " + summary.surveyAssetsUpdated()
                            + ". Rows without a cost or acquisition year were left unchanged.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Recompute failed: " + ex.getMessage());
        }
        return "redirect:/admin/depreciation";
    }
}
