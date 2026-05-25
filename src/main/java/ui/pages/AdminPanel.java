package ui.pages;

import com.codeborne.selenide.*;
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
        usernameInput.shouldBe(Condition.visible).sendKeys(username);
        passwordInput.shouldBe(Condition.visible).sendKeys(password);
        addUserButton.shouldBe(Condition.visible).click();
        return this;
    }

    public AdminPanel createUserAndAcceptAlert(String username, String password, String expectedMessage) {
        createUser(username, password);
        checkAlertMessageAndAccept(expectedMessage);
        return this;
    }

    public ElementsCollection getAllUsers() {
        return $(Selectors.byText("All Users")).parent().findAll("li");
    }

    public SelenideElement findUser(String username) {
        return getAllUsers().findBy(Condition.exactText(username + "\nUSER"));
    }

    public AdminPanel refreshAndWaitForUser(String username) {
        Selenide.refresh();
        adminPanelText.shouldBe(Condition.visible);
        return this;
    }
}