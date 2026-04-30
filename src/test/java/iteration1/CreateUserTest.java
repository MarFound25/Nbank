package iteration1;

import generators.RandomData;
import models.CreateUserRequest;
import models.CreateUserResponse;
import models.UserRole;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.skelethon.requesters.CrudRequesters;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

public class CreateUserTest extends BaseTest {

    public static Stream<Arguments> validUserData() {
        return Stream.of(
                Arguments.of(RandomData.getUsername(), RandomData.getPassword(), UserRole.USER.toString()),
                Arguments.of(RandomData.getUsernameWithDot(), RandomData.getPassword(), UserRole.USER.toString()),
                Arguments.of(RandomData.getUsernameWithDash(), RandomData.getPassword(), UserRole.USER.toString()),
                Arguments.of(RandomData.getUsernameWithUnderscore(), RandomData.getPassword(), UserRole.USER.toString()),
                Arguments.of(RandomData.getUsernameWithDigits(), RandomData.getPassword(), UserRole.USER.toString())
        );
    }

    @MethodSource("validUserData")
    @ParameterizedTest(name = "Создание пользователя: username={0}, role={2}")
    public void adminCanCreateUserWithCorrectData(String username, String password, String role) {
        CreateUserRequest request = CreateUserRequest.builder()
                .username(username)
                .password(password)
                .role(role)
                .build();

        CreateUserResponse response = AdminSteps.createUser(request);

        softly.assertThat(response.getUsername()).isEqualTo(request.getUsername());
        softly.assertThat(response.getPassword()).isNotEqualTo(request.getPassword());
        softly.assertThat(response.getRole()).isEqualTo(request.getRole());
    }

    @Test
    public void adminCanCreateUserWithMinUsernameLengthTest() {
        String username = RandomData.getUsernameMinLength();

        CreateUserRequest request = CreateUserRequest.builder()
                .username(username)
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        CreateUserResponse response = AdminSteps.createUser(request);

        softly.assertThat(response.getUsername()).isEqualTo(username);
    }

    @Test
    public void adminCanCreateUserWithMaxUsernameLengthTest() {
        String username = RandomData.getUsernameMaxLength();

        CreateUserRequest request = CreateUserRequest.builder()
                .username(username)
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        CreateUserResponse response = AdminSteps.createUser(request);

        softly.assertThat(response.getUsername()).isEqualTo(username);
    }

    @Test
    public void adminCanCreateUserWithAdminRoleTest() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.ADMIN.toString())
                .build();

        CreateUserResponse response = AdminSteps.createUser(request);

        softly.assertThat(response.getRole()).isEqualTo(UserRole.ADMIN.toString());
    }

    public static Stream<Arguments> invalidUsernameData() {
        return Stream.of(
                Arguments.of("", RandomData.getPassword(), "USER", "Username cannot be blank"),
                Arguments.of("ab", RandomData.getPassword(), "USER", "Username must be between 3 and 15 characters"),
                Arguments.of("abcdefghijklmnop", RandomData.getPassword(), "USER", "Username must be between 3 and 15 characters"),
                Arguments.of("abc%", RandomData.getPassword(), "USER", "Username must contain only letters, digits, dashes, underscores, and dots"),
                Arguments.of("john doe", RandomData.getPassword(), "USER", "Username must contain only letters, digits, dashes, underscores, and dots")
        );
    }

    @MethodSource("invalidUsernameData")
    @ParameterizedTest
    public void adminCannotCreateUserWithInvalidUsername(String username, String password, String role, String expectedError) {
        CreateUserRequest request = CreateUserRequest.builder()
                .username(username)
                .password(password)
                .role(role)
                .build();

        new CrudRequesters(RequestSpecs.adminSpec(), ResponseSpecs.requestReturnsBadRequest())
                .create(request)
                .body("username", Matchers.hasItem(expectedError));
    }

    public static Stream<Arguments> invalidPasswordData() {
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

    @MethodSource("invalidPasswordData")
    @ParameterizedTest
    public void adminCannotCreateUserWithInvalidPassword(String password, String expectedError) {
        CreateUserRequest request = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(password)
                .role(UserRole.USER.toString())
                .build();

        new CrudRequesters(RequestSpecs.adminSpec(), ResponseSpecs.requestReturnsBadRequest())
                .create(request)
                .body("password", Matchers.hasItem(expectedError));
    }


    @Test
    public void adminCannotCreateUserWithInvalidRoleTest() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role("EDITOR")
                .build();

        AdminSteps.createUserAndExpectBadRequest(request);
    }

    @Test
    public void adminCannotCreateUserThatAlreadyExistsTest() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        AdminSteps.createUser(request);

        String expectedError = "Error: Username '" + request.getUsername() + "' already exists.";
        AdminSteps.createUserAndExpectBadRequest(request, expectedError);
    }

    @Test
    public void adminCanGetAllUsersTest() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        AdminSteps.createUser(request);

        var users = AdminSteps.getAllUsers();

        softly.assertThat(users)
                .extracting(CreateUserResponse::getUsername)
                .contains(request.getUsername());
    }

    @Test
    public void userCannotGetAllUsersWithBearerTokenTest() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        AdminSteps.createUser(request);
        String token = UserSteps.loginAndGetToken(request.getUsername(), request.getPassword());

        AdminSteps.getAllUsersAndExpectForbidden(token);
    }

    @Test
    public void adminCanDeleteUserTest() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        CreateUserResponse response = AdminSteps.createUser(request);
        AdminSteps.deleteUser(response.getId());

        UserSteps.loginAndExpectUnauthorized(request.getUsername(), request.getPassword());
    }

    @Test
    public void adminCannotDeleteNonExistentUserTest() {
        AdminSteps.deleteUserAndExpectNotFound(999999);
    }

    @Test
    public void userCannotDeleteUserTest() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        AdminSteps.createUser(request);
        String token = UserSteps.loginAndGetToken(request.getUsername(), request.getPassword());

        AdminSteps.deleteUserAndExpectForbidden(token, 1);
    }
}