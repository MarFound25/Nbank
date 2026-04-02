package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import static io.restassured.RestAssured.given;

public class AdminDeleteUserRequester {

    private final RequestSpecification requestSpec;
    private final ResponseSpecification responseSpec;

    public AdminDeleteUserRequester(RequestSpecification requestSpec, ResponseSpecification responseSpec) {
        this.requestSpec = requestSpec;
        this.responseSpec = responseSpec;
    }

    public ValidatableResponse delete(int userId) {
        return given()
                .spec(requestSpec)
                .when()
                .delete("/api/v1/admin/users/" + userId)
                .then()
                .spec(responseSpec);
    }
}