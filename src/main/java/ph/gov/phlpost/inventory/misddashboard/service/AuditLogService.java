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
    public void logAssignment(String referenceType, String referenceId, String employeeId, String actionType,
            String notes) {
        logAssignment(referenceType, referenceId, employeeId, null, actionType, null, notes);
    }

    /**
     * @param endUserId  the person actually using the asset at the time of this transaction (may differ from
     *                   employeeId, the accountable person); null when not known/applicable.
     * @param documentNo PAR/PTR/ICS number backing this assignment; null when not known/applicable.
     */
    @Transactional
    public void logAssignment(String referenceType, String referenceId, String employeeId, String endUserId,
            String actionType, String documentNo, String notes) {
        AssetAssignmentLog log = new AssetAssignmentLog();
        log.setReferenceType(referenceType);
        log.setAssetTag(referenceId);
        log.setEmployeeID(employeeId);
        log.setEndUserID(endUserId);
        log.setActionType(actionType);
        log.setDocumentNo(documentNo);
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