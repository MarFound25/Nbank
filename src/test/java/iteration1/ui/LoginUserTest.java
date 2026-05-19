package iteration1.ui;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import generators.RandomData;
import models.CreateUserRequest;
import models.UserRole;
import org.junit.jupiter.api.Test;
import requests.steps.AdminSteps;
import ui.pages.LoginPage;

import static com.codeborne.selenide.Selenide.$;

public class LoginUserTest extends BaseUiTest {

    private LoginPage loginPage = new LoginPage();

    @Test
    public void adminCanLoginWithCorrectDataTest() {
        loginPage.open()
                .login("admin", "admin");

        $(Selectors.byText("Admin Panel")).shouldBe(Condition.visible);
    }

    @Test
    public void userCanLoginWithCorrectDataTest() {
        String username = RandomData.getUsername();
        String password = RandomData.getPassword();

        CreateUserRequest createRequest = CreateUserRequest.builder()
                .username(username)
                .password(password)
                .name("Test User")
                .role(UserRole.USER.toString())
                .build();

        AdminSteps.createUser(createRequest);

        loginPage.open()
                .login(username, password);

        $(".welcome-text").shouldBe(Condition.visible);
    }
}