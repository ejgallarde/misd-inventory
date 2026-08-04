package ph.gov.phlpost.inventory.misddashboard.repository;

import ph.gov.phlpost.inventory.misddashboard.model.RealEstateProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface RealEstatePropertyRepository extends JpaRepository<RealEstateProperty, Integer> {

        @Query(value = "SELECT COALESCE(SUM(LotAreaSqm), 0) FROM RealEstateProperties", nativeQuery = true)
        BigDecimal sumTotalLandArea();

        @Query(value = "SELECT COALESCE(SUM(LotAreaSqm), 0) FROM RealEstateProperties " +
                        "WHERE UPPER(COALESCE(PropertyType, '')) = UPPER(:propertyType)", nativeQuery = true)
        BigDecimal sumTotalLandAreaByType(@Param("propertyType") String propertyType);

        @Query(value = "SELECT COUNT(*) FROM RealEstateProperties WHERE PropertyTaxStatus IN ('Pending', 'Overdue', 'Unpaid')", nativeQuery = true)
        long countPropertiesNeedingPayment();

        @Query(value = "SELECT * FROM RealEstateProperties WHERE PropertyTaxStatus IN ('Pending', 'Overdue', 'Unpaid')", nativeQuery = true)
        List<RealEstateProperty> findPropertiesNeedingPayment();

        @Query(value = "SELECT COUNT(*) FROM RealEstateProperties WHERE LegalTitlingStatus IN ('For Titling/Processing', 'Under Litigation/Dispute', 'No Title')", nativeQuery = true)
        long countPropertiesWithLegalIssues();

        @Query(value = "SELECT COUNT(*) FROM RealEstateProperties WHERE OperationalStatus IS NULL OR OperationalStatus <> 'Active/In Use'", nativeQuery = true)
        long countPropertiesWithOperationalIssues();

        @Query(value = "SELECT COUNT(*) FROM RealEstateProperties WHERE ConditionStatus IN ('Fair', 'Poor', 'Condemned')", nativeQuery = true)
        long countPropertiesWithConditionIssues();

        @Query(value = "SELECT COUNT(*) FROM RealEstateProperties WHERE " +
                        "PropertyTaxStatus IN ('Pending', 'Overdue', 'Unpaid') " +
                        "OR LegalTitlingStatus IN ('For Titling/Processing', 'Under Litigation/Dispute', 'No Title') " +
                        "OR OperationalStatus IS NULL OR OperationalStatus <> 'Active/In Use' " +
                        "OR ConditionStatus IN ('Fair', 'Poor', 'Condemned')", nativeQuery = true)
        long countProblematicProperties();

        @Query(value = "SELECT * FROM RealEstateProperties WHERE " +
                        "PropertyTaxStatus IN ('Pending', 'Overdue', 'Unpaid') " +
                        "OR LegalTitlingStatus IN ('For Titling/Processing', 'Under Litigation/Dispute', 'No Title') " +
                        "OR OperationalStatus IS NULL OR OperationalStatus <> 'Active/In Use' " +
                        "OR ConditionStatus IN ('Fair', 'Poor', 'Condemned')", nativeQuery = true)
        List<RealEstateProperty> findProblematicProperties();

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

        @Query(value = "SELECT COUNT(*) FROM RealEstateProperties WHERE " +
                        "UPPER(COALESCE(PropertyType, '')) = UPPER(:propertyType) AND (" +
                        "PropertyTaxStatus IN ('Pending', 'Overdue', 'Unpaid') " +
                        "OR LegalTitlingStatus IN ('For Titling/Processing', 'Under Litigation/Dispute', 'No Title') " +
                        "OR OperationalStatus IS NULL OR OperationalStatus <> 'Active/In Use' " +
                        "OR ConditionStatus IN ('Fair', 'Poor', 'Condemned'))", nativeQuery = true)
        long countProblematicPropertiesByType(@Param("propertyType") String propertyType);

        @Query(value = "SELECT COUNT(*) FROM RealEstateProperties WHERE " +
                        "UPPER(COALESCE(PropertyType, '')) <> UPPER(:propertyType) AND (" +
                        "PropertyTaxStatus IN ('Pending', 'Overdue', 'Unpaid') " +
                        "OR LegalTitlingStatus IN ('For Titling/Processing', 'Under Litigation/Dispute', 'No Title') " +
                        "OR OperationalStatus IS NULL OR OperationalStatus <> 'Active/In Use' " +
                        "OR ConditionStatus IN ('Fair', 'Poor', 'Condemned'))", nativeQuery = true)
        long countProblematicPropertiesByTypeExcluding(@Param("propertyType") String propertyType);

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

        long countByOperationalStatus(String operationalStatus);

        long countByOperationalStatusNot(String operationalStatus);
}