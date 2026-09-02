package ph.gov.phlpost.inventory.misddashboard.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void duplicateSerialNumberIsDescribedInRegistryTerms() {
        String message = handler.describeConstraintViolation(
                "Duplicate entry '' for key 'assets.SerialNumber'");

        assertEquals("An asset with this serial number is already registered.", message);
    }

    @Test
    void duplicatePlateNumberIsDescribedInRegistryTerms() {
        String message = handler.describeConstraintViolation(
                "Duplicate entry 'ABC 1234' for key 'fleetvehicles.PlateNumber'");

        assertEquals("A vehicle with this plate number is already registered.", message);
    }

    @Test
    void taxDeclarationConstraintWinsOverTheBareColumnName() {
        String message = handler.describeConstraintViolation(
                "Duplicate entry 'TD-1' for key 'UK_RealEstateProperties_TaxDeclarationNumber'");

        assertEquals("A property with this Tax Declaration number is already registered.", message);
    }

    @Test
    void ownerForeignKeyFailureNamesTheEmployeeLookup() {
        String message = handler.describeConstraintViolation(
                "Cannot add or update a child row: a foreign key constraint fails "
                        + "(`misd_inventory`.`assets`, CONSTRAINT `assets_ibfk_2` FOREIGN KEY (`CurrentOwnerID`))");

        assertEquals("The selected accountable owner is not a known employee.", message);
    }

    @Test
    void unrecognisedConstraintFallsBackWithoutLeakingSql() {
        String message = handler.describeConstraintViolation(
                "Duplicate entry 'x' for key 'sometable.SomeIndex'");

        assertEquals("Another record already uses one of these identifying numbers.", message);
        assertTrue(message.indexOf("sometable") < 0, "raw SQL identifiers must not reach the user");
    }

    @Test
    void nullRootCauseMessageStillProducesReadableText() {
        String message = handler.describeConstraintViolation(null);

        assertEquals("The registry rejected this change because it conflicts with an existing record.", message);
    }

    @Test
    void missingParameterNamesTheFieldThatWasNotSubmitted() {
        MissingServletRequestParameterException exception =
                new MissingServletRequestParameterException("employeeID", "String");

        assertEquals("Required field 'employeeID' was not submitted.",
                handler.describeInvalidSubmission(exception));
    }

    @Test
    void ajaxCallerReceivesJsonRatherThanARedirect() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/assets/update");
        request.addHeader("X-Requested-With", "XMLHttpRequest");

        Object response = handler.handleDataIntegrityViolation(
                duplicateSerialViolation(), null, request, new RedirectAttributesModelMap());

        ResponseEntity<?> entity = assertInstanceOf(ResponseEntity.class, response);
        assertEquals(HttpStatus.CONFLICT, entity.getStatusCode());
        assertEquals(Map.of("error", "An asset with this serial number is already registered."), entity.getBody());
    }

    @Test
    void formPostRedirectsBackToTheSubmittingPageWithAFlashMessage() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/assets/receive");
        request.setServerName("localhost");
        request.addHeader("Referer", "http://localhost:8080/assets?search=PPC&page=2");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        Object response = handler.handleDataIntegrityViolation(
                duplicateSerialViolation(), null, request, redirectAttributes);

        assertEquals("redirect:/assets?search=PPC&page=2", response);
        assertEquals("An asset with this serial number is already registered.",
                redirectAttributes.getFlashAttributes().get("errorMessage"));
    }

    @Test
    void offSiteRefererIsNotUsedAsARedirectTarget() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/assets/receive");
        request.setServerName("localhost");
        request.addHeader("Referer", "https://example.invalid/phish");

        Object response = handler.handleDataIntegrityViolation(
                duplicateSerialViolation(), null, request, new RedirectAttributesModelMap());

        assertEquals("redirect:/", response);
    }

    @Test
    void missingRefererFallsBackToTheDashboard() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/fleet/add");

        Object response = handler.handleDataIntegrityViolation(
                duplicateSerialViolation(), null, request, new RedirectAttributesModelMap());

        assertEquals("redirect:/", response);
    }

    private DataIntegrityViolationException duplicateSerialViolation() {
        return new DataIntegrityViolationException("could not execute statement",
                new SQLIntegrityConstraintViolationException(
                        "Duplicate entry '' for key 'assets.SerialNumber'"));
    }
}
