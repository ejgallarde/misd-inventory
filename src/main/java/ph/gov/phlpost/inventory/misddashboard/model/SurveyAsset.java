package ph.gov.phlpost.inventory.misddashboard.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "SurveyAssets")
public class SurveyAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SurveyAssetID")
    private Integer surveyAssetID;

    @Column(name = "PropertyNumber")
    private String propertyNumber;

    @Column(name = "CatalogID", nullable = false)
    private Integer catalogID;

    @Column(name = "AssetTag", unique = true, nullable = true)
    private String assetTag;

    @Column(name = "SerialNumber", unique = true, nullable = true)
    private String serialNumber;

    @Column(name = "AcquisitionDate")
    private LocalDate acquisitionDate;

    @Column(name = "Cost")
    private BigDecimal cost;

    @Column(name = "CalibrationDueDate")
    private LocalDate calibrationDueDate;

    @Column(name = "LastCalibrationDate")
    private LocalDate lastCalibrationDate;

    @Column(name = "AssignedCustodianID")
    private String assignedCustodianID;

    @Column(name = "AdminLegalStatus")
    private String adminLegalStatus;

    @Column(name = "OperationalStatus")
    private String operationalStatus;

    @Column(name = "ConditionStatus")
    private String conditionStatus;

    @Column(name = "Remarks", columnDefinition = "TEXT")
    private String remarks;

    public SurveyAsset() {
    }

    public Integer getSurveyAssetID() {
        return surveyAssetID;
    }

    public void setSurveyAssetID(Integer surveyAssetID) {
        this.surveyAssetID = surveyAssetID;
    }

    public Integer getCatalogID() {
        return catalogID;
    }

    public void setCatalogID(Integer catalogID) {
        this.catalogID = catalogID;
    }

    public String getAssetTag() {
        return assetTag;
    }

    public void setAssetTag(String assetTag) {
        this.assetTag = assetTag;
    }

    public String getPropertyNumber() {
        return propertyNumber;
    }

    public void setPropertyNumber(String propertyNumber) {
        this.propertyNumber = propertyNumber;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public LocalDate getAcquisitionDate() {
        return acquisitionDate;
    }

    public void setAcquisitionDate(LocalDate acquisitionDate) {
        this.acquisitionDate = acquisitionDate;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public LocalDate getCalibrationDueDate() {
        return calibrationDueDate;
    }

    public void setCalibrationDueDate(LocalDate calibrationDueDate) {
        this.calibrationDueDate = calibrationDueDate;
    }

    public LocalDate getLastCalibrationDate() {
        return lastCalibrationDate;
    }

    public void setLastCalibrationDate(LocalDate lastCalibrationDate) {
        this.lastCalibrationDate = lastCalibrationDate;
    }

    public String getAssignedCustodianID() {
        return assignedCustodianID;
    }

    public void setAssignedCustodianID(String assignedCustodianID) {
        this.assignedCustodianID = assignedCustodianID;
    }

    public String getAdminLegalStatus() {
        return adminLegalStatus;
    }

    public void setAdminLegalStatus(String adminLegalStatus) {
        this.adminLegalStatus = adminLegalStatus;
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

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
