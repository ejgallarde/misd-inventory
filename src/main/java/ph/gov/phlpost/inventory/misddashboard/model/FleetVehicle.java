package ph.gov.phlpost.inventory.misddashboard.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "FleetVehicles")
public class FleetVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "VehicleID")
    private Integer vehicleID;

    @Column(name = "PlateNumber", unique = true, nullable = true)
    private String plateNumber;

    @Column(name = "BodyNumber", unique = true, nullable = true)
    private String bodyNumber;

    @Column(name = "VehicleType", nullable = true)
    private String vehicleType;

    @Column(name = "Make", nullable = true)
    private String make;

    @Column(name = "Model", nullable = true)
    private String model;

    @Column(name = "ManufactureYear")
    private Integer manufactureYear;

    @Column(name = "EngineNumber", unique = true)
    private String engineNumber;

    @Column(name = "ChassisNumberVIN", unique = true)
    private String chassisNumberVIN;

    @Column(name = "FuelType")
    private String fuelType;

    @Column(name = "RegistrationExpiry")
    private LocalDate registrationExpiry;

    @Column(name = "InsuranceExpiry")
    private LocalDate insuranceExpiry;

    @Column(name = "AssignedDriverID")
    private String assignedDriverID;

    @Column(name = "AdminLegaltionalStatus")
    private String adminLegaltionalStatus;

    @Column(name = "OperationalStatus")
    private String operationalStatus;

    @Column(name = "MaintenanceStatus")
    private String maintenanceStatus;

    @Column(name = "Cost")
    private String cost;

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

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getManufactureYear() {
        return manufactureYear;
    }

    public void setManufactureYear(Integer manufactureYear) {
        this.manufactureYear = manufactureYear;
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

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
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

    public String getCost() {
        return cost;
    }

    public void setCost(String cost) {
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