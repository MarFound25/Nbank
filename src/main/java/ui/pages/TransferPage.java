package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

@Getter
public class TransferPage extends BasePage<TransferPage> {

    private SelenideElement accountSelector = $("select.account-selector");
    private SelenideElement recipientNameInput = $("input[placeholder='Enter recipient name']");
    private SelenideElement recipientAccountInput = $("input[placeholder='Enter recipient account number']");
    private SelenideElement amountInput = $("input[placeholder='Enter amount']");
    private SelenideElement confirmButton = $(Selectors.byText("Confirm details are correct"));
    private SelenideElement sendButton = $(Selectors.byXpath("//button[contains(text(), 'Send Transfer')]"));

    @Override
    public String url() {
        return "/dashboard";
    }

    public TransferPage selectFromAccount(int index) {
        accountSelector.shouldBe(Condition.visible);
        accountSelector.click();
        $$("select.account-selector option").shouldHave(size(3));
        accountSelector.selectOption(index);
        return this;
    }

    public TransferPage enterRecipientName(String name) {
        recipientNameInput.shouldBe(Condition.visible);
        recipientNameInput.click();
        recipientNameInput.clear();
        recipientNameInput.setValue(name);
        return this;
    }

    public TransferPage enterRecipientAccount(String accountNumber) {
        recipientAccountInput.shouldBe(Condition.visible);
        recipientAccountInput.click();
        recipientAccountInput.clear();
        recipientAccountInput.setValue(accountNumber);
        return this;
    }

    public TransferPage enterAmount(double amount) {
        amountInput.shouldBe(Condition.visible);
        amountInput.click();
        amountInput.clear();
        amountInput.setValue(String.valueOf(amount));
        return this;
    }

    public TransferPage confirm() {
        confirmButton.click();
        return this;
    }

    public TransferPage send() {
        sendButton.click();
        return this;
    }

    public TransferPage makeTransfer(int fromAccountIndex, String toAccount, double amount) {
        selectFromAccount(fromAccountIndex);
        enterRecipientName("Test User");
        enterRecipientAccount(toAccount);
        enterAmount(amount);
        confirm();
        send();
        return this;
    }
}