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
import java.util.UUID;

import static io.restassured.RestAssured.given;

public class LoginUserTest {

    @BeforeAll
    public static void setupRestAssured() {
        RestAssured.baseURI = "http://localhost:4111";

        RestAssured.filters(
                List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()));
    }

    private String createTestUserAndReturnUsername() {
        String username = "login" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body(String.format("""
                    {
                    "username": "%s",
                    "password": "Valid1#Pass",
                    "role": "USER"
                    }
                    """, username))
                .post("/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        return username;
    }

    @Test
    public void adminCanGenerateAuthTokenTest() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                        "username": "admin",
                        "password": "admin"
                        }
                        """)
                .post("/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .header("Authorization", Matchers.notNullValue());
    }

    @Test
    public void userCanGenerateAuthTokenTest() {
        String username = createTestUserAndReturnUsername();

        given()
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
                .header("Authorization", Matchers.notNullValue());
    }


    @Test
    public void userCannotLoginWithWrongPasswordTest() {
        String username = createTestUserAndReturnUsername();

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format("""
                        {
                        "username": "%s",
                        "password": "WrongPassword123!"
                        }
                        """, username))
                .post("/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    public void userCannotLoginWithNonExistentUsernameTest() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                        "username": "nonexistentuser123",
                        "password": "AnyPassword123!"
                        }
                        """)
                .post("/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    public void userCannotLoginWithEmptyUsernameTest() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                        "username": "",
                        "password": "Valid1#Pass"
                        }
                        """)
                .post("/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    public void userCannotLoginWithEmptyPasswordTest() {
        String username = createTestUserAndReturnUsername();

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(String.format("""
                        {
                        "username": "%s",
                        "password": ""
                        }
                        """, username))
                .post("/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    public void userCannotLoginWithEmptyCredentialsTest() {
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body("""
                        {
                        "username": "",
                        "password": ""
                        }
                        """)
                .post("/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_UNAUTHORIZED);
    }
}