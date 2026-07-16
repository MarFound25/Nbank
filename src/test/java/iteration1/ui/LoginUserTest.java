package iteration1.ui;

import com.codeborne.selenide.Condition;
import common.annotations.Browsers;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import org.junit.jupiter.api.Test;
import ui.pages.AdminPanel;
import ui.pages.LoginPage;
import ui.pages.UserDashboard;
import ui.pages.TestDataConstants;

public class LoginUserTest extends BaseUiTest {

    @Test
    @Browsers({"chrome"})
    public void adminCanLoginWithCorrectDataTest() {
        new LoginPage().open();
        common.helpers.UiBrokenJsonMock.setAdminUsersOverride(java.util.List.of());
        new LoginPage()
                .login(TestDataConstants.ADMIN_USERNAME, TestDataConstants.ADMIN_PASSWORD)
                .getPage(AdminPanel.class)
                .getAdminPanelText()
                .shouldBe(Condition.visible);
    }

    @Test
    @UserSession
    public void userCanLoginWithCorrectDataTest() {
        new LoginPage().open()
                .login(SessionStorage.getUser().getUsername(), SessionStorage.getUser().getPassword())
                .getPage(UserDashboard.class)
                .getWelcomeText()
                .shouldBe(Condition.visible)
                .shouldHave(Condition.text(TestDataConstants.WELCOME_TEXT_PREFIX));
    }
}
