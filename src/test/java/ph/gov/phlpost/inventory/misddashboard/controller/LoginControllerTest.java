package ph.gov.phlpost.inventory.misddashboard.controller;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import static org.assertj.core.api.Assertions.assertThat;

class LoginControllerTest {

    @Test
    void loginPageExposesEntraConfigurationFlag() {
        LoginController controller = new LoginController(false, "", "", "");
        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = controller.login(model);

        assertThat(viewName).isEqualTo("login");
        assertThat(model.getAttribute("entraConfigured")).isEqualTo(false);
    }
}
