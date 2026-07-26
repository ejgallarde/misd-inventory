package ph.gov.phlpost.inventory.misddashboard.repository;

import ph.gov.phlpost.inventory.misddashboard.model.AgingEquipmentReport;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DashboardRepository extends Repository<AgingEquipmentReport, String> {

    @Query(value = "SELECT " +
            "a.AssetTag AS AssetTag, " +
            "COALESCE(c.Category, 'Unknown') AS Category, " +
            "COALESCE(c.ModelName, 'Unknown') AS ModelName, " +
            "CASE " +
            "  WHEN p.EmployeeID IS NULL THEN 'MISD Storage' " +
            "  ELSE CONCAT(COALESCE(p.LastName, ''), ', ', COALESCE(p.FirstName, '')) " +
            "END AS AccountableOwner, " +
            "a.PurchaseDate AS PurchaseDate, " +
            "TIMESTAMPDIFF(YEAR, a.PurchaseDate, CURDATE()) AS AgeInYears " +
            "FROM Assets a " +
            "LEFT JOIN EquipmentCatalog c ON c.CatalogID = a.CatalogID " +
            "LEFT JOIN Personnel p ON p.EmployeeID = a.CurrentOwnerID " +
            "WHERE a.PurchaseDate IS NOT NULL " +
            "AND TIMESTAMPDIFF(YEAR, a.PurchaseDate, CURDATE()) >= 10 " +
            "ORDER BY AgeInYears DESC, a.AssetTag ASC", nativeQuery = true)
    List<AgingEquipmentReport> findAgingAssets();

    @Query(value = "SELECT COUNT(*) FROM Assets " +
            "WHERE PurchaseDate IS NOT NULL " +
            "AND TIMESTAMPDIFF(YEAR, PurchaseDate, CURDATE()) >= 10", nativeQuery = true)
    long countAgingAssets();

    @Query(value = "SELECT COUNT(*) FROM Assets", nativeQuery = true)
    long countTotalAssets();

    @Query(value = "SELECT COUNT(*) FROM Assets WHERE DeploymentStatus = 'Deployed / Assigned'", nativeQuery = true)
    long countDeployedAssets();

    @Query(value = "SELECT COUNT(*) FROM Assets WHERE MaintenanceHealthStatus IN ('Degraded', 'Under Repair', 'Awaiting Parts', 'Beyond Economic Repair (BER)')", nativeQuery = true)
    long countMaintenanceAssets();
}