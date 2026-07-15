package common.extensions;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import generators.RandomData;
import models.CreateUserRequest;
import models.UserRole;
import requests.steps.AdminSteps;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import ui.pages.BasePage;

import java.util.LinkedList;
import java.util.List;

public class UserSessionExtension implements BeforeEachCallback {
    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        UserSession annotation = extensionContext.getRequiredTestMethod().getAnnotation(UserSession.class);
        if (annotation != null) {
            if (!WebDriverRunner.hasWebDriverStarted()) {
                Configuration.browser = "chrome";
                Configuration.browserSize = "1920x1080";
                Configuration.baseUrl = "http://localhost:3000";
                Configuration.timeout = 10000;
                Configuration.headless = false;
                Selenide.open("/");
            }

            int userCount = annotation.value();

            SessionStorage.clear();

            List<CreateUserRequest> users = new LinkedList<>();

            for (int i = 0; i < userCount; i++) {
                CreateUserRequest user = CreateUserRequest.builder()
                        .username(RandomData.getUsername())
                        .password(RandomData.getPassword())
                        .name("Test User")
                        .role(UserRole.USER.toString())
                        .build();

                AdminSteps.createUser(user);
                users.add(user);
            }

            SessionStorage.addUsers(users);

            int authAsUser = annotation.auth();

            BasePage.authAsUser(SessionStorage.getUser(authAsUser));
        }
    }
}
