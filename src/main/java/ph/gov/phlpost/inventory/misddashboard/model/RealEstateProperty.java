package ph.gov.phlpost.inventory.misddashboard.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "RealEstateProperties")
public class RealEstateProperty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PropertyID")
    private Integer propertyID;

    @Column(name = "PropertyType", nullable = false)
    private String propertyType;

    @Column(name = "PropertyName", nullable = false)
    private String propertyName;

    @Column(name = "Area")
    private String area;

    @Column(name = "TitleNumber", unique = true)
    private String titleNumber;

    @Column(name = "TaxDeclarationNumber", unique = true)
    private String taxDeclarationNumber;

    @Column(name = "SurveyPlanNumber")
    private String surveyPlanNumber;

    @Column(name = "PropertyDetails", columnDefinition = "TEXT")
    private String propertyDetails;

    @Column(name = "AddressLine1")
    private String addressLine1;

    @Column(name = "AddressLine2")
    private String addressLine2;

    @Column(name = "Province")
    private String province;

    @Column(name = "City")
    private String city;

    @Column(name = "Barangay")
    private String barangay;

    @Column(name = "ZipCode")
    private String zipCode;

    @Column(name = "LotAreaSqm")
    private BigDecimal lotAreaSqm;

    @Column(name = "FloorAreaSqm")
    private BigDecimal floorAreaSqm;

    @Column(name = "AcquisitionDate")
    private LocalDate acquisitionDate;

    @Column(name = "AssessedValue")
    private BigDecimal assessedValue;

    @Column(name = "PropertyTaxStatus")
    private String propertyTaxStatus;

    @Column(name = "LegalTitlingStatus")
    private String legalTitlingStatus;

    @Column(name = "OperationalStatus")
    private String operationalStatus;

    @Column(name = "ConditionStatus")
    private String conditionStatus;

    @Column(name = "CustodianID")
    private String custodianID;

    @Column(name = "Remarks", columnDefinition = "TEXT")
    private String remarks;

    public Integer getPropertyID() {
        return propertyID;
    }

    public void setPropertyID(Integer propertyID) {
        this.propertyID = propertyID;
    }

    public String getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(String propertyType) {
        this.propertyType = propertyType;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getTitleNumber() {
        return titleNumber;
    }

    public void setTitleNumber(String titleNumber) {
        this.titleNumber = titleNumber;
    }

    public String getTaxDeclarationNumber() {
        return taxDeclarationNumber;
    }

    public void setTaxDeclarationNumber(String taxDeclarationNumber) {
        this.taxDeclarationNumber = taxDeclarationNumber;
    }

    public String getSurveyPlanNumber() {
        return surveyPlanNumber;
    }

    public void setSurveyPlanNumber(String surveyPlanNumber) {
        this.surveyPlanNumber = surveyPlanNumber;
    }

    public String getPropertyDetails() {
        return propertyDetails;
    }

    public void setPropertyDetails(String propertyDetails) {
        this.propertyDetails = propertyDetails;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getBarangay() {
        return barangay;
    }

    public void setBarangay(String barangay) {
        this.barangay = barangay;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public BigDecimal getLotAreaSqm() {
        return lotAreaSqm;
    }

    public void setLotAreaSqm(BigDecimal lotAreaSqm) {
        this.lotAreaSqm = lotAreaSqm;
    }

    public BigDecimal getFloorAreaSqm() {
        return floorAreaSqm;
    }

    public void setFloorAreaSqm(BigDecimal floorAreaSqm) {
        this.floorAreaSqm = floorAreaSqm;
    }

    public LocalDate getAcquisitionDate() {
        return acquisitionDate;
    }

    public void setAcquisitionDate(LocalDate acquisitionDate) {
        this.acquisitionDate = acquisitionDate;
    }

    public BigDecimal getAssessedValue() {
        return assessedValue;
    }

    public void setAssessedValue(BigDecimal assessedValue) {
        this.assessedValue = assessedValue;
    }

    public String getPropertyTaxStatus() {
        return propertyTaxStatus;
    }

    public void setPropertyTaxStatus(String propertyTaxStatus) {
        this.propertyTaxStatus = propertyTaxStatus;
    }

    public String getLegalTitlingStatus() {
        return legalTitlingStatus;
    }

    public void setLegalTitlingStatus(String legalTitlingStatus) {
        this.legalTitlingStatus = legalTitlingStatus;
    }

    public String getOperationalStatus() {
        return operationalStatus;
    }

    public void setOperationalStatus(String operationalStatus) {
        this.operationalStatus = operationalStatus;
    }

    public String getConditionStatus() {
        return conditionStatus;
    }

    public void setConditionStatus(String conditionStatus) {
        this.conditionStatus = conditionStatus;
    }

    public String getCustodianID() {
        return custodianID;
    }

    public void setCustodianID(String custodianID) {
        this.custodianID = custodianID;
    }

    public RealEstateProperty() {
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

}