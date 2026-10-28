package ph.gov.phlpost.inventory.misddashboard.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

/** Role parsing, the permission flags derived from roles, and the Entra claim mapping. */
class RolesTest {

    @Test
    void eachRoleGetsExactlyItsPermissions() {
        assertThat(UserPermissions.of(user("ROLE_ADMIN")))
                .isEqualTo(new UserPermissions(true, true, true, "Admin"));
        assertThat(UserPermissions.of(user("ROLE_CLERK")))
                .isEqualTo(new UserPermissions(true, false, false, "Clerk"));
        assertThat(UserPermissions.of(user("ROLE_FINANCE")))
                .isEqualTo(new UserPermissions(false, true, false, "Finance"));
        assertThat(UserPermissions.of(user("ROLE_VIEWER")))
                .isEqualTo(new UserPermissions(false, false, false, "Viewer"));
        assertThat(UserPermissions.of(null)).isEqualTo(UserPermissions.NONE);
    }

    @Test
    void permissionsCombineAcrossRoles() {
        UserPermissions both = UserPermissions.of(user("ROLE_CLERK", "ROLE_FINANCE"));

        assertThat(both.operate()).isTrue();
        assertThat(both.financial()).isTrue();
        assertThat(both.admin()).isFalse();
        assertThat(both.metaContent()).isEqualTo("operate financial");
    }

    @Test
    void demoUsersParseAsEmailAndRole() {
        assertThat(SecurityConfig.parseDemoUsers(" a@x.ph:ADMIN , f@x.ph:finance "))
                .containsExactly(new SecurityConfig.DemoAccount("a@x.ph", AppRole.ADMIN),
                        new SecurityConfig.DemoAccount("f@x.ph", AppRole.FINANCE));
    }

    @Test
    void misconfiguredDemoUserFailsStartupInsteadOfGrantingNothing() {
        assertThatThrownBy(() -> SecurityConfig.parseDemoUsers("a@x.ph:SUPERUSER"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SecurityConfig.parseDemoUsers("a@x.ph"))
                .hasMessageContaining("email:ROLE");
    }

    @Test
    void entraAppRolesMapToRegistryRoles() {
        var mapper = new RoleClaimAuthoritiesMapper("roles", "MISD.Finance:FINANCE,MISD.Clerk:CLERK");
        var oidc = new OAuth2UserAuthority(Map.of("sub", "u-1", "roles", List.of("MISD.Finance", "Unrelated")));

        Set<String> granted = mapper.mapAuthorities(List.of(oidc)).stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertThat(granted).contains("ROLE_FINANCE").doesNotContain("ROLE_CLERK", "ROLE_ADMIN", "ROLE_VIEWER");
    }

    @Test
    void entraUserWithoutAMappedRoleGetsNoRegistryRole() {
        var mapper = new RoleClaimAuthoritiesMapper("roles", "MISD.Admin:ADMIN");
        var oidc = new OAuth2UserAuthority(Map.of("sub", "u-2"));

        assertThat(mapper.mapAuthorities(List.of(oidc)))
                .extracting(GrantedAuthority::getAuthority)
                .noneMatch(authority -> authority.startsWith("ROLE_"));
    }

    private static TestingAuthenticationToken user(String... authorities) {
        return new TestingAuthenticationToken("someone@phlpost.gov.ph", null, authorities);
    }
}
