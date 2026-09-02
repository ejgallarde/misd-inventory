package ph.gov.phlpost.inventory.misddashboard.config;

import jakarta.servlet.http.HttpServletRequest;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Single place where an unhandled failure becomes something a user can act on.
 *
 * <p>
 * Every controller here catches only {@link IllegalArgumentException}, so a
 * constraint violation, a mistyped number, or an oversized upload used to reach
 * Spring's Whitelabel Error Page — no navigation back and no indication of what
 * went wrong, with the submitted form discarded. This advice turns those into a
 * flash message on the page the user came from, or a JSON error for the AJAX
 * endpoints, and only falls through to a rendered error page for genuinely
 * unexpected failures.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Database constraint names mapped to the wording a registry clerk expects.
     * Keys are matched case-insensitively against the driver's message, longest
     * first, so {@code UK_RealEstateProperties_TaxDeclarationNumber} wins over a
     * bare column name that also appears in it.
     */
    private static final Map<String, String> CONSTRAINT_MESSAGES = buildConstraintMessages();

    private static Map<String, String> buildConstraintMessages() {
        Map<String, String> messages = new LinkedHashMap<>();
        messages.put("uk_realestateproperties_taxdeclarationnumber",
                "A property with this Tax Declaration number is already registered.");
        messages.put("assets.serialnumber", "An asset with this serial number is already registered.");
        messages.put("fleetvehicles.platenumber", "A vehicle with this plate number is already registered.");
        messages.put("fleetvehicles.enginenumber", "A vehicle with this engine number is already registered.");
        messages.put("fleetvehicles.chassisnumbervin", "A vehicle with this chassis number / VIN is already registered.");
        messages.put("fleetvehicles.bodynumber", "A vehicle with this body number is already registered.");
        messages.put("fleetvehicles.body_number", "A vehicle with this body number is already registered.");
        messages.put("realestateproperties.titlenumber",
                "A property with this Title Number / TCT is already registered.");
        messages.put("assets_ibfk_1", "The selected catalog item no longer exists. Refresh the page and try again.");
        messages.put("assets_ibfk_2", "The selected accountable owner is not a known employee.");
        messages.put("assetassignments_ibfk_2", "The selected employee is not in the personnel registry.");
        messages.put("fleetvehicles_ibfk_1", "The selected driver is not a known employee.");
        messages.put("personnel_ibfk_1", "The selected manager is not a known employee.");
        messages.put("personnel_ibfk_2", "The selected base location no longer exists.");
        return messages;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Object handleDataIntegrityViolation(DataIntegrityViolationException exception,
            HandlerMethod handlerMethod,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        String detail = rootCauseMessage(exception);
        log.warn("Constraint violation on {} {}: {}", request.getMethod(), request.getRequestURI(), detail);
        return respond(handlerMethod, request, redirectAttributes, HttpStatus.CONFLICT,
                describeConstraintViolation(detail));
    }

    @ExceptionHandler({ BindException.class, MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class })
    public Object handleInvalidSubmission(Exception exception,
            HandlerMethod handlerMethod,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        log.warn("Rejected submission on {} {}: {}", request.getMethod(), request.getRequestURI(),
                exception.getMessage());
        return respond(handlerMethod, request, redirectAttributes, HttpStatus.BAD_REQUEST,
                describeInvalidSubmission(exception));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object handleUploadTooLarge(MaxUploadSizeExceededException exception,
            HandlerMethod handlerMethod,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        log.warn("Upload rejected on {}: {}", request.getRequestURI(), exception.getMessage());
        return respond(handlerMethod, request, redirectAttributes, HttpStatus.CONTENT_TOO_LARGE,
                "That upload is too large. Attach files under the size limit shown on the upload field.");
    }

    /**
     * Turns a MySQL constraint message into registry wording. Falls back to a
     * generic sentence rather than exposing the raw SQL to the user — the full
     * driver message is already in the log line above the call site.
     */
    String describeConstraintViolation(String rootCauseMessage) {
        String haystack = rootCauseMessage == null ? "" : rootCauseMessage.toLowerCase(Locale.ROOT);

        for (Map.Entry<String, String> candidate : CONSTRAINT_MESSAGES.entrySet()) {
            if (haystack.contains(candidate.getKey())) {
                return candidate.getValue();
            }
        }

        if (haystack.contains("duplicate entry")) {
            return "Another record already uses one of these identifying numbers.";
        }
        if (haystack.contains("foreign key constraint")) {
            return "This record refers to something that no longer exists. Refresh the page and try again.";
        }
        if (haystack.contains("cannot be null")) {
            return "A required field was left blank.";
        }
        if (haystack.contains("incorrect decimal value") || haystack.contains("out of range")) {
            return "A number was entered in a format the registry cannot store. Use digits only, for example 1500000.00.";
        }

        return "The registry rejected this change because it conflicts with an existing record.";
    }

    String describeInvalidSubmission(Exception exception) {
        if (exception instanceof MissingServletRequestParameterException missing) {
            return "Required field '" + missing.getParameterName() + "' was not submitted.";
        }
        if (exception instanceof HttpMessageNotReadableException) {
            return "The submitted values could not be read. Check numeric and date fields, then try again.";
        }
        return "Some values could not be accepted. Check numeric and date fields, then try again.";
    }

    /**
     * AJAX callers get JSON in the shape their error handlers already read
     * ({@code responseJSON.error}); browser form posts get a redirect back to
     * the page they submitted from, carrying the flash message the templates
     * already render.
     */
    private Object respond(HandlerMethod handlerMethod,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes,
            HttpStatus status,
            String message) {
        if (expectsJson(handlerMethod, request)) {
            return ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("error", message));
        }

        redirectAttributes.addFlashAttribute("errorMessage", message);
        return "redirect:" + safeReturnPath(request);
    }

    private boolean expectsJson(HandlerMethod handlerMethod, HttpServletRequest request) {
        if (handlerMethod != null) {
            if (ResponseEntity.class.isAssignableFrom(handlerMethod.getMethod().getReturnType())) {
                return true;
            }
            if (handlerMethod.hasMethodAnnotation(ResponseBody.class)
                    || handlerMethod.getBeanType().isAnnotationPresent(ResponseBody.class)) {
                return true;
            }
        }

        if ("XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))) {
            return true;
        }

        String accept = request.getHeader(HttpHeaders.ACCEPT);
        return accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE);
    }

    /**
     * Only same-origin, path-only Referer values are used as redirect targets, so
     * a forged header cannot bounce the user off-site.
     */
    private String safeReturnPath(HttpServletRequest request) {
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

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage();
    }
}
