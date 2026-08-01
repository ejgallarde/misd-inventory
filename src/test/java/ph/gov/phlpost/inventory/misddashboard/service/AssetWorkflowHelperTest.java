package ph.gov.phlpost.inventory.misddashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AssetWorkflowHelperTest {

    private final AssetWorkflowHelper helper = new AssetWorkflowHelper();

    @Test
    void repairActionsFollowMaintenanceState() {
        assertThat(helper.canSendForMisdMaintenance("Operational")).isTrue();
        assertThat(helper.canSendForMisdMaintenance("Under Repair")).isFalse();
        assertThat(helper.canReturnForWarranty("Operational")).isTrue();
        assertThat(helper.canReturnForWarranty("Under Repair")).isFalse();
    }

    @Test
    void markRepairedIsAvailableOnlyForRecognizedRepairAssignments() {
        assertThat(helper.canMarkRepaired("With Service Center", "Under Repair")).isTrue();
        assertThat(helper.canMarkRepaired("With MISD Technician", "Under Repair")).isTrue();
        assertThat(helper.canMarkRepaired("Deployed", "Under Repair")).isFalse();
        assertThat(helper.canMarkRepaired("With MISD Technician", "Operational")).isFalse();
    }
}