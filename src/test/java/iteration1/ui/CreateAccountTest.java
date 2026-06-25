package iteration1.ui;

import com.codeborne.selenide.Condition;
import models.CreateAccountResponse;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import org.junit.jupiter.api.Test;
import ui.pages.BankAlert;
import ui.pages.UserDashboard;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateAccountTest extends BaseUiTest {

    @Test
    @UserSession
    public void userCanCreateAccountTest() {
        UserDashboard dashboard = new UserDashboard().open();

        dashboard.getWelcomeText().shouldBe(Condition.visible);

        dashboard.createNewAccount();

        List<CreateAccountResponse> createdAccounts = SessionStorage.getSteps().getAllAccounts();

        assertThat(createdAccounts)
                .as("Должен быть создан ровно один счет")
                .hasSize(1);

        dashboard.checkAlertMessageAndAccept(
                BankAlert.NEW_ACCOUNT_CREATED.getMessage() + createdAccounts.getFirst().getAccountNumber());

        assertThat(createdAccounts.getFirst().getBalance())
                .as("Баланс нового счета должен быть 0")
                .isZero();
    }
}