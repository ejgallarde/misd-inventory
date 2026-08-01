package ph.gov.phlpost.inventory.misddashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ph.gov.phlpost.inventory.misddashboard.model.Asset;
import ph.gov.phlpost.inventory.misddashboard.model.Personnel;
import ph.gov.phlpost.inventory.misddashboard.repository.AssetRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.PersonnelRepository;

@ExtendWith(MockitoExtension.class)
class ITAssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private PersonnelRepository personnelRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private Personnel technician;

    private ITAssetService service;

    @BeforeEach
    void setUp() {
        service = new ITAssetService(
                assetRepository,
                personnelRepository,
                auditLogService,
                "SUPPLIER",
                "Computer Maintenance Technologist");
    }

    @Test
    void sendForWarrantyAssignsSupplierAndRepairStatuses() {
        Asset asset = asset("TAG-1");
        when(assetRepository.findById("TAG-1")).thenReturn(Optional.of(asset));

        service.sendForWarranty("TAG-1", "RMA-100");

        assertThat(asset.getCurrentOwnerID()).isEqualTo("SUPPLIER");
        assertThat(asset.getDeploymentStatus()).isEqualTo("With Service Center");
        assertThat(asset.getMaintenanceHealthStatus()).isEqualTo("Under Repair");
        assertThat(asset.getLifecycleStatus()).isEqualTo("Inactive");
        verify(personnelRepository).saveAndFlush(org.mockito.ArgumentMatchers.any(Personnel.class));
        verify(assetRepository).save(asset);
        verify(auditLogService).logAssignment("TAG-1", "SUPPLIER", "Warranty Repair", "RMA-100");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Computer Maintenance Technologist",
            "Computer Maintenance Technologist I",
            "Computer Maintenance Technologist II",
            "Computer Maintenance Technologist III"
    })
    void sendForMisdMaintenanceAssignsEligibleTechnicianAndRepairStatuses(String jobTitle) {
        Asset asset = asset("TAG-2");
        when(assetRepository.findById("TAG-2")).thenReturn(Optional.of(asset));
        when(personnelRepository.findById("TECH-1")).thenReturn(Optional.of(technician));
        when(technician.getEmployeeID()).thenReturn("TECH-1");
        when(technician.getJobTitle()).thenReturn(jobTitle);

        service.sendForMisdMaintenance("TAG-2", "TECH-1", "Replace power supply");

        assertThat(asset.getCurrentOwnerID()).isEqualTo("TECH-1");
        assertThat(asset.getDeploymentStatus()).isEqualTo("With MISD Technician");
        assertThat(asset.getMaintenanceHealthStatus()).isEqualTo("Under Repair");
        assertThat(asset.getLifecycleStatus()).isEqualTo("Inactive");
        verify(assetRepository).save(asset);
        verify(auditLogService).logAssignment(
                "TAG-2", "TECH-1", "MISD Maintenance", "Replace power supply");
    }

    @Test
    void sendForMisdMaintenanceRejectsEmployeeWithWrongJobTitle() {
        when(personnelRepository.findById("EMP-1")).thenReturn(Optional.of(technician));
        when(technician.getJobTitle()).thenReturn("Administrative Assistant");

        assertThatThrownBy(() -> service.sendForMisdMaintenance("TAG-3", "EMP-1", "Repair"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Computer Maintenance Technologist");

        verify(assetRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void assignAssetRecordsReassignmentAndResultingStatuses() {
        Asset asset = asset("TAG-REASSIGN");
        asset.setCurrentOwnerID("EMP-OLD");
        asset.setDeploymentStatus("Deployed");
        asset.setMaintenanceHealthStatus("Operational");
        asset.setLifecycleStatus("Active");
        when(assetRepository.findById("TAG-REASSIGN")).thenReturn(Optional.of(asset));

        service.assignAsset("TAG-REASSIGN", "EMP-NEW", "Transferred to new user");

        verify(auditLogService).logAssignment(
                "TAG-REASSIGN", "EMP-NEW", "Reassignment", "Transferred to new user");
        verify(auditLogService).logLifecycleEvent(
                org.mockito.ArgumentMatchers.eq("TAG-REASSIGN"),
                org.mockito.ArgumentMatchers.eq("SYSTEM"),
                org.mockito.ArgumentMatchers.eq("Reassignment"),
                org.mockito.ArgumentMatchers.contains("Deployment: Deployed"));
    }

    @Test
    void markUnserviceableRecordsResultingBerAndEndOfLifeStatuses() {
        Asset asset = asset("TAG-BER");
        asset.setDeploymentStatus("In Storage");
        asset.setMaintenanceHealthStatus("Degraded");
        asset.setLifecycleStatus("Active");
        when(assetRepository.findById("TAG-BER")).thenReturn(Optional.of(asset));

        service.updateLifecycle("TAG-BER", "Unserviceable", "Marked Unserviceable", "Failed inspection");

        verify(auditLogService).logLifecycleEvent(
                org.mockito.ArgumentMatchers.eq("TAG-BER"),
                org.mockito.ArgumentMatchers.eq("SYSTEM"),
                org.mockito.ArgumentMatchers.eq("Marked Unserviceable"),
                org.mockito.ArgumentMatchers.contains("Health: Beyond Economic Repair (BER)"));
        assertThat(asset.getLifecycleStatus()).isEqualTo("End of Life (EOL)");
    }

    @Test
    void markRepairedReturnsAssetToStorageAndClearsOwner() {
        Asset asset = asset("TAG-4");
        asset.setCurrentOwnerID("TECH-1");
        asset.setDeploymentStatus("With MISD Technician");
        asset.setMaintenanceHealthStatus("Under Repair");
        asset.setLifecycleStatus("Inactive");
        when(assetRepository.findById("TAG-4")).thenReturn(Optional.of(asset));
        when(personnelRepository.existsById("TECH-1")).thenReturn(true);

        service.markRepaired("TAG-4", "Passed diagnostics");

        assertThat(asset.getCurrentOwnerID()).isNull();
        assertThat(asset.getDeploymentStatus()).isEqualTo("In Storage");
        assertThat(asset.getMaintenanceHealthStatus()).isEqualTo("Operational");
        assertThat(asset.getLifecycleStatus()).isEqualTo("Active");
        verify(assetRepository).save(asset);
        verify(auditLogService).logLifecycleEvent(
                "TAG-4", "SYSTEM", "Asset Repaired", "Passed diagnostics");
    }

    @Test
    void markRepairedHandlesLegacyRepairWithoutAccountableOwner() {
        Asset asset = asset("TAG-LEGACY");
        asset.setCurrentOwnerID(null);
        asset.setDeploymentStatus("With Service Center");
        asset.setMaintenanceHealthStatus("Under Repair");
        asset.setLifecycleStatus("Inactive");
        when(assetRepository.findById("TAG-LEGACY")).thenReturn(Optional.of(asset));

        service.markRepaired("TAG-LEGACY", "Passed diagnostics");

        assertThat(asset.getDeploymentStatus()).isEqualTo("In Storage");
        assertThat(asset.getMaintenanceHealthStatus()).isEqualTo("Operational");
        assertThat(asset.getLifecycleStatus()).isEqualTo("Active");
        verify(auditLogService, never()).logAssignment(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
        verify(auditLogService).logLifecycleEvent(
                "TAG-LEGACY", "SYSTEM", "Asset Repaired", "Passed diagnostics");
    }

    @Test
    void markRepairedRejectsAssetOutsideRepairWorkflow() {
        Asset asset = asset("TAG-5");
        asset.setDeploymentStatus("Deployed");
        asset.setMaintenanceHealthStatus("Operational");
        when(assetRepository.findById("TAG-5")).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.markRepaired("TAG-5", "Not applicable"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currently assigned for repair");

        verify(assetRepository, never()).save(asset);
    }

    private Asset asset(String assetTag) {
        Asset asset = new Asset();
        asset.setAssetTag(assetTag);
        return asset;
    }
}