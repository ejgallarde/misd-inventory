package ph.gov.phlpost.inventory.misddashboard.controller;

import ph.gov.phlpost.inventory.misddashboard.model.FleetVehicle;
import ph.gov.phlpost.inventory.misddashboard.repository.FleetVehicleRepository;
import ph.gov.phlpost.inventory.misddashboard.service.FleetService;
import ph.gov.phlpost.inventory.misddashboard.service.RegistryService;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/fleet")
public class FleetController {

    private final FleetVehicleRepository fleetRepo;
    private final FleetService fleetService;
    private final RegistryService registryService;

    public FleetController(FleetVehicleRepository fleetRepo, FleetService fleetService,
            RegistryService registryService) {
        this.fleetRepo = fleetRepo;
        this.fleetService = fleetService;
        this.registryService = registryService;
    }

    @GetMapping
    public String viewAllFleet(Model model) {
        model.addAttribute("allVehicles", fleetRepo.findAll());
        model.addAttribute("employeeMap", registryService.getEmployeeNameMap());
        return "fleet";
    }

    @PostMapping("/add")
    public String registerVehicle(@ModelAttribute FleetVehicle newVehicle, RedirectAttributes redirectAttributes) {
        fleetRepo.save(newVehicle);
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

    @GetMapping("/fleet/{id}")
    public ResponseEntity<FleetVehicle> getVehicleDetails(@PathVariable Integer id) {
        return fleetRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/fleet/update")
    public ResponseEntity<String> updateVehicle(@RequestBody FleetVehicle updatedVehicle) {
        if (fleetRepo.existsById(updatedVehicle.getVehicleID())) {
            fleetRepo.save(updatedVehicle);
            return ResponseEntity.ok("Vehicle updated successfully");
        }
        return ResponseEntity.notFound().build();
    }
}