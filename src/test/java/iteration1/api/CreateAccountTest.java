package iteration1.api;

import generators.RandomData;
import models.*;
import org.junit.jupiter.api.Test;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;

import java.util.List;

public class CreateAccountTest extends BaseTest {

    private String createUserAndGetToken() {
        CreateUserRequest createRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .role(UserRole.USER.toString())
                .build();

        AdminSteps.createUser(createRequest);
        return UserSteps.loginAndGetToken(createRequest.getUsername(), createRequest.getPassword());
    }

    @Test
    public void userCanCreateAccountTest() {
        String token = createUserAndGetToken();
        int accountId = UserSteps.createAccount(token);
        softly.assertThat(accountId).isNotNull();
        softly.assertThat(accountId).isGreaterThan(0);
    }

    @Test
    public void userCanCreateMultipleAccountsTest() {
        String token = createUserAndGetToken();

        int account1 = UserSteps.createAccount(token);
        int account2 = UserSteps.createAccount(token);

        List<Account> accounts = UserSteps.getAccounts(token);

        softly.assertThat(accounts)
                .extracting(Account::getId)
                .contains((long) account1, (long) account2);
        softly.assertThat(accounts).hasSize(2);
    }

    @Test
    public void userCanViewOwnAccountsTest() {
        String token = createUserAndGetToken();

        int account1 = UserSteps.createAccount(token);
        int account2 = UserSteps.createAccount(token);

        List<Account> accounts = UserSteps.getAccounts(token);

        softly.assertThat(accounts).hasSize(2);
        softly.assertThat(accounts)
                .extracting(Account::getId)
                .contains((long) account1, (long) account2);
    }

    @Test
    public void userCannotViewAnotherUsersAccountsTest() {
        String token1 = createUserAndGetToken();
        int user1Account = UserSteps.createAccount(token1);

        String token2 = createUserAndGetToken();

        List<Account> accounts = UserSteps.getAccounts(token2);

        softly.assertThat(accounts).isEmpty();
        softly.assertThat(accounts)
                .extracting(Account::getId)
                .doesNotContain((long) user1Account);
    }

    @Test
    public void userCanGetAccountTransactionsTest() {
        String token = createUserAndGetToken();

        int accountId = UserSteps.createAccount(token);
        UserSteps.deposit(token, accountId, 1000.00);

        List<Transaction> transactions = UserSteps.getTransactions(token, accountId);

        softly.assertThat(transactions).isNotNull();
        softly.assertThat(transactions).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    public void userCannotGetAnotherUsersAccountTransactionsTest() {
        String token1 = createUserAndGetToken();
        int user1Account = UserSteps.createAccount(token1);
        UserSteps.deposit(token1, user1Account, 500.00);

        String token2 = createUserAndGetToken();

        UserSteps.getTransactionsAndExpectForbidden(token2, user1Account);
    }

    @Test
    public void userCannotGetTransactionsForNonexistentAccountTest() {
        String token = createUserAndGetToken();
        UserSteps.getTransactionsAndExpectForbidden(token, 999999);
    }

    @Test
    public void userCannotGetTransactionsWithoutAuthTest() {
        UserSteps.getTransactionsAndExpectUnauthorized(1);
    }

    @Test
    public void userCannotGetTransactionsWithInvalidTokenTest() {
        UserSteps.getTransactionsWithInvalidTokenAndExpectUnauthorized(1);
    }

    @Test
    public void userCannotGetTransactionsWithInvalidBasicAuthTest() {
        UserSteps.getTransactionsWithInvalidBasicAuthAndExpectUnauthorized("invalid", 1);
    }

    @Test
    public void userCannotGetTransactionsWithEmptyTokenTest() {
        UserSteps.getTransactionsWithEmptyTokenAndExpectUnauthorized(1);
    }
}