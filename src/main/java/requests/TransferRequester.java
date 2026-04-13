package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import static io.restassured.RestAssured.given;
import java.util.Locale;

public class TransferRequester {

    private final RequestSpecification requestSpec;
    private final ResponseSpecification responseSpec;

    public TransferRequester(RequestSpecification requestSpec, ResponseSpecification responseSpec) {
        this.requestSpec = requestSpec;
        this.responseSpec = responseSpec;
    }

    public ValidatableResponse post(int fromAccountId, int toAccountId, double amount) {
        String body = String.format(Locale.US, """
            {
            "senderAccountId": %d,
            "receiverAccountId": %d,
            "amount": %.2f
            }
            """, fromAccountId, toAccountId, amount);

        System.out.println("=== TRANSFER REQUEST ===");
        System.out.println("URL: /api/v1/accounts/transfer");
        System.out.println("Body: " + body);
        System.out.println("========================");

        return given()
                .spec(requestSpec)
                .body(body)
                .when()
                .post("/api/v1/accounts/transfer")
                .then()
                .log().all()
                .spec(responseSpec);
    }
}