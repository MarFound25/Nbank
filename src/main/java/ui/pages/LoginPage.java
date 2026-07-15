package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class LoginPage extends BasePage<LoginPage> {

    private SelenideElement button = $("button");

    @Override
    public String url() {
        return "/login";
    }

    public LoginPage login(String username, String password) {
        usernameInput.shouldBe(Condition.visible).clear();
        usernameInput.sendKeys(username);
        passwordInput.shouldBe(Condition.visible).clear();
        passwordInput.sendKeys(password);
        button.shouldBe(Condition.visible).click();
        return this;
    }
}
