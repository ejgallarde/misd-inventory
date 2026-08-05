package ph.gov.phlpost.inventory.misddashboard.repository;

import ph.gov.phlpost.inventory.misddashboard.model.EquipmentCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentCatalogRepository extends JpaRepository<EquipmentCatalog, Integer> {
}