package iteration1.ui;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import generators.RandomData;
import models.CreateUserRequest;
import models.ProfileResponse;
import models.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;

import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;

public class ProfileUiTest extends UiTestBase {

    private String userToken;
    private String originalName = "Old Name";

    @BeforeEach
    public void prepareData() {
        String username = RandomData.getUsername();
        String password = RandomData.getPassword();

        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .username(username)
                .password(password)
                .name(originalName)
                .role(UserRole.USER.toString())
                .build();

        AdminSteps.createUser(createUserRequest);
        userToken = UserSteps.loginAndGetToken(username, password);

        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userToken);
        Selenide.open("/dashboard");

        $(Selectors.byClassName("welcome-text")).shouldBe(Condition.visible);
    }

    private void openEditProfile() {
        $(".profile-header").click();
        $$("h1").get(1).shouldHave(Condition.text("Edit Profile"));
    }

    @Test
    public void userCanChangeNameValidTest() {
        String newName = originalName + "a";

        openEditProfile();

        SelenideElement nameInput = $("input.form-control.mt-3");
        nameInput.shouldBe(Condition.visible);
        nameInput.click();
        nameInput.clear();
        nameInput.setValue(newName);

        $("button.btn-primary.mt-3").click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("success");
        alert.accept();

        ProfileResponse profile = UserSteps.getProfile(userToken);
        assertThat(profile.getName()).isEqualTo(newName);
    }

    @Test
    public void userCannotChangeToSingleWordTest() {
        String invalidName = "John";

        openEditProfile();

        SelenideElement nameInput = $("input.form-control.mt-3");
        nameInput.shouldBe(Condition.visible);
        nameInput.click();
        nameInput.clear();
        nameInput.setValue(invalidName);

        $("button.btn-primary.mt-3").click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("two words");
        alert.accept();

        ProfileResponse profile = UserSteps.getProfile(userToken);
        assertThat(profile.getName()).isEqualTo(originalName);
    }

    @Test
    public void userCannotChangeNameWithDigitsTest() {
        String invalidName = "John 123";

        openEditProfile();

        SelenideElement nameInput = $("input.form-control.mt-3");
        nameInput.shouldBe(Condition.visible);
        nameInput.click();
        nameInput.clear();
        nameInput.setValue(invalidName);

        $("button.btn-primary.mt-3").click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("letters");
        alert.accept();

        ProfileResponse profile = UserSteps.getProfile(userToken);
        assertThat(profile.getName()).isEqualTo(originalName);
    }

    @Test
    public void userCannotChangeNameWithSpecialCharsTest() {
        String invalidName = "John@ Doe";

        openEditProfile();

        SelenideElement nameInput = $("input.form-control.mt-3");
        nameInput.shouldBe(Condition.visible);
        nameInput.click();
        nameInput.clear();
        nameInput.setValue(invalidName);

        $("button.btn-primary.mt-3").click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("letters");
        alert.accept();

        ProfileResponse profile = UserSteps.getProfile(userToken);
        assertThat(profile.getName()).isEqualTo(originalName);
    }

    @Test
    public void userCannotChangeToEmptyNameTest() {
        String invalidName = "";

        openEditProfile();

        SelenideElement nameInput = $("input.form-control.mt-3");
        nameInput.shouldBe(Condition.visible);
        nameInput.click();
        nameInput.clear();
        nameInput.setValue(invalidName);

        $("button.btn-primary.mt-3").click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("⚠\uFE0F New name is the same as the current one.");
        alert.accept();

        ProfileResponse profile = UserSteps.getProfile(userToken);
        assertThat(profile.getName()).isEqualTo(originalName);
    }

    @Test
    public void userNameShouldBeTrimmedTest() {
        String inputName = "  Anna Smith  ";
        String expectedName = "Anna Smith";

        openEditProfile();

        SelenideElement nameInput = $("input.form-control.mt-3");
        nameInput.shouldBe(Condition.visible);
        nameInput.click();
        nameInput.clear();
        nameInput.setValue(expectedName);

        $("button.btn-primary.mt-3").click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("✅ Name updated successfully!");
        alert.accept();

        ProfileResponse profile = UserSteps.getProfile(userToken);
        assertThat(profile.getName()).isEqualTo(expectedName);
    }
}