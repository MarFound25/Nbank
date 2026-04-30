package iteration1;

import generators.RandomData;
import models.*;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.within;

public class TransferTest extends BaseTest {

    private String createUserAndGetAuth(String prefix) {
        String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        String username = prefix + uniqueSuffix;

        if (username.length() > 15) {
            username = username.substring(0, 15);
        }

        CreateUserRequest createRequest = CreateUserRequest.builder()
                .username(username)
                .password(RandomData.getPassword())
                .name("Test User")
                .role(UserRole.USER.toString())
                .build();

        AdminSteps.createUser(createRequest);
        return UserSteps.loginAndGetToken(createRequest.getUsername(), createRequest.getPassword());
    }

    private double getAccountBalance(String token, long accountId) {
        List<Account> accounts = UserSteps.getAccounts(token);
        return accounts.stream()
                .filter(a -> a.getId() == accountId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Account not found: " + accountId))
                .getBalance();
    }

    private void transfer(String token, int fromAccount, int toAccount, double amount) {
        new requests.skelethon.requesters.CrudRequesters(
                RequestSpecs.authWithToken(token),
                ResponseSpecs.requestReturnsOK())
                .create(endpoints.Endpoint.ACCOUNTS_TRANSFER, new TransferRequest(fromAccount, toAccount, amount));
    }

    private void transferAndExpectError(String token, int fromAccount, int toAccount, double amount, int expectedStatusCode) {
        new requests.skelethon.requesters.CrudRequesters(
                RequestSpecs.authWithToken(token),
                ResponseSpecs.custom(expectedStatusCode))
                .create(endpoints.Endpoint.ACCOUNTS_TRANSFER, new TransferRequest(fromAccount, toAccount, amount));
    }

    @ParameterizedTest
    @MethodSource("provideValidTransferData")
    public void userCanTransferValidAmountsTest(double depositAmount, double transferAmount) {
        String token = createUserAndGetAuth("Tr");

        int fromAccount = UserSteps.createAccount(token);
        int toAccount = UserSteps.createAccount(token);

        UserSteps.deposit(token, fromAccount, depositAmount);

        double fromInitialBalance = getAccountBalance(token, fromAccount);
        double toInitialBalance = getAccountBalance(token, toAccount);

        transfer(token, fromAccount, toAccount, transferAmount);

        double fromFinalBalance = getAccountBalance(token, fromAccount);
        double toFinalBalance = getAccountBalance(token, toAccount);

        softly.assertThat(fromFinalBalance).isCloseTo(fromInitialBalance - transferAmount, within(0.01));
        softly.assertThat(toFinalBalance).isCloseTo(toInitialBalance + transferAmount, within(0.01));
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
        String user1Token = createUserAndGetAuth("Sd");
        String user2Token = createUserAndGetAuth("Rc");

        int fromAccount = UserSteps.createAccount(user1Token);
        int toAccount = UserSteps.createAccount(user2Token);

        UserSteps.deposit(user1Token, fromAccount, depositAmount);

        double fromInitialBalance = getAccountBalance(user1Token, fromAccount);
        double toInitialBalance = getAccountBalance(user2Token, toAccount);

        transfer(user1Token, fromAccount, toAccount, transferAmount);

        double fromFinalBalance = getAccountBalance(user1Token, fromAccount);
        double toFinalBalance = getAccountBalance(user2Token, toAccount);

        softly.assertThat(fromFinalBalance).isCloseTo(fromInitialBalance - transferAmount, within(0.01));
        softly.assertThat(toFinalBalance).isCloseTo(toInitialBalance + transferAmount, within(0.01));
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
    public void userCannotTransferInvalidAmountsTest(double amount, int expectedStatusCode) {
        String token = createUserAndGetAuth("TN");

        int fromAccount = UserSteps.createAccount(token);
        int toAccount = UserSteps.createAccount(token);

        UserSteps.deposit(token, fromAccount, 1000.0);

        double fromInitialBalance = getAccountBalance(token, fromAccount);
        double toInitialBalance = getAccountBalance(token, toAccount);

        transferAndExpectError(token, fromAccount, toAccount, amount, expectedStatusCode);

        double fromFinalBalance = getAccountBalance(token, fromAccount);
        double toFinalBalance = getAccountBalance(token, toAccount);

        softly.assertThat(fromFinalBalance).isEqualTo(fromInitialBalance);
        softly.assertThat(toFinalBalance).isEqualTo(toInitialBalance);
    }

    private static Stream<Arguments> provideInvalidTransferData() {
        return Stream.of(
                Arguments.of(-50.0, HttpStatus.SC_BAD_REQUEST),
                Arguments.of(0.0, HttpStatus.SC_BAD_REQUEST),
                Arguments.of(10001.0, HttpStatus.SC_BAD_REQUEST),
                Arguments.of(999999.0, HttpStatus.SC_BAD_REQUEST)
        );
    }

    @Test
    public void userCannotTransferToNonExistentAccountTest() {
        String token = createUserAndGetAuth("TN");

        int fromAccount = UserSteps.createAccount(token);
        int nonExistentAccount = 999999;

        UserSteps.deposit(token, fromAccount, 1000.0);

        double fromInitialBalance = getAccountBalance(token, fromAccount);

        transferAndExpectError(token, fromAccount, nonExistentAccount, 100.0, HttpStatus.SC_BAD_REQUEST);

        double fromFinalBalance = getAccountBalance(token, fromAccount);
        softly.assertThat(fromFinalBalance).isEqualTo(fromInitialBalance);
    }

    @Test
    public void userCannotTransferFromAnotherUsersAccountTest() {
        String user1Token = createUserAndGetAuth("U1");
        String user2Token = createUserAndGetAuth("U2");

        int user1Account = UserSteps.createAccount(user1Token);
        int user2Account = UserSteps.createAccount(user2Token);

        UserSteps.deposit(user1Token, user1Account, 1000.0);

        double user1InitialBalance = getAccountBalance(user1Token, user1Account);
        double user2InitialBalance = getAccountBalance(user2Token, user2Account);

        transferAndExpectError(user2Token, user1Account, user2Account, 100.00, HttpStatus.SC_FORBIDDEN);

        double user1FinalBalance = getAccountBalance(user1Token, user1Account);
        double user2FinalBalance = getAccountBalance(user2Token, user2Account);

        softly.assertThat(user1FinalBalance).isEqualTo(user1InitialBalance);
        softly.assertThat(user2FinalBalance).isEqualTo(user2InitialBalance);
    }

    @Test
    public void userCanTransferMaxLimitAmountTest() {
        String token = createUserAndGetAuth("Tr");

        int fromAccount = UserSteps.createAccount(token);
        int toAccount = UserSteps.createAccount(token);

        UserSteps.deposit(token, fromAccount, 5000.00);
        UserSteps.deposit(token, fromAccount, 5000.00);
        UserSteps.deposit(token, fromAccount, 5000.00);

        double fromInitialBalance = getAccountBalance(token, fromAccount);
        double toInitialBalance = getAccountBalance(token, toAccount);

        transfer(token, fromAccount, toAccount, 10000.00);

        double fromFinalBalance = getAccountBalance(token, fromAccount);
        double toFinalBalance = getAccountBalance(token, toAccount);

        softly.assertThat(fromFinalBalance).isCloseTo(fromInitialBalance - 10000.0, within(0.01));
        softly.assertThat(toFinalBalance).isCloseTo(toInitialBalance + 10000.0, within(0.01));
    }

    @Test
    public void userCannotTransferAboveLimitTest() {
        String token = createUserAndGetAuth("Tr");

        int fromAccount = UserSteps.createAccount(token);
        int toAccount = UserSteps.createAccount(token);

        UserSteps.deposit(token, fromAccount, 5000.00);
        UserSteps.deposit(token, fromAccount, 5000.00);
        UserSteps.deposit(token, fromAccount, 5000.00);

        double fromInitialBalance = getAccountBalance(token, fromAccount);
        double toInitialBalance = getAccountBalance(token, toAccount);

        transferAndExpectError(token, fromAccount, toAccount, 10000.01, HttpStatus.SC_BAD_REQUEST);

        double fromFinalBalance = getAccountBalance(token, fromAccount);
        double toFinalBalance = getAccountBalance(token, toAccount);

        softly.assertThat(fromFinalBalance).isEqualTo(fromInitialBalance);
        softly.assertThat(toFinalBalance).isEqualTo(toInitialBalance);
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
        new requests.skelethon.requesters.CrudRequesters(
                RequestSpecs.customAuth(authHeader),
                ResponseSpecs.custom(expectedStatusCode))
                .create(endpoints.Endpoint.ACCOUNTS_TRANSFER, new TransferRequest(1, 2, 100.00));
    }
}