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
                .name("Test User")
                .role(UserRole.USER.toString())
                .build();

        AdminSteps.createUser(createUserRequest);
        userToken = UserSteps.loginAndGetToken(username, password);

        fromAccountId = UserSteps.createAccount(userToken);
        toAccountId = UserSteps.createAccount(userToken);

        for (int i = 0; i < 3; i++) {
            UserSteps.deposit(userToken, fromAccountId, 5000.00);
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
        double transferAmount = 3000.00;

        openTransferPage();
        transferPage.selectFromAccount(1);
        transferPage.enterRecipientName("Test User");
        transferPage.enterRecipientAccount(toAccountNumber);
        transferPage.enterAmount(transferAmount);
        transferPage.confirm();
        transferPage.send();
        transferPage.checkAlertMessageAndAccept(BankAlert.TRANSFER_SUCCESSFULLY.getMessage());

        double newFromBalance = UserSteps.getAccountBalance(userToken, fromAccountId);
        double newToBalance = UserSteps.getAccountBalance(userToken, toAccountId);

        assertThat(newFromBalance).isEqualTo(fromInitialBalance - transferAmount);
        assertThat(newToBalance).isEqualTo(toInitialBalance + transferAmount);
    }

    @Test
    public void userCanTransferMaxLimitAmountTest() {
        double transferAmount = 10000.00;

        openTransferPage();
        transferPage.selectFromAccount(1);
        transferPage.enterRecipientName("Test User");
        transferPage.enterRecipientAccount(toAccountNumber);
        transferPage.enterAmount(transferAmount);
        transferPage.confirm();
        transferPage.send();
        transferPage.checkAlertMessageAndAccept(BankAlert.TRANSFER_SUCCESSFULLY.getMessage());

        double newFromBalance = UserSteps.getAccountBalance(userToken, fromAccountId);
        double newToBalance = UserSteps.getAccountBalance(userToken, toAccountId);

        assertThat(newFromBalance).isEqualTo(fromInitialBalance - transferAmount);
        assertThat(newToBalance).isEqualTo(toInitialBalance + transferAmount);
    }

    @Test
    public void userCannotTransferAboveLimitTest() {
        double invalidAmount = 10001.00;

        openTransferPage();
        transferPage.selectFromAccount(1);
        transferPage.enterRecipientName("Test User");
        transferPage.enterRecipientAccount(toAccountNumber);
        transferPage.enterAmount(invalidAmount);
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
        double invalidAmount = fromInitialBalance + 1000;

        openTransferPage();
        transferPage.selectFromAccount(1);
        transferPage.enterRecipientName("Test User");
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

        transferPage.enterRecipientName("Test User");
        transferPage.enterRecipientAccount(toAccountNumber);
        transferPage.enterAmount(1000);
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
        transferPage.selectFromAccount(1);
        transferPage.enterRecipientName("Test User");
        transferPage.enterRecipientAccount(toAccountNumber);
        transferPage.enterAmount(1000);
        transferPage.send();
        transferPage.checkAlertMessageAndAccept(BankAlert.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM.getMessage());

        double newFromBalance = UserSteps.getAccountBalance(userToken, fromAccountId);
        double newToBalance = UserSteps.getAccountBalance(userToken, toAccountId);

        assertThat(newFromBalance).isEqualTo(fromInitialBalance);
        assertThat(newToBalance).isEqualTo(toInitialBalance);
    }
}