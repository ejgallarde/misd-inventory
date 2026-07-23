package ph.gov.phlpost.inventory.misddashboard.repository;

import ph.gov.phlpost.inventory.misddashboard.model.RealEstateProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface RealEstatePropertyRepository extends JpaRepository<RealEstateProperty, Integer> {

    @Query(value = "SELECT COALESCE(SUM(LotAreaSqm), 0) FROM RealEstateProperties", nativeQuery = true)
    BigDecimal sumTotalLandArea();

    @Query(value = "SELECT COUNT(*) FROM RealEstateProperties WHERE PropertyTaxStatus IN ('Pending', 'Overdue', 'Unpaid')", nativeQuery = true)
    long countPropertiesNeedingPayment();

    @Query(value = "SELECT * FROM RealEstateProperties WHERE PropertyTaxStatus IN ('Pending', 'Overdue', 'Unpaid')", nativeQuery = true)
    List<RealEstateProperty> findPropertiesNeedingPayment();
}