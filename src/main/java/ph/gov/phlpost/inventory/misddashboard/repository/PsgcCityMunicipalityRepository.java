package ph.gov.phlpost.inventory.misddashboard.repository;

import ph.gov.phlpost.inventory.misddashboard.model.PsgcCityMunicipality;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PsgcCityMunicipalityRepository extends JpaRepository<PsgcCityMunicipality, String> {
    List<PsgcCityMunicipality> findByProvinceCodeOrderByCityMunicipalityNameAsc(String provinceCode);
}
