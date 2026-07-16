package iteration1.ui;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.logevents.SelenideLogger;
import common.extensions.BrowserMatchExtension;
import iteration1.api.BaseTest;
import io.qameta.allure.selenide.AllureSelenide;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.Alert;
import common.extensions.AdminSessionExtension;
import common.extensions.UserSessionExtension;

import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({AdminSessionExtension.class, UserSessionExtension.class, BrowserMatchExtension.class})
public class BaseUiTest extends BaseTest {

    @BeforeAll
    public static void setupUi() {
        Configuration.browser = "chrome";
        Configuration.browserSize = "1920x1080";
        Configuration.baseUrl = "http://localhost:3000";
        Configuration.timeout = 10000;
        Configuration.headless = true;

        SelenideLogger.addListener("AllureSelenide", new AllureSelenide()
                .screenshots(true)
                .savePageSource(true)
                .includeSelenideSteps(true));
    }

    @BeforeEach
    public void openMainPage() {
        // Ensure browser is up so JSON sanitizer can be installed for SPA XHR hooks.
        if (!com.codeborne.selenide.WebDriverRunner.hasWebDriverStarted()) {
            open("/login");
        }
        common.helpers.UiBrokenJsonMock.install();
    }

    @AfterEach
    public void tearDown() {
        Selenide.closeWebDriver();
        common.helpers.UiBrokenJsonMock.resetCdpFlag();
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
}
