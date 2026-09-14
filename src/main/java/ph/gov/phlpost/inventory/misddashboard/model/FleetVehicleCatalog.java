package ph.gov.phlpost.inventory.misddashboard.model;

import jakarta.persistence.*;

@Entity
@Table(name = "FleetVehicleCatalog")
public class FleetVehicleCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CatalogID")
    private Integer catalogID;

    @Column(name = "Category", nullable = false)
    private String category;

    @Column(name = "Manufacturer", nullable = false)
    private String manufacturer;

    @Column(name = "ModelName", nullable = false)
    private String modelName;

    @Column(name = "YearModel", nullable = false)
    private Integer yearModel;

    @Column(name = "FuelType", nullable = false)
    private String fuelType;

    public FleetVehicleCatalog() {}

    public Integer getCatalogID() { return catalogID; }
    public void setCatalogID(Integer catalogID) { this.catalogID = catalogID; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public Integer getYearModel() { return yearModel; }
    public void setYearModel(Integer yearModel) { this.yearModel = yearModel; }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }
}
