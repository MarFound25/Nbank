package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

import java.util.Locale;

import static io.restassured.RestAssured.given;

public class DepositRequester {

    private final RequestSpecification requestSpec;
    private final ResponseSpecification responseSpec;

    public DepositRequester(RequestSpecification requestSpec, ResponseSpecification responseSpec) {
        this.requestSpec = requestSpec;
        this.responseSpec = responseSpec;
    }

    public ValidatableResponse post(int accountId, double amount) {
        String body = String.format(Locale.US, """
                {
                "id": %d,
                "balance": %.2f
                }
                """, accountId, amount);

        System.out.println("=== DEPOSIT REQUEST ===");
        System.out.println("URL: /api/v1/accounts/deposit");
        System.out.println("Body: " + body);
        System.out.println("========================");

        return given()
                .spec(requestSpec)
                .body(body)
                .when()
                .post("/api/v1/accounts/deposit")
                .then()
                .spec(responseSpec);
    }
}