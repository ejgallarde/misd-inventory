package ph.gov.phlpost.inventory.misddashboard.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "LifecycleAuditLog")
public class LifecycleAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LogID")
    private Integer logID;

    @Column(name = "ReferenceID", nullable = false)
    private String referenceID;

    @Column(name = "ActionType", nullable = false)
    private String actionType;

    @Column(name = "PerformedBy", nullable = false)
    private String performedBy;

    @Column(name = "TransactionDate", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "Notes")
    private String notes;

    public LifecycleAuditLog() {
    }

    public Integer getLogID() {
        return logID;
    }

    public void setLogID(Integer logID) {
        this.logID = logID;
    }

    public String getReferenceID() {
        return referenceID;
    }

    public void setReferenceID(String referenceID) {
        this.referenceID = referenceID;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}