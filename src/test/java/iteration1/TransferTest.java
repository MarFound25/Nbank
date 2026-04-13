package iteration1;

import generators.RandomData;
import models.*;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.*;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.within;

public class TransferTest extends BaseTest {

    private LoginUserRequest toLoginRequest(CreateUserRequest createRequest) {
        return LoginUserRequest.builder()
                .username(createRequest.getUsername())
                .password(createRequest.getPassword())
                .build();
    }

    private String createUserAndGetAuth(String prefix) {
        String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        String username = prefix + uniqueSuffix;

        if (username.length() > 15) {
            username = username.substring(0, 15);
        }

        CreateUserRequest createRequest = CreateUserRequest.builder()
                .username(username)
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
    @MethodSource("provideValidTransferData")
    public void userCanTransferValidAmountsTest(double depositAmount, double transferAmount) {
        String authToken = createUserAndGetAuth("Tr");

        CreateAccountResponse fromAccount = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .createAccount();

        CreateAccountResponse toAccount = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .createAccount();

        new DepositRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .post(fromAccount.getId(), depositAmount);

        List<Account> accountsAfterDeposit = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();

        Account fromAccountData = accountsAfterDeposit.stream()
                .filter(a -> a.getId().equals(fromAccount.getId()))
                .findFirst()
                .orElseThrow();

        Account toAccountData = accountsAfterDeposit.stream()
                .filter(a -> a.getId().equals(toAccount.getId()))
                .findFirst()
                .orElseThrow();

        double fromInitialBalance = fromAccountData.getBalance();
        double toInitialBalance = toAccountData.getBalance();

        new TransferRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .post(fromAccount.getId(), toAccount.getId(), transferAmount);

        List<Account> accountsAfterTransfer = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();

        Account fromAccountAfter = accountsAfterTransfer.stream()
                .filter(a -> a.getId().equals(fromAccount.getId()))
                .findFirst()
                .orElseThrow();

        Account toAccountAfter = accountsAfterTransfer.stream()
                .filter(a -> a.getId().equals(toAccount.getId()))
                .findFirst()
                .orElseThrow();

        softly.assertThat(fromAccountAfter.getBalance())
                .isCloseTo(fromInitialBalance - transferAmount, within(0.01));
        softly.assertThat(toAccountAfter.getBalance())
                .isCloseTo(toInitialBalance + transferAmount, within(0.01));
    }

    private static Stream<Arguments> provideValidTransferData() {
        return Stream.of(
                Arguments.of(1000.0, 300.0),
                Arguments.of(500.0, 500.0),
                Arguments.of(5000.0, 1.0),
                Arguments.of(2000.0, 1999.99)
        );
    }

    @ParameterizedTest
    @MethodSource("provideValidTransferToAnotherUserData")
    public void userCanTransferToAnotherUserTest(double depositAmount, double transferAmount) {
        String user1Auth = createUserAndGetAuth("Sd");
        String user2Auth = createUserAndGetAuth("Rc");

        CreateAccountResponse fromAccount = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(user1Auth),
                ResponseSpecs.entityWasCreated())
                .createAccount();

        CreateAccountResponse toAccount = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(user2Auth),
                ResponseSpecs.entityWasCreated())
                .createAccount();

        new DepositRequester(
                RequestSpecs.authWithBearerToken(user1Auth),
                ResponseSpecs.requestReturnsOK())
                .post(fromAccount.getId(), depositAmount);

        List<Account> user1Accounts = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(user1Auth),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();

        Account fromAccountData = user1Accounts.stream()
                .filter(a -> a.getId().equals(fromAccount.getId()))
                .findFirst()
                .orElseThrow();

        List<Account> user2Accounts = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(user2Auth),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();

        Account toAccountData = user2Accounts.stream()
                .filter(a -> a.getId().equals(toAccount.getId()))
                .findFirst()
                .orElseThrow();

        double fromInitialBalance = fromAccountData.getBalance();
        double toInitialBalance = toAccountData.getBalance();

        new TransferRequester(
                RequestSpecs.authWithBearerToken(user1Auth),
                ResponseSpecs.requestReturnsOK())
                .post(fromAccount.getId(), toAccount.getId(), transferAmount);

        List<Account> user1AccountsAfter = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(user1Auth),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();

        List<Account> user2AccountsAfter = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(user2Auth),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();

        Account fromAccountAfter = user1AccountsAfter.stream()
                .filter(a -> a.getId().equals(fromAccount.getId()))
                .findFirst()
                .orElseThrow();

        Account toAccountAfter = user2AccountsAfter.stream()
                .filter(a -> a.getId().equals(toAccount.getId()))
                .findFirst()
                .orElseThrow();

        softly.assertThat(fromAccountAfter.getBalance())
                .isCloseTo(fromInitialBalance - transferAmount, within(0.01));
        softly.assertThat(toAccountAfter.getBalance())
                .isCloseTo(toInitialBalance + transferAmount, within(0.01));
    }

    private static Stream<Arguments> provideValidTransferToAnotherUserData() {
        return Stream.of(
                Arguments.of(1000.0, 400.0),
                Arguments.of(2000.0, 1000.0),
                Arguments.of(500.0, 250.0)
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidTransferData")
    public void userCannotMakeInvalidTransferTest(double amount, String accountType, int expectedStatusCode) {
        String authToken = createUserAndGetAuth("TN");

        CreateAccountResponse fromAccount = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .createAccount();

        new DepositRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .post(fromAccount.getId(), 1000.0);

        List<Account> accounts = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();

        Account fromAccountData = accounts.stream()
                .filter(a -> a.getId().equals(fromAccount.getId()))
                .findFirst()
                .orElseThrow();

        double fromInitialBalance = fromAccountData.getBalance();

        Integer targetAccountId;
        double toInitialBalance = 0.0;
        String targetAuthToken = null;

        switch (accountType) {
            case "valid":
                CreateAccountResponse targetAccount = new CreateAccountRequester(
                        RequestSpecs.authWithBearerToken(authToken),
                        ResponseSpecs.entityWasCreated())
                        .createAccount();
                targetAccountId = targetAccount.getId();
                toInitialBalance = targetAccount.getBalance();
                targetAuthToken = authToken;
                break;
            case "non-existent-acc":
                targetAccountId = 999999;
                break;
            default:
                targetAccountId = fromAccount.getId();
        }

        new TransferRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.custom(expectedStatusCode))
                .post(fromAccount.getId(), targetAccountId, amount);

        List<Account> accountsAfter = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();

        Account fromAccountAfter = accountsAfter.stream()
                .filter(a -> a.getId().equals(fromAccount.getId()))
                .findFirst()
                .orElseThrow();

        softly.assertThat(fromAccountAfter.getBalance()).isEqualTo(fromInitialBalance);

        if ("valid".equals(accountType)) {
            List<Account> targetAccounts = new GetCustomerAccountsRequester(
                    RequestSpecs.authWithBearerToken(targetAuthToken),
                    ResponseSpecs.requestReturnsOK())
                    .getAccounts();

            Account targetAccountAfter = targetAccounts.stream()
                    .filter(a -> a.getId().equals(targetAccountId))
                    .findFirst()
                    .orElseThrow();

            softly.assertThat(targetAccountAfter.getBalance()).isEqualTo(toInitialBalance);
        }
    }

    private static Stream<Arguments> provideInvalidTransferData() {
        return Stream.of(
                Arguments.of(-50.0, "valid", HttpStatus.SC_BAD_REQUEST),
                Arguments.of(0.0, "valid", HttpStatus.SC_BAD_REQUEST),
                Arguments.of(10001.0, "valid", HttpStatus.SC_BAD_REQUEST),
                Arguments.of(999999.0, "valid", HttpStatus.SC_BAD_REQUEST),
                Arguments.of(100.0, "non-existent-acc", HttpStatus.SC_BAD_REQUEST)
        );
    }


    @Test
    public void userCannotTransferFromAnotherUsersAccountTest() {
        String user1Auth = createUserAndGetAuth("U1");
        String user2Auth = createUserAndGetAuth("U2");

        CreateAccountResponse user1Account = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(user1Auth),
                ResponseSpecs.entityWasCreated())
                .createAccount();

        CreateAccountResponse user2Account = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(user2Auth),
                ResponseSpecs.entityWasCreated())
                .createAccount();

        new DepositRequester(
                RequestSpecs.authWithBearerToken(user1Auth),
                ResponseSpecs.requestReturnsOK())
                .post(user1Account.getId(), 1000.0);

        List<Account> user1Accounts = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(user1Auth),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();

        List<Account> user2Accounts = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(user2Auth),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();

        Account user1AccountData = user1Accounts.stream()
                .filter(a -> a.getId().equals(user1Account.getId()))
                .findFirst()
                .orElseThrow();

        Account user2AccountData = user2Accounts.stream()
                .filter(a -> a.getId().equals(user2Account.getId()))
                .findFirst()
                .orElseThrow();

        double user1InitialBalance = user1AccountData.getBalance();
        double user2InitialBalance = user2AccountData.getBalance();

        new TransferRequester(
                RequestSpecs.authWithBearerToken(user2Auth),
                ResponseSpecs.requestReturnsForbidden())
                .post(user1Account.getId(), user2Account.getId(), 100.00);

        List<Account> user1AccountsAfter = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(user1Auth),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();

        List<Account> user2AccountsAfter = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(user2Auth),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();

        Account user1AccountAfter = user1AccountsAfter.stream()
                .filter(a -> a.getId().equals(user1Account.getId()))
                .findFirst()
                .orElseThrow();

        Account user2AccountAfter = user2AccountsAfter.stream()
                .filter(a -> a.getId().equals(user2Account.getId()))
                .findFirst()
                .orElseThrow();

        softly.assertThat(user1AccountAfter.getBalance()).isEqualTo(user1InitialBalance);
        softly.assertThat(user2AccountAfter.getBalance()).isEqualTo(user2InitialBalance);
    }

    @Test
    public void userCanTransferMaxLimitAmountTest() {
        String authToken = createUserAndGetAuth("Tr");

        CreateAccountResponse fromAccount = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .createAccount();

        CreateAccountResponse toAccount = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .createAccount();

        new DepositRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .post(fromAccount.getId(), 5000.00);

        new DepositRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .post(fromAccount.getId(), 5000.00);

        new DepositRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .post(fromAccount.getId(), 5000.00);

        List<Account> accounts = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();

        Account fromAccountData = accounts.stream()
                .filter(a -> a.getId().equals(fromAccount.getId()))
                .findFirst()
                .orElseThrow();

        Account toAccountData = accounts.stream()
                .filter(a -> a.getId().equals(toAccount.getId()))
                .findFirst()
                .orElseThrow();

        double fromInitialBalance = fromAccountData.getBalance();
        double toInitialBalance = toAccountData.getBalance();

        new TransferRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .post(fromAccount.getId(), toAccount.getId(), 10000.00);

        List<Account> accountsAfter = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();

        Account fromAccountAfter = accountsAfter.stream()
                .filter(a -> a.getId().equals(fromAccount.getId()))
                .findFirst()
                .orElseThrow();

        Account toAccountAfter = accountsAfter.stream()
                .filter(a -> a.getId().equals(toAccount.getId()))
                .findFirst()
                .orElseThrow();

        softly.assertThat(fromAccountAfter.getBalance())
                .isCloseTo(fromInitialBalance - 10000.0, within(0.01));
        softly.assertThat(toAccountAfter.getBalance())
                .isCloseTo(toInitialBalance + 10000.0, within(0.01));
    }

    @Test
    public void userCannotTransferAboveLimitTest() {
        String authToken = createUserAndGetAuth("Tr");

        CreateAccountResponse fromAccount = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .createAccount();

        CreateAccountResponse toAccount = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .createAccount();

        new DepositRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .post(fromAccount.getId(), 5000.00);

        new DepositRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .post(fromAccount.getId(), 5000.00);

        new DepositRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .post(fromAccount.getId(), 5000.00);

        List<Account> accounts = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();

        Account fromAccountData = accounts.stream()
                .filter(a -> a.getId().equals(fromAccount.getId()))
                .findFirst()
                .orElseThrow();

        Account toAccountData = accounts.stream()
                .filter(a -> a.getId().equals(toAccount.getId()))
                .findFirst()
                .orElseThrow();

        double fromInitialBalance = fromAccountData.getBalance();
        double toInitialBalance = toAccountData.getBalance();

        new TransferRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsBadRequest())
                .post(fromAccount.getId(), toAccount.getId(), 10000.01);

        List<Account> accountsAfter = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getAccounts();

        Account fromAccountAfter = accountsAfter.stream()
                .filter(a -> a.getId().equals(fromAccount.getId()))
                .findFirst()
                .orElseThrow();

        Account toAccountAfter = accountsAfter.stream()
                .filter(a -> a.getId().equals(toAccount.getId()))
                .findFirst()
                .orElseThrow();

        softly.assertThat(fromAccountAfter.getBalance()).isEqualTo(fromInitialBalance);
        softly.assertThat(toAccountAfter.getBalance()).isEqualTo(toInitialBalance);
    }


    private static Stream<Arguments> provideUnauthorizedData() {
        return Stream.of(
                Arguments.of("", HttpStatus.SC_UNAUTHORIZED),
                Arguments.of("Bearer invalid.token", HttpStatus.SC_UNAUTHORIZED),
                Arguments.of("Basic invalid", HttpStatus.SC_UNAUTHORIZED)
        );
    }

    @ParameterizedTest
    @MethodSource("provideUnauthorizedData")
    public void userCannotTransferWithoutAuthTest(String authHeader, int expectedStatusCode) {
        new TransferRequester(
                RequestSpecs.customAuth(authHeader),
                ResponseSpecs.custom(expectedStatusCode))
                .post(1, 2, 100.00);
    }
}