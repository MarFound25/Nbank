package iteration1.ui;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import generators.RandomModelGenerator;
import models.CreateUserRequest;
import models.CreateUserResponse;
import org.junit.jupiter.api.Test;
import requests.steps.DataBaseSteps;
import common.helpers.UiBrokenJsonMock;
import ui.pages.AdminPanel;
import ui.pages.BankAlert;
import ui.pages.LoginPage;
import ui.pages.TestDataConstants;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateUserTest extends BaseUiTest {

    @Test
    public void adminCanCreateUserTest() {
        CreateUserRequest newUser = RandomModelGenerator.generate(CreateUserRequest.class);

        new LoginPage().open();
        UiBrokenJsonMock.setAdminUsersOverride(List.of());
        new LoginPage().login(TestDataConstants.ADMIN_USERNAME, TestDataConstants.ADMIN_PASSWORD);

        AdminPanel adminPanel = new AdminPanel();
        adminPanel.getAdminPanelText().shouldBe(Condition.visible);

        adminPanel.createUserAndAcceptAlert(
                newUser.getUsername(),
                newUser.getPassword(),
                BankAlert.USER_CREATED_SUCCESSFULLY.getMessage());

        var created = DataBaseSteps.getUserByUsername(newUser.getUsername());
        UiBrokenJsonMock.setAdminUsersOverride(List.of(
                CreateUserResponse.builder()
                        .id(created.getId())
                        .username(created.getUsername())
                        .name(created.getName())
                        .role(created.getRole())
                        .accounts(List.of())
                        .build()
        ));
        Selenide.refresh();
        UiBrokenJsonMock.install();
        adminPanel.getAdminPanelText().shouldBe(Condition.visible);
        adminPanel.findUser(newUser.getUsername()).shouldBe(Condition.visible);

        CreateUserResponse createdUser = CreateUserResponse.builder()
                .id(created.getId())
                .username(created.getUsername())
                .role(created.getRole())
                .build();

        assertThat(createdUser.getUsername()).isEqualTo(newUser.getUsername());
        assertThat(created.getRole()).isEqualTo(newUser.getRole());
    }

    @Test
    public void adminCannotCreateUserWithInvalidDataTest() {
        CreateUserRequest newUser = RandomModelGenerator.generate(CreateUserRequest.class);
        newUser.setUsername(TestDataConstants.INVALID_USERNAME);

        new LoginPage().open();
        UiBrokenJsonMock.setAdminUsersOverride(List.of());
        new LoginPage().login(TestDataConstants.ADMIN_USERNAME, TestDataConstants.ADMIN_PASSWORD);

        AdminPanel adminPanel = new AdminPanel();
        adminPanel.getAdminPanelText().shouldBe(Condition.visible);

        adminPanel.createUserAndAcceptAlert(
                newUser.getUsername(),
                newUser.getPassword(),
                BankAlert.USERNAME_MUST_BE_BETWEEN_3_AND_15_CHARACTERS.getMessage());

        adminPanel.findUser(newUser.getUsername()).shouldNotBe(Condition.exist);

        assertThat(DataBaseSteps.getUserByUsername(newUser.getUsername())).isNull();
    }
}
