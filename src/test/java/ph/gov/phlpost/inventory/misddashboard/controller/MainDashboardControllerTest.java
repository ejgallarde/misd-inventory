package ph.gov.phlpost.inventory.misddashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import ph.gov.phlpost.inventory.misddashboard.model.EquipmentCatalog;
import ph.gov.phlpost.inventory.misddashboard.model.FleetVehicleCatalog;
import ph.gov.phlpost.inventory.misddashboard.model.Personnel;
import ph.gov.phlpost.inventory.misddashboard.model.SurveyEquipmentCatalog;
import ph.gov.phlpost.inventory.misddashboard.repository.AssetRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.DashboardRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.EquipmentCatalogRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.FleetVehicleCatalogRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.FleetVehicleRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.PersonnelRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.SurveyAssetRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.SurveyEquipmentCatalogRepository;
import ph.gov.phlpost.inventory.misddashboard.service.AssetHistoryService;
import ph.gov.phlpost.inventory.misddashboard.service.RegistryService;

@ExtendWith(MockitoExtension.class)
class MainDashboardControllerTest {

    @Mock
    private DashboardRepository dashboardRepo;

    @Mock
    private AssetRepository assetRepo;

    @Mock
    private FleetVehicleRepository fleetRepo;

    @Mock
    private SurveyAssetRepository surveyAssetRepo;

    @Mock
    private EquipmentCatalogRepository catalogRepo;

    @Mock
    private SurveyEquipmentCatalogRepository surveyCatalogRepo;

    @Mock
    private FleetVehicleCatalogRepository fleetCatalogRepo;

    @Mock
    private PersonnelRepository personnelRepo;

    @Mock
    private RegistryService registryService;

    @Mock
    private AssetHistoryService assetHistoryService;

    @InjectMocks
    private MainDashboardController controller;

