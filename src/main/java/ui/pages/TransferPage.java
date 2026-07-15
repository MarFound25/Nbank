package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import common.helpers.StepLogger;
import lombok.Getter;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
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
        return StepLogger.log("Select transfer from-account index " + index, () -> {
            accountSelector.shouldBe(Condition.visible);
            accountSelector.click();
            $$("select.account-selector option").shouldHave(sizeGreaterThan(1));
            accountSelector.selectOption(index);
            return this;
        });
    }

    public TransferPage enterRecipientName(String name) {
        return StepLogger.log("Enter recipient name " + name, () -> {
            recipientNameInput.shouldBe(Condition.visible);
            recipientNameInput.click();
            recipientNameInput.clear();
            recipientNameInput.setValue(name);
            return this;
        });
    }

    public TransferPage enterRecipientAccount(String accountNumber) {
        return StepLogger.log("Enter recipient account " + accountNumber, () -> {
            recipientAccountInput.shouldBe(Condition.visible);
            recipientAccountInput.click();
            recipientAccountInput.clear();
            recipientAccountInput.setValue(accountNumber);
            return this;
        });
    }

    public TransferPage enterAmount(double amount) {
        return StepLogger.log("Enter transfer amount " + amount, () -> {
            amountInput.shouldBe(Condition.visible);
            amountInput.click();
            amountInput.clear();
            amountInput.setValue(String.valueOf(amount));
            return this;
        });
    }

    public TransferPage confirm() {
        return StepLogger.log("Confirm transfer details", () -> {
            confirmButton.click();
            return this;
        });
    }

    public TransferPage send() {
        return StepLogger.log("Send transfer", () -> {
            sendButton.click();
            return this;
        });
    }

    public TransferPage makeTransfer(int fromAccountIndex, String toAccount, double amount) {
        return StepLogger.log("Make transfer to " + toAccount + " amount=" + amount, () -> {
            selectFromAccount(fromAccountIndex);
            enterRecipientName("Test User");
            enterRecipientAccount(toAccount);
            enterAmount(amount);
            confirm();
            send();
            return this;
        });
    }

    public TransferPage makeTransferByAccountNumber(String fromAccountNumber, String toAccountNumber, double amount) {
        return StepLogger.log("Make transfer " + fromAccountNumber + " -> " + toAccountNumber, () -> {
            selectFromAccountByNumber(fromAccountNumber);
            enterRecipientName(TestDataConstants.DEFAULT_USER_NAME);
            enterRecipientAccount(toAccountNumber);
            enterAmount(amount);
            confirm();
            send();
            return this;
        });
    }

    private void selectFromAccountByNumber(String accountNumber) {
        accountSelector.shouldBe(Condition.visible);
        accountSelector.click();
        $$("select.account-selector option").shouldHave(sizeGreaterThan(1));
        $$("select.account-selector option").findBy(Condition.text(accountNumber)).click();
    }
}
