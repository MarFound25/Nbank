package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.Transaction;

import java.util.List;

import static io.restassured.RestAssured.given;

public class GetAccountTransactionsRequester {

    private final RequestSpecification requestSpec;
    private final ResponseSpecification responseSpec;

    public GetAccountTransactionsRequester(RequestSpecification requestSpec, ResponseSpecification responseSpec) {
        this.requestSpec = requestSpec;
        this.responseSpec = responseSpec;
    }

    public ValidatableResponse get(int accountId) {
        return given()
                .spec(requestSpec)
                .when()
                .get("/api/v1/accounts/" + accountId + "/transactions")
                .then()
                .spec(responseSpec);
    }

    public List<Transaction> getTransactions(int accountId) {
        return get(accountId)
                .extract()
                .jsonPath()
                .getList("", Transaction.class);
    }
}