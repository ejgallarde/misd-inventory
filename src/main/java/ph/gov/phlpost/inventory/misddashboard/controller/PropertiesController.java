package ph.gov.phlpost.inventory.misddashboard.controller;

import ph.gov.phlpost.inventory.misddashboard.model.RealEstateProperty;
import ph.gov.phlpost.inventory.misddashboard.repository.RealEstatePropertyRepository;
import ph.gov.phlpost.inventory.misddashboard.service.AssetHistoryService;
import ph.gov.phlpost.inventory.misddashboard.service.AuditLogService;
import ph.gov.phlpost.inventory.misddashboard.service.DocumentService;
import ph.gov.phlpost.inventory.misddashboard.service.RegistryService;

import java.util.List;
import java.util.Map;
import java.util.Comparator;

import org.springframework.beans.factory.annotation.Value;
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
    private final AssetHistoryService assetHistoryService;

    @Value("${document.upload.max-size-mb:15}")
    private int documentUploadMaxSizeMb;

    @Value("${document.upload.allowed-extensions:pdf,jpg,jpeg,png,doc,docx,xls,xlsx}")
    private String documentUploadAllowedExtensions;

    @Value("#{'${document.upload.categories}'.split(',')}")
    private List<String> documentUploadCategories;

    @Value("#{'${dropdown.property-tax-status-update}'.split(',')}")
    private List<String> propertyTaxStatusesUpdate;

    @Value("#{'${dropdown.property-legal-titling-statuses}'.split(',')}")
    private List<String> propertyLegalTitlingStatuses;

    @Value("#{'${dropdown.property-operational-statuses}'.split(',')}")
    private List<String> propertyOperationalStatuses;

    @Value("#{'${dropdown.property-condition-statuses}'.split(',')}")
    private List<String> propertyConditionStatuses;

    public PropertiesController(RealEstatePropertyRepository propertyRepo, RegistryService registryService,
            AuditLogService auditService,
            DocumentService documentService,
            AssetHistoryService assetHistoryService) {
        this.propertyRepo = propertyRepo;
        this.registryService = registryService;
        this.auditService = auditService;
        this.documentService = documentService;
        this.assetHistoryService = assetHistoryService;
    }

    @GetMapping
    public String viewAllProperties(Model model) {
        List<RealEstateProperty> allProperties = propertyRepo.findAll().stream()
                .sorted(Comparator.comparing(
                        property -> normalize(property.getPropertyName()),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<RealEstateProperty> landProperties = allProperties.stream()
                .filter(property -> isLotType(property.getPropertyType()))
                .toList();

        List<RealEstateProperty> buildingFacilityProperties = allProperties.stream()
                .filter(property -> !isLotType(property.getPropertyType()))
                .toList();

        model.addAttribute("allProperties", allProperties);
        model.addAttribute("landProperties", landProperties);
        model.addAttribute("buildingFacilityProperties", buildingFacilityProperties);
        model.addAttribute("employeeMap", registryService.getEmployeeNameMap());
        model.addAttribute("documentUploadMaxSizeMb", documentUploadMaxSizeMb);
        model.addAttribute("documentUploadAllowedExtensions", documentUploadAllowedExtensions);
        model.addAttribute("documentUploadCategories", documentUploadCategories.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList());
        model.addAttribute("propertyTaxStatusesUpdate", propertyTaxStatusesUpdate.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList());
        model.addAttribute("propertyLegalTitlingStatuses", propertyLegalTitlingStatuses);
        model.addAttribute("propertyOperationalStatuses", propertyOperationalStatuses);
        model.addAttribute("propertyConditionStatuses", propertyConditionStatuses);
        return "properties";
    }

    @PostMapping("/add")
    public String addProperty(@ModelAttribute RealEstateProperty newProperty,
            @RequestParam(value = "titleNotAvailable", defaultValue = "false") boolean titleNotAvailable,
            @RequestParam(value = "propertyRegistrationContext", defaultValue = "LAND") String propertyRegistrationContext,
            @RequestParam(value = "documentFiles", required = false) MultipartFile[] documentFiles,
            @RequestParam(value = "documentCategories", required = false) String[] documentCategories,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        String registrationContext = normalize(propertyRegistrationContext).toUpperCase();
        if ("LAND".equals(registrationContext)) {
            newProperty.setPropertyType("Lot");
            newProperty.setFloorAreaSqm(null);
        }

        String validationError = validatePropertyRegistration(newProperty, titleNotAvailable,
                propertyRegistrationContext);
        if (validationError != null) {
            redirectAttributes.addFlashAttribute("errorMessage", validationError);
            return "redirect:/";
        }

        if (titleNotAvailable) {
            newProperty.setTitleNumber(null);
        }

        if (newProperty.getOperationalStatus() == null || newProperty.getOperationalStatus().isBlank()) {
            newProperty.setOperationalStatus("Active/In Use");
        }

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

    private String validatePropertyRegistration(RealEstateProperty property, boolean titleNotAvailable,
            String propertyRegistrationContext) {
        if (isBlank(property.getPropertyName())) {
            return "Property name is required.";
        }
        if (isBlank(property.getPropertyType())) {
            return "Property type is required.";
        }
        if (isBlank(property.getAddressLine1())) {
            return "Address Line 1 is required.";
        }
        if (isBlank(property.getProvince())) {
            return "Province is required.";
        }
        if (isBlank(property.getCity())) {
            return "City / Municipality is required.";
        }
        if (isBlank(property.getBarangay())) {
            return "Barangay is required.";
        }
        if (isBlank(property.getOperationalStatus())) {
            return "Operational & Utilization Status is required.";
        }
        if (isBlank(property.getConditionStatus())) {
            return "Condition Status is required.";
        }
        if (isBlank(property.getArea())) {
            return "Area is required.";
        }

        String propertyType = normalize(property.getPropertyType());
        String titleNumber = normalize(property.getTitleNumber());
        String taxDeclarationNumber = normalize(property.getTaxDeclarationNumber());
        String surveyPlanNumber = normalize(property.getSurveyPlanNumber());
        String registrationContext = normalize(propertyRegistrationContext).toUpperCase();

        if (!registrationContext.isEmpty()) {
            if ("LAND".equals(registrationContext) && !isLotType(propertyType)) {
                return "Land Assets intake accepts lot properties only.";
            }
            if ("BUILDING_FACILITY".equals(registrationContext) && isLotType(propertyType)) {
                return "Buildings & Facilities intake does not accept lot properties.";
            }
        }

        if ("LAND".equals(registrationContext)) {
            if (isBlank(property.getLegalTitlingStatus())) {
                return "Legal & Titling Status is required for land assets.";
            }
            if (titleNumber.isEmpty()) {
                return "Title Number / TCT is required for land assets.";
            }
            if (taxDeclarationNumber.isEmpty()) {
                return "Tax Declaration number is required for land assets.";
            }
            if (surveyPlanNumber.isEmpty()) {
                return "Survey Plan number is required for land assets.";
            }
            return null;
        }

        if (isLotType(propertyType)) {
            if (titleNotAvailable) {
                if (taxDeclarationNumber.isEmpty()) {
                    return "Tax Declaration number is required when Title Number is not available.";
                }
            } else if (titleNumber.isEmpty()) {
                return "Title Number is required for lot properties.";
            }
        } else if (taxDeclarationNumber.isEmpty()) {
            return "Tax Declaration number is required for non-lot properties.";
        }

        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isLotType(String propertyType) {
        return propertyType != null && propertyType.equalsIgnoreCase("Lot");
    }

    private String resolveReferenceId(RealEstateProperty property) {
        return property.getTitleNumber() != null ? property.getTitleNumber() : "PROP-" + property.getPropertyID();
    }

    @PostMapping("/assign-custodian")
    @Transactional
    public String assignCustodian(@RequestParam Integer propertyID, @RequestParam String employeeID,
            @RequestParam String conditionNotes, RedirectAttributes redirectAttributes) {
        RealEstateProperty property = propertyRepo.findById(propertyID).orElse(null);
        if (property != null) {
            property.setCustodianID(employeeID);
            propertyRepo.save(property);
            String refId = resolveReferenceId(property);
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
            String refId = resolveReferenceId(property);
            auditService.logLifecycleEvent(refId, "FINANCE", "Tax Status Updated: " + taxStatus, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Tax status updated.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Property not found.");
        }
        return "redirect:/properties";
    }

    @GetMapping("/{id}/history")
    @ResponseBody
    public ResponseEntity<List<AssetHistoryService.AssetHistoryEntry>> getPropertyHistory(@PathVariable Integer id) {
        return propertyRepo.findById(id)
                .map(property -> assetHistoryService.getHistory(resolveReferenceId(property)))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPropertyDetails(@PathVariable Integer id) {
        return propertyRepo.findById(id)
                .map(property -> {
                    String custodianId = property.getCustodianID();
                    String custodianName = custodianId == null || custodianId.isBlank()
                            ? "Unassigned"
                            : registryService.getEmployeeNameMap().getOrDefault(custodianId, custodianId);

                    Map<String, Object> response = Map.ofEntries(
                            Map.entry("propertyID", property.getPropertyID()),
                            Map.entry("propertyType",
                                    property.getPropertyType() == null ? "" : property.getPropertyType()),
                            Map.entry("propertyName",
                                    property.getPropertyName() == null ? "" : property.getPropertyName()),
                            Map.entry("area", property.getArea() == null ? "" : property.getArea()),
                            Map.entry("titleNumber",
                                    property.getTitleNumber() == null ? "" : property.getTitleNumber()),
                            Map.entry("taxDeclarationNumber",
                                    property.getTaxDeclarationNumber() == null ? ""
                                            : property.getTaxDeclarationNumber()),
                            Map.entry("surveyPlanNumber",
                                    property.getSurveyPlanNumber() == null ? "" : property.getSurveyPlanNumber()),
                            Map.entry("propertyDetails",
                                    property.getPropertyDetails() == null ? "" : property.getPropertyDetails()),
                            Map.entry("addressLine1",
                                    property.getAddressLine1() == null ? "" : property.getAddressLine1()),
                            Map.entry("addressLine2",
                                    property.getAddressLine2() == null ? "" : property.getAddressLine2()),
                            Map.entry("province", property.getProvince() == null ? "" : property.getProvince()),
                            Map.entry("city", property.getCity() == null ? "" : property.getCity()),
                            Map.entry("barangay", property.getBarangay() == null ? "" : property.getBarangay()),
                            Map.entry("zipCode", property.getZipCode() == null ? "" : property.getZipCode()),
                            Map.entry("lotAreaSqm", property.getLotAreaSqm() == null ? "" : property.getLotAreaSqm()),
                            Map.entry("floorAreaSqm",
                                    property.getFloorAreaSqm() == null ? "" : property.getFloorAreaSqm()),
                            Map.entry("acquisitionDate",
                                    property.getAcquisitionDate() == null ? "" : property.getAcquisitionDate()),
                            Map.entry("assessedValue",
                                    property.getAssessedValue() == null ? "" : property.getAssessedValue()),
                            Map.entry("propertyTaxStatus",
                                    property.getPropertyTaxStatus() == null ? "" : property.getPropertyTaxStatus()),
                            Map.entry("legalTitlingStatus",
                                    property.getLegalTitlingStatus() == null ? ""
                                            : property.getLegalTitlingStatus()),
                            Map.entry("operationalStatus",
                                    property.getOperationalStatus() == null ? "" : property.getOperationalStatus()),
                            Map.entry("conditionStatus",
                                    property.getConditionStatus() == null ? "" : property.getConditionStatus()),
                            Map.entry("custodianID", custodianId == null ? "" : custodianId),
                            Map.entry("custodianName", custodianName),
                            Map.entry("remarks", property.getRemarks() == null ? "" : property.getRemarks()));
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/update")
    public ResponseEntity<String> updateProperty(@RequestBody RealEstateProperty updatedProp) {
        Integer propertyId = updatedProp.getPropertyID();
        if (propertyId == null) {
            return ResponseEntity.badRequest().body("Property ID is required.");
        }

        return propertyRepo.findById(propertyId)
                .map(existing -> {
                    existing.setAssessedValue(updatedProp.getAssessedValue());
                    existing.setPropertyTaxStatus(updatedProp.getPropertyTaxStatus());
                    existing.setLegalTitlingStatus(updatedProp.getLegalTitlingStatus());
                    existing.setOperationalStatus(updatedProp.getOperationalStatus());
                    existing.setConditionStatus(updatedProp.getConditionStatus());
                    existing.setZipCode(updatedProp.getZipCode());
                    existing.setLotAreaSqm(updatedProp.getLotAreaSqm());
                    existing.setFloorAreaSqm(updatedProp.getFloorAreaSqm());
                    existing.setPropertyDetails(updatedProp.getPropertyDetails());
                    propertyRepo.save(existing);
                    return ResponseEntity.ok("Property updated successfully");
                })
                .orElse(ResponseEntity.notFound().build());
    }
}