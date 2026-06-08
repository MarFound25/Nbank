package ui.pages;

import lombok.Getter;

@Getter
public enum BankAlert {
    USER_CREATED_SUCCESSFULLY("✅ User created successfully!"),
    USERNAME_MUST_BE_BETWEEN_3_AND_15_CHARACTERS("Username must be between 3 and 15 characters"),

    NEW_ACCOUNT_CREATED("✅ New Account Created! Account Number: "),

    DEPOSIT_SUCCESSFULLY("Successfully deposited"),
    DEPOSIT_LIMIT_EXCEEDED("❌ Please deposit less or equal to 5000$."),
    SELECT_ACCOUNT("select an account"),

    TRANSFER_SUCCESSFULLY("Successfully transferred"),
    TRANSFER_LIMIT_EXCEEDED("cannot exceed 10000"),
    TRANSFER_AMOUNT_EXCEED("Transfer amount cannot exceed"),
    PLEASE_FILL_ALL_FIELDS("Please fill all fields"),
    PLEASE_FILL_ALL_FIELDS_AND_CONFIRM("Please fill all fields and confirm"),

    PROFILE_SUCCESS("success"),
    PROFILE_TWO_WORDS("two words"),
    PROFILE_LETTERS_ONLY("letters"),
    PROFILE_SAME_AS_CURRENT("❌ Please enter a valid name."),
    PROFILE_UPDATED_SUCCESSFULLY("updated successfully");

    private final String message;

    BankAlert(String message) {
        this.message = message;
    }
}