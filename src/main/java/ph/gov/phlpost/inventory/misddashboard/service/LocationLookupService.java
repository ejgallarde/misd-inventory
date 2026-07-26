package ph.gov.phlpost.inventory.misddashboard.service;

import ph.gov.phlpost.inventory.misddashboard.model.LocationOption;
import ph.gov.phlpost.inventory.misddashboard.repository.PsgcBarangayRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.PsgcCityMunicipalityRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.PsgcProvinceRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationLookupService {

    private final PsgcProvinceRepository provinceRepository;
    private final PsgcCityMunicipalityRepository cityMunicipalityRepository;
    private final PsgcBarangayRepository barangayRepository;

    public LocationLookupService(PsgcProvinceRepository provinceRepository,
            PsgcCityMunicipalityRepository cityMunicipalityRepository,
            PsgcBarangayRepository barangayRepository) {
        this.provinceRepository = provinceRepository;
        this.cityMunicipalityRepository = cityMunicipalityRepository;
        this.barangayRepository = barangayRepository;
    }

    @Cacheable("psgcProvinces")
    public List<LocationOption> getProvinces() {
        return provinceRepository.findAllByOrderByProvinceNameAsc().stream()
                .map(province -> new LocationOption(
                        province.getProvinceCode(),
                        province.getProvinceName(),
                        null))
                .toList();
    }

    @Cacheable(value = "psgcCities", key = "#provinceCode")
    public List<LocationOption> getCities(String provinceCode) {
        return cityMunicipalityRepository.findByProvinceCodeOrderByCityMunicipalityNameAsc(provinceCode).stream()
                .map(cityMunicipality -> new LocationOption(
                        cityMunicipality.getCityMunicipalityCode(),
                        cityMunicipality.getCityMunicipalityName(),
                        null))
                .toList();
    }

    @Cacheable(value = "psgcBarangays", key = "#cityMunicipalityCode")
    public List<LocationOption> getBarangays(String cityMunicipalityCode) {
        return barangayRepository.findByCityMunicipalityCodeOrderByBarangayNameAsc(cityMunicipalityCode).stream()
                .map(barangay -> new LocationOption(
                        barangay.getBarangayCode(),
                        barangay.getBarangayName(),
                        barangay.getZipCode()))
                .toList();
    }
}
