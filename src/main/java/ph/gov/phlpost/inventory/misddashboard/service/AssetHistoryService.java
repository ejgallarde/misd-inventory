package ph.gov.phlpost.inventory.misddashboard.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
                                                log.getEmployeeID(), log.getDocumentNo(), log.getConditionNotes())));

                lifecycleLogRepository.findByReferenceIDOrderByTransactionDateDescLogIDDesc(assetTag)
                                .forEach(log -> history.add(new AssetHistoryEntry(
                                                log.getTransactionDate(), "Lifecycle", log.getActionType(),
                                                log.getPerformedBy(), null, log.getNotes())));

                history.sort(Comparator.comparing(
                                entry -> entry.transactionDate(),
                                Comparator.nullsLast(Comparator.reverseOrder())));
                return List.copyOf(history);
        }

        /**
         * The PAR/PTR/ICS number backing each asset's/vehicle's/survey-asset's most
         * recent assignment transaction, for display as a column on the table
         * views (one query for all three domains - see
         * {@link AssetAssignmentLogRepository#findLatestDocumentNoPerAssetTag()}).
         * Absent from the map (or blank) when the latest transaction has none.
         */
        @Transactional(readOnly = true)
        public Map<String, String> getLatestDocumentNoMap() {
                Map<String, String> map = new LinkedHashMap<>();
                for (Object[] row : assignmentLogRepository.findLatestDocumentNoPerAssetTag()) {
                        map.put((String) row[0], (String) row[1]);
                }
                return map;
        }

        public record AssetHistoryEntry(
                        LocalDateTime transactionDate,
                        String logType,
                        String actionType,
                        String recordedBy,
                        String documentNo,
                        String notes) {
        }
}