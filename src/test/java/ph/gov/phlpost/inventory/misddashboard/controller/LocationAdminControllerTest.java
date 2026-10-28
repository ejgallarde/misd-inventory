package ph.gov.phlpost.inventory.misddashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import ph.gov.phlpost.inventory.misddashboard.service.LocationImportService;
import ph.gov.phlpost.inventory.misddashboard.service.ReferenceDataRefresher;
import ph.gov.phlpost.inventory.misddashboard.service.RegistryService;

@ExtendWith(MockitoExtension.class)
class LocationAdminControllerTest {

    @Mock
    private LocationImportService locationImportService;

    @Mock
    private ReferenceDataRefresher referenceDataRefresher;

    @InjectMocks
    private LocationAdminController controller;

    @Test
    void reloadNowRefreshesPersonnelCachesAndConfirms() {
        var flash = new RedirectAttributesModelMap();

        String view = controller.refreshReferenceData(flash);

        verify(referenceDataRefresher).refresh();
        assertThat(view).isEqualTo("redirect:/admin/locations");
        assertThat(flash.getFlashAttributes().get("successMessage")).asString().contains("reloaded");
    }

    @Test
    void scheduledRefreshEvictsThroughTheRegistryService() {
        // Through the injected (proxied) bean, so @CacheEvict applies.
        RegistryService registry = org.mockito.Mockito.mock(RegistryService.class);

        new ReferenceDataRefresher(registry).refresh();

        verify(registry).evictReferenceDataCaches();
    }
}
