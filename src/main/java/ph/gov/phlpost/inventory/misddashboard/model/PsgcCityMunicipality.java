package ph.gov.phlpost.inventory.misddashboard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "PsgcCitiesMunicipalities")
public class PsgcCityMunicipality {

    @Id
    @Column(name = "CityMunicipalityCode", nullable = false, length = 20)
    private String cityMunicipalityCode;

    @Column(name = "ProvinceCode", nullable = false, length = 20)
    private String provinceCode;

    @Column(name = "CityMunicipalityName", nullable = false, length = 150)
    private String cityMunicipalityName;

    public String getCityMunicipalityCode() {
        return cityMunicipalityCode;
    }

    public void setCityMunicipalityCode(String cityMunicipalityCode) {
        this.cityMunicipalityCode = cityMunicipalityCode;
    }

    public String getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    public String getCityMunicipalityName() {
        return cityMunicipalityName;
    }

    public void setCityMunicipalityName(String cityMunicipalityName) {
        this.cityMunicipalityName = cityMunicipalityName;
    }
}
