package iteration1.api;

import api.dao.AccountDao;
import api.dao.UserDao;
import configs.Config;
import requests.skelethon.Endpoint;
import generators.RandomData;
import models.*;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.skelethon.requesters.CrudRequesters;
import requests.steps.AdminSteps;
import requests.steps.DataBaseSteps;
import requests.steps.UserSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.within;

@DisplayName("Transfer Tests - API & Database Integration")
public class TransferTest extends BaseTest {


    private static Stream<Arguments> provideValidTransferData() {
        return Stream.of(
                Arguments.of(1000.0, 300.0),
                Arguments.of(500.0, 500.0),
                Arguments.of(5000.0, 1.0),
                Arguments.of(2000.0, 1999.99)
        );
    }

    private static Stream<Arguments> provideValidTransferToAnotherUserData() {
        return Stream.of(
                Arguments.of(1000.0, 400.0),
                Arguments.of(2000.0, 1000.0),
                Arguments.of(500.0, 250.0)
        );
    }

    private static Stream<Arguments> provideInvalidTransferData() {
        return Stream.of(
                Arguments.of(-50.0, HttpStatus.SC_BAD_REQUEST),
                Arguments.of(0.0, HttpStatus.SC_BAD_REQUEST),
                Arguments.of(10001.0, HttpStatus.SC_BAD_REQUEST),
                Arguments.of(999999.0, HttpStatus.SC_BAD_REQUEST)
        );
    }

    private static Stream<Arguments> provideUnauthorizedData() {
        return Stream.of(
                Arguments.of("", HttpStatus.SC_UNAUTHORIZED),
                Arguments.of("Bearer invalid.token", HttpStatus.SC_UNAUTHORIZED),
                Arguments.of("Basic invalid", HttpStatus.SC_UNAUTHORIZED)
        );
    }

    private static final double BALANCE_DELTA = Double.parseDouble(
            Config.getProperty("balance.delta", "0.01")
    );

    private String currentUsername1;
    private String currentUsername2;
    private Long currentUserId1;
    private Long currentUserId2;
    private Long currentFromAccountId;
    private Long currentToAccountId;

    private String createUserAndGetAuth(String prefix) {
        String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        String username = prefix + uniqueSuffix;

        if (username.length() > 15) {
            username = username.substring(0, 15);
        }

        String password = RandomData.getPassword();

        CreateUserRequest createRequest = CreateUserRequest.builder()
                .username(username)
                .password(password)
                .name("Test User")
                .role(UserRole.USER.toString())
                .build();

        AdminSteps.createUser(createRequest);

        UserDao userDao = DataBaseSteps.getUserByUsername(username);

        if (currentUsername1 == null) {
            currentUsername1 = username;
            currentUserId1 = userDao.getId();
            trackUser(currentUserId1);
        } else {
            currentUsername2 = username;
            currentUserId2 = userDao.getId();
            trackUser(currentUserId2);
        }

        return UserSteps.loginAndGetToken(username, password);
    }

    private int createAccountAndTrack(String token, Long userId) {
        int accountId = UserSteps.createAccount(token);
        AccountDao accountDao = DataBaseSteps.getAccountById((long) accountId);

        if (currentFromAccountId == null) {
            currentFromAccountId = accountDao.getId();
        } else {
            currentToAccountId = accountDao.getId();
        }

        trackAccount(accountDao.getId());
        return accountId;
    }

    private double getAccountBalanceFromDb(long accountId) {
        AccountDao accountDao = DataBaseSteps.getAccountById(accountId);
        return accountDao.getBalance();
    }

    private void transfer(String token, int fromAccount, int toAccount, double amount) {
        new CrudRequesters(
                RequestSpecs.authWithToken(token),
                ResponseSpecs.requestReturnsOK())
                .create(Endpoint.ACCOUNTS_TRANSFER, new TransferRequest(fromAccount, toAccount, amount));
    }

    private void transferAndExpectError(String token, int fromAccount, int toAccount, double amount, int expectedStatusCode) {
        new CrudRequesters(
                RequestSpecs.authWithToken(token),
                ResponseSpecs.custom(expectedStatusCode))
                .create(Endpoint.ACCOUNTS_TRANSFER, new TransferRequest(fromAccount, toAccount, amount));
    }

    @Nested
    @DisplayName("Positive Scenarios with Database Verification")
    class PositiveTests {

        @ParameterizedTest
        @MethodSource("iteration1.api.TransferTest#provideValidTransferData")
        @DisplayName("TC-TRF-001: User can transfer between own accounts - DB verification")
        void userCanTransferValidAmountsTest(double depositAmount, double transferAmount) {
            String token = createUserAndGetAuth("Tr");
            int fromAccount = createAccountAndTrack(token, currentUserId1);
            int toAccount = createAccountAndTrack(token, currentUserId1);

            UserSteps.deposit(token, fromAccount, depositAmount);

            double fromInitialBalance = getAccountBalanceFromDb(fromAccount);
            double toInitialBalance = getAccountBalanceFromDb(toAccount);

            transfer(token, fromAccount, toAccount, transferAmount);

            AccountDao fromAccountAfter = DataBaseSteps.getAccountById((long) fromAccount);
            AccountDao toAccountAfter = DataBaseSteps.getAccountById((long) toAccount);

            softly.assertThat(fromAccountAfter.getBalance())
                    .isCloseTo(fromInitialBalance - transferAmount, within(BALANCE_DELTA));
            softly.assertThat(toAccountAfter.getBalance())
                    .isCloseTo(toInitialBalance + transferAmount, within(BALANCE_DELTA));
        }

        @ParameterizedTest
        @MethodSource("iteration1.api.TransferTest#provideValidTransferToAnotherUserData")
        @DisplayName("TC-TRF-002: User can transfer to another user - DB verification")
        void userCanTransferToAnotherUserTest(double depositAmount, double transferAmount) {
            String user1Token = createUserAndGetAuth("Sd");
            int fromAccount = createAccountAndTrack(user1Token, currentUserId1);

            String user2Token = createUserAndGetAuth("Rc");
            int toAccount = createAccountAndTrack(user2Token, currentUserId2);

            UserSteps.deposit(user1Token, fromAccount, depositAmount);

            double fromInitialBalance = getAccountBalanceFromDb(fromAccount);
            double toInitialBalance = getAccountBalanceFromDb(toAccount);

            transfer(user1Token, fromAccount, toAccount, transferAmount);

            AccountDao fromAccountAfter = DataBaseSteps.getAccountById((long) fromAccount);
            AccountDao toAccountAfter = DataBaseSteps.getAccountById((long) toAccount);

            softly.assertThat(fromAccountAfter.getBalance())
                    .isCloseTo(fromInitialBalance - transferAmount, within(BALANCE_DELTA));
            softly.assertThat(toAccountAfter.getBalance())
                    .isCloseTo(toInitialBalance + transferAmount, within(BALANCE_DELTA));
        }

        @Test
        @DisplayName("TC-TRF-003: User can transfer maximum allowed amount (10000.00)")
        void userCanTransferMaxLimitAmountTest() {
            String token = createUserAndGetAuth("Tr");
            int fromAccount = createAccountAndTrack(token, currentUserId1);
            int toAccount = createAccountAndTrack(token, currentUserId1);

            UserSteps.deposit(token, fromAccount, 5000.00);
            UserSteps.deposit(token, fromAccount, 5000.00);
            UserSteps.deposit(token, fromAccount, 5000.00);

            double fromInitialBalance = getAccountBalanceFromDb(fromAccount);
            double toInitialBalance = getAccountBalanceFromDb(toAccount);

            transfer(token, fromAccount, toAccount, 10000.00);

            AccountDao fromAccountAfter = DataBaseSteps.getAccountById((long) fromAccount);
            AccountDao toAccountAfter = DataBaseSteps.getAccountById((long) toAccount);
            softly.assertThat(fromAccountAfter.getBalance())
                    .isCloseTo(fromInitialBalance - 10000.0, within(BALANCE_DELTA));
            softly.assertThat(toAccountAfter.getBalance())
                    .isCloseTo(toInitialBalance + 10000.0, within(BALANCE_DELTA));
        }
    }

    @Nested
    @DisplayName("Negative Scenarios - Validation with DB Verification")
    class NegativeTests {

        @ParameterizedTest
        @MethodSource("iteration1.api.TransferTest#provideInvalidTransferData")
        @DisplayName("TC-TRF-004: Invalid transfer amounts are rejected - DB unchanged")
        void userCannotTransferInvalidAmountsTest(double amount, int expectedStatusCode) {
            String token = createUserAndGetAuth("TN");
            int fromAccount = createAccountAndTrack(token, currentUserId1);
            int toAccount = createAccountAndTrack(token, currentUserId1);

            UserSteps.deposit(token, fromAccount, 1000.0);

            double fromInitialBalance = getAccountBalanceFromDb(fromAccount);
            double toInitialBalance = getAccountBalanceFromDb(toAccount);

            transferAndExpectError(token, fromAccount, toAccount, amount, expectedStatusCode);

            AccountDao fromAccountAfter = DataBaseSteps.getAccountById((long) fromAccount);
            AccountDao toAccountAfter = DataBaseSteps.getAccountById((long) toAccount);
            softly.assertThat(fromAccountAfter.getBalance()).isEqualTo(fromInitialBalance);
            softly.assertThat(toAccountAfter.getBalance()).isEqualTo(toInitialBalance);
        }
    }

    @Nested
    @DisplayName("Security & Authorization Tests")
    class SecurityTests {

        @Test
        @DisplayName("TC-TRF-005: Cannot transfer from another user's account - security isolation")
        void userCannotTransferFromAnotherUsersAccountTest() {
            String user1Token = createUserAndGetAuth("U1");
            int user1Account = createAccountAndTrack(user1Token, currentUserId1);
            UserSteps.deposit(user1Token, user1Account, 1000.0);
            double user1InitialBalance = getAccountBalanceFromDb(user1Account);

            String user2Token = createUserAndGetAuth("U2");
            int user2Account = createAccountAndTrack(user2Token, currentUserId2);
            double user2InitialBalance = getAccountBalanceFromDb(user2Account);

            transferAndExpectError(user2Token, user1Account, user2Account, 100.00, HttpStatus.SC_FORBIDDEN);

            AccountDao user1AccountAfter = DataBaseSteps.getAccountById((long) user1Account);
            AccountDao user2AccountAfter = DataBaseSteps.getAccountById((long) user2Account);
            softly.assertThat(user1AccountAfter.getBalance()).isEqualTo(user1InitialBalance);
            softly.assertThat(user2AccountAfter.getBalance()).isEqualTo(user2InitialBalance);
        }

        @ParameterizedTest
        @MethodSource("iteration1.api.TransferTest#provideUnauthorizedData")
        @DisplayName("TC-TRF-006: Unauthorized transfer requests are rejected")
        void userCannotTransferWithoutAuthTest(String authHeader, int expectedStatusCode) {
            new CrudRequesters(
                    RequestSpecs.customAuth(authHeader),
                    ResponseSpecs.custom(expectedStatusCode))
                    .create(Endpoint.ACCOUNTS_TRANSFER, new TransferRequest(1, 2, 100.00));
        }
    }
}