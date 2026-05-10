package iteration1.ui;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import generators.RandomData;
import models.CreateUserRequest;
import models.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;

import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;

public class DepositUiTest extends UiTestBase {

    private String userToken;
    private int accountId;
    private double initialBalance;

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

        accountId = UserSteps.createAccount(userToken);
        initialBalance = UserSteps.getAccountBalance(userToken, accountId);

        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userToken);
        Selenide.open("/dashboard");
    }

    private void makeDeposit(double amount, boolean selectAccount) {
        $$(".custom-btn.action-btn").first().click();

        if (selectAccount) {
            $(".account-selector").click();
            $(".account-selector option").shouldBe(Condition.visible);
            $(".account-selector").selectOption(1);
        }

        $(Selectors.byAttribute("placeholder", "Enter amount")).setValue(String.valueOf(amount));
        $(Selectors.byXpath("//button[contains(text(), 'Deposit')]")).click();
    }

    @Test
    public void userCanDepositValidAmountTest() {
        double depositAmount = 1000.00;

        makeDeposit(depositAmount, true);

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Successfully deposited");
        alert.accept();

        double newBalance = UserSteps.getAccountBalance(userToken, accountId);
        assertThat(newBalance).isEqualTo(initialBalance + depositAmount);
    }

    @Test
    public void userCanDepositMaxLimitAmountTest() {
        double depositAmount = 5000.00;

        makeDeposit(depositAmount, true);

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Successfully deposited");
        alert.accept();

        double newBalance = UserSteps.getAccountBalance(userToken, accountId);
        assertThat(newBalance).isEqualTo(initialBalance + depositAmount);
    }

    @Test
    public void userCannotDepositAboveLimitTest() {
        double invalidAmount = 5001.00;

        makeDeposit(invalidAmount, true);

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("❌ Please deposit less or equal to 5000$.");
        alert.accept();

        double newBalance = UserSteps.getAccountBalance(userToken, accountId);
        assertThat(newBalance).isEqualTo(initialBalance);
    }

    @Test
    public void userCannotDepositWithoutAccountTest() {
        makeDeposit(1000.00, false);

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("select an account");
        alert.accept();

        double newBalance = UserSteps.getAccountBalance(userToken, accountId);
        assertThat(newBalance).isEqualTo(initialBalance);
    }
}