    private void setDropdownFields() {
        // These come from @Value-injected properties, which Spring only resolves
        // inside a running context; instantiated directly (as @InjectMocks does
        // here) they are null, so viewDashboard's own .stream() calls on them
        // would NPE without being set first.
        ReflectionTestUtils.setField(controller, "equipmentCategories", List.of("Laptop", "Desktop"));
        ReflectionTestUtils.setField(controller, "vehicleTypes", List.of("Sedan"));
        ReflectionTestUtils.setField(controller, "vehicleYears", List.of("2024", "2025"));
        ReflectionTestUtils.setField(controller, "fuelTypes", List.of("Diesel"));
        ReflectionTestUtils.setField(controller, "fleetAdminLegalStatuses", List.of("Registered"));
        ReflectionTestUtils.setField(controller, "fleetOperationalStatuses", List.of("Available/Idle"));
        ReflectionTestUtils.setField(controller, "fleetMaintenanceStatuses", List.of("Roadworthy"));
        ReflectionTestUtils.setField(controller, "surveyAssetTypes", List.of("GNSS"));
        ReflectionTestUtils.setField(controller, "surveyAssetAdminLegalStatuses", List.of("Registered"));
        ReflectionTestUtils.setField(controller, "surveyAssetOperationalStatuses", List.of("Serviceable"));
        ReflectionTestUtils.setField(controller, "surveyAssetConditionStatuses", List.of("Serviceable"));
        ReflectionTestUtils.setField(controller, "documentUploadMaxSizeMb", 15);
        ReflectionTestUtils.setField(controller, "documentUploadAllowedExtensions", "pdf,jpg");
        ReflectionTestUtils.setField(controller, "itDocumentUploadCategoriesCsv", "Official Receipt / Invoice");
        ReflectionTestUtils.setField(controller, "vehicleDocumentUploadCategoriesCsv", "Delivery Receipt");
        ReflectionTestUtils.setField(controller, "surveyAssetDocumentUploadCategoriesCsv", "Calibration Certificate");
        ReflectionTestUtils.setField(controller, "assetDeploymentStatusOptions", List.of("In Storage"));
        ReflectionTestUtils.setField(controller, "assetMaintenanceHealthStatusOptions", List.of("Operational"));
        ReflectionTestUtils.setField(controller, "assetLifecycleStatusOptions", List.of("Active"));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void viewDashboardGroupsCatalogItemsByCategoryAndReturnsTheDashboardView() {
        setDropdownFields();
        when(registryService.getEmployeeNameMap()).thenReturn(Map.of());
        when(registryService.getCatalogMap()).thenReturn(Map.of());
        when(registryService.getSurveyCatalogMap()).thenReturn(Map.of());
        when(registryService.getFleetCatalogMap()).thenReturn(Map.of());
        when(registryService.getDepartmentMap()).thenReturn(Map.of());
        when(registryService.getDivisionMap()).thenReturn(Map.of());
        when(registryService.getPersonnelLocationMap()).thenReturn(Map.of());

        EquipmentCatalog laptop = new EquipmentCatalog();
        laptop.setCatalogID(1);
        laptop.setCategory("Laptop");
        laptop.setManufacturer("Dell");
        laptop.setModelName("Latitude");
        when(catalogRepo.findAll()).thenReturn(List.of(laptop));
        when(fleetCatalogRepo.findAll()).thenReturn(List.<FleetVehicleCatalog>of());
        when(surveyCatalogRepo.findAll()).thenReturn(List.<SurveyEquipmentCatalog>of());
        when(assetRepo.findProblematicAssets()).thenReturn(List.of());
        when(fleetRepo.findProblematicVehicles()).thenReturn(List.of());
        when(surveyAssetRepo.findProblematicSurveyAssets()).thenReturn(List.of());

        Model model = new ExtendedModelMap();
        String view = controller.viewDashboard(model);

        assertThat(view).isEqualTo("dashboard");
        @SuppressWarnings("unchecked")
        Map<String, List<EquipmentCatalog>> catalogByCategory =
                (Map<String, List<EquipmentCatalog>>) model.getAttribute("catalogItemsByCategory");
        assertThat(catalogByCategory).containsOnlyKeys("Laptop");
        assertThat(catalogByCategory.get("Laptop")).containsExactly(laptop);
        assertThat(model.getAttribute("userDisplayName")).isEqualTo("User");
    }

    @Test
    void viewDashboardDerivesDisplayNameFromAnAuthenticatedPrincipal() {
        setDropdownFields();
        when(registryService.getEmployeeNameMap()).thenReturn(Map.of());
        when(registryService.getCatalogMap()).thenReturn(Map.of());
        when(registryService.getSurveyCatalogMap()).thenReturn(Map.of());
        when(registryService.getFleetCatalogMap()).thenReturn(Map.of());
        when(registryService.getDepartmentMap()).thenReturn(Map.of());
        when(registryService.getDivisionMap()).thenReturn(Map.of());
        when(registryService.getPersonnelLocationMap()).thenReturn(Map.of());
        when(catalogRepo.findAll()).thenReturn(List.of());
        when(fleetCatalogRepo.findAll()).thenReturn(List.of());
        when(surveyCatalogRepo.findAll()).thenReturn(List.of());
        when(assetRepo.findProblematicAssets()).thenReturn(List.of());
        when(fleetRepo.findProblematicVehicles()).thenReturn(List.of());
        when(surveyAssetRepo.findProblematicSurveyAssets()).thenReturn(List.of());

        var authentication = new TestingAuthenticationToken("juan.delacruz@dar.gov.ph", "n/a");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String view = controller.viewDashboard(new ExtendedModelMap());

        assertThat(view).isEqualTo("dashboard");
    }

    @Test
    void searchPersonnelWithoutJobTitleSearchesByNameOnly() {
        Personnel person = new Personnel();
        person.setEmployeeID("PERS-0001");
        person.setFirstName("Juan");
        person.setLastName("Dela Cruz");
        person.setDepartment("MISD");
        person.setDivision("Property");
        when(personnelRepo.findByLastNameContainingIgnoreCaseOrFirstNameContainingIgnoreCase(
                "juan", "juan", PageRequest.of(0, 15)))
                .thenReturn(new PageImpl<>(List.of(person)));

        Map<String, Object> response = controller.searchPersonnel("juan", null, PageRequest.of(0, 15));

        @SuppressWarnings("unchecked")
        List<Map<String, String>> results = (List<Map<String, String>>) response.get("results");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).get("id")).isEqualTo("PERS-0001");
        assertThat(results.get(0).get("text")).isEqualTo("Dela Cruz, Juan (MISD / Property)");
    }

    @Test
    void searchPersonnelWithJobTitleFiltersByJobTitle() {
        Personnel person = new Personnel();
        person.setEmployeeID("PERS-0002");
        person.setFirstName("Ana");
        person.setLastName("Reyes");
        person.setDepartment("MISD");
        person.setDivision(null);
        when(personnelRepo.searchByJobTitle("Computer Maintenance Technologist", "ana", PageRequest.of(0, 15)))
                .thenReturn(new PageImpl<>(List.of(person)));

        Map<String, Object> response = controller.searchPersonnel(
                "ana", "Computer Maintenance Technologist", PageRequest.of(0, 15));

        @SuppressWarnings("unchecked")
        List<Map<String, String>> results = (List<Map<String, String>>) response.get("results");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).get("text")).isEqualTo("Reyes, Ana (MISD)");
    }
}
