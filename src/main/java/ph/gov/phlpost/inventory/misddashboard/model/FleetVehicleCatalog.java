package ph.gov.phlpost.inventory.misddashboard.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "FleetVehicleCatalog")
public class FleetVehicleCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CatalogID")
    private Integer catalogID;

    @NotBlank
    @Size(max = 100)
    @Column(name = "Category", nullable = false)
    private String category;

    @NotBlank
    @Size(max = 100)
    @Column(name = "Manufacturer", nullable = false)
    private String manufacturer;

    @NotBlank
    @Size(max = 100)
    @Column(name = "ModelName", nullable = false)
    private String modelName;

    // Presence only here; FleetController.addFleetCatalog already enforces the
    // 1980..currentYear+1 range with a friendlier message than a static
    // annotation could produce (the upper bound moves every year).
    @NotNull
    @Column(name = "YearModel", nullable = false)
    private Integer yearModel;

    @NotBlank
    @Size(max = 50)
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
