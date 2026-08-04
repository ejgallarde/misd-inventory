package ph.gov.phlpost.inventory.misddashboard.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ph.gov.phlpost.inventory.misddashboard.repository.AssetAssignmentLogRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.LifecycleAuditLogRepository;

@Service
public class AssetHistoryService {

        private final AssetAssignmentLogRepository assignmentLogRepository;
        private final LifecycleAuditLogRepository lifecycleLogRepository;

        public AssetHistoryService(AssetAssignmentLogRepository assignmentLogRepository,
                        LifecycleAuditLogRepository lifecycleLogRepository) {
                this.assignmentLogRepository = assignmentLogRepository;
                this.lifecycleLogRepository = lifecycleLogRepository;
        }

        @Transactional(readOnly = true)
        public List<AssetHistoryEntry> getHistory(String assetTag) {
                List<AssetHistoryEntry> history = new ArrayList<>();

                assignmentLogRepository.findByAssetTagOrderByTransactionDateDescTransactionIDDesc(assetTag)
                                .forEach(log -> history.add(new AssetHistoryEntry(
                                                log.getTransactionDate(), "Assignment", log.getActionType(),
                                                log.getEmployeeID(), log.getConditionNotes())));

                lifecycleLogRepository.findByReferenceIDOrderByTransactionDateDescLogIDDesc(assetTag)
                                .forEach(log -> history.add(new AssetHistoryEntry(
                                                log.getTransactionDate(), "Lifecycle", log.getActionType(),
                                                log.getPerformedBy(), log.getNotes())));

                history.sort(Comparator.comparing(
                                entry -> entry.transactionDate(),
                                Comparator.nullsLast(Comparator.reverseOrder())));
                return List.copyOf(history);
        }

        public record AssetHistoryEntry(
                        LocalDateTime transactionDate,
                        String logType,
                        String actionType,
                        String recordedBy,
                        String notes) {
        }
}