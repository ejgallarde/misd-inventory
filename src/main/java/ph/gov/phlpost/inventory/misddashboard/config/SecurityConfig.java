package ph.gov.phlpost.inventory.misddashboard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableWebSecurity
/**
 * LoginController owns GET /login. A view-controller registration for the same
 * path used to shadow it here; annotation mapping won on handler ordering, but
 * had that ever changed, the controller's entraConfigured model attribute would
 * have gone unset and the login page would have shown "SSO is not configured"
 * with no error anywhere.
 */
public class SecurityConfig {

    @Value("${app.security.demo-mode:false}")
    private boolean demoMode;

    @Value("${app.security.demo-user-email:admin@dar.gov.ph}")
    private String demoUserEmail;

    @Value("${app.security.demo-user-name:Administrator}")
    private String demoUserName;

    @Value("${app.security.oauth2.enabled:false}")
    private boolean oauth2Enabled;

    /**
     * Demo-mode sign-in password. Supplied from application-local.properties or
     * the environment, never committed — it used to be the literal "change-me"
     * in this file, which is not a password anyone would remember to change.
     */
    @Value("${app.security.demo-user-password}")
    private String demoUserPassword;

    /**
     * script-src/style-src are pinned to 'self' plus the exact CDN origins the
     * templates load from (jsdelivr for Bootstrap/Select2, datatables.net,
     * code.jquery.com, cdnjs for DataTables buttons) rather than left open, so a
     * new CDN dependency must be added here deliberately instead of silently
     * working under a looser policy.
     */
    private static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "script-src 'self' https://cdn.jsdelivr.net https://cdn.datatables.net https://code.jquery.com https://cdnjs.cloudflare.com",
            "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdn.datatables.net",
            "img-src 'self' data:",
            "font-src 'self' https://cdn.jsdelivr.net data:",
            "connect-src 'self'",
            "frame-ancestors 'self'");

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        HttpSecurity configured = http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/logout", "/css/**", "/js/**", "/images/**", "/*.png",
                                "/*.jpg", "/*.jpeg", "/*.gif", "/*.svg", "/*.webp", "/*.ico", "/webjars/**",
                                "/error", "/actuator/health")
                        .permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .csrf(Customizer.withDefaults())
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                        // No-op until TLS is terminated in front of the app (reverse proxy/load
                        // balancer); browsers ignore HSTS on a plain-HTTP response.
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true)))
                .sessionManagement(session -> session
                        .sessionFixation(fixation -> fixation.changeSessionId())
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false));

        if (oauth2Enabled) {
            configured = configured.oauth2Login(oauth2 -> oauth2
                    .loginPage("/login")
                    .defaultSuccessUrl("/", true));
        }

        return configured.build();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(PasswordEncoder passwordEncoder) {
        if (!demoMode) {
            return new InMemoryUserDetailsManager();
        }

        UserDetails user = User.builder()
                .username(demoUserEmail)
                .password(passwordEncoder.encode(demoUserPassword))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Required for maximumSessions(1) above to actually notice a session has
     * ended (logout or timeout) rather than leaving it in the concurrent-session
     * registry until it expires, which would otherwise lock a user out of their
     * next login for no visible reason.
     */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
