package ph.gov.phlpost.inventory.misddashboard.service;

import ph.gov.phlpost.inventory.misddashboard.model.AssetAssignmentLog;
import ph.gov.phlpost.inventory.misddashboard.model.LifecycleAuditLog;
import ph.gov.phlpost.inventory.misddashboard.repository.AssetAssignmentLogRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.LifecycleAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class AuditLogService {

    private final AssetAssignmentLogRepository assignmentLogRepo;
    private final LifecycleAuditLogRepository lifecycleLogRepo;

    public AuditLogService(AssetAssignmentLogRepository assignmentLogRepo,
            LifecycleAuditLogRepository lifecycleLogRepo) {
        this.assignmentLogRepo = assignmentLogRepo;
        this.lifecycleLogRepo = lifecycleLogRepo;
    }

    @Transactional
    public void logAssignment(String referenceId, String employeeId, String actionType, String notes) {
        AssetAssignmentLog log = new AssetAssignmentLog();
        log.setAssetTag(referenceId);
        log.setEmployeeID(employeeId);
        log.setActionType(actionType);
        log.setTransactionDate(LocalDateTime.now());
        log.setConditionNotes(notes);
        assignmentLogRepo.save(log);
    }

    @Transactional
    public void logLifecycleEvent(String referenceId, String performedBy, String actionType, String notes) {
        LifecycleAuditLog log = new LifecycleAuditLog();
        log.setReferenceID(referenceId);
        log.setPerformedBy(performedBy);
        log.setActionType(actionType);
        log.setTransactionDate(LocalDateTime.now());
        log.setNotes(notes);
        lifecycleLogRepo.save(log);
    }
}