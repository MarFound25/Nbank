package requests.steps;

import endpoints.Endpoint;
import generators.RandomData;
import models.*;
import org.hamcrest.Matchers;
import requests.skelethon.requesters.CrudRequesters;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.List;

import static io.restassured.RestAssured.given;

public class AdminSteps {

    public static CreateUserResponse createUser(CreateUserRequest request) {
        return new CrudRequesters(RequestSpecs.adminSpec(), ResponseSpecs.entityWasCreated())
                .create(request)
                .extract()
                .as(CreateUserResponse.class);
    }

    public static String createUserAndGetToken(CreateUserRequest request) {
        createUser(request);

        LoginUserRequest loginRequest = LoginUserRequest.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .build();

        return given()
                .spec(RequestSpecs.unauthSpec())
                .body(loginRequest)
                .when()
                .post(Endpoint.AUTH_LOGIN)
                .then()
                .extract()
                .jsonPath()
                .getString("token");
    }



    public static void getAllUsersAndExpectForbidden(String token) {
        new CrudRequesters(RequestSpecs.authWithToken(token), ResponseSpecs.requestReturnsForbidden())
                .readAll();
    }

    public static void deleteUser(long userId) {
        new CrudRequesters(RequestSpecs.adminSpec(), ResponseSpecs.requestReturnsOK())
                .delete(userId);
    }

    public static void deleteUserAndExpectNotFound(long userId) {
        new CrudRequesters(RequestSpecs.adminSpec(), ResponseSpecs.requestReturnsNotFound())
                .delete(userId);
    }

    public static void deleteUserAndExpectForbidden(String token, long userId) {
        new CrudRequesters(RequestSpecs.authWithToken(token), ResponseSpecs.requestReturnsForbidden())
                .delete(userId);
    }

    public static void createUserAndExpectBadRequest(CreateUserRequest request) {
        new CrudRequesters(RequestSpecs.adminSpec(), ResponseSpecs.requestReturnsBadRequest())
                .create(request);
    }

    public static void createUserAndExpectBadRequest(CreateUserRequest request, String expectedError) {
        new CrudRequesters(
                RequestSpecs.adminSpec(),
                ResponseSpecs.requestReturnsBadRequest(expectedError))
                .create(request);
    }

    public static void createUserAndExpectBadRequest(CreateUserRequest request, String errorKey, String errorValue) {
        new CrudRequesters(
                RequestSpecs.adminSpec(),
                ResponseSpecs.requestReturnsBadRequest(errorKey, errorValue))
                .create(request);
    }

    public static String getAdminToken() {
        LoginUserRequest loginRequest = LoginUserRequest.builder()
                .username("admin")
                .password("admin")
                .build();

        return given()
                .spec(RequestSpecs.unauthSpec())
                .body(loginRequest)
                .when()
                .post(Endpoint.AUTH_LOGIN)
                .then()
                .spec(ResponseSpecs.requestReturnsOK())
                .extract()
                .header("Authorization");
    }

    public static void createUserAndExpectUsernameError(CreateUserRequest request, String expectedError) {
        new CrudRequesters(RequestSpecs.adminSpec(), ResponseSpecs.requestReturnsBadRequest())
                .create(request)
                .body("username", Matchers.hasItem(expectedError));
    }

    public static void createUserAndExpectPasswordError(CreateUserRequest request, String expectedError) {
        new CrudRequesters(RequestSpecs.adminSpec(), ResponseSpecs.requestReturnsBadRequest())
                .create(request)
                .body("password", Matchers.hasItem(expectedError));
    }

    public static List<CreateUserResponse> getAllUsers() {
        return given()
                .spec(RequestSpecs.adminSpec())
                .when()
                .get(Endpoint.ADMIN_USERS)
                .then()
                .spec(ResponseSpecs.requestReturnsOK())
                .extract()
                .jsonPath()
                .getList("", CreateUserResponse.class);
    }

    public static CreateUserResponse createUser() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .name("Test User")
                .role(UserRole.USER.toString())
                .build();
        return createUser(request);
    }

    public static UserWithPassword createUserWithPassword() {
        String rawPassword = RandomData.getPassword();
        String username = RandomData.getUsername();

        CreateUserRequest request = CreateUserRequest.builder()
                .username(username)
                .password(rawPassword)
                .name("Test User")
                .role(UserRole.USER.toString())
                .build();

        CreateUserResponse response = createUser(request);
        return new UserWithPassword(response, rawPassword);
    }
}