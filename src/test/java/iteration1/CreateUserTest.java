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
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

public class CreateUserTest {
    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.filters(
                List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()));
    }

    public static Stream<Arguments> validUserData() {
        return Stream.of(
                Arguments.of("JohnDoe06", "JohnDoy05#", "USER"),
                Arguments.of("John.Doe-123_5", "Password123#", "USER")
        );
    }

    @MethodSource("validUserData")
    @ParameterizedTest(name = "Создание пользователя: username={0}, role={2}")
    public void adminCanCreateUserWithCorrectData(String username, String password, String role) {
        String requestBody = String.format(
                """
                {
                "username": "%s",
                "password": "%s",
                "role": "%s"
                }
                """, username, password, role);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body(requestBody)
                .when()
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .body("username", Matchers.equalTo(username))
                .body("password", Matchers.not(Matchers.equalTo(password)))
                .body("role", Matchers.equalTo(role));
    }

    public static Stream<Arguments> userInvalidData() {
        return Stream.of(
                Arguments.of(" ", "Password22$", "USER", "username", "Username cannot be blank"),
                Arguments.of("ab", "Password22$", "USER", "username", "Username must be between 3 and 15 characters"),
                Arguments.of("abc%", "Password22$", "USER", "username", "Username must contain only letters, digits, dashes, underscores, and dots"));
    }

    @MethodSource("userInvalidData")
    @ParameterizedTest
    public void adminCanNotCreateUserWithInvalidData(String username, String password, String role, String errorKey, String errorValue) {
        String requestBody = String.format(
                """
                        {"username":"%s",
                        "password":"%s",
                        "role":"%s"
                        }
                        """, username, password, role);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body(requestBody)
                .when()
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(errorKey, Matchers.hasItem(errorValue));
    }
}