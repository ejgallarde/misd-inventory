package ph.gov.phlpost.inventory.misddashboard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "Assets")
public class Asset {

    /**
     * Deliberately unconstrained (no @NotBlank/@Size) even though the column is
     * a NOT NULL varchar(50): on /assets/receive this arrives blank in the
     * "base" template object and ITAssetService generates the real tag itself,
     * so a presence check here would reject every multi-unit receipt at bind
     * time before the service ever runs. /assets/update always has it set
     * because that endpoint identifies an existing row.
     */
    @Id
    @Column(name = "AssetTag")
    private String assetTag;

    @Size(max = 100)
    @Column(name = "PropertyNumber")
    private String propertyNumber;

    @Size(max = 50)
    @Column(name = "BundledWithAssetTag")
    private String bundledWithAssetTag;

    @NotNull
    @Column(name = "CatalogID", nullable = false)
    private Integer catalogID;

    @Size(max = 100)
    @Column(name = "SerialNumber", unique = true)
    private String serialNumber;

    @Column(name = "PurchaseDate")
    private LocalDate purchaseDate;

    @DecimalMin(value = "0.00", message = "Purchase price cannot be negative")
    @Column(name = "PurchasePrice")
    private BigDecimal purchasePrice;

    @Size(max = 20)
    @Column(name = "CurrentOwnerID")
    private String currentOwnerID;

    @NotBlank
    @Size(max = 255)
    @Column(name = "DeploymentStatus")
    private String deploymentStatus;

    @NotBlank
    @Size(max = 255)
    @Column(name = "MaintenanceHealthStatus")
    private String maintenanceHealthStatus;

    @NotBlank
    @Size(max = 255)
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

    public String getPropertyNumber() {
        return propertyNumber;
    }

    public void setPropertyNumber(String propertyNumber) {
        this.propertyNumber = propertyNumber;
    }

    public String getBundledWithAssetTag() {
        return bundledWithAssetTag;
    }

    public void setBundledWithAssetTag(String bundledWithAssetTag) {
        this.bundledWithAssetTag = bundledWithAssetTag;
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