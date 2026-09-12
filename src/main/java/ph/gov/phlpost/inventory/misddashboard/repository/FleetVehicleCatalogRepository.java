package ph.gov.phlpost.inventory.misddashboard.repository;

import ph.gov.phlpost.inventory.misddashboard.model.FleetVehicleCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FleetVehicleCatalogRepository extends JpaRepository<FleetVehicleCatalog, Integer> {
}
