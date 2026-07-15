package common.extensions;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import common.annotations.AdminSession;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import ui.pages.LoginPage;

import static com.codeborne.selenide.Selenide.$;

public class AdminSessionExtension implements BeforeEachCallback {
    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        AdminSession annotation = extensionContext.getRequiredTestMethod().getAnnotation(AdminSession.class);
        if (annotation == null) {
            return;
        }

        new LoginPage().open()
                .login("admin", "admin");

        $(Selectors.byText("Admin Panel")).shouldBe(Condition.visible);
    }
}
