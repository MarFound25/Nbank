package specs;

import com.github.viclovsky.swagger.coverage.FileSystemOutputWriter;
import com.github.viclovsky.swagger.coverage.SwaggerCoverageV3RestAssured;
import configs.Config;
import io.qameta.allure.Allure;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.RequestSpecification;
import requests.steps.UserSteps;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

import static com.github.viclovsky.swagger.coverage.SwaggerCoverageConstants.OUTPUT_DIRECTORY;

public class RequestSpecs {

    private static final Filter ALLURE_SAFE_FILTER = new Filter() {
        @Override
        public Response filter(FilterableRequestSpecification requestSpec,
                               FilterableResponseSpecification responseSpec,
                               FilterContext ctx) {
            Response response = ctx.next(requestSpec, responseSpec);
            try {
                String requestInfo = requestSpec.getMethod() + " " + requestSpec.getURI();
                Allure.addAttachment("HTTP Request", "text/plain",
                        new ByteArrayInputStream(requestInfo.getBytes(StandardCharsets.UTF_8)), ".txt");

                Object body = requestSpec.getBody();
                if (body != null) {
                    String bodyText = truncate(String.valueOf(body), 4000);
                    Allure.addAttachment("Request body", "application/json",
                            new ByteArrayInputStream(bodyText.getBytes(StandardCharsets.UTF_8)), ".json");
                }

                String responseText = truncate(response.asString(), 4000);
                Allure.addAttachment("HTTP Response " + response.statusCode(), "application/json",
                        new ByteArrayInputStream(responseText.getBytes(StandardCharsets.UTF_8)), ".json");
            } catch (Exception ignored) {
                
            }
            return response;
        }

        private String truncate(String value, int max) {
            if (value == null) {
                return "";
            }
            return value.length() <= max ? value : value.substring(0, max) + "...(truncated)";
        }
    };

    /**
     * OpenAPI 3 coverage filter. Writes call snapshots under target/swagger-coverage-output.
     * Paths in Endpoint.java must match Swagger (/api/v1/...), otherwise coverage stays empty.
     */
    private static final Filter SWAGGER_COVERAGE_FILTER = new SwaggerCoverageV3RestAssured(
            new FileSystemOutputWriter(Paths.get("target/" + OUTPUT_DIRECTORY))
    );

    private static RequestSpecBuilder baseBuilder() {
        return new RequestSpecBuilder()
                .setBaseUri(Config.getBaseUrl())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                // Do not add ResponseLoggingFilter here: with Swagger/Allure filters it
                // re-reads a closed body stream and flakes tests as "broken"
                // (IOException: Attempted read on closed stream). Allure already attaches HTTP I/O.
                .addFilters(List.of(
                        SWAGGER_COVERAGE_FILTER,
                        ALLURE_SAFE_FILTER
                ));
    }

    public static RequestSpecification adminSpec() {
        return baseBuilder()
                .addHeader("Authorization", Config.getAdminBasicAuth())
                .build();
    }

    public static RequestSpecification unauthSpec() {
        return baseBuilder().build();
    }

    public static RequestSpecification authWithToken(String token) {
        return baseBuilder()
                .addHeader("Authorization", token)
                .build();
    }

    public static RequestSpecification noAuthSpec() {
        return baseBuilder().build();
    }

    public static RequestSpecification authWithBasic(String basicAuth) {
        return baseBuilder()
                .addHeader("Authorization", "Basic " + basicAuth)
                .build();
    }

    public static RequestSpecification customAuth(String authHeader) {
        return baseBuilder()
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
