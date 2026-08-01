package ph.gov.phlpost.inventory.misddashboard.repository;

import ph.gov.phlpost.inventory.misddashboard.model.LifecycleAuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LifecycleAuditLogRepository extends JpaRepository<LifecycleAuditLog, Integer> {
    List<LifecycleAuditLog> findByReferenceIDOrderByTransactionDateDescLogIDDesc(String referenceID);
}