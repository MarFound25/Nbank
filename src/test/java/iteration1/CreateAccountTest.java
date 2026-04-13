package iteration1;

import generators.RandomData;
import models.*;
import org.junit.jupiter.api.Test;
import requests.*;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.List;

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

        CreateAccountResponse accountResponse = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .createAccount();

        softly.assertThat(accountResponse.getId()).isNotNull();
        softly.assertThat(accountResponse.getBalance()).isEqualTo(0.0);
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

        CreateAccountResponse account1 = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .createAccount();

        CreateAccountResponse account2 = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .createAccount();

        List<Account> accounts = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();

        softly.assertThat(accounts)
                .extracting(Account::getId)
                .contains(account1.getId(), account2.getId());
        softly.assertThat(accounts).hasSize(2);
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

        CreateAccountResponse account1 = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .createAccount();

        CreateAccountResponse account2 = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .createAccount();

        List<Account> accounts = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();

        softly.assertThat(accounts)
                .extracting(Account::getId)
                .contains(account1.getId(), account2.getId());
        softly.assertThat(accounts).hasSize(2);
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

        CreateAccountResponse user1Account = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken1),
                ResponseSpecs.entityWasCreated())
                .createAccount();

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

        List<Account> accounts = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken2),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();

        softly.assertThat(accounts)
                .extracting(Account::getId)
                .doesNotContain(user1Account.getId());
        softly.assertThat(accounts).isEmpty();
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

        CreateAccountResponse account = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .createAccount();

        new DepositRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .post(account.getId(), 1000.00);

        List<Transaction> transactions = new GetAccountTransactionsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getTransactions(account.getId());

        softly.assertThat(transactions).isNotNull();
        softly.assertThat(transactions).hasSizeGreaterThanOrEqualTo(1);
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

        CreateAccountResponse user1Account = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken1),
                ResponseSpecs.entityWasCreated())
                .createAccount();

        new DepositRequester(
                RequestSpecs.authWithBearerToken(authToken1),
                ResponseSpecs.requestReturnsOK())
                .post(user1Account.getId(), 500.00);

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
                .get(user1Account.getId());
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