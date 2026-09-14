package ph.gov.phlpost.inventory.misddashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import ph.gov.phlpost.inventory.misddashboard.model.Asset;
import ph.gov.phlpost.inventory.misddashboard.repository.AssetRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.EquipmentCatalogRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.PersonnelRepository;
import ph.gov.phlpost.inventory.misddashboard.service.AssetHistoryService;
import ph.gov.phlpost.inventory.misddashboard.service.DocumentService;
import ph.gov.phlpost.inventory.misddashboard.service.ITAssetService;
import ph.gov.phlpost.inventory.misddashboard.service.RegistryService;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class ITAssetControllerTest {

    @Mock
    private AssetRepository assetRepo;

    @Mock
    private EquipmentCatalogRepository catalogRepo;

    @Mock
    private ITAssetService assetService;

    @Mock
    private RegistryService registryService;

    @Mock
    private DocumentService documentService;

    @Mock
    private AssetHistoryService assetHistoryService;

    @Mock
    private PersonnelRepository personnelRepo;

    @Mock
    private JsonMapper jsonMapper;

    @InjectMocks
    private ITAssetController controller;

    @Test
    void receiveAssetFlashesSuccessWithTheCreatedAssetCount() {
        Asset baseAsset = new Asset();
        baseAsset.setCatalogID(1);
        when(assetService.receiveAssets(baseAsset, 2, "SYSTEM"))
                .thenReturn(List.of("PPC-2026-01-01-00001", "PPC-2026-01-01-00002"));
        when(documentService.hasFiles(null)).thenReturn(false);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.receiveAsset(baseAsset, 2, null, null, null, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/");
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("Successfully received 2 asset(s) into storage.");
    }

    @Test
    void receiveAssetFlashesTheValidationErrorInsteadOfPropagatingIt() {
        Asset baseAsset = new Asset();
        when(assetService.receiveAssets(any(), eq(0), any()))
                .thenThrow(new IllegalArgumentException("Quantity must be at least 1."));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.receiveAsset(baseAsset, 0, null, null, null, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("Quantity must be at least 1.");
    }

    @Test
    void assignAssetFlashesSuccessAndRedirectsBackToTheSubmittingSearchAndPage() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.assignAsset("PPC-1", "PERS-0001", null, "laptop", 2, redirectAttributes);

        verify(assetService).assignAsset("PPC-1", "PERS-0001", null);
        assertThat(view).isEqualTo("redirect:/assets?search=laptop&page=2");
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage"))
                .isEqualTo("Asset assigned successfully.");
    }

    @Test
    void assignAssetFlashesTheErrorWhenTheServiceRejectsIt() {
        doThrow(new IllegalArgumentException("Asset not found."))
                .when(assetService).assignAsset("PPC-404", "PERS-0001", null);
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.assignAsset("PPC-404", "PERS-0001", null, null, null, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/assets");
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage")).isEqualTo("Asset not found.");
    }

    @Test
    void returnAssetDelegatesToTheServiceAndFlashesSuccess() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller.returnAsset("PPC-1", "Good condition", null, null, redirectAttributes);

        verify(assetService).returnAsset("PPC-1", "Good condition");
        assertThat(redirectAttributes.getFlashAttributes().get("successMessage")).isEqualTo("Asset returned to MISD.");
    }

    @Test
    void markUnserviceableDelegatesWithTheExpectedLifecycleArguments() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller.markUnserviceable("PPC-1", "Broken screen", null, null, redirectAttributes);

        verify(assetService).updateLifecycle("PPC-1", "Unserviceable", "Marked Unserviceable", "Broken screen");
    }

    @Test
    void markForWarrantyDelegatesToTheService() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller.markForWarranty("PPC-1", "DOA on arrival", null, null, redirectAttributes);

        verify(assetService).sendForWarranty("PPC-1", "DOA on arrival");
    }

    @Test
    void sendForMisdMaintenanceDelegatesToTheService() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller.sendForMisdMaintenance("PPC-1", "PERS-TECH", "Won't boot", null, null, redirectAttributes);

        verify(assetService).sendForMisdMaintenance("PPC-1", "PERS-TECH", "Won't boot");
    }

    @Test
    void markAssetRepairedDelegatesToTheService() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller.markAssetRepaired("PPC-1", "Replaced keyboard", null, null, redirectAttributes);

        verify(assetService).markRepaired("PPC-1", "Replaced keyboard");
    }

    @Test
    void retireAssetDelegatesWithTheExpectedLifecycleArguments() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        controller.retireAsset("PPC-1", "End of service life", null, null, redirectAttributes);

        verify(assetService).updateLifecycle("PPC-1", "Retired", "Asset Retired", "End of service life");
    }

    @Test
    void getITAssetDetailsReturnsNotFoundForAnUnknownAssetTag() {
        when(assetRepo.findById("PPC-404")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getITAssetDetails("PPC-404");

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void getITAssetDetailsReturnsTheAssetWhenFound() {
        Asset asset = new Asset();
        asset.setAssetTag("PPC-1");
        asset.setCatalogID(1);
        asset.setDeploymentStatus("In Storage");
        asset.setMaintenanceHealthStatus("Operational");
        asset.setLifecycleStatus("Active");
        when(assetRepo.findById("PPC-1")).thenReturn(Optional.of(asset));
        when(registryService.getCatalogMap()).thenReturn(java.util.Map.of());

        ResponseEntity<?> response = controller.getITAssetDetails("PPC-1");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void getAssetHistoryReturnsNotFoundForAnUnknownAssetTag() {
        when(assetRepo.existsById("PPC-404")).thenReturn(false);

        ResponseEntity<List<AssetHistoryService.AssetHistoryEntry>> response =
                controller.getAssetHistory("PPC-404");

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        verify(assetHistoryService, never()).getHistory(any());
    }

    @Test
    void getAssetHistoryReturnsTheHistoryWhenTheAssetExists() {
        when(assetRepo.existsById("PPC-1")).thenReturn(true);
        when(assetHistoryService.getHistory("PPC-1")).thenReturn(List.of());

        ResponseEntity<List<AssetHistoryService.AssetHistoryEntry>> response = controller.getAssetHistory("PPC-1");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void updateITAssetNormalizesBlanksAndDelegatesToTheService() {
        Asset updatedAsset = new Asset();
        updatedAsset.setAssetTag("PPC-1");
        updatedAsset.setCatalogID(1);
        updatedAsset.setSerialNumber("   ");
        updatedAsset.setCurrentOwnerID("");
        updatedAsset.setDeploymentStatus("In Storage");
        updatedAsset.setMaintenanceHealthStatus("Operational");
        updatedAsset.setLifecycleStatus("Procured / Pre-Deployment");

        ResponseEntity<String> response = controller.updateITAsset(updatedAsset, null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(updatedAsset.getSerialNumber()).isNull();
        assertThat(updatedAsset.getCurrentOwnerID()).isNull();
        verify(assetService).updateAsset(updatedAsset, "SYSTEM");
    }
}
