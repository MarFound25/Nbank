package iteration1.api;

import models.*;
import models.comparison.ModelAssertions;
import requests.steps.AccountSteps;
import requests.steps.AdminSteps;
import common.extensions.TimingExtension;
import iteration1.api.FraudCheckWireMockExtension;
import common.annotations.FraudCheckMock;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({TimingExtension.class, FraudCheckWireMockExtension.class})
public class TransferWithFraudCheckTest extends BaseTest {

    private static final double MIN_DEPOSIT_AMOUNT = 0.1;
    private static final double MAX_DEPOSIT_AMOUNT = 4999.9;
    private static final double FRAUD_RISK_SCORE = 0.2;
    private static final String FRAUD_REASON = "Low risk transaction";
    private static final String EXPECTED_STATUS = "APPROVED";
    private static final String EXPECTED_MESSAGE = "Transfer approved and processed immediately";
    private static final boolean REQUIRES_MANUAL_REVIEW = false;
    private static final boolean REQUIRES_VERIFICATION = false;

    private UserWithPassword senderUser;
    private UserWithPassword receiverUser;
    private CreateAccountResponse senderAccount;
    private CreateAccountResponse receiverAccount;
    private DepositResponse depositResponse;
    private TransferResponse transferResponse;
    private AccountSteps senderAccountSteps;
    private AccountSteps receiverAccountSteps;

    @BeforeEach
    public void setUp() {
        softly = new SoftAssertions();
        initializeTestUsersAndAccounts();
    }

    private void initializeTestUsersAndAccounts() {
        senderUser = AdminSteps.createUserWithPassword();
        senderAccountSteps = new AccountSteps(senderUser.getUsername(), senderUser.getPassword());
        senderAccount = senderAccountSteps.createAccount();

        receiverUser = AdminSteps.createUserWithPassword();
        receiverAccountSteps = new AccountSteps(receiverUser.getUsername(), receiverUser.getPassword());
        receiverAccount = receiverAccountSteps.createAccount();
    }

    @Test
    @FraudCheckMock(
            port = 8082,
            status = "SUCCESS",
            decision = "APPROVED",
            riskScore = FRAUD_RISK_SCORE,
            reason = FRAUD_REASON,
            requiresManualReview = REQUIRES_MANUAL_REVIEW,
            additionalVerificationRequired = REQUIRES_VERIFICATION
    )
    public void shouldTransferAmountWithFraudCheckWhenLowRiskTransaction() {
        double depositAmount = generateRandomDepositAmount();
        depositResponse = senderAccountSteps.depositToAccount(senderAccount.getId().longValue(), depositAmount);
        double transferAmount = generateRandomTransferAmount(depositAmount);

        transferResponse = senderAccountSteps.transferWithFraudCheck(
                senderAccount.getId().longValue(),
                receiverAccount.getId().longValue(),
                transferAmount
        );

        softly.assertThat(transferResponse)
                .as("Transfer response should not be null")
                .isNotNull();

        TransferResponse expectedResponse = buildExpectedTransferResponse(transferAmount);
        ModelAssertions.assertThatModels(expectedResponse, transferResponse)
                .as("Transfer response should match expected values")
                .match();
    }

    private double generateRandomDepositAmount() {
        return Math.random() * MAX_DEPOSIT_AMOUNT + MIN_DEPOSIT_AMOUNT;
    }

    private double generateRandomTransferAmount(double depositAmount) {
        double minTransferAmount = MIN_DEPOSIT_AMOUNT;
        double maxTransferAmount = depositAmount - MIN_DEPOSIT_AMOUNT;

        if (maxTransferAmount <= minTransferAmount) {
            return minTransferAmount;
        }

        return Math.random() * (depositAmount - MIN_DEPOSIT_AMOUNT) + MIN_DEPOSIT_AMOUNT;
    }

    private TransferResponse buildExpectedTransferResponse(double transferAmount) {
        return TransferResponse.builder()
                .status(EXPECTED_STATUS)
                .message(EXPECTED_MESSAGE)
                .amount(transferAmount)
                .senderAccountId(Long.valueOf(senderAccount.getId()))
                .receiverAccountId(Long.valueOf(receiverAccount.getId()))
                .fraudRiskScore(FRAUD_RISK_SCORE)
                .fraudReason(FRAUD_REASON)
                .requiresManualReview(REQUIRES_MANUAL_REVIEW)
                .requiresVerification(REQUIRES_VERIFICATION)
                .build();
    }

    @AfterEach
    public void tearDown() {
        if (softly != null) {
            softly.assertAll();
        }
    }
}