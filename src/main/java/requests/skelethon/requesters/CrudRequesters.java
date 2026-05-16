package requests.skelethon.requesters;

import endpoints.Endpoint;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import requests.skelethon.HttpRequest;

public class CrudRequesters extends HttpRequest {
    protected ResponseSpecification responseSpec;

    public CrudRequesters(RequestSpecification requestSpec) {
        super(requestSpec);
    }

    public CrudRequesters(RequestSpecification requestSpec, ResponseSpecification responseSpec) {
        super(requestSpec);
        this.responseSpec = responseSpec;
    }

    public ValidatableResponse create(Object body) {
        return postWithValidation(Endpoint.ADMIN_USERS, body)
                .spec(responseSpec);
    }

    public ValidatableResponse create(String endpoint, Object body) {
        return postWithValidation(endpoint, body)
                .spec(responseSpec);
    }

    public ValidatableResponse readAll() {
        return getWithValidation(Endpoint.ADMIN_USERS)
                .spec(responseSpec);
    }

    public ValidatableResponse readOne(long id) {
        return getWithValidation(Endpoint.ADMIN_USER_BY_ID, id)
                .spec(responseSpec);
    }

    public ValidatableResponse update(long id, Object body) {
        return putWithValidation(Endpoint.ADMIN_USER_BY_ID, body, id)
                .spec(responseSpec);
    }

    public ValidatableResponse delete(long id) {
        return deleteWithValidation(Endpoint.ADMIN_USER_BY_ID, id)
                .spec(responseSpec);
    }
}