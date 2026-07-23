package ph.gov.phlpost.inventory.misddashboard.model;

import jakarta.persistence.*;

@Entity
@Table(name = "EquipmentCatalog")
public class EquipmentCatalog {

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

    // We use String here to capture the JSON text from the HTML form
    @Column(name = "Specifications", columnDefinition = "json")
    private String specifications;

    public EquipmentCatalog() {}

    // Getters and Setters are required for form binding
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