package ph.gov.phlpost.inventory.misddashboard.repository;

import ph.gov.phlpost.inventory.misddashboard.model.AssetAssignmentLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetAssignmentLogRepository extends JpaRepository<AssetAssignmentLog, Integer> {
}