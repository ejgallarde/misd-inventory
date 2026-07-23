package ph.gov.phlpost.inventory.misddashboard.repository;

import ph.gov.phlpost.inventory.misddashboard.model.FleetVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FleetVehicleRepository extends JpaRepository<FleetVehicle, Integer> {

    @Query(value = "SELECT COUNT(*) FROM FleetVehicles WHERE CurrentStatus IN ('In Repair', 'In Shop', 'Maintenance')", nativeQuery = true)
    long countVehiclesInRepair();

    @Query(value = "SELECT COUNT(*) FROM FleetVehicles WHERE RegistrationExpiry BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)", nativeQuery = true)
    long countExpiringRegistrations();

    // Aging threshold: 10 years old OR 200,000+ mileage
    // Convert to a view
    @Query(value = "SELECT * FROM FleetVehicles WHERE (YEAR(CURDATE()) - ManufactureYear) >= 10", nativeQuery = true)
    List<FleetVehicle> findAgingVehicles();
}