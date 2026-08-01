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
                        "COALESCE(TIMESTAMPDIFF(YEAR, a.PurchaseDate, CURDATE()), 0) AS AgeInYears, " +
                        "COALESCE(a.DeploymentStatus, '') AS DeploymentStatus, " +
                        "COALESCE(a.MaintenanceHealthStatus, '') AS MaintenanceHealthStatus, " +
                        "COALESCE(a.LifecycleStatus, '') AS LifecycleStatus " +
                        "FROM Assets a " +
                        "LEFT JOIN EquipmentCatalog c ON c.CatalogID = a.CatalogID " +
                        "LEFT JOIN Personnel p ON p.EmployeeID = a.CurrentOwnerID " +
                        "WHERE " +
                        "a.DeploymentStatus = 'Missing / Unaccounted' " +
                        "OR a.MaintenanceHealthStatus IN ('Degraded', 'Under Repair', 'Beyond Economic Repair (BER)') "
                        +
                        "OR a.LifecycleStatus IN ('End of Life (EOL)', 'Decommissioned / Retired', 'Disposed', 'Sold') "
                        +
                        "OR (a.PurchaseDate IS NOT NULL AND TIMESTAMPDIFF(YEAR, a.PurchaseDate, CURDATE()) >= 10) " +
                        "ORDER BY AgeInYears DESC, a.AssetTag ASC", nativeQuery = true)
        List<AgingEquipmentReport> findProblematicAssets();

        @Query(value = "SELECT COUNT(*) FROM Assets WHERE " +
                        "DeploymentStatus = 'Missing / Unaccounted' " +
                        "OR MaintenanceHealthStatus IN ('Degraded', 'Under Repair', 'Beyond Economic Repair (BER)') "
                        +
                        "OR LifecycleStatus IN ('End of Life (EOL)', 'Decommissioned / Retired', 'Disposed', 'Sold') " +
                        "OR (PurchaseDate IS NOT NULL AND TIMESTAMPDIFF(YEAR, PurchaseDate, CURDATE()) >= 10)", nativeQuery = true)
        long countProblematicAssets();

        @Query(value = "SELECT COUNT(*) FROM Assets WHERE DeploymentStatus = 'Missing / Unaccounted'", nativeQuery = true)
        long countDeploymentIssues();

        @Query(value = "SELECT COUNT(*) FROM Assets WHERE MaintenanceHealthStatus IN ('Degraded', 'Under Repair', 'Beyond Economic Repair (BER)')", nativeQuery = true)
        long countMaintenanceIssues();

        @Query(value = "SELECT COUNT(*) FROM Assets WHERE LifecycleStatus = 'Decommissioned / Retired'", nativeQuery = true)
        long countDecommissionedRetiredAssets();

        @Query(value = "SELECT COUNT(*) FROM Assets", nativeQuery = true)
        long countTotalAssets();

        @Query(value = "SELECT COUNT(*) FROM Assets WHERE DeploymentStatus IN ('Deployed', 'Deployed / Assigned')", nativeQuery = true)
        long countDeployedAssets();

        @Query(value = "SELECT COUNT(*) FROM Assets WHERE MaintenanceHealthStatus IN ('Degraded', 'Under Repair', 'Beyond Economic Repair (BER)')", nativeQuery = true)
        long countMaintenanceAssets();
}