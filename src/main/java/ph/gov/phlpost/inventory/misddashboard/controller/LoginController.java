package ph.gov.phlpost.inventory.misddashboard.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    private final boolean entraConfigured;

    public LoginController(
            @Value("${app.security.oauth2.enabled:false}") boolean oauth2Enabled,
            @Value("${spring.security.oauth2.client.registration.azure.client-id:}") String clientId,
            @Value("${spring.security.oauth2.client.registration.azure.client-secret:}") String clientSecret,
            @Value("${spring.security.oauth2.client.provider.azure.issuer-uri:}") String issuerUri) {
        this.entraConfigured = oauth2Enabled && !clientId.isBlank() && !clientSecret.isBlank() && !issuerUri.isBlank();
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("entraConfigured", entraConfigured);
        return "login";
    }
}
