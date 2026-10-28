package ph.gov.phlpost.inventory.misddashboard.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import ph.gov.phlpost.inventory.misddashboard.util.TextUtils;

@Configuration
@EnableWebSecurity
/**
 * LoginController owns GET /login. A view-controller registration for the same
 * path used to shadow it here; annotation mapping won on handler ordering, but
 * had that ever changed, the controller's entraConfigured model attribute would
 * have gone unset and the login page would have shown "SSO is not configured"
 * with no error anywhere.
 *
 * <p>
 * Authorization is by {@link AppRole}. The URL rules below are the single
 * enforcement point; templates only hide what a role cannot use. Any
 * state-changing endpoint not listed falls through to ADMIN-only, so a new
 * endpoint is closed until someone decides who may call it. Field-level rules
 * inside the three detail-panel update endpoints (cost and tax fields for
 * FINANCE, the rest for CLERK) live in those controllers via
 * {@link UserPermissions}.
 */
public class SecurityConfig {

    private static final String ADMIN = AppRole.ADMIN.name();
    private static final String CLERK = AppRole.CLERK.name();
    private static final String FINANCE = AppRole.FINANCE.name();

    @Value("${app.security.demo-mode:false}")
    private boolean demoMode;

    /**
     * Demo accounts as "email:ROLE" pairs, all signing in with the demo
     * password. Replaces the single app.security.demo-user-email account.
     */
    @Value("${app.security.demo-users:admin@phlpost.gov.ph:ADMIN}")
    private String demoUsers;

    @Value("${app.security.oauth2.enabled:false}")
    private boolean oauth2Enabled;

    @Value("${app.security.oauth2.roles-claim:roles}")
    private String oauth2RolesClaim;

    @Value("${app.security.oauth2.role-mapping:}")
    private String oauth2RoleMapping;

    /**
     * Demo-mode sign-in password. Supplied from application-local.properties or
     * the environment, never committed — it used to be the literal "change-me"
     * in this file, which is not a password anyone would remember to change.
     */
    @Value("${app.security.demo-user-password}")
    private String demoUserPassword;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        HttpSecurity configured = http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/logout", "/css/**", "/js/**", "/vendor/**", "/icons/**",
                                "/images/**", "/*.png", "/*.jpg", "/*.jpeg", "/*.gif", "/*.svg", "/*.webp",
                                "/*.ico", "/webjars/**", "/error")
                        .permitAll()
                        // Reference-data administration and irreversible deletes.
                        .requestMatchers("/admin/**", "/api/locations/import/**").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/documents/**").hasRole(ADMIN)
                        // Finance-owned changes.
                        .requestMatchers(HttpMethod.POST, "/properties/update-tax").hasAnyRole(ADMIN, FINANCE)
                        // Detail-panel saves: the controller limits FINANCE to cost/tax fields
                        // and CLERK to everything else.
                        .requestMatchers(HttpMethod.POST, "/assets/update", "/fleet/update", "/properties/update")
                        .hasAnyRole(ADMIN, CLERK, FINANCE)
                        // Registry operations.
                        .requestMatchers(HttpMethod.POST, "/assets/**", "/fleet/**", "/properties/**",
                                "/catalog/**", "/documents/add")
                        .hasAnyRole(ADMIN, CLERK)
                        // Reading is open to every registry role (not to a signed-in
                        // identity with no role, e.g. an unmapped Entra user).
                        .requestMatchers(HttpMethod.GET, "/**").hasAnyRole(AppRole.allNames())
                        .anyRequest().hasRole(ADMIN))
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler(new RoleAwareAccessDeniedHandler()))
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .csrf(Customizer.withDefaults());

        if (oauth2Enabled) {
            RoleClaimAuthoritiesMapper roleMapper = new RoleClaimAuthoritiesMapper(oauth2RolesClaim,
                    oauth2RoleMapping);
            configured = configured.oauth2Login(oauth2 -> oauth2
                    .loginPage("/login")
                    .defaultSuccessUrl("/", true)
                    .userInfoEndpoint(userInfo -> userInfo.userAuthoritiesMapper(roleMapper)));
        }

        return configured.build();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(PasswordEncoder passwordEncoder) {
        if (!demoMode) {
            return new InMemoryUserDetailsManager();
        }
        String encodedPassword = passwordEncoder.encode(demoUserPassword);
        return new InMemoryUserDetailsManager(parseDemoUsers(demoUsers).stream()
                .map(account -> User.builder()
                        .username(account.email())
                        .password(encodedPassword)
                        .roles(account.role().name())
                        .build())
                .toArray(UserDetails[]::new));
    }

    record DemoAccount(String email, AppRole role) {
    }

    /** Parses "email:ROLE,email:ROLE"; an unknown role fails startup rather than granting nothing. */
    static List<DemoAccount> parseDemoUsers(String csv) {
        List<DemoAccount> accounts = new ArrayList<>();
        for (String entry : TextUtils.splitCsv(csv)) {
            int separator = entry.lastIndexOf(':');
            if (separator <= 0 || separator == entry.length() - 1) {
                throw new IllegalArgumentException(
                        "app.security.demo-users entry '" + entry + "' must look like email:ROLE.");
            }
            accounts.add(new DemoAccount(entry.substring(0, separator).trim(),
                    AppRole.valueOf(entry.substring(separator + 1).trim().toUpperCase())));
        }
        return accounts;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
