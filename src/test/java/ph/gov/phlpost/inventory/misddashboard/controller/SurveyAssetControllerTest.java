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

import ph.gov.phlpost.inventory.misddashboard.model.SurveyAsset;
import ph.gov.phlpost.inventory.misddashboard.repository.SurveyAssetRepository;
import ph.gov.phlpost.inventory.misddashboard.repository.SurveyEquipmentCatalogRepository;
import ph.gov.phlpost.inventory.misddashboard.service.AssetHistoryService;
import ph.gov.phlpost.inventory.misddashboard.service.DocumentService;
import ph.gov.phlpost.inventory.misddashboard.service.RegistryService;
import ph.gov.phlpost.inventory.misddashboard.service.SurveyAssetService;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class SurveyAssetControllerTest {

    @Mock
    private SurveyAssetRepository surveyAssetRepo;

    @Mock
    private SurveyEquipmentCatalogRepository surveyCatalogRepo;

    @Mock
    private SurveyAssetService surveyAssetService;

    @Mock
    private RegistryService registryService;

    @Mock
    private DocumentService documentService;

    @Mock
    private AssetHistoryService assetHistoryService;

    @Mock
    private JsonMapper jsonMapper;

    @InjectMocks
    private SurveyAssetController controller;

    @Test
    void returnsSurveyAssetHistoryUsingImmutableIdAuditKey() {
        SurveyAsset asset = new SurveyAsset();
        asset.setSurveyAssetID(17);
        asset.setAssetTag("SA-0017");
        var entry = new AssetHistoryService.AssetHistoryEntry(
                LocalDateTime.of(2026, 8, 1, 9, 30),
                "Lifecycle", "Survey Asset Returned", "EQUIPMENT ROOM", "Returned in good condition");

        when(surveyAssetRepo.findById(17)).thenReturn(Optional.of(asset));
        when(assetHistoryService.getHistory("SURVEYASSET-17")).thenReturn(List.of(entry));

        ResponseEntity<List<AssetHistoryService.AssetHistoryEntry>> response = controller.getSurveyAssetHistory(17);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(entry);
        verify(assetHistoryService).getHistory("SURVEYASSET-17");
    }

    @Test
    void returnsNotFoundWhenSurveyAssetDoesNotExist() {
        when(surveyAssetRepo.findById(404)).thenReturn(Optional.empty());

        ResponseEntity<List<AssetHistoryService.AssetHistoryEntry>> response = controller.getSurveyAssetHistory(404);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void tagLessSurveyAssetReportsHistoryLoggedAgainstItsId() {
        var asset = new SurveyAsset();
        asset.setSurveyAssetID(21);
        asset.setAssetTag("   ");
        var entry = new AssetHistoryService.AssetHistoryEntry(
                LocalDateTime.of(2026, 8, 14, 11, 0),
                "Lifecycle", "Marked Missing", "SYSTEM", "Not found after field trip");

        when(surveyAssetRepo.findById(21)).thenReturn(Optional.of(asset));
        when(assetHistoryService.getHistory("SURVEYASSET-21")).thenReturn(List.of(entry));

        ResponseEntity<List<AssetHistoryService.AssetHistoryEntry>> response = controller.getSurveyAssetHistory(21);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(entry);
    }
}
