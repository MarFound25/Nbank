package iteration1.ui;

import models.CreateUserRequest;
import models.ProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import requests.steps.UserSteps;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import ui.pages.ProfilePage;
import ui.pages.BankAlert;
import ui.pages.TestDataConstants;
import ui.pages.BasePage;

import static org.assertj.core.api.Assertions.assertThat;

public class ProfileUiTest extends BaseUiTest {

    private String userToken;
    private String originalName;

    @BeforeEach
    public void prepareData() {
        CreateUserRequest user = SessionStorage.getUser();
        userToken = UserSteps.loginAndGetToken(user.getUsername(), user.getPassword());
        BasePage.authAsUser(user);

        ProfileResponse profile = UserSteps.getProfile(userToken);
        originalName = profile.getName();
    }

    @Test
    @UserSession
    public void userCanChangeNameValidTest() {
        String newName = originalName + TestDataConstants.NEW_NAME_SUFFIX;

        new ProfilePage().changeName(newName, BankAlert.PROFILE_SUCCESS.getMessage());

        ProfileResponse profile = UserSteps.getProfile(userToken);
        assertThat(profile.getName()).isEqualTo(newName);
    }

    @Test
    @UserSession
    public void userCannotChangeToSingleWordTest() {
        new ProfilePage().changeName(TestDataConstants.INVALID_SINGLE_WORD, BankAlert.PROFILE_TWO_WORDS.getMessage());

        ProfileResponse profile = UserSteps.getProfile(userToken);
        assertThat(profile.getName()).isEqualTo(originalName);
    }

    @Test
    @UserSession
    public void userCannotChangeNameWithDigitsTest() {
        new ProfilePage().changeName(TestDataConstants.INVALID_WITH_DIGITS, BankAlert.PROFILE_LETTERS_ONLY.getMessage());

        ProfileResponse profile = UserSteps.getProfile(userToken);
        assertThat(profile.getName()).isEqualTo(originalName);
    }

    @Test
    @UserSession
    public void userCannotChangeNameWithSpecialCharsTest() {
        new ProfilePage().changeName(TestDataConstants.INVALID_WITH_SPECIAL, BankAlert.PROFILE_LETTERS_ONLY.getMessage());

        ProfileResponse profile = UserSteps.getProfile(userToken);
        assertThat(profile.getName()).isEqualTo(originalName);
    }

    @Test
    @UserSession
    public void userCannotChangeToEmptyNameTest() {
        new ProfilePage().changeName(TestDataConstants.EMPTY_STRING, BankAlert.PROFILE__ENTER_VALID_NAME.getMessage());

        ProfileResponse profile = UserSteps.getProfile(userToken);
        assertThat(profile.getName()).isEqualTo(originalName);
    }

    @Test
    @UserSession
    public void userNameShouldBeTrimmedTest() {
        String expectedName = TestDataConstants.EXPECTED_TRIMMED_NAME;

        new ProfilePage().changeName(expectedName, BankAlert.PROFILE_UPDATED_SUCCESSFULLY.getMessage());

        ProfileResponse profile = UserSteps.getProfile(userToken);
        assertThat(profile.getName()).isEqualTo(expectedName);


    }
}