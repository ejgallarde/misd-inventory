package ph.gov.phlpost.inventory.misddashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import ph.gov.phlpost.inventory.misddashboard.model.RealEstateProperty;
import ph.gov.phlpost.inventory.misddashboard.repository.RealEstatePropertyRepository;
import ph.gov.phlpost.inventory.misddashboard.service.AssetHistoryService;
import ph.gov.phlpost.inventory.misddashboard.service.AuditLogService;
import ph.gov.phlpost.inventory.misddashboard.service.DocumentService;
import ph.gov.phlpost.inventory.misddashboard.service.RegistryService;

@ExtendWith(MockitoExtension.class)
class PropertiesControllerTest {

    @Mock
    private RealEstatePropertyRepository propertyRepo;

    @Mock
    private RegistryService registryService;

    @Mock
    private AuditLogService auditService;

    @Mock
    private DocumentService documentService;

    @Mock
    private AssetHistoryService assetHistoryService;

    @InjectMocks
    private PropertiesController controller;

    @Test
    void landIntakeForcesLotTypeAndClearsFloorArea() {
        RealEstateProperty land = validLand();
        land.setPropertyType("Facility - Building");
        land.setFloorAreaSqm(new BigDecimal("120.00"));
        var flash = new RedirectAttributesModelMap();

        String view = controller.addProperty(land, false, "LAND", null, null, null, flash);

        assertThat(view).isEqualTo("redirect:/");
        assertThat(flash.getFlashAttributes().get("successMessage")).isEqualTo("Property added to registry.");
        ArgumentCaptor<RealEstateProperty> saved = ArgumentCaptor.forClass(RealEstateProperty.class);
        verify(propertyRepo).save(saved.capture());
        assertThat(saved.getValue().getPropertyType()).isEqualTo("Lot");
        assertThat(saved.getValue().getFloorAreaSqm()).isNull();
    }

    @Test
    void landIntakeRequiresTitleTaxDeclarationAndSurveyPlan() {
        assertRejected(withLand(p -> p.setTitleNumber(null)), "LAND",
                "Title Number / TCT is required for land assets.");
        assertRejected(withLand(p -> p.setTaxDeclarationNumber(" ")), "LAND",
                "Tax Declaration number is required for land assets.");
        assertRejected(withLand(p -> p.setSurveyPlanNumber(null)), "LAND",
                "Survey Plan number is required for land assets.");
        assertRejected(withLand(p -> p.setLegalTitlingStatus(null)), "LAND",
                "Legal & Titling Status is required for land assets.");
    }

    @Test
    void commonFieldsAreValidatedBeforeAnythingIsSaved() {
        assertRejected(withLand(p -> p.setPropertyName("")), "LAND", "Property name is required.");
        assertRejected(withLand(p -> p.setAddressLine1(null)), "LAND", "Address Line 1 is required.");
        assertRejected(withLand(p -> p.setBarangay(null)), "LAND", "Barangay is required.");
        assertRejected(withLand(p -> p.setArea(null)), "LAND", "Area is required.");
        assertRejected(withLand(p -> p.setConditionStatus(null)), "LAND", "Condition Status is required.");
    }

    @Test
    void buildingIntakeRejectsLotsAndRequiresTaxDeclaration() {
        RealEstateProperty lot = validLand();
        assertRejected(lot, "BUILDING_FACILITY", "Buildings & Facilities intake does not accept lot properties.");

        RealEstateProperty building = validLand();
        building.setPropertyType("Facility - Building");
        building.setTaxDeclarationNumber(null);
        assertRejected(building, "BUILDING_FACILITY", "Tax Declaration number is required for non-lot properties.");
    }

    @Test
    void buildingWithoutTitleIsAcceptedAndStoredWithNullTitle() {
        RealEstateProperty building = validLand();
        building.setPropertyType("Facility - Warehouse");
        building.setTitleNumber("");
        var flash = new RedirectAttributesModelMap();

        controller.addProperty(building, true, "BUILDING_FACILITY", null, null, null, flash);

        ArgumentCaptor<RealEstateProperty> saved = ArgumentCaptor.forClass(RealEstateProperty.class);
        verify(propertyRepo).save(saved.capture());
        // A blank title must reach MySQL as NULL: TitleNumber is UNIQUE.
        assertThat(saved.getValue().getTitleNumber()).isNull();
    }

    @Test
    void updateRecordsStatusChangeAgainstThePrimaryKeyReference() {
        RealEstateProperty existing = validLand();
        existing.setPropertyID(9);
        existing.setPropertyTaxStatus("Pending");
        when(propertyRepo.findById(9)).thenReturn(Optional.of(existing));

        RealEstateProperty submitted = validLand();
        submitted.setPropertyID(9);
        submitted.setPropertyTaxStatus("Paid (Current Year)");

        ResponseEntity<String> response = controller.updateProperty(submitted, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(existing.getPropertyTaxStatus()).isEqualTo("Paid (Current Year)");
        verify(auditService).logLifecycleEvent(eq("PROP-9"), eq("SYSTEM"), eq("Property Status Updated"),
                org.mockito.ArgumentMatchers.contains("Tax: Pending"));
    }

    @Test
    void updateWithoutStatusChangeIsLoggedAsDetailsUpdate() {
        RealEstateProperty existing = validLand();
        existing.setPropertyID(4);
        when(propertyRepo.findById(4)).thenReturn(Optional.of(existing));

        RealEstateProperty submitted = validLand();
        submitted.setPropertyID(4);
        submitted.setZipCode("1001");

        controller.updateProperty(submitted, null);

        verify(auditService).logLifecycleEvent(eq("PROP-4"), anyString(), eq("Property Details Updated"), anyString());
    }

    @Test
    void updateDoesNotTouchIdentifyingFields() {
        RealEstateProperty existing = validLand();
        existing.setPropertyID(5);
        when(propertyRepo.findById(5)).thenReturn(Optional.of(existing));

        RealEstateProperty submitted = validLand();
        submitted.setPropertyID(5);
        submitted.setTitleNumber("TCT-CHANGED");
        submitted.setPropertyName("Renamed");

        controller.updateProperty(submitted, null);

        assertThat(existing.getTitleNumber()).isEqualTo("TCT-1");
        assertThat(existing.getPropertyName()).isEqualTo("Central Lot");
    }

    @Test
    void updateWithoutIdIsBadRequestAndUnknownIdIsNotFound() {
        assertThat(controller.updateProperty(new RealEstateProperty(), null).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        RealEstateProperty unknown = new RealEstateProperty();
        unknown.setPropertyID(404);
        when(propertyRepo.findById(404)).thenReturn(Optional.empty());
        assertThat(controller.updateProperty(unknown, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void assignCustodianOnUnknownPropertyFlashesAnError() {
        when(propertyRepo.findById(77)).thenReturn(Optional.empty());
        var flash = new RedirectAttributesModelMap();

        String view = controller.assignCustodian(77, "E-1", null, flash);

        assertThat(view).isEqualTo("redirect:/properties");
        assertThat(flash.getFlashAttributes().get("errorMessage")).isEqualTo("Property not found.");
        verify(auditService, never()).logAssignment(anyString(), anyString(), anyString(), any());
    }

    @Test
    void assignCustodianLogsAgainstThePrimaryKeyReference() {
        RealEstateProperty property = validLand();
        property.setPropertyID(12);
        when(propertyRepo.findById(12)).thenReturn(Optional.of(property));

        controller.assignCustodian(12, "E-7", "Handover", new RedirectAttributesModelMap());

        assertThat(property.getCustodianID()).isEqualTo("E-7");
        verify(auditService).logAssignment("PROP-12", "E-7", "Custodian Assigned", "Handover");
    }

    @Test
    void detailsReturnEmptyStringsInsteadOfNullsAndResolveCustodianName() {
        RealEstateProperty property = validLand();
        property.setPropertyID(3);
        property.setCustodianID("E-1");
        property.setAddressLine2(null);
        when(propertyRepo.findById(3)).thenReturn(Optional.of(property));
        when(registryService.resolveDisplayName("E-1")).thenReturn("Santos, Juan");

        ResponseEntity<Map<String, Object>> response = controller.getPropertyDetails(3);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("addressLine2", "")
                .containsEntry("custodianName", "Santos, Juan")
                .containsEntry("titleNumber", "TCT-1");
    }

    @Test
    void taxUpdateIsAttributedToTheSignedInUser() {
        RealEstateProperty property = validLand();
        property.setPropertyID(8);
        when(propertyRepo.findById(8)).thenReturn(Optional.of(property));
        var user = new org.springframework.security.authentication.TestingAuthenticationToken("clerk@phlpost.gov.ph",
                null);

        controller.updatePropertyTax(8, "Overdue", "OR pending", user, new RedirectAttributesModelMap());

        assertThat(property.getPropertyTaxStatus()).isEqualTo("Overdue");
        verify(auditService).logLifecycleEvent("PROP-8", "clerk@phlpost.gov.ph", "Tax Status Updated: Overdue",
                "OR pending");
    }

    @Test
    void detailsForUnknownPropertyAreNotFound() {
        when(propertyRepo.findById(404)).thenReturn(Optional.empty());

        assertThat(controller.getPropertyDetails(404).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private void assertRejected(RealEstateProperty property, String context, String expectedMessage) {
        var flash = new RedirectAttributesModelMap();

        String view = controller.addProperty(property, false, context, null, null, null, flash);

        assertThat(view).isEqualTo("redirect:/");
        assertThat(flash.getFlashAttributes().get("errorMessage")).isEqualTo(expectedMessage);
        verify(propertyRepo, never()).save(property);
    }

    private static RealEstateProperty withLand(java.util.function.Consumer<RealEstateProperty> change) {
        RealEstateProperty property = validLand();
        change.accept(property);
        return property;
    }

    private static RealEstateProperty validLand() {
        RealEstateProperty property = new RealEstateProperty();
        property.setPropertyName("Central Lot");
        property.setPropertyType("Lot");
        property.setArea("Central Office");
        property.setAddressLine1("Liwasang Bonifacio");
        property.setProvince("Metro Manila");
        property.setCity("Manila");
        property.setBarangay("Barangay 659");
        property.setOperationalStatus("Active/In Use");
        property.setConditionStatus("Usable - No Known Issues");
        property.setLegalTitlingStatus("Titled");
        property.setTitleNumber("TCT-1");
        property.setTaxDeclarationNumber("TD-1");
        property.setSurveyPlanNumber("PSD-1");
        return property;
    }
}
