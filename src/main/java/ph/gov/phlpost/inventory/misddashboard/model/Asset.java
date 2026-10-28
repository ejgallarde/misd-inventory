package ph.gov.phlpost.inventory.misddashboard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "Assets")
public class Asset {

    @Id
    @Column(name = "AssetTag")
    private String assetTag;

    @Column(name = "CatalogID", nullable = false)
    private Integer catalogID;

    @Column(name = "SerialNumber", unique = true)
    private String serialNumber;

    @Column(name = "PurchaseDate")
    private LocalDate purchaseDate;

    @Column(name = "PurchasePrice")
    private BigDecimal purchasePrice;

    /** The accountable owner: must hold an eligible employment status (see AccountabilityService). */
    @Column(name = "CurrentOwnerID")
    private String currentOwnerID;

    /**
     * Who actually uses the asset. May be non-regular (e.g. Job Order); the
     * accountable owner is then usually their manager.
     */
    @Column(name = "AssignedUserID")
    private String assignedUserID;

    @Column(name = "DeploymentStatus")
    private String deploymentStatus;

    @Column(name = "MaintenanceHealthStatus")
    private String maintenanceHealthStatus;

    @Column(name = "LifecycleStatus")
    private String lifecycleStatus;

    @Column(name = "Remarks", columnDefinition = "TEXT")
    private String remarks;

    public Asset() {
    }

    public String getAssetTag() {
        return assetTag;
    }

    public void setAssetTag(String assetTag) {
        this.assetTag = assetTag;
    }

    public Integer getCatalogID() {
        return catalogID;
    }

    public void setCatalogID(Integer catalogID) {
        this.catalogID = catalogID;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(BigDecimal purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public String getCurrentOwnerID() {
        return currentOwnerID;
    }

    public void setCurrentOwnerID(String currentOwnerID) {
        this.currentOwnerID = currentOwnerID;
    }

    public String getAssignedUserID() {
        return assignedUserID;
    }

    public void setAssignedUserID(String assignedUserID) {
        this.assignedUserID = assignedUserID;
    }

    public String getDeploymentStatus() {
        return deploymentStatus;
    }

    public void setDeploymentStatus(String deploymentStatus) {
        this.deploymentStatus = deploymentStatus;
    }

    public String getMaintenanceHealthStatus() {
        return maintenanceHealthStatus;
    }

    public void setMaintenanceHealthStatus(String maintenanceHealthStatus) {
        this.maintenanceHealthStatus = maintenanceHealthStatus;
    }

    public String getLifecycleStatus() {
        return lifecycleStatus;
    }

    public void setLifecycleStatus(String lifecycleStatus) {
        this.lifecycleStatus = lifecycleStatus;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}