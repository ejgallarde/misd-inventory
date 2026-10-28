package ph.gov.phlpost.inventory.misddashboard.util;

import org.springframework.security.core.Authentication;

/**
 * The name recorded as "performed by" / "uploaded by" in the audit trail.
 *
 * <p>
 * Every workflow action used to log the literal "SYSTEM" (or "MOTORPOOL",
 * "FINANCE", "SystemUser"), so the history never said who acted. Controllers
 * now resolve the signed-in user once, through this, and pass it down.
 */
public final class AuditActor {

    /** Recorded only when there is no signed-in user, e.g. a scheduled job. */
    public static final String SYSTEM = "SYSTEM";

    private AuditActor() {
    }

    public static String of(Authentication authentication) {
        if (authentication == null || TextUtils.isBlank(authentication.getName())) {
            return SYSTEM;
        }
        return authentication.getName();
    }
}
