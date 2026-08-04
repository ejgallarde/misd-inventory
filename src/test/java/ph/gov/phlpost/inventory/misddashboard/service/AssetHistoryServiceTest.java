package ph.gov.phlpost.inventory.misddashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ph.gov.phlpost.inventory.misddashboard.model.AssetAssignmentLog;
import ph.gov.phlpost.inventory.misddashboard.model.LifecycleAuditLog;
import ph.gov.phlpost.inventory.misddashboard.repository.AssetAssignmentLogRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.LifecycleAuditLogRepository;

@ExtendWith(MockitoExtension.class)
class AssetHistoryServiceTest {

        @Mock
        private AssetAssignmentLogRepository assignmentLogRepository;

        @Mock
        private LifecycleAuditLogRepository lifecycleLogRepository;

        @InjectMocks
        private AssetHistoryService service;

        @Test
        void combinesBothLogSourcesWithMostRecentEntryFirst() {
                LocalDateTime earlier = LocalDateTime.of(2026, 7, 1, 9, 0);
                LocalDateTime later = LocalDateTime.of(2026, 7, 2, 10, 30);

                AssetAssignmentLog assignment = new AssetAssignmentLog();
                assignment.setAssetTag("TAG-1");
                assignment.setEmployeeID("EMP-1");
                assignment.setActionType("Checkout");
                assignment.setTransactionDate(earlier);
                assignment.setConditionNotes("Issued to employee");

                LifecycleAuditLog lifecycle = new LifecycleAuditLog();
                lifecycle.setReferenceID("TAG-1");
                lifecycle.setPerformedBy("SYSTEM");
                lifecycle.setActionType("Asset Status Updated");
                lifecycle.setTransactionDate(later);
                lifecycle.setNotes("Health changed");

                when(assignmentLogRepository.findByAssetTagOrderByTransactionDateDescTransactionIDDesc("TAG-1"))
                                .thenReturn(List.of(assignment));
                when(lifecycleLogRepository.findByReferenceIDOrderByTransactionDateDescLogIDDesc("TAG-1"))
                                .thenReturn(List.of(lifecycle));

                List<AssetHistoryService.AssetHistoryEntry> history = service.getHistory("TAG-1");

                assertThat(history).extracting(entry -> entry.actionType())
                                .containsExactly("Asset Status Updated", "Checkout");
                assertThat(history.get(0).logType()).isEqualTo("Lifecycle");
                assertThat(history.get(1).recordedBy()).isEqualTo("EMP-1");
        }
}