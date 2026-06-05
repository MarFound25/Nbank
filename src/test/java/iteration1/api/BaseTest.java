package iteration1.api;

import api.dao.AccountDao;
import api.dao.UserDao;
import generators.RandomData;
import models.*;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import requests.steps.DataBaseSteps;
import requests.steps.UserSteps;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseTest {

    protected SoftAssertions softly;

    // Простые списки, не ThreadLocal (если не нужен параллельный запуск)
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
        // Очищаем БД после каждого теста
        if (!createdAccounts.isEmpty() || !createdUsers.isEmpty()) {
            DataBaseSteps.cleanupTestData(createdAccounts, createdUsers);
        }
        softly.assertAll();
    }

    // Вспомогательные методы для трекинга
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