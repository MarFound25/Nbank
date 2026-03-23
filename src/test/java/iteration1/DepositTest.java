package iteration1;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
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

public class DepositTest {

    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.baseURI = "http://localhost:4111";

        RestAssured.filters(
                List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()));
    }


    private String createUserAndGetAuth() {
        String username = "User" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

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

    private static Stream<Arguments> provideValidDepositData() {
        return Stream.of(
                Arguments.of(1.00),
                Arguments.of(100.00),
                Arguments.of(999.99),
                Arguments.of(1000.00),
                Arguments.of(5000.00)
        );
    }

    @ParameterizedTest
    @MethodSource("provideValidDepositData")
    public void userCanDepositValidAmountsTest(double amount) {
        String authHeader = createUserAndGetAuth();
        Integer accountId = createAccount(authHeader);

        double initialBalance = getAccountBalance(authHeader, accountId);

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
                .statusCode(HttpStatus.SC_OK)
                .body("balance", Matchers.is((float) (initialBalance + amount)));

        double finalBalance = getAccountBalance(authHeader, accountId);
        org.junit.jupiter.api.Assertions.assertEquals(initialBalance + amount, finalBalance, 0.01);
    }

    private static Stream<Arguments> provideInvalidDepositData() {
        return Stream.of(
                Arguments.of(-100.00, "own", HttpStatus.SC_BAD_REQUEST),
                Arguments.of(0.00, "own", HttpStatus.SC_BAD_REQUEST),
                Arguments.of(5001.00, "own", HttpStatus.SC_BAD_REQUEST),
                Arguments.of(100.00, "someone-elses-acc", HttpStatus.SC_FORBIDDEN),
                Arguments.of(100.00, "non-existent-acc", HttpStatus.SC_FORBIDDEN)
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidDepositData")
    public void userCannotMakeInvalidDepositTest(double amount,
                                                 String accountType,
                                                 int expectedStatusCode) {
        String authHeader = createUserAndGetAuth();
        Integer ownAccountId = createAccount(authHeader);

        double ownInitialBalance = getAccountBalance(authHeader, ownAccountId);

        Integer targetAccountId;
        double targetInitialBalance = 0.0;
        String targetAuthHeader = null;

        switch (accountType) {
            case "own":
                targetAccountId = ownAccountId;
                targetInitialBalance = ownInitialBalance;
                targetAuthHeader = authHeader;
                break;
            case "someone-elses-acc":
                String otherAuth = createUserAndGetAuth();
                targetAccountId = createAccount(otherAuth);
                targetInitialBalance = getAccountBalance(otherAuth, targetAccountId);
                targetAuthHeader = otherAuth;
                break;
            case "non-existent-acc":
                targetAccountId = 999999;
                break;
            default:
                targetAccountId = ownAccountId;
        }

        given()
                .header("Authorization", authHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format(Locale.US, """
                        {
                        "id": %d,
                        "balance": %.2f
                        }
                        """, targetAccountId, amount))
                .post("/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(expectedStatusCode);

        double ownFinalBalance = getAccountBalance(authHeader, ownAccountId);
        org.junit.jupiter.api.Assertions.assertEquals(ownInitialBalance, ownFinalBalance, 0.01);

        if ("own".equals(accountType) || "someone-elses-acc".equals(accountType)) {
            double targetFinalBalance = getAccountBalance(targetAuthHeader, targetAccountId);
            org.junit.jupiter.api.Assertions.assertEquals(targetInitialBalance, targetFinalBalance, 0.01);
        }
    }

    private static Stream<Arguments> provideUnauthorizedData() {
        return Stream.of(
                Arguments.of("", HttpStatus.SC_UNAUTHORIZED),
                Arguments.of("Bearer invalid.token", HttpStatus.SC_UNAUTHORIZED),
                Arguments.of("Basic invalid", HttpStatus.SC_UNAUTHORIZED)
        );
    }

    @ParameterizedTest
    @MethodSource("provideUnauthorizedData")
    public void userCannotDepositWithoutAuthTest(String authHeader, int expectedStatusCode) {
        given()
                .header("Authorization", authHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                        "id": 1,
                        "balance": 100.00
                        }
                        """)
                .post("/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(expectedStatusCode);
    }

    @Test
    public void userCanDepositMaxLimitAmountTest() {
        String authHeader = createUserAndGetAuth();
        Integer accountId = createAccount(authHeader);

        double initialBalance = getAccountBalance(authHeader, accountId);

        given()
                .header("Authorization", authHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format(Locale.US, """
                        {
                        "id": %d,
                        "balance": %.2f
                        }
                        """, accountId, 5000.00))
                .post("/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("balance", Matchers.is((float) (initialBalance + 5000.0)));

        double finalBalance = getAccountBalance(authHeader, accountId);
        org.junit.jupiter.api.Assertions.assertEquals(initialBalance + 5000.0, finalBalance, 0.01);
    }

    @Test
    public void userCannotDepositAboveLimitTest() {
        String authHeader = createUserAndGetAuth();
        Integer accountId = createAccount(authHeader);

        double initialBalance = getAccountBalance(authHeader, accountId);

        given()
                .header("Authorization", authHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format(Locale.US, """
                        {
                        "id": %d,
                        "balance": %.2f
                        }
                        """, accountId, 5000.01))
                .post("/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        double finalBalance = getAccountBalance(authHeader, accountId);
        org.junit.jupiter.api.Assertions.assertEquals(initialBalance, finalBalance, 0.01);
    }
}