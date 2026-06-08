package iteration1.api;

import api.dao.AccountDao;
import api.dao.UserDao;
import api.dao.comparison.DaoAndModelAssertions;
import generators.RandomData;
import models.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import requests.steps.AdminSteps;
import requests.steps.DataBaseSteps;
import requests.steps.UserSteps;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@DisplayName("Account Management Tests")
public class CreateAccountTest extends BaseTest {

    private String currentUsername;
    private Long currentUserId;

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

    @Nested
    @DisplayName("Positive Scenarios")
    class PositiveTests {

        @Test
        @DisplayName("TC-ACC-001: User can create account - DB verification")
        public void userCanCreateAccountTest() {
            String token = createUserAndGetToken();
            int accountId = UserSteps.createAccount(token);

            softly.assertThat(accountId).isNotNull();
            softly.assertThat(accountId).isGreaterThan(0);

            AccountDao accountDao = DataBaseSteps.getAccountById((long) accountId);

            List<AccountDTO> accounts = UserSteps.getAccounts(token);
            AccountDTO apiAccount = accounts.stream()
                    .filter(a -> a.getId() == accountId)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Account not found in API response"));

            DaoAndModelAssertions.assertThat(apiAccount, accountDao).matches();

            trackAccount(accountDao.getId());
        }

        @Test
        @DisplayName("TC-ACC-003: User can view own accounts - matches DB")
        public void userCanViewOwnAccountsTest() {
            String token = createUserAndGetToken();

            int account1 = UserSteps.createAccount(token);
            int account2 = UserSteps.createAccount(token);

            List<AccountDTO> apiAccounts = UserSteps.getAccounts(token);
            List<AccountDao> dbAccounts = DataBaseSteps.getAccountsByUserId(currentUserId);

            softly.assertThat(apiAccounts).hasSize(2);
            softly.assertThat(dbAccounts).hasSize(2);

            Map<Long, AccountDao> dbAccountMap = dbAccounts.stream()
                    .collect(Collectors.toMap(AccountDao::getId, Function.identity()));

            apiAccounts.forEach(apiAccount -> {
                AccountDao correspondingDbAccount = dbAccountMap.get(apiAccount.getId());

                softly.assertThat(correspondingDbAccount)
                        .as("Account %d not found in database", apiAccount.getId())
                        .isNotNull();

                DaoAndModelAssertions.assertThat(apiAccount, correspondingDbAccount).matches();
            });

            trackAccount((long) account1);
            trackAccount((long) account2);
        }
    }
}