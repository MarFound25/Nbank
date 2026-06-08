package requests.skelethon.requesters;

import endpoints.Endpoint;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.BaseModel;
import models.CreateUserResponse;
import requests.skelethon.HttpRequest;
import requests.skelethon.interfaces.CrudEndpointInterface;
import requests.skelethon.interfaces.GetAllEndpointInterface;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import static io.restassured.RestAssured.*;

public class CrudRequesters extends HttpRequest implements CrudEndpointInterface, GetAllEndpointInterface {
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

    @Override
    public Object post(BaseModel model) {
        return null;
    }

    @Override
    public Object get(long id) {
        return null;
    }

    @Override
    public Object update(long id, BaseModel model) {
        return null;
    }

    public ValidatableResponse delete(long id) {
        return deleteWithValidation(Endpoint.ADMIN_USER_BY_ID, id)
                .spec(responseSpec);
    }

    @Override
    public ValidatableResponse getAll(Class<?> clazz) {
        return (ValidatableResponse) given()
                .spec(RequestSpecs.adminSpec())
                .when()
                .get(Endpoint.ADMIN_USERS)
                .then()
                .spec(ResponseSpecs.requestReturnsOK())
                .extract()
                .jsonPath()
                .getList("", CreateUserResponse.class);
    }
}