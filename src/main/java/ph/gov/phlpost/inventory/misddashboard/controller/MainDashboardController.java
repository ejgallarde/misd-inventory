package ph.gov.phlpost.inventory.misddashboard.controller;

import ph.gov.phlpost.inventory.misddashboard.model.EquipmentCatalog;
import ph.gov.phlpost.inventory.misddashboard.model.Personnel;
import ph.gov.phlpost.inventory.misddashboard.repository.DashboardRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.AssetRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.EquipmentCatalogRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.FleetVehicleRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.PersonnelRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.RealEstatePropertyRepository;
import ph.gov.phlpost.inventory.misddashboard.service.RegistryService;
import ph.gov.phlpost.inventory.misddashboard.util.TextUtils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.stream.Collectors;

@Controller
public class MainDashboardController {

        private final DashboardRepository dashboardRepo;
        private final AssetRepository assetRepo;
        private final FleetVehicleRepository fleetRepo;
        private final RealEstatePropertyRepository propertyRepo;
        private final EquipmentCatalogRepository catalogRepo;
        private final PersonnelRepository personnelRepo;
        private final RegistryService registryService;

        @Value("#{'${inventory.categories}'.split(',')}")
        private List<String> equipmentCategories;

        @Value("#{'${dropdown.vehicle-types}'.split(',')}")
        private List<String> vehicleTypes;

        @Value("#{'${dropdown.vehicle-years}'.split(',')}")
        private List<String> vehicleYears;

        @Value("#{'${dropdown.fuel-types}'.split(',')}")
        private List<String> fuelTypes;

        @Value("#{'${dropdown.fleet-admin-legal-statuses}'.split(',')}")
        private List<String> fleetAdminLegalStatuses;

        @Value("#{'${dropdown.fleet-operational-statuses}'.split(',')}")
        private List<String> fleetOperationalStatuses;

        @Value("#{'${dropdown.fleet-maintenance-statuses}'.split(',')}")
        private List<String> fleetMaintenanceStatuses;

        @Value("#{'${dropdown.property-types}'.split(',')}")
        private List<String> propertyTypes;

        @Value("#{'${dropdown.property-areas}'.split(',')}")
        private List<String> propertyAreas;

        @Value("#{'${dropdown.property-tax-status-add}'.split(',')}")
        private List<String> propertyTaxStatusesAdd;

        @Value("#{'${dropdown.property-legal-titling-statuses}'.split(',')}")
        private List<String> propertyLegalTitlingStatuses;

        @Value("#{'${dropdown.property-operational-statuses}'.split(',')}")
        private List<String> propertyOperationalStatuses;

        @Value("#{'${dropdown.property-condition-statuses}'.split(',')}")
        private List<String> propertyConditionStatuses;

        @Value("${document.upload.max-size-mb:15}")
        private int documentUploadMaxSizeMb;

        @Value("${document.upload.allowed-extensions:pdf,jpg,jpeg,png,doc,docx,xls,xlsx}")
        private String documentUploadAllowedExtensions;

        @Value("${document.upload.categories.it}")
        private String itDocumentUploadCategoriesCsv;

        @Value("${document.upload.categories.vehicle}")
        private String vehicleDocumentUploadCategoriesCsv;

        @Value("${document.upload.categories.property}")
        private String propertyDocumentUploadCategoriesCsv;

        @Value("#{'${dropdown.asset-deployment-statuses}'.split(',')}")
        private List<String> assetDeploymentStatusOptions;

        @Value("#{'${dropdown.asset-maintenance-health-statuses}'.split(',')}")
        private List<String> assetMaintenanceHealthStatusOptions;

        @Value("#{'${dropdown.asset-lifecycle-statuses}'.split(',')}")
        private List<String> assetLifecycleStatusOptions;

