package requests.steps;

import configs.Config;
import requests.skelethon.Endpoint;
import generators.RandomData;
import models.CreateUserRequest;
import models.CreateUserResponse;
import models.LoginUserRequest;
import models.UserRole;
import models.UserWithPassword;
import org.hamcrest.Matchers;
import requests.skelethon.requesters.CrudRequesters;
import specs.RequestSpecs;
import specs.ResponseSpecs;
import java.util.List;
import static io.restassured.RestAssured.given;

public class AdminSteps {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";

    public static CreateUserResponse createUser(CreateUserRequest request) {
        System.out.println("=== [DEBUG] Full URL: " + Config.getBaseUrl() + Endpoint.ADMIN_USERS);
        System.out.println("=== [DEBUG] Request body: " + request);
        System.out.println("=== [DEBUG] Auth header: " + Config.getAdminBasicAuth());

        CrudRequesters crudRequester = new CrudRequesters(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USERS,
                ResponseSpecs.entityWasCreated()
        );

        CreateUserResponse response = crudRequester.create(request)
                .extract()
                .as(CreateUserResponse.class);

        System.out.println("=== [DEBUG] Response: " + response);
        return response;
    }

    public static CreateUserRequest createUserRequest() {
        return CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .name("Test User")
                .role(UserRole.USER.toString())
                .build();
    }

    public static CreateUserResponse createUser() {
        CreateUserRequest request = createUserRequest();
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
        new CrudRequesters(
                RequestSpecs.authWithToken(token),
                Endpoint.ADMIN_USERS,
                ResponseSpecs.requestReturnsForbidden()
        ).readAll();
    }

    // ✅ ИСПРАВЛЕННЫЙ МЕТОД - используем deleteWithPathParam
    public static void deleteUser(long userId) {
        new CrudRequesters(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USER_BY_ID,
                ResponseSpecs.requestReturnsOK()
        ).deleteWithPathParam(userId);  // ← Изменено с delete() на deleteWithPathParam()
    }

    public static void deleteUserAndExpectNotFound(long userId) {
        new CrudRequesters(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USER_BY_ID,
                ResponseSpecs.requestReturnsNotFound()
        ).deleteWithPathParam(userId);  // ← Тоже исправлено
    }

    public static void deleteUserAndExpectForbidden(String token, long userId) {
        new CrudRequesters(
                RequestSpecs.authWithToken(token),
                Endpoint.ADMIN_USER_BY_ID,
                ResponseSpecs.requestReturnsForbidden()
        ).deleteWithPathParam(userId);  // ← Тоже исправлено
    }

    public static void createUserAndExpectBadRequest(CreateUserRequest request) {
        new CrudRequesters(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USERS,
                ResponseSpecs.requestReturnsBadRequest()
        ).create(request);
    }

    public static void createUserAndExpectBadRequest(CreateUserRequest request, String expectedError) {
        new CrudRequesters(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USERS,
                ResponseSpecs.requestReturnsBadRequest(expectedError)
        ).create(request);
    }

    public static void createUserAndExpectBadRequest(CreateUserRequest request, String errorKey, String errorValue) {
        new CrudRequesters(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USERS,
                ResponseSpecs.requestReturnsBadRequest(errorKey, errorValue)
        ).create(request);
    }

    public static String getAdminToken() {
        LoginUserRequest loginRequest = LoginUserRequest.builder()
                .username(ADMIN_USERNAME)
                .password(ADMIN_PASSWORD)
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
        new CrudRequesters(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USERS,
                ResponseSpecs.requestReturnsBadRequest()
        ).create(request)
                .body("username", Matchers.hasItem(expectedError));
    }

    public static void createUserAndExpectPasswordError(CreateUserRequest request, String expectedError) {
        new CrudRequesters(
                RequestSpecs.adminSpec(),
                Endpoint.ADMIN_USERS,
                ResponseSpecs.requestReturnsBadRequest()
        ).create(request)
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
}