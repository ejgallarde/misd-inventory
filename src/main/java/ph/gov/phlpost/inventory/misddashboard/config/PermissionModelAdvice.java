package ph.gov.phlpost.inventory.misddashboard.config;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Exposes the signed-in user's {@link UserPermissions} to every template as ${perm}. */
@ControllerAdvice
public class PermissionModelAdvice {

    @ModelAttribute("perm")
    public UserPermissions permissions(Authentication authentication) {
        return UserPermissions.of(authentication);
    }
}
