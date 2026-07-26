package ph.gov.phlpost.inventory.misddashboard.repository;

import ph.gov.phlpost.inventory.misddashboard.model.PsgcProvince;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PsgcProvinceRepository extends JpaRepository<PsgcProvince, String> {
    List<PsgcProvince> findAllByOrderByProvinceNameAsc();
}
