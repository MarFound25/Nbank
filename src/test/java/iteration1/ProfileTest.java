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
import java.util.Random;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

public class ProfileTest {
    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.filters(
                List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()));
    }

    private String createUserAndGetAuth(String name) {
        Random random = new Random();
        String username = "ProfileUser" + random.nextInt(1000);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body(String.format("""
                        {
                        "username": "%s",
                        "password": "JohnDoe01#",
                        "name": "%s",
                        "role": "USER"
                        }
                        """, username, name))
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

    @ParameterizedTest
    @MethodSource("provideValidNameData")
    public void userCanChangeNameWithValidDataTest(String oldName, String newName) {
        String authHeader = createUserAndGetAuth(oldName);

        given()
                .header("Authorization", authHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format("""
                    {
                    "name": "%s"
                    }
                    """, newName))
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("customer.name", Matchers.is(newName));
    }

    private static Stream<Arguments> provideValidNameData() {
        return Stream.of(
                Arguments.of("Old Name", "Anna Smith"),
                Arguments.of("John Wick", "John Doe"),
                Arguments.of("Test User", "Sara Connor"),
                Arguments.of("Ivanov Ivan", "Ivan Petrov"),
                Arguments.of("Maria Lopez", "Maria Garcia")
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidNameData")
    public void userCannotChangeNameWithInvalidDataTest(String oldName, String invalidName) {
        String authHeader = createUserAndGetAuth(oldName);

        given()
                .header("Authorization", authHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format("""
                        {
                        "name": "%s"
                        }
                        """, invalidName))
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    private static Stream<Arguments> provideInvalidNameData() {
        return Stream.of(
                Arguments.of("Valid Name", "John"),
                Arguments.of("Valid Name", "John Peter Smith"),
                Arguments.of("Valid Name", "John 123"),
                Arguments.of("Valid Name", "John@ Doe"),
                Arguments.of("Valid Name", ""),
                Arguments.of("Valid Name", " "),
                Arguments.of("Valid Name", "  ")
        );
    }

    @ParameterizedTest
    @MethodSource("provideUnauthorizedData")
    public void userCannotChangeNameWithoutAuthTest(String authHeader, int expectedStatusCode) {

        given()
                .header("Authorization", authHeader)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                        "name": "New Name"
                        }
                        """)
                .put("http://localhost:4111/api/v1/customer/profile")  // ← ИСПРАВЛЕНО!
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