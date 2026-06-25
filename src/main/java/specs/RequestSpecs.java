package specs;

import configs.Config;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import requests.steps.UserSteps;

public class RequestSpecs {

    public static RequestSpecification adminSpec() {
        System.out.println("=== [DEBUG] adminSpec - Base URL: " + Config.getBaseUrl());

        return new RequestSpecBuilder()
                .setBaseUri(Config.getBaseUrl())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addHeader("Authorization", Config.getAdminBasicAuth())
                .build();
    }

    public static RequestSpecification unauthSpec() {
        return new RequestSpecBuilder()
                .setBaseUri(Config.getBaseUrl())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();
    }

    public static RequestSpecification authWithToken(String token) {
        return new RequestSpecBuilder()
                .setBaseUri(Config.getBaseUrl())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addHeader("Authorization", token)
                .build();
    }

    public static RequestSpecification noAuthSpec() {
        return new RequestSpecBuilder()
                .setBaseUri(Config.getBaseUrl())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .build();
    }

    public static RequestSpecification authWithBasic(String basicAuth) {
        return new RequestSpecBuilder()
                .setBaseUri(Config.getBaseUrl())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addHeader("Authorization", "Basic " + basicAuth)
                .build();
    }

    public static RequestSpecification customAuth(String authHeader) {
        return new RequestSpecBuilder()
                .setBaseUri(Config.getBaseUrl())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addHeader("Authorization", authHeader)
                .build();
    }

    public static String getUserAuthHeader(String username, String password) {
        return username;
    }
    public static RequestSpecification authAsUser(String username, String password) {
        String token = UserSteps.loginAndGetToken(username, password);
        return authWithToken(token);
    }
}