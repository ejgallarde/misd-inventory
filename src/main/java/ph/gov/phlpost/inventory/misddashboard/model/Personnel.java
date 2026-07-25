package ph.gov.phlpost.inventory.misddashboard.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Personnel")
public class Personnel {

    @Id
    @Column(name = "EmployeeID")
    private String employeeID;

    @Column(name = "FirstName")
    private String firstName;

    @Column(name = "LastName")
    private String lastName;

    @Column(name = "Department")
    private String department;

    @Column(name = "Division")
    private String division;

    @Column(name = "ManagerID")
    private String managerID;

    public Personnel() {
    }

    // Getters
    public String getEmployeeID() {
        return employeeID;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDepartment() {
        return department;
    }

    public String getDivision() {
        return division;
    }

    public String getManagerID() {
        return managerID;
    }
}