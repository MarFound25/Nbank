package iteration1.ui;

import models.CreateUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import requests.steps.UserSteps;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import ui.pages.BankAlert;
import ui.pages.UserDashboard;
import ui.pages.TestDataConstants;
import ui.pages.BasePage;

import static org.assertj.core.api.Assertions.assertThat;

public class DepositUiTest extends BaseUiTest {

    private String userToken;
    private int accountId;
    private double initialBalance;

    @BeforeEach
    public void prepareData() {
        CreateUserRequest user = SessionStorage.getUser();
        userToken = UserSteps.loginAndGetToken(user.getUsername(), user.getPassword());

        accountId = UserSteps.createAccount(userToken);
        initialBalance = UserSteps.getAccountBalance(userToken, accountId);

        BasePage.authAsUser(user);
    }

    @Test
    @UserSession
    public void userCanDepositValidAmountTest() {
        new UserDashboard().open()
                .openDepositPage()
                .makeDeposit(TestDataConstants.VALID_DEPOSIT_AMOUNT, true, TestDataConstants.FIRST_ACCOUNT_INDEX)
                .checkAlertMessageAndAccept(BankAlert.DEPOSIT_SUCCESSFULLY.getMessage());

        double newBalance = UserSteps.getAccountBalance(userToken, accountId);
        assertThat(newBalance).isEqualTo(initialBalance + TestDataConstants.VALID_DEPOSIT_AMOUNT);
    }

    @Test
    @UserSession
    public void userCanDepositMaxLimitAmountTest() {
        new UserDashboard().open()
                .openDepositPage()
                .makeDeposit(TestDataConstants.MAX_DEPOSIT_AMOUNT, true, TestDataConstants.FIRST_ACCOUNT_INDEX)
                .checkAlertMessageAndAccept(BankAlert.DEPOSIT_SUCCESSFULLY.getMessage());

        double newBalance = UserSteps.getAccountBalance(userToken, accountId);
        assertThat(newBalance).isEqualTo(initialBalance + TestDataConstants.MAX_DEPOSIT_AMOUNT);
    }

    @Test
    @UserSession
    public void userCannotDepositAboveLimitTest() {
        new UserDashboard().open()
                .openDepositPage()
                .makeDeposit(TestDataConstants.INVALID_DEPOSIT_AMOUNT, true, TestDataConstants.FIRST_ACCOUNT_INDEX)
                .checkAlertMessageAndAccept(BankAlert.DEPOSIT_LIMIT_EXCEEDED.getMessage());

        double newBalance = UserSteps.getAccountBalance(userToken, accountId);
        assertThat(newBalance).isEqualTo(initialBalance);
    }

    @Test
    @UserSession
    public void userCannotDepositWithoutAccountTest() {
        new UserDashboard().open()
                .openDepositPage()
                .makeDeposit(TestDataConstants.TRANSFER_AMOUNT_1000, false, TestDataConstants.FIRST_ACCOUNT_INDEX)
                .checkAlertMessageAndAccept(BankAlert.SELECT_ACCOUNT.getMessage());

        double newBalance = UserSteps.getAccountBalance(userToken, accountId);
        assertThat(newBalance).isEqualTo(initialBalance);
    }
}