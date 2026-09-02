package ph.gov.phlpost.inventory.misddashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ph.gov.phlpost.inventory.misddashboard.model.FleetVehicle;
import ph.gov.phlpost.inventory.misddashboard.repository.FleetVehicleRepository;
import ph.gov.phlpost.inventory.misddashboard.service.AssetHistoryService;
import ph.gov.phlpost.inventory.misddashboard.service.DocumentService;
import ph.gov.phlpost.inventory.misddashboard.service.FleetService;
import ph.gov.phlpost.inventory.misddashboard.service.RegistryService;

@ExtendWith(MockitoExtension.class)
class FleetControllerTest {

    @Mock
    private FleetVehicleRepository fleetRepo;

    @Mock
    private FleetService fleetService;

    @Mock
    private RegistryService registryService;

    @Mock
    private DocumentService documentService;

    @Mock
    private AssetHistoryService assetHistoryService;

    @InjectMocks
    private FleetController controller;

    @Test
    void returnsVehicleHistoryUsingPlateNumberAuditKey() {
        FleetVehicle vehicle = new FleetVehicle();
        vehicle.setVehicleID(17);
        vehicle.setPlateNumber("ABC-1234");
        var entry = new AssetHistoryService.AssetHistoryEntry(
                LocalDateTime.of(2026, 8, 1, 9, 30),
                "Lifecycle", "Vehicle Returned", "MOTORPOOL", "Returned in good condition");

        when(fleetRepo.findById(17)).thenReturn(Optional.of(vehicle));
        when(assetHistoryService.getHistory("VEHICLE-17")).thenReturn(List.of(entry));

        ResponseEntity<List<AssetHistoryService.AssetHistoryEntry>> response = controller.getVehicleHistory(17);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(entry);
        verify(assetHistoryService).getHistory("VEHICLE-17");
    }

    @Test
    void returnsNotFoundWhenVehicleDoesNotExist() {
        when(fleetRepo.findById(404)).thenReturn(Optional.empty());

        ResponseEntity<List<AssetHistoryService.AssetHistoryEntry>> response = controller.getVehicleHistory(404);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void plateLessVehicleReportsHistoryLoggedAgainstItsId() {
        // Registration does not require a plate number, and audit rows for such a
        // vehicle are filed under "VEHICLE-{id}". Looking history up by the blank
        // plate used to return an empty list, wrongly reporting no history at all.
        var vehicle = new FleetVehicle();
        vehicle.setVehicleID(21);
        vehicle.setPlateNumber("   ");
        var entry = new AssetHistoryService.AssetHistoryEntry(
                LocalDateTime.of(2026, 8, 14, 11, 0),
                "Lifecycle", "Marked Impounded", "SYSTEM", "Held pending clearance");

        when(fleetRepo.findById(21)).thenReturn(Optional.of(vehicle));
        when(assetHistoryService.getHistory("VEHICLE-21")).thenReturn(List.of(entry));

        ResponseEntity<List<AssetHistoryService.AssetHistoryEntry>> response = controller.getVehicleHistory(21);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(entry);
    }
}