package ph.gov.phlpost.inventory.misddashboard.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically drops the personnel-derived caches in {@link RegistryService}.
 *
 * <p>
 * Personnel are maintained outside this application, and nothing ever called
 * {@link RegistryService#evictReferenceDataCaches()}, so a new hire, transfer
 * or manager change stayed invisible until a restart. The interval is
 * app.reference-data.refresh-interval (ISO-8601, default 15 minutes); admins
 * can also refresh at once from the PSGC/reference-data admin page.
 *
 * <p>
 * A separate bean on purpose: calling the evict method through the injected
 * RegistryService proxy is what makes its @CacheEvict take effect.
 */
@Component
public class ReferenceDataRefresher {

    private final RegistryService registryService;

    public ReferenceDataRefresher(RegistryService registryService) {
        this.registryService = registryService;
    }

    @Scheduled(fixedDelayString = "${app.reference-data.refresh-interval:PT15M}",
            initialDelayString = "${app.reference-data.refresh-interval:PT15M}")
    public void refresh() {
        registryService.evictReferenceDataCaches();
    }
}
