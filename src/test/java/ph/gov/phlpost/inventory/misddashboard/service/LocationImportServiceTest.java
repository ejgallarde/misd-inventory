package ph.gov.phlpost.inventory.misddashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.mock.web.MockMultipartFile;

import ph.gov.phlpost.inventory.misddashboard.model.PsgcImportResult;
import ph.gov.phlpost.inventory.misddashboard.repository.PsgcBarangayRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.PsgcCityMunicipalityRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.PsgcProvinceRepository;

@ExtendWith(MockitoExtension.class)
class LocationImportServiceTest {

    @Mock
    private PsgcProvinceRepository provinceRepository;

    @Mock
    private PsgcCityMunicipalityRepository cityMunicipalityRepository;

    @Mock
    private PsgcBarangayRepository barangayRepository;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private LocationImportService locationImportService;

    private void stubCaches() {
        Cache cache = mock(Cache.class);
        lenient().when(cacheManager.getCache("psgcProvinces")).thenReturn(cache);
        lenient().when(cacheManager.getCache("psgcCities")).thenReturn(cache);
        lenient().when(cacheManager.getCache("psgcBarangays")).thenReturn(cache);
    }

    @Test
    void singleCsvLinksProvinceCityAndBarangayByDerivedCode() throws IOException {
        stubCaches();
        String csv = String.join("\n",
                "0102800000,Ilocos Norte,,Prov",
                "0102805000,Laoag City,,City",
                "0102805001,Barangay 1,,Bgy",
                "");
        MockMultipartFile file = new MockMultipartFile("psgcFile", "psgc.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        PsgcImportResult result = locationImportService.importFromSinglePsgcCsv(file);

        assertThat(result.provinces()).isEqualTo(1);
        assertThat(result.citiesMunicipalities()).isEqualTo(1);
        assertThat(result.barangays()).isEqualTo(1);
        verify(provinceRepository).deleteAllInBatch();
        verify(cityMunicipalityRepository).deleteAllInBatch();
        verify(barangayRepository).deleteAllInBatch();
        verify(provinceRepository).saveAll(anyList());
        verify(cityMunicipalityRepository).saveAll(anyList());
        verify(barangayRepository).saveAll(anyList());
    }

    @Test
    void singleCsvSynthesizesAFallbackProvinceWhenOnlyACityRowIsPresent() throws IOException {
        stubCaches();
        String csv = "0102805000,Laoag City,,City\n";
        MockMultipartFile file = new MockMultipartFile("psgcFile", "psgc.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        PsgcImportResult result = locationImportService.importFromSinglePsgcCsv(file);

        // No explicit PROV row exists, but the city still needs a parent to satisfy
        // the FK, so importFromSinglePsgcCsv fabricates one from the derived code.
        assertThat(result.provinces()).isEqualTo(1);
        assertThat(result.citiesMunicipalities()).isEqualTo(1);
    }

    @Test
    void singleCsvDropsABarangayWhoseCityWasNotSaved() throws IOException {
        stubCaches();
        // The barangay's derived city code (0102805000) never appears as a city row,
        // so it must be filtered out rather than saved with a dangling reference.
        String csv = "0102805001,Orphan Barangay,,Bgy\n";
        MockMultipartFile file = new MockMultipartFile("psgcFile", "psgc.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        PsgcImportResult result = locationImportService.importFromSinglePsgcCsv(file);

        assertThat(result.barangays()).isEqualTo(0);
    }

    @Test
    void singleCsvIgnoresMalformedAndTooShortLines() throws IOException {
        stubCaches();
        String csv = String.join("\n",
                "Code,Name,Level,Type", // header: does not start with a digit
                "12,Too Short,,Prov", // fewer than 12 characters
                "");
        MockMultipartFile file = new MockMultipartFile("psgcFile", "psgc.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        PsgcImportResult result = locationImportService.importFromSinglePsgcCsv(file);

        assertThat(result.provinces()).isEqualTo(0);
        assertThat(result.citiesMunicipalities()).isEqualTo(0);
        assertThat(result.barangays()).isEqualTo(0);
    }

    @Test
    void threeFileImportSkipsTheHeaderRowOfEachFile() throws IOException {
        stubCaches();
        MockMultipartFile provinces = new MockMultipartFile("provincesFile", "provinces.csv", "text/csv",
                "ProvinceCode,ProvinceName\n01,Ilocos Norte\n".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile cities = new MockMultipartFile("citiesFile", "cities.csv", "text/csv",
                "CityCode,ProvinceCode,CityName\n0102,01,Laoag City\n".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile barangays = new MockMultipartFile("barangaysFile", "barangays.csv", "text/csv",
                "BarangayCode,CityCode,BarangayName,ZipCode\n010203,0102,Barangay 1,2900\n"
                        .getBytes(StandardCharsets.UTF_8));

        PsgcImportResult result = locationImportService.importFromCsv(provinces, cities, barangays);

        assertThat(result).isEqualTo(new PsgcImportResult(1, 1, 1));
    }
}
