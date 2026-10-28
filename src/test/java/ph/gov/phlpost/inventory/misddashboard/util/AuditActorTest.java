package ph.gov.phlpost.inventory.misddashboard.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

class AuditActorTest {

    @Test
    void signedInUserIsRecordedByName() {
        assertThat(AuditActor.of(new TestingAuthenticationToken("clerk@phlpost.gov.ph", null)))
                .isEqualTo("clerk@phlpost.gov.ph");
    }

    @Test
    void noUserFallsBackToSystem() {
        assertThat(AuditActor.of(null)).isEqualTo(AuditActor.SYSTEM);
        assertThat(AuditActor.of(new TestingAuthenticationToken(" ", null))).isEqualTo(AuditActor.SYSTEM);
    }
}
