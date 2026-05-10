package iteration1.api;
import configs.Config;
import generators.RandomData;
import models.CreateUserRequest;
import models.UserRole;
import org.junit.jupiter.api.Test;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;

public class LoginUserTest extends BaseTest {

    @Test
    public void adminCanGenerateAuthTokenTest() {
        String token = UserSteps.loginAndGetToken(Config.getAdminUsername(), Config.getAdminPassword());
        softly.assertThat(token).isNotNull();
        softly.assertThat(token).startsWith("Basic ");
    }

    @Test
    public void userCanGenerateAuthTokenTest() {
        CreateUserRequest createRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        AdminSteps.createUser(createRequest);

        String token = UserSteps.loginAndGetToken(createRequest.getUsername(), createRequest.getPassword());

        softly.assertThat(token).isNotNull();
        softly.assertThat(token).startsWith("Basic ");
    }


    @Test
    public void userCannotLoginWithWrongPasswordTest() {
        CreateUserRequest createRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        AdminSteps.createUser(createRequest);

        UserSteps.loginAndExpectUnauthorized(createRequest.getUsername(), "WrongPassword123!");
    }

    @Test
    public void userCannotLoginWithNonExistentUsernameTest() {
        UserSteps.loginAndExpectUnauthorized("nonexistentuser123", "AnyPassword123!");
    }

    @Test
    public void userCannotLoginWithEmptyUsernameTest() {
        UserSteps.loginAndExpectUnauthorized("", RandomData.getPassword());
    }

    @Test
    public void userCannotLoginWithEmptyPasswordTest() {
        CreateUserRequest createRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        AdminSteps.createUser(createRequest);

        UserSteps.loginAndExpectUnauthorized(createRequest.getUsername(), "");
    }

    @Test
    public void userCannotLoginWithEmptyCredentialsTest() {
        UserSteps.loginAndExpectUnauthorized("", "");
    }
}