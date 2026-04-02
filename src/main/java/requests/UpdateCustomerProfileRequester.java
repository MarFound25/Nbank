package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.UpdateProfileRequest;

import static io.restassured.RestAssured.given;

public class UpdateCustomerProfileRequester {

    private final RequestSpecification requestSpec;
    private final ResponseSpecification responseSpec;

    public UpdateCustomerProfileRequester(RequestSpecification requestSpec, ResponseSpecification responseSpec) {
        this.requestSpec = requestSpec;
        this.responseSpec = responseSpec;
    }

    public ValidatableResponse put(UpdateProfileRequest request) {
        return given()
                .spec(requestSpec)
                .body(request)
                .when()
                .put("/api/v1/customer/profile")
                .then()
                .spec(responseSpec);
    }
}