package ph.gov.phlpost.inventory.misddashboard.service;

import org.springframework.stereotype.Component;

@Component("fleetHelper")
public class FleetWorkflowHelper {

    public String legalBadgeClass(String status) {
        if (status == null || status.isBlank()) {
            return "badge bg-dark";
        }
        return switch (status) {
            case "Registered", "Active / Registered" -> "badge bg-success";
            case "Under Investigation" -> "badge bg-warning text-dark";
            case "Registration Expired", "Impounded" -> "badge bg-danger";
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
            case "Dispatched", "Dispatched/In Transit" -> "badge bg-success";
            case "Grounded", "Missing", "Missing/Stolen", "Stolen", "Slated for Disposal" -> "badge bg-danger";
            case "Reserved" -> "badge bg-warning text-dark";
            default -> "badge bg-dark";
        };
    }

    public String maintenanceBadgeClass(String status) {
        if (status == null || status.isBlank()) {
            return "badge bg-dark";
        }
        return switch (status) {
            case "Roadworthy" -> "badge bg-success";
            case "Under Repair" -> "badge bg-warning text-dark";
            case "Scheduled Maintenance", "Awaiting Parts" -> "badge bg-secondary";
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
            case "Registered", "Active / Registered" -> "Registered and compliant for use.";
            case "Under Investigation" -> "Vehicle is under investigation.";
            case "Registration Expired" -> "Registration renewal is required.";
            case "Impounded" -> "Vehicle is impounded and cannot operate.";
            case "Decommissioned" -> "Removed from active circulation.";
            case "Disposed" -> "Disposed from fleet records.";
            case "Sold" -> "Sold and closed out.";
            default -> "Legal status.";
        };
    }

    public String operationalTooltip(String status) {
        if (status == null) {
            return "Operational status.";
        }
        return switch (status) {
            case "Available/Idle" -> "Ready for dispatch.";
            case "Dispatched", "Dispatched/In Transit" -> "Currently deployed for operations.";
            case "Grounded" -> "Not allowed to operate pending action.";
            case "Missing", "Missing/Stolen" -> "Vehicle is missing.";
            case "Stolen" -> "Vehicle is reported stolen.";
            case "Slated for Disposal" -> "Queued for retirement/disposal.";
            case "Reserved" -> "Reserved and awaiting dispatch.";
            default -> "Operational status.";
        };
    }

    public String maintenanceTooltip(String status) {
        if (status == null) {
            return "Maintenance health status.";
        }
        return switch (status) {
            case "Roadworthy" -> "Vehicle is fit for operation.";
            case "Under Repair" -> "Actively being repaired.";
            case "Scheduled Maintenance" -> "Scheduled maintenance is pending.";
            case "Awaiting Parts" -> "Repair is waiting for parts.";
            case "Beyond Economic Repair (BER)" -> "Repair is no longer economical.";
            case "Not Applicable" -> "Status not applicable.";
            default -> "Maintenance health status.";
        };
    }

    public String legalLabel(String status) {
        return status == null ? "" : "Legal: " + status;
    }

    public String operationalLabel(String status) {
        return status == null ? "" : "Operational: " + status;
    }

    public String maintenanceLabel(String status) {
        return status == null ? "" : "Condition: " + status;
    }

    public boolean hasActions(String legalStatus) {
        return !isTerminal(legalStatus);
    }

    public boolean canAssignDriver(String operationalStatus) {
        return "Available/Idle".equals(operationalStatus);
    }

    public boolean canReturnToMotorpool(String operationalStatus) {
        return "Dispatched".equals(operationalStatus) || "Dispatched/In Transit".equals(operationalStatus);
    }

    public boolean canMarkUnderMaintenance(String maintenanceStatus) {
        return !"Under Repair".equals(maintenanceStatus) && !"Beyond Economic Repair (BER)".equals(maintenanceStatus);
    }

    public boolean canMarkImpounded(String legalStatus) {
        return !isTerminal(legalStatus);
    }

    public boolean canMarkBeyondEconomicRepair(String maintenanceStatus) {
        return !"Beyond Economic Repair (BER)".equals(maintenanceStatus);
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
