package iteration1.ui;

import common.annotations.Browsers;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import org.junit.jupiter.api.Test;
import ui.pages.AdminPanel;
import ui.pages.LoginPage;
import ui.pages.TestDataConstants;
import ui.pages.UserDashboard;

public class LoginUserTest extends BaseUiTest {

    @Test
    @Browsers({"chrome"})
    public void adminCanLoginWithCorrectDataTest() {
        new LoginPage()
                .open()
                .login(TestDataConstants.ADMIN_USERNAME, TestDataConstants.ADMIN_PASSWORD)
                .getPage(AdminPanel.class)
                .getAdminPanelText();  // проверка видимости внутри метода
    }

    @Test
    @UserSession
    public void userCanLoginWithCorrectDataTest() {
        new LoginPage()
                .open()
                .login(SessionStorage.getUser().getUsername(), SessionStorage.getUser().getPassword())
                .getPage(UserDashboard.class)
                .welcomeTextShouldHave(TestDataConstants.WELCOME_TEXT_PREFIX);
    }
}