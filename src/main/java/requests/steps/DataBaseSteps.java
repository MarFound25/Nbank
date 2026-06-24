package requests.steps;

import api.database.DBRequest;
import api.database.DBRequest.Condition;
import api.dao.UserDao;
import api.dao.AccountDao;
import configs.Config;
import common.helpers.StepLogger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class DataBaseSteps {

    static {
        System.out.println("=========================================");
        System.out.println("🔍 DataBaseSteps Configuration Debug:");
        System.out.println("   DB_URL: " + Config.getJdbcUrl());
        System.out.println("   DB_USERNAME: " + Config.getDbUsername());
        System.out.println("   DB_PASSWORD: " + Config.getDbPassword());
        System.out.println("   API_BASE_URL: " + Config.getBaseUrl());
        System.out.println("=========================================");

        // Проверка переменных окружения напрямую
        System.out.println("📌 Direct env check:");
        System.out.println("   DB_URL env: " + System.getenv("DB_URL"));
        System.out.println("   DB_USERNAME env: " + System.getenv("DB_USERNAME"));
        System.out.println("   DB_PASSWORD env: " + System.getenv("DB_PASSWORD"));
        System.out.println("=========================================");
    }

    public static UserDao getUserByUsername(String username) {
        return StepLogger.log("Get user by username: " + username, () -> {
            return DBRequest.<UserDao>builder()
                    .requestType(DBRequest.RequestType.SELECT)
                    .table("customers")
                    .where(Condition.equalTo("username", username))
                    .extractAs(UserDao.class);
        });
    }

    public static UserDao getUserById(Long id) {
        return StepLogger.log("Get user by ID: " + id, () -> {
            return DBRequest.<UserDao>builder()
                    .requestType(DBRequest.RequestType.SELECT)
                    .table("customers")
                    .where(Condition.equalTo("id", id))
                    .extractAs(UserDao.class);
        });
    }

    public static AccountDao getAccountById(Long id) {
        return StepLogger.log("Get account by ID: " + id, () -> {
            return DBRequest.<AccountDao>builder()
                    .requestType(DBRequest.RequestType.SELECT)
                    .table("accounts")
                    .where(Condition.equalTo("id", id))
                    .extractAs(AccountDao.class);
        });
    }

    public static List<AccountDao> getAccountsByUserId(Long userId) {
        return StepLogger.log("Get accounts by user ID: " + userId, () -> {
            return DBRequest.<AccountDao>builder()
                    .requestType(DBRequest.RequestType.SELECT)
                    .table("accounts")
                    .where(Condition.equalTo("customer_id", userId))
                    .extractAsList(AccountDao.class);
        });
    }

    public static void updateAccountBalance(Long accountId, Double newBalance) {
        StepLogger.logVoid("Update account balance: " + accountId + " -> " + newBalance, () -> {
            try (Connection conn = DriverManager.getConnection(
                    Config.getProperty("db.url"),
                    Config.getProperty("db.username"),
                    Config.getProperty("db.password"))) {
                String sql = "UPDATE accounts SET balance = ? WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setDouble(1, newBalance);
                    stmt.setLong(2, accountId);
                    int rows = stmt.executeUpdate();
                    if (rows == 0) throw new RuntimeException("Account not found: " + accountId);
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to update balance", e);
            }
        });
    }

    public static boolean accountExistsInDb(Long accountId) {
        return getAccountById(accountId) != null;
    }

    public static boolean userExistsInDb(String username) {
        try {
            UserDao user = getUserByUsername(username);
            return user != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static void cleanupTestData(List<Long> accountIds, List<Long> userIds) {
        StepLogger.logVoid("Cleaning up test data", () -> {
            try (Connection conn = DriverManager.getConnection(
                    Config.getProperty("db.url"),
                    Config.getProperty("db.username"),
                    Config.getProperty("db.password"))) {
                conn.setAutoCommit(false);

                if (accountIds != null && !accountIds.isEmpty()) {
                    for (Long id : accountIds) {
                        try (PreparedStatement stmt = conn.prepareStatement(
                                "DELETE FROM transactions WHERE account_id = ? OR related_account_id = ?")) {
                            stmt.setLong(1, id);
                            stmt.setLong(2, id);
                            stmt.executeUpdate();
                        }
                        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM accounts WHERE id = ?")) {
                            stmt.setLong(1, id);
                            stmt.executeUpdate();
                        }
                    }
                }

                if (userIds != null && !userIds.isEmpty()) {
                    for (Long id : userIds) {
                        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM customers WHERE id = ?")) {
                            stmt.setLong(1, id);
                            stmt.executeUpdate();
                        }
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                throw new RuntimeException("Cleanup failed", e);
            }
        });
    }

    // Добавить в конец файла DataBaseSteps.java

    public static void waitForAccountInDb(Long accountId, long timeoutMs) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                AccountDao account = getAccountById(accountId);
                if (account != null) {
                    System.out.println("Account found in DB: " + accountId);
                    return;
                }
            } catch (Exception e) {
                // Account not found yet
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("Account not found after timeout: " + accountId);
    }

    public static void waitForBalanceUpdate(Long accountId, double expectedAmount, long timeoutMs) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            AccountDao account = getAccountById(accountId);
            if (account != null && Math.abs(account.getBalance() - expectedAmount) < 0.01) {
                System.out.println("Balance updated: " + accountId + " = " + account.getBalance());
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("Balance not updated after timeout for account: " + accountId);
    }

}