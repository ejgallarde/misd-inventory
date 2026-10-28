package ph.gov.phlpost.inventory.misddashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;

import ph.gov.phlpost.inventory.misddashboard.model.LocationOption;
import ph.gov.phlpost.inventory.misddashboard.service.LocationLookupService;

@ExtendWith(MockitoExtension.class)
class LocationOptionsControllerTest {

    private static final LocationOption NCR = new LocationOption("1339", "Metro Manila", null);
    private static final LocationOption PAMPANGA = new LocationOption("0354", "Pampanga", null);
    private static final LocationOption MANILA = new LocationOption("133901", "Manila", null);

    @Mock
    private LocationLookupService lookup;

    @InjectMocks
    private LocationOptionsController controller;

    @BeforeEach
    void provinces() {
        when(lookup.getProvinces()).thenReturn(List.of(NCR, PAMPANGA));
    }

    @Test
    void citiesAreLoadedByTheSelectedProvinceName() {
        when(lookup.getCities("1339")).thenReturn(List.of(MANILA));
        var model = new ExtendedModelMap();

        String view = controller.cities(" metro manila ", model);

        assertThat(view).isEqualTo("fragments/location-options :: cityResponse(cities=${cities})");
        assertThat(model.get("cities")).isEqualTo(List.of(MANILA));
    }

    @Test
    void unknownOrBlankProvinceGivesAnEmptyDisabledCityList() {
        var model = new ExtendedModelMap();

        controller.cities(null, model);

        assertThat(model.get("cities")).isEqualTo(List.of());
        verify(lookup, never()).getCities(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void barangaysResolveTheCityWithinItsProvince() {
        // "San Fernando" exists in more than one province; the province picks which.
        LocationOption sanFernando = new LocationOption("035416", "San Fernando", null);
        LocationOption dolores = new LocationOption("035416010", "Dolores", "2000");
        when(lookup.getCities("0354")).thenReturn(List.of(sanFernando));
        when(lookup.getBarangays("035416")).thenReturn(List.of(dolores));
        var model = new ExtendedModelMap();

        controller.barangays("Pampanga", "San Fernando", model);

        assertThat(model.get("barangays")).isEqualTo(List.of(dolores));
    }

    @Test
    void zipComesFromPsgcAndIsLeftAloneWhenUnknown() {
        when(lookup.getCities("1339")).thenReturn(List.of(MANILA));
        when(lookup.getBarangays("133901")).thenReturn(List.of(
                new LocationOption("133901659", "Barangay 659", "1000"),
                new LocationOption("133901660", "Barangay 660", null)));

        var model = new ExtendedModelMap();
        assertThat(controller.zip("Metro Manila", "Manila", "Barangay 659", model))
                .isEqualTo("fragments/location-options :: zipInput(zipCode=${zipCode})");
        assertThat(model.get("zipCode")).isEqualTo("1000");

        // 204 = htmx performs no swap, so a ZIP the user typed survives.
        Object noZip = controller.zip("Metro Manila", "Manila", "Barangay 660", new ExtendedModelMap());
        assertThat(((ResponseEntity<?>) noZip).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
