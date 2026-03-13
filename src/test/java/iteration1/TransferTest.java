package iteration1;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
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
        RestAssured.filters(
                List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()));
    }

    private String createUserAndGetAuth(String prefix) {
        int maxPrefixLength = 15 - 8;
        String shortPrefix = prefix.length() > maxPrefixLength ?
                prefix.substring(0, maxPrefixLength) : prefix;

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
                .post("http://localhost:4111/api/v1/admin/users")
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
                .post("http://localhost:4111/api/v1/auth/login")
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
                .post("http://localhost:4111/api/v1/accounts")
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
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);
    }

    private void checkAccountBalance(String authHeader, Integer accountId, double expectedBalance) {
        given()
                .header("Authorization", authHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("find { it.id == " + accountId + " }.balance", Matchers.is((float) expectedBalance));
    }

    @ParameterizedTest
    @MethodSource("provideValidTransferData")
    public void userCanTransferValidAmountsTest(double depositAmount,
                                                double transferAmount,
                                                double expectedFromBalance,
                                                double expectedToBalance) {
        String authHeader = createUserAndGetAuth("Transfer");
        Integer fromAccountId = createAccount(authHeader);
        Integer toAccountId = createAccount(authHeader);

        depositToAccount(authHeader, fromAccountId, depositAmount);

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
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        checkAccountBalance(authHeader, fromAccountId, expectedFromBalance);
        checkAccountBalance(authHeader, toAccountId, expectedToBalance);
    }

    private static Stream<Arguments> provideValidTransferData() {
        return Stream.of(
                Arguments.of(1000.00, 300.00, 700.00, 300.00),
                Arguments.of(500.00, 500.00, 0.00, 500.00),
                Arguments.of(5000.00, 1.00, 4999.00, 1.00),
                Arguments.of(2000.00, 1999.99, 0.01, 1999.99)
        );
    }

    @ParameterizedTest
    @MethodSource("provideValidTransferToAnotherUserData")
    public void userCanTransferToAnotherUserTest(double depositAmount,
                                                 double transferAmount,
                                                 double expectedSenderBalance,
                                                 double expectedReceiverBalance) {
        String user1Auth = createUserAndGetAuth("Sender");
        String user2Auth = createUserAndGetAuth("Receiver");

        Integer fromAccountId = createAccount(user1Auth);
        Integer toAccountId = createAccount(user2Auth);

        depositToAccount(user1Auth, fromAccountId, depositAmount);

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
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        checkAccountBalance(user1Auth, fromAccountId, expectedSenderBalance);
        checkAccountBalance(user2Auth, toAccountId, expectedReceiverBalance);
    }

    private static Stream<Arguments> provideValidTransferToAnotherUserData() {
        return Stream.of(
                Arguments.of(1000.00, 400.00, 600.00, 400.00),
                Arguments.of(2000.00, 1000.00, 1000.00, 1000.00),
                Arguments.of(500.00, 250.00, 250.00, 250.00)
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidTransferData")
    public void userCannotMakeInvalidTransferTest(double amount,
                                                  String accountType,
                                                  int expectedStatusCode) {
        String authHeader = createUserAndGetAuth("TransferNeg");
        Integer fromAccountId = createAccount(authHeader);

        depositToAccount(authHeader, fromAccountId, 1000.00);

        Integer targetAccountId;
        switch (accountType) {
            case "valid":
                targetAccountId = createAccount(authHeader);
                break;
            case "non-existent-acc":
                targetAccountId = 999999;
                break;
            case "someone-elses-acc":
                String otherAuth = createUserAndGetAuth("Other");
                targetAccountId = createAccount(otherAuth);
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
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(expectedStatusCode);
    }

    private static Stream<Arguments> provideInvalidTransferData() {
        return Stream.of(
                Arguments.of(-50.00, "valid", HttpStatus.SC_BAD_REQUEST),
                Arguments.of(0.00, "valid", HttpStatus.SC_BAD_REQUEST),
                Arguments.of(10001.00, "valid", HttpStatus.SC_BAD_REQUEST),
                Arguments.of(999999.00, "valid", HttpStatus.SC_BAD_REQUEST),
                Arguments.of(100.00, "non-existent-acc", HttpStatus.SC_BAD_REQUEST),
                Arguments.of(100.00, "someone-elses-acc", HttpStatus.SC_OK)
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
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(expectedStatusCode);
    }

    private static Stream<Arguments> provideUnauthorizedData() {
        return Stream.of(
                Arguments.of("", HttpStatus.SC_UNAUTHORIZED),
                Arguments.of("Bearer invalid.token", HttpStatus.SC_UNAUTHORIZED),
                Arguments.of("Basic invalid", HttpStatus.SC_UNAUTHORIZED)
        );
    }
}