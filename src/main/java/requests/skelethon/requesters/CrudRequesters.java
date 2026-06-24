package requests.skelethon.requesters;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.BaseModel;
import requests.skelethon.HttpRequest;
import requests.skelethon.interfaces.CrudEndpointInterface;
import requests.skelethon.interfaces.GetAllEndpointInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.restassured.RestAssured.given;

public class CrudRequesters extends HttpRequest implements CrudEndpointInterface, GetAllEndpointInterface {

    private static final Logger log = LoggerFactory.getLogger(CrudRequesters.class);
    private static final String DEFAULT_ENDPOINT = "admin/users";

    protected ResponseSpecification responseSpec;
    private String baseEndpoint;
    private Class<?> responseModel;

    public CrudRequesters(RequestSpecification requestSpec) {
        super(requestSpec);
    }

    public CrudRequesters(RequestSpecification requestSpec, ResponseSpecification responseSpec) {
        super(requestSpec);
        this.responseSpec = responseSpec;
    }

    public CrudRequesters(RequestSpecification requestSpec, String endpoint, ResponseSpecification responseSpec) {
        super(requestSpec);
        this.baseEndpoint = endpoint;
        this.responseSpec = responseSpec;
    }

    public void setResponseModel(Class<?> responseModel) {
        this.responseModel = responseModel;
    }

    private String getEndpoint() {
        return baseEndpoint != null ? baseEndpoint : DEFAULT_ENDPOINT;
    }

    public ValidatableResponse create(Object body) {
        return postWithValidation(getEndpoint(), body)
                .spec(responseSpec);
    }

    public ValidatableResponse create(String endpoint, Object body) {
        return postWithValidation(endpoint, body)
                .spec(responseSpec);
    }

    public ValidatableResponse readAll() {
        return getWithValidation(getEndpoint())
                .spec(responseSpec);
    }

    // ✅ НОВЫЙ МЕТОД - для URL с {id} через pathParam
    public ValidatableResponse readOneWithPathParam(long id) {
        return given()
                .spec(requestSpec)
                .pathParam("id", id)
                .when()
                .get(getEndpoint())
                .then()
                .spec(responseSpec);
    }

    // ✅ СТАРЫЙ МЕТОД - для URL с конкатенацией (оставляем для совместимости)
    public ValidatableResponse readOne(long id) {
        return getWithValidation(getEndpoint() + "/" + id)
                .spec(responseSpec);
    }

    // ✅ НОВЫЙ МЕТОД - для URL с {id} через pathParam
    public ValidatableResponse updateWithPathParam(long id, Object body) {
        return given()
                .spec(requestSpec)
                .pathParam("id", id)
                .body(body)
                .when()
                .put(getEndpoint())
                .then()
                .spec(responseSpec);
    }

    // ✅ СТАРЫЙ МЕТОД
    public ValidatableResponse update(long id, Object body) {
        return putWithValidation(getEndpoint() + "/" + id, body)
                .spec(responseSpec);
    }

    // ✅ НОВЫЙ МЕТОД - для URL с {id} через pathParam
    public ValidatableResponse deleteWithPathParam(long id) {
        return given()
                .spec(requestSpec)
                .pathParam("id", id)
                .when()
                .delete(getEndpoint())
                .then()
                .spec(responseSpec);
    }

    // ✅ СТАРЫЙ МЕТОД
    public ValidatableResponse delete(long id) {
        return deleteWithValidation(getEndpoint() + "/" + id)
                .spec(responseSpec);
    }

    @Override
    public Object post(BaseModel model) {
        var response = postWithValidation(baseEndpoint, model)
                .spec(responseSpec)
                .extract();

        String responseBody = response.asString();
        if (responseBody == null || responseBody.trim().isEmpty()) {
            log.debug("Empty response body received for POST request to {}", baseEndpoint);
            return null;
        }

        try {
            return response.as(responseModel);
        } catch (Exception e) {
            log.error("Failed to deserialize response for POST request to {}: {}", baseEndpoint, e.getMessage());
            return null;
        }
    }

    @Override
    public Object get(long id) {
        var response = getWithValidation(baseEndpoint + "/" + id)
                .spec(responseSpec)
                .extract();

        String responseBody = response.asString();
        if (responseBody == null || responseBody.trim().isEmpty()) {
            log.debug("Empty response body received for GET request to {}/{}", baseEndpoint, id);
            return null;
        }

        try {
            return response.as(responseModel);
        } catch (Exception e) {
            log.error("Failed to deserialize response for GET request to {}/{}: {}", baseEndpoint, id, e.getMessage());
            return null;
        }
    }

    @Override
    public Object update(long id, BaseModel model) {
        var response = putWithValidation(baseEndpoint + "/" + id, model)
                .spec(responseSpec)
                .extract();

        String responseBody = response.asString();
        if (responseBody == null || responseBody.trim().isEmpty()) {
            log.debug("Empty response body received for PUT request to {}/{}", baseEndpoint, id);
            return null;
        }

        try {
            return response.as(responseModel);
        } catch (Exception e) {
            log.error("Failed to deserialize response for PUT request to {}/{}: {}", baseEndpoint, id, e.getMessage());
            return null;
        }
    }

    @Override
    public Object[] getAll(Class<?> clazz) {
        try {
            return getWithValidation(baseEndpoint)
                    .spec(responseSpec)
                    .extract()
                    .jsonPath()
                    .getList("", clazz)
                    .toArray(new Object[0]);
        } catch (Exception e) {
            log.error("Failed to deserialize response for GET ALL request to {}: {}", baseEndpoint, e.getMessage());
            return new Object[0];
        }
    }
}