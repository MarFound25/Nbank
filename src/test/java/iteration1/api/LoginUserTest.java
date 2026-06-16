package iteration1.api;

import api.dao.UserDao;
import configs.Config;
import generators.RandomData;
import models.CreateUserRequest;
import models.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import requests.steps.AdminSteps;
import requests.steps.DataBaseSteps;
import requests.steps.UserSteps;


@DisplayName("Login Tests - API & Database Integration")
public class LoginUserTest extends BaseTest {

    private String currentUsername;
    private Long currentUserId;

    private void createUserAndTrack(String username, String password) {
        CreateUserRequest createRequest = CreateUserRequest.builder()
                .username(username)
                .password(password)
                .role(UserRole.USER.toString())
                .build();

        AdminSteps.createUser(createRequest);

        UserDao userDao = DataBaseSteps.getUserByUsername(username);
        currentUserId = userDao.getId();
        currentUsername = username;
        trackUser(currentUserId);
    }

    @Nested
    @DisplayName("Positive Scenarios")
    class PositiveTests {

        @Test
        @DisplayName("TC-LOG-001: Admin can generate auth token - user exists in DB")
        void adminCanGenerateAuthTokenTest() {
            String adminUsername = Config.getAdminUsername();
            String adminPassword = Config.getAdminPassword();

            String token = UserSteps.loginAndGetToken(adminUsername, adminPassword);

            softly.assertThat(token).isNotNull();
            softly.assertThat(token).startsWith("Basic ");

            UserDao adminDao = DataBaseSteps.getUserByUsername(adminUsername);
            softly.assertThat(adminDao).isNotNull();
            softly.assertThat(adminDao.getRole()).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("TC-LOG-002: User can generate auth token - user exists in DB")
        void userCanGenerateAuthTokenTest() {
            String username = RandomData.getUsername();
            String password = RandomData.getPassword();
            createUserAndTrack(username, password);

            String token = UserSteps.loginAndGetToken(username, password);

            softly.assertThat(token).isNotNull();
            softly.assertThat(token).startsWith("Basic ");

            UserDao userDao = DataBaseSteps.getUserByUsername(username);
            softly.assertThat(userDao).isNotNull();
            softly.assertThat(userDao.getUsername()).isEqualTo(username);
            softly.assertThat(userDao.getPasswordHash()).isNotEqualTo(password);
            softly.assertThat(userDao.getPasswordHash()).matches("^\\$2[ayb]\\$.{56}$");
        }
    }

    @Nested
    @DisplayName("Negative Scenarios - Authentication")
    class NegativeTests {

        @Test
        @DisplayName("TC-LOG-003: User cannot login with wrong password - DB unchanged")
        void userCannotLoginWithWrongPasswordTest() {
            String username = RandomData.getUsername();
            String correctPassword = RandomData.getPassword();
            createUserAndTrack(username, correctPassword);

            UserDao userBefore = DataBaseSteps.getUserByUsername(username);
            String passwordHashBefore = userBefore.getPasswordHash();

            UserSteps.loginAndExpectUnauthorized(username, "WrongPassword123!");

            UserDao userAfter = DataBaseSteps.getUserByUsername(username);
            softly.assertThat(userAfter).isNotNull();
            softly.assertThat(userAfter.getPasswordHash()).isEqualTo(passwordHashBefore);
        }

        @Test
        @DisplayName("TC-LOG-004: Cannot login with non-existent username")
        void userCannotLoginWithNonExistentUsernameTest() {
            String nonExistentUsername = "nonexistentuser123";

            UserSteps.loginAndExpectUnauthorized(nonExistentUsername, "AnyPassword123!");

            boolean userExists = DataBaseSteps.userExistsInDb(nonExistentUsername);
            softly.assertThat(userExists).isFalse();
        }
    }
}