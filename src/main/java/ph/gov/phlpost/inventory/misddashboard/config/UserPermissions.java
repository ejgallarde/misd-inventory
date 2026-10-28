package ph.gov.phlpost.inventory.misddashboard.config;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * What the signed-in user may do, derived from their roles. The URL rules in
 * {@link SecurityConfig} are the enforcement; this drives field-level rules in
 * the update endpoints and which buttons the templates show.
 *
 * @param operate   registry operations (CLERK, ADMIN)
 * @param financial tax status and costs (FINANCE, ADMIN)
 * @param admin     document deletion, PSGC import (ADMIN)
 * @param roleLabel role names for display, e.g. "Finance"
 */
public record UserPermissions(boolean operate, boolean financial, boolean admin, String roleLabel) {

    public static final UserPermissions NONE = new UserPermissions(false, false, false, "");

    public static UserPermissions of(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return NONE;
        }
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        boolean admin = authorities.contains(AppRole.ADMIN.authority());
        boolean clerk = authorities.contains(AppRole.CLERK.authority());
        boolean finance = authorities.contains(AppRole.FINANCE.authority());

        String label = java.util.Arrays.stream(AppRole.values())
                .filter(role -> authorities.contains(role.authority()))
                .map(role -> role.name().charAt(0) + role.name().substring(1).toLowerCase())
                .collect(Collectors.joining(", "));

        return new UserPermissions(admin || clerk, admin || finance, admin, label);
    }

    /** Space-separated flags for the misd-permissions meta tag read by ui-common.js. */
    public String metaContent() {
        return ((operate ? "operate " : "") + (financial ? "financial " : "") + (admin ? "admin" : "")).trim();
    }
}
