package ph.gov.phlpost.inventory.misddashboard.repository;

import ph.gov.phlpost.inventory.misddashboard.model.AssetAssignmentLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AssetAssignmentLogRepository extends JpaRepository<AssetAssignmentLog, Integer> {
    List<AssetAssignmentLog> findByAssetTagOrderByTransactionDateDescTransactionIDDesc(String assetTag);

    /**
     * One (AssetTag, DocumentNo) row per asset/vehicle/survey-asset, taken from
     * its most recent assignment transaction (by TransactionID) - the PAR/PTR/ICS
     * backing current custody, for display on the table views. AssetTag values
     * are unique across reference types (natural asset tag, or VEHICLE-{id} /
     * SURVEYASSET-{id}), so one query covers all three domains at once.
     */
    @Query(value = "SELECT aa.AssetTag, aa.DocumentNo FROM assetassignments aa " +
            "INNER JOIN (SELECT AssetTag, MAX(TransactionID) AS MaxTransactionID FROM assetassignments GROUP BY AssetTag) latest "
            +
            "ON aa.AssetTag = latest.AssetTag AND aa.TransactionID = latest.MaxTransactionID", nativeQuery = true)
    List<Object[]> findLatestDocumentNoPerAssetTag();
}