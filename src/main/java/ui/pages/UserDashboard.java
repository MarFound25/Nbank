package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

@Getter
public class UserDashboard extends BasePage<UserDashboard> {

    private SelenideElement welcomeText = $(Selectors.byClassName("welcome-text"));
    private SelenideElement createNewAccount = $$(".custom-btn.action-btn").last();

    @Override
    public String url() {
        return "/dashboard";
    }

    public UserDashboard createNewAccount() {
        createNewAccount.shouldBe(Condition.visible).click();
        Selenide.sleep(500);
        return this;
    }

    public DepositPage openDepositPage() {
        $$(".custom-btn.action-btn").first().click();
        return new DepositPage();
    }

    public TransferPage openTransferPage() {
        $$(".custom-btn.action-btn").get(1).click();
        $("input[placeholder='Enter recipient account number']").shouldBe(Condition.visible);
        return new TransferPage();
    }
}