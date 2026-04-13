package iteration1;

import generators.RandomData;
import models.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.*;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.List;
import java.util.stream.Stream;

public class DepositTest extends BaseTest {

    private LoginUserRequest toLoginRequest(CreateUserRequest createRequest) {
        return LoginUserRequest.builder()
                .username(createRequest.getUsername())
                .password(createRequest.getPassword())
                .build();
    }

    private String createUserAndGetAuth() {
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

        return new LoginUserRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsOK())
                .getToken(loginRequest);
    }

    @ParameterizedTest
    @MethodSource("provideValidDepositData")
    public void userCanDepositValidAmountsTest(double amount) {
        String authToken = createUserAndGetAuth();

        CreateAccountResponse account = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .createAccount();

        double initialBalance = account.getBalance();

        new DepositRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .post(account.getId(), amount);

        List<Account> accounts = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();

        Account updatedAccount = accounts.stream()
                .filter(a -> a.getId().equals(account.getId()))
                .findFirst()
                .orElse(null);

        softly.assertThat(updatedAccount).isNotNull();
        softly.assertThat(updatedAccount.getBalance()).isEqualTo(initialBalance + amount);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidDepositData")
    public void userCannotMakeInvalidDepositTest(double amount, String accountType, int expectedStatusCode) {
        String authToken = createUserAndGetAuth();

        CreateAccountResponse ownAccount = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .createAccount();

        double ownInitialBalance = ownAccount.getBalance();

        Integer targetAccountId;
        double targetInitialBalance = 0.0;
        String targetAuthToken = null;

        switch (accountType) {
            case "own":
                targetAccountId = ownAccount.getId();
                targetInitialBalance = ownInitialBalance;
                targetAuthToken = authToken;
                break;
            case "someone-elses-acc":
                targetAuthToken = createUserAndGetAuth();
                CreateAccountResponse otherAccount = new CreateAccountRequester(
                        RequestSpecs.authWithBearerToken(targetAuthToken),
                        ResponseSpecs.entityWasCreated())
                        .createAccount();
                targetAccountId = otherAccount.getId();
                targetInitialBalance = otherAccount.getBalance();
                break;
            case "non-existent-acc":
                targetAccountId = 999999;
                break;
            default:
                targetAccountId = ownAccount.getId();
        }

        new DepositRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.custom(expectedStatusCode))
                .post(targetAccountId, amount);

        List<Account> accounts = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();

        Account ownAccountAfter = accounts.stream()
                .filter(a -> a.getId().equals(ownAccount.getId()))
                .findFirst()
                .orElse(null);

        softly.assertThat(ownAccountAfter.getBalance()).isEqualTo(ownInitialBalance);

        if ("own".equals(accountType) || "someone-elses-acc".equals(accountType)) {
            List<Account> targetAccounts = new GetCustomerAccountsRequester(
                    RequestSpecs.authWithBearerToken(targetAuthToken),
                    ResponseSpecs.requestReturnsOK())
                    .getAccounts();

            Account targetAccountAfter = targetAccounts.stream()
                    .filter(a -> a.getId().equals(targetAccountId))
                    .findFirst()
                    .orElse(null);

            softly.assertThat(targetAccountAfter.getBalance()).isEqualTo(targetInitialBalance);
        }
    }

    @Test
    public void userCanDepositMaxLimitAmountTest() {
        String authToken = createUserAndGetAuth();

        CreateAccountResponse account = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .createAccount();

        double initialBalance = account.getBalance();

        new DepositRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .post(account.getId(), 5000.00);

        List<Account> accounts = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();

        Account updatedAccount = accounts.stream()
                .filter(a -> a.getId().equals(account.getId()))
                .findFirst()
                .orElse(null);

        softly.assertThat(updatedAccount.getBalance()).isEqualTo(initialBalance + 5000.0);
    }

    @Test
    public void userCannotDepositAboveLimitTest() {
        String authToken = createUserAndGetAuth();

        CreateAccountResponse account = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .createAccount();

        double initialBalance = account.getBalance();

        new DepositRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsBadRequest())
                .post(account.getId(), 5000.01);

        List<Account> accounts = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();

        Account updatedAccount = accounts.stream()
                .filter(a -> a.getId().equals(account.getId()))
                .findFirst()
                .orElse(null);

        softly.assertThat(updatedAccount.getBalance()).isEqualTo(initialBalance);
    }

    private static Stream<Arguments> provideValidDepositData() {
        return Stream.of(
                Arguments.of(1.00),
                Arguments.of(100.00),
                Arguments.of(999.99),
                Arguments.of(1000.00),
                Arguments.of(5000.00)
        );
    }

    private static Stream<Arguments> provideInvalidDepositData() {
        return Stream.of(
                Arguments.of(-100.00, "own", 400),
                Arguments.of(0.00, "own", 400),
                Arguments.of(5001.00, "own", 400),
                Arguments.of(100.00, "someone-elses-acc", 403),
                Arguments.of(100.00, "non-existent-acc", 403)
        );
    }
}