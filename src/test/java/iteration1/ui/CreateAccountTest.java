package iteration1.ui;

import common.annotations.UserSession;
import org.junit.jupiter.api.Test;
import ui.pages.UserDashboard;

public class CreateAccountTest extends BaseUiTest {

    @Test
    @UserSession
    public void userCanCreateAccountTest() {
        new UserDashboard()
                .open()
                .getWelcomeText()
                .createNewAccount()
                .assertAccountSize(1)
                .assertBalanceIsZero()
                .checkAlertWithAccountNumber();
    }
    }