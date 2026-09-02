package ph.gov.phlpost.inventory.misddashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ph.gov.phlpost.inventory.misddashboard.model.FleetVehicle;

/**
 * Audit rows are keyed on the immutable VehicleID, not the plate number:
 * PlateNumber is nullable while both audit key columns are NOT NULL, and a plate
 * can be filled in or changed later, which would orphan earlier history.
 */
class FleetServiceAuditReferenceTest {

    @Test
    void referenceIsTheVehicleIdEvenWhenAPlateIsPresent() {
        assertEquals("VEHICLE-105", FleetService.auditReferenceId(vehicle(105, "NDJ6010")));
    }

    @Test
    void nullPlateNumberStillYieldsAStableReference() {
        assertEquals("VEHICLE-107", FleetService.auditReferenceId(vehicle(107, null)));
    }

    @Test
    void blankPlateNumberStillYieldsAStableReference() {
        assertEquals("VEHICLE-108", FleetService.auditReferenceId(vehicle(108, "   ")));
    }

    @Test
    void rePlatingAVehicleDoesNotMoveItsHistory() {
        FleetVehicle before = vehicle(106, "AAA 5396");
        FleetVehicle after = vehicle(106, "BBB 1111");
        assertEquals(FleetService.auditReferenceId(before), FleetService.auditReferenceId(after));
    }

    private FleetVehicle vehicle(Integer vehicleId, String plateNumber) {
        FleetVehicle vehicle = new FleetVehicle();
        vehicle.setVehicleID(vehicleId);
        vehicle.setPlateNumber(plateNumber);
        return vehicle;
    }
}
