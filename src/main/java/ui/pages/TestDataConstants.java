package ui.pages;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TestDataConstants {

    public static final double VALID_DEPOSIT_AMOUNT = 1000.00;
    public static final double MAX_DEPOSIT_AMOUNT = 5000.00;
    public static final double INVALID_DEPOSIT_AMOUNT = 5001.00;
    public static final double VALID_TRANSFER_AMOUNT = 3000.00;
    public static final double MAX_TRANSFER_AMOUNT = 10000.00;
    public static final double INVALID_TRANSFER_AMOUNT = 10001.00;
    public static final double TRANSFER_AMOUNT_1000 = 1000.00;

    public static final int FIRST_ACCOUNT_INDEX = 1;
    public static final int DEPOSIT_STEPS_COUNT = 3;

    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "admin";
    public static final String DEFAULT_USER_NAME = "Test User";
    public static final String ORIGINAL_NAME = "Old Name";
    public static final String NEW_NAME_SUFFIX = "a";
    public static final String INVALID_USERNAME = "a";
    public static final String INVALID_SINGLE_WORD = "John";
    public static final String INVALID_WITH_DIGITS = "John 123";
    public static final String INVALID_WITH_SPECIAL = "John@ Doe";
    public static final String EXPECTED_TRIMMED_NAME = "Anna Smith";

    public static final String WELCOME_TEXT_PREFIX = "Welcome,";
    public static final String EMPTY_STRING = "";

    public static final class Api {
        public static final double DEFAULT_DEPOSIT_AMOUNT = 5000.0;
        public static final double BALANCE_DELTA = 0.01;
        public static final double MIN_POSITIVE_AMOUNT = 0.01;

        public static final double FRAUD_CHECK_THRESHOLD = 1000.0;
        public static final double BLOCK_THRESHOLD = 10000.0;
        public static final double HIGH_RISK_THRESHOLD = 0.3;

        public static final int NON_EXISTENT_ACCOUNT_ID = 999999;
        public static final int CONCURRENT_THREADS = 10;
        public static final double CONCURRENT_TRANSFER_AMOUNT = 100.0;
        public static final double[] CUMULATIVE_TRANSFER_AMOUNTS = {100.0, 250.0, 75.0, 500.0, 125.0};

        public static final int WIREMOCK_PORT = 8082;
        public static final String FRAUD_ENDPOINT = "/fraud-check";
        public static final String BASE_API_URL = "http://localhost:8080";
    }

    public static final class HttpStatus {
        public static final int OK = 200;
        public static final int CREATED = 201;
        public static final int BAD_REQUEST = 400;
        public static final int UNAUTHORIZED = 401;
        public static final int FORBIDDEN = 403;
        public static final int NOT_FOUND = 404;
        public static final int CONFLICT = 409;
        public static final int INTERNAL_ERROR = 500;
    }

    public static final class Timeouts {
        public static final int DEFAULT_SECONDS = 30;
        public static final int FRAUD_CHECK_DELAY_MS = 2000;
        public static final int THREAD_POOL_SHUTDOWN_SECONDS = 10;
        public static final int UI_IMPLICIT_WAIT_SECONDS = 5;
        public static final int UI_EXPLICIT_WAIT_SECONDS = 10;
        public static final int UI_PAGE_LOAD_SECONDS = 15;
    }

    public static final class Paths {
        public static final String LOGS_DIR = "./logs";
        public static final String REPORTS_DIR = "./reports";
        public static final String SCREENSHOTS_DIR = "./screenshots";
        public static final String TEST_DATA_DIR = "./src/test/resources/test-data";
        public static final String DOWNLOADS_DIR = "./downloads";
    }

    public static final class ErrorMessages {
        public static final String INSUFFICIENT_BALANCE = "Insufficient balance";
        public static final String INVALID_AMOUNT = "Amount must be positive";
        public static final String ACCOUNT_NOT_FOUND = "Account not found";
        public static final String UNAUTHORIZED_ACCESS = "Unauthorized access";
        public static final String FORBIDDEN_ACCESS = "Forbidden access";
        public static final String FRAUD_DETECTED = "Transaction blocked by fraud check";
        public static final String INVALID_TOKEN = "Invalid or expired token";
    }

    public static final class Patterns {
        public static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";
        public static final String PHONE_PATTERN = "^\\+?[0-9]{10,15}$";
        public static final String NAME_PATTERN = "^[A-Za-z\\s-]{2,50}$";
        public static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$";
        public static final String AMOUNT_PATTERN = "^\\d+(\\.\\d{1,2})?$";
    }

    public static final class UserData {
        public static final String DEFAULT_FIRST_NAME = "John";
        public static final String DEFAULT_LAST_NAME = "Doe";
        public static final String DEFAULT_EMAIL = "john.doe@example.com";
        public static final String DEFAULT_PASSWORD = "Test@1234";
        public static final String DEFAULT_PHONE = "+1234567890";

        public static final String TEST_USER_PREFIX = "test_user_";
        public static final String TEST_EMAIL_DOMAIN = "@test.com";
    }
}
