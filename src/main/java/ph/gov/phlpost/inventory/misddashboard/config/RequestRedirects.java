package ph.gov.phlpost.inventory.misddashboard.config;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * How to answer a refused request: JSON for AJAX callers, a redirect back to
 * the submitting page for form posts. Shared by {@link GlobalExceptionHandler}
 * and {@link RoleAwareAccessDeniedHandler}.
 */
final class RequestRedirects {

    private RequestRedirects() {
    }

    /** True for jQuery/fetch callers and JSON request bodies. */
    static boolean prefersJson(HttpServletRequest request) {
        if ("XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))) {
            return true;
        }
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        if (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE)) {
            return true;
        }
        String contentType = request.getContentType();
        return contentType != null && contentType.contains(MediaType.APPLICATION_JSON_VALUE);
    }

    /**
     * Only same-origin, path-only Referer values are used as redirect targets, so
     * a forged header cannot bounce the user off-site.
     */
    static String safeReturnPath(HttpServletRequest request) {
        String referer = request.getHeader(HttpHeaders.REFERER);
        if (referer == null || referer.isBlank()) {
            return "/";
        }

        try {
            java.net.URI candidate = java.net.URI.create(referer);
            String host = candidate.getHost();
            if (host != null && !host.equalsIgnoreCase(request.getServerName())) {
                return "/";
            }

            String path = candidate.getRawPath();
            if (path == null || path.isBlank() || !path.startsWith("/")) {
                return "/";
            }

            String query = candidate.getRawQuery();
            return query == null || query.isBlank() ? path : path + "?" + query;
        } catch (IllegalArgumentException ex) {
            return "/";
        }
    }
}
