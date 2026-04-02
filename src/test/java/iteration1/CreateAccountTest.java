package iteration1;

import generators.RandomData;
import models.CreateUserRequest;
import models.LoginUserRequest;
import models.UserRole;
import org.junit.jupiter.api.Test;
import requests.*;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import static org.hamcrest.Matchers.*;

public class CreateAccountTest extends BaseTest {

    private LoginUserRequest toLoginRequest(CreateUserRequest createRequest) {
        return LoginUserRequest.builder()
                .username(createRequest.getUsername())
                .password(createRequest.getPassword())
                .build();
    }


    @Test
    public void userCanCreateAccountTest() {
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
                .getToken(loginRequest);

        new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .body("id", notNullValue())
                .body("balance", is(0.0f));
    }

    @Test
    public void userCanCreateMultipleAccountsTest() {
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
                .getToken(loginRequest);

        Integer account1 = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .jsonPath()
                .getInt("id");

        Integer account2 = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .jsonPath()
                .getInt("id");

        new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .get()
                .body("id", hasItems(account1, account2))
                .body("size()", is(2));
    }

    @Test
    public void userCanViewOwnAccountsTest() {
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
                .getToken(loginRequest);

        Integer account1 = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .jsonPath()
                .getInt("id");

        Integer account2 = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .jsonPath()
                .getInt("id");

        new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .get()
                .body("find { it.id == " + account1 + " }", notNullValue())
                .body("find { it.id == " + account2 + " }", notNullValue())
                .body("size()", is(2));
    }

    @Test
    public void userCannotViewAnotherUsersAccountsTest() {
        CreateUserRequest user1Request = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(user1Request);

        LoginUserRequest loginRequest1 = toLoginRequest(user1Request);

        String authToken1 = new LoginUserRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsOK())
                .getToken(loginRequest1);

        Integer user1Account = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken1),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .jsonPath()
                .getInt("id");

        CreateUserRequest user2Request = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(user2Request);

        LoginUserRequest loginRequest2 = toLoginRequest(user2Request);

        String authToken2 = new LoginUserRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsOK())
                .getToken(loginRequest2);

        new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken2),
                ResponseSpecs.requestReturnsOK())
                .get()
                .body("find { it.id == " + user1Account + " }", nullValue())
                .body("size()", is(0));
    }



    @Test
    public void userCanGetAccountTransactionsTest() {
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
                .getToken(loginRequest);

        Integer accountId = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .jsonPath()
                .getInt("id");

        new DepositRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .post(accountId, 1000.00);

        new GetAccountTransactionsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .get(accountId)
                .body("$", notNullValue())
                .body("size()", greaterThanOrEqualTo(1));
    }

    @Test
    public void userCannotGetAnotherUsersAccountTransactionsTest() {
        CreateUserRequest user1Request = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(user1Request);

        LoginUserRequest loginRequest1 = toLoginRequest(user1Request);

        String authToken1 = new LoginUserRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsOK())
                .getToken(loginRequest1);

        Integer user1Account = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken1),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .jsonPath()
                .getInt("id");

        new DepositRequester(
                RequestSpecs.authWithBearerToken(authToken1),
                ResponseSpecs.requestReturnsOK())
                .post(user1Account, 500.00);

        CreateUserRequest user2Request = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(user2Request);

        LoginUserRequest loginRequest2 = toLoginRequest(user2Request);

        String authToken2 = new LoginUserRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsOK())
                .getToken(loginRequest2);

        new GetAccountTransactionsRequester(
                RequestSpecs.authWithBearerToken(authToken2),
                ResponseSpecs.requestReturnsForbidden())
                .get(user1Account);
    }

    @Test
    public void userCannotGetTransactionsForNonexistentAccountTest() {
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
                .getToken(loginRequest);

        new GetAccountTransactionsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsForbidden())
                .get(999999);
    }

    @Test
    public void userCannotGetTransactionsWithoutAuthTest() {
        new GetAccountTransactionsRequester(
                RequestSpecs.noAuthSpec(),
                ResponseSpecs.requestReturnsUnauthorized())
                .get(1);
    }

    @Test
    public void userCannotGetTransactionsWithInvalidTokenTest() {
        new GetAccountTransactionsRequester(
                RequestSpecs.authWithBearerToken("invalid.token.here"),
                ResponseSpecs.requestReturnsUnauthorized())
                .get(1);
    }

    @Test
    public void userCannotGetTransactionsWithInvalidBasicAuthTest() {
        new GetAccountTransactionsRequester(
                RequestSpecs.authWithBasic("invalid"),
                ResponseSpecs.requestReturnsUnauthorized())
                .get(1);
    }

    @Test
    public void userCannotGetTransactionsWithEmptyTokenTest() {
        new GetAccountTransactionsRequester(
                RequestSpecs.authWithBearerToken(""),
                ResponseSpecs.requestReturnsUnauthorized())
                .get(1);
    }
}