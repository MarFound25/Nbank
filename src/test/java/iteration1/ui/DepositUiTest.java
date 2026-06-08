package iteration1.ui;

import common.annotations.UserSession;
import org.junit.jupiter.api.Test;
import ui.pages.BankAlert;
import ui.pages.TestDataConstants;
import ui.pages.UserDashboard;

public class DepositUiTest extends BaseUiTest {

    @Test
    @UserSession
    public void userCanDepositValidAmountTest() {
        new UserDashboard()
                .openWithPreparedAccount()
                .makeDeposit(TestDataConstants.VALID_DEPOSIT_AMOUNT, true)
                .checkAlertMessageAndAccept(BankAlert.DEPOSIT_SUCCESSFULLY.getMessage())
                .assertBalanceIncreasedBy(TestDataConstants.VALID_DEPOSIT_AMOUNT);
    }

    @Test
    @UserSession
    public void userCanDepositMaxLimitAmountTest() {
        new UserDashboard()
                .openWithPreparedAccount()
                .makeDeposit(TestDataConstants.MAX_DEPOSIT_AMOUNT, true)
                .checkAlertMessageAndAccept(BankAlert.DEPOSIT_SUCCESSFULLY.getMessage())
                .assertBalanceIncreasedBy(TestDataConstants.MAX_DEPOSIT_AMOUNT);
    }

    @Test
    @UserSession
    public void userCannotDepositAboveLimitTest() {
        new UserDashboard()
                .openWithPreparedAccount()
                .makeDeposit(TestDataConstants.INVALID_DEPOSIT_AMOUNT, true)
                .checkAlertMessageAndAccept(BankAlert.DEPOSIT_LIMIT_EXCEEDED.getMessage())
                .assertBalanceNotChanged();
    }

    @Test
    @UserSession
    public void userCannotDepositWithoutAccountTest() {
        new UserDashboard()
                .openWithPreparedAccount()
                .makeDeposit(TestDataConstants.TRANSFER_AMOUNT_1000, false)
                .checkAlertMessageAndAccept(BankAlert.SELECT_ACCOUNT.getMessage())
                .assertBalanceNotChanged();
    }
}