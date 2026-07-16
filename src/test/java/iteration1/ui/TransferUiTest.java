package iteration1.ui;

import api.dao.AccountDao;
import models.AccountDTO;
import models.CreateUserRequest;
import models.CreateUserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import requests.steps.DataBaseSteps;
import requests.steps.UserSteps;
import common.annotations.UserSession;
import common.helpers.UiBrokenJsonMock;
import common.storage.SessionStorage;
import ui.pages.TransferPage;
import ui.pages.BankAlert;
import ui.pages.TestDataConstants;
import ui.pages.BasePage;

import java.util.List;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
import static org.assertj.core.api.Assertions.assertThat;

public class TransferUiTest extends BaseUiTest {

    private String userToken;
    private int fromAccountId;
    private int toAccountId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private double fromInitialBalance;
    private double toInitialBalance;
    private List<AccountDTO> mockAccounts;

    @BeforeEach
    public void prepareData() {
        CreateUserRequest user = SessionStorage.getUser();
        userToken = UserSteps.loginAndGetToken(user.getUsername(), user.getPassword());

        fromAccountId = UserSteps.createAccount(userToken);
        toAccountId = UserSteps.createAccount(userToken);

        for (int i = 0; i < TestDataConstants.DEPOSIT_STEPS_COUNT; i++) {
            UserSteps.deposit(userToken, fromAccountId, TestDataConstants.MAX_DEPOSIT_AMOUNT);
        }

        AccountDao fromAccount = DataBaseSteps.getAccountById((long) fromAccountId);
        AccountDao toAccount = DataBaseSteps.getAccountById((long) toAccountId);
        fromAccountNumber = fromAccount.getAccountNumber();
        toAccountNumber = toAccount.getAccountNumber();
        fromInitialBalance = fromAccount.getBalance();
        toInitialBalance = toAccount.getBalance();

        mockAccounts = List.of(
                AccountDTO.builder()
                        .id(fromAccountId)
                        .accountNumber(fromAccountNumber)
                        .balance(fromInitialBalance)
                        .build(),
                AccountDTO.builder()
                        .id(toAccountId)
                        .accountNumber(toAccountNumber)
                        .balance(toInitialBalance)
                        .build()
        );

        BasePage.authAsUser(user);
        installTransferApiMocks(user);
    }

    private void installTransferApiMocks(CreateUserRequest user) {
        acceptOpenAlerts();
        long userId = DataBaseSteps.getUserByUsername(user.getUsername()).getId();
        CreateUserResponse uiUser = UiBrokenJsonMock.userWithAccounts(
                userId,
                user.getUsername(),
                TestDataConstants.DEFAULT_USER_NAME,
                "USER",
                mockAccounts
        );
        UiBrokenJsonMock.setCustomerAccountsOverride(mockAccounts);
        UiBrokenJsonMock.setAdminUsersOverride(List.of(uiUser));
    }

