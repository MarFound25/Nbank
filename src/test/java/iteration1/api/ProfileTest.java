package iteration1.api;

import generators.RandomData;
import models.*;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.steps.AdminSteps;
import requests.steps.UserSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

public class ProfileTest extends BaseTest {

    private String createUserAndGetAuth(String name) {
        String username = RandomData.getUsername();
        String password = RandomData.getPassword();

        CreateUserRequest createRequest = CreateUserRequest.builder()
                .username(username)
                .password(password)
                .name(name)
                .role(UserRole.USER.toString())
                .build();

        AdminSteps.createUser(createRequest);
        return UserSteps.loginAndGetToken(username, password);
    }

    private ProfileResponse getProfile(String token) {
        return new requests.skelethon.requesters.CrudRequesters(
                RequestSpecs.authWithToken(token),
                ResponseSpecs.requestReturnsOK())
                .getWithValidation(endpoints.Endpoint.CUSTOMER_PROFILE)
                .extract()
                .as(ProfileResponse.class);
    }

    private void updateUserName(String token, String newName) {
        UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
                .name(newName)
                .build();

        new requests.skelethon.requesters.CrudRequesters(
                RequestSpecs.authWithToken(token),
                ResponseSpecs.requestReturnsOK())
                .putWithValidation(endpoints.Endpoint.CUSTOMER_PROFILE, updateRequest);
    }

    private void updateUserNameAndExpectBadRequest(String token, String invalidName) {
        UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
                .name(invalidName)
                .build();

        new requests.skelethon.requesters.CrudRequesters(
                RequestSpecs.authWithToken(token),
                ResponseSpecs.requestReturnsBadRequest())
                .putWithValidation(endpoints.Endpoint.CUSTOMER_PROFILE, updateRequest);
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
        String token = createUserAndGetAuth(oldName);

        ProfileResponse profileBefore = getProfile(token);
        softly.assertThat(profileBefore.getName()).isEqualTo(oldName);

        updateUserName(token, newName);

        ProfileResponse profileAfter = getProfile(token);
        softly.assertThat(profileAfter.getName()).isEqualTo(newName);
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
        String token = createUserAndGetAuth(oldName);

        ProfileResponse profileBefore = getProfile(token);
        softly.assertThat(profileBefore.getName()).isEqualTo(oldName);

        updateUserNameAndExpectBadRequest(token, invalidName);

        ProfileResponse profileAfter = getProfile(token);
        softly.assertThat(profileAfter.getName()).isEqualTo(oldName);
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

        new requests.skelethon.requesters.CrudRequesters(
                RequestSpecs.customAuth(authHeader),
                ResponseSpecs.custom(expectedStatusCode))
                .putWithValidation(endpoints.Endpoint.CUSTOMER_PROFILE, updateRequest);
    }

    @Test
    public void userCanGetOwnProfileTest() {
        String expectedName = "John Doe";
        String token = createUserAndGetAuth(expectedName);

        ProfileResponse profile = getProfile(token);

        softly.assertThat(profile.getName()).isEqualTo(expectedName);
        softly.assertThat(profile.getUsername()).isNotNull();
        softly.assertThat(profile.getId()).isNotNull();
    }

    @Test
    public void userCannotGetProfileWithoutAuthTest() {
        new requests.skelethon.requesters.CrudRequesters(
                RequestSpecs.noAuthSpec(),
                ResponseSpecs.requestReturnsUnauthorized())
                .getWithValidation(endpoints.Endpoint.CUSTOMER_PROFILE);
    }

    @Test
    public void userCannotGetProfileWithInvalidTokenTest() {
        new requests.skelethon.requesters.CrudRequesters(
                RequestSpecs.authWithToken("invalid.token.here"),
                ResponseSpecs.requestReturnsUnauthorized())
                .getWithValidation(endpoints.Endpoint.CUSTOMER_PROFILE);
    }
}