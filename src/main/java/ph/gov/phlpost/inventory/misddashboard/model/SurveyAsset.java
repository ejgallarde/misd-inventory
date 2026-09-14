package ph.gov.phlpost.inventory.misddashboard.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "SurveyAssets")
public class SurveyAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SurveyAssetID")
    private Integer surveyAssetID;

    @Size(max = 100)
    @Column(name = "PropertyNumber")
    private String propertyNumber;

    @NotNull
    @Column(name = "CatalogID", nullable = false)
    private Integer catalogID;

    // No @NotBlank: /survey-assets/receive leaves this blank on the incoming
    // "base" template for multi-unit receipts and SurveyAssetService generates
    // or assigns it afterward, same pattern as Asset.assetTag.
    @Size(max = 50)
    @Column(name = "AssetTag", unique = true, nullable = true)
    private String assetTag;

    @Size(max = 255)
    @Column(name = "SerialNumber", unique = true, nullable = true)
    private String serialNumber;

    @Column(name = "AcquisitionDate")
    private LocalDate acquisitionDate;

    @DecimalMin(value = "0.00", message = "Cost cannot be negative")
    @Column(name = "Cost")
    private BigDecimal cost;

    @Column(name = "CalibrationDueDate")
    private LocalDate calibrationDueDate;

    @Column(name = "LastCalibrationDate")
    private LocalDate lastCalibrationDate;

    @Size(max = 20)
    @Column(name = "AssignedCustodianID")
    private String assignedCustodianID;

    @Size(max = 255)
    @Column(name = "AdminLegalStatus")
    private String adminLegalStatus;

    @Size(max = 255)
    @Column(name = "OperationalStatus")
    private String operationalStatus;

    @Size(max = 255)
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
