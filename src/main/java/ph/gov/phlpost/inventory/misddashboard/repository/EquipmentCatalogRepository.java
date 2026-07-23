package ph.gov.phlpost.inventory.misddashboard.repository;

import ph.gov.phlpost.inventory.misddashboard.model.EquipmentCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.stereotype.Repository;

// @Repository
public interface EquipmentCatalogRepository extends JpaRepository<EquipmentCatalog, Integer> {
}