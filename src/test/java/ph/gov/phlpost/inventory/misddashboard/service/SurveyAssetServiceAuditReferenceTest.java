package ph.gov.phlpost.inventory.misddashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import ph.gov.phlpost.inventory.misddashboard.model.SurveyAsset;

/**
 * Audit rows are keyed on the immutable SurveyAssetID, not the asset tag or
 * serial number: both are nullable optional-unique columns, following the same
 * convention as {@link FleetService#auditReferenceId} (see
 * FleetServiceAuditReferenceTest).
 */
class SurveyAssetServiceAuditReferenceTest {

    @Test
    void referenceIsTheSurveyAssetIdEvenWhenATagIsPresent() {
        assertEquals("SURVEYASSET-105", SurveyAssetService.auditReferenceId(surveyAsset(105, "SA-0001")));
    }

    @Test
    void nullAssetTagStillYieldsAStableReference() {
        assertEquals("SURVEYASSET-107", SurveyAssetService.auditReferenceId(surveyAsset(107, null)));
    }

    @Test
    void blankAssetTagStillYieldsAStableReference() {
        assertEquals("SURVEYASSET-108", SurveyAssetService.auditReferenceId(surveyAsset(108, "   ")));
    }

    @Test
    void retaggingASurveyAssetDoesNotMoveItsHistory() {
        SurveyAsset before = surveyAsset(106, "SA-0006");
        SurveyAsset after = surveyAsset(106, "SA-0006-B");
        assertEquals(SurveyAssetService.auditReferenceId(before), SurveyAssetService.auditReferenceId(after));
    }

    private SurveyAsset surveyAsset(Integer surveyAssetId, String assetTag) {
        SurveyAsset asset = new SurveyAsset();
        asset.setSurveyAssetID(surveyAssetId);
        asset.setAssetTag(assetTag);
        return asset;
    }
}
