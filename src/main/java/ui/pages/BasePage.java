package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import models.CreateAccountResponse;
import models.CreateUserRequest;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.Alert;
import requests.steps.UserSteps;

import java.util.List;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.executeJavaScript;
import static com.codeborne.selenide.Selenide.switchTo;

public abstract class BasePage<T extends BasePage<T>> {
    protected SelenideElement usernameInput = $(Selectors.byAttribute("placeholder", "Username"));
    protected SelenideElement passwordInput = $(Selectors.byAttribute("placeholder", "Password"));

    protected List<CreateAccountResponse> createdAccounts;

    public static void authAsUser(CreateUserRequest user) {
        String token = UserSteps.loginAndGetToken(user.getUsername(), user.getPassword());
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", token);

        if (user.getRole().equals("ADMIN")) {
            Selenide.open("/admin");
            $("h1").shouldBe(Condition.visible);
        } else {
            Selenide.open("/dashboard");
            $(".welcome-text").shouldBe(Condition.visible);
        }
    }

    public abstract String url();

    public T open() {
        return Selenide.open(url(), (Class<T>) this.getClass());
    }

    public <P extends BasePage> P getPage(Class<P> pageClass) {
        return Selenide.page(pageClass);
    }

    public T checkAlertMessageAndAccept(String bankAlert) {
        Alert alert = switchTo().alert();
        String actualText = alert.getText();
        if (!actualText.contains(bankAlert)) {
            throw new AssertionError("Expected alert to contain: '" + bankAlert + "', but was: '" + actualText + "'");
        }
        alert.accept();
        return (T) this;
    }

    public T assertThat() {
        return (T) this;
    }

    public T and() {
        return (T) this;
    }

    public T withCreatedAccounts(List<CreateAccountResponse> accounts) {
        this.createdAccounts = accounts;
        return (T) this;
    }

    public T assertAccountSize(int expectedSize) {
        Assertions.assertEquals(expectedSize, createdAccounts.size(),
                "Должен быть создан ровно " + expectedSize + " счет(ов)");
        return (T) this;
    }

    public T assertBalanceIsZero() {
        Assertions.assertEquals(0, createdAccounts.getFirst().getBalance(),
                "Баланс нового счета должен быть 0");
        return (T) this;
    }

    public String getFirstAccountNumber() {
        return createdAccounts.getFirst().getAccountNumber();
    }
}