package requests.skelethon;

import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

public class HttpRequest {
    protected RequestSpecification requestSpec;

    public HttpRequest(RequestSpecification requestSpec) {
        this.requestSpec = requestSpec;
    }

    public Response get(String url, Object... pathParams) {
        return given()
                .spec(requestSpec)
                .when()
                .get(url, pathParams);
    }

    public Response post(String url, Object body) {
        return given()
                .spec(requestSpec)
                .body(body)
                .when()
                .post(url);
    }

    public Response put(String url, Object body) {
        return given()
                .spec(requestSpec)
                .body(body)
                .when()
                .put(url);
    }

    public Response delete(String url, Object... pathParams) {
        return given()
                .spec(requestSpec)
                .when()
                .delete(url, pathParams);
    }

    public ValidatableResponse getWithValidation(String url, Object... pathParams) {
        return given()
                .spec(requestSpec)
                .when()
                .get(url, pathParams)
                .then();
    }

    public ValidatableResponse postWithValidation(String url, Object body) {
//        return given()
//                .spec(requestSpec)
//                .body(body)
//                .when()
//                .post(url)
//                .then();
        var request = given().spec(requestSpec);
        if (body != null) {
            request.body(body);
        }
        return request
                .when()
                .post(url)
                .then();
    }

    public ValidatableResponse putWithValidation(String url, Object body, Object... pathParams) {
        return given()
                .spec(requestSpec)
                .body(body)
                .when()
                .put(url, pathParams)
                .then();
    }

    public ValidatableResponse deleteWithValidation(String url, Object... pathParams) {
        return given()
                .spec(requestSpec)
                .when()
                .delete(url, pathParams)
                .then();
    }
}