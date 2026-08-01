package ph.gov.phlpost.inventory.misddashboard.repository;

import ph.gov.phlpost.inventory.misddashboard.model.AgingEquipmentReport;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;

public interface DashboardRepository extends Repository<AgingEquipmentReport, String> {

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