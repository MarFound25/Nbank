package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;
import models.CreateUserRequest;
import models.ProfileResponse;
import org.junit.jupiter.api.Assertions;
import requests.steps.UserSteps;
import common.storage.SessionStorage;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static org.assertj.core.api.Assertions.assertThat;

@Getter
public class ProfilePage extends BasePage<ProfilePage> {

    private SelenideElement profileHeader = $(".profile-header");
    private SelenideElement nameInput = $("input.form-control.mt-3");
    private SelenideElement saveButton = $("button.btn-primary.mt-3");

    private String userToken;
    private String originalName;

    @Override
    public String url() {
        return "/dashboard";
    }

    public ProfilePage openEditProfile() {
        profileHeader.click();
        $$("h1").get(1).shouldHave(Condition.text("Edit Profile"));
        return this;
    }

    public ProfilePage enterNewName(String newName) {
        nameInput.shouldBe(Condition.visible);
        nameInput.click();
        nameInput.clear();
        nameInput.setValue(newName);
        return this;
    }

    public ProfilePage clickSave() {
        saveButton.click();
        return this;
    }

    public ProfilePage changeName(String newName, String expectedAlertMessage) {
        openEditProfile();
        enterNewName(newName);
        clickSave();
        checkAlertMessageAndAccept(expectedAlertMessage);
        return this;
    }

    public ProfilePage openWithAuth() {
        CreateUserRequest user = SessionStorage.getUser();
        userToken = UserSteps.loginAndGetToken(user.getUsername(), user.getPassword());
        originalName = user.getName();
        authAsUser(user);
        open();
        return this;
    }

    public ProfilePage assertNameChanged(String expectedNewName) {
        Selenide.sleep(500);
        ProfileResponse profile = UserSteps.getProfile(userToken);
        Assertions.assertEquals(expectedNewName, profile.getName(),
                "Имя должно измениться на " + expectedNewName);
        originalName = expectedNewName;
        return this;
    }

    public ProfilePage assertNameNotChanged() {
        Selenide.sleep(500);
        ProfileResponse profile = UserSteps.getProfile(userToken);
        Assertions.assertEquals(originalName, profile.getName(),
                "Имя не должно измениться, ожидалось " + originalName);
        return this;
    }

    public String getOriginalName() {
        return originalName;
    }
}