package iteration1.api;

import api.dao.AccountDao;
import common.builders.TestDataBuilder;
import models.UserWithToken;
import models.UserRole;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import requests.steps.DataBaseSteps;
import requests.steps.UserSteps;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseTest {

    protected static final Logger log = LoggerFactory.getLogger(BaseTest.class);
    protected static final double BALANCE_DELTA = 0.01;
    protected static final int DEFAULT_WAIT_MS = 500;

    protected SoftAssertions softly;
    protected TestDataBuilder dataBuilder;

    private final List<Long> createdUsers = new ArrayList<>();
    private final List<Long> createdAccounts = new ArrayList<>();
    private final List<Runnable> cleanupTasks = new ArrayList<>();

    @BeforeEach
    void setUp() {
        softly = new SoftAssertions();
        dataBuilder = new TestDataBuilder();
        createdUsers.clear();
        createdAccounts.clear();
        cleanupTasks.clear();
        log.debug("Test setup completed for: {}", this.getClass().getSimpleName());
    }

    @AfterEach
    void tearDown() {
        executeCleanup();
        softly.assertAll();
        log.debug("Test teardown completed for: {}", this.getClass().getSimpleName());
    }

    private void executeCleanup() {
        cleanupTasks.forEach(Runnable::run);
        if (!createdAccounts.isEmpty() || !createdUsers.isEmpty()) {
            DataBaseSteps.cleanupTestData(createdAccounts, createdUsers);
        }
    }

    protected void trackUser(Long userId) {
        if (userId != null) {
            createdUsers.add(userId);
            log.debug("Tracked user ID: {}", userId);
        }
    }

    protected void trackAccount(Long accountId) {
        if (accountId != null) {
            createdAccounts.add(accountId);
            log.debug("Tracked account ID: {}", accountId);
        }
    }

    protected void addCleanupTask(Runnable task) {
        cleanupTasks.add(task);
    }

    protected UserWithToken createTrackedUser() {
        UserWithToken user = dataBuilder.createUserWithToken();
        trackUser(user.getId());
        log.info("Created tracked user: {} (ID: {})", user.getUsername(), user.getId());
        return user;
    }

    protected UserWithToken createTrackedUser(UserRole role) {
        UserWithToken user = dataBuilder.createUserWithToken(role);
        trackUser(user.getId());
        log.info("Created tracked user with role {}: {} (ID: {})", role, user.getUsername(), user.getId());
        return user;
    }

    protected Long createTrackedAccount(String token) {
        Long accountId = dataBuilder.createAccount(token);
        trackAccount(accountId);
        log.debug("Created tracked account ID: {}", accountId);
        return accountId;
    }

    protected Long createTrackedAccountWithDeposit(String token, double amount) {
        Long accountId = dataBuilder.createAccountAndDeposit(token, amount);
        trackAccount(accountId);
        log.debug("Created tracked account with deposit ${}: {}", amount, accountId);
        return accountId;
    }

    protected double getAccountBalance(Long accountId) {
        AccountDao account = DataBaseSteps.getAccountById(accountId);
        return account != null ? account.getBalance() : 0.0;
    }

    protected double getAccountBalanceFromDb(Long accountId) {
        return getAccountBalance(accountId);
    }

    protected boolean accountExists(Long accountId) {
        return DataBaseSteps.getAccountById(accountId) != null;
    }

    protected boolean userExists(String username) {
        return DataBaseSteps.userExistsInDb(username);
    }

    protected void waitForAsyncCompletion() {
        try {
            Thread.sleep(DEFAULT_WAIT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Wait interrupted", e);
        }
    }

    protected void waitForFraudCheckCompletion() {
        waitForAsyncCompletion();
    }

    protected void waitForCondition(WaitCondition condition, long timeoutMs) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (condition.isMet()) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new AssertionError("Condition not met within " + timeoutMs + "ms");
    }

    @FunctionalInterface
    protected interface WaitCondition {
        boolean isMet();
    }
}
