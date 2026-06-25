package iteration1.api;

import api.dao.UserDao;
import api.dao.comparison.DaoAndModelAssertions;
import generators.RandomData;
import models.CreateUserRequest;
import models.CreateUserResponse;
import models.UserRole;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.steps.AdminSteps;
import requests.steps.DataBaseSteps;
import requests.steps.UserSteps;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Create User Tests - API & Database Integration")
public class CreateUserTest extends BaseTest {

    private static Stream<Arguments> validUserData() {
        return Stream.of(
                Arguments.of(RandomData.getUsername(), RandomData.getPassword(), UserRole.USER.toString()),
                Arguments.of(RandomData.getUsernameWithDot(), RandomData.getPassword(), UserRole.USER.toString()),
                Arguments.of(RandomData.getUsernameWithDash(), RandomData.getPassword(), UserRole.USER.toString()),
                Arguments.of(RandomData.getUsernameWithUnderscore(), RandomData.getPassword(), UserRole.USER.toString()),
                Arguments.of(RandomData.getUsernameWithDigits(), RandomData.getPassword(), UserRole.USER.toString())
        );
    }

    private static Stream<Arguments> invalidUsernameData() {
        return Stream.of(
                Arguments.of("", "Username cannot be blank"),
                Arguments.of("ab", "Username must be between 3 and 15 characters"),
                Arguments.of("abcdefghijklmnop", "Username must be between 3 and 15 characters"),
                Arguments.of("abc%", "Username must contain only letters, digits, dashes, underscores, and dots"),
                Arguments.of("john doe", "Username must contain only letters, digits, dashes, underscores, and dots")
        );
    }

    private static Stream<Arguments> invalidPasswordData() {
        String expectedMessage = "Password must contain at least one digit, one lower case, one upper case, one special character, no spaces, and be at least 8 characters long";
        return Stream.of(
                Arguments.of("", expectedMessage),
                Arguments.of("Pass1#", expectedMessage),
                Arguments.of("password", expectedMessage),
                Arguments.of("PASSWORD", expectedMessage),
                Arguments.of("12345678", expectedMessage),
                Arguments.of("Password", expectedMessage),
                Arguments.of("Password1", expectedMessage),
                Arguments.of("Password1# ", expectedMessage)
        );
    }

    @Nested
    @DisplayName("Positive Scenarios")
    class PositiveTests {

        @ParameterizedTest
        @MethodSource("iteration1.api.CreateUserTest#validUserData")
        @DisplayName("TC-001: Admin can create user with valid data")
        void adminCanCreateUserWithCorrectData(String username, String password, String role) {
            CreateUserRequest request = CreateUserRequest.builder()
                    .username(username)
                    .password(password)
                    .role(role)
                    .build();

            CreateUserResponse response = AdminSteps.createUser(request);

            UserDao userDao = DataBaseSteps.getUserByUsername(username);

            DaoAndModelAssertions.assertThat(response, userDao).matches();

            trackUser(userDao.getId());
        }

        @Test
        @Disabled("BACKEND-1234: StackOverflow из-за рекурсии в JSON ответе")
        @DisplayName("TC-005: Admin can get all users - includes newly created user")
        void adminCanGetAllUsersTest() {
            CreateUserRequest request = CreateUserRequest.builder()
                    .username(RandomData.getUsername())
                    .password(RandomData.getPassword())
                    .role(UserRole.USER.toString())
                    .build();

            CreateUserResponse createdUser = AdminSteps.createUser(request);

            var users = AdminSteps.getAllUsers();

            assertThat(users)
                    .extracting(CreateUserResponse::getUsername)
                    .contains(request.getUsername());

            UserDao userDao = DataBaseSteps.getUserByUsername(request.getUsername());
            DaoAndModelAssertions.assertThat(createdUser, userDao).matches();

            trackUser(userDao.getId());
        }

        @Test
        @DisplayName("TC-006: Admin can delete user - user cannot login after deletion")
        void adminCanDeleteUserTest() {
            CreateUserRequest request = CreateUserRequest.builder()
                    .username(RandomData.getUsername())
                    .password(RandomData.getPassword())
                    .role(UserRole.USER.toString())
                    .build();

            CreateUserResponse response = AdminSteps.createUser(request);

            UserDao userBefore = DataBaseSteps.getUserByUsername(request.getUsername());
            assertThat(userBefore).isNotNull();

            AdminSteps.deleteUser(response.getId());

            UserSteps.loginAndExpectUnauthorized(request.getUsername(), request.getPassword());

            boolean userExists = DataBaseSteps.userExistsInDb(request.getUsername());
            assertThat(userExists).isFalse();
        }
    }

    @Nested
    @DisplayName("Negative Scenarios - Validation")
    class ValidationTests {

        @ParameterizedTest
        @MethodSource("iteration1.api.CreateUserTest#invalidUsernameData")
        @DisplayName("TC-007: Admin cannot create user with invalid username")
        void adminCannotCreateUserWithInvalidUsername(String username, String expectedError) {
            CreateUserRequest request = CreateUserRequest.builder()
                    .username(username)
                    .password(RandomData.getPassword())
                    .role(UserRole.USER.toString())
                    .build();

            AdminSteps.createUserAndExpectUsernameError(request, expectedError);

            boolean userExists = DataBaseSteps.userExistsInDb(username);
            assertThat(userExists).isFalse();
        }
    }
}