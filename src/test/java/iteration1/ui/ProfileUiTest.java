package iteration1.ui;

import generators.RandomData;
import models.CreateUserRequest;
import models.ProfileResponse;
import models.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;
import ui.pages.ProfilePage;
import ui.pages.BankAlert;

import static org.assertj.core.api.Assertions.assertThat;

public class ProfileUiTest extends BaseUiTest {

    private String userToken;
    private String originalName = "Old Name";
    private ProfilePage profilePage;

    @BeforeEach
    public void prepareData() {
        String username = RandomData.getUsername();
        String password = RandomData.getPassword();

        CreateUserRequest createUserRequest = CreateUserRequest.builder()
                .username(username)
                .password(password)
                .name(originalName)
                .role(UserRole.USER.toString())
                .build();

        AdminSteps.createUser(createUserRequest);
        userToken = UserSteps.loginAndGetToken(username, password);

        authThroughLocalStorage(userToken);
        profilePage = new ProfilePage();
    }

    @Test
    public void userCanChangeNameValidTest() {
        String newName = originalName + "a";

        profilePage.changeName(newName, BankAlert.PROFILE_SUCCESS.getMessage());

        ProfileResponse profile = UserSteps.getProfile(userToken);
        assertThat(profile.getName()).isEqualTo(newName);
    }

    @Test
    public void userCannotChangeToSingleWordTest() {
        String invalidName = "John";

        profilePage.changeName(invalidName, BankAlert.PROFILE_TWO_WORDS.getMessage());

        ProfileResponse profile = UserSteps.getProfile(userToken);
        assertThat(profile.getName()).isEqualTo(originalName);
    }

    @Test
    public void userCannotChangeNameWithDigitsTest() {
        String invalidName = "John 123";

        profilePage.changeName(invalidName, BankAlert.PROFILE_LETTERS_ONLY.getMessage());

        ProfileResponse profile = UserSteps.getProfile(userToken);
        assertThat(profile.getName()).isEqualTo(originalName);
    }

    @Test
    public void userCannotChangeNameWithSpecialCharsTest() {
        String invalidName = "John@ Doe";

        profilePage.changeName(invalidName, BankAlert.PROFILE_LETTERS_ONLY.getMessage());

        ProfileResponse profile = UserSteps.getProfile(userToken);
        assertThat(profile.getName()).isEqualTo(originalName);
    }

    @Test
    public void userCannotChangeToEmptyNameTest() {
        String invalidName = "";

        profilePage.changeName(invalidName, BankAlert.PROFILE_SAME_AS_CURRENT.getMessage());

        ProfileResponse profile = UserSteps.getProfile(userToken);
        assertThat(profile.getName()).isEqualTo(originalName);
    }

    @Test
    public void userNameShouldBeTrimmedTest() {
        String expectedName = "Anna Smith";

        profilePage.changeName(expectedName, BankAlert.PROFILE_UPDATED_SUCCESSFULLY.getMessage());

        ProfileResponse profile = UserSteps.getProfile(userToken);
        assertThat(profile.getName()).isEqualTo(expectedName);
    }
}