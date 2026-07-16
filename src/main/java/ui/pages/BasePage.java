package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import common.helpers.StepLogger;
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
        StepLogger.log("Auth as user " + user.getUsername() + " via UI localStorage", () -> {
            String token = UserSteps.loginAndGetToken(user.getUsername(), user.getPassword());
            // Full navigation after setting token so React restores auth.role from Basic token.
            Selenide.open("/login");
            common.helpers.UiBrokenJsonMock.install();
            executeJavaScript("localStorage.setItem('authToken', arguments[0]);", token);

            if (user.getRole().equals("ADMIN")) {
                Selenide.open("/admin");
                common.helpers.UiBrokenJsonMock.install();
                $(Selectors.byText("Admin Panel")).shouldBe(Condition.visible);
            } else {
                Selenide.open("/dashboard");
                common.helpers.UiBrokenJsonMock.install();
                $(".welcome-text").shouldBe(Condition.visible);
            }
            return null;
        });
    }

    public abstract String url();

    public T open() {
        T page = Selenide.open(url(), (Class<T>) this.getClass());
        common.helpers.UiBrokenJsonMock.install();
        return page;
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
