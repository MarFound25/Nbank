package iteration1.ui;

import common.annotations.UserSession;
import org.junit.jupiter.api.Test;
import ui.pages.BankAlert;
import ui.pages.ProfilePage;
import ui.pages.TestDataConstants;

public class ProfileUiTest extends BaseUiTest {

    @Test
    @UserSession
    public void userCanChangeNameValidTest() {
        new ProfilePage()
                .openWithAuth()
                .changeName(TestDataConstants.NEW_NAME_SUFFIX, BankAlert.PROFILE_SUCCESS.getMessage())
                .assertNameChanged(TestDataConstants.NEW_NAME_SUFFIX);
    }

    @Test
    @UserSession
    public void userCannotChangeToSingleWordTest() {
        new ProfilePage()
                .openWithAuth()
                .changeName(TestDataConstants.INVALID_SINGLE_WORD, BankAlert.PROFILE_TWO_WORDS.getMessage())
                .assertNameNotChanged();
    }

    @Test
    @UserSession
    public void userCannotChangeNameWithDigitsTest() {
        new ProfilePage()
                .openWithAuth()
                .changeName(TestDataConstants.INVALID_WITH_DIGITS, BankAlert.PROFILE_LETTERS_ONLY.getMessage())
                .assertNameNotChanged();
    }

    @Test
    @UserSession
    public void userCannotChangeNameWithSpecialCharsTest() {
        new ProfilePage()
                .openWithAuth()
                .changeName(TestDataConstants.INVALID_WITH_SPECIAL, BankAlert.PROFILE_LETTERS_ONLY.getMessage())
                .assertNameNotChanged();
    }

    @Test
    @UserSession
    public void userCannotChangeToEmptyNameTest() {
        new ProfilePage()
                .openWithAuth()
                .changeName(TestDataConstants.EMPTY_STRING, BankAlert.PROFILE_SAME_AS_CURRENT.getMessage())
                .assertNameNotChanged();
    }

    @Test
    @UserSession
    public void userNameShouldBeTrimmedTest() {
        new ProfilePage()
                .openWithAuth()
                .changeName(TestDataConstants.EXPECTED_TRIMMED_NAME, BankAlert.PROFILE_UPDATED_SUCCESSFULLY.getMessage())
                .assertNameChanged(TestDataConstants.EXPECTED_TRIMMED_NAME);
    }
}