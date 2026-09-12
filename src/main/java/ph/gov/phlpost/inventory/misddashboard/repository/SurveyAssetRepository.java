package ph.gov.phlpost.inventory.misddashboard.repository;

import ph.gov.phlpost.inventory.misddashboard.model.SurveyAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SurveyAssetRepository extends JpaRepository<SurveyAsset, Integer> {

        Optional<SurveyAsset> findTopByAssetTagStartingWithOrderByAssetTagDesc(String prefix);

        // Equipment age is measured from AcquisitionDate; calibration due date
        // replaces Fleet's registration expiry as the recurring compliance check.

        @Query(value = "SELECT COUNT(*) FROM SurveyAssets WHERE COALESCE(AdminLegalStatus, '') NOT IN ('Sold', 'Disposed', 'Decommissioned')", nativeQuery = true)
        long countCurrentInventorySurveyAssets();

        @Query(value = "SELECT COUNT(*) FROM SurveyAssets WHERE OperationalStatus = 'Available/Idle'", nativeQuery = true)
        long countAvailableIdleSurveyAssets();

        @Query(value = "SELECT COUNT(*) FROM SurveyAssets WHERE OperationalStatus = 'Deployed/Field Use'", nativeQuery = true)
        long countDeployedSurveyAssets();

        @Query(value = "SELECT COUNT(*) FROM SurveyAssets WHERE ConditionStatus = 'Under Repair'", nativeQuery = true)
        long countUnderRepairSurveyAssets();

        @Query(value = "SELECT COUNT(*) FROM SurveyAssets WHERE ConditionStatus = 'Needs Calibration'", nativeQuery = true)
        long countNeedsCalibrationSurveyAssets();

        @Query(value = "SELECT COUNT(*) FROM SurveyAssets WHERE OperationalStatus = 'Slated for Disposal'", nativeQuery = true)
        long countSlatedForDisposalSurveyAssets();

        @Query(value = "SELECT COUNT(*) FROM SurveyAssets WHERE AdminLegalStatus IN ('Disposed', 'Decommissioned', 'Sold')", nativeQuery = true)
        long countDecommissionedSurveyAssets();

        @Query(value = "SELECT COUNT(*) FROM SurveyAssets WHERE AdminLegalStatus IN ('Under Investigation')", nativeQuery = true)
        long countSurveyAssetsWithAdminLegalIssues();

        @Query(value = "SELECT COUNT(*) FROM SurveyAssets WHERE COALESCE(AdminLegalStatus, '') NOT IN ('Sold', 'Disposed', 'Decommissioned') AND (OperationalStatus IN ('Missing', 'Stolen', 'Slated for Disposal') OR ConditionStatus IN ('Needs Calibration', 'Under Repair', 'Beyond Economic Repair (BER)'))", nativeQuery = true)
        long countSurveyAssetsWithOperationalConditionIssues();

        @Query(value = "SELECT COUNT(*) FROM SurveyAssets WHERE AdminLegalStatus = 'Sold'", nativeQuery = true)
        long countSoldSurveyAssets();

        @Query(value = "SELECT COUNT(*) FROM SurveyAssets WHERE AdminLegalStatus IN ('Disposed', 'Decommissioned')", nativeQuery = true)
        long countDisposedOrDecommissionedSurveyAssets();

        @Query(value = "SELECT COUNT(*) FROM SurveyAssets WHERE CalibrationDueDate BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)", nativeQuery = true)
        long countCalibrationDueSurveyAssets();

        @Query(value = "SELECT COUNT(*) FROM SurveyAssets WHERE " +
                        "COALESCE(AdminLegalStatus, '') NOT IN ('Sold', 'Disposed', 'Decommissioned') AND (" +
                        "(YEAR(CURDATE()) - YEAR(AcquisitionDate)) >= 10 " +
                        "OR CalibrationDueDate BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY) " +
                        "OR AdminLegalStatus IN ('Under Investigation') " +
                        "OR OperationalStatus IN ('Missing', 'Stolen', 'Slated for Disposal') " +
                        "OR ConditionStatus IN ('Needs Calibration', 'Under Repair', 'Beyond Economic Repair (BER)')"
                        +
                        ")", nativeQuery = true)
        long countProblematicSurveyAssets();

        @Query(value = "SELECT * FROM SurveyAssets WHERE " +
                        "COALESCE(AdminLegalStatus, '') NOT IN ('Sold', 'Disposed', 'Decommissioned') AND (" +
                        "(YEAR(CURDATE()) - YEAR(AcquisitionDate)) >= 10 " +
                        "OR CalibrationDueDate BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY) " +
                        "OR AdminLegalStatus IN ('Under Investigation') " +
                        "OR OperationalStatus IN ('Missing', 'Stolen', 'Slated for Disposal') " +
                        "OR ConditionStatus IN ('Needs Calibration', 'Under Repair', 'Beyond Economic Repair (BER)')"
                        +
                        ")", nativeQuery = true)
        List<SurveyAsset> findProblematicSurveyAssets();
}
