package requests.steps;

import configs.Config;
import requests.skelethon.Endpoint;
import io.restassured.response.Response;
import models.AccountDTO;
import models.CreateAccountResponse;
import models.LoginUserRequest;
import models.ProfileResponse;
import models.Transaction;
import models.TransferResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import specs.RequestSpecs;
import specs.ResponseSpecs;
import common.utils.JsonUtils;
import api.dao.AccountDao;
import java.util.List;
import java.util.Locale;

import static io.restassured.RestAssured.given;

public class UserSteps {

    private static final Logger log = LoggerFactory.getLogger(UserSteps.class);
    private String username;
    private String password;

    public UserSteps(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public List<CreateAccountResponse> getAllAccounts() {
        String body = given()
                .spec(RequestSpecs.authAsUser(username, password))
                .when()
                .get(Endpoint.CUSTOMER_ACCOUNTS)
                .then()
                .spec(ResponseSpecs.requestReturnsOK())
                .extract()
                .asString();
        return JsonUtils.readList(body, CreateAccountResponse.class);
    }

    public static String loginAndGetToken(String username, String password) {
        LoginUserRequest loginRequest = LoginUserRequest.builder()
                .username(username)
                .password(password)
                .build();

        return given()
                .spec(RequestSpecs.unauthSpec())
                .body(loginRequest)
                .when()
                .post(Endpoint.AUTH_LOGIN)
                .then()
                .spec(ResponseSpecs.requestReturnsOK())
                .extract()
                .header("Authorization");
    }

    public static void loginAndExpectUnauthorized(String username, String password) {
        LoginUserRequest loginRequest = LoginUserRequest.builder()
                .username(username)
                .password(password)
                .build();

        given()
                .spec(RequestSpecs.unauthSpec())
                .body(loginRequest)
                .when()
                .post(Endpoint.AUTH_LOGIN)
                .then()
                .spec(ResponseSpecs.requestReturnsUnauthorized());
    }

    public static int createAccount(String token) {
        return given()
                .spec(RequestSpecs.authWithToken(token))
                .when()
                .post(Endpoint.ACCOUNTS)
                .then()
                .spec(ResponseSpecs.entityWasCreated())
                .extract()
                .jsonPath()
                .getInt("id");
    }

    public static List<AccountDTO> getAccounts(String token) {
        io.restassured.response.Response response = given()
                .spec(RequestSpecs.authWithToken(token))
                .when()
                .get(Endpoint.CUSTOMER_ACCOUNTS);
        response.then().statusCode(200);
        return JsonUtils.readList(response.asString(), AccountDTO.class);
    }

    @Deprecated
    public static double getAccountBalance(String token, int accountId) {
        AccountDao account = DataBaseSteps.getAccountById((long) accountId);
        if (account == null) {
            throw new RuntimeException("Account not found: " + accountId);
        }
        return account.getBalance();
    }

    public static ProfileResponse getProfile(String token) {
        return given()
                .spec(RequestSpecs.authWithToken(token))
                .when()
                .get(Endpoint.CUSTOMER_PROFILE)
                .then()
                .spec(ResponseSpecs.requestReturnsOK())
                .extract()
                .as(ProfileResponse.class);
    }

    public static void deposit(String token, int accountId, double amount) {
        String requestBody = String.format(Locale.US, "{\"id\": %d, \"balance\": %.2f}", accountId, amount);

        System.out.println("=== [DEPOSIT DEBUG] ===");
        System.out.println("URL: " + Config.getBaseUrl() + Endpoint.ACCOUNTS_DEPOSIT);
        System.out.println("Request body: " + requestBody);
        System.out.println("Token: " + (token != null ? token.substring(0, Math.min(50, token.length())) + "..." : "null"));
        System.out.println("Account ID: " + accountId);
        System.out.println("Amount: " + amount);

        Response response = given()
                .spec(RequestSpecs.authWithToken(token))
                .body(requestBody)
                .when()
                .post(Endpoint.ACCOUNTS_DEPOSIT);

        int status = response.statusCode();
        System.out.println("Response status: " + status);
        System.out.println("=== [DEPOSIT DEBUG END] ===");

        if (status != 200) {
            throw new RuntimeException("Deposit failed. Status: " + status);
        }
    }

    public static void depositAndExpectBadRequest(String token, int accountId, double amount) {
        given()
                .spec(RequestSpecs.authWithToken(token))
                .body(String.format(Locale.US, "{\"id\": %d, \"balance\": %.2f}", accountId, amount))
                .when()
                .post(Endpoint.ACCOUNTS_DEPOSIT)
                .then()
                .spec(ResponseSpecs.requestReturnsBadRequest());
    }

    public static void depositAndExpectForbidden(String token, int accountId, double amount) {
        given()
                .spec(RequestSpecs.authWithToken(token))
                .body(String.format(Locale.US, "{\"id\": %d, \"balance\": %.2f}", accountId, amount))
                .when()
                .post(Endpoint.ACCOUNTS_DEPOSIT)
                .then()
                .spec(ResponseSpecs.requestReturnsForbidden());
    }

    public static void depositAndExpectUnauthorized(int accountId, double amount) {
        given()
                .spec(RequestSpecs.noAuthSpec())
                .body(String.format(Locale.US, "{\"id\": %d, \"balance\": %.2f}", accountId, amount))
                .when()
                .post(Endpoint.ACCOUNTS_DEPOSIT)
                .then()
                .spec(ResponseSpecs.requestReturnsUnauthorized());
    }

    public static void depositWithInvalidTokenAndExpectUnauthorized(int accountId, double amount) {
        given()
                .spec(RequestSpecs.authWithToken("invalid.token.here"))
                .body(String.format(Locale.US, "{\"id\": %d, \"balance\": %.2f}", accountId, amount))
                .when()
                .post(Endpoint.ACCOUNTS_DEPOSIT)
                .then()
                .spec(ResponseSpecs.requestReturnsUnauthorized());
    }

    public static List<Transaction> getTransactions(String token, int accountId) {
        return given()
                .spec(RequestSpecs.authWithToken(token))
                .when()
                .get(Endpoint.ACCOUNTS_TRANSACTIONS, accountId)
                .then()
                .spec(ResponseSpecs.requestReturnsOK())
                .extract()
                .jsonPath()
                .getList("", Transaction.class);
    }

    public static void getTransactionsAndExpectForbidden(String token, int accountId) {
        given()
                .spec(RequestSpecs.authWithToken(token))
                .when()
                .get(Endpoint.ACCOUNTS_TRANSACTIONS, accountId)
                .then()
                .spec(ResponseSpecs.requestReturnsForbidden());
    }

    public static void getTransactionsAndExpectUnauthorized(int accountId) {
        given()
                .spec(RequestSpecs.noAuthSpec())
                .when()
                .get(Endpoint.ACCOUNTS_TRANSACTIONS, accountId)
                .then()
                .spec(ResponseSpecs.requestReturnsUnauthorized());
    }

    public static void getTransactionsWithInvalidTokenAndExpectUnauthorized(int accountId) {
        given()
                .spec(RequestSpecs.authWithToken("invalid.token.here"))
                .when()
                .get(Endpoint.ACCOUNTS_TRANSACTIONS, accountId)
                .then()
                .spec(ResponseSpecs.requestReturnsUnauthorized());
    }

    public static void getTransactionsWithEmptyTokenAndExpectUnauthorized(int accountId) {
        given()
                .spec(RequestSpecs.authWithToken(""))
                .when()
                .get(Endpoint.ACCOUNTS_TRANSACTIONS, accountId)
                .then()
                .spec(ResponseSpecs.requestReturnsUnauthorized());
    }

    public static void getTransactionsWithInvalidBasicAuthAndExpectUnauthorized(String basicAuth, int accountId) {
        given()
                .spec(RequestSpecs.authWithBasic(basicAuth))
                .when()
                .get(Endpoint.ACCOUNTS_TRANSACTIONS, accountId)
                .then()
                .spec(ResponseSpecs.requestReturnsUnauthorized());
    }

    public static TransferResponse transfer(String token, int fromAccount, int toAccount, double amount) {
        return given()
                .spec(RequestSpecs.authWithToken(token))
                .body(String.format(Locale.US, "{\"senderAccountId\": %d, \"receiverAccountId\": %d, \"amount\": %.2f}",
                        fromAccount, toAccount, amount))
                .when()
                .post(Endpoint.ACCOUNTS_TRANSFER)
                .then()
                .spec(ResponseSpecs.requestReturnsOK())
                .extract()
                .as(TransferResponse.class);
    }

    public static void transferAndExpectError(String token, int fromAccount, int toAccount, double amount, int expectedStatusCode) {
        given()
                .spec(RequestSpecs.authWithToken(token))
                .body(String.format(Locale.US, "{\"senderAccountId\": %d, \"receiverAccountId\": %d, \"amount\": %.2f}",
                        fromAccount, toAccount, amount))
                .when()
                .post(Endpoint.ACCOUNTS_TRANSFER)
                .then()
                .spec(ResponseSpecs.custom(expectedStatusCode));
    }
}
