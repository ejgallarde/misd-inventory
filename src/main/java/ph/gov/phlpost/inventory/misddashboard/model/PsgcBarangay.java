package ph.gov.phlpost.inventory.misddashboard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "PsgcBarangays")
public class PsgcBarangay {

    @Id
    @Column(name = "BarangayCode", nullable = false, length = 20)
    private String barangayCode;

    @Column(name = "CityMunicipalityCode", nullable = false, length = 20)
    private String cityMunicipalityCode;

    @Column(name = "BarangayName", nullable = false, length = 150)
    private String barangayName;

    @Column(name = "ZipCode", length = 10)
    private String zipCode;

    public String getBarangayCode() {
        return barangayCode;
    }

    public void setBarangayCode(String barangayCode) {
        this.barangayCode = barangayCode;
    }

    public String getCityMunicipalityCode() {
        return cityMunicipalityCode;
    }

    public void setCityMunicipalityCode(String cityMunicipalityCode) {
        this.cityMunicipalityCode = cityMunicipalityCode;
    }

    public String getBarangayName() {
        return barangayName;
    }

    public void setBarangayName(String barangayName) {
        this.barangayName = barangayName;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }
}
