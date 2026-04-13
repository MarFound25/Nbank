package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.LoginUserRequest;

import static io.restassured.RestAssured.given;

public class LoginUserRequester {

    private final RequestSpecification requestSpec;
    private final ResponseSpecification responseSpec;

    public LoginUserRequester(RequestSpecification requestSpec, ResponseSpecification responseSpec) {
        this.requestSpec = requestSpec;
        this.responseSpec = responseSpec;
    }

    public ValidatableResponse post(LoginUserRequest request) {
        return given()
                .spec(requestSpec)
                .body(request)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .spec(responseSpec);
    }

    public String getToken(LoginUserRequest request) {
        return given()
                .spec(requestSpec)
                .body(request)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .extract()
                .header("Authorization");
    }
}