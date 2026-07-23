package ph.gov.phlpost.inventory.misddashboard.controller;

import ph.gov.phlpost.inventory.misddashboard.model.Personnel;
import ph.gov.phlpost.inventory.misddashboard.repository.DashboardRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.EquipmentCatalogRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.FleetVehicleRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.PersonnelRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.RealEstatePropertyRepository;
import ph.gov.phlpost.inventory.misddashboard.service.RegistryService;

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
import java.util.stream.Collectors;

@Controller
public class MainDashboardController {

    private final DashboardRepository dashboardRepo;
    private final FleetVehicleRepository fleetRepo;
    private final RealEstatePropertyRepository propertyRepo;
    private final EquipmentCatalogRepository catalogRepo;
    private final PersonnelRepository personnelRepo;
    private final RegistryService registryService;

    @Value("#{'${inventory.categories}'.split(',')}")
    private List<String> equipmentCategories;

    public MainDashboardController(DashboardRepository dashboardRepo, FleetVehicleRepository fleetRepo,
            RealEstatePropertyRepository propertyRepo, EquipmentCatalogRepository catalogRepo,
            PersonnelRepository personnelRepo, RegistryService registryService) {
        this.dashboardRepo = dashboardRepo;
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
        model.addAttribute("deployedAssets", dashboardRepo.countDeployedAssets());
        model.addAttribute("maintenanceCount", dashboardRepo.countMaintenanceAssets());
        model.addAttribute("agingAssets", dashboardRepo.findAll());
        model.addAttribute("agingCount", dashboardRepo.findAll().size());
        model.addAttribute("catalogItems", catalogRepo.findAll());

        // Fleet
        model.addAttribute("totalVehicles", fleetRepo.count());
        model.addAttribute("vehiclesInRepair", fleetRepo.countVehiclesInRepair());
        model.addAttribute("expiringRegistrations", fleetRepo.countExpiringRegistrations());
        model.addAttribute("agingVehicles", fleetRepo.findAgingVehicles());

        // Properties
        model.addAttribute("totalProperties", propertyRepo.count());
        model.addAttribute("totalLandArea", propertyRepo.sumTotalLandArea());
        model.addAttribute("pendingTaxesCount", propertyRepo.countPropertiesNeedingPayment());
        model.addAttribute("pendingProperties", propertyRepo.findPropertiesNeedingPayment());

        // Mappings
        model.addAttribute("employeeMap", registryService.getEmployeeNameMap());
        model.addAttribute("catalogMap", registryService.getCatalogMap());
        model.addAttribute("equipmentCategories", equipmentCategories);

        return "dashboard";
    }

    @GetMapping("/api/personnel/search")
    @ResponseBody
    public Map<String, Object> searchPersonnel(@RequestParam(value = "q", required = false, defaultValue = "") String q,
            @PageableDefault(size = 15) Pageable pageable) {
        Page<Personnel> results = personnelRepo.findByLastNameContainingIgnoreCaseOrFirstNameContainingIgnoreCase(q, q,
                pageable);
        List<Map<String, String>> items = results.getContent().stream().map(p -> {
            Map<String, String> map = new HashMap<>();
            map.put("id", p.getEmployeeID());
            map.put("text", p.getLastName() + ", " + p.getFirstName() + " (" + p.getDepartment() + ")");
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("results", items);
        response.put("pagination", Collections.singletonMap("more", results.hasNext()));
        return response;
    }
}