package ph.gov.phlpost.inventory.misddashboard.service;

import org.springframework.stereotype.Component;

@Component("surveyAssetHelper")
public class SurveyAssetWorkflowHelper {

    public String legalBadgeClass(String status) {
        if (status == null || status.isBlank()) {
            return "badge bg-dark";
        }
        return switch (status) {
            case "Registered/Accountable" -> "badge bg-success";
            case "Under Investigation" -> "badge bg-warning text-dark";
            case "Decommissioned", "Disposed", "Sold" -> "badge bg-dark";
            default -> "badge bg-dark";
        };
    }

    public String operationalBadgeClass(String status) {
        if (status == null || status.isBlank()) {
            return "badge bg-dark";
        }
        return switch (status) {
            case "Available/Idle" -> "badge bg-primary";
            case "Deployed/Field Use" -> "badge bg-success";
            case "Missing", "Stolen", "Slated for Disposal" -> "badge bg-danger";
            default -> "badge bg-dark";
        };
    }

    public String conditionBadgeClass(String status) {
        if (status == null || status.isBlank()) {
            return "badge bg-dark";
        }
        return switch (status) {
            case "Operational" -> "badge bg-success";
            case "Needs Calibration", "Under Repair" -> "badge bg-warning text-dark";
            case "Beyond Economic Repair (BER)" -> "badge bg-danger";
            case "Not Applicable" -> "badge bg-dark";
            default -> "badge bg-dark";
        };
    }

    public String legalTooltip(String status) {
        if (status == null) {
            return "Legal status.";
        }
        return switch (status) {
            case "Registered/Accountable" -> "Recorded and accountable for use.";
            case "Under Investigation" -> "Equipment is under investigation.";
            case "Decommissioned" -> "Removed from active circulation.";
            case "Disposed" -> "Disposed from equipment records.";
            case "Sold" -> "Sold and closed out.";
            default -> "Legal status.";
        };
    }

    public String operationalTooltip(String status) {
        if (status == null) {
            return "Operational status.";
        }
        return switch (status) {
            case "Available/Idle" -> "Ready for field deployment.";
            case "Deployed/Field Use" -> "Currently deployed for field survey work.";
            case "Missing" -> "Equipment is missing.";
            case "Stolen" -> "Equipment is reported stolen.";
            case "Slated for Disposal" -> "Queued for retirement/disposal.";
            default -> "Operational status.";
        };
    }

    public String conditionTooltip(String status) {
        if (status == null) {
            return "Condition status.";
        }
        return switch (status) {
            case "Operational" -> "Equipment is fit for use.";
            case "Needs Calibration" -> "Calibration is due or overdue.";
            case "Under Repair" -> "Actively being repaired.";
            case "Beyond Economic Repair (BER)" -> "Repair is no longer economical.";
            case "Not Applicable" -> "Status not applicable.";
            default -> "Condition status.";
        };
    }

    public String legalLabel(String status) {
        return status == null ? "" : "Legal: " + status;
    }

    public String operationalLabel(String status) {
        return status == null ? "" : "Operational: " + status;
    }

    public String conditionLabel(String status) {
        return status == null ? "" : "Condition: " + status;
    }

    public boolean hasActions(String legalStatus) {
        return !isTerminal(legalStatus);
    }

    public boolean canAssignCustodian(String operationalStatus) {
        return "Available/Idle".equals(operationalStatus);
    }

    public boolean canReturnAsset(String operationalStatus) {
        return "Deployed/Field Use".equals(operationalStatus);
    }

    public boolean canMarkUnderCalibration(String conditionStatus) {
        return !"Needs Calibration".equals(conditionStatus) && !"Beyond Economic Repair (BER)".equals(conditionStatus);
    }

    public boolean canMarkUnderRepair(String conditionStatus) {
        return !"Under Repair".equals(conditionStatus) && !"Beyond Economic Repair (BER)".equals(conditionStatus);
    }

    public boolean canMarkBeyondEconomicRepair(String conditionStatus) {
        return !"Beyond Economic Repair (BER)".equals(conditionStatus);
    }

    public boolean canMarkStolen(String legalStatus) {
        return !isTerminal(legalStatus);
    }

    public boolean canMarkMissing(String legalStatus) {
        return !isTerminal(legalStatus);
    }

    public boolean isTerminal(String legalStatus) {
        return "Decommissioned".equals(legalStatus)
                || "Disposed".equals(legalStatus)
                || "Sold".equals(legalStatus);
    }
}
