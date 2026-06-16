package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import models.CreateUserRequest;
import org.openqa.selenium.Alert;
import requests.steps.UserSteps;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.executeJavaScript;
import static com.codeborne.selenide.Selenide.switchTo;

public abstract class BasePage<T extends BasePage> {
    protected SelenideElement usernameInput = $(Selectors.byAttribute("placeholder", "Username"));
    protected SelenideElement passwordInput = $(Selectors.byAttribute("placeholder", "Password"));

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

    public <T extends BasePage> T getPage(Class<T> pageClass) {
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
}