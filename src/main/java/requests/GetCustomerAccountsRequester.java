package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.Account;

import java.util.List;

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

    public List<Account> getAccounts() {
        return get()
                .extract()
                .jsonPath()
                .getList("", Account.class);
    }

    public Double getBalance(Integer accountId) {
        List<Account> accounts = getAccounts();
        return accounts.stream()
                .filter(a -> a.getId().equals(accountId))
                .findFirst()
                .map(Account::getBalance)
                .orElse(0.0);
    }
}