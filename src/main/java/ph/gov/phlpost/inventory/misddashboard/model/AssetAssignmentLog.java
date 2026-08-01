package ph.gov.phlpost.inventory.misddashboard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "AssetAssignments")
public class AssetAssignmentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TransactionID")
    private Integer transactionID;

    @Column(name = "AssetTag", nullable = false)
    private String assetTag;

    @Column(name = "EmployeeID", nullable = false)
    private String employeeID;

    @Column(name = "ActionType", nullable = false)
    private String actionType;

    @Column(name = "TransactionDate")
    private LocalDateTime transactionDate;

    @Column(name = "ConditionNotes")
    private String conditionNotes;

    public AssetAssignmentLog() {
    }

    public Integer getTransactionID() {
        return transactionID;
    }

    public String getAssetTag() {
        return assetTag;
    }

    public String getEmployeeID() {
        return employeeID;
    }

    public String getActionType() {
        return actionType;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public String getConditionNotes() {
        return conditionNotes;
    }

    public void setAssetTag(String assetTag) {
        this.assetTag = assetTag;
    }

    public void setEmployeeID(String employeeID) {
        this.employeeID = employeeID;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public void setConditionNotes(String conditionNotes) {
        this.conditionNotes = conditionNotes;
    }
}