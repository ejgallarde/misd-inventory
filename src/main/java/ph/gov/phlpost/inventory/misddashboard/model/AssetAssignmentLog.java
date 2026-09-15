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

    @Column(name = "ReferenceType", nullable = false)
    private String referenceType;

    @Column(name = "AssetTag", nullable = false)
    private String assetTag;

    @Column(name = "EmployeeID", nullable = false)
    private String employeeID;

    /** The person actually using the asset at the time of this transaction, distinct from EmployeeID (the accountable person). */
    @Column(name = "EndUserID")
    private String endUserID;

    @Column(name = "ActionType", nullable = false)
    private String actionType;

    /** PAR/PTR/ICS document number backing this assignment, when one exists. */
    @Column(name = "DocumentNo")
    private String documentNo;

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

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getEndUserID() {
        return endUserID;
    }

    public void setEndUserID(String endUserID) {
        this.endUserID = endUserID;
    }

    public String getDocumentNo() {
        return documentNo;
    }

    public void setDocumentNo(String documentNo) {
        this.documentNo = documentNo;
    }
}