package ph.gov.phlpost.inventory.misddashboard.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ph.gov.phlpost.inventory.misddashboard.model.LocationOption;
import ph.gov.phlpost.inventory.misddashboard.service.LocationLookupService;
import ph.gov.phlpost.inventory.misddashboard.util.TextUtils;

/**
 * htmx partials for the province -> city/municipality -> barangay pickers
 * (templates/fragments/location-options.html).
 *
 * <p>
 * The selects submit names, because names are what RealEstateProperty stores,
 * so each request resolves them back to PSGC codes through the cached lists in
 * {@link LocationLookupService}. A city is only looked up within its province
 * and a barangay within its city, since PSGC names repeat across those.
 */
@Controller
@RequestMapping("/locations/options")
public class LocationOptionsController {

    private final LocationLookupService locationLookupService;

    public LocationOptionsController(LocationLookupService locationLookupService) {
        this.locationLookupService = locationLookupService;
    }

    /** City select for a province, plus an out-of-band reset of the barangay select. */
    @GetMapping("/cities")
    public String cities(@RequestParam(required = false) String province, Model model) {
        model.addAttribute("cities", findProvince(province)
                .map(found -> locationLookupService.getCities(found.code()))
                .orElse(List.of()));
        return "fragments/location-options :: cityResponse(cities=${cities})";
    }

    @GetMapping("/barangays")
    public String barangays(@RequestParam(required = false) String province,
            @RequestParam(required = false) String city, Model model) {
        model.addAttribute("barangays", findCity(province, city)
                .map(found -> locationLookupService.getBarangays(found.code()))
                .orElse(List.of()));
        return "fragments/location-options :: barangaySelect(barangays=${barangays}, oob=false)";
    }

    /**
     * ZIP code of the chosen barangay. Answers 204 when PSGC has none, which
     * htmx treats as "no swap", so a ZIP the user typed is kept.
     */
    @GetMapping("/zip")
    public Object zip(@RequestParam(required = false) String province, @RequestParam(required = false) String city,
            @RequestParam(required = false) String barangay, Model model) {
        Optional<String> zipCode = findCity(province, city)
                .flatMap(found -> findByName(locationLookupService.getBarangays(found.code()), barangay))
                .map(LocationOption::zipCode)
                .filter(value -> !TextUtils.isBlank(value));
        if (zipCode.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        model.addAttribute("zipCode", zipCode.get());
        return "fragments/location-options :: zipInput(zipCode=${zipCode})";
    }

    private Optional<LocationOption> findProvince(String name) {
        return findByName(locationLookupService.getProvinces(), name);
    }

    private Optional<LocationOption> findCity(String provinceName, String cityName) {
        return findProvince(provinceName)
                .flatMap(province -> findByName(locationLookupService.getCities(province.code()), cityName));
    }

    private static Optional<LocationOption> findByName(List<LocationOption> options, String name) {
        if (TextUtils.isBlank(name)) {
            return Optional.empty();
        }
        String wanted = name.trim();
        return options.stream().filter(option -> wanted.equalsIgnoreCase(option.name())).findFirst();
    }
}
