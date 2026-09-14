package ph.gov.phlpost.inventory.misddashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import ph.gov.phlpost.inventory.misddashboard.model.PsgcImportResult;
import ph.gov.phlpost.inventory.misddashboard.service.LocationImportService;

@ExtendWith(MockitoExtension.class)
class LocationAdminControllerTest {

    @Mock
    private LocationImportService locationImportService;

    @InjectMocks
    private LocationAdminController controller;

    @Test
    void viewLocationAdminPageReturnsItsTemplate() {
        assertThat(controller.viewLocationAdminPage()).isEqualTo("location-admin");
    }

    @Test
    void importPsgcSingleCsvRejectsAMissingFileWithoutCallingTheService() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        MockMultipartFile empty = new MockMultipartFile("psgcFile", "psgc.csv", "text/csv", new byte[0]);

        String view = controller.importPsgcSingleCsv(empty, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/locations");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("Please select a PSGC CSV file.");
    }

    @Test
    void importPsgcSingleCsvFlashesTheSummaryOnSuccess() throws IOException {
        MockMultipartFile file = new MockMultipartFile("psgcFile", "psgc.csv", "text/csv", "data".getBytes());
        when(locationImportService.importFromSinglePsgcCsv(file)).thenReturn(new PsgcImportResult(1, 2, 3));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.importPsgcSingleCsv(file, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/locations");
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("PSGC import completed. Provinces: 1, Cities/Municipalities: 2, Barangays: 3.");
        assertThat(redirectAttributes.getFlashAttributes().get("psgcImported")).isEqualTo(true);
    }

    @Test
    void importPsgcSingleCsvFlashesTheFailureReasonWhenTheServiceThrows() throws IOException {
        MockMultipartFile file = new MockMultipartFile("psgcFile", "psgc.csv", "text/csv", "data".getBytes());
        when(locationImportService.importFromSinglePsgcCsv(file)).thenThrow(new IOException("malformed row"));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.importPsgcSingleCsv(file, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/admin/locations");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("PSGC import failed: malformed row");
    }
}
