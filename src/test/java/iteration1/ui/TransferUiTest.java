package iteration1.ui;

import common.annotations.UserSession;
import org.junit.jupiter.api.Test;
import ui.pages.BankAlert;
import ui.pages.TestDataConstants;
import ui.pages.UserDashboard;

public class TransferUiTest extends BaseUiTest {

    @Test
    @UserSession
    public void userCanTransferValidAmountTest() {
        new UserDashboard()
                .openWithTwoPreparedAccounts()
                .makeTransfer(TestDataConstants.FIRST_ACCOUNT_INDEX, TestDataConstants.VALID_TRANSFER_AMOUNT)
                .checkAlertMessageAndAccept(BankAlert.TRANSFER_SUCCESSFULLY.getMessage())
                .assertTransferCompleted(TestDataConstants.VALID_TRANSFER_AMOUNT);
    }

    @Test
    @UserSession
    public void userCanTransferMaxLimitAmountTest() {
        new UserDashboard()
                .openWithTwoPreparedAccounts()
                .makeTransfer(TestDataConstants.FIRST_ACCOUNT_INDEX, TestDataConstants.MAX_TRANSFER_AMOUNT)
                .checkAlertMessageAndAccept(BankAlert.TRANSFER_SUCCESSFULLY.getMessage())
                .assertTransferCompleted(TestDataConstants.MAX_TRANSFER_AMOUNT);
    }

    @Test
    @UserSession
    public void userCannotTransferAboveLimitTest() {
        new UserDashboard()
                .openWithTwoPreparedAccounts()
                .makeTransfer(TestDataConstants.FIRST_ACCOUNT_INDEX, TestDataConstants.INVALID_TRANSFER_AMOUNT)
                .checkAlertMessageAndAccept(BankAlert.TRANSFER_LIMIT_EXCEEDED.getMessage())
                .assertTransferNotCompleted();
    }

    @Test
    @UserSession
    public void userCannotTransferMoreThanBalanceTest() {
        new UserDashboard()
                .openWithTwoPreparedAccounts()
                .makeTransfer(TestDataConstants.FIRST_ACCOUNT_INDEX, 999999.99)
                .checkAlertMessageAndAccept(BankAlert.TRANSFER_AMOUNT_EXCEED.getMessage())
                .assertTransferNotCompleted();
    }

    @Test
    @UserSession
    public void userCannotTransferWithoutFromAccountTest() {
        new UserDashboard()
                .openWithTwoPreparedAccounts()
                .makeTransferWithoutFromAccount(
                        TestDataConstants.DEFAULT_USER_NAME,
                        TestDataConstants.VALID_TRANSFER_AMOUNT
                )
                .checkAlertMessageAndAccept(BankAlert.PLEASE_FILL_ALL_FIELDS.getMessage())
                .assertTransferNotCompleted();
    }

    @Test
    @UserSession
    public void userCannotTransferWithoutConfirmationTest() {
        new UserDashboard()
                .openWithTwoPreparedAccounts()
                .makeTransferWithoutConfirmation(
                        TestDataConstants.FIRST_ACCOUNT_INDEX,
                        TestDataConstants.DEFAULT_USER_NAME,
                        TestDataConstants.VALID_TRANSFER_AMOUNT
                )
                .checkAlertMessageAndAccept(BankAlert.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM.getMessage())
                .assertTransferNotCompleted();
    }
}