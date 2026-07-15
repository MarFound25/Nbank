package common.builders;

import api.dao.UserDao;
import models.UserWithToken;
import generators.RandomData;
import models.CreateUserRequest;
import models.UserRole;
import requests.steps.AdminSteps;
import requests.steps.DataBaseSteps;
import requests.steps.UserSteps;

import java.util.ArrayList;
import java.util.List;

public class TestDataBuilder {
    private final List<Long> createdUsers = new ArrayList<>();
    private final List<Long> createdAccounts = new ArrayList<>();

    public UserWithToken createUserWithToken() {
        return createUserWithToken(UserRole.USER);
    }

    public UserWithToken createUserWithToken(UserRole role) {
        String username = RandomData.getUsername();
        String password = RandomData.getPassword();

        CreateUserRequest request = CreateUserRequest.builder()
                .username(username)
                .password(password)
                .role(role.toString())
                .name("Test User " + System.currentTimeMillis())
                .build();

        AdminSteps.createUser(request);

        UserDao userDao = DataBaseSteps.getUserByUsername(username);
        createdUsers.add(userDao.getId());

        String token = UserSteps.loginAndGetToken(username, password);

        return UserWithToken.builder()
                .id(userDao.getId())
                .username(username)
                .password(password)
                .token(token)
                .build();
    }

    public Long createAccount(String token) {
        int accountId = UserSteps.createAccount(token);
        Long id = (long) accountId;
        createdAccounts.add(id);

        DataBaseSteps.waitForAccountInDb(id, 5000);

        return id;
    }

    public Long createAccountAndDeposit(String token, double amount) {
        Long accountId = createAccount(token);
        UserSteps.deposit(token, accountId.intValue(), amount);

        DataBaseSteps.waitForBalanceUpdate(accountId, amount, 5000);

        return accountId;
    }

    public TestDataBuilder createTwoUsersWithAccounts() {
        return this; 
    }

    public List<Long> getCreatedUsers() {
        return new ArrayList<>(createdUsers);
    }

    public List<Long> getCreatedAccounts() {
        return new ArrayList<>(createdAccounts);
    }

    public void cleanup() {
        if (!createdAccounts.isEmpty() || !createdUsers.isEmpty()) {
            DataBaseSteps.cleanupTestData(createdAccounts, createdUsers);
        }
        createdUsers.clear();
        createdAccounts.clear();
    }
}
