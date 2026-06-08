package iteration1.ui;

import generators.RandomModelGenerator;
import models.CreateUserRequest;
import common.annotations.AdminSession;
import org.junit.jupiter.api.Test;
import ui.pages.AdminPanel;
import ui.pages.BankAlert;
import ui.pages.TestDataConstants;

public class CreateUserTest extends BaseUiTest {

    @Test
    @AdminSession
    public void adminCanCreateUserTest() {
        CreateUserRequest newUser = RandomModelGenerator.generate(CreateUserRequest.class);

        new AdminPanel()
                .open()
                .getAdminPanelText()
                .createUser(newUser.getUsername(), newUser.getPassword())
                .checkAlertMessageAndAccept(BankAlert.USER_CREATED_SUCCESSFULLY.getMessage())
                .findUser(newUser.getUsername())
                .assertUserExists()
                .assertUserMatchesApi(newUser);
    }

    @Test
    @AdminSession
    public void adminCannotCreateUserWithInvalidDataTest() {
        CreateUserRequest newUser = RandomModelGenerator.generate(CreateUserRequest.class);
        newUser.setUsername(TestDataConstants.INVALID_USERNAME);

        new AdminPanel()
                .open()
                .getAdminPanelText()
                .createUser(newUser.getUsername(), newUser.getPassword())
                .checkAlertMessageAndAccept(BankAlert.USERNAME_MUST_BE_BETWEEN_3_AND_15_CHARACTERS.getMessage())
                .findUser(newUser.getUsername())
                .assertUserNotExists()
                .assertUserNotExistsInApi(newUser);
    }
}