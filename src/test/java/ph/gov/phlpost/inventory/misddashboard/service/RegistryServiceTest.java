package ph.gov.phlpost.inventory.misddashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ph.gov.phlpost.inventory.misddashboard.model.Personnel;
import ph.gov.phlpost.inventory.misddashboard.repository.EquipmentCatalogRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.PersonnelRepository;

@ExtendWith(MockitoExtension.class)
class RegistryServiceTest {

    @Mock
    private PersonnelRepository personnelRepository;

    @Mock
    private EquipmentCatalogRepository catalogRepository;

    private RegistryService service;

    @BeforeEach
    void setUp() {
        service = new RegistryService(personnelRepository, catalogRepository, "SUPPLIER");
    }

    @Test
    void employeeMapIsLastNameFirstAndIncludesTheSupplierPseudoEmployee() {
        List<Personnel> personnel = List.of(person("E-1", "Juan", "Santos", null));
        when(personnelRepository.findAll()).thenReturn(personnel);

        assertThat(service.getEmployeeNameMap())
                .containsEntry("E-1", "Santos, Juan")
                .containsEntry("SUPPLIER", "Supplier");
    }

    @Test
    void displayNameFallsBackToTheIdForUnknownEmployeesAndUnassignedForBlank() {
        List<Personnel> personnel = List.of(person("E-1", "Juan", "Santos", null));
        when(personnelRepository.findAll()).thenReturn(personnel);

        assertThat(service.resolveDisplayName("E-1")).isEqualTo("Santos, Juan");
        assertThat(service.resolveDisplayName("E-404")).isEqualTo("E-404");
        assertThat(service.resolveDisplayName(" ")).isEqualTo("Unassigned");
    }

    @Test
    void managerNameResolvesThroughTheEmployeeRecord() {
        Personnel manager = person("M-1", "Maria", "Reyes", null);
        Personnel staff = person("E-1", "Juan", "Santos", "M-1");
        Personnel orphan = person("E-2", "Ana", "Cruz", "GONE");
        when(personnelRepository.findAll()).thenReturn(List.of(manager, staff, orphan));
        when(personnelRepository.findById("E-1")).thenReturn(Optional.of(staff));
        when(personnelRepository.findById("E-2")).thenReturn(Optional.of(orphan));
        when(personnelRepository.findById("M-1")).thenReturn(Optional.of(manager));

        assertThat(service.getManagerNameByEmployeeId("E-1")).isEqualTo("Reyes, Maria");
        assertThat(service.getManagerNameByEmployeeId("E-2")).isEqualTo("Unknown Manager");
        assertThat(service.getManagerNameByEmployeeId("M-1")).isEqualTo("No Manager");
        assertThat(service.getManagerNameByEmployeeId(null)).isEqualTo("No Manager");
    }

    /** Personnel has no ManagerID setter (the column is maintained outside the app), so it is mocked. */
    private static Personnel person(String id, String first, String last, String managerId) {
        Personnel personnel = mock(Personnel.class);
        lenient().when(personnel.getEmployeeID()).thenReturn(id);
        lenient().when(personnel.getFirstName()).thenReturn(first);
        lenient().when(personnel.getLastName()).thenReturn(last);
        lenient().when(personnel.getManagerID()).thenReturn(managerId);
        return personnel;
    }
}
