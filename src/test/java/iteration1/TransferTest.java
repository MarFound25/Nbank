package iteration1;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

public class TransferTest {

    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.baseURI = "http://localhost:4111";

        RestAssured.filters(
                List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()));
    }

    private String createUserAndGetAuth(String prefix) {
        int maxPrefixLength = 7;
        String shortPrefix = prefix.length() > maxPrefixLength ? prefix.substring(0, maxPrefixLength) : prefix;
        String username = shortPrefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body(String.format("""
                    {
                    "username": "%s",
                    "password": "JohnDoe01#",
                    "role": "USER"
                    }
                    """, username))
                .post("/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        return given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format("""
                    {
                    "username": "%s",
                    "password": "JohnDoe01#"
                    }
                    """, username))
                .post("/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");
    }

    private Integer createAccount(String authHeader) {
        return given()
                .header("Authorization", authHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");
    }

    private void depositToAccount(String authHeader, Integer accountId, double amount) {
        given()
                .header("Authorization", authHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format(Locale.US, """
                        {
                        "id": %d,
                        "balance": %.2f
                        }
                        """, accountId, amount))
                .post("/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);
    }

    private void depositMultipleAmounts(String authHeader, Integer accountId, double totalAmount) {
        double remaining = totalAmount;
        while (remaining > 0) {
            double depositAmount = Math.min(remaining, 5000.0);
            depositToAccount(authHeader, accountId, depositAmount);
            remaining -= depositAmount;
        }
    }

    private Double getAccountBalance(String authHeader, Integer accountId) {
        Float balance = given()
                .header("Authorization", authHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .path("find { it.id == " + accountId + " }.balance");
        return balance != null ? balance.doubleValue() : 0.0;
    }

    @ParameterizedTest
    @MethodSource("provideValidTransferData")
    public void userCanTransferValidAmountsTest(double depositAmount, double transferAmount) {
        String authHeader = createUserAndGetAuth("Transfer");
        Integer fromAccountId = createAccount(authHeader);
        Integer toAccountId = createAccount(authHeader);

        depositToAccount(authHeader, fromAccountId, depositAmount);

        double fromInitialBalance = getAccountBalance(authHeader, fromAccountId);
        double toInitialBalance = getAccountBalance(authHeader, toAccountId);

        given()
                .header("Authorization", authHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format(Locale.US, """
                        {
                        "senderAccountId": %d,
                        "receiverAccountId": %d,
                        "amount": %.2f
                        }
                        """, fromAccountId, toAccountId, transferAmount))
                .post("/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        double fromFinalBalance = getAccountBalance(authHeader, fromAccountId);
        double toFinalBalance = getAccountBalance(authHeader, toAccountId);

        org.junit.jupiter.api.Assertions.assertEquals(fromInitialBalance - transferAmount, fromFinalBalance, 0.01);
        org.junit.jupiter.api.Assertions.assertEquals(toInitialBalance + transferAmount, toFinalBalance, 0.01);
    }

    private static Stream<Arguments> provideValidTransferData() {
        return Stream.of(
                Arguments.of(1000.0, 300.0, 700.0, 300.0),
                Arguments.of(500.0, 500.0, 0.0, 500.0),
                Arguments.of(5000.0, 1.0, 4999.0, 1.0),
                Arguments.of(2000.0, 1999.99, 0.01, 1999.99)
        );
    }

    @ParameterizedTest
    @MethodSource("provideValidTransferToAnotherUserData")
    public void userCanTransferToAnotherUserTest(double depositAmount, double transferAmount) {
        String user1Auth = createUserAndGetAuth("Sender");
        String user2Auth = createUserAndGetAuth("Receiver");
        Integer fromAccountId = createAccount(user1Auth);
        Integer toAccountId = createAccount(user2Auth);

        depositToAccount(user1Auth, fromAccountId, depositAmount);

        double fromInitialBalance = getAccountBalance(user1Auth, fromAccountId);
        double toInitialBalance = getAccountBalance(user2Auth, toAccountId);

        given()
                .header("Authorization", user1Auth)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format(Locale.US, """
                        {
                        "senderAccountId": %d,
                        "receiverAccountId": %d,
                        "amount": %.2f
                        }
                        """, fromAccountId, toAccountId, transferAmount))
                .post("/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        double fromFinalBalance = getAccountBalance(user1Auth, fromAccountId);
        double toFinalBalance = getAccountBalance(user2Auth, toAccountId);

        org.junit.jupiter.api.Assertions.assertEquals(fromInitialBalance - transferAmount, fromFinalBalance, 0.01);
        org.junit.jupiter.api.Assertions.assertEquals(toInitialBalance + transferAmount, toFinalBalance, 0.01);
    }

    private static Stream<Arguments> provideValidTransferToAnotherUserData() {
        return Stream.of(
                Arguments.of(1000.0, 400.0, 600.0, 400.0),
                Arguments.of(2000.0, 1000.0, 1000.0, 1000.0),
                Arguments.of(500.0, 250.0, 250.0, 250.0)
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidTransferData")
    public void userCannotMakeInvalidTransferTest(double amount, String accountType, int expectedStatusCode) {
        String authHeader = createUserAndGetAuth("TransferNeg");
        Integer fromAccountId = createAccount(authHeader);
        depositToAccount(authHeader, fromAccountId, 1000.0);

        double fromInitialBalance = getAccountBalance(authHeader, fromAccountId);

        Integer targetAccountId;
        double toInitialBalance = 0.0;
        String targetAuthHeader = null;

        switch (accountType) {
            case "valid":
                targetAccountId = createAccount(authHeader);
                toInitialBalance = getAccountBalance(authHeader, targetAccountId);
                targetAuthHeader = authHeader;
                break;
            case "non-existent-acc":
                targetAccountId = 999999;
                break;
            default:
                targetAccountId = fromAccountId;
        }

        given()
                .header("Authorization", authHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format(Locale.US, """
                        {
                        "senderAccountId": %d,
                        "receiverAccountId": %d,
                        "amount": %.2f
                        }
                        """, fromAccountId, targetAccountId, amount))
                .post("/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(expectedStatusCode);

        double fromFinalBalance = getAccountBalance(authHeader, fromAccountId);
        org.junit.jupiter.api.Assertions.assertEquals(fromInitialBalance, fromFinalBalance, 0.01);

        if ("valid".equals(accountType)) {
            double toFinalBalance = getAccountBalance(targetAuthHeader, targetAccountId);
            org.junit.jupiter.api.Assertions.assertEquals(toInitialBalance, toFinalBalance, 0.01);
        }
    }

    private static Stream<Arguments> provideInvalidTransferData() {
        return Stream.of(
                Arguments.of(-50.0, "valid", 400),
                Arguments.of(0.0, "valid", 400),
                Arguments.of(10001.0, "valid", 400),
                Arguments.of(999999.0, "valid", 400),
                Arguments.of(100.0, "non-existent-acc", 400)
        );
    }

    private static Stream<Arguments> provideUnauthorizedData() {
        return Stream.of(
                Arguments.of("", 401),
                Arguments.of("Bearer invalid.token", 401),
                Arguments.of("Basic invalid", 401)
        );
    }

    @ParameterizedTest
    @MethodSource("provideUnauthorizedData")
    public void userCannotTransferWithoutAuthTest(String authHeader, int expectedStatusCode) {
        given()
                .header("Authorization", authHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                        "senderAccountId": 1,
                        "receiverAccountId": 2,
                        "amount": 100.00
                        }
                        """)
                .post("/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(expectedStatusCode);
    }

    @Test
    public void userCannotTransferFromAnotherUsersAccountTest() {
        String user1Auth = createUserAndGetAuth("User1");
        String user2Auth = createUserAndGetAuth("User2");

        Integer user1Account = createAccount(user1Auth);
        Integer user2Account = createAccount(user2Auth);

        depositToAccount(user1Auth, user1Account, 1000.0);

        double user1InitialBalance = getAccountBalance(user1Auth, user1Account);
        double user2InitialBalance = getAccountBalance(user2Auth, user2Account);

        given()
                .header("Authorization", user2Auth)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format(Locale.US, """
                        {
                        "senderAccountId": %d,
                        "receiverAccountId": %d,
                        "amount": 100.00
                        }
                        """, user1Account, user2Account))
                .post("/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN);

        double user1FinalBalance = getAccountBalance(user1Auth, user1Account);
        double user2FinalBalance = getAccountBalance(user2Auth, user2Account);

        org.junit.jupiter.api.Assertions.assertEquals(user1InitialBalance, user1FinalBalance, 0.01);
        org.junit.jupiter.api.Assertions.assertEquals(user2InitialBalance, user2FinalBalance, 0.01);
    }

    @Test
    public void userCanTransferMaxLimitAmountTest() {
        String authHeader = createUserAndGetAuth("Transfer");
        Integer fromAccount = createAccount(authHeader);
        Integer toAccount = createAccount(authHeader);

        depositMultipleAmounts(authHeader, fromAccount, 15000.0);

        double fromInitialBalance = getAccountBalance(authHeader, fromAccount);
        double toInitialBalance = getAccountBalance(authHeader, toAccount);

        given()
                .header("Authorization", authHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format(Locale.US, """
                        {
                        "senderAccountId": %d,
                        "receiverAccountId": %d,
                        "amount": %.2f
                        }
                        """, fromAccount, toAccount, 10000.00))
                .post("/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        double fromFinalBalance = getAccountBalance(authHeader, fromAccount);
        double toFinalBalance = getAccountBalance(authHeader, toAccount);

        org.junit.jupiter.api.Assertions.assertEquals(fromInitialBalance - 10000.0, fromFinalBalance, 0.01);
        org.junit.jupiter.api.Assertions.assertEquals(toInitialBalance + 10000.0, toFinalBalance, 0.01);
    }

    @Test
    public void userCannotTransferAboveLimitTest() {
        String authHeader = createUserAndGetAuth("Transfer");
        Integer fromAccount = createAccount(authHeader);
        Integer toAccount = createAccount(authHeader);

        depositMultipleAmounts(authHeader, fromAccount, 15000.0);

        double fromInitialBalance = getAccountBalance(authHeader, fromAccount);
        double toInitialBalance = getAccountBalance(authHeader, toAccount);

        given()
                .header("Authorization", authHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format(Locale.US, """
                        {
                        "senderAccountId": %d,
                        "receiverAccountId": %d,
                        "amount": %.2f
                        }
                        """, fromAccount, toAccount, 10000.01))
                .post("/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        double fromFinalBalance = getAccountBalance(authHeader, fromAccount);
        double toFinalBalance = getAccountBalance(authHeader, toAccount);

        org.junit.jupiter.api.Assertions.assertEquals(fromInitialBalance, fromFinalBalance, 0.01);
        org.junit.jupiter.api.Assertions.assertEquals(toInitialBalance, toFinalBalance, 0.01);
    }
}