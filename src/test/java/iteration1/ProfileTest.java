package iteration1;

import generators.RandomData;
import models.*;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.*;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

public class ProfileTest extends BaseTest {


    private String createUserAndGetAuth(String name) {
        CreateUserRequest createRequest = CreateUserRequest.builder()
                .username(RandomData.getUsername())
                .password(RandomData.getPassword())
                .name(name)
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(createRequest);

        LoginUserRequest loginRequest = LoginUserRequest.builder()
                .username(createRequest.getUsername())
                .password(createRequest.getPassword())
                .build();

        return new LoginUserRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsOK())
                .getToken(loginRequest);
    }

    private String getUserName(String authToken) {
        ProfileResponse profile = new GetCustomerProfileRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getProfile();
        return profile.getName();
    }


    private static Stream<Arguments> provideValidNameData() {
        return Stream.of(
                Arguments.of("Old Name", "Anna Smith"),
                Arguments.of("John Wick", "John Doe"),
                Arguments.of("Test User", "Sara Connor"),
                Arguments.of("Ivanov Ivan", "Ivan Petrov"),
                Arguments.of("Maria Lopez", "Maria Garcia")
        );
    }

    @ParameterizedTest
    @MethodSource("provideValidNameData")
    public void userCanChangeNameWithValidDataTest(String oldName, String newName) {
        String authToken = createUserAndGetAuth(oldName);

        String currentName = getUserName(authToken);
        softly.assertThat(currentName).isEqualTo(oldName);

        UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
                .name(newName)
                .build();

        UpdateProfileResponse response = new UpdateCustomerProfileRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .updateProfile(updateRequest);

        softly.assertThat(response.getCustomer().getName()).isEqualTo(newName);

        String updatedName = getUserName(authToken);
        softly.assertThat(updatedName).isEqualTo(newName);
    }


    private static Stream<Arguments> provideInvalidNameData() {
        return Stream.of(
                Arguments.of("Valid Name", "John"),
                Arguments.of("Valid Name", "John Peter Smith"),
                Arguments.of("Valid Name", "John 123"),
                Arguments.of("Valid Name", "John@ Doe"),
                Arguments.of("Valid Name", ""),
                Arguments.of("Valid Name", " "),
                Arguments.of("Valid Name", "  ")
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidNameData")
    public void userCannotChangeNameWithInvalidDataTest(String oldName, String invalidName) {
        String authToken = createUserAndGetAuth(oldName);

        String currentName = getUserName(authToken);
        softly.assertThat(currentName).isEqualTo(oldName);

        UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
                .name(invalidName)
                .build();

        new UpdateCustomerProfileRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsBadRequest())
                .put(updateRequest);

        String unchangedName = getUserName(authToken);
        softly.assertThat(unchangedName).isEqualTo(oldName);
    }


    private static Stream<Arguments> provideUnauthorizedData() {
        return Stream.of(
                Arguments.of("", HttpStatus.SC_UNAUTHORIZED),
                Arguments.of("Bearer invalid.token", HttpStatus.SC_UNAUTHORIZED),
                Arguments.of("Basic invalid", HttpStatus.SC_UNAUTHORIZED)
        );
    }

    @ParameterizedTest
    @MethodSource("provideUnauthorizedData")
    public void userCannotChangeNameWithoutAuthTest(String authHeader, int expectedStatusCode) {
        UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
                .name("New Name")
                .build();

        new UpdateCustomerProfileRequester(
                RequestSpecs.customAuth(authHeader),
                ResponseSpecs.custom(expectedStatusCode))
                .put(updateRequest);
    }


    @Test
    public void userCanGetOwnProfileTest() {
        String expectedName = "John Doe";
        String authToken = createUserAndGetAuth(expectedName);

        new GetCustomerProfileRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .get()
                .body("name", Matchers.equalTo(expectedName))
                .body("username", Matchers.notNullValue());
    }

    @Test
    public void userCannotGetProfileWithoutAuthTest() {
        new GetCustomerProfileRequester(
                RequestSpecs.noAuthSpec(),
                ResponseSpecs.requestReturnsUnauthorized())
                .get();
    }

    @Test
    public void userCannotGetProfileWithInvalidTokenTest() {
        new GetCustomerProfileRequester(
                RequestSpecs.authWithBearerToken("invalid.token.here"),
                ResponseSpecs.requestReturnsUnauthorized())
                .get();
    }
}