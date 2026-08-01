package ph.gov.phlpost.inventory.misddashboard.service;

import ph.gov.phlpost.inventory.misddashboard.model.EquipmentCatalog;
import ph.gov.phlpost.inventory.misddashboard.repository.EquipmentCatalogRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.PersonnelRepository;
import org.springframework.beans.factory.annotation.Value;
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
    private final String supplierOwnerId;

    public RegistryService(PersonnelRepository personnelRepository, EquipmentCatalogRepository catalogRepository,
            @Value("${asset.workflow.supplier-owner-id:SUPPLIER}") String supplierOwnerId) {
        this.personnelRepository = personnelRepository;
        this.catalogRepository = catalogRepository;
        this.supplierOwnerId = supplierOwnerId;
    }

    // Caches the employee list for 1 hour (or until app restart)
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
                        .thenComparing(EquipmentCatalog::getCatalogID))
                .collect(Collectors.toMap(
                        EquipmentCatalog::getCatalogID,
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

    public String getManagerNameByEmployeeId(String employeeId) {
        if (employeeId == null || employeeId.isBlank()) {
            return "No Manager";
        }
        return personnelRepository.findById(employeeId)
                .map(employee -> {
                    String managerID = employee.getManagerID();
                    if (managerID == null || managerID.isBlank()) {
                        return "No Manager";
                    }
                    return getEmployeeNameMap().getOrDefault(managerID, "Unknown Manager");
                })
                .orElse("No Manager");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}