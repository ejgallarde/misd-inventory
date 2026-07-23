package ph.gov.phlpost.inventory.misddashboard.service;

import ph.gov.phlpost.inventory.misddashboard.model.EquipmentCatalog;
import ph.gov.phlpost.inventory.misddashboard.repository.EquipmentCatalogRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.PersonnelRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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
                // Changed EquipmentCatalog::getCatalogID to c -> c.getCatalogID()
                .collect(Collectors.toMap(c -> c.getCatalogID(), c -> c));
    }

    @Cacheable("departmentMap")
    public Map<String, String> getDepartmentMap() {
        return personnelRepository.findAll().stream()
                .collect(Collectors.toMap(
                        p -> p.getEmployeeID(), // Fixed: Changed from Personnel::getEmployeeID
                        p -> p.getDepartment() != null ? p.getDepartment() : "Unassigned"));
    }
}