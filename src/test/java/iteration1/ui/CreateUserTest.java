package iteration1.ui;

import com.codeborne.selenide.*;
import generators.RandomData;
import models.CreateUserRequest;
import models.CreateUserResponse;
import models.UserRole;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import specs.RequestSpecs;
import java.util.Arrays;
import java.util.Map;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.switchTo;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CreateUserTest {
    @BeforeAll
    public static void setupSelenoid() {
        Configuration.remote = "http://localhost:4444/wd/hub";
        Configuration.baseUrl = "http://172.19.192.1:3000";
        Configuration.browser = "chrome";
        Configuration.browserSize = "1920x1080";

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true)
        );
    }

    @Test
    public void adminCanCreateUserTest() {
        Selenide.open("/login");

        $(Selectors.byAttribute("placeholder", "Username")).sendKeys("admin");
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys("admin");
        $("button").click();

        $(Selectors.byText("Admin Panel")).shouldBe(Condition.visible);

        CreateUserRequest newUser = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .name("Test User")
                .role(UserRole.USER.toString())
                .build();

        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(newUser.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(newUser.getPassword());
        $(Selectors.byText("Add User")).click();

        Alert alert = switchTo().alert();
        assertEquals("✅ User created successfully!", alert.getText());
        alert.accept();

        ElementsCollection allUsersFromDashboard = $(Selectors.byText("All Users")).parent().findAll("li");
        allUsersFromDashboard.findBy(Condition.exactText(newUser.getUsername() + "\nUSER")).shouldBe(Condition.visible);

        CreateUserResponse[] users = given()
                .spec(RequestSpecs.adminSpec())
                .get("http://localhost:4111/api/v1/admin/users")
                .then().assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract().as(CreateUserResponse[].class);

        CreateUserResponse createUser = Arrays.stream(users)
                .filter(user -> user.getUsername().equals(newUser.getUsername()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("User not found: " + newUser.getUsername()));

        assertThat(createUser.getUsername()).isEqualTo(newUser.getUsername());
        assertThat(createUser.getRole()).isEqualTo(UserRole.USER.toString());
    }

    @Test
    public void adminCannotCreateUserWithInvalidDataTest() {
        Selenide.open("/login");

        $(Selectors.byAttribute("placeholder", "Username")).sendKeys("admin");
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys("admin");
        $("button").click();

        $(Selectors.byText("Admin Panel")).shouldBe(Condition.visible);

        CreateUserRequest newUser = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .name("Test User")
                .role(UserRole.USER.toString())
                .build();

        newUser.setUsername("a");

        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(newUser.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(newUser.getPassword());
        $(Selectors.byText("Add User")).click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText().contains("Username must be between 3 and 15 characters"));
        alert.accept();

        ElementsCollection allUsersFromDashboard = $(Selectors.byText("All Users")).parent().findAll("li");
        allUsersFromDashboard.findBy(Condition.exactText(newUser.getUsername() + "\nUSER")).shouldNotBe(Condition.exist);

        CreateUserResponse[] users = given()
                .spec(RequestSpecs.adminSpec())
                .get("http://localhost:4111/api/v1/admin/users")
                .then().assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract().as(CreateUserResponse[].class);

        long usersWithSameUserNameAsNewUser = Arrays.stream(users)
                .filter(user -> user.getUsername().equals(newUser.getUsername()))
                .count();

        assertThat(usersWithSameUserNameAsNewUser).isZero();
    }
    }

