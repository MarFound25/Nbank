package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.ProfileResponse;

import static io.restassured.RestAssured.given;

public class GetCustomerProfileRequester {

    private final RequestSpecification requestSpec;
    private final ResponseSpecification responseSpec;

    public GetCustomerProfileRequester(RequestSpecification requestSpec, ResponseSpecification responseSpec) {
        this.requestSpec = requestSpec;
        this.responseSpec = responseSpec;
    }

    public ValidatableResponse get() {
        return given()
                .spec(requestSpec)
                .when()
                .get("/api/v1/customer/profile")
                .then()
                .spec(responseSpec);
    }

    public ProfileResponse getProfile() {
        return get()
                .extract()
                .as(ProfileResponse.class);
    }
}