package ph.gov.phlpost.inventory.misddashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import ph.gov.phlpost.inventory.misddashboard.model.EquipmentCatalog;
import ph.gov.phlpost.inventory.misddashboard.model.Personnel;
import ph.gov.phlpost.inventory.misddashboard.model.PersonnelBaseLocation;
import ph.gov.phlpost.inventory.misddashboard.repository.EquipmentCatalogRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.FleetVehicleCatalogRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.PersonnelRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.SurveyEquipmentCatalogRepository;

@ExtendWith(MockitoExtension.class)
class RegistryServiceTest {

    @Mock
    private PersonnelRepository personnelRepository;

    @Mock
    private EquipmentCatalogRepository catalogRepository;

    @Mock
    private SurveyEquipmentCatalogRepository surveyCatalogRepository;

    @Mock
    private FleetVehicleCatalogRepository fleetCatalogRepository;

    private RegistryService registryService;

    private void createService() {
        registryService = new RegistryService(personnelRepository, catalogRepository, surveyCatalogRepository,
                fleetCatalogRepository, "SUPPLIER");
    }

    private Personnel personnel(String id, String first, String last, String department, String division,
            String managerId, PersonnelBaseLocation location) {
        Personnel personnel = new Personnel();
        personnel.setEmployeeID(id);
        personnel.setFirstName(first);
        personnel.setLastName(last);
        personnel.setDepartment(department);
        personnel.setDivision(division);
        ReflectionTestUtils.setField(personnel, "managerID", managerId);
        ReflectionTestUtils.setField(personnel, "baseLocation", location);
        return personnel;
    }

    @Test
    void employeeNameMapAddsASyntheticSupplierEntry() {
        createService();
        when(personnelRepository.findAll())
                .thenReturn(List.of(personnel("PERS-0001", "Juan", "Dela Cruz", null, null, null, null)));

        Map<String, String> names = registryService.getEmployeeNameMap();

        assertThat(names).containsEntry("PERS-0001", "Dela Cruz, Juan");
        assertThat(names).containsEntry("SUPPLIER", "Supplier");
    }

    @Test
    void catalogMapIsSortedByManufacturerThenModelThenId() {
        createService();
        EquipmentCatalog zebra = catalog(3, "Zebra Systems", "Model A");
        EquipmentCatalog acme1 = catalog(1, "Acme", "Model B");
        EquipmentCatalog acme0 = catalog(2, "Acme", "Model A");
        when(catalogRepository.findAll()).thenReturn(List.of(zebra, acme1, acme0));

        Map<Integer, EquipmentCatalog> catalogMap = registryService.getCatalogMap();

        assertThat(catalogMap.keySet()).containsExactly(2, 1, 3);
    }

    private EquipmentCatalog catalog(int id, String manufacturer, String modelName) {
        EquipmentCatalog catalog = new EquipmentCatalog();
        catalog.setCatalogID(id);
        catalog.setManufacturer(manufacturer);
        catalog.setModelName(modelName);
        catalog.setCategory("Laptop");
        return catalog;
    }

    @Test
    void departmentAndDivisionDefaultToUnassignedWhenNull() {
        createService();
        when(personnelRepository.findAll())
                .thenReturn(List.of(personnel("PERS-0002", "Ana", "Reyes", null, null, null, null)));

        assertThat(registryService.getDepartmentMap()).containsEntry("PERS-0002", "Unassigned");
        assertThat(registryService.getDivisionMap()).containsEntry("PERS-0002", "Unassigned");
    }

    @Test
    void personnelLocationMapFormatsAKnownLocationAndDefaultsAnUnknownOneToUnassigned() {
        createService();
        PersonnelBaseLocation location = new PersonnelBaseLocation();
        ReflectionTestUtils.setField(location, "area", "Region 1");
        ReflectionTestUtils.setField(location, "province", "Ilocos Norte");
        ReflectionTestUtils.setField(location, "officeAddress", "");
        when(personnelRepository.findAll()).thenReturn(List.of(
                personnel("PERS-0003", "Mia", "Santos", null, null, null, location),
                personnel("PERS-0004", "Leo", "Cruz", null, null, null, null)));

        Map<String, String> locations = registryService.getPersonnelLocationMap();

        assertThat(locations).containsEntry("PERS-0003", "Region 1, Ilocos Norte");
        assertThat(locations).containsEntry("PERS-0004", "Unassigned");
    }

    @Test
    void managerNameMapResolvesKnownManagersAndFallsBackForUnknownOrMissingOnes() {
        createService();
        Personnel manager = personnel("PERS-0010", "Boss", "One", null, null, null, null);
        Personnel withKnownManager = personnel("PERS-0011", "Rank", "File", null, null, "PERS-0010", null);
        Personnel withUnknownManager = personnel("PERS-0012", "Lost", "Report", null, null, "PERS-9999", null);
        Personnel withNoManager = personnel("PERS-0013", "Solo", "Worker", null, null, null, null);
        when(personnelRepository.findAll())
                .thenReturn(List.of(manager, withKnownManager, withUnknownManager, withNoManager));

        Map<String, String> managerNames = registryService.getManagerNameMap();

        assertThat(managerNames).containsEntry("PERS-0011", "One, Boss");
        assertThat(managerNames).containsEntry("PERS-0012", "Unknown Manager");
        assertThat(managerNames).containsEntry("PERS-0013", "No Manager");
        assertThat(managerNames).containsEntry("SUPPLIER", "No Manager");
    }

    @Test
    void resolveDisplayNameFallsBackToTheRawIdWhenUnknownAndToUnassignedWhenBlank() {
        createService();
        when(personnelRepository.findAll())
                .thenReturn(List.of(personnel("PERS-0020", "Known", "Employee", null, null, null, null)));

        assertThat(registryService.resolveDisplayName(null)).isEqualTo("Unassigned");
        assertThat(registryService.resolveDisplayName("  ")).isEqualTo("Unassigned");
        assertThat(registryService.resolveDisplayName("PERS-0020")).isEqualTo("Employee, Known");
        assertThat(registryService.resolveDisplayName("PERS-9999")).isEqualTo("PERS-9999");
    }

    @Test
    void managerNameByEmployeeIdCoversBlankUnknownAndManagerlessEmployees() {
        createService();
        Personnel noManager = personnel("PERS-0030", "Alone", "Worker", null, null, null, null);
        when(personnelRepository.findById("PERS-0030")).thenReturn(java.util.Optional.of(noManager));
        when(personnelRepository.findById("PERS-9999")).thenReturn(java.util.Optional.empty());

        assertThat(registryService.getManagerNameByEmployeeId(null)).isEqualTo("No Manager");
        assertThat(registryService.getManagerNameByEmployeeId("PERS-0030")).isEqualTo("No Manager");
        assertThat(registryService.getManagerNameByEmployeeId("PERS-9999")).isEqualTo("No Manager");
    }
}
