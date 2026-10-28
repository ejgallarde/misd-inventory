package ph.gov.phlpost.inventory.misddashboard.config;

/**
 * Application roles. Granted as Spring Security authorities "ROLE_" + name().
 *
 * <ul>
 * <li>ADMIN: everything, including document deletion and PSGC imports.</li>
 * <li>CLERK: day-to-day registry work (registration, assignments, repairs,
 * retirements, custodians, detail edits, document uploads), but not tax
 * status or costs after registration.</li>
 * <li>FINANCE: views everything; may change property tax status and assessed
 * value, asset purchase prices and vehicle costs. Nothing else.</li>
 * <li>VIEWER: read-only.</li>
 * </ul>
 *
 * Demo mode assigns these from app.security.demo-users; with Entra ID they
 * come from the token's app-role claim via {@link RoleClaimAuthoritiesMapper}.
 */
public enum AppRole {
    ADMIN, CLERK, FINANCE, VIEWER;

    public String authority() {
        return "ROLE_" + name();
    }

    /** Every role, for rules that apply to any signed-in registry user. */
    public static String[] allNames() {
        return java.util.Arrays.stream(values()).map(Enum::name).toArray(String[]::new);
    }
}
