package ph.gov.phlpost.inventory.misddashboard.config;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Applies one binding rule to every form-backed controller method: a submitted
 * field that is empty or whitespace-only binds as {@code null}, not {@code ""}.
 *
 * <p>
 * Several columns are both optional and {@code UNIQUE} — Assets.SerialNumber,
 * FleetVehicles.PlateNumber / BodyNumber / EngineNumber / ChassisNumberVIN,
 * RealEstateProperties.TitleNumber / TaxDeclarationNumber. MySQL permits many
 * {@code NULL}s in a unique index but only one {@code ''}, so binding a blank
 * field to an empty string made the *second* record with that field left blank
 * fail on a duplicate-key error. Assets.CurrentOwnerID has the same problem
 * against its foreign key to Personnel.
 *
 * <p>
 * This only covers {@code @ModelAttribute} and {@code @RequestParam} binding.
 * JSON request bodies are deserialized by Jackson and never reach a
 * {@link WebDataBinder}, so the {@code /assets/update}, {@code /fleet/update}
 * and {@code /properties/update} endpoints normalize blanks themselves.
 */
@ControllerAdvice
public class WebBindingAdvice {

    @InitBinder
    public void registerBlankToNullEditor(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }
}
