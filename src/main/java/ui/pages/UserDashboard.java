package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;
import models.Account;
import models.CreateAccountResponse;
import models.CreateUserRequest;
import org.junit.jupiter.api.Assertions;
import requests.steps.UserSteps;
import common.storage.SessionStorage;

import java.util.List;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

@Getter
public class UserDashboard extends BasePage<UserDashboard> {

    private SelenideElement welcomeText = $(Selectors.byClassName("welcome-text"));
    private SelenideElement createNewAccount = $$(".custom-btn.action-btn").last();

    private String userToken;
    private int accountId;
    private double currentBalance;
    private int fromAccountId;
    private int toAccountId;
    private double fromInitialBalance;
    private double toInitialBalance;

    @Override
    public String url() {
        return "/dashboard";
    }

    public UserDashboard getWelcomeText() {
        welcomeText.shouldBe(Condition.visible);
        return this;
    }

    public UserDashboard welcomeTextShouldHave(String expectedText) {
        welcomeText.shouldBe(Condition.visible);
        welcomeText.shouldHave(Condition.text(expectedText));
        return this;
    }

    public UserDashboard checkAlertWithAccountNumber() {
        String accountNumber = getFirstAccountNumber();
        checkAlertMessageAndAccept(BankAlert.NEW_ACCOUNT_CREATED.getMessage() + accountNumber);
        return this;
    }

    public UserDashboard createNewAccount() {
        createNewAccount.shouldBe(Condition.visible).click();
        Selenide.sleep(500);
        return this;
    }

    public DepositPage openDepositPage() {
        $$(".custom-btn.action-btn").first().click();
        return new DepositPage();
    }

    public TransferPage openTransferPage() {
        $$(".custom-btn.action-btn").get(1).click();
        $("input[placeholder='Enter recipient account number']").shouldBe(Condition.visible);
        return new TransferPage();
    }


    public UserDashboard openWithPreparedAccount() {
        CreateUserRequest user = SessionStorage.getUser();
        userToken = UserSteps.loginAndGetToken(user.getUsername(), user.getPassword());
        accountId = UserSteps.createAccount(userToken);
        currentBalance = UserSteps.getAccountBalance(userToken, accountId);
        authAsUser(user);
        open();
        return this;
    }

    public UserDashboard makeDeposit(double amount, boolean withAccount) {
        openDepositPage();
        if (withAccount) {
            $("select.form-select").selectOption(0);
        }
        $("#amount").setValue(String.valueOf(amount));
        $("button[type='submit']").click();
        return this;
    }

    public UserDashboard assertBalanceIncreasedBy(double depositedAmount) {
        Selenide.sleep(500);
        double newBalance = UserSteps.getAccountBalance(userToken, accountId);
        Assertions.assertEquals(currentBalance + depositedAmount, newBalance,
                "Баланс должен увеличиться на " + depositedAmount);
        currentBalance = newBalance;
        return this;
    }

    public UserDashboard assertBalanceNotChanged() {
        Selenide.sleep(500);
        double newBalance = UserSteps.getAccountBalance(userToken, accountId);
        Assertions.assertEquals(currentBalance, newBalance,
                "Баланс не должен измениться");
        return this;
    }


    public UserDashboard openWithTwoPreparedAccounts() {
        CreateUserRequest user = SessionStorage.getUser();
        userToken = UserSteps.loginAndGetToken(user.getUsername(), user.getPassword());

        fromAccountId = UserSteps.createAccount(userToken);
        toAccountId = UserSteps.createAccount(userToken);

        for (int i = 0; i < 3; i++) {
            UserSteps.deposit(userToken, fromAccountId, 10000);
        }

        List<Account> accounts = UserSteps.getAccounts(userToken);
        for (Account account : accounts) {
            if (account.getId() == fromAccountId) {
                fromInitialBalance = account.getBalance();
            }
            if (account.getId() == toAccountId) {
                toInitialBalance = account.getBalance();
            }
        }

        authAsUser(user);
        open();
        return this;
    }

    public UserDashboard makeTransfer(int fromIndex, double amount) {
        openTransferPage();
        $("select.form-select").selectOption(fromIndex);
        $("input[placeholder='Enter recipient account number']").setValue(String.valueOf(toAccountId));
        $("input[placeholder='Enter amount']").setValue(String.valueOf(amount));
        $("input[placeholder='Enter recipient name']").setValue("Test Recipient");
        $("button[type='submit']").click();
        return this;
    }

    public UserDashboard makeTransferWithoutFromAccount(String recipientName, double amount) {
        openTransferPage();
        $("input[placeholder='Enter recipient account number']").setValue(String.valueOf(toAccountId));
        $("input[placeholder='Enter amount']").setValue(String.valueOf(amount));
        $("input[placeholder='Enter recipient name']").setValue(recipientName);
        $("button[type='submit']").click();
        return this;
    }

    public UserDashboard makeTransferWithoutConfirmation(int fromIndex, String recipientName, double amount) {
        openTransferPage();
        $("select.form-select").selectOption(fromIndex);
        $("input[placeholder='Enter recipient account number']").setValue(String.valueOf(toAccountId));
        $("input[placeholder='Enter amount']").setValue(String.valueOf(amount));
        $("input[placeholder='Enter recipient name']").setValue(recipientName);
        return this;
    }

    public UserDashboard assertTransferCompleted(double transferredAmount) {
        Selenide.sleep(500);
        double newFromBalance = UserSteps.getAccountBalance(userToken, fromAccountId);
        double newToBalance = UserSteps.getAccountBalance(userToken, toAccountId);

        Assertions.assertEquals(fromInitialBalance - transferredAmount, newFromBalance,
                "Баланс отправителя должен уменьшиться на " + transferredAmount);
        Assertions.assertEquals(toInitialBalance + transferredAmount, newToBalance,
                "Баланс получателя должен увеличиться на " + transferredAmount);

        fromInitialBalance = newFromBalance;
        toInitialBalance = newToBalance;
        return this;
    }

    public UserDashboard assertTransferNotCompleted() {
        Selenide.sleep(500);
        double newFromBalance = UserSteps.getAccountBalance(userToken, fromAccountId);
        double newToBalance = UserSteps.getAccountBalance(userToken, toAccountId);

        Assertions.assertEquals(fromInitialBalance, newFromBalance,
                "Баланс отправителя не должен измениться");
        Assertions.assertEquals(toInitialBalance, newToBalance,
                "Баланс получателя не должен измениться");
        return this;
    }

    public String getSecondAccountNumber() {
        return String.valueOf(toAccountId);
    }


    public UserDashboard assertAccountSize(int expectedSize) {
        List<CreateAccountResponse> accounts = SessionStorage.getSteps().getAllAccounts();
        Assertions.assertEquals(expectedSize, accounts.size(),
                "Должен быть создан ровно " + expectedSize + " счет(ов)");
        return this;
    }

    public UserDashboard assertBalanceIsZero() {
        List<CreateAccountResponse> accounts = SessionStorage.getSteps().getAllAccounts();
        Assertions.assertEquals(0, accounts.getFirst().getBalance(),
                "Баланс нового счета должен быть 0");
        return this;
    }

    public String getFirstAccountNumber() {
        List<CreateAccountResponse> accounts = SessionStorage.getSteps().getAllAccounts();
        return accounts.getFirst().getAccountNumber();
    }
}