package iteration1.api;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import requests.steps.DataBaseSteps;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseTest {

    protected SoftAssertions softly;

    protected final List<Long> createdUsers = new ArrayList<>();
    protected final List<Long> createdAccounts = new ArrayList<>();

    @BeforeEach
    void setUp() {
        softly = new SoftAssertions();
        createdUsers.clear();
        createdAccounts.clear();
    }

    @AfterEach
    void tearDown() {

        if (!createdAccounts.isEmpty() || !createdUsers.isEmpty()) {
            DataBaseSteps.cleanupTestData(createdAccounts, createdUsers);
        }
        softly.assertAll();
    }

    protected void trackUser(Long userId) {
        if (userId != null) {
            createdUsers.add(userId);
        }
    }

    protected void trackAccount(Long accountId) {
        if (accountId != null) {
            createdAccounts.add(accountId);
        }
    }
}