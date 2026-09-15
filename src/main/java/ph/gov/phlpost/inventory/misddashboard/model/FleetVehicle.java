package ph.gov.phlpost.inventory.misddashboard.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "FleetVehicles")
public class FleetVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VehicleID")
    private Integer vehicleID;

    @Size(max = 100)
    @Column(name = "PropertyNumber")
    private String propertyNumber;

    @NotNull
    @Column(name = "CatalogID", nullable = false)
    private Integer catalogID;

    @Size(max = 255)
    @Column(name = "PlateNumber", unique = true, nullable = true)
    private String plateNumber;

    @Size(max = 255)
    @Column(name = "BodyNumber", unique = true, nullable = true)
    private String bodyNumber;

    @Size(max = 255)
    @Column(name = "EngineNumber", unique = true)
    private String engineNumber;

    @Size(max = 255)
    @Column(name = "ChassisNumberVIN", unique = true)
    private String chassisNumberVIN;

    @Column(name = "RegistrationExpiry")
    private LocalDate registrationExpiry;

    @Column(name = "InsuranceExpiry")
    private LocalDate insuranceExpiry;

    @Size(max = 20)
    @Column(name = "AssignedDriverID")
    private String assignedDriverID;

    /** The person actually using the vehicle day-to-day, distinct from the accountable driver above. */
    @Size(max = 20)
    @Column(name = "EndUserID")
    private String endUserID;

    @Column(name = "CurrentValue")
    private BigDecimal currentValue;

    @Column(name = "DepreciationAmount")
    private BigDecimal depreciationAmount;

    @Column(name = "ValuationAsOfDate")
    private LocalDate valuationAsOfDate;

    // No @NotBlank: the column allows NULL and /fleet/add leaves this blank on
    // the incoming form, defaulting it server-side after binding completes.
    @Size(max = 255)
    @Column(name = "AdminLegaltionalStatus")
    private String adminLegaltionalStatus;

    @Size(max = 255)
    @Column(name = "OperationalStatus")
    private String operationalStatus;

    @Size(max = 255)
    @Column(name = "MaintenanceStatus")
    private String maintenanceStatus;

    @DecimalMin(value = "0.00", message = "Cost cannot be negative")
    @Column(name = "Cost")
    private BigDecimal cost;

    @Column(name = "AcquisitionYear")
    private Integer acquisitionYear;

    @Column(name = "Remarks", columnDefinition = "TEXT")
    private String remarks;

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Integer getVehicleID() {
        return vehicleID;
    }

    public void setVehicleID(Integer vehicleID) {
        this.vehicleID = vehicleID;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public String getPropertyNumber() {
        return propertyNumber;
    }

    public void setPropertyNumber(String propertyNumber) {
        this.propertyNumber = propertyNumber;
    }

    public Integer getCatalogID() {
        return catalogID;
    }

    public void setCatalogID(Integer catalogID) {
        this.catalogID = catalogID;
    }

    public String getEngineNumber() {
        return engineNumber;
    }

    public void setEngineNumber(String engineNumber) {
        this.engineNumber = engineNumber;
    }

    public String getChassisNumberVIN() {
        return chassisNumberVIN;
    }

    public void setChassisNumberVIN(String chassisNumberVIN) {
        this.chassisNumberVIN = chassisNumberVIN;
    }

    public LocalDate getRegistrationExpiry() {
        return registrationExpiry;
    }

    public void setRegistrationExpiry(LocalDate registrationExpiry) {
        this.registrationExpiry = registrationExpiry;
    }

    public LocalDate getInsuranceExpiry() {
        return insuranceExpiry;
    }

    public void setInsuranceExpiry(LocalDate insuranceExpiry) {
        this.insuranceExpiry = insuranceExpiry;
    }

    public String getAssignedDriverID() {
        return assignedDriverID;
    }

    public void setAssignedDriverID(String assignedDriverID) {
        this.assignedDriverID = assignedDriverID;
    }

    public String getEndUserID() {
        return endUserID;
    }

    public void setEndUserID(String endUserID) {
        this.endUserID = endUserID;
    }

    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(BigDecimal currentValue) {
        this.currentValue = currentValue;
    }

    public BigDecimal getDepreciationAmount() {
        return depreciationAmount;
    }

    public void setDepreciationAmount(BigDecimal depreciationAmount) {
        this.depreciationAmount = depreciationAmount;
    }

    public LocalDate getValuationAsOfDate() {
        return valuationAsOfDate;
    }

    public void setValuationAsOfDate(LocalDate valuationAsOfDate) {
        this.valuationAsOfDate = valuationAsOfDate;
    }

    public String getAdminLegaltionalStatus() {
        return adminLegaltionalStatus;
    }

    public void setAdminLegaltionalStatus(String adminLegaltionalStatus) {
        this.adminLegaltionalStatus = adminLegaltionalStatus;
    }

    public String getOperationalStatus() {
        return operationalStatus;
    }

    public void setOperationalStatus(String operationalStatus) {
        this.operationalStatus = operationalStatus;
    }

    public String getMaintenanceStatus() {
        return maintenanceStatus;
    }

    public void setMaintenanceStatus(String maintenanceStatus) {
        this.maintenanceStatus = maintenanceStatus;
    }

    public String getBodyNumber() {
        return bodyNumber;
    }

    public void setBodyNumber(String bodyNumber) {
        this.bodyNumber = bodyNumber;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public Integer getAcquisitionYear() {
        return acquisitionYear;
    }

    public void setAcquisitionYear(Integer acquisitionYear) {
        this.acquisitionYear = acquisitionYear;
    }

    public FleetVehicle() {
    }

}