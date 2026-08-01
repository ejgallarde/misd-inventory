package ph.gov.phlpost.inventory.misddashboard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "locations")
public class PersonnelBaseLocation {

    @Id
    @Column(name = "LocationID")
    private Integer locationID;

    @Column(name = "Area")
    private String area;

    @Column(name = "Province")
    private String province;

    @Column(name = "OfficeAddress")
    private String officeAddress;

    public Integer getLocationID() {
        return locationID;
    }

    public String getArea() {
        return area;
    }

    public String getProvince() {
        return province;
    }

    public String getOfficeAddress() {
        return officeAddress;
    }
}