package ph.gov.phlpost.inventory.misddashboard.repository;

import ph.gov.phlpost.inventory.misddashboard.model.PsgcBarangay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PsgcBarangayRepository extends JpaRepository<PsgcBarangay, String> {
    List<PsgcBarangay> findByCityMunicipalityCodeOrderByBarangayNameAsc(String cityMunicipalityCode);
}
