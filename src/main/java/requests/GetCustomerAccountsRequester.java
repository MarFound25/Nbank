package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import static io.restassured.RestAssured.given;

public class GetCustomerAccountsRequester {

    private final RequestSpecification requestSpec;
    private final ResponseSpecification responseSpec;

    public GetCustomerAccountsRequester(RequestSpecification requestSpec, ResponseSpecification responseSpec) {
        this.requestSpec = requestSpec;
        this.responseSpec = responseSpec;
    }

    public ValidatableResponse get() {
        return given()
                .spec(requestSpec)
                .when()
                .get("/api/v1/customer/accounts")
                .then()
                .spec(responseSpec);
    }

    public double getBalance(Integer accountId) {
        Float balance = get()
                .extract()
                .jsonPath()
                .getFloat("find { it.id == " + accountId + " }.balance");
        return balance != null ? balance.doubleValue() : 0.0;
    }
}