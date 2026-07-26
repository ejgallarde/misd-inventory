package ph.gov.phlpost.inventory.misddashboard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "vw_agingequipment") // Kept for native query result mapping compatibility.
public class AgingEquipmentReport {

    // JPA requires a primary key, so we use the unique AssetTag
    @Id
    @Column(name = "AssetTag")
    private String assetTag;

    @Column(name = "Category")
    private String category;

    @Column(name = "ModelName")
    private String modelName;

    @Column(name = "AccountableOwner")
    private String accountableOwner;

    @Column(name = "PurchaseDate")
    private LocalDate purchaseDate;

    @Column(name = "AgeInYears")
    private int ageInYears;

    // --- Empty Constructor required by JPA ---
    public AgingEquipmentReport() {
    }

    // --- Getters ---
    public String getAssetTag() {
        return assetTag;
    }

    public String getCategory() {
        return category;
    }

    public String getModelName() {
        return modelName;
    }

    public String getAccountableOwner() {
        return accountableOwner;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public int getAgeInYears() {
        return ageInYears;
    }
}