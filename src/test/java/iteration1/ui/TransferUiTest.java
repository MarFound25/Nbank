package iteration1.ui;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import generators.RandomData;
import models.Account;
import models.CreateUserRequest;
import models.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;

import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;

public class TransferUiTest extends UiTestBase {

    private String userToken;
    private int fromAccountId;
    private int toAccountId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private double fromInitialBalance;
    private double toInitialBalance;
    private String currentUserName = "Test User";

    @BeforeEach
    public void prepareData() {
        String username = RandomData.getUsername();
        String password = RandomData.getPassword();

        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .username(username)
                .password(password)
                .name(currentUserName)
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

        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userToken);
        Selenide.open("/dashboard");
        $(".welcome-text").shouldBe(Condition.visible);
    }

    private void openTransferPage() {
        $$(".custom-btn.action-btn").get(1).click();
        $("input[placeholder='Enter recipient account number']").shouldBe(Condition.visible);
    }


    @Test
    public void userCanTransferValidAmountTest() {
        double transferAmount = 3000.00;

        openTransferPage();

        $("select.account-selector").click();
        $("select.account-selector").selectOption(1);

        $("input[placeholder='Enter recipient name']").click();
        $("input[placeholder='Enter recipient name']").clear();
        $("input[placeholder='Enter recipient name']").setValue(currentUserName);

        $("input[placeholder='Enter recipient account number']").click();
        $("input[placeholder='Enter recipient account number']").clear();
        $("input[placeholder='Enter recipient account number']").setValue(toAccountNumber);

        $("input[placeholder='Enter amount']").click();
        $("input[placeholder='Enter amount']").clear();
        $("input[placeholder='Enter amount']").setValue(String.valueOf(transferAmount));

        $(Selectors.byText("Confirm details are correct")).click();
        $(Selectors.byXpath("//button[contains(text(), 'Send Transfer')]")).click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Successfully transferred");
        alert.accept();

        double newFromBalance = UserSteps.getAccountBalance(userToken, fromAccountId);
        double newToBalance = UserSteps.getAccountBalance(userToken, toAccountId);

        assertThat(newFromBalance).isEqualTo(fromInitialBalance - transferAmount);
        assertThat(newToBalance).isEqualTo(toInitialBalance + transferAmount);
    }

    @Test
    public void userCanTransferMaxLimitAmountTest() {
        double transferAmount = 10000.00;

        openTransferPage();

        $("select.account-selector").click();
        $("select.account-selector").selectOption(2);
        $("input[placeholder='Enter recipient name']").click();
        $("input[placeholder='Enter recipient name']").clear();
        $("input[placeholder='Enter recipient name']").setValue(currentUserName);
        $("input[placeholder='Enter recipient account number']").click();
        $("input[placeholder='Enter recipient account number']").clear();
        $("input[placeholder='Enter recipient account number']").setValue(toAccountNumber);
        $("input[placeholder='Enter amount']").click();
        $("input[placeholder='Enter amount']").clear();
        $("input[placeholder='Enter amount']").setValue(String.valueOf(transferAmount));
        $(Selectors.byText("Confirm details are correct")).click();
        $(Selectors.byXpath("//button[contains(text(), 'Send Transfer')]")).click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Successfully transferred");
        alert.accept();

        double newFromBalance = UserSteps.getAccountBalance(userToken, fromAccountId);
        double newToBalance = UserSteps.getAccountBalance(userToken, toAccountId);

        assertThat(newFromBalance).isEqualTo(fromInitialBalance - transferAmount);
        assertThat(newToBalance).isEqualTo(toInitialBalance + transferAmount);
    }

    @Test
    public void userCannotTransferAboveLimitTest() {
        double invalidAmount = 10001.00;

        openTransferPage();

        $("select.account-selector").click();
        $("select.account-selector").selectOption(1);
        $("input[placeholder='Enter recipient name']").click();
        $("input[placeholder='Enter recipient name']").clear();
        $("input[placeholder='Enter recipient name']").setValue(currentUserName);
        $("input[placeholder='Enter recipient account number']").click();
        $("input[placeholder='Enter recipient account number']").clear();
        $("input[placeholder='Enter recipient account number']").setValue(toAccountNumber);
        $("input[placeholder='Enter amount']").click();
        $("input[placeholder='Enter amount']").clear();
        $("input[placeholder='Enter amount']").setValue(String.valueOf(invalidAmount));
        $(Selectors.byText("Confirm details are correct")).click();
        $(Selectors.byXpath("//button[contains(text(), 'Send Transfer')]")).click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("cannot exceed 10000");
        alert.accept();

        double newFromBalance = UserSteps.getAccountBalance(userToken, fromAccountId);
        double newToBalance = UserSteps.getAccountBalance(userToken, toAccountId);

        assertThat(newFromBalance).isEqualTo(fromInitialBalance);
        assertThat(newToBalance).isEqualTo(toInitialBalance);
    }

    @Test
    public void userCannotTransferMoreThanBalanceTest() {
        double invalidAmount = fromInitialBalance + 1000;

        openTransferPage();

        $("select.account-selector").click();
        $("select.account-selector").selectOption(1);
        $("input[placeholder='Enter recipient name']").click();
        $("input[placeholder='Enter recipient name']").clear();
        $("input[placeholder='Enter recipient name']").setValue(currentUserName);
        $("input[placeholder='Enter recipient account number']").click();
        $("input[placeholder='Enter recipient account number']").clear();
        $("input[placeholder='Enter recipient account number']").setValue(toAccountNumber);
        $("input[placeholder='Enter amount']").click();
        $("input[placeholder='Enter amount']").clear();
        $("input[placeholder='Enter amount']").setValue(String.valueOf(invalidAmount));
        $(Selectors.byText("Confirm details are correct")).click();
        $(Selectors.byXpath("//button[contains(text(), 'Send Transfer')]")).click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Transfer amount cannot exceed");
        alert.accept();

        double newFromBalance = UserSteps.getAccountBalance(userToken, fromAccountId);
        double newToBalance = UserSteps.getAccountBalance(userToken, toAccountId);

        assertThat(newFromBalance).isEqualTo(fromInitialBalance);
        assertThat(newToBalance).isEqualTo(toInitialBalance);
    }
@Test
    public void userCannotTransferWithoutFromAccountTest() {
        openTransferPage();

        $("input[placeholder='Enter recipient name']").click();
        $("input[placeholder='Enter recipient name']").clear();
        $("input[placeholder='Enter recipient name']").setValue(currentUserName);
        $("input[placeholder='Enter recipient account number']").click();
        $("input[placeholder='Enter recipient account number']").clear();
        $("input[placeholder='Enter recipient account number']").setValue(toAccountNumber);
        $("input[placeholder='Enter amount']").click();
        $("input[placeholder='Enter amount']").clear();
        $("input[placeholder='Enter amount']").setValue("1000");
        $(Selectors.byText("Confirm details are correct")).click();
        $(Selectors.byXpath("//button[contains(text(), 'Send Transfer')]")).click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Please fill all fields");
        alert.accept();

        double newFromBalance = UserSteps.getAccountBalance(userToken, fromAccountId);
        double newToBalance = UserSteps.getAccountBalance(userToken, toAccountId);

        assertThat(newFromBalance).isEqualTo(fromInitialBalance);
        assertThat(newToBalance).isEqualTo(toInitialBalance);
    }

    @Test
    public void userCannotTransferWithoutConfirmationTest() {
        openTransferPage();

        $("select.account-selector").click();
        $("select.account-selector").selectOption(1);
        $("input[placeholder='Enter recipient name']").click();
        $("input[placeholder='Enter recipient name']").clear();
        $("input[placeholder='Enter recipient name']").setValue(currentUserName);
        $("input[placeholder='Enter recipient account number']").click();
        $("input[placeholder='Enter recipient account number']").clear();
        $("input[placeholder='Enter recipient account number']").setValue(toAccountNumber);
        $("input[placeholder='Enter amount']").click();
        $("input[placeholder='Enter amount']").clear();
        $("input[placeholder='Enter amount']").setValue("1000");


        SelenideElement sendButton = $(Selectors.byXpath("//button[contains(text(), 'Send Transfer')]"));
        sendButton.click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Please fill all fields and confirm");
        alert.accept();

        double newFromBalance = UserSteps.getAccountBalance(userToken, fromAccountId);
        double newToBalance = UserSteps.getAccountBalance(userToken, toAccountId);

        assertThat(newFromBalance).isEqualTo(fromInitialBalance);
        assertThat(newToBalance).isEqualTo(toInitialBalance);
    }
}