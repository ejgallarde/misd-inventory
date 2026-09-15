package ph.gov.phlpost.inventory.misddashboard.service;

import ph.gov.phlpost.inventory.misddashboard.model.EquipmentCatalog;
import ph.gov.phlpost.inventory.misddashboard.model.FleetVehicleCatalog;
import ph.gov.phlpost.inventory.misddashboard.model.PersonnelBaseLocation;
import ph.gov.phlpost.inventory.misddashboard.model.SurveyEquipmentCatalog;
import ph.gov.phlpost.inventory.misddashboard.repository.EquipmentCatalogRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.FleetVehicleCatalogRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.PersonnelRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.SurveyEquipmentCatalogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RegistryService {

    private final PersonnelRepository personnelRepository;
    private final EquipmentCatalogRepository catalogRepository;
    private final SurveyEquipmentCatalogRepository surveyCatalogRepository;
    private final FleetVehicleCatalogRepository fleetCatalogRepository;
    private final String supplierOwnerId;

    public RegistryService(PersonnelRepository personnelRepository, EquipmentCatalogRepository catalogRepository,
            SurveyEquipmentCatalogRepository surveyCatalogRepository,
            FleetVehicleCatalogRepository fleetCatalogRepository,
            @Value("${asset.workflow.supplier-owner-id:SUPPLIER}") String supplierOwnerId) {
        this.personnelRepository = personnelRepository;
        this.catalogRepository = catalogRepository;
        this.surveyCatalogRepository = surveyCatalogRepository;
        this.fleetCatalogRepository = fleetCatalogRepository;
        this.supplierOwnerId = supplierOwnerId;
    }

    /*
     * These maps are cached for the life of the process.
     *
     * There is no cache provider on the classpath, so Spring uses a plain
     * ConcurrentMapCache, which has no TTL — an earlier comment here claimed a
     * one-hour expiry that never existed. Personnel changes made directly in the
     * database therefore do not appear until the caches are evicted; call
     * evictReferenceDataCaches() after any change to the Personnel table.
     */
    @Cacheable("employeeMap")
    public Map<String, String> getEmployeeNameMap() {
        Map<String, String> employeeNames = personnelRepository.findAll().stream()
                .collect(Collectors.toMap(p -> p.getEmployeeID(),
                        p -> p.getLastName() + ", " + p.getFirstName(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));
        employeeNames.put(supplierOwnerId, "Supplier");
        return employeeNames;
    }

    // Caches the catalog items
    @Cacheable("catalogMap")
    public Map<Integer, EquipmentCatalog> getCatalogMap() {
        return catalogRepository.findAll().stream()
                .sorted(Comparator
                        .comparing((EquipmentCatalog c) -> normalize(c.getManufacturer()))
                        .thenComparing(c -> normalize(c.getModelName()))
                        .thenComparing(c -> c.getCatalogID()))
                .collect(Collectors.toMap(
                        c -> c.getCatalogID(),
                        c -> c,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));
    }

    // Caches the survey equipment catalog items
    @Cacheable("surveyCatalogMap")
    public Map<Integer, SurveyEquipmentCatalog> getSurveyCatalogMap() {
        return surveyCatalogRepository.findAll().stream()
                .sorted(Comparator
                        .comparing((SurveyEquipmentCatalog c) -> normalize(c.getManufacturer()))
                        .thenComparing(c -> normalize(c.getModelName()))
                        .thenComparing(c -> c.getCatalogID()))
                .collect(Collectors.toMap(
                        c -> c.getCatalogID(),
                        c -> c,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));
    }

    // Caches the fleet vehicle catalog items
    @Cacheable("fleetCatalogMap")
    public Map<Integer, FleetVehicleCatalog> getFleetCatalogMap() {
        return fleetCatalogRepository.findAll().stream()
                .sorted(Comparator
                        .comparing((FleetVehicleCatalog c) -> normalize(c.getManufacturer()))
                        .thenComparing(c -> normalize(c.getModelName()))
                        .thenComparing(c -> c.getCatalogID()))
                .collect(Collectors.toMap(
                        c -> c.getCatalogID(),
                        c -> c,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));
    }

    @Cacheable("departmentMap")
    public Map<String, String> getDepartmentMap() {
        Map<String, String> departments = personnelRepository.findAll().stream()
                .collect(Collectors.toMap(
                        p -> p.getEmployeeID(),
                        p -> p.getDepartment() != null ? p.getDepartment() : "Unassigned",
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));
        departments.put(supplierOwnerId, "External Supplier");
        return departments;
    }

    @Cacheable("divisionMap")
    public Map<String, String> getDivisionMap() {
        Map<String, String> divisions = personnelRepository.findAll().stream()
                .collect(Collectors.toMap(
                        p -> p.getEmployeeID(),
                        p -> p.getDivision() != null ? p.getDivision() : "Unassigned",
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));
        divisions.put(supplierOwnerId, "Warranty Service");
        return divisions;
    }

    @Cacheable("personnelLocationMap")
    public Map<String, String> getPersonnelLocationMap() {
        Map<String, String> locations = personnelRepository.findAll().stream()
                .collect(Collectors.toMap(
                        personnel -> personnel.getEmployeeID(),
                        personnel -> formatLocation(personnel.getBaseLocation()),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));
        locations.put(supplierOwnerId, "External Service Center");
        return locations;
    }

    /**
     * Drops every cached personnel-derived map so the next lookup re-reads the
     * database. Without this, a new hire, a transfer, or a department change is
     * invisible until the application restarts.
     */
    @CacheEvict(cacheNames = { "allPersonnel", "employeeMap", "departmentMap", "divisionMap",
            "personnelLocationMap" }, allEntries = true)
    public void evictReferenceDataCaches() {
        // Annotation-driven; the body is intentionally empty.
    }

    public String resolveDisplayName(String employeeId) {
        if (employeeId == null || employeeId.isBlank()) {
            return "Unassigned";
        }
        return getEmployeeNameMap().getOrDefault(employeeId, employeeId);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String formatLocation(PersonnelBaseLocation location) {
        if (location == null) {
            return "Unassigned";
        }
        return Stream.of(location.getArea(), location.getProvince(), location.getOfficeAddress())
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));
    }
}