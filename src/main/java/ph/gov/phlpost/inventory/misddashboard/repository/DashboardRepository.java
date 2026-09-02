package ph.gov.phlpost.inventory.misddashboard.repository;

import ph.gov.phlpost.inventory.misddashboard.model.Asset;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;

/**
 * Aggregate counts for the dashboard cards.
 *
 * <p>
 * Typed against Asset purely to satisfy the Repository type parameters — every
 * method here is a native COUNT and none materializes an entity. It previously
 * named an AgingEquipmentReport entity mapped to a view, vw_agingequipment, that
 * is not present in the database; adding a single derived query would have
 * failed at runtime with nothing to warn you at compile time.
 */
public interface DashboardRepository extends Repository<Asset, String> {

        @Query(value = "SELECT COUNT(*) FROM Assets " +
                        "WHERE MaintenanceHealthStatus = 'Beyond Economic Repair (BER)' " +
                        "AND COALESCE(LifecycleStatus, '') <> 'Decommissioned / Retired'", nativeQuery = true)
        long countProblematicAssets();

        @Query(value = "SELECT COUNT(*) FROM Assets WHERE DeploymentStatus = 'Missing / Unaccounted'", nativeQuery = true)
        long countDeploymentIssues();

        @Query(value = "SELECT COUNT(*) FROM Assets " +
                        "WHERE MaintenanceHealthStatus = 'Under Repair' " +
                        "AND DeploymentStatus IN ('With Service Center', 'With MISD Technician')", nativeQuery = true)
        long countUnderMaintenanceAssets();

        @Query(value = "SELECT COUNT(*) FROM Assets WHERE LifecycleStatus = 'Decommissioned / Retired'", nativeQuery = true)
        long countDecommissionedRetiredAssets();

        @Query(value = "SELECT COUNT(*) FROM Assets", nativeQuery = true)
        long countTotalAssets();

        @Query(value = "SELECT COUNT(*) FROM Assets " +
                        "WHERE COALESCE(LifecycleStatus, '') <> 'Decommissioned / Retired'", nativeQuery = true)
        long countCurrentInventoryAssets();

        @Query(value = "SELECT COUNT(*) FROM Assets WHERE DeploymentStatus IN ('Deployed', 'Deployed / Assigned')", nativeQuery = true)
        long countDeployedAssets();

}