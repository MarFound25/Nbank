package iteration1.ui;

import com.codeborne.selenide.Condition;
import common.extensions.UserSessionExtension;
import generators.RandomModelGenerator;
import models.CreateUserRequest;
import models.CreateUserResponse;
import org.junit.jupiter.api.extension.ExtendWith;
import requests.steps.AdminSteps;
import common.annotations.AdminSession;
import org.junit.jupiter.api.Test;
import ui.pages.AdminPanel;
import ui.pages.BankAlert;
import ui.pages.TestDataConstants;

import static org.assertj.core.api.Assertions.assertThat;
@ExtendWith(UserSessionExtension.class)
public class CreateUserTest extends BaseUiTest {

    @Test
    @AdminSession
    public void adminCanCreateUserTest() {
        CreateUserRequest newUser = RandomModelGenerator.generate(CreateUserRequest.class);

        AdminPanel adminPanel = new AdminPanel().open();

        adminPanel.getAdminPanelText().shouldBe(Condition.visible);

        adminPanel.createUser(newUser.getUsername(), newUser.getPassword())
                .checkAlertMessageAndAccept(BankAlert.USER_CREATED_SUCCESSFULLY.getMessage());

        adminPanel.findUser(newUser.getUsername()).shouldBe(Condition.visible);

        CreateUserResponse createdUser = AdminSteps.getAllUsers().stream()
                .filter(user -> user.getUsername().equals(newUser.getUsername()))
                .findFirst()
                .orElseThrow();

        assertThat(createdUser.getUsername()).isEqualTo(newUser.getUsername());
        assertThat(createdUser.getRole()).isEqualTo(newUser.getRole());
    }

    @Test
    @AdminSession
    public void adminCannotCreateUserWithInvalidDataTest() {
        CreateUserRequest newUser = RandomModelGenerator.generate(CreateUserRequest.class);
        newUser.setUsername(TestDataConstants.INVALID_USERNAME);

        AdminPanel adminPanel = new AdminPanel().open();

        adminPanel.getAdminPanelText().shouldBe(Condition.visible);

        adminPanel.createUser(newUser.getUsername(), newUser.getPassword())
                .checkAlertMessageAndAccept(
                        BankAlert.USERNAME_MUST_BE_BETWEEN_3_AND_15_CHARACTERS.getMessage());

        adminPanel.findUser(newUser.getUsername()).shouldNotBe(Condition.exist);

        long usersWithSameUsernameAsNewUser = AdminSteps.getAllUsers().stream()
                .filter(user -> user.getUsername().equals(newUser.getUsername()))
                .count();

        assertThat(usersWithSameUsernameAsNewUser).isZero();
    }
}