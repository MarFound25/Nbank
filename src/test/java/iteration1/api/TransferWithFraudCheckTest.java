package iteration1.api;

import api.dao.AccountDao;
import common.annotations.FraudCheckMock;
import common.extensions.FraudCheckWireMockExtension;
import common.extensions.TimingExtension;
import models.UserWithToken;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import requests.steps.DataBaseSteps;
import requests.steps.UserSteps;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertAll;
import static ui.pages.TestDataConstants.Api.*;
import static ui.pages.TestDataConstants.HttpStatus.*;
import static ui.pages.TestDataConstants.Timeouts.*;

@Slf4j
@ExtendWith({TimingExtension.class, FraudCheckWireMockExtension.class})
@DisplayName("Anti-Fraud Transfer Validation Suite")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TransferWithFraudCheckTest extends BaseTest {

    private UserWithToken sender;
    private UserWithToken receiver;
    private Long senderAccountId;
    private Long receiverAccountId;
    private TransferTestContext testContext;

    @BeforeEach
    void setUpTestContext() {
        testContext = new TransferTestContext();
        initializeTestUsersAndAccounts();
    }

    private void initializeTestUsersAndAccounts() {
        sender = createTrackedUser();
        receiver = createTrackedUser();

        senderAccountId = createTrackedAccount(sender.getToken());
        receiverAccountId = createTrackedAccount(receiver.getToken());

        UserSteps.deposit(sender.getToken(), senderAccountId.intValue(), DEFAULT_DEPOSIT_AMOUNT);

        testContext.captureInitialBalances(
                getAccountBalance(senderAccountId),
                getAccountBalance(receiverAccountId)
        );

        log.info("Настройка теста завершена. Отправитель: {}, баланс: {}. Получатель: {}, баланс: {}",
                sender.getUsername(), testContext.getSenderInitialBalance(),
                receiver.getUsername(), testContext.getReceiverInitialBalance());
    }

    private void executeTransfer(double amount) {
        UserSteps.transfer(sender.getToken(), senderAccountId.intValue(), receiverAccountId.intValue(), amount);
        waitForFraudCheckCompletion();
        testContext.recordTransfer(amount);
    }

    private void assertBalancesChanged(double expectedSenderDecrease, double expectedReceiverIncrease) {
        AccountDao senderAfter = DataBaseSteps.getAccountById(senderAccountId);
        AccountDao receiverAfter = DataBaseSteps.getAccountById(receiverAccountId);

        assertAll(
                () -> assertThat(senderAfter.getBalance())
                        .as("Баланс отправителя должен уменьшиться на %.2f", expectedSenderDecrease)
                        .isCloseTo(testContext.getSenderInitialBalance() - expectedSenderDecrease, within(BALANCE_DELTA)),
                () -> assertThat(receiverAfter.getBalance())
                        .as("Баланс получателя должен увеличиться на %.2f", expectedReceiverIncrease)
                        .isCloseTo(testContext.getReceiverInitialBalance() + expectedReceiverIncrease, within(BALANCE_DELTA))
        );
    }

    @Nested
    @DisplayName("Механизм принятия решений anti-fraud")
    class FraudDecisionTests {

        @Test
        @Order(1)
        @FraudCheckMock(
                port = WIREMOCK_PORT,
                endpoint = FRAUD_ENDPOINT,
                decision = "APPROVED",
                riskScore = 0.15,
                reason = "Normal transaction pattern",
                requiresManualReview = false
        )
        @DisplayName("APPROVED: Low-risk transaction completes immediately")
        void approved_lowRiskTransaction_completesImmediately() {
            double transferAmount = DEFAULT_DEPOSIT_AMOUNT * 0.1;
            executeTransfer(transferAmount);
            assertBalancesChanged(transferAmount, transferAmount);
        }
    }

    @Nested
    @DisplayName("Проверка граничных значений оценки риска")
    class RiskScoreBoundaryTests {

        @Test
        @Order(4)
        @FraudCheckMock(
                port = WIREMOCK_PORT,
                endpoint = FRAUD_ENDPOINT,
                riskScore = HIGH_RISK_THRESHOLD - 0.01,
                decision = "APPROVED"
        )
        @DisplayName("Risk score 0.29 → APPROVED (just below threshold)")
        void riskScore_29_approved() {
            double transferAmount = 100.00;
            executeTransfer(transferAmount);
            assertBalancesChanged(transferAmount, transferAmount);
        }
    }

    @Nested
    @DisplayName("Оценка риска на основе суммы перевода")
    class AmountThresholdTests {

        static Stream<Arguments> amountAndExpectationProvider() {
            return Stream.of(
                    Arguments.of(50.00, true, "Малая сумма должна быть одобрена"),
                    Arguments.of(FRAUD_CHECK_THRESHOLD - 0.01, true, "Сумма чуть ниже порога должна быть одобрена"),
                    Arguments.of(FRAUD_CHECK_THRESHOLD, false, "Сумма на пороге должна отправиться на проверку"),
                    Arguments.of(FRAUD_CHECK_THRESHOLD * 2.5, false, "Средняя сумма должна отправиться на проверку"),
                    Arguments.of(FRAUD_CHECK_THRESHOLD * 5, false, "Крупная сумма должна отправиться на проверку"),
                    Arguments.of(BLOCK_THRESHOLD - 0.01, false, "Сумма чуть ниже порога блокировки должна отправиться на проверку"),
                    Arguments.of(BLOCK_THRESHOLD, false, "Сумма на пороге блокировки должна быть заблокирована")
            );
        }

        @ParameterizedTest(name = "[{index}] Сумма: ${0} -> Ожидаем перевод: {1} ({2})")
        @MethodSource("amountAndExpectationProvider")
        @Order(8)
        @DisplayName("Проверка пороговых сумм для anti-fraud")
        @Disabled("BACKEND-1234: Не реализована проверка суммы для anti-fraud. " +
                "Ожидаемое поведение: суммы >= " + FRAUD_CHECK_THRESHOLD + " НЕ должны выполняться, уходить на MANUAL_REVIEW. " +
                "Фактическое поведение: суммы " + FRAUD_CHECK_THRESHOLD + "-5000 выполняются, суммы >= 9999.99 возвращают HTTP 400. " +
                "Решение: добавить проверку суммы в сервисе переводов. Убрать @Disabled после исправления.")
        void amountThresholds_shouldDetermineBehavior(double amount, boolean expectTransfer, String reason) {
            double senderBefore = getAccountBalanceFromDb(senderAccountId);
            double receiverBefore = getAccountBalanceFromDb(receiverAccountId);

            executeTransfer(amount);

            double senderAfter = getAccountBalanceFromDb(senderAccountId);
            double receiverAfter = getAccountBalanceFromDb(receiverAccountId);

            if (expectTransfer) {
                assertAll(
                        () -> assertThat(senderAfter).as(reason).isCloseTo(senderBefore - amount, within(BALANCE_DELTA)),
                        () -> assertThat(receiverAfter).as(reason).isCloseTo(receiverBefore + amount, within(BALANCE_DELTA))
                );
            } else {
                assertAll(
                        () -> assertThat(senderAfter).as(reason).isCloseTo(senderBefore, within(BALANCE_DELTA)),
                        () -> assertThat(receiverAfter).as(reason).isCloseTo(receiverBefore, within(BALANCE_DELTA))
                );
            }
        }
    }

    @Nested
    @DisplayName("Проверка согласованности базы данных")
    class DatabaseConsistencyTests {

        @Test
        @Order(12)
        @FraudCheckMock(port = WIREMOCK_PORT, endpoint = FRAUD_ENDPOINT, decision = "APPROVED")
        @DisplayName("Multiple approved transfers maintain correct cumulative balances")
        void multipleApprovedTransfers_cumulativeBalancesCorrect() {
            double totalSent = 0;

            for (double amount : CUMULATIVE_TRANSFER_AMOUNTS) {
                executeTransfer(amount);
                totalSent += amount;
            }

            assertBalancesChanged(totalSent, totalSent);
        }

        @Test
        @Order(13)
        @FraudCheckMock(port = WIREMOCK_PORT, endpoint = FRAUD_ENDPOINT, decision = "APPROVED", riskScore = 0.15)
        @DisplayName("Account balance never goes negative")
        void accountBalance_neverNegative() {
            double availableBalance = getAccountBalanceFromDb(senderAccountId);
            double attemptAmount = availableBalance + 1000.00;

            UserSteps.transferAndExpectError(
                    sender.getToken(),
                    senderAccountId.intValue(),
                    receiverAccountId.intValue(),
                    attemptAmount,
                    BAD_REQUEST
            );

            AccountDao senderAfter = DataBaseSteps.getAccountById(senderAccountId);
            assertThat(senderAfter.getBalance()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @Order(14)
        @FraudCheckMock(port = WIREMOCK_PORT, endpoint = FRAUD_ENDPOINT, decision = "APPROVED")
        @DisplayName("Transfer from non-existent account returns 403")
        void transferFromNonExistentAccount_returns404() {
            UserSteps.transferAndExpectError(
                    sender.getToken(),
                    NON_EXISTENT_ACCOUNT_ID,
                    receiverAccountId.intValue(),
                    100.00,
                    FORBIDDEN
            );
        }
    }

    @Nested
    @DisplayName("Конкурентность и состояния гонки")
    class ConcurrencyTests {

        @Test
        @Order(15)
        @FraudCheckMock(port = WIREMOCK_PORT, endpoint = FRAUD_ENDPOINT, decision = "APPROVED")
        @DisplayName("Concurrent transfers from same account should be atomic")
        void concurrentTransfers_atomicityGuaranteed() throws InterruptedException {
            double balanceBefore = getAccountBalanceFromDb(senderAccountId);

            ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
            CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
            AtomicInteger successfulTransfers = new AtomicInteger(0);
            AtomicInteger failedTransfers = new AtomicInteger(0);

            for (int i = 0; i < CONCURRENT_THREADS; i++) {
                executor.submit(() -> {
                    try {
                        UserSteps.transfer(
                                sender.getToken(),
                                senderAccountId.intValue(),
                                receiverAccountId.intValue(),
                                CONCURRENT_TRANSFER_AMOUNT
                        );
                        successfulTransfers.incrementAndGet();
                    } catch (Exception e) {
                        failedTransfers.incrementAndGet();
                        log.debug("Перевод не удался (ожидаемо из-за состояния гонки): {}", e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(DEFAULT_SECONDS, TimeUnit.SECONDS);
            executor.shutdown();
            executor.awaitTermination(THREAD_POOL_SHUTDOWN_SECONDS, TimeUnit.SECONDS);

            waitForFraudCheckCompletion();
            Thread.sleep(FRAUD_CHECK_DELAY_MS);

            double balanceAfter = getAccountBalanceFromDb(senderAccountId);
            double actualDecrease = balanceBefore - balanceAfter;

            log.info("Результаты конкурентного теста - Успешно: {}, Неудачно: {}, Фактическое списание: {}",
                    successfulTransfers.get(), failedTransfers.get(), actualDecrease);

            assertThat(actualDecrease)
                    .as("Общее списание с баланса должно равняться сумме успешных переводов")
                    .isCloseTo(successfulTransfers.get() * CONCURRENT_TRANSFER_AMOUNT, within(BALANCE_DELTA * CONCURRENT_THREADS));

            assertThat(successfulTransfers.get())
                    .as("Хотя бы один перевод должен быть успешным")
                    .isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Безопасность и авторизация")
    class SecurityTests {

        @Test
        @Order(17)
        @DisplayName("Transfer without authentication returns 401")
        void transferWithoutAuth_returns401() {
            UserSteps.transferAndExpectError(
                    "",
                    senderAccountId.intValue(),
                    receiverAccountId.intValue(),
                    100.00,
                    UNAUTHORIZED
            );
        }

        @Test
        @Order(18)
        @DisplayName("Transfer with invalid token returns 401")
        void transferWithInvalidToken_returns401() {
            UserSteps.transferAndExpectError(
                    "invalid.token.12345",
                    senderAccountId.intValue(),
                    receiverAccountId.intValue(),
                    100.00,
                    UNAUTHORIZED
            );
        }

        @Test
        @Order(19)
        @DisplayName("User cannot transfer from another user's account")
        void userCannotTransferFromAnotherAccount_forbidden() {
            UserSteps.transferAndExpectError(
                    receiver.getToken(),
                    senderAccountId.intValue(),
                    receiverAccountId.intValue(),
                    100.00,
                    FORBIDDEN
            );
        }
    }

    @Nested
    @DisplayName("Пограничные случаи и валидация входных данных")
    class EdgeCasesTests {

        @Test
        @Order(20)
        @DisplayName("Zero amount transfer is rejected")
        void zeroAmount_rejected() {
            UserSteps.transferAndExpectError(
                    sender.getToken(),
                    senderAccountId.intValue(),
                    receiverAccountId.intValue(),
                    0.00,
                    BAD_REQUEST
            );
        }

        @Test
        @Order(21)
        @DisplayName("Negative amount transfer is rejected")
        void negativeAmount_rejected() {
            UserSteps.transferAndExpectError(
                    sender.getToken(),
                    senderAccountId.intValue(),
                    receiverAccountId.intValue(),
                    -100.00,
                    BAD_REQUEST
            );
        }

        @ParameterizedTest
        @Order(22)
        @ValueSource(doubles = {MIN_POSITIVE_AMOUNT, 0.10, 0.99, 1.00})
        @DisplayName("Very small but valid amounts should work")
        void verySmallAmounts_shouldWork(double amount) {
            double senderBefore = getAccountBalanceFromDb(senderAccountId);
            double receiverBefore = getAccountBalanceFromDb(receiverAccountId);

            executeTransfer(amount);

            assertAll(
                    () -> assertThat(getAccountBalanceFromDb(senderAccountId))
                            .isCloseTo(senderBefore - amount, within(BALANCE_DELTA)),
                    () -> assertThat(getAccountBalanceFromDb(receiverAccountId))
                            .isCloseTo(receiverBefore + amount, within(BALANCE_DELTA))
            );
        }

        @Test
        @Order(23)
        @DisplayName("Transfer to self (same user, different account)")
        void selfTransfer_handledCorrectly() {
            Long secondAccountId = createTrackedAccount(sender.getToken());
            UserSteps.deposit(sender.getToken(), secondAccountId.intValue(), DEFAULT_DEPOSIT_AMOUNT);

            double firstAccountBefore = getAccountBalanceFromDb(senderAccountId);
            double secondAccountBefore = getAccountBalanceFromDb(secondAccountId);
            double transferAmount = 200.00;

            UserSteps.transfer(sender.getToken(), senderAccountId.intValue(), secondAccountId.intValue(), transferAmount);
            waitForFraudCheckCompletion();

            double firstAccountAfter = getAccountBalanceFromDb(senderAccountId);
            double secondAccountAfter = getAccountBalanceFromDb(secondAccountId);

            boolean transferCompleted = Math.abs(firstAccountAfter - (firstAccountBefore - transferAmount)) < BALANCE_DELTA;
            boolean transferRejected = Math.abs(firstAccountAfter - firstAccountBefore) < BALANCE_DELTA;

            assertThat(transferCompleted || transferRejected)
                    .as("Перевод самому себе должен либо выполниться, либо быть отклоненным последовательно")
                    .isTrue();
        }
    }

    private static class TransferTestContext {
        private double senderInitialBalance;
        private double receiverInitialBalance;
        private int transferCount = 0;
        private double totalTransferred = 0.0;

        void captureInitialBalances(double senderBalance, double receiverBalance) {
            this.senderInitialBalance = senderBalance;
            this.receiverInitialBalance = receiverBalance;
        }

        void recordTransfer(double amount) {
            transferCount++;
            totalTransferred += amount;
        }

        double getSenderInitialBalance() { return senderInitialBalance; }
        double getReceiverInitialBalance() { return receiverInitialBalance; }
        int getTransferCount() { return transferCount; }
        double getTotalTransferred() { return totalTransferred; }

        void reset() {
            transferCount = 0;
            totalTransferred = 0.0;
        }
    }
}