package ph.gov.phlpost.inventory.misddashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FleetWorkflowHelperTest {

    private final FleetWorkflowHelper helper = new FleetWorkflowHelper();

    @ParameterizedTest
    @ValueSource(strings = { "Decommissioned", "Disposed", "Sold" })
    void terminalLegalStatusesHideEveryLifecycleAction(String legalStatus) {
        assertThat(helper.isTerminal(legalStatus)).isTrue();
        assertThat(helper.hasActions(legalStatus)).isFalse();
        assertThat(helper.canMarkImpounded(legalStatus)).isFalse();
        assertThat(helper.canMarkStolen(legalStatus)).isFalse();
        assertThat(helper.canMarkMissing(legalStatus)).isFalse();
    }

    @Test
    void onlyIdleVehiclesCanBeAssignedAndOnlyDispatchedOnesReturned() {
        assertThat(helper.canAssignDriver("Available/Idle")).isTrue();
        assertThat(helper.canAssignDriver("Dispatched")).isFalse();
        assertThat(helper.canReturnToMotorpool("Dispatched")).isTrue();
        assertThat(helper.canReturnToMotorpool("Dispatched/In Transit")).isTrue();
        assertThat(helper.canReturnToMotorpool("Available/Idle")).isFalse();
    }

    @Test
    void maintenanceActionsFollowCurrentCondition() {
        assertThat(helper.canMarkUnderMaintenance("Roadworthy")).isTrue();
        assertThat(helper.canMarkUnderMaintenance("Under Repair")).isFalse();
        assertThat(helper.canMarkUnderMaintenance("Beyond Economic Repair (BER)")).isFalse();
        assertThat(helper.canMarkBeyondEconomicRepair("Beyond Economic Repair (BER)")).isFalse();
    }

    @Test
    void everyConfiguredDropdownValueHasASpecificBadge() {
        // Values from application-dropdowns.properties; "badge bg-dark" is the
        // fallback for an unrecognised value, so a typo there shows up here.
        for (String status : new String[] { "Registered", "Registration Expired", "Under Investigation",
                "Impounded" }) {
            assertThat(helper.legalTooltip(status)).as(status).isNotEqualTo("Legal status.");
        }
        for (String status : new String[] { "Available/Idle", "Dispatched", "Grounded", "Missing", "Stolen",
                "Slated for Disposal" }) {
            assertThat(helper.operationalBadgeClass(status)).as(status).isNotEqualTo("badge bg-dark");
        }
        for (String status : new String[] { "Roadworthy", "Under Repair", "Beyond Economic Repair (BER)" }) {
            assertThat(helper.maintenanceBadgeClass(status)).as(status).isNotEqualTo("badge bg-dark");
        }
    }

    @Test
    void labelsAreEmptyForMissingStatus() {
        assertThat(helper.legalLabel(null)).isEmpty();
        assertThat(helper.operationalLabel("Grounded")).isEqualTo("Operational: Grounded");
        assertThat(helper.maintenanceLabel("Roadworthy")).isEqualTo("Condition: Roadworthy");
    }
}
