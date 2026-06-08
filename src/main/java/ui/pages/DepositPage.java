package ui.pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
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
        accountSelector.click();
        accountSelector.selectOption(index);
        return this;
    }

    public DepositPage enterAmount(double amount) {
        amountInput.setValue(String.valueOf(amount));
        return this;
    }

    public void clickDeposit() {
        depositButton.click();
    }

    public DepositPage makeDeposit(double amount, boolean selectAccount, int accountIndex) {
        if (selectAccount) {
            selectAccount(accountIndex);
        }
        enterAmount(amount);
        clickDeposit();
        return this;
    }
}