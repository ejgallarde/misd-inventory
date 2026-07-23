package ph.gov.phlpost.inventory.misddashboard.repository;

import ph.gov.phlpost.inventory.misddashboard.model.AgingEquipmentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DashboardRepository extends JpaRepository<AgingEquipmentReport, String> {

    // The JpaRepository automatically gives findAll() for aging equipment view.

    @Query(value = "SELECT COUNT(*) FROM Assets", nativeQuery = true)
    long countTotalAssets();

    @Query(value = "SELECT COUNT(*) FROM Assets WHERE CurrentStatus = 'Deployed'", nativeQuery = true)
    long countDeployedAssets();

    @Query(value = "SELECT COUNT(DISTINCT AssetTag) FROM AssetMaintenance", nativeQuery = true)
    long countMaintenanceAssets();
}