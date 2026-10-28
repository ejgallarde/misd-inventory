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
                new AssetWorkflowHelper(),
                "SUPPLIER",
                "Computer Maintenance Technologist",
                100);
    }

    @Test
    void sendForWarrantyAssignsSupplierAndRepairStatuses() {
        Asset asset = asset("TAG-1");
        when(assetRepository.findById("TAG-1")).thenReturn(Optional.of(asset));

        service.sendForWarranty("TAG-1", "RMA-100", "clerk");

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

        service.sendForMisdMaintenance("TAG-2", "TECH-1", "Replace power supply", "clerk");

        assertThat(asset.getCurrentOwnerID()).isEqualTo("TECH-1");
        assertThat(asset.getDeploymentStatus()).isEqualTo("With MISD Technician");
        assertThat(asset.getMaintenanceHealthStatus()).isEqualTo("Under Repair");
        assertThat(asset.getLifecycleStatus()).isEqualTo("Inactive");
        verify(assetRepository).save(asset);
        verify(auditLogService).logAssignment(
                "TAG-2", "TECH-1", "MISD Maintenance", "Replace power supply");
        // The person who sent it, not the technician, is the actor.
        verify(auditLogService).logLifecycleEvent("TAG-2", "clerk", "Sent for MISD Maintenance",
                "Technician: TECH-1; Notes: Replace power supply");
    }

    @Test
    void sendForMisdMaintenanceRejectsEmployeeWithWrongJobTitle() {
        when(personnelRepository.findById("EMP-1")).thenReturn(Optional.of(technician));
        when(technician.getJobTitle()).thenReturn("Administrative Assistant");

        assertThatThrownBy(() -> service.sendForMisdMaintenance("TAG-3", "EMP-1", "Repair", "clerk"))
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

        service.assignAsset("TAG-REASSIGN", "EMP-NEW", "Transferred to new user", "clerk");

        verify(auditLogService).logAssignment(
                "TAG-REASSIGN", "EMP-NEW", "Reassignment", "Transferred to new user");
        verify(auditLogService).logLifecycleEvent(
                org.mockito.ArgumentMatchers.eq("TAG-REASSIGN"),
                org.mockito.ArgumentMatchers.eq("clerk"),
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

        service.updateLifecycle("TAG-BER", "Unserviceable", "Marked Unserviceable", "Failed inspection", "clerk");

        verify(auditLogService).logLifecycleEvent(
                org.mockito.ArgumentMatchers.eq("TAG-BER"),
                org.mockito.ArgumentMatchers.eq("clerk"),
                org.mockito.ArgumentMatchers.eq("Marked Unserviceable"),
                org.mockito.ArgumentMatchers.contains("Health: Beyond Economic Repair (BER)"));
        assertThat(asset.getLifecycleStatus()).isEqualTo("End of Life (EOL)");
    }

    @Test
    void markUnserviceableReleasesTheAssetFromItsHolder() {
        // An asset declared beyond repair used to stay "Deployed" and accountable
        // to its holder, and isUnserviceable then hid every action that could
        // have corrected that.
        Asset asset = asset("TAG-BER-DEPLOYED");
        asset.setCurrentOwnerID("EMP-9");
        asset.setDeploymentStatus("Deployed");
        asset.setMaintenanceHealthStatus("Degraded");
        asset.setLifecycleStatus("Active");
        when(assetRepository.findById("TAG-BER-DEPLOYED")).thenReturn(Optional.of(asset));
        when(personnelRepository.existsById("EMP-9")).thenReturn(true);

        service.updateLifecycle("TAG-BER-DEPLOYED", "Unserviceable", "Marked Unserviceable", "Water damage", "clerk");

        assertThat(asset.getCurrentOwnerID()).isNull();
        assertThat(asset.getDeploymentStatus()).isEqualTo("Unavailable");
        assertThat(asset.getMaintenanceHealthStatus()).isEqualTo("Beyond Economic Repair (BER)");
        verify(auditLogService).logAssignment("TAG-BER-DEPLOYED", "EMP-9", "Returned - Unserviceable", "Water damage");
    }

    @Test
    void receiveAssetsRejectsAQuantityBelowOne() {
        // The form's min="1" is client-side only.
        assertThatThrownBy(() -> service.receiveAssets(new Asset(), 0, "tester"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 1");
    }

    @Test
    void receiveAssetsRejectsAQuantityAboveTheConfiguredCeiling() {
        assertThatThrownBy(() -> service.receiveAssets(new Asset(), 101, "tester"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum of 100");
    }

    @Test
    void receivingASingleUnitKeepsItsSerialNumberAndGeneratesATag() {
        Asset submitted = new Asset();
        submitted.setCatalogID(19);
        submitted.setSerialNumber("SN-12345");
        when(assetRepository.findTopByAssetTagStartingWithOrderByAssetTagDesc(
                org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());

        java.util.List<String> tags = service.receiveAssets(submitted, 1, "tester");

        assertThat(tags).hasSize(1);
        assertThat(tags.get(0)).endsWith("00001");
        verify(assetRepository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(
                saved -> "SN-12345".equals(saved.getSerialNumber())
                        && "In Storage".equals(saved.getDeploymentStatus())
                        && saved.getCurrentOwnerID() == null));
    }

    @Test
    void receivingMultipleUnitsLeavesSerialNumbersUnsetSoTheyDoNotCollide() {
        // SerialNumber is UNIQUE; one submitted value cannot belong to three units.
        Asset submitted = new Asset();
        submitted.setCatalogID(19);
        submitted.setSerialNumber("SN-12345");
        when(assetRepository.findTopByAssetTagStartingWithOrderByAssetTagDesc(
                org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());

        java.util.List<String> tags = service.receiveAssets(submitted, 3, "tester");

        assertThat(tags).hasSize(3);
        verify(assetRepository, org.mockito.Mockito.times(3)).saveAndFlush(
                org.mockito.ArgumentMatchers.argThat(saved -> saved.getSerialNumber() == null));
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

        service.markRepaired("TAG-4", "Passed diagnostics", "clerk");

        assertThat(asset.getCurrentOwnerID()).isNull();
        assertThat(asset.getDeploymentStatus()).isEqualTo("In Storage");
        assertThat(asset.getMaintenanceHealthStatus()).isEqualTo("Operational");
        assertThat(asset.getLifecycleStatus()).isEqualTo("Active");
        verify(assetRepository).save(asset);
        verify(auditLogService).logLifecycleEvent(
                "TAG-4", "clerk", "Asset Repaired", "Passed diagnostics");
    }

    @Test
    void markRepairedHandlesLegacyRepairWithoutAccountableOwner() {
        Asset asset = asset("TAG-LEGACY");
        asset.setCurrentOwnerID(null);
        asset.setDeploymentStatus("With Service Center");
        asset.setMaintenanceHealthStatus("Under Repair");
        asset.setLifecycleStatus("Inactive");
        when(assetRepository.findById("TAG-LEGACY")).thenReturn(Optional.of(asset));

        service.markRepaired("TAG-LEGACY", "Passed diagnostics", "clerk");

        assertThat(asset.getDeploymentStatus()).isEqualTo("In Storage");
        assertThat(asset.getMaintenanceHealthStatus()).isEqualTo("Operational");
        assertThat(asset.getLifecycleStatus()).isEqualTo("Active");
        verify(auditLogService, never()).logAssignment(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
        verify(auditLogService).logLifecycleEvent(
                "TAG-LEGACY", "clerk", "Asset Repaired", "Passed diagnostics");
    }

    @Test
    void markRepairedRejectsAssetOutsideRepairWorkflow() {
        Asset asset = asset("TAG-5");
        asset.setDeploymentStatus("Deployed");
        asset.setMaintenanceHealthStatus("Operational");
        when(assetRepository.findById("TAG-5")).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.markRepaired("TAG-5", "Not applicable", "clerk"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currently assigned for repair");

        verify(assetRepository, never()).save(asset);
    }

    @Test
    void retiredAssetCannotBeAssignedEvenByADirectRequest() {
        Asset asset = asset("TAG-RET");
        asset.setDeploymentStatus("In Storage");
        asset.setLifecycleStatus("Decommissioned / Retired");
        when(assetRepository.findById("TAG-RET")).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.assignAsset("TAG-RET", "EMP-1", null, "clerk"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Cannot assign it: asset TAG-RET");
        verify(assetRepository, never()).save(asset);
    }

    @Test
    void berAssetCannotBeAssignedOrSentForRepair() {
        Asset asset = asset("TAG-BER2");
        asset.setDeploymentStatus("In Storage");
        asset.setMaintenanceHealthStatus("Beyond Economic Repair (BER)");
        asset.setLifecycleStatus("End of Life (EOL)");
        when(assetRepository.findById("TAG-BER2")).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.assignAsset("TAG-BER2", "EMP-1", null, "clerk"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.sendForWarranty("TAG-BER2", null, "clerk"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(assetRepository, never()).save(asset);
    }

    @Test
    void returningAnAssetThatIsInStorageIsRejected() {
        Asset asset = asset("TAG-STORED");
        asset.setDeploymentStatus("In Storage");
        asset.setLifecycleStatus("Active");
        when(assetRepository.findById("TAG-STORED")).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> service.returnAsset("TAG-STORED", null, "clerk"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("return it to MISD");
    }

    @Test
    void updateCopiesOnlyEditableFieldsAndKeepsARecordedSerialNumber() {
        Asset stored = asset("TAG-EDIT");
        stored.setSerialNumber("SN-ORIGINAL");
        stored.setPurchasePrice(new java.math.BigDecimal("1000.00"));
        stored.setDeploymentStatus("In Storage");
        stored.setMaintenanceHealthStatus("Operational");
        stored.setLifecycleStatus("Active");
        when(assetRepository.findById("TAG-EDIT")).thenReturn(Optional.of(stored));

        Asset submitted = asset("TAG-EDIT");
        submitted.setSerialNumber("SN-TAMPERED");
        submitted.setPurchasePrice(new java.math.BigDecimal("1200.00"));
        submitted.setDeploymentStatus("In Storage");
        submitted.setMaintenanceHealthStatus("Degraded");
        submitted.setLifecycleStatus("Active");
        submitted.setRemarks("Cracked hinge");

        service.updateAsset(submitted, "clerk", true, true);

        verify(assetRepository).save(stored);
        assertThat(stored.getSerialNumber()).isEqualTo("SN-ORIGINAL");
        assertThat(stored.getPurchasePrice()).isEqualByComparingTo("1200.00");
        assertThat(stored.getMaintenanceHealthStatus()).isEqualTo("Degraded");
        assertThat(stored.getRemarks()).isEqualTo("Cracked hinge");
        verify(auditLogService).logLifecycleEvent(
                org.mockito.ArgumentMatchers.eq("TAG-EDIT"),
                org.mockito.ArgumentMatchers.eq("clerk"),
                org.mockito.ArgumentMatchers.eq("Asset Status Updated"),
                org.mockito.ArgumentMatchers.contains("Health: Operational"));
    }

    @Test
    void clerkEditLeavesThePurchasePriceAlone() {
        Asset stored = asset("TAG-CLERK");
        stored.setPurchasePrice(new java.math.BigDecimal("1000.00"));
        when(assetRepository.findById("TAG-CLERK")).thenReturn(Optional.of(stored));
        Asset submitted = asset("TAG-CLERK");
        submitted.setPurchasePrice(new java.math.BigDecimal("1.00"));
        submitted.setRemarks("Relabelled");

        service.updateAsset(submitted, "clerk", true, false);

        assertThat(stored.getPurchasePrice()).isEqualByComparingTo("1000.00");
        assertThat(stored.getRemarks()).isEqualTo("Relabelled");
    }

    @Test
    void financeEditChangesOnlyThePurchasePriceAndRecordsBothValues() {
        Asset stored = asset("TAG-FIN");
        stored.setPurchasePrice(new java.math.BigDecimal("1000.00"));
        stored.setDeploymentStatus("In Storage");
        stored.setMaintenanceHealthStatus("Operational");
        stored.setLifecycleStatus("Active");
        when(assetRepository.findById("TAG-FIN")).thenReturn(Optional.of(stored));
        Asset submitted = asset("TAG-FIN");
        submitted.setPurchasePrice(new java.math.BigDecimal("950.00"));
        submitted.setDeploymentStatus("Deployed");
        submitted.setCurrentOwnerID("EMP-1");

        service.updateAsset(submitted, "finance", false, true);

        assertThat(stored.getPurchasePrice()).isEqualByComparingTo("950.00");
        assertThat(stored.getDeploymentStatus()).isEqualTo("In Storage");
        assertThat(stored.getCurrentOwnerID()).isNull();
        verify(auditLogService).logLifecycleEvent(
                org.mockito.ArgumentMatchers.eq("TAG-FIN"),
                org.mockito.ArgumentMatchers.eq("finance"),
                org.mockito.ArgumentMatchers.eq("Asset Cost Updated"),
                org.mockito.ArgumentMatchers.endsWith("; Purchase price: 1000.00 -> 950.00"));
    }

    @Test
    void updateFillsInABlankSerialNumber() {
        Asset stored = asset("TAG-NOSN");
        when(assetRepository.findById("TAG-NOSN")).thenReturn(Optional.of(stored));
        Asset submitted = asset("TAG-NOSN");
        submitted.setSerialNumber("SN-NEW");

        service.updateAsset(submitted, "clerk", true, true);

        assertThat(stored.getSerialNumber()).isEqualTo("SN-NEW");
    }

    private Asset asset(String assetTag) {
        Asset asset = new Asset();
        asset.setAssetTag(assetTag);
        return asset;
    }
}