    private void acceptOpenAlerts() {
        try {
            var driver = com.codeborne.selenide.WebDriverRunner.getWebDriver();
            var wait = new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofMillis(300));
            org.openqa.selenium.Alert alert = wait.until(org.openqa.selenium.support.ui.ExpectedConditions.alertIsPresent());
            alert.accept();
        } catch (Exception ignored) {
            // no alert
        }
    }

    @Test
    @UserSession
    public void userCanTransferValidAmountTest() {
        openTransferWithMocks()
                .makeTransferByAccountNumber(fromAccountNumber, toAccountNumber, TestDataConstants.VALID_TRANSFER_AMOUNT)
                .checkAlertMessageAndAccept(BankAlert.TRANSFER_SUCCESSFULLY.getMessage());

        assertThat(UserSteps.getAccountBalance(userToken, fromAccountId))
                .isEqualTo(fromInitialBalance - TestDataConstants.VALID_TRANSFER_AMOUNT);
        assertThat(UserSteps.getAccountBalance(userToken, toAccountId))
                .isEqualTo(toInitialBalance + TestDataConstants.VALID_TRANSFER_AMOUNT);
    }

    @Test
    @UserSession
    public void userCanTransferMaxLimitAmountTest() {
        openTransferWithMocks()
                .makeTransferByAccountNumber(fromAccountNumber, toAccountNumber, TestDataConstants.MAX_TRANSFER_AMOUNT)
                .checkAlertMessageAndAccept(BankAlert.TRANSFER_SUCCESSFULLY.getMessage());

        assertThat(UserSteps.getAccountBalance(userToken, fromAccountId))
                .isEqualTo(fromInitialBalance - TestDataConstants.MAX_TRANSFER_AMOUNT);
        assertThat(UserSteps.getAccountBalance(userToken, toAccountId))
                .isEqualTo(toInitialBalance + TestDataConstants.MAX_TRANSFER_AMOUNT);
    }

    @Test
    @UserSession
    public void userCannotTransferAboveLimitTest() {
        openTransferWithMocks()
                .makeTransferByAccountNumber(fromAccountNumber, toAccountNumber, TestDataConstants.INVALID_TRANSFER_AMOUNT)
                .checkAlertMessageAndAccept(BankAlert.TRANSFER_LIMIT_EXCEEDED.getMessage());

        assertThat(UserSteps.getAccountBalance(userToken, fromAccountId)).isEqualTo(fromInitialBalance);
        assertThat(UserSteps.getAccountBalance(userToken, toAccountId)).isEqualTo(toInitialBalance);
    }

    @Test
    @UserSession
    public void userCannotTransferMoreThanBalanceTest() {
        double invalidAmount = fromInitialBalance + TestDataConstants.TRANSFER_AMOUNT_1000;

        openTransferWithMocks()
                .makeTransferByAccountNumber(fromAccountNumber, toAccountNumber, invalidAmount)
                .checkAlertMessageAndAccept(BankAlert.TRANSFER_AMOUNT_EXCEED.getMessage());

        assertThat(UserSteps.getAccountBalance(userToken, fromAccountId)).isEqualTo(fromInitialBalance);
        assertThat(UserSteps.getAccountBalance(userToken, toAccountId)).isEqualTo(toInitialBalance);
    }

    @Test
    @UserSession
    public void userCannotTransferWithoutFromAccountTest() {
        openTransferWithMocks();

        new TransferPage()
                .enterRecipientName(TestDataConstants.DEFAULT_USER_NAME)
                .enterRecipientAccount(toAccountNumber)
                .enterAmount(TestDataConstants.TRANSFER_AMOUNT_1000)
                .confirm()
                .send()
                .checkAlertMessageAndAccept(BankAlert.PLEASE_FILL_ALL_FIELDS.getMessage());

        assertThat(UserSteps.getAccountBalance(userToken, fromAccountId)).isEqualTo(fromInitialBalance);
        assertThat(UserSteps.getAccountBalance(userToken, toAccountId)).isEqualTo(toInitialBalance);
    }

    @Test
    @UserSession
    public void userCannotTransferWithoutConfirmationTest() {
        openTransferWithMocks()
                .selectFromAccount(TestDataConstants.FIRST_ACCOUNT_INDEX)
                .enterRecipientName(TestDataConstants.DEFAULT_USER_NAME)
                .enterRecipientAccount(toAccountNumber)
                .enterAmount(TestDataConstants.TRANSFER_AMOUNT_1000)
                .send()
                .checkAlertMessageAndAccept(BankAlert.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM.getMessage());

        assertThat(UserSteps.getAccountBalance(userToken, fromAccountId)).isEqualTo(fromInitialBalance);
        assertThat(UserSteps.getAccountBalance(userToken, toAccountId)).isEqualTo(toInitialBalance);
    }

    private TransferPage openTransferWithMocks() {
        CreateUserRequest user = SessionStorage.getUser();
        installTransferApiMocks(user);
        open("/transfer");
        acceptOpenAlerts();
        UiBrokenJsonMock.install();
        installTransferApiMocks(user);
        acceptOpenAlerts();
        $("input[placeholder='Enter recipient account number']").shouldBe(visible);
        return new TransferPage();
    }
}
