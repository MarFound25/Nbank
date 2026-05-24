package iteration1.ui;

import generators.RandomData;
import models.Account;
import models.CreateUserRequest;
import models.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;
import ui.pages.UserDashboard;
import ui.pages.TransferPage;
import ui.pages.BankAlert;

import static iteration1.ui.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;

public class TransferUiTest extends BaseUiTest {

    private String userToken;
    private int fromAccountId;
    private int toAccountId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private double fromInitialBalance;
    private double toInitialBalance;
    private UserDashboard dashboard;
    private TransferPage transferPage;

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

        fromAccountId = UserSteps.createAccount(userToken);
        toAccountId = UserSteps.createAccount(userToken);

        for (int i = 0; i < 3; i++) {
            UserSteps.deposit(userToken, fromAccountId, MAX_DEPOSIT_AMOUNT);
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

        authThroughLocalStorage(userToken);

        dashboard = new UserDashboard();
        transferPage = new TransferPage();
    }

    private void openTransferPage() {
        dashboard.open();
        dashboard.openTransferPage();
    }

    @Test
    public void userCanTransferValidAmountTest() {
        openTransferPage();
        transferPage.selectFromAccount(FIRST_ACCOUNT_INDEX);
        transferPage.enterRecipientName(DEFAULT_RECIPIENT_NAME);
        transferPage.enterRecipientAccount(toAccountNumber);
        transferPage.enterAmount(VALID_TRANSFER_AMOUNT);
        transferPage.confirm();
        transferPage.send();
        transferPage.checkAlertMessageAndAccept(BankAlert.TRANSFER_SUCCESSFULLY.getMessage());

        double newFromBalance = UserSteps.getAccountBalance(userToken, fromAccountId);
        double newToBalance = UserSteps.getAccountBalance(userToken, toAccountId);

        assertThat(newFromBalance).isEqualTo(fromInitialBalance - VALID_TRANSFER_AMOUNT);
        assertThat(newToBalance).isEqualTo(toInitialBalance + VALID_TRANSFER_AMOUNT);
    }

    @Test
    public void userCanTransferMaxLimitAmountTest() {
        openTransferPage();
        transferPage.selectFromAccount(FIRST_ACCOUNT_INDEX);
        transferPage.enterRecipientName(DEFAULT_RECIPIENT_NAME);
        transferPage.enterRecipientAccount(toAccountNumber);
        transferPage.enterAmount(MAX_TRANSFER_AMOUNT);
        transferPage.confirm();
        transferPage.send();
        transferPage.checkAlertMessageAndAccept(BankAlert.TRANSFER_SUCCESSFULLY.getMessage());

        double newFromBalance = UserSteps.getAccountBalance(userToken, fromAccountId);
        double newToBalance = UserSteps.getAccountBalance(userToken, toAccountId);

        assertThat(newFromBalance).isEqualTo(fromInitialBalance - MAX_TRANSFER_AMOUNT);
        assertThat(newToBalance).isEqualTo(toInitialBalance + MAX_TRANSFER_AMOUNT);
    }

    @Test
    public void userCannotTransferAboveLimitTest() {
        openTransferPage();
        transferPage.selectFromAccount(FIRST_ACCOUNT_INDEX);
        transferPage.enterRecipientName(DEFAULT_RECIPIENT_NAME);
        transferPage.enterRecipientAccount(toAccountNumber);
        transferPage.enterAmount(INVALID_TRANSFER_AMOUNT);
        transferPage.confirm();
        transferPage.send();
        transferPage.checkAlertMessageAndAccept(BankAlert.TRANSFER_LIMIT_EXCEEDED.getMessage());

        double newFromBalance = UserSteps.getAccountBalance(userToken, fromAccountId);
        double newToBalance = UserSteps.getAccountBalance(userToken, toAccountId);

        assertThat(newFromBalance).isEqualTo(fromInitialBalance);
        assertThat(newToBalance).isEqualTo(toInitialBalance);
    }

    @Test
    public void userCannotTransferMoreThanBalanceTest() {
        double invalidAmount = fromInitialBalance + TRANSFER_AMOUNT_1000;

        openTransferPage();
        transferPage.selectFromAccount(FIRST_ACCOUNT_INDEX);
        transferPage.enterRecipientName(DEFAULT_RECIPIENT_NAME);
        transferPage.enterRecipientAccount(toAccountNumber);
        transferPage.enterAmount(invalidAmount);
        transferPage.confirm();
        transferPage.send();
        transferPage.checkAlertMessageAndAccept(BankAlert.TRANSFER_AMOUNT_EXCEED.getMessage());

        double newFromBalance = UserSteps.getAccountBalance(userToken, fromAccountId);
        double newToBalance = UserSteps.getAccountBalance(userToken, toAccountId);

        assertThat(newFromBalance).isEqualTo(fromInitialBalance);
        assertThat(newToBalance).isEqualTo(toInitialBalance);
    }

    @Test
    public void userCannotTransferWithoutFromAccountTest() {
        openTransferPage();

        transferPage.enterRecipientName(DEFAULT_RECIPIENT_NAME);
        transferPage.enterRecipientAccount(toAccountNumber);
        transferPage.enterAmount(TRANSFER_AMOUNT_1000);
        transferPage.confirm();
        transferPage.send();
        transferPage.checkAlertMessageAndAccept(BankAlert.PLEASE_FILL_ALL_FIELDS.getMessage());

        double newFromBalance = UserSteps.getAccountBalance(userToken, fromAccountId);
        double newToBalance = UserSteps.getAccountBalance(userToken, toAccountId);

        assertThat(newFromBalance).isEqualTo(fromInitialBalance);
        assertThat(newToBalance).isEqualTo(toInitialBalance);
    }

    @Test
    public void userCannotTransferWithoutConfirmationTest() {
        openTransferPage();
        transferPage.selectFromAccount(FIRST_ACCOUNT_INDEX);
        transferPage.enterRecipientName(DEFAULT_RECIPIENT_NAME);
        transferPage.enterRecipientAccount(toAccountNumber);
        transferPage.enterAmount(TRANSFER_AMOUNT_1000);
        transferPage.send();
        transferPage.checkAlertMessageAndAccept(BankAlert.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM.getMessage());

        double newFromBalance = UserSteps.getAccountBalance(userToken, fromAccountId);
        double newToBalance = UserSteps.getAccountBalance(userToken, toAccountId);

        assertThat(newFromBalance).isEqualTo(fromInitialBalance);
        assertThat(newToBalance).isEqualTo(toInitialBalance);
    }
}