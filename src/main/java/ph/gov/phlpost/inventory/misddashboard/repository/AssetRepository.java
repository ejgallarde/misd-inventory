package ph.gov.phlpost.inventory.misddashboard.repository;

import ph.gov.phlpost.inventory.misddashboard.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, String> {

    Optional<Asset> findTopByAssetTagStartingWithOrderByAssetTagDesc(String prefix);

    /**
     * Assets needing attention: BER units awaiting disposal, and units nobody can
     * account for. Missing assets used to be counted on the dashboard but never
     * listed, so there was nothing to click through to.
     */
    String PROBLEM_PREDICATE = "COALESCE(asset.LifecycleStatus, '') NOT IN ('Decommissioned / Retired', 'Disposed', 'Sold') "
            + "AND (asset.MaintenanceHealthStatus = 'Beyond Economic Repair (BER)' "
            + "OR asset.DeploymentStatus = 'Missing / Unaccounted')";

    @Query(value = "SELECT asset.* FROM Assets asset WHERE " + PROBLEM_PREDICATE
            + " ORDER BY asset.AssetTag ASC", nativeQuery = true)
    List<Asset> findProblematicAssets();
}