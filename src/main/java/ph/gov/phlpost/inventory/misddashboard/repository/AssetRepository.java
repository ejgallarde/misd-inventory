package ph.gov.phlpost.inventory.misddashboard.repository;

import ph.gov.phlpost.inventory.misddashboard.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, String> {

    // Add this inside your AssetRepository interface
    Optional<Asset> findTopByAssetTagStartingWithOrderByAssetTagDesc(String prefix);
}