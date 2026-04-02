package iteration1;

import generators.RandomData;
import models.CreateUserRequest;
import models.CreateUserResponse;
import models.LoginUserRequest;
import models.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.*;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.notNullValue;

public class CreateUserTest extends BaseTest {

    private LoginUserRequest toLoginRequest(CreateUserRequest createRequest) {
        return LoginUserRequest.builder()
                .username(createRequest.getUsername())
                .password(createRequest.getPassword())
                .build();
    }


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

        CreateUserResponse response = new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(request)
                .extract()
                .as(CreateUserResponse.class);

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

        CreateUserResponse response = new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(request)
                .extract()
                .as(CreateUserResponse.class);

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

        CreateUserResponse response = new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(request)
                .extract()
                .as(CreateUserResponse.class);

        softly.assertThat(response.getUsername()).isEqualTo(username);
    }

    @Test
    public void adminCanCreateUserWithAdminRoleTest() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.ADMIN.toString())
                .build();

        CreateUserResponse response = new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(request)
                .extract()
                .as(CreateUserResponse.class);

        softly.assertThat(response.getRole()).isEqualTo(UserRole.ADMIN.toString());
    }


    public static Stream<Arguments> invalidUsernameData() {
        return Stream.of(
                Arguments.of("", RandomData.getPassword(), "USER", "username", "Username cannot be blank"),
                Arguments.of("ab", RandomData.getPassword(), "USER", "username", "Username must be between 3 and 15 characters"),
                Arguments.of("abcdefghijklmnop", RandomData.getPassword(), "USER", "username", "Username must be between 3 and 15 characters"),
                Arguments.of("abc%", RandomData.getPassword(), "USER", "username", "Username must contain only letters, digits, dashes, underscores, and dots"),
                Arguments.of("john doe", RandomData.getPassword(), "USER", "username", "Username must contain only letters, digits, dashes, underscores, and dots")
        );
    }

    @MethodSource("invalidUsernameData")
    @ParameterizedTest
    public void adminCannotCreateUserWithInvalidUsername(String username, String password, String role,
                                                         String errorKey, String errorValue) {
        CreateUserRequest request = CreateUserRequest.builder()
                .username(username)
                .password(password)
                .role(role)
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.requestReturnsBadRequest(errorKey, errorValue))
                .post(request);
    }


    public static Stream<Arguments> invalidPasswordData() {
        String expectedMessage = "Password must contain at least one digit, one lower case, one upper case, one special character, no spaces, and be at least 8 characters long";

        return Stream.of(
                Arguments.of("testuser", "", "USER", "password", "Password cannot be blank"),
                Arguments.of("testuser", "Pass1#", "USER", "password", expectedMessage),
                Arguments.of("testuser", "password", "USER", "password", expectedMessage),
                Arguments.of("testuser", "PASSWORD", "USER", "password", expectedMessage),
                Arguments.of("testuser", "12345678", "USER", "password", expectedMessage),
                Arguments.of("testuser", "Password", "USER", "password", expectedMessage),
                Arguments.of("testuser", "Password1", "USER", "password", expectedMessage),
                Arguments.of("testuser", "Password1# ", "USER", "password", expectedMessage)
        );
    }

    @MethodSource("invalidPasswordData")
    @ParameterizedTest
    public void adminCannotCreateUserWithInvalidPassword(String username, String password, String role,
                                                         String errorKey, String errorValue) {
        CreateUserRequest request = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(password)
                .role(role)
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.requestReturnsBadRequest(errorKey, errorValue))
                .post(request);
    }


    @Test
    public void adminCannotCreateUserWithInvalidRoleTest() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role("EDITOR")
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.requestReturnsBadRequest())
                .post(request);
    }


    @Test
    public void adminCannotCreateUserThatAlreadyExistsTest() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(request);

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.requestReturnsBadRequest("Error: Username '" + request.getUsername() + "' already exists."))
                .post(request);
    }


    @Test
    public void adminCanGetAllUsersTest() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(request);

        new AdminGetAllUsersRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.requestReturnsOK())
                .get()
                .body("find { it.username == '" + request.getUsername() + "' }", notNullValue());
    }

    @Test
    public void userCannotGetAllUsersWithBearerTokenTest() {
        CreateUserRequest createRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(createRequest);


        LoginUserRequest loginRequest = toLoginRequest(createRequest);

        String authToken = new LoginUserRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsOK())
                .post(loginRequest)
                .extract()
                .header("Authorization");

        new AdminGetAllUsersRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsForbidden())
                .get();
    }


    @Test
    public void adminCanDeleteUserTest() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        Integer userId = new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(request)
                .extract()
                .jsonPath()
                .getInt("id");

        new AdminDeleteUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.requestReturnsOK())
                .delete(userId);

        LoginUserRequest loginRequest = toLoginRequest(request);

        new LoginUserRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsUnauthorized())
                .post(loginRequest);
    }

    @Test
    public void adminCannotDeleteNonExistentUserTest() {
        new AdminDeleteUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.requestReturnsNotFound())
                .delete(999999);
    }

    @Test
    public void userCannotDeleteUserTest() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(request);

        LoginUserRequest loginRequest = toLoginRequest(request);

        String authToken = new LoginUserRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsOK())
                .post(loginRequest)
                .extract()
                .header("Authorization");

        new AdminDeleteUserRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsForbidden())
                .delete(1);
    }
}