package iteration1;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.builder.RequestSpecBuilder;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;
import java.util.UUID;

import static io.restassured.RestAssured.given;

public class CreateUserTest {

    private static RequestSpecification adminRequestSpec;

    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.baseURI = "http://localhost:4111";

        RestAssured.filters(
                List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()));

        adminRequestSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addHeader("Authorization", "Basic YWRtaW46YWRtaW4=")
                .build();
    }

    private String createRegularUserAndGetAuth() {
        String username = "reguser" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        given()
                .spec(adminRequestSpec)
                .body(createUserRequestBody(username, "Valid1#Pass", "USER"))
                .post("/api/v1/admin/users")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        return given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format("""
                {
                "username": "%s",
                "password": "Valid1#Pass"
                }
                """, username))
                .post("/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .header("Authorization");
    }

    private String createUserRequestBody(String username, String password, String role) {
        return String.format(
                """
                {
                  "username": "%s",
                  "password": "%s",
                  "role": "%s"
                }
                """, username, password, role);
    }

    public static Stream<Arguments> validUserData() {
        return Stream.of(
                Arguments.of("JohnDoe06", "JohnDoy05#", "USER"),
                Arguments.of("John.Doe", "Password123#", "USER"),
                Arguments.of("john-doe", "Test1234!", "USER"),
                Arguments.of("john_doe", "Valid1#Pass", "USER"),
                Arguments.of("john123", "Secure1#Pass", "USER")
        );
    }

    @MethodSource("validUserData")
    @ParameterizedTest(name = "Создание пользователя: username={0}, role={2}")
    public void adminCanCreateUserWithCorrectData(String username, String password, String role) {
        String uniqueUsername = username + UUID.randomUUID().toString().replace("-", "").substring(0, 5);
        uniqueUsername = uniqueUsername.substring(0, Math.min(uniqueUsername.length(), 15));

        String requestBody = createUserRequestBody(uniqueUsername, password, role);

        given()
                .spec(adminRequestSpec)
                .body(requestBody)
                .when()
                .post("/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .body("username", Matchers.equalTo(uniqueUsername))
                .body("password", Matchers.not(Matchers.equalTo(password)))
                .body("role", Matchers.equalTo(role));
    }

    @Test
    public void adminCanCreateUserWithMinUsernameLengthTest() {
        String username = "a" + UUID.randomUUID().toString().replace("-", "").substring(0, 2);
        username = username.substring(0, 3);
        String requestBody = createUserRequestBody(username, "Valid1#Pass", "USER");

        given()
                .spec(adminRequestSpec)
                .body(requestBody)
                .when()
                .post("/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .body("username", Matchers.equalTo(username));
    }

    @Test
    public void adminCanCreateUserWithMaxUsernameLengthTest() {
        String username = UUID.randomUUID().toString().replace("-", "").substring(0, 15);
        String requestBody = createUserRequestBody(username, "Valid1#Pass", "USER");

        given()
                .spec(adminRequestSpec)
                .body(requestBody)
                .when()
                .post("/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .body("username", Matchers.equalTo(username));
    }

    @Test
    public void adminCanCreateUserWithAdminRoleTest() {
        String username = "adm" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String requestBody = createUserRequestBody(username, "Valid1#Pass", "ADMIN");

        given()
                .spec(adminRequestSpec)
                .body(requestBody)
                .post("/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .body("role", Matchers.equalTo("ADMIN"));
    }

    public static Stream<Arguments> userInvalidData() {
        return Stream.of(
                Arguments.of("", "Password22$", "USER", "username", "Username cannot be blank"),
                Arguments.of("ab", "Password22$", "USER", "username", "Username must be between 3 and 15 characters"),
                Arguments.of("abcdefghijklmnop", "Password22$", "USER", "username", "Username must be between 3 and 15 characters"),
                Arguments.of("abc%", "Password22$", "USER", "username", "Username must contain only letters, digits, dashes, underscores, and dots"),
                Arguments.of("john doe", "Password22$", "USER", "username", "Username must contain only letters, digits, dashes, underscores, and dots")
        );
    }

    @MethodSource("userInvalidData")
    @ParameterizedTest
    public void adminCanNotCreateUserWithInvalidUsername(String username, String password, String role,
                                                         String errorKey, String errorValue) {
        String requestBody = createUserRequestBody(username, password, role);

        given()
                .spec(adminRequestSpec)
                .body(requestBody)
                .when()
                .post("/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(errorKey, Matchers.hasItem(errorValue));
    }

    public static Stream<Arguments> invalidPasswordData() {
        String expectedMessage = "Password must contain at least one digit, one lower case, one upper case, one special character, no spaces, and be at least 8 characters long";

        return Stream.of(
                Arguments.of("testuser", "", "USER", "password", "Password cannot be blank"),
                Arguments.of("testuser", "Pass1#", "USER", "password", expectedMessage),
                Arguments.of("testuser", "password", "USER", "password", expectedMessage),
                Arguments.of("testuser", "PASSWORD", "USER", "password", expectedMessage),
                Arguments.of("testuser", "12345678", "USER", "password", expectedMessage),
                Arguments.of("testuser", "Password", "USER", "password", expectedMessage),
                Arguments.of("testuser", "Password1", "USER", "password", expectedMessage),
                Arguments.of("testuser", "Password1# ", "USER", "password", expectedMessage)
        );
    }

    @MethodSource("invalidPasswordData")
    @ParameterizedTest
    public void adminCanNotCreateUserWithInvalidPassword(String username, String password, String role,
                                                         String errorKey, String errorValue) {
        String uniqueUsername = username + UUID.randomUUID().toString().replace("-", "").substring(0, 5);
        uniqueUsername = uniqueUsername.substring(0, Math.min(uniqueUsername.length(), 15));
        String requestBody = createUserRequestBody(uniqueUsername, password, role);

        given()
                .spec(adminRequestSpec)
                .body(requestBody)
                .when()
                .post("/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(errorKey, Matchers.hasItem(errorValue));
    }

    @Test
    public void adminCannotCreateUserWithInvalidRoleTest() {
        String username = "invrole" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        String requestBody = createUserRequestBody(username, "Valid1#Pass", "EDITOR");

        given()
                .spec(adminRequestSpec)
                .body(requestBody)
                .post("/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void adminCanNotCreateUserThatAlreadyExistsTest() {
        String username = "exist" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        username = username.substring(0, Math.min(username.length(), 15));
        String password = "Valid1#Pass";
        String role = "USER";

        String requestBody = createUserRequestBody(username, password, role);

        given()
                .spec(adminRequestSpec)
                .body(requestBody)
                .when()
                .post("/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        given()
                .spec(adminRequestSpec)
                .body(requestBody)
                .when()
                .post("/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(Matchers.equalTo("Error: Username '" + username + "' already exists."));
    }

    @Test
    public void adminCanGetAllUsersTest() {
        String uniqueUsername = "list" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        given()
                .spec(adminRequestSpec)
                .body(createUserRequestBody(uniqueUsername, "Valid1#Pass", "USER"))
                .post("/api/v1/admin/users")
                .then()
                .statusCode(HttpStatus.SC_CREATED);

        given()
                .spec(adminRequestSpec)
                .get("/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("find { it.username == '" + uniqueUsername + "' }", Matchers.notNullValue());
    }

    @Test
    public void userCannotGetAllUsersWithBearerTokenTest() {
        String userAuth = createRegularUserAndGetAuth();

        given()
                .header("Authorization", userAuth)
                .get("/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void adminCanDeleteUserTest() {
        String username = "del" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        Integer userId = given()
                .spec(adminRequestSpec)
                .body(createUserRequestBody(username, "Valid1#Pass", "USER"))
                .post("/api/v1/admin/users")
                .then()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .path("id");


        given()
                .spec(adminRequestSpec)
                .delete("/api/v1/admin/users/" + userId)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                {
                "username": "%s",
                "password": "Valid1#Pass"
                }
                """, username))
                .post("/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    public void adminCannotDeleteNonExistentUserTest() {
        given()
                .spec(adminRequestSpec)
                .delete("/api/v1/admin/users/999999")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void userCannotDeleteUserTest() {
        String userAuth = createRegularUserAndGetAuth();

        given()
                .header("Authorization", userAuth)
                .delete("/api/v1/admin/users/1")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN);
    }
}