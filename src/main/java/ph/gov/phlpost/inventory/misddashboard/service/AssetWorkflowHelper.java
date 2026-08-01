package ph.gov.phlpost.inventory.misddashboard.service;

import org.springframework.stereotype.Component;

/**
 * Thymeleaf utility bean for IT asset status badges and action workflow rules.
 *
 * Accessible in templates as: ${@assetHelper.method(param)}
 *
 * Centralising all badge CSS logic and action predicates here means:
 * - Templates stay formatter-safe (no long nested ternaries).
 * - Adding a new status / action requires a single-file change.
 * - Logic is unit-testable without a browser or Spring context.
 */
@Component("assetHelper")
public class AssetWorkflowHelper {

    // -----------------------------------------------------------------------
    // Badge CSS classes
    // -----------------------------------------------------------------------

    public String deploymentBadgeClass(String status) {
        if (status == null || status.isBlank())
            return "badge bg-dark";
        return switch (status) {
            case "In Storage" -> "badge bg-secondary";
            case "Deployed" -> "badge bg-success";
            case "Unavailable", "Missing / Unaccounted" -> "badge bg-danger";
            case "With Service Center", "With MISD Technician" -> "badge bg-info";
            default -> "badge bg-dark";
        };
    }

    public String healthBadgeClass(String status) {
        if (status == null || status.isBlank())
            return "badge bg-dark";
        return switch (status) {
            case "Operational" -> "badge bg-success";
            case "Degraded" -> "badge bg-warning text-dark";
            case "Under Repair" -> "badge bg-danger";
            case "Beyond Economic Repair (BER)" -> "badge bg-danger";
            case "Not Applicable" -> "badge bg-dark";
            default -> "badge bg-dark";
        };
    }

    public String lifecycleBadgeClass(String status) {
        if (status == null || status.isBlank())
            return "badge bg-dark";
        return "Active".equals(status) ? "badge bg-success" : "badge bg-dark";
    }

    // -----------------------------------------------------------------------
    // Tooltip text
    // -----------------------------------------------------------------------

    public String deploymentTooltip(String status) {
        if (status == null)
            return "Deployment status.";
        return switch (status) {
            case "In Storage" -> "Stored and ready for deployment.";
            case "Deployed" -> "Currently assigned to a user.";
            case "Missing / Unaccounted" -> "Cannot be physically accounted for.";
            case "Unavailable" -> "Not available for deployment.";
            case "With Service Center" -> "In warranty or service repair.";
            case "With MISD Technician" -> "Assigned to an MISD technician for repair.";
            default -> "Deployment status.";
        };
    }

    public String healthTooltip(String status) {
        if (status == null)
            return "Maintenance health status.";
        return switch (status) {
            case "Operational" -> "Fully working.";
            case "Degraded" -> "Working with reduced performance.";
            case "Under Repair" -> "Under repair by technician/vendor.";
            case "Beyond Economic Repair (BER)" -> "Repair cost exceeds asset value.";
            case "Not Applicable" -> "Status not applicable to this asset.";
            default -> "Maintenance health status.";
        };
    }

    public String lifecycleTooltip(String status) {
        if (status == null)
            return "Lifecycle status.";
        return switch (status) {
            case "Procured / Pre-Deployment" -> "Procured and preparing for use.";
            case "Active" -> "Currently in active service.";
            case "End of Life (EOL)" -> "Reached end-of-life and needs disposition planning.";
            case "Decommissioned / Retired" -> "Removed from active service.";
            case "Disposed" -> "Disposed from inventory.";
            case "Sold" -> "Sold and closed out.";
            default -> "Lifecycle status.";
        };
    }

    // -----------------------------------------------------------------------
    // Display label
    // -----------------------------------------------------------------------

    /** Returns the display label for a deployment status. */
    public String deploymentLabel(String status) {
        if (status == null)
            return "";
        return "Deployment: " + status;
    }

    // -----------------------------------------------------------------------
    // Action predicates (used by assets.html action dropdown)
    // -----------------------------------------------------------------------

    /** Show "Actions" dropdown at all? */
    public boolean hasActions(String lifecycleStatus) {
        return !"Decommissioned / Retired".equals(lifecycleStatus)
                && !"Disposed".equals(lifecycleStatus)
                && !"Sold".equals(lifecycleStatus);
    }

    /** "Assign Asset" — asset is available for deployment. */
    public boolean canAssign(String deploymentStatus) {
        return "In Storage".equals(deploymentStatus);
    }

    /** "Re-assign User" — asset is already out with someone. */
    public boolean canReassign(String deploymentStatus) {
        return "Deployed".equals(deploymentStatus);
    }

    /**
     * "Return to MISD" — asset is deployed and needs to come back.
     */
    public boolean canReturn(String deploymentStatus) {
        return "Deployed".equals(deploymentStatus);
    }

    /** "Return for Warranty" — only when not already under repair. */
    public boolean canReturnForWarranty(String maintenanceHealthStatus) {
        return !"Under Repair".equals(maintenanceHealthStatus);
    }

    public boolean canSendForMisdMaintenance(String maintenanceHealthStatus) {
        return !"Under Repair".equals(maintenanceHealthStatus);
    }

    public boolean canMarkRepaired(String deploymentStatus, String maintenanceHealthStatus) {
        return "Under Repair".equals(maintenanceHealthStatus)
                && ("With Service Center".equals(deploymentStatus)
                        || "With MISD Technician".equals(deploymentStatus));
    }

    /** "Mark Unserviceable" — only when not already BER. */
    public boolean canMarkUnserviceable(String maintenanceHealthStatus) {
        return !"Beyond Economic Repair (BER)".equals(maintenanceHealthStatus);
    }

    /** BER assets are locked to the disposal/retirement workflow. */
    public boolean isUnserviceable(String maintenanceHealthStatus) {
        return "Beyond Economic Repair (BER)".equals(maintenanceHealthStatus);
    }
}
