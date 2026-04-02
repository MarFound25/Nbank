package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import static io.restassured.RestAssured.given;

public class AdminGetAllUsersRequester {

    private final RequestSpecification requestSpec;
    private final ResponseSpecification responseSpec;

    public AdminGetAllUsersRequester(RequestSpecification requestSpec, ResponseSpecification responseSpec) {
        this.requestSpec = requestSpec;
        this.responseSpec = responseSpec;
    }

    public ValidatableResponse get() {
        return given()
                .spec(requestSpec)
                .when()
                .get("/api/v1/admin/users")
                .then()
                .spec(responseSpec);
    }
}