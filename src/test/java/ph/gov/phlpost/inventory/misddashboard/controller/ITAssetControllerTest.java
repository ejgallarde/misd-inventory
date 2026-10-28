package ph.gov.phlpost.inventory.misddashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import tools.jackson.databind.json.JsonMapper;

import ph.gov.phlpost.inventory.misddashboard.model.Asset;
import ph.gov.phlpost.inventory.misddashboard.repository.AssetRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.EquipmentCatalogRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.PersonnelRepository;
import ph.gov.phlpost.inventory.misddashboard.service.AssetHistoryService;
import ph.gov.phlpost.inventory.misddashboard.service.DocumentService;
import ph.gov.phlpost.inventory.misddashboard.service.ITAssetService;
import ph.gov.phlpost.inventory.misddashboard.service.RegistryService;

@ExtendWith(MockitoExtension.class)
class ITAssetControllerTest {

    @Mock
    private AssetRepository assetRepo;

    @Mock
    private EquipmentCatalogRepository catalogRepo;

    @Mock
    private PersonnelRepository personnelRepo;

    @Mock
    private ITAssetService assetService;

    @Mock
    private RegistryService registryService;

    @Mock
    private DocumentService documentService;

    @Mock
    private AssetHistoryService assetHistoryService;

    private ITAssetController controller;

    private static final org.springframework.security.core.Authentication ADMIN_USER =
            new org.springframework.security.authentication.TestingAuthenticationToken("admin@phlpost.gov.ph", null,
                    "ROLE_ADMIN");

    @BeforeEach
    void setUp() {
        controller = new ITAssetController(assetRepo, catalogRepo, assetService, registryService, documentService,
                assetHistoryService, personnelRepo, JsonMapper.builder().build());
        // @Value-injected in the app; same values as application-dropdowns.properties.
        ReflectionTestUtils.setField(controller, "assetDeploymentStatusOptions", List.of("In Storage", "Deployed",
                "Missing / Unaccounted", "Unavailable", "With Service Center", "With MISD Technician"));
        ReflectionTestUtils.setField(controller, "assetMaintenanceHealthStatusOptions", List.of("Operational",
                "Degraded", "Under Repair", "Beyond Economic Repair (BER)", "Not Applicable"));
        ReflectionTestUtils.setField(controller, "assetLifecycleStatusOptions", List.of("Procured / Pre-Deployment",
                "Active", "Inactive", "End of Life (EOL)", "Decommissioned / Retired", "Disposed", "Sold"));
        lenient().when(assetRepo.existsById(anyString())).thenReturn(true);
    }

    @Test
    void updatingAnUnknownAssetIsNotFoundWithAJsonError() {
        when(assetRepo.existsById("NOPE")).thenReturn(false);

        ResponseEntity<?> response = controller.updateITAsset(asset("NOPE", "In Storage", "Operational", "Active"), ADMIN_USER);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(Map.of("error", "Asset not found."));
        verify(assetService, never()).updateAsset(any(Asset.class), anyString(), anyBoolean(), anyBoolean());
    }

    @Test
    void statusOutsideTheConfiguredDropdownIsRejected() {
        ResponseEntity<?> response = controller.updateITAsset(asset("TAG-6", "On the moon", "Operational", "Active"),
                ADMIN_USER);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(Map.of("error", "Unknown deployment status: On the moon"));
        verify(assetService, never()).updateAsset(any(Asset.class), anyString(), anyBoolean(), anyBoolean());
    }

    @Test
    void serviceRejectionBecomesABadRequestRatherThanA500() {
        doThrow(new IllegalArgumentException("Owner is not a known employee."))
                .when(assetService).updateAsset(any(Asset.class), anyString(), anyBoolean(), anyBoolean());

        ResponseEntity<?> response = controller.updateITAsset(asset("TAG-7", "In Storage", "Operational", "Active"),
                ADMIN_USER);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(Map.of("error", "Owner is not a known employee."));
    }

    @Test
    void blankOptionalFieldsFromTheJsonSlideoutArriveAsNull() {
        Asset submitted = asset("TAG-1", "In Storage", "Operational", "Active");
        submitted.setSerialNumber("  ");
        submitted.setCurrentOwnerID("");
        submitted.setRemarks("");

        controller.updateITAsset(submitted, ADMIN_USER);

        Asset saved = capturedUpdate();
        assertThat(saved.getSerialNumber()).isNull();
        assertThat(saved.getCurrentOwnerID()).isNull();
        assertThat(saved.getRemarks()).isNull();
    }

    @Test
    void berHealthForcesEndOfLife() {
        controller.updateITAsset(asset("TAG-2", "In Storage", "Beyond Economic Repair (BER)", "Active"), ADMIN_USER);

        assertThat(capturedUpdate().getLifecycleStatus()).isEqualTo("End of Life (EOL)");
    }

    @Test
    void endStateReleasesOwnerAndMovesADeployedAssetToStorage() {
        Asset submitted = asset("TAG-3", "Deployed", "Operational", "Disposed");
        submitted.setCurrentOwnerID("E-1");

        controller.updateITAsset(submitted, ADMIN_USER);

        Asset saved = capturedUpdate();
        assertThat(saved.getCurrentOwnerID()).isNull();
        assertThat(saved.getDeploymentStatus()).isEqualTo("In Storage");
    }

    @Test
    void deployingAPreDeploymentAssetPromotesItToActive() {
        Asset submitted = asset("TAG-4", "Deployed", "Operational", "Procured / Pre-Deployment");
        submitted.setCurrentOwnerID("E-1");

        controller.updateITAsset(submitted, ADMIN_USER);

        assertThat(capturedUpdate().getLifecycleStatus()).isEqualTo("Active");
    }

    @Test
    void adminUpdatesEverythingAndIsRecordedByName() {
        controller.updateITAsset(asset("TAG-5", "In Storage", "Operational", "Active"), ADMIN_USER);

        verify(assetService).updateAsset(any(Asset.class), eq("admin@phlpost.gov.ph"), eq(true), eq(true));
    }

    @Test
    void financeMayChangeOnlyTheCostAndSkipsStatusRules() {
        var finance = new org.springframework.security.authentication.TestingAuthenticationToken(
                "finance@phlpost.gov.ph", null, "ROLE_FINANCE");
        // Statuses are ignored for FINANCE, so even an unconfigured value is not rejected.
        Asset submitted = asset("TAG-8", "Anything", "Beyond Economic Repair (BER)", "Active");

        ResponseEntity<?> response = controller.updateITAsset(submitted, finance);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(submitted.getLifecycleStatus()).isEqualTo("Active"); // no BER -> EOL transition applied
        verify(assetService).updateAsset(submitted, "finance@phlpost.gov.ph", false, true);
    }

    @Test
    void clerkMayChangeEverythingButTheCost() {
        var clerk = new org.springframework.security.authentication.TestingAuthenticationToken(
                "clerk@phlpost.gov.ph", null, "ROLE_CLERK");

        controller.updateITAsset(asset("TAG-9", "In Storage", "Operational", "Active"), clerk);

        verify(assetService).updateAsset(any(Asset.class), eq("clerk@phlpost.gov.ph"), eq(true), eq(false));
    }

    @Test
    void validationFailureOnAssignIsFlashedAndKeepsTheSearch() {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Asset not found."))
                .when(assetService).assignAsset("NOPE", "E-1", null, "SYSTEM");
        var flash = new RedirectAttributesModelMap();

        String view = controller.assignAsset("NOPE", "E-1", null, "laptop & dock", 2, null, flash);

        assertThat(view).isEqualTo("redirect:/assets?search=laptop%20%26%20dock&page=2");
        assertThat(flash.getFlashAttributes().get("errorMessage")).isEqualTo("Asset not found.");
    }

    @Test
    void receiveRejectsAnOutOfRangeQuantityWithAFlashMessage() {
        when(assetService.receiveAssets(org.mockito.ArgumentMatchers.any(Asset.class), anyInt(), anyString()))
                .thenThrow(new IllegalArgumentException("Quantity must be at least 1."));
        var flash = new RedirectAttributesModelMap();

        String view = controller.receiveAsset(new Asset(), 0, null, null, null, flash);

        assertThat(view).isEqualTo("redirect:/");
        assertThat(flash.getFlashAttributes().get("errorMessage")).isEqualTo("Quantity must be at least 1.");
    }

    @Test
    void receiveReportsHowManyTagsWereCreated() {
        when(assetService.receiveAssets(org.mockito.ArgumentMatchers.any(Asset.class), eq(2), anyString()))
                .thenReturn(List.of("PPC-1", "PPC-2"));
        var flash = new RedirectAttributesModelMap();

        controller.receiveAsset(new Asset(), 2, null, null, null, flash);

        assertThat(flash.getFlashAttributes().get("successMessage"))
                .isEqualTo("Successfully received 2 asset(s) into storage.");
    }

    @Test
    void detailsAndHistoryForUnknownAssetAreNotFound() {
        when(assetRepo.findById("NOPE")).thenReturn(Optional.empty());
        when(assetRepo.existsById("NOPE")).thenReturn(false);

        assertThat(controller.getITAssetDetails("NOPE").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(controller.getAssetHistory("NOPE").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Asset capturedUpdate() {
        ArgumentCaptor<Asset> captor = ArgumentCaptor.forClass(Asset.class);
        verify(assetService).updateAsset(captor.capture(), anyString(), anyBoolean(), anyBoolean());
        return captor.getValue();
    }

    private static Asset asset(String tag, String deployment, String health, String lifecycle) {
        Asset asset = new Asset();
        asset.setAssetTag(tag);
        asset.setCatalogID(1);
        asset.setDeploymentStatus(deployment);
        asset.setMaintenanceHealthStatus(health);
        asset.setLifecycleStatus(lifecycle);
        return asset;
    }
}
