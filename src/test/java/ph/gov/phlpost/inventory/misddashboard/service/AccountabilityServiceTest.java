package ph.gov.phlpost.inventory.misddashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ph.gov.phlpost.inventory.misddashboard.model.Personnel;
import ph.gov.phlpost.inventory.misddashboard.repository.PersonnelRepository;
import ph.gov.phlpost.inventory.misddashboard.service.AccountabilityService.Basis;

/**
 * The accountability rule: non-regular personnel may use property but cannot
 * be accountable for it; that falls to an eligible manager.
 */
@ExtendWith(MockitoExtension.class)
class AccountabilityServiceTest {

    @Mock
    private PersonnelRepository personnelRepo;

    private AccountabilityService service;

    @BeforeEach
    void setUp() {
        service = new AccountabilityService(personnelRepo, "Regular");
        person("DIR", "Regular", null);
        person("MGR", "Regular", "DIR");
        person("JO-1", "Job Order", "MGR");
        person("COS-LEAD", "Contract of Service", "MGR");
        person("JO-2", "Job Order", "COS-LEAD");
        person("ORPHAN", "Job Order", null);
    }

    @Test
    void regularEmployeeIsAccountableForWhatTheyUse() {
        var result = service.resolve("MGR", null);

        assertThat(result).isEqualTo(new AccountabilityService.Accountability("MGR", "MGR", Basis.SELF));
    }

    @Test
    void jobOrderUsersManagerBecomesAccountable() {
        var result = service.resolve("JO-1", null);

        assertThat(result).isEqualTo(new AccountabilityService.Accountability("JO-1", "MGR", Basis.MANAGER));
    }

    @Test
    void nonRegularManagerIsSkippedForTheNextEligibleOneUp() {
        // JO-2 reports to a Contract of Service lead, who reports to MGR.
        var result = service.resolve("JO-2", null);

        assertThat(result.accountableId()).isEqualTo("MGR");
        assertThat(result.basis()).isEqualTo(Basis.MANAGER);
    }

    @Test
    void explicitChoiceWinsWhenEligible() {
        var result = service.resolve("JO-1", "DIR");

        assertThat(result).isEqualTo(new AccountabilityService.Accountability("JO-1", "DIR", Basis.CHOSEN));
    }

    @Test
    void ineligibleExplicitChoiceIsRefused() {
        assertThatThrownBy(() -> service.resolve("MGR", "JO-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is Job Order and cannot be the accountable owner");
    }

    @Test
    void nonRegularWithNoEligibleManagerMustHaveAnOwnerChosen() {
        assertThatThrownBy(() -> service.resolve("ORPHAN", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Choose an accountable owner");
    }

    @Test
    void unknownEmployeeIsReportedNotSilentlyAccepted() {
        lenient().when(personnelRepo.findById("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve("NOPE", null)).hasMessage("Assigned employee was not found.");
    }

    @Test
    void custodianMustBeEligible() {
        service.requireEligible("MGR", "Custodian");
        assertThatThrownBy(() -> service.requireEligible("JO-1", "Custodian"))
                .hasMessageContaining("cannot be the accountable owner");
    }

    @Test
    void eligibleStatusesAreConfigurable() {
        var permissive = new AccountabilityService(personnelRepo, "Regular, Contract of Service");

        assertThat(permissive.resolve("COS-LEAD", null).basis()).isEqualTo(Basis.SELF);
    }

    private void person(String id, String status, String managerId) {
        Personnel personnel = mock(Personnel.class);
        lenient().when(personnel.getEmployeeID()).thenReturn(id);
        lenient().when(personnel.getFirstName()).thenReturn(id);
        lenient().when(personnel.getLastName()).thenReturn("Person");
        lenient().when(personnel.getEmploymentStatus()).thenReturn(status);
        lenient().when(personnel.getManagerID()).thenReturn(managerId);
        lenient().when(personnelRepo.findById(id)).thenReturn(Optional.of(personnel));
    }
}
