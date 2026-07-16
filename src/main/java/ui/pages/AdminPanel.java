package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import common.helpers.StepLogger;
import lombok.Getter;

import static com.codeborne.selenide.Selenide.$;

@Getter
public class AdminPanel extends BasePage<AdminPanel> {

    private SelenideElement adminPanelText = $(Selectors.byText("Admin Panel"));
    private SelenideElement addUserButton = $(".btn.btn-primary.w-100");
    private SelenideElement usernameInput = $(Selectors.byAttribute("placeholder", "Username"));
    private SelenideElement passwordInput = $(Selectors.byAttribute("placeholder", "Password"));

    @Override
    public String url() {
        return "/admin";
    }

    public AdminPanel createUser(String username, String password) {
        return StepLogger.log("Create user " + username + " via Admin Panel", () -> {
            adminPanelText.shouldBe(Condition.visible);
            if (!usernameInput.is(Condition.visible)) {
                addUserButton.shouldBe(Condition.visible).click();
            }
            usernameInput.shouldBe(Condition.visible).clear();
            usernameInput.sendKeys(username);
            passwordInput.shouldBe(Condition.visible).clear();
            passwordInput.sendKeys(password);
            addUserButton.shouldBe(Condition.visible).click();
            return this;
        });
    }

    public AdminPanel createUserAndAcceptAlert(String username, String password, String expectedMessage) {
        return StepLogger.log("Create user " + username + " and accept alert", () -> {
            createUser(username, password);
            checkAlertMessageAndAccept(expectedMessage);
            return this;
        });
    }

    public ElementsCollection getAllUsers() {
        return StepLogger.log("Get all users from Admin Panel", () ->
                $(Selectors.byText("All Users")).parent().findAll("li"));
    }

    public SelenideElement findUser(String username) {
        return StepLogger.log("Find user " + username + " on Admin Panel", () ->
                getAllUsers().findBy(Condition.text(username)));
    }

    public AdminPanel refreshAndWaitForUser(String username) {
        return StepLogger.log("Refresh Admin Panel and wait for user " + username, () -> {
            Selenide.refresh();
            common.helpers.UiBrokenJsonMock.install();
            adminPanelText.shouldBe(Condition.visible);
            return this;
        });
    }

    public AdminPanel waitUntilVisible() {
        return StepLogger.log("Wait for Admin Panel", () -> {
            adminPanelText.shouldBe(Condition.visible);
            return this;
        });
    }
}
