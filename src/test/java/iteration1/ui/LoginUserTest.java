package iteration1.ui;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import generators.RandomData;
import models.CreateUserRequest;
import models.UserRole;
import org.junit.jupiter.api.Test;
import requests.steps.AdminSteps;
import static com.codeborne.selenide.Selenide.$;

public class LoginUserTest extends UiTestBase {

    @Test
    public void adminCanLoginWithCorrectDataTest() {
        Selenide.open("/login");

        $(Selectors.byAttribute("placeholder", "Username")).sendKeys("admin");
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys("admin");
        $("button").click();

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

        Selenide.open("/login");

        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(username);
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(password);
        $("button").click();

        $(Selectors.byClassName("welcome-text")).shouldBe(Condition.visible)
                .shouldHave(Condition.text("Welcome, noname!"));
    }
}