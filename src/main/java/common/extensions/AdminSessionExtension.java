package common.extensions;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import models.CreateUserRequest;
import models.UserRole;
import common.annotations.AdminSession;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import ui.pages.BasePage;

public class AdminSessionExtension implements BeforeEachCallback {
    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        AdminSession annotation = extensionContext.getRequiredTestMethod().getAnnotation(AdminSession.class);
        if (annotation != null) {
            if (!WebDriverRunner.hasWebDriverStarted()) {
                Configuration.browser = "chrome";
                Configuration.browserSize = "1920x1080";
                Configuration.baseUrl = "http://localhost:3000";
                Configuration.timeout = 10000;
                Configuration.headless = false;
                Selenide.open("/");
            }

            CreateUserRequest admin = CreateUserRequest.builder()
                    .username("admin")
                    .password("admin")
                    .name("Admin")
                    .role(UserRole.ADMIN.toString())
                    .build();

            BasePage.authAsUser(admin);
        }
    }
}