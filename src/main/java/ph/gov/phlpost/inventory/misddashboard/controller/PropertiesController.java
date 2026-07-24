package ph.gov.phlpost.inventory.misddashboard.controller;

import ph.gov.phlpost.inventory.misddashboard.model.RealEstateProperty;
import ph.gov.phlpost.inventory.misddashboard.repository.RealEstatePropertyRepository;
import ph.gov.phlpost.inventory.misddashboard.service.AuditLogService;
import ph.gov.phlpost.inventory.misddashboard.service.DocumentService;
import ph.gov.phlpost.inventory.misddashboard.service.RegistryService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/properties")
public class PropertiesController {

    private final RealEstatePropertyRepository propertyRepo;
    private final RegistryService registryService;
    private final AuditLogService auditService;
    private final DocumentService documentService;

    public PropertiesController(RealEstatePropertyRepository propertyRepo, RegistryService registryService,
            AuditLogService auditService,
            DocumentService documentService) {
        this.propertyRepo = propertyRepo;
        this.registryService = registryService;
        this.auditService = auditService;
        this.documentService = documentService;
    }

    @GetMapping
    public String viewAllProperties(Model model) {
        model.addAttribute("allProperties", propertyRepo.findAll());
        model.addAttribute("employeeMap", registryService.getEmployeeNameMap());
        return "properties";
    }

    @PostMapping("/add")
    public String addProperty(@ModelAttribute RealEstateProperty newProperty,
            @RequestParam(value = "documentFiles", required = false) MultipartFile[] documentFiles,
            @RequestParam(value = "documentCategories", required = false) String[] documentCategories,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        propertyRepo.save(newProperty);

        if (documentService.hasFiles(documentFiles) && newProperty.getPropertyID() != null) {
            String uploadedBy = authentication != null ? authentication.getName() : "SystemUser";
            try {
                documentService.uploadAndSaveDocuments(
                        documentFiles,
                        "PROPERTY",
                        String.valueOf(newProperty.getPropertyID()),
                        documentCategories,
                        uploadedBy);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Property saved, but document upload failed: " + e.getMessage());
                return "redirect:/";
            }
        }

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