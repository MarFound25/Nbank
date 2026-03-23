package iteration1;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static io.restassured.RestAssured.given;

public class CreateAccountTest {

    private static final Random random = new Random();

    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.baseURI = "http://localhost:4111";

        RestAssured.filters(
                List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()));
    }


    private String createUserAndGetAuth() {
        String username = "User" + random.nextInt(1000);

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


    @Test
    public void userCanCreateAccountTest() {
        String userAuthHeader = createUserAndGetAuth();

        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .body("id", Matchers.notNullValue())
                .body("balance", Matchers.is(0.0f));
    }

    @Test
    public void userCanCreateMultipleAccountsTest() {
        String userAuthHeader = createUserAndGetAuth();

        Integer account1 = given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        Integer account2 = given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("id", Matchers.hasItems(account1, account2))
                .body("size()", Matchers.is(2));
    }

    @Test
    public void userCanGetAccountTransactionsTest() {
        String username = "John" + random.nextInt(1000);

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

        String userAuthHeader = given()
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

        Integer accountId = given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format("""
                    {
                    "id": %d,
                    "balance": 1000.00
                    }
                    """, accountId))
                .post("/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        given()
                .header("Authorization", userAuthHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/api/v1/accounts/" + accountId + "/transactions")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("$", Matchers.not(Matchers.nullValue()))
                .body("size()", Matchers.greaterThanOrEqualTo(1));
    }

    @Test
    public void userCannotGetAnotherUsersAccountTransactionsTest() {
        String user1Username = "User1_" + random.nextInt(1000);

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
                    """, user1Username))
                .post("/api/v1/admin/users")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        String user1Auth = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format("""
                    {
                    "username": "%s",
                    "password": "JohnDoe01#"
                    }
                    """, user1Username))
                .post("/api/v1/auth/login")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");

        Integer user1AccountId = given()
                .header("Authorization", user1Auth)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .post("/api/v1/accounts")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");

        String user2Username = "User2_" + random.nextInt(1000);

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
                    """, user2Username))
                .post("/api/v1/admin/users")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        String user2Auth = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format("""
                    {
                    "username": "%s",
                    "password": "JohnDoe01#"
                    }
                    """, user2Username))
                .post("/api/v1/auth/login")
                .then()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");

        given()
                .header("Authorization", user1Auth)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format("""
                    {
                    "id": %d,
                    "balance": 500.00
                    }
                    """, user1AccountId))
                .post("/api/v1/accounts/deposit")
                .then()
                .statusCode(HttpStatus.SC_OK);

        given()
                .header("Authorization", user2Auth)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/api/v1/accounts/" + user1AccountId + "/transactions")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void userCannotGetTransactionsForNonexistentAccountTest() {
        String userAuth = createUserAndGetAuth();

        given()
                .header("Authorization", userAuth)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/api/v1/accounts/999999/transactions")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void userCannotGetTransactionsWithoutAuthTest() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/api/v1/accounts/1/transactions")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    public void userCannotGetTransactionsWithInvalidTokenTest() {
        given()
                .header("Authorization", "Bearer invalid.token.here")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/api/v1/accounts/1/transactions")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    public void userCannotGetTransactionsWithInvalidBasicAuthTest() {
        given()
                .header("Authorization", "Basic invalid")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/api/v1/accounts/1/transactions")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    public void userCannotGetTransactionsWithEmptyTokenTest() {
        given()
                .header("Authorization", "")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .get("/api/v1/accounts/1/transactions")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    public void userCanViewOwnAccountsTest() {
        String userAuth = createUserAndGetAuth();

        Integer account1 = createAccount(userAuth);
        Integer account2 = createAccount(userAuth);

        given()
                .header("Authorization", userAuth)
                .get("/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("find { it.id == " + account1 + " }", Matchers.notNullValue())
                .body("find { it.id == " + account2 + " }", Matchers.notNullValue())
                .body("size()", Matchers.is(2));
    }

    @Test
    public void userCannotViewAnotherUsersAccountsTest() {
        String user1Auth = createUserAndGetAuth();
        Integer user1Account = createAccount(user1Auth);

        String user2Auth = createUserAndGetAuth();

        given()
                .header("Authorization", user2Auth)
                .get("/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("find { it.id == " + user1Account + " }", Matchers.nullValue())
                .body("size()", Matchers.is(0));
    }
}