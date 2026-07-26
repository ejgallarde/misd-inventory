package ph.gov.phlpost.inventory.misddashboard.controller;

import ph.gov.phlpost.inventory.misddashboard.model.LocationOption;
import ph.gov.phlpost.inventory.misddashboard.model.PsgcImportResult;
import ph.gov.phlpost.inventory.misddashboard.service.LocationImportService;
import ph.gov.phlpost.inventory.misddashboard.service.LocationLookupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationLookupService locationLookupService;
    private final LocationImportService locationImportService;

    public LocationController(LocationLookupService locationLookupService,
            LocationImportService locationImportService) {
        this.locationLookupService = locationLookupService;
        this.locationImportService = locationImportService;
    }

    @GetMapping("/provinces")
    public List<LocationOption> listProvinces() {
        return locationLookupService.getProvinces();
    }

    @GetMapping("/cities")
    public List<LocationOption> listCities(
            @RequestParam(value = "provinceCode", required = false) String provinceCode) {
        if (provinceCode == null || provinceCode.isBlank()) {
            return Collections.emptyList();
        }
        return locationLookupService.getCities(provinceCode);
    }

    @GetMapping("/barangays")
    public List<LocationOption> listBarangays(
            @RequestParam(value = "cityMunicipalityCode", required = false) String cityMunicipalityCode) {
        if (cityMunicipalityCode == null || cityMunicipalityCode.isBlank()) {
            return Collections.emptyList();
        }
        return locationLookupService.getBarangays(cityMunicipalityCode);
    }

    @PostMapping("/import/csv")
    public ResponseEntity<Map<String, Object>> importPsgcCsv(
            @RequestParam("provincesFile") MultipartFile provincesFile,
            @RequestParam("citiesMunicipalitiesFile") MultipartFile citiesMunicipalitiesFile,
            @RequestParam("barangaysFile") MultipartFile barangaysFile) {
        Map<String, Object> response = new HashMap<>();

        try {
            PsgcImportResult importResult = locationImportService.importFromCsv(
                    provincesFile,
                    citiesMunicipalitiesFile,
                    barangaysFile);

            response.put("message", "PSGC data imported successfully.");
            response.put("summary", importResult);
            return ResponseEntity.ok(response);
        } catch (IOException ex) {
            response.put("error", "Failed to parse CSV files: " + ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception ex) {
            response.put("error", "Failed to import PSGC data: " + ex.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/import/psgc-single")
    public ResponseEntity<Map<String, Object>> importSinglePsgcCsv(
            @RequestParam("psgcFile") MultipartFile psgcFile) {
        Map<String, Object> response = new HashMap<>();

        try {
            PsgcImportResult importResult = locationImportService.importFromSinglePsgcCsv(psgcFile);
            response.put("message", "Single-file PSGC data imported successfully.");
            response.put("summary", importResult);
            return ResponseEntity.ok(response);
        } catch (IOException ex) {
            response.put("error", "Failed to parse PSGC CSV file: " + ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception ex) {
            response.put("error", "Failed to import PSGC data: " + ex.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
