package ph.gov.phlpost.inventory.misddashboard.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.SessionFlashMapManager;

/**
 * Answers a refused request the way the rest of the app reports errors,
 * instead of Spring's bare 403 page:
 * <ul>
 * <li>AJAX / JSON callers get 403 with {@code {"error": ...}}, the shape the
 * page scripts already read;</li>
 * <li>a refused form post redirects back to the page it came from with the
 * message in the error toast;</li>
 * <li>a refused page view (e.g. /admin/locations for a clerk) gets the 403
 * error page.</li>
 * </ul>
 * CSRF failures arrive here too and are worded as an expired session.
 */
public class RoleAwareAccessDeniedHandler implements AccessDeniedHandler {

    static final String NOT_PERMITTED = "Your role does not allow this action.";
    static final String CSRF_REJECTED = "Your session has expired. Reload the page and try again.";

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException exception) throws IOException {
        String message = exception instanceof CsrfException ? CSRF_REJECTED : NOT_PERMITTED;

        if (RequestRedirects.prefersJson(request)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"error\":\"" + message + "\"}");
            return;
        }

        if ("GET".equalsIgnoreCase(request.getMethod())) {
            response.sendError(HttpStatus.FORBIDDEN.value(), message);
            return;
        }

        // Runs in the security filter chain, before DispatcherServlet, so the
        // flash map is saved directly through the (default) session manager.
        String target = RequestRedirects.safeReturnPath(request);
        FlashMap flash = new FlashMap();
        flash.put("errorMessage", message);
        flash.setTargetRequestPath(target.contains("?") ? target.substring(0, target.indexOf('?')) : target);
        new SessionFlashMapManager().saveOutputFlashMap(flash, request, response);
        response.sendRedirect(request.getContextPath() + target);
    }
}
