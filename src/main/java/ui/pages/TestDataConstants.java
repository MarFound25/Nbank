package ui.pages;

public class TestDataConstants {
    private TestDataConstants() {}

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

    public static final String WELCOME_TEXT_PREFIX = "Welcome, noname!";
    public static final String EMPTY_STRING = "";
}