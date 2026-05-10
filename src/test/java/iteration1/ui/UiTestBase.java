package iteration1.ui;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import iteration1.api.BaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import static com.codeborne.selenide.Selenide.open;

public class UiTestBase extends BaseTest {

    @BeforeAll
    public static void setupUi() {
        Configuration.browser = "chrome";
        Configuration.browserSize = "1920x1080";
        Configuration.baseUrl = "http://172.19.192.1:3000";
        Configuration.timeout = 10000;
        Configuration.headless = false;

    }

    @BeforeEach
    public void openMainPage() {
        open("/");
    }

    @AfterEach
    public void tearDown() {
        Selenide.closeWebDriver();
    }
}