package iteration1.api;

import api.dao.AccountDao;
import api.dao.UserDao;
import configs.Config;
import generators.RandomData;
import models.CreateUserRequest;
import models.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.steps.AdminSteps;
import requests.steps.DataBaseSteps;
import requests.steps.UserSteps;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.within;

@DisplayName("Deposit Tests - API & Database Integration")
public class DepositTest extends BaseTest {

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
                Arguments.of(-100.00),
                Arguments.of(0.00),
                Arguments.of(5001.00)
        );
    }

    private static final double BALANCE_DELTA = Double.parseDouble(
            Config.getProperty("balance.delta", "0.01")
    );

    private String currentUsername;
    private Long currentUserId;
    private Long currentAccountId;

    private String createUserAndGetToken() {
        currentUsername = RandomData.getUsername();
        String password = RandomData.getPassword();

        CreateUserRequest createRequest = CreateUserRequest.builder()
                .username(currentUsername)
                .password(password)
                .role(UserRole.USER.toString())
                .build();

        AdminSteps.createUser(createRequest);

        UserDao userDao = DataBaseSteps.getUserByUsername(currentUsername);
        currentUserId = userDao.getId();
        trackUser(currentUserId);

        return UserSteps.loginAndGetToken(currentUsername, password);
    }

    private int createAccountAndTrack(String token) {
        int accountId = UserSteps.createAccount(token);
        currentAccountId = (long) accountId;
        trackAccount(currentAccountId);
        return accountId;
    }

    @Nested
    @DisplayName("Positive Scenarios")
    class PositiveTests {

        @ParameterizedTest
        @MethodSource("iteration1.api.DepositTest#provideValidDepositData")
        @DisplayName("TC-DEP-001: User can deposit valid amounts - DB verification")
        void userCanDepositValidAmountsTest(double amount) {
            String token = createUserAndGetToken();
            int accountId = createAccountAndTrack(token);

            AccountDao accountBefore = DataBaseSteps.getAccountById((long) accountId);
            double initialBalance = accountBefore.getBalance();

            UserSteps.deposit(token, accountId, amount);

            // Примечание: API запрос GET после внесения депозита возвращает битый ответ
            // (проблема на стороне бэкенда: слишком большой ответ с циклическими ссылками)
            // Использовала базу данных как главный источник правды - так надежнее и быстрее
            AccountDao accountAfter = DataBaseSteps.getAccountById((long) accountId);

            softly.assertThat(accountAfter.getBalance())
                    .as("Balance in DB after deposit")
                    .isCloseTo(initialBalance + amount, within(BALANCE_DELTA));

            softly.assertThat(accountAfter.getBalance())
                    .as("Balance should never be negative")
                    .isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Negative Scenarios - Validation")
    class ValidationTests {

        @ParameterizedTest
        @MethodSource("iteration1.api.DepositTest#provideInvalidDepositData")
        @DisplayName("TC-DEP-002: Invalid deposit amounts are rejected - DB unchanged")
        void userCannotDepositInvalidAmountsTest(double amount) {
            String token = createUserAndGetToken();
            int accountId = createAccountAndTrack(token);

            AccountDao accountBefore = DataBaseSteps.getAccountById((long) accountId);
            double initialBalance = accountBefore.getBalance();

            UserSteps.depositAndExpectBadRequest(token, accountId, amount);

            AccountDao accountAfter = DataBaseSteps.getAccountById((long) accountId);
            softly.assertThat(accountAfter.getBalance()).isEqualTo(initialBalance);
        }
    }
}