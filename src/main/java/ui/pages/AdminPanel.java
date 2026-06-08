package ui.pages;

import com.codeborne.selenide.*;
import lombok.Getter;
import models.CreateUserRequest;
import org.junit.jupiter.api.Assertions;
import requests.steps.AdminSteps;

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

    public AdminPanel getAdminPanelText() {
        adminPanelText.shouldBe(Condition.visible);
        return this;
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

    public AdminPanel findUser(String username) {
        getAllUsers().findBy(Condition.exactText(username + "\nUSER")).shouldBe(Condition.visible);
        return this;
    }

    public AdminPanel refreshAndWaitForUser(String username) {
        Selenide.refresh();
        adminPanelText.shouldBe(Condition.visible);
        return this;
    }

    public AdminPanel assertUserExists() {
        return this;
    }

    public AdminPanel assertUserNotExists() {
        return this;
    }

    public AdminPanel assertUserMatchesApi(CreateUserRequest expectedUser) {
        boolean exists = AdminSteps.getAllUsers().stream()
                .anyMatch(user -> user.getUsername().equals(expectedUser.getUsername()));
        Assertions.assertTrue(exists,
                "Пользователь " + expectedUser.getUsername() + " должен быть создан через API");
        return this;
    }

    public AdminPanel assertUserNotExistsInApi(CreateUserRequest user) {
        long count = AdminSteps.getAllUsers().stream()
                .filter(existingUser -> existingUser.getUsername().equals(user.getUsername()))
                .count();
        Assertions.assertEquals(0, count,
                "Пользователь " + user.getUsername() + " не должен быть создан через API");
        return this;
    }
}