package iteration1.ui;

import models.Account;
import models.CreateUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import requests.steps.UserSteps;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import ui.pages.UserDashboard;
import ui.pages.TransferPage;
import ui.pages.BankAlert;
import ui.pages.TestDataConstants;
import ui.pages.BasePage;

import static org.assertj.core.api.Assertions.assertThat;

public class TransferUiTest extends BaseUiTest {

    private String userToken;
    private int fromAccountId;
    private int toAccountId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private double fromInitialBalance;
    private double toInitialBalance;

    @BeforeEach
    public void prepareData() {
        CreateUserRequest user = SessionStorage.getUser();
        userToken = UserSteps.loginAndGetToken(user.getUsername(), user.getPassword());

        fromAccountId = UserSteps.createAccount(userToken);
        toAccountId = UserSteps.createAccount(userToken);

        for (int i = 0; i < TestDataConstants.DEPOSIT_STEPS_COUNT; i++) {
            UserSteps.deposit(userToken, fromAccountId, TestDataConstants.MAX_DEPOSIT_AMOUNT);
        }

        Account[] accounts = UserSteps.getAccounts(userToken).toArray(new Account[0]);
        for (Account account : accounts) {
            if (account.getId() == fromAccountId) {
                fromAccountNumber = account.getAccountNumber();
                fromInitialBalance = account.getBalance();
            }
            if (account.getId() == toAccountId) {
                toAccountNumber = account.getAccountNumber();
                toInitialBalance = account.getBalance();
            }
        }

        BasePage.authAsUser(user);
    }

    @Test
    @UserSession
    public void userCanTransferValidAmountTest() {
        new UserDashboard().open()
                .openTransferPage()
                .makeTransfer(TestDataConstants.FIRST_ACCOUNT_INDEX, toAccountNumber, TestDataConstants.VALID_TRANSFER_AMOUNT)
                .checkAlertMessageAndAccept(BankAlert.TRANSFER_SUCCESSFULLY.getMessage());

        double newFromBalance = UserSteps.getAccountBalance(userToken, fromAccountId);
        double newToBalance = UserSteps.getAccountBalance(userToken, toAccountId);

        assertThat(newFromBalance).isEqualTo(fromInitialBalance - TestDataConstants.VALID_TRANSFER_AMOUNT);
        assertThat(newToBalance).isEqualTo(toInitialBalance + TestDataConstants.VALID_TRANSFER_AMOUNT);
    }

    @Test
    @UserSession
    public void userCanTransferMaxLimitAmountTest() {
        new UserDashboard().open()
                .openTransferPage()
                .makeTransfer(TestDataConstants.FIRST_ACCOUNT_INDEX, toAccountNumber, TestDataConstants.MAX_TRANSFER_AMOUNT)
                .checkAlertMessageAndAccept(BankAlert.TRANSFER_SUCCESSFULLY.getMessage());

        double newFromBalance = UserSteps.getAccountBalance(userToken, fromAccountId);
        double newToBalance = UserSteps.getAccountBalance(userToken, toAccountId);

        assertThat(newFromBalance).isEqualTo(fromInitialBalance - TestDataConstants.MAX_TRANSFER_AMOUNT);
        assertThat(newToBalance).isEqualTo(toInitialBalance + TestDataConstants.MAX_TRANSFER_AMOUNT);
    }

    @Test
    @UserSession
    public void userCannotTransferAboveLimitTest() {
        new UserDashboard().open()
                .openTransferPage()
                .makeTransfer(TestDataConstants.FIRST_ACCOUNT_INDEX, toAccountNumber, TestDataConstants.INVALID_TRANSFER_AMOUNT)
                .checkAlertMessageAndAccept(BankAlert.TRANSFER_LIMIT_EXCEEDED.getMessage());

        double newFromBalance = UserSteps.getAccountBalance(userToken, fromAccountId);
        double newToBalance = UserSteps.getAccountBalance(userToken, toAccountId);

        assertThat(newFromBalance).isEqualTo(fromInitialBalance);
        assertThat(newToBalance).isEqualTo(toInitialBalance);
    }

    @Test
    @UserSession
    public void userCannotTransferMoreThanBalanceTest() {
        double invalidAmount = fromInitialBalance + TestDataConstants.TRANSFER_AMOUNT_1000;

        new UserDashboard().open()
                .openTransferPage()
                .makeTransfer(TestDataConstants.FIRST_ACCOUNT_INDEX, toAccountNumber, invalidAmount)
                .checkAlertMessageAndAccept(BankAlert.TRANSFER_AMOUNT_EXCEED.getMessage());

        double newFromBalance = UserSteps.getAccountBalance(userToken, fromAccountId);
        double newToBalance = UserSteps.getAccountBalance(userToken, toAccountId);

        assertThat(newFromBalance).isEqualTo(fromInitialBalance);
        assertThat(newToBalance).isEqualTo(toInitialBalance);
    }

    @Test
    @UserSession
    public void userCannotTransferWithoutFromAccountTest() {
        new UserDashboard().open()
                .openTransferPage();

        new TransferPage()
                .enterRecipientName(TestDataConstants.DEFAULT_USER_NAME)
                .enterRecipientAccount(toAccountNumber)
                .enterAmount(TestDataConstants.TRANSFER_AMOUNT_1000)
                .confirm()
                .send()
                .checkAlertMessageAndAccept(BankAlert.PLEASE_FILL_ALL_FIELDS.getMessage());

        double newFromBalance = UserSteps.getAccountBalance(userToken, fromAccountId);
        double newToBalance = UserSteps.getAccountBalance(userToken, toAccountId);

        assertThat(newFromBalance).isEqualTo(fromInitialBalance);
        assertThat(newToBalance).isEqualTo(toInitialBalance);
    }

    @Test
    @UserSession
    public void userCannotTransferWithoutConfirmationTest() {
        new UserDashboard().open()
                .openTransferPage()
                .selectFromAccount(TestDataConstants.FIRST_ACCOUNT_INDEX)
                .enterRecipientName(TestDataConstants.DEFAULT_USER_NAME)
                .enterRecipientAccount(toAccountNumber)
                .enterAmount(TestDataConstants.TRANSFER_AMOUNT_1000)
                .send()
                .checkAlertMessageAndAccept(BankAlert.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM.getMessage());

        double newFromBalance = UserSteps.getAccountBalance(userToken, fromAccountId);
        double newToBalance = UserSteps.getAccountBalance(userToken, toAccountId);

        assertThat(newFromBalance).isEqualTo(fromInitialBalance);
        assertThat(newToBalance).isEqualTo(toInitialBalance);
    }
}