package ph.gov.phlpost.inventory.misddashboard.service;

import ph.gov.phlpost.inventory.misddashboard.model.EquipmentCatalog;
import ph.gov.phlpost.inventory.misddashboard.repository.EquipmentCatalogRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.PersonnelRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RegistryService {

    private final PersonnelRepository personnelRepository;
    private final EquipmentCatalogRepository catalogRepository;

    public RegistryService(PersonnelRepository personnelRepository, EquipmentCatalogRepository catalogRepository) {
        this.personnelRepository = personnelRepository;
        this.catalogRepository = catalogRepository;
    }

    // Caches the employee list for 1 hour (or until app restart)
    @Cacheable("employeeMap")
    public Map<String, String> getEmployeeNameMap() {
        return personnelRepository.findAll().stream()
                // Changed Personnel::getEmployeeID to p -> p.getEmployeeID()
                .collect(Collectors.toMap(p -> p.getEmployeeID(),
                        p -> p.getLastName() + ", " + p.getFirstName()));
    }

    // Caches the catalog items
    @Cacheable("catalogMap")
    public Map<Integer, EquipmentCatalog> getCatalogMap() {
        return catalogRepository.findAll().stream()
                .sorted(Comparator
                        .comparing((EquipmentCatalog c) -> normalize(c.getManufacturer()))
                        .thenComparing(c -> normalize(c.getModelName()))
                        .thenComparing(EquipmentCatalog::getCatalogID))
                .collect(Collectors.toMap(
                        EquipmentCatalog::getCatalogID,
                        c -> c,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));
    }

    @Cacheable("departmentMap")
    public Map<String, String> getDepartmentMap() {
        return personnelRepository.findAll().stream()
                .collect(Collectors.toMap(
                        p -> p.getEmployeeID(), // Fixed: Changed from Personnel::getEmployeeID
                        p -> p.getDepartment() != null ? p.getDepartment() : "Unassigned"));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}