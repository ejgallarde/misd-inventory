package ph.gov.phlpost.inventory.misddashboard.controller;

import ph.gov.phlpost.inventory.misddashboard.model.RealEstateProperty;
import ph.gov.phlpost.inventory.misddashboard.repository.RealEstatePropertyRepository;
import ph.gov.phlpost.inventory.misddashboard.service.AuditLogService;
import ph.gov.phlpost.inventory.misddashboard.service.RegistryService;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/properties")
public class PropertiesController {

    private final RealEstatePropertyRepository propertyRepo;
    private final RegistryService registryService;
    private final AuditLogService auditService;

    public PropertiesController(RealEstatePropertyRepository propertyRepo, RegistryService registryService,
            AuditLogService auditService) {
        this.propertyRepo = propertyRepo;
        this.registryService = registryService;
        this.auditService = auditService;
    }

    @GetMapping
    public String viewAllProperties(Model model) {
        model.addAttribute("allProperties", propertyRepo.findAll());
        model.addAttribute("employeeMap", registryService.getEmployeeNameMap());
        return "properties";
    }

    @PostMapping("/add")
    public String addProperty(@ModelAttribute RealEstateProperty newProperty, RedirectAttributes redirectAttributes) {
        propertyRepo.save(newProperty);
        redirectAttributes.addFlashAttribute("successMessage", "Property added to registry.");
        return "redirect:/";
    }

    @PostMapping("/assign-custodian")
    @Transactional
    public String assignCustodian(@RequestParam Integer propertyID, @RequestParam String employeeID,
            @RequestParam String conditionNotes, RedirectAttributes redirectAttributes) {
        RealEstateProperty property = propertyRepo.findById(propertyID).orElse(null);
        if (property != null) {
            property.setCustodianID(employeeID);
            propertyRepo.save(property);
            String refId = property.getTitleNumber() != null ? property.getTitleNumber() : "PROP-" + propertyID;
            auditService.logAssignment(refId, employeeID, "Custodian Assigned", conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Custodian assigned.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Property not found.");
        }
        return "redirect:/properties";
    }

    @PostMapping("/update-tax")
    @Transactional
    public String updatePropertyTax(@RequestParam Integer propertyID, @RequestParam String taxStatus,
            @RequestParam String conditionNotes, RedirectAttributes redirectAttributes) {
        RealEstateProperty property = propertyRepo.findById(propertyID).orElse(null);
        if (property != null) {
            property.setPropertyTaxStatus(taxStatus);
            propertyRepo.save(property);
            String refId = property.getTitleNumber() != null ? property.getTitleNumber() : "PROP-" + propertyID;
            auditService.logLifecycleEvent(refId, "FINANCE", "Tax Status Updated: " + taxStatus, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Tax status updated.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Property not found.");
        }
        return "redirect:/properties";
    }

    @GetMapping("/properties/{id}")
    public ResponseEntity<RealEstateProperty> getPropertyDetails(@PathVariable Integer id) {
        return propertyRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/properties/update")
    public ResponseEntity<String> updateProperty(@RequestBody RealEstateProperty updatedProp) {
        propertyRepo.save(updatedProp);
        return ResponseEntity.ok("Property updated successfully");
    }
}