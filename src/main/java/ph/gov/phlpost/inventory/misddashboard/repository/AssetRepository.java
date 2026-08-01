package ph.gov.phlpost.inventory.misddashboard.repository;

import ph.gov.phlpost.inventory.misddashboard.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, String> {

    // Add this inside your AssetRepository interface
    Optional<Asset> findTopByAssetTagStartingWithOrderByAssetTagDesc(String prefix);

    @Query(value = "SELECT asset.* FROM Assets asset " +
            "WHERE asset.DeploymentStatus = 'Missing / Unaccounted' " +
            "OR asset.MaintenanceHealthStatus IN ('Degraded', 'Under Repair', 'Beyond Economic Repair (BER)') " +
            "OR asset.LifecycleStatus IN ('End of Life (EOL)', 'Decommissioned / Retired', 'Disposed', 'Sold') " +
            "OR (asset.PurchaseDate IS NOT NULL AND TIMESTAMPDIFF(YEAR, asset.PurchaseDate, CURDATE()) >= 10) " +
            "ORDER BY asset.AssetTag ASC", nativeQuery = true)
    List<Asset> findProblematicAssets();
}