package iteration1.ui;

import models.CreateAccountResponse;
import models.UserWithPassword;
import org.junit.jupiter.api.Test;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;
import ui.pages.BankAlert;
import ui.pages.UserDashboard;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateAccountTest extends BaseUiTest {

    @Test
    public void userCanCreateAccountTest() {
        UserWithPassword user = AdminSteps.createUserWithPassword();
        authThroughLocalStorage(user.getUsername(), user.getPassword());

        new UserDashboard().open()
                .createNewAccount()
                .checkAlertMessageAndAccept(BankAlert.NEW_ACCOUNT_CREATED.getMessage());

        List<CreateAccountResponse> createdAccounts = new UserSteps(user.getUsername(), user.getPassword())
                .getAllAccounts();

        assertThat(createdAccounts).hasSize(1);
        assertThat(createdAccounts.getFirst().getBalance()).isZero();
    }
}