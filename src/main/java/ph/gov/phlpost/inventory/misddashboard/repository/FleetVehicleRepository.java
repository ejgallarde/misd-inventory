package ph.gov.phlpost.inventory.misddashboard.repository;

import ph.gov.phlpost.inventory.misddashboard.model.FleetVehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FleetVehicleRepository extends JpaRepository<FleetVehicle, Integer> {

    @Query(value = "SELECT COUNT(*) FROM FleetVehicles WHERE AdminLegaltionalStatus IN ('Registration Expired', 'Impounded')", nativeQuery = true)
    long countVehiclesWithAdminLegalIssues();

    @Query(value = "SELECT COUNT(*) FROM FleetVehicles WHERE COALESCE(AdminLegaltionalStatus, '') NOT IN ('Sold', 'Disposed', 'Decommissioned') AND (OperationalStatus IN ('Grounded', 'Missing/Stolen', 'Slated for Disposal') OR MaintenanceStatus IN ('Scheduled Maintenance', 'Under Repair', 'Awaiting Parts', 'Beyond Economic Repair (BER)'))", nativeQuery = true)
    long countVehiclesWithOperationalMaintenanceIssues();

    @Query(value = "SELECT COUNT(*) FROM FleetVehicles WHERE AdminLegaltionalStatus = 'Sold'", nativeQuery = true)
    long countSoldVehicles();

    @Query(value = "SELECT COUNT(*) FROM FleetVehicles WHERE AdminLegaltionalStatus IN ('Disposed', 'Decommissioned')", nativeQuery = true)
    long countDisposedOrDecommissionedVehicles();

    @Query(value = "SELECT COUNT(*) FROM FleetVehicles WHERE RegistrationExpiry BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)", nativeQuery = true)
    long countExpiringRegistrations();

    @Query(value = "SELECT COUNT(*) FROM FleetVehicles WHERE " +
            "COALESCE(AdminLegaltionalStatus, '') NOT IN ('Sold', 'Disposed', 'Decommissioned') AND (" +
            "(YEAR(CURDATE()) - ManufactureYear) >= 10 " +
            "OR RegistrationExpiry BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY) " +
            "OR AdminLegaltionalStatus IN ('Registration Expired', 'Impounded') " +
            "OR OperationalStatus IN ('Grounded', 'Missing/Stolen', 'Slated for Disposal') " +
            "OR MaintenanceStatus IN ('Scheduled Maintenance', 'Under Repair', 'Awaiting Parts', 'Beyond Economic Repair (BER)')"
            +
            ")", nativeQuery = true)
    long countProblematicVehicles();

    @Query(value = "SELECT * FROM FleetVehicles WHERE " +
            "COALESCE(AdminLegaltionalStatus, '') NOT IN ('Sold', 'Disposed', 'Decommissioned') AND (" +
            "(YEAR(CURDATE()) - ManufactureYear) >= 10 " +
            "OR RegistrationExpiry BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY) " +
            "OR AdminLegaltionalStatus IN ('Registration Expired', 'Impounded') " +
            "OR OperationalStatus IN ('Grounded', 'Missing/Stolen', 'Slated for Disposal') " +
            "OR MaintenanceStatus IN ('Scheduled Maintenance', 'Under Repair', 'Awaiting Parts', 'Beyond Economic Repair (BER)')"
            +
            ")", nativeQuery = true)
    List<FleetVehicle> findProblematicVehicles();
}