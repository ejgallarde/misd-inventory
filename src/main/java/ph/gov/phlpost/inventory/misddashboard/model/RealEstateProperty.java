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

    @Column(name = "TitleNumber", unique = true)
    private String titleNumber;

    @Column(name = "FullAddress", nullable = false)
    private String fullAddress;

    @Column(name = "Region")
    private String region;

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

    @Column(name = "CurrentStatus")
    private String currentStatus = "Active";

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

    public String getTitleNumber() {
        return titleNumber;
    }

    public void setTitleNumber(String titleNumber) {
        this.titleNumber = titleNumber;
    }

    public String getFullAddress() {
        return fullAddress;
    }

    public void setFullAddress(String fullAddress) {
        this.fullAddress = fullAddress;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
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

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
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