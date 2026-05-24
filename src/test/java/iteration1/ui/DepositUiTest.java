package iteration1.ui;

import generators.RandomData;
import models.CreateUserRequest;
import models.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;
import ui.pages.BankAlert;
import ui.pages.UserDashboard;
import ui.pages.DepositPage;

import static iteration1.ui.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;

public class DepositUiTest extends BaseUiTest {

    private String userToken;
    private int accountId;
    private double initialBalance;
    private UserDashboard dashboard;
    private DepositPage depositPage;

    @BeforeEach
    public void prepareData() {
        String username = RandomData.getUsername();
        String password = RandomData.getPassword();

        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .username(username)
                .password(password)
                .name(DEFAULT_USER_NAME)
                .role(UserRole.USER.toString())
                .build();

        AdminSteps.createUser(createUserRequest);
        userToken = UserSteps.loginAndGetToken(username, password);

        accountId = UserSteps.createAccount(userToken);
        initialBalance = UserSteps.getAccountBalance(userToken, accountId);

        authThroughLocalStorage(userToken);

        dashboard = new UserDashboard();
        depositPage = new DepositPage();
    }

    @Test
    public void userCanDepositValidAmountTest() {
        dashboard.open();
        dashboard.openDepositPage();
        depositPage.makeDeposit(VALID_DEPOSIT_AMOUNT, true, FIRST_ACCOUNT_INDEX);
        depositPage.checkAlertMessageAndAccept(BankAlert.DEPOSIT_SUCCESSFULLY.getMessage());

        double newBalance = UserSteps.getAccountBalance(userToken, accountId);
        assertThat(newBalance).isEqualTo(initialBalance + VALID_DEPOSIT_AMOUNT);
    }

    @Test
    public void userCanDepositMaxLimitAmountTest() {
        dashboard.open();
        dashboard.openDepositPage();
        depositPage.makeDeposit(MAX_DEPOSIT_AMOUNT, true, FIRST_ACCOUNT_INDEX);
        depositPage.checkAlertMessageAndAccept(BankAlert.DEPOSIT_SUCCESSFULLY.getMessage());

        double newBalance = UserSteps.getAccountBalance(userToken, accountId);
        assertThat(newBalance).isEqualTo(initialBalance + MAX_DEPOSIT_AMOUNT);
    }

    @Test
    public void userCannotDepositAboveLimitTest() {
        dashboard.open();
        dashboard.openDepositPage();
        depositPage.makeDeposit(INVALID_DEPOSIT_AMOUNT, true, FIRST_ACCOUNT_INDEX);
        depositPage.checkAlertMessageAndAccept(BankAlert.DEPOSIT_LIMIT_EXCEEDED.getMessage());

        double newBalance = UserSteps.getAccountBalance(userToken, accountId);
        assertThat(newBalance).isEqualTo(initialBalance);
    }

    @Test
    public void userCannotDepositWithoutAccountTest() {
        dashboard.open();
        dashboard.openDepositPage();
        depositPage.makeDeposit(DEPOSIT_AMOUNT_1000, false, FIRST_ACCOUNT_INDEX);
        depositPage.checkAlertMessageAndAccept(BankAlert.SELECT_ACCOUNT.getMessage());

        double newBalance = UserSteps.getAccountBalance(userToken, accountId);
        assertThat(newBalance).isEqualTo(initialBalance);
    }
}