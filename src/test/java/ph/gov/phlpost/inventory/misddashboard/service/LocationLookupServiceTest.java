package ph.gov.phlpost.inventory.misddashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ph.gov.phlpost.inventory.misddashboard.model.LocationOption;
import ph.gov.phlpost.inventory.misddashboard.model.PsgcBarangay;
import ph.gov.phlpost.inventory.misddashboard.model.PsgcCityMunicipality;
import ph.gov.phlpost.inventory.misddashboard.model.PsgcProvince;
import ph.gov.phlpost.inventory.misddashboard.repository.PsgcBarangayRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.PsgcCityMunicipalityRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.PsgcProvinceRepository;

@ExtendWith(MockitoExtension.class)
class LocationLookupServiceTest {

    @Mock
    private PsgcProvinceRepository provinceRepository;

    @Mock
    private PsgcCityMunicipalityRepository cityMunicipalityRepository;

    @Mock
    private PsgcBarangayRepository barangayRepository;

    @InjectMocks
    private LocationLookupService locationLookupService;

    @Test
    void provincesAreMappedToLocationOptionsWithNoZipCode() {
        PsgcProvince province = new PsgcProvince();
        province.setProvinceCode("01");
        province.setProvinceName("Ilocos Norte");
        when(provinceRepository.findAllByOrderByProvinceNameAsc()).thenReturn(List.of(province));

        List<LocationOption> options = locationLookupService.getProvinces();

        assertThat(options).containsExactly(new LocationOption("01", "Ilocos Norte", null));
    }

    @Test
    void citiesAreScopedToTheRequestedProvince() {
        PsgcCityMunicipality city = new PsgcCityMunicipality();
        city.setCityMunicipalityCode("0102");
        city.setProvinceCode("01");
        city.setCityMunicipalityName("Laoag City");
        when(cityMunicipalityRepository.findByProvinceCodeOrderByCityMunicipalityNameAsc("01"))
                .thenReturn(List.of(city));

        List<LocationOption> options = locationLookupService.getCities("01");

        assertThat(options).containsExactly(new LocationOption("0102", "Laoag City", null));
    }

    @Test
    void barangaysCarryTheirZipCode() {
        PsgcBarangay barangay = new PsgcBarangay();
        barangay.setBarangayCode("010203");
        barangay.setCityMunicipalityCode("0102");
        barangay.setBarangayName("Barangay 1");
        barangay.setZipCode("2900");
        when(barangayRepository.findByCityMunicipalityCodeOrderByBarangayNameAsc("0102"))
                .thenReturn(List.of(barangay));

        List<LocationOption> options = locationLookupService.getBarangays("0102");

        assertThat(options).containsExactly(new LocationOption("010203", "Barangay 1", "2900"));
    }
}
