package ph.gov.phlpost.inventory.misddashboard.config;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

import ph.gov.phlpost.inventory.misddashboard.util.TextUtils;

/**
 * Turns identity-provider role claims into {@link AppRole} authorities, so that
 * switching from demo users to Entra ID needs configuration only.
 *
 * <p>
 * Entra ID puts the app roles assigned to a user (App registration, App roles)
 * in the "roles" claim of the ID token. Configure, for example:
 *
 * <pre>
 * app.security.oauth2.roles-claim=roles
 * app.security.oauth2.role-mapping=MISD.Admin:ADMIN,MISD.Clerk:CLERK,MISD.Finance:FINANCE,MISD.Viewer:VIEWER
 * </pre>
 *
 * A user whose token carries none of the mapped values receives no registry
 * role and is refused every page, rather than silently becoming a viewer.
 */
public class RoleClaimAuthoritiesMapper implements GrantedAuthoritiesMapper {

    private final String rolesClaim;
    private final Map<String, AppRole> claimToRole;

    public RoleClaimAuthoritiesMapper(String rolesClaim, String roleMappingCsv) {
        this.rolesClaim = TextUtils.isBlank(rolesClaim) ? "roles" : rolesClaim.trim();
        this.claimToRole = parseMapping(roleMappingCsv);
    }

    static Map<String, AppRole> parseMapping(String csv) {
        Map<String, AppRole> mapping = new LinkedHashMap<>();
        for (String entry : TextUtils.splitCsv(csv)) {
            int separator = entry.lastIndexOf(':');
            if (separator <= 0 || separator == entry.length() - 1) {
                throw new IllegalArgumentException(
                        "Role mapping entry '" + entry + "' must look like <claim value>:<ROLE>.");
            }
            mapping.put(entry.substring(0, separator).trim(),
                    AppRole.valueOf(entry.substring(separator + 1).trim().toUpperCase()));
        }
        return mapping;
    }

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Set<GrantedAuthority> mapped = new HashSet<>(authorities);
        for (GrantedAuthority authority : authorities) {
            if (authority instanceof OAuth2UserAuthority userAuthority) {
                Object claim = userAuthority.getAttributes().get(rolesClaim);
                if (claim instanceof Collection<?> values) {
                    for (Object value : values) {
                        AppRole role = claimToRole.get(String.valueOf(value));
                        if (role != null) {
                            mapped.add(new SimpleGrantedAuthority(role.authority()));
                        }
                    }
                }
            }
        }
        return mapped;
    }
}
