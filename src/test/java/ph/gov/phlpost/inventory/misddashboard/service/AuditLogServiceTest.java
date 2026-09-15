package ph.gov.phlpost.inventory.misddashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ph.gov.phlpost.inventory.misddashboard.model.AssetAssignmentLog;
import ph.gov.phlpost.inventory.misddashboard.model.LifecycleAuditLog;
import ph.gov.phlpost.inventory.misddashboard.repository.AssetAssignmentLogRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.LifecycleAuditLogRepository;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AssetAssignmentLogRepository assignmentLogRepo;

    @Mock
    private LifecycleAuditLogRepository lifecycleLogRepo;

    @InjectMocks
    private AuditLogService auditLogService;

    @Captor
    private ArgumentCaptor<AssetAssignmentLog> assignmentCaptor;

    @Captor
    private ArgumentCaptor<LifecycleAuditLog> lifecycleCaptor;

    @Test
    void logAssignmentSavesEveryFieldPassedIn() {
        auditLogService.logAssignment("FLEETVEHICLE", "VEHICLE-17", "PERS-0001", "Checkout",
                "Handed over at motorpool");

        verify(assignmentLogRepo).save(assignmentCaptor.capture());
        AssetAssignmentLog saved = assignmentCaptor.getValue();
        assertThat(saved.getReferenceType()).isEqualTo("FLEETVEHICLE");
        assertThat(saved.getAssetTag()).isEqualTo("VEHICLE-17");
        assertThat(saved.getEmployeeID()).isEqualTo("PERS-0001");
        assertThat(saved.getEndUserID()).isNull();
        assertThat(saved.getActionType()).isEqualTo("Checkout");
        assertThat(saved.getDocumentNo()).isNull();
        assertThat(saved.getConditionNotes()).isEqualTo("Handed over at motorpool");
        assertThat(saved.getTransactionDate()).isNotNull();
    }

    @Test
    void logAssignmentWithEndUserAndDocumentNoSavesEveryFieldPassedIn() {
        auditLogService.logAssignment("FLEETVEHICLE", "VEHICLE-17", "PERS-0001", "PERS-0002", "Checkout",
                "PAR-2026-001", "Handed over at motorpool");

        verify(assignmentLogRepo).save(assignmentCaptor.capture());
        AssetAssignmentLog saved = assignmentCaptor.getValue();
        assertThat(saved.getReferenceType()).isEqualTo("FLEETVEHICLE");
        assertThat(saved.getAssetTag()).isEqualTo("VEHICLE-17");
        assertThat(saved.getEmployeeID()).isEqualTo("PERS-0001");
        assertThat(saved.getEndUserID()).isEqualTo("PERS-0002");
        assertThat(saved.getActionType()).isEqualTo("Checkout");
        assertThat(saved.getDocumentNo()).isEqualTo("PAR-2026-001");
        assertThat(saved.getConditionNotes()).isEqualTo("Handed over at motorpool");
        assertThat(saved.getTransactionDate()).isNotNull();
    }

    @Test
    void logLifecycleEventSavesEveryFieldPassedIn() {
        auditLogService.logLifecycleEvent("SURVEYASSET-42", "SYSTEM", "Marked Impounded", "Held pending clearance");

        verify(lifecycleLogRepo).save(lifecycleCaptor.capture());
        LifecycleAuditLog saved = lifecycleCaptor.getValue();
        assertThat(saved.getReferenceID()).isEqualTo("SURVEYASSET-42");
        assertThat(saved.getPerformedBy()).isEqualTo("SYSTEM");
        assertThat(saved.getActionType()).isEqualTo("Marked Impounded");
        assertThat(saved.getNotes()).isEqualTo("Held pending clearance");
        assertThat(saved.getTransactionDate()).isNotNull();
    }
}
