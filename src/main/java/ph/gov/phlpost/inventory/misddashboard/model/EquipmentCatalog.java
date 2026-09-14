package ph.gov.phlpost.inventory.misddashboard.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "EquipmentCatalog")
public class EquipmentCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CatalogID")
    private Integer catalogID;

    @NotBlank
    @Size(max = 50)
    @Column(name = "Category", nullable = false)
    private String category;

    @NotBlank
    @Size(max = 50)
    @Column(name = "Manufacturer", nullable = false)
    private String manufacturer;

    @NotBlank
    @Size(max = 100)
    @Column(name = "ModelName", nullable = false)
    private String modelName;

    // We use String here to capture the JSON text from the HTML form
    @Column(name = "Specifications", columnDefinition = "json")
    private String specifications;

    public EquipmentCatalog() {}

    public Integer getCatalogID() { return catalogID; }
    public void setCatalogID(Integer catalogID) { this.catalogID = catalogID; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getSpecifications() { return specifications; }
    public void setSpecifications(String specifications) { this.specifications = specifications; }
}