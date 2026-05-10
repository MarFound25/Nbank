package iteration1.api;

import generators.RandomData;
import models.CreateUserRequest;
import models.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;

import java.util.stream.Stream;

public class DepositTest extends BaseTest {

    private String createUserAndGetToken() {
        CreateUserRequest createRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        AdminSteps.createUser(createRequest);
        return UserSteps.loginAndGetToken(createRequest.getUsername(), createRequest.getPassword());
    }

    @ParameterizedTest
    @MethodSource("provideValidDepositData")
    public void userCanDepositValidAmountsTest(double amount) {
        String token = createUserAndGetToken();
        int accountId = UserSteps.createAccount(token);

        double initialBalance = UserSteps.getAccountBalance(token, accountId);
        UserSteps.deposit(token, accountId, amount);
        double finalBalance = UserSteps.getAccountBalance(token, accountId);

        softly.assertThat(finalBalance).isEqualTo(initialBalance + amount);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidDepositData")
    public void userCannotDepositToOwnAccountWithInvalidAmountTest(double amount) {
        String token = createUserAndGetToken();
        int accountId = UserSteps.createAccount(token);

        double initialBalance = UserSteps.getAccountBalance(token, accountId);
        UserSteps.depositAndExpectBadRequest(token, accountId, amount);
        double finalBalance = UserSteps.getAccountBalance(token, accountId);

        softly.assertThat(finalBalance).isEqualTo(initialBalance);
    }

    @Test
    public void userCannotDepositToAnotherUsersAccountTest() {
        String token1 = createUserAndGetToken();
        int account1Id = UserSteps.createAccount(token1);
        double account1InitialBalance = UserSteps.getAccountBalance(token1, account1Id);

        String token2 = createUserAndGetToken();
        UserSteps.depositAndExpectForbidden(token2, account1Id, 100.00);

        double account1FinalBalance = UserSteps.getAccountBalance(token1, account1Id);
        softly.assertThat(account1FinalBalance).isEqualTo(account1InitialBalance);
    }

    @Test
    public void userCannotDepositToNonExistentAccountTest() {
        String token = createUserAndGetToken();
        UserSteps.depositAndExpectForbidden(token, 999999, 100.00);
    }

    @Test
    public void userCanDepositMaxLimitAmountTest() {
        String token = createUserAndGetToken();
        int accountId = UserSteps.createAccount(token);

        double initialBalance = UserSteps.getAccountBalance(token, accountId);
        UserSteps.deposit(token, accountId, 5000.00);
        double finalBalance = UserSteps.getAccountBalance(token, accountId);

        softly.assertThat(finalBalance).isEqualTo(initialBalance + 5000.0);
    }

    @Test
    public void userCannotDepositAboveLimitTest() {
        String token = createUserAndGetToken();
        int accountId = UserSteps.createAccount(token);

        double initialBalance = UserSteps.getAccountBalance(token, accountId);
        UserSteps.depositAndExpectBadRequest(token, accountId, 5000.01);
        double finalBalance = UserSteps.getAccountBalance(token, accountId);

        softly.assertThat(finalBalance).isEqualTo(initialBalance);
    }

    @Test
    public void userCannotDepositWithoutAuthTest() {
        UserSteps.depositAndExpectUnauthorized(1, 100.00);
    }

    @Test
    public void userCannotDepositWithInvalidTokenTest() {
        UserSteps.depositWithInvalidTokenAndExpectUnauthorized(1, 100.00);
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
                Arguments.of(-100.00),
                Arguments.of(0.00),
                Arguments.of(5001.00)
        );
    }
}