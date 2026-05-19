package iteration1.ui;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import iteration1.api.BaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.Alert;
import requests.steps.UserSteps;

import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;

public class BaseUiTest extends BaseTest {

    @BeforeAll
    public static void setupUi() {
        Configuration.browser = "chrome";
        Configuration.browserSize = "1920x1080";
        Configuration.baseUrl = "http://172.19.192.1:3000";
        Configuration.timeout = 10000;
        Configuration.headless = false;
        Configuration.screenshots = true;
        Configuration.savePageSource = true;
    }

    @BeforeEach
    public void openMainPage() {
        open("/");
    }

    @AfterEach
    public void tearDown() {
        Selenide.closeWebDriver();
    }

    protected void authThroughLocalStorage(String token) {
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", token);
        open("/dashboard");
        $(".welcome-text").shouldBe(Condition.visible);
    }

    protected void authThroughLocalStorage(String username, String password) {
        String token = UserSteps.loginAndGetToken(username, password);
        authThroughLocalStorage(token);
    }

    protected void loginThroughUi(String username, String password) {
        open("/login");
        $("input[placeholder='Username']").sendKeys(username);
        $("input[placeholder='Password']").sendKeys(password);
        $(".btn.btn-primary.w-100").click();
    }

    protected void loginAsAdmin() {
        loginThroughUi("admin", "admin");
        $(Selectors.byText("Admin Panel")).shouldBe(Condition.visible);
    }

    protected void assertAlertTextAndAccept(String expectedText) {
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains(expectedText);
        alert.accept();
    }

    protected String getAlertTextAndAccept() {
        Alert alert = switchTo().alert();
        String text = alert.getText();
        alert.accept();
        return text;
    }

    protected void assertAlertTextAndDismiss(String expectedText) {
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains(expectedText);
        alert.dismiss();
    }

    protected void clearLocalStorage() {
        executeJavaScript("localStorage.clear();");
    }

    protected void refreshPage() {
        Selenide.refresh();
    }

    protected void waitFor(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}