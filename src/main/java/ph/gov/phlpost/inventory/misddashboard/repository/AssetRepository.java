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
            "WHERE asset.MaintenanceHealthStatus = 'Beyond Economic Repair (BER)' " +
            "AND COALESCE(asset.LifecycleStatus, '') <> 'Decommissioned / Retired' " +
            "ORDER BY asset.AssetTag ASC", nativeQuery = true)
    List<Asset> findProblematicAssets();
}