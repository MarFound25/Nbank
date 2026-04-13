package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.CreateUserRequest;

import static io.restassured.RestAssured.given;

public class AdminCreateUserRequester {

    private final RequestSpecification requestSpec;
    private final ResponseSpecification responseSpec;

    public AdminCreateUserRequester(RequestSpecification requestSpec, ResponseSpecification responseSpec) {
        this.requestSpec = requestSpec;
        this.responseSpec = responseSpec;
    }

    public ValidatableResponse post(CreateUserRequest request) {
        return given()
                .spec(requestSpec)
                .body(request)
                .when()
                .post("/api/v1/admin/users")
                .then()
                .spec(responseSpec);
    }
}