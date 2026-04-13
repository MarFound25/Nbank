package iteration1;

import generators.RandomData;
import models.CreateUserRequest;
import models.LoginUserRequest;
import models.UserRole;
import org.junit.jupiter.api.Test;
import requests.AdminCreateUserRequester;
import requests.LoginUserRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import static org.hamcrest.Matchers.notNullValue;

public class LoginUserTest extends BaseTest {

    private LoginUserRequest toLoginRequest(CreateUserRequest createRequest) {
        return LoginUserRequest.builder()
                .username(createRequest.getUsername())
                .password(createRequest.getPassword())
                .build();
    }

    @Test
    public void adminCanGenerateAuthTokenTest() {
        LoginUserRequest request = LoginUserRequest.builder()
                .username("admin")
                .password("admin")
                .build();

        new LoginUserRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsOK())
                .post(request)
                .header("Authorization", notNullValue());
    }

    @Test
    public void userCanGenerateAuthTokenTest() {
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

        new LoginUserRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsOK())
                .post(loginRequest)
                .header("Authorization", notNullValue());
    }


    @Test
    public void userCannotLoginWithWrongPasswordTest() {
        CreateUserRequest createRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(createRequest);

        LoginUserRequest loginRequest = LoginUserRequest.builder()
                .username(createRequest.getUsername())
                .password("WrongPassword123!")
                .build();

        new LoginUserRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsUnauthorized())
                .post(loginRequest);
    }

    @Test
    public void userCannotLoginWithNonExistentUsernameTest() {
        LoginUserRequest request = LoginUserRequest.builder()
                .username("nonexistentuser123")
                .password("AnyPassword123!")
                .build();

        new LoginUserRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsUnauthorized())
                .post(request);
    }

    @Test
    public void userCannotLoginWithEmptyUsernameTest() {
        LoginUserRequest request = LoginUserRequest.builder()
                .username("")
                .password(RandomData.getPassword())
                .build();

        new LoginUserRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsUnauthorized())
                .post(request);
    }

    @Test
    public void userCannotLoginWithEmptyPasswordTest() {
        CreateUserRequest createRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(createRequest);

        LoginUserRequest loginRequest = LoginUserRequest.builder()
                .username(createRequest.getUsername())
                .password("")
                .build();

        new LoginUserRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsUnauthorized())
                .post(loginRequest);
    }

    @Test
    public void userCannotLoginWithEmptyCredentialsTest() {
        LoginUserRequest request = LoginUserRequest.builder()
                .username("")
                .password("")
                .build();

        new LoginUserRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsUnauthorized())
                .post(request);
    }
}