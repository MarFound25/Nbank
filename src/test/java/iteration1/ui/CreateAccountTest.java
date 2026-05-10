package iteration1.ui;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.*;
import generators.RandomData;
import models.*;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;
import specs.RequestSpecs;
import java.util.Map;

import static com.codeborne.selenide.Selenide.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreateAccountTest {
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
    public void userCanCreateAccountTest() {
        String username = RandomData.getUsername();
        String password = RandomData.getPassword();

        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .username(username)
                .password(password)
                .name("Test User")
                .role(UserRole.USER.toString())
                .build();

        AdminSteps.createUser(createUserRequest);

        String token = UserSteps.loginAndGetToken(username, password);

        Selenide.open("/");
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", token);
        Selenide.open("/dashboard");

        $$(".custom-btn.action-btn").last().click();

        Alert alert = switchTo().alert();
        String alertText = alert.getText();

        assertThat(alertText).contains("New Account Created! Account Number:");
        alert.accept();

        Pattern pattern = Pattern.compile("Account Number: (\\w+)");
        Matcher matcher = pattern.matcher(alertText);
        matcher.find();
        String createdAccNumber = matcher.group(1);
        assertThat(createdAccNumber).isNotNull();

        Account[] existingUserAccounts = given()
                .spec(RequestSpecs.authWithToken(token))
                .get(endpoints.Endpoint.CUSTOMER_ACCOUNTS)
                .then().assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract().as(Account[].class);

        assertThat(existingUserAccounts).hasSize(1);

        Account createdAccount = existingUserAccounts[0];

        assertThat(createdAccount).isNotNull();
        assertThat(createdAccount.getBalance()).isEqualTo(0.0);
    }
}
