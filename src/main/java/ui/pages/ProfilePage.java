package ui.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import common.helpers.StepLogger;
import lombok.Getter;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

@Getter
public class ProfilePage extends BasePage<ProfilePage> {

    private SelenideElement profileHeader = $(".profile-header");
    private SelenideElement nameInput = $("input.form-control.mt-3");
    private SelenideElement saveButton = $("button.btn-primary.mt-3");

    @Override
    public String url() {
        return "/dashboard";
    }

    public ProfilePage openEditProfile() {
        profileHeader.click();
        $$("h1").get(1).shouldHave(Condition.text("Edit Profile"));
        return this;
    }

    public ProfilePage enterNewName(String newName) {
        return StepLogger.log("Enter new profile name: '" + newName + "'", () -> {
            nameInput.shouldBe(Condition.visible);
            nameInput.click();
            nameInput.sendKeys(org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.CONTROL, "a"));
            nameInput.sendKeys(org.openqa.selenium.Keys.DELETE);
            if (newName != null && !newName.isEmpty()) {
                nameInput.setValue(newName);
            }
            return this;
        });
    }

    public void clickSave() {
        StepLogger.log("Click Save on Profile page", () -> {
            saveButton.click();
            return null;
        });
    }

    public void changeName(String newName, String expectedAlertMessage) {
        StepLogger.log("Change profile name to '" + newName + "'", () -> {
            openEditProfile();
            enterNewName(newName);
            clickSave();
            checkAlertMessageAndAccept(expectedAlertMessage);
            return null;
        });
    }
}
