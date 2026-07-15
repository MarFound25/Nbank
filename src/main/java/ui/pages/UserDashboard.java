package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import common.helpers.StepLogger;
import lombok.Getter;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

@Getter
public class UserDashboard extends BasePage<UserDashboard> {

    private SelenideElement welcomeText = $(Selectors.byClassName("welcome-text"));
    private SelenideElement createNewAccountButton = $$(".custom-btn.action-btn").last();

    @Override
    public String url() {
        return "/dashboard";
    }

    public UserDashboard createNewAccount() {
        return StepLogger.log("Create new account from Dashboard", () -> {
            createNewAccountButton.shouldBe(Condition.visible).click();
            Selenide.sleep(500);
            return this;
        });
    }

    public DepositPage openDepositPage() {
        return StepLogger.log("Open Deposit page from Dashboard", () -> {
            $$(".custom-btn.action-btn").shouldHave(sizeGreaterThan(0));
            $$(".custom-btn.action-btn").first().shouldBe(Condition.visible).click();
            return new DepositPage();
        });
    }

    public TransferPage openTransferPage() {
        return StepLogger.log("Open Transfer page from Dashboard", () -> {
            $(".welcome-text").shouldBe(Condition.visible);
            $$(".custom-btn.action-btn").shouldHave(sizeGreaterThan(1));
            $$(".custom-btn.action-btn").get(1).shouldBe(Condition.visible).click();
            $("input[placeholder='Enter recipient account number']").shouldBe(Condition.visible);
            return new TransferPage();
        });
    }
}
