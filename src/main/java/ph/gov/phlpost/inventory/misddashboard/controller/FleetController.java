package ph.gov.phlpost.inventory.misddashboard.controller;

import ph.gov.phlpost.inventory.misddashboard.model.FleetVehicle;
import ph.gov.phlpost.inventory.misddashboard.model.FleetVehicleCatalog;
import ph.gov.phlpost.inventory.misddashboard.repository.FleetVehicleCatalogRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.FleetVehicleRepository;
import ph.gov.phlpost.inventory.misddashboard.service.AssetHistoryService;
import ph.gov.phlpost.inventory.misddashboard.service.DocumentService;
import ph.gov.phlpost.inventory.misddashboard.service.FleetService;
import ph.gov.phlpost.inventory.misddashboard.service.RegistryService;
import ph.gov.phlpost.inventory.misddashboard.util.TextUtils;

import java.time.Year;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
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
    private final FleetVehicleCatalogRepository fleetCatalogRepo;
    private final FleetService fleetService;
    private final RegistryService registryService;
    private final DocumentService documentService;
    private final AssetHistoryService assetHistoryService;

    @Value("${document.upload.max-size-mb:15}")
    private int documentUploadMaxSizeMb;

    @Value("${document.upload.allowed-extensions:pdf,jpg,jpeg,png,doc,docx,xls,xlsx}")
    private String documentUploadAllowedExtensions;

    @Value("${document.upload.categories.vehicle}")
    private String vehicleDocumentUploadCategoriesCsv;

    @Value("#{'${dropdown.fleet-admin-legal-statuses}'.split(',')}")
    private List<String> fleetAdminLegalStatuses;

    @Value("#{'${dropdown.fleet-operational-statuses}'.split(',')}")
    private List<String> fleetOperationalStatuses;

    @Value("#{'${dropdown.fleet-maintenance-statuses}'.split(',')}")
    private List<String> fleetMaintenanceStatuses;

    @Value("#{'${dropdown.vehicle-years}'.split(',')}")
    private List<String> fleetVehicleYears;

    public FleetController(FleetVehicleRepository fleetRepo,
            FleetVehicleCatalogRepository fleetCatalogRepo,
            FleetService fleetService,
            RegistryService registryService,
            DocumentService documentService,
            AssetHistoryService assetHistoryService) {
        this.fleetRepo = fleetRepo;
        this.fleetCatalogRepo = fleetCatalogRepo;
        this.fleetService = fleetService;
        this.registryService = registryService;
        this.documentService = documentService;
        this.assetHistoryService = assetHistoryService;
    }

    @GetMapping
    public String viewAllFleet(@RequestParam(required = false) String filter, Model model) {
        model.addAttribute("allVehicles", fleetRepo.findAll());
        model.addAttribute("filter", filter);
        model.addAttribute("employeeMap", registryService.getEmployeeNameMap());
        model.addAttribute("managerNameMap", registryService.getManagerNameMap());
        model.addAttribute("documentUploadMaxSizeMb", documentUploadMaxSizeMb);
        model.addAttribute("documentUploadAllowedExtensions", documentUploadAllowedExtensions);
        model.addAttribute("vehicleDocumentUploadCategories", TextUtils.splitCsv(vehicleDocumentUploadCategoriesCsv).stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList());
        model.addAttribute("fleetAdminLegalStatuses", fleetAdminLegalStatuses);
        model.addAttribute("fleetOperationalStatuses", fleetOperationalStatuses);
        model.addAttribute("fleetMaintenanceStatuses", fleetMaintenanceStatuses);
        model.addAttribute("fleetVehicleYears", fleetVehicleYears);
        model.addAttribute("fleetCatalogMap", registryService.getFleetCatalogMap());
        return "fleet";
    }

    @PostMapping("/catalog/add")
    @CacheEvict(value = "fleetCatalogMap", allEntries = true)
    public String addFleetCatalog(@ModelAttribute FleetVehicleCatalog newCatalog,
            RedirectAttributes redirectAttributes) {
        int currentYear = Year.now().getValue();
        if (newCatalog.getYearModel() == null
                || newCatalog.getYearModel() < 1980 || newCatalog.getYearModel() > currentYear + 1) {
            redirectAttributes.addFlashAttribute("errorMessage", "Year model is out of allowed range.");
            return "redirect:/";
        }

        fleetCatalogRepo.save(newCatalog);
        redirectAttributes.addFlashAttribute("successMessage", "Fleet vehicle catalog updated.");
        return "redirect:/";
    }

    @PostMapping("/add")
    public String registerVehicle(@ModelAttribute FleetVehicle newVehicle,
            @RequestParam(value = "documentFiles", required = false) MultipartFile[] documentFiles,
            @RequestParam(value = "documentCategories", required = false) String[] documentCategories,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        // Registration does not set assignment; this is handled by lifecycle actions.
        newVehicle.setAssignedDriverID(null);

        String validationError = validateVehicleRegistration(newVehicle);
        if (validationError != null) {
            redirectAttributes.addFlashAttribute("errorMessage", validationError);
            return "redirect:/";
        }

        if (newVehicle.getAdminLegaltionalStatus() == null || newVehicle.getAdminLegaltionalStatus().isBlank()) {
            newVehicle.setAdminLegaltionalStatus("Registered");
        }
        if (newVehicle.getOperationalStatus() == null || newVehicle.getOperationalStatus().isBlank()) {
            newVehicle.setOperationalStatus("Available/Idle");
        }
        if (newVehicle.getMaintenanceStatus() == null || newVehicle.getMaintenanceStatus().isBlank()) {
            newVehicle.setMaintenanceStatus("Roadworthy");
        }

        fleetRepo.save(newVehicle);

        if (documentService.hasFiles(documentFiles) && newVehicle.getVehicleID() != null) {
            String uploadedBy = authentication != null ? authentication.getName() : "SystemUser";
            try {
                documentService.uploadAndSaveDocuments(
                        documentFiles,
                        "VEHICLE",
                        String.valueOf(newVehicle.getVehicleID()),
                        documentCategories,
                        uploadedBy);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Vehicle saved, but document upload failed: " + e.getMessage());
                return "redirect:/";
            }
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "Success! Vehicle registered.");
        return "redirect:/";
    }

    private String validateVehicleRegistration(FleetVehicle vehicle) {
        if (vehicle.getCatalogID() == null) {
            return "Vehicle model is required.";
        }
        if (TextUtils.isBlank(vehicle.getEngineNumber())) {
            return "Engine number is required.";
        }
        if (TextUtils.isBlank(vehicle.getChassisNumberVIN())) {
            return "Chassis number / VIN is required.";
        }
        if (TextUtils.isBlank(vehicle.getAdminLegaltionalStatus())) {
            return "Administrative & Legal Status is required.";
        }
        if (TextUtils.isBlank(vehicle.getOperationalStatus())) {
            return "Operational & Dispatch Status is required.";
        }
        if (TextUtils.isBlank(vehicle.getMaintenanceStatus())) {
            return "Maintenance & Health Status is required.";
        }
        return null;
    }

    @PostMapping("/assign")
    public String assignVehicle(@RequestParam Integer vehicleID, @RequestParam String employeeID,
            @RequestParam(required = false) String conditionNotes, RedirectAttributes redirectAttributes) {
        try {
            fleetService.assignVehicle(vehicleID, employeeID, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Success! Vehicle assigned.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/fleet";
    }

    @PostMapping("/{vehicleID}/under-maintenance")
    public String markUnderMaintenance(@PathVariable Integer vehicleID,
            @RequestParam(required = false) String conditionNotes,
            RedirectAttributes redirectAttributes) {
        try {
            fleetService.markVehicleUnderMaintenance(vehicleID, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle marked under maintenance.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/fleet";
    }

    @PostMapping("/{vehicleID}/impound")
    public String markImpounded(@PathVariable Integer vehicleID,
            @RequestParam(required = false) String conditionNotes,
            RedirectAttributes redirectAttributes) {
        try {
            fleetService.markVehicleImpounded(vehicleID, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle marked impounded.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/fleet";
    }

    @PostMapping("/{vehicleID}/ber")
    public String markBeyondEconomicRepair(@PathVariable Integer vehicleID,
            @RequestParam(required = false) String conditionNotes,
            RedirectAttributes redirectAttributes) {
        try {
            fleetService.markVehicleBeyondEconomicRepair(vehicleID, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle marked beyond economic repair.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/fleet";
    }

    @PostMapping("/{vehicleID}/stolen")
    public String markStolen(@PathVariable Integer vehicleID,
            @RequestParam(required = false) String conditionNotes,
            RedirectAttributes redirectAttributes) {
        try {
            fleetService.markVehicleStolen(vehicleID, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle marked stolen.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/fleet";
    }

    @PostMapping("/{vehicleID}/missing")
    public String markMissing(@PathVariable Integer vehicleID,
            @RequestParam(required = false) String conditionNotes,
            RedirectAttributes redirectAttributes) {
        try {
            fleetService.markVehicleMissing(vehicleID, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle marked missing.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/fleet";
    }

    /**
     * Returns a vehicle to the motorpool. Replaces the notes-less
     * {@code /{vehicleID}/return-to-motorpool} variant that did the same thing:
     * the Actions dropdown now opens the Return modal so a return records its
     * condition notes like every other fleet action.
     */
    @PostMapping("/return")
    public String returnVehicle(@RequestParam Integer vehicleID, @RequestParam(required = false) String conditionNotes,
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
    public String retireVehicle(@RequestParam Integer vehicleID, @RequestParam(required = false) String conditionNotes,
            RedirectAttributes redirectAttributes) {
        try {
            fleetService.retireVehicle(vehicleID, conditionNotes);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle retired.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/fleet";
    }

    @GetMapping("/{id}/history")
    @ResponseBody
    public ResponseEntity<List<AssetHistoryService.AssetHistoryEntry>> getVehicleHistory(@PathVariable Integer id) {
        // Resolved the same way FleetService writes it, so a vehicle registered
        // without a plate number still shows the history logged against its ID
        // instead of silently reporting that it has none.
        return fleetRepo.findById(id)
                .map(FleetService::auditReferenceId)
                .map(assetHistoryService::getHistory)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getVehicleDetails(@PathVariable Integer id) {
        try {
            // Read-only: opening the detail panel must not change the record.
            FleetVehicle vehicle = fleetService.findVehicle(id);
            FleetService.VehicleStatusFlags statusFlags = FleetService.deriveStatusFlags(vehicle);
            FleetVehicleCatalog catalog = registryService.getFleetCatalogMap().get(vehicle.getCatalogID());

            String assignedDriverId = vehicle.getAssignedDriverID();
            String assignedDriverName = registryService.resolveDisplayName(assignedDriverId);
            String assignedDriverManagerName = assignedDriverId == null || assignedDriverId.isBlank()
                    ? "N/A"
                    : registryService.getManagerNameByEmployeeId(assignedDriverId);

            Map<String, Object> response = Map.ofEntries(
                    Map.entry("vehicleID", vehicle.getVehicleID()),
                    Map.entry("plateNumber", vehicle.getPlateNumber() == null ? "" : vehicle.getPlateNumber()),
                    Map.entry("bodyNumber", vehicle.getBodyNumber() == null ? "" : vehicle.getBodyNumber()),
                    Map.entry("catalogID", vehicle.getCatalogID() == null ? "" : vehicle.getCatalogID()),
                    Map.entry("catalogCategory", catalog == null || catalog.getCategory() == null ? "" : catalog.getCategory()),
                    Map.entry("catalogManufacturer",
                            catalog == null || catalog.getManufacturer() == null ? "" : catalog.getManufacturer()),
                    Map.entry("catalogModelName",
                            catalog == null || catalog.getModelName() == null ? "" : catalog.getModelName()),
                    Map.entry("catalogYearModel",
                            catalog == null || catalog.getYearModel() == null ? "" : catalog.getYearModel()),
                    Map.entry("catalogFuelType",
                            catalog == null || catalog.getFuelType() == null ? "" : catalog.getFuelType()),
                    Map.entry("engineNumber",
                            vehicle.getEngineNumber() == null ? "" : vehicle.getEngineNumber()),
                    Map.entry("chassisNumberVIN",
                            vehicle.getChassisNumberVIN() == null ? "" : vehicle.getChassisNumberVIN()),
                    Map.entry("registrationExpiry",
                            vehicle.getRegistrationExpiry() == null ? "" : vehicle.getRegistrationExpiry()),
                    Map.entry("insuranceExpiry",
                            vehicle.getInsuranceExpiry() == null ? "" : vehicle.getInsuranceExpiry()),
                    Map.entry("assignedDriverID", assignedDriverId == null ? "" : assignedDriverId),
                    Map.entry("assignedDriverName", assignedDriverName),
                    Map.entry("assignedDriverManagerName", assignedDriverManagerName),
                    Map.entry("adminLegaltionalStatus", vehicle.getAdminLegaltionalStatus() == null ? ""
                            : vehicle.getAdminLegaltionalStatus()),
                    Map.entry("operationalStatus",
                            vehicle.getOperationalStatus() == null ? "" : vehicle.getOperationalStatus()),
                    Map.entry("maintenanceStatus",
                            vehicle.getMaintenanceStatus() == null ? "" : vehicle.getMaintenanceStatus()),
                    Map.entry("cost", vehicle.getCost() == null ? "" : vehicle.getCost()),
                    Map.entry("acquisitionYear",
                            vehicle.getAcquisitionYear() == null ? "" : vehicle.getAcquisitionYear()),
                    Map.entry("isFullyDepreciated", statusFlags.fullyDepreciated()),
                    Map.entry("isRegistrationExpired", statusFlags.registrationExpired()),
                    Map.entry("remarks", vehicle.getRemarks() == null ? "" : vehicle.getRemarks()));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/update")
    public ResponseEntity<String> updateVehicle(@RequestBody FleetVehicle updatedVehicle,
            Authentication authentication) {
        Integer vehicleId = updatedVehicle.getVehicleID();
        if (vehicleId == null || !fleetRepo.existsById(vehicleId)) {
            return ResponseEntity.notFound().build();
        }

        String performedBy = authentication != null ? authentication.getName() : "SYSTEM";
        fleetService.updateVehicleDetails(updatedVehicle, performedBy);
        return ResponseEntity.ok("Fleet vehicle details updated successfully");
    }
}