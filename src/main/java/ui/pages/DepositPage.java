package ui.pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import common.helpers.StepLogger;
import lombok.Getter;

import static com.codeborne.selenide.Selenide.$;

@Getter
public class DepositPage extends BasePage<DepositPage> {

    private SelenideElement accountSelector = $(".account-selector");
    private SelenideElement amountInput = $(Selectors.byAttribute("placeholder", "Enter amount"));
    private SelenideElement depositButton = $(Selectors.byXpath("//button[contains(text(), 'Deposit')]"));

    @Override
    public String url() {
        return "/dashboard";
    }

    public DepositPage selectAccount(int index) {
        return StepLogger.log("Select deposit account index " + index, () -> {
            accountSelector.click();
            accountSelector.selectOption(index);
            return this;
        });
    }

    public DepositPage enterAmount(double amount) {
        return StepLogger.log("Enter deposit amount " + amount, () -> {
            amountInput.setValue(String.valueOf(amount));
            return this;
        });
    }

    public void clickDeposit() {
        StepLogger.log("Click Deposit button", () -> {
            depositButton.click();
            return null;
        });
    }

    public DepositPage makeDeposit(double amount, boolean shouldSelectAccount, int accountIndex) {
        return StepLogger.log("Make deposit amount=" + amount, () -> {
            if (shouldSelectAccount) {
                selectAccount(accountIndex);
            }
            enterAmount(amount);
            clickDeposit();
            return this;
        });
    }
}