        public MainDashboardController(DashboardRepository dashboardRepo, AssetRepository assetRepo,
                        FleetVehicleRepository fleetRepo,
                        RealEstatePropertyRepository propertyRepo, EquipmentCatalogRepository catalogRepo,
                        PersonnelRepository personnelRepo, RegistryService registryService) {
                this.dashboardRepo = dashboardRepo;
                this.assetRepo = assetRepo;
                this.fleetRepo = fleetRepo;
                this.propertyRepo = propertyRepo;
                this.catalogRepo = catalogRepo;
                this.personnelRepo = personnelRepo;
                this.registryService = registryService;
        }

        @GetMapping("/")
        public String viewDashboard(Model model) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String displayName = "User";
                if (authentication != null && authentication.isAuthenticated()
                                && !"anonymousUser".equals(authentication.getPrincipal().toString())) {
                        String principal = authentication.getName();
                        String[] parts = principal.split("@", 2);
                        displayName = parts.length > 0 ? parts[0] : principal;
                        displayName = displayName.replace('.', ' ').trim();
                        if (displayName.isBlank()) {
                                displayName = "User";
                        }
                }
                model.addAttribute("userDisplayName", displayName);

                // IT Assets
                model.addAttribute("totalAssets", dashboardRepo.countTotalAssets());
                model.addAttribute("currentInventoryAssets", dashboardRepo.countCurrentInventoryAssets());
                model.addAttribute("deployedAssets", dashboardRepo.countDeployedAssets());
                model.addAttribute("problematicAssets", assetRepo.findProblematicAssets());
                model.addAttribute("problematicAssetsCount", dashboardRepo.countProblematicAssets());
                model.addAttribute("deploymentIssueCount", dashboardRepo.countDeploymentIssues());
                model.addAttribute("underMaintenanceAssets", dashboardRepo.countUnderMaintenanceAssets());
                model.addAttribute("decommissionedRetiredAssets", dashboardRepo.countDecommissionedRetiredAssets());
                model.addAttribute("catalogItems", catalogRepo.findAll());
                model.addAttribute("catalogItemsByCategory", groupCatalogItemsByCategory(catalogRepo.findAll()));

                // Fleet
                model.addAttribute("totalVehicles", fleetRepo.count());
                model.addAttribute("currentInventoryVehicles", fleetRepo.countCurrentInventoryVehicles());
                model.addAttribute("availableIdleVehicles", fleetRepo.countAvailableIdleVehicles());
                model.addAttribute("dispatchedVehicles", fleetRepo.countDispatchedVehicles());
                model.addAttribute("underRepairVehicles", fleetRepo.countUnderRepairVehicles());
                model.addAttribute("slatedForDisposalVehicles", fleetRepo.countSlatedForDisposalVehicles());
                model.addAttribute("decommissionedVehicles", fleetRepo.countDecommissionedVehicles());
                model.addAttribute("totalProblematicVehicles", fleetRepo.countProblematicVehicles());
                model.addAttribute("vehiclesWithAdminLegalIssues", fleetRepo.countVehiclesWithAdminLegalIssues());
                model.addAttribute("vehiclesWithOperationalMaintenanceIssues",
                                fleetRepo.countVehiclesWithOperationalMaintenanceIssues());
                model.addAttribute("expiringRegistrations", fleetRepo.countExpiringRegistrations());
                model.addAttribute("soldVehicles", fleetRepo.countSoldVehicles());
                model.addAttribute("disposedOrDecommissionedVehicles",
                                fleetRepo.countDisposedOrDecommissionedVehicles());
                model.addAttribute("problematicVehicles", fleetRepo.findProblematicVehicles());

                // Land Assets and Buildings & Facilities
                String lotType = "Lot";

                model.addAttribute("totalLandAssets", propertyRepo.countByPropertyTypeIgnoreCase(lotType));
                model.addAttribute("totalLandArea", propertyRepo.sumTotalLandAreaByType(lotType));
                model.addAttribute("currentInventoryLandAssets",
                                propertyRepo.countByTypeAndOperationalStatusNot(lotType, "Slated for Disposal"));
                model.addAttribute("activeInUseLandAssets",
                                propertyRepo.countByTypeAndOperationalStatus(lotType, "Active/In Use"));
                model.addAttribute("vacantIdleLandAssets",
                                propertyRepo.countByTypeAndOperationalStatus(lotType, "Vacant/Idle"));
                model.addAttribute("coLocatedLandAssets",
                                propertyRepo.countByTypeAndOperationalStatus(lotType, "Co-Located"));
                model.addAttribute("leasedOutLandAssets",
                                propertyRepo.countByTypeAndOperationalStatus(lotType, "Leased Out"));
                model.addAttribute("underConstructionLandAssets",
                                propertyRepo.countByTypeAndOperationalStatus(lotType, "Under Construction"));
                model.addAttribute("slatedForDisposalLandAssets",
                                propertyRepo.countByTypeAndOperationalStatus(lotType, "Slated for Disposal"));
                model.addAttribute("problematicLandAssets",
                                propertyRepo.findProblematicPropertiesByType(lotType));

                model.addAttribute("totalBuildingFacilityAssets", propertyRepo.countByPropertyTypeExcluding(lotType));
                model.addAttribute("currentInventoryBuildingAssets",
                                propertyRepo.countByTypeExcludingAndOperationalStatusNot(lotType,
                                                "Slated for Disposal"));
                model.addAttribute("activeInUseBuildingAssets",
                                propertyRepo.countByTypeExcludingAndOperationalStatus(lotType, "Active/In Use"));
                model.addAttribute("vacantIdleBuildingAssets",
                                propertyRepo.countByTypeExcludingAndOperationalStatus(lotType, "Vacant/Idle"));
                model.addAttribute("coLocatedBuildingAssets",
                                propertyRepo.countByTypeExcludingAndOperationalStatus(lotType, "Co-Located"));
                model.addAttribute("leasedOutBuildingAssets",
                                propertyRepo.countByTypeExcludingAndOperationalStatus(lotType, "Leased Out"));
                model.addAttribute("underConstructionBuildingAssets",
                                propertyRepo.countByTypeExcludingAndOperationalStatus(lotType,
                                                "Under Construction"));
                model.addAttribute("slatedForDisposalBuildingAssets",
                                propertyRepo.countByTypeExcludingAndOperationalStatus(lotType,
                                                "Slated for Disposal"));
                model.addAttribute("problematicBuildingAssets",
                                propertyRepo.findProblematicPropertiesByTypeExcluding(lotType));

                // Mappings
                model.addAttribute("employeeMap", registryService.getEmployeeNameMap());
                model.addAttribute("catalogMap", registryService.getCatalogMap());
                model.addAttribute("departmentMap", registryService.getDepartmentMap());
                model.addAttribute("divisionMap", registryService.getDivisionMap());
                model.addAttribute("personnelLocationMap", registryService.getPersonnelLocationMap());
                model.addAttribute("managerNameMap", registryService.getManagerNameMap());
                model.addAttribute("equipmentCategories", equipmentCategories.stream()
                                .sorted(String.CASE_INSENSITIVE_ORDER)
                                .toList());
                model.addAttribute("documentUploadMaxSizeMb", documentUploadMaxSizeMb);
                model.addAttribute("documentUploadAllowedExtensions", documentUploadAllowedExtensions);
                model.addAttribute("itDocumentUploadCategories",
                                TextUtils.splitCsv(itDocumentUploadCategoriesCsv).stream()
                                                .sorted(String.CASE_INSENSITIVE_ORDER)
                                                .toList());
                model.addAttribute("vehicleDocumentUploadCategories",
                                TextUtils.splitCsv(vehicleDocumentUploadCategoriesCsv).stream()
                                                .sorted(String.CASE_INSENSITIVE_ORDER)
                                                .toList());
                model.addAttribute("propertyDocumentUploadCategories",
                                TextUtils.splitCsv(propertyDocumentUploadCategoriesCsv).stream()
                                                .sorted(String.CASE_INSENSITIVE_ORDER)
                                                .toList());
                model.addAttribute("assetDeploymentStatusOptions", assetDeploymentStatusOptions);
                model.addAttribute("assetMaintenanceHealthStatusOptions", assetMaintenanceHealthStatusOptions);
                model.addAttribute("assetLifecycleStatusOptions", assetLifecycleStatusOptions);
                model.addAttribute("vehicleTypes", vehicleTypes.stream()
                                .sorted(String.CASE_INSENSITIVE_ORDER)
                                .toList());
                model.addAttribute("vehicleYears", vehicleYears);
                model.addAttribute("fuelTypes", fuelTypes.stream()
                                .sorted(String.CASE_INSENSITIVE_ORDER)
                                .toList());
                model.addAttribute("fleetVehicleYears", vehicleYears);
                model.addAttribute("fleetFuelTypes", fuelTypes.stream()
                                .sorted(String.CASE_INSENSITIVE_ORDER)
                                .toList());
                model.addAttribute("fleetAdminLegalStatuses", fleetAdminLegalStatuses);
                model.addAttribute("fleetOperationalStatuses", fleetOperationalStatuses);
                model.addAttribute("fleetMaintenanceStatuses", fleetMaintenanceStatuses);
                model.addAttribute("propertyTypes", propertyTypes.stream()
                                .sorted(String.CASE_INSENSITIVE_ORDER)
                                .toList());
                model.addAttribute("propertyAreas", propertyAreas.stream()
                                .sorted(String.CASE_INSENSITIVE_ORDER)
                                .toList());
                model.addAttribute("propertyTaxStatusesAdd", propertyTaxStatusesAdd.stream()
                                .sorted(String.CASE_INSENSITIVE_ORDER)
                                .toList());
                model.addAttribute("propertyLegalTitlingStatuses", propertyLegalTitlingStatuses);
                model.addAttribute("propertyOperationalStatuses", propertyOperationalStatuses);
                model.addAttribute("propertyConditionStatuses", propertyConditionStatuses);

                return "dashboard";
        }

        @GetMapping("/api/personnel/search")
        @ResponseBody
        public Map<String, Object> searchPersonnel(
                        @RequestParam(value = "q", required = false, defaultValue = "") String q,
                        @RequestParam(value = "jobTitle", required = false) String jobTitle,
                        @PageableDefault(size = 15) Pageable pageable) {
                Page<Personnel> results = jobTitle == null || jobTitle.isBlank()
                                ? personnelRepo.findByLastNameContainingIgnoreCaseOrFirstNameContainingIgnoreCase(
                                                q, q, pageable)
                                : personnelRepo.searchByJobTitle(jobTitle.trim(), q, pageable);
                List<Map<String, String>> items = results.getContent().stream().map(p -> {
                        Map<String, String> map = new HashMap<>();
                        map.put("id", p.getEmployeeID());
                        map.put("text", p.getLastName() + ", " + p.getFirstName() + " (" + p.getDepartment() +
                                        (p.getDivision() != null && !p.getDivision().isBlank() ? " / " + p.getDivision()
                                                        : "")
                                        + ")");
                        return map;
                }).sorted(Comparator.comparing(item -> item.get("text"), String.CASE_INSENSITIVE_ORDER))
                                .collect(Collectors.toList());

                Map<String, Object> response = new HashMap<>();
                response.put("results", items);
                response.put("pagination", Collections.singletonMap("more", results.hasNext()));
                return response;
        }

        private Map<String, List<EquipmentCatalog>> groupCatalogItemsByCategory(List<EquipmentCatalog> catalogItems) {
                return catalogItems.stream()
                                .sorted(Comparator
                                                .comparing((EquipmentCatalog item) -> item.getManufacturer() == null
                                                                ? ""
                                                                : item.getManufacturer(),
                                                                String.CASE_INSENSITIVE_ORDER)
                                                .thenComparing(item -> item.getModelName() == null ? ""
                                                                : item.getModelName(),
                                                                String.CASE_INSENSITIVE_ORDER))
                                .collect(Collectors.groupingBy(
                                                item -> item.getCategory() == null || item.getCategory().isBlank()
                                                                ? "Uncategorized"
                                                                : item.getCategory(),
                                                java.util.TreeMap::new,
                                                Collectors.toList()));
        }
}