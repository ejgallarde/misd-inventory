package ph.gov.phlpost.inventory.misddashboard.repository;

import ph.gov.phlpost.inventory.misddashboard.model.RealEstateProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface RealEstatePropertyRepository extends JpaRepository<RealEstateProperty, Integer> {

        @Query(value = "SELECT COALESCE(SUM(LotAreaSqm), 0) FROM RealEstateProperties " +
                        "WHERE UPPER(COALESCE(PropertyType, '')) = UPPER(:propertyType)", nativeQuery = true)
        BigDecimal sumTotalLandAreaByType(@Param("propertyType") String propertyType);

        long countByPropertyTypeIgnoreCase(String propertyType);

        @Query(value = "SELECT COUNT(*) FROM RealEstateProperties " +
                        "WHERE UPPER(COALESCE(PropertyType, '')) <> UPPER(:propertyType)", nativeQuery = true)
        long countByPropertyTypeExcluding(@Param("propertyType") String propertyType);

        @Query(value = "SELECT COUNT(*) FROM RealEstateProperties " +
                        "WHERE UPPER(COALESCE(PropertyType, '')) = UPPER(:propertyType) " +
                        "AND COALESCE(OperationalStatus, '') <> :status", nativeQuery = true)
        long countByTypeAndOperationalStatusNot(@Param("propertyType") String propertyType,
                        @Param("status") String status);

        @Query(value = "SELECT COUNT(*) FROM RealEstateProperties " +
                        "WHERE UPPER(COALESCE(PropertyType, '')) <> UPPER(:propertyType) " +
                        "AND COALESCE(OperationalStatus, '') <> :status", nativeQuery = true)
        long countByTypeExcludingAndOperationalStatusNot(@Param("propertyType") String propertyType,
                        @Param("status") String status);

        @Query(value = "SELECT COUNT(*) FROM RealEstateProperties " +
                        "WHERE UPPER(COALESCE(PropertyType, '')) = UPPER(:propertyType) " +
                        "AND OperationalStatus = :status", nativeQuery = true)
        long countByTypeAndOperationalStatus(@Param("propertyType") String propertyType,
                        @Param("status") String status);

        @Query(value = "SELECT COUNT(*) FROM RealEstateProperties " +
                        "WHERE UPPER(COALESCE(PropertyType, '')) <> UPPER(:propertyType) " +
                        "AND OperationalStatus = :status", nativeQuery = true)
        long countByTypeExcludingAndOperationalStatus(@Param("propertyType") String propertyType,
                        @Param("status") String status);



        @Query(value = "SELECT * FROM RealEstateProperties WHERE " +
                        "UPPER(COALESCE(PropertyType, '')) = UPPER(:propertyType) AND (" +
                        "PropertyTaxStatus IN ('Pending', 'Overdue', 'Unpaid') " +
                        "OR LegalTitlingStatus IN ('For Titling/Processing', 'Under Litigation/Dispute', 'No Title') " +
                        "OR OperationalStatus IS NULL OR OperationalStatus <> 'Active/In Use' " +
                        "OR ConditionStatus IN ('Fair', 'Poor', 'Condemned'))", nativeQuery = true)
        List<RealEstateProperty> findProblematicPropertiesByType(@Param("propertyType") String propertyType);

        @Query(value = "SELECT * FROM RealEstateProperties WHERE " +
                        "UPPER(COALESCE(PropertyType, '')) <> UPPER(:propertyType) AND (" +
                        "PropertyTaxStatus IN ('Pending', 'Overdue', 'Unpaid') " +
                        "OR LegalTitlingStatus IN ('For Titling/Processing', 'Under Litigation/Dispute', 'No Title') " +
                        "OR OperationalStatus IS NULL OR OperationalStatus <> 'Active/In Use' " +
                        "OR ConditionStatus IN ('Fair', 'Poor', 'Condemned'))", nativeQuery = true)
        List<RealEstateProperty> findProblematicPropertiesByTypeExcluding(@Param("propertyType") String propertyType);
}