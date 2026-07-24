package ph.gov.phlpost.inventory.misddashboard.controller;

import ph.gov.phlpost.inventory.misddashboard.model.FleetVehicle;
import ph.gov.phlpost.inventory.misddashboard.repository.FleetVehicleRepository;
import ph.gov.phlpost.inventory.misddashboard.service.DocumentService;
import ph.gov.phlpost.inventory.misddashboard.service.FleetService;
import ph.gov.phlpost.inventory.misddashboard.service.RegistryService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/fleet")
public class FleetController {

    private final FleetVehicleRepository fleetRepo;
    private final FleetService fleetService;
    private final RegistryService registryService;
    private final DocumentService documentService;

    public FleetController(FleetVehicleRepository fleetRepo, FleetService fleetService,
            RegistryService registryService,
            DocumentService documentService) {
        this.fleetRepo = fleetRepo;
        this.fleetService = fleetService;
        this.registryService = registryService;
        this.documentService = documentService;
    }

    @GetMapping
    public String viewAllFleet(Model model) {
        model.addAttribute("allVehicles", fleetRepo.findAll());
        model.addAttribute("employeeMap", registryService.getEmployeeNameMap());
        return "fleet";
    }

    @PostMapping("/add")
    public String registerVehicle(@ModelAttribute FleetVehicle newVehicle,
            @RequestParam(value = "documentFiles", required = false) MultipartFile[] documentFiles,
            @RequestParam(value = "documentCategory", required = false) String documentCategory,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        fleetRepo.save(newVehicle);

        if (documentService.hasFiles(documentFiles) && newVehicle.getVehicleID() != null) {
            String uploadedBy = authentication != null ? authentication.getName() : "SystemUser";
            try {
                documentService.uploadAndSaveDocuments(
                        documentFiles,
                        "VEHICLE",
                        String.valueOf(newVehicle.getVehicleID()),
                        documentCategory,
                        uploadedBy);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Vehicle saved, but document upload failed: " + e.getMessage());
                return "redirect:/";
            }
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "Success! Vehicle " + newVehicle.getPlateNumber() + " registered.");
        return "redirect:/";
    }

    @PostMapping("/assign")
    public String assignVehicle(@RequestParam Integer vehicleID, @RequestParam String employeeID,
            @RequestParam String conditionNotes, RedirectAttributes redirectAttributes) {
        try {
            fleetService.assignVehicle(vehicleID, employeeID, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Success! Vehicle assigned.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/fleet";
    }

    @PostMapping("/return")
    public String returnVehicle(@RequestParam Integer vehicleID, @RequestParam String conditionNotes,
            RedirectAttributes redirectAttributes) {
        try {
            fleetService.returnVehicle(vehicleID, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle returned to motorpool.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/fleet";
    }

    @PostMapping("/retire")
    public String retireVehicle(@RequestParam Integer vehicleID, @RequestParam String conditionNotes,
            RedirectAttributes redirectAttributes) {
        try {
            fleetService.retireVehicle(vehicleID, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle retired.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/fleet";
    }

    @GetMapping("/{id}")
    public ResponseEntity<FleetVehicle> getVehicleDetails(@PathVariable Integer id) {
        return fleetRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/update")
    public ResponseEntity<String> updateVehicle(@RequestBody FleetVehicle updatedVehicle) {
        if (fleetRepo.existsById(updatedVehicle.getVehicleID())) {
            fleetRepo.save(updatedVehicle);
            return ResponseEntity.ok("Vehicle updated successfully");
        }
        return ResponseEntity.notFound().build();
    }
}