package ph.gov.phlpost.inventory.misddashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import ph.gov.phlpost.inventory.misddashboard.model.LocationOption;
import ph.gov.phlpost.inventory.misddashboard.model.PsgcImportResult;
import ph.gov.phlpost.inventory.misddashboard.service.LocationImportService;
import ph.gov.phlpost.inventory.misddashboard.service.LocationLookupService;

@ExtendWith(MockitoExtension.class)
class LocationControllerTest {

    @Mock
    private LocationLookupService locationLookupService;

    @Mock
    private LocationImportService locationImportService;

    @InjectMocks
    private LocationController controller;

    @Test
    void listProvincesDelegatesToTheLookupService() {
        when(locationLookupService.getProvinces()).thenReturn(List.of(new LocationOption("01", "Ilocos Norte", null)));

        List<LocationOption> provinces = controller.listProvinces();

        assertThat(provinces).extracting(LocationOption::code).containsExactly("01");
    }

    @Test
    void listCitiesReturnsEmptyWhenNoProvinceCodeIsGiven() {
        List<LocationOption> cities = controller.listCities(null);

        assertThat(cities).isEmpty();
    }

    @Test
    void listCitiesDelegatesToTheLookupServiceWhenAProvinceCodeIsGiven() {
        when(locationLookupService.getCities("01")).thenReturn(List.of(new LocationOption("0102", "Laoag City", null)));

        List<LocationOption> cities = controller.listCities("01");

        assertThat(cities).extracting(LocationOption::code).containsExactly("0102");
    }

    @Test
    void listBarangaysReturnsEmptyWhenNoCityCodeIsGiven() {
        assertThat(controller.listBarangays("   ")).isEmpty();
    }

    @Test
    void listBarangaysDelegatesToTheLookupServiceWhenACityCodeIsGiven() {
        when(locationLookupService.getBarangays("0102"))
                .thenReturn(List.of(new LocationOption("010203", "Barangay 1", "2900")));

        List<LocationOption> barangays = controller.listBarangays("0102");

        assertThat(barangays).extracting(LocationOption::code).containsExactly("010203");
    }

    @Test
    void importPsgcCsvReturnsTheSummaryOnSuccess() throws IOException {
        MockMultipartFile provinces = new MockMultipartFile("provincesFile", "p.csv", "text/csv", "x".getBytes());
        MockMultipartFile cities = new MockMultipartFile("citiesMunicipalitiesFile", "c.csv", "text/csv", "x".getBytes());
        MockMultipartFile barangays = new MockMultipartFile("barangaysFile", "b.csv", "text/csv", "x".getBytes());
        when(locationImportService.importFromCsv(provinces, cities, barangays))
                .thenReturn(new PsgcImportResult(1, 2, 3));

        ResponseEntity<Map<String, Object>> response = controller.importPsgcCsv(provinces, cities, barangays);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("summary", new PsgcImportResult(1, 2, 3));
    }

    @Test
    void importPsgcCsvReturnsBadRequestWhenParsingFails() throws IOException {
        MockMultipartFile provinces = new MockMultipartFile("provincesFile", "p.csv", "text/csv", "x".getBytes());
        MockMultipartFile cities = new MockMultipartFile("citiesMunicipalitiesFile", "c.csv", "text/csv", "x".getBytes());
        MockMultipartFile barangays = new MockMultipartFile("barangaysFile", "b.csv", "text/csv", "x".getBytes());
        when(locationImportService.importFromCsv(provinces, cities, barangays))
                .thenThrow(new IOException("bad csv"));

        ResponseEntity<Map<String, Object>> response = controller.importPsgcCsv(provinces, cities, barangays);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().get("error")).asString().contains("bad csv");
    }

    @Test
    void importSinglePsgcCsvReturnsInternalServerErrorOnUnexpectedFailure() throws IOException {
        MockMultipartFile file = new MockMultipartFile("psgcFile", "psgc.csv", "text/csv", "x".getBytes());
        when(locationImportService.importFromSinglePsgcCsv(file)).thenThrow(new RuntimeException("boom"));

        ResponseEntity<Map<String, Object>> response = controller.importSinglePsgcCsv(file);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
    }

    @Test
    void importSinglePsgcCsvReturnsTheSummaryOnSuccess() throws IOException {
        MockMultipartFile file = new MockMultipartFile("psgcFile", "psgc.csv", "text/csv", "x".getBytes());
        when(locationImportService.importFromSinglePsgcCsv(file)).thenReturn(new PsgcImportResult(4, 5, 6));

        ResponseEntity<Map<String, Object>> response = controller.importSinglePsgcCsv(file);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("summary", new PsgcImportResult(4, 5, 6));
    }
}
