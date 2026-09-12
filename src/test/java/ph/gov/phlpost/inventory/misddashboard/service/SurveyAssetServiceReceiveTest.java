package ph.gov.phlpost.inventory.misddashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ph.gov.phlpost.inventory.misddashboard.model.SurveyAsset;
import ph.gov.phlpost.inventory.misddashboard.repository.SurveyAssetRepository;

@ExtendWith(MockitoExtension.class)
class SurveyAssetServiceReceiveTest {

    @Mock
    private SurveyAssetRepository surveyAssetRepository;

    @Mock
    private AuditLogService auditLogService;

    private SurveyAssetService service;

    @BeforeEach
    void setUp() {
        service = new SurveyAssetService(surveyAssetRepository, auditLogService, 100);
    }

    @Test
    void receiveSurveyAssetsRejectsAQuantityBelowOne() {
        // The form's min="1" is client-side only.
        assertThatThrownBy(() -> service.receiveSurveyAssets(new SurveyAsset(), 0, "tester"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 1");
    }

    @Test
    void receiveSurveyAssetsRejectsAQuantityAboveTheConfiguredCeiling() {
        assertThatThrownBy(() -> service.receiveSurveyAssets(new SurveyAsset(), 101, "tester"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum of 100");
    }

    @Test
    void receivingASingleUnitKeepsItsSerialNumberAndGeneratesATag() {
        SurveyAsset submitted = new SurveyAsset();
        submitted.setCatalogID(7);
        submitted.setSerialNumber("SN-12345");
        org.mockito.Mockito.when(surveyAssetRepository.findTopByAssetTagStartingWithOrderByAssetTagDesc(
                org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());

        List<SurveyAsset> created = service.receiveSurveyAssets(submitted, 1, "tester");

        assertThat(created).hasSize(1);
        SurveyAsset saved = created.get(0);
        assertThat(saved.getAssetTag()).startsWith("SVY-").endsWith("00001");
        assertThat(saved.getSerialNumber()).isEqualTo("SN-12345");
        assertThat(saved.getCatalogID()).isEqualTo(7);
        assertThat(saved.getAdminLegalStatus()).isEqualTo("Registered/Accountable");
        assertThat(saved.getOperationalStatus()).isEqualTo("Available/Idle");
        assertThat(saved.getConditionStatus()).isEqualTo("Operational");
        assertThat(saved.getAssignedCustodianID()).isNull();
        org.mockito.Mockito.verify(surveyAssetRepository).saveAndFlush(saved);
    }

    @Test
    void receivingMultipleUnitsLeavesSerialNumbersUnsetSoTheyDoNotCollide() {
        // SerialNumber is UNIQUE; one submitted value cannot belong to three units.
        SurveyAsset submitted = new SurveyAsset();
        submitted.setCatalogID(7);
        submitted.setSerialNumber("SN-12345");
        org.mockito.Mockito.when(surveyAssetRepository.findTopByAssetTagStartingWithOrderByAssetTagDesc(
                org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());

        List<SurveyAsset> created = service.receiveSurveyAssets(submitted, 3, "tester");

        assertThat(created).hasSize(3);
        assertThat(created).allSatisfy(asset -> assertThat(asset.getSerialNumber()).isNull());
        org.mockito.Mockito.verify(surveyAssetRepository, org.mockito.Mockito.times(3))
                .saveAndFlush(org.mockito.ArgumentMatchers.any(SurveyAsset.class));
    }
}
