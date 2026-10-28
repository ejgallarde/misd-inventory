package ph.gov.phlpost.inventory.misddashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import ph.gov.phlpost.inventory.misddashboard.model.Personnel;
import ph.gov.phlpost.inventory.misddashboard.repository.AssetRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.DashboardRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.EquipmentCatalogRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.FleetVehicleRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.PersonnelRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.RealEstatePropertyRepository;
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
    private RealEstatePropertyRepository propertyRepo;

    @Mock
    private EquipmentCatalogRepository catalogRepo;

    @Mock
    private PersonnelRepository personnelRepo;

    @Mock
    private RegistryService registryService;

    @Mock
    private ph.gov.phlpost.inventory.misddashboard.service.LocationLookupService locationLookupService;

    @Mock
    private ph.gov.phlpost.inventory.misddashboard.service.AccountabilityService accountabilityService;

    @InjectMocks
    private MainDashboardController controller;

    @Test
    void technicianSearchWithNoTermStillMatchesEveryone() {
        // A blank q reaches the controller as null (WebBindingAdvice trims it),
        // and passing null to the LIKE query matched no technician at all.
        Pageable pageable = PageRequest.of(0, 15);
        Page<Personnel> empty = new PageImpl<>(List.of());
        when(personnelRepo.searchByJobTitle(eq("Computer Maintenance Technologist"), eq(""), any(Pageable.class)))
                .thenReturn(empty);

        Map<String, Object> response = controller.searchPersonnel(null, " Computer Maintenance Technologist ", false,
                pageable);

        verify(personnelRepo).searchByJobTitle("Computer Maintenance Technologist", "", pageable);
        assertThat(response).containsKeys("results", "pagination");
    }

    @Test
    void nameSearchWithNoTermPassesAnEmptyStringNotNull() {
        Pageable pageable = PageRequest.of(0, 15);
        when(personnelRepo.findByLastNameContainingIgnoreCaseOrFirstNameContainingIgnoreCase("", "", pageable))
                .thenReturn(new PageImpl<>(List.of()));

        controller.searchPersonnel(null, null, false, pageable);

        verify(personnelRepo).findByLastNameContainingIgnoreCaseOrFirstNameContainingIgnoreCase("", "", pageable);
    }
}
