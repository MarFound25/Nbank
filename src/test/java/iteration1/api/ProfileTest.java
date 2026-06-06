package iteration1.api;

import api.dao.UserDao;
import api.dao.comparison.DaoAndModelAssertions;
import endpoints.Endpoint;
import generators.RandomData;
import models.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.skelethon.requesters.CrudRequesters;
import requests.steps.AdminSteps;
import requests.steps.DataBaseSteps;
import requests.steps.UserSteps;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Profile Management Tests - API & Database Integration")
public class ProfileTest extends BaseTest {

    private static Stream<Arguments> provideValidNameData() {
        return Stream.of(
                Arguments.of("Old Name", "Anna Smith"),
                Arguments.of("John Wick", "John Doe"),
                Arguments.of("Test User", "Sara Connor"),
                Arguments.of("Ivanov Ivan", "Ivan Petrov"),
                Arguments.of("Maria Lopez", "Maria Garcia")
        );
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

    private static Stream<Arguments> provideUnauthorizedData() {
        return Stream.of(
                Arguments.of("", 401),
                Arguments.of("Bearer invalid.token", 401),
                Arguments.of("Basic invalid", 401)
        );
    }


    private String currentUsername;
    private Long currentUserId;
    private LocalDateTime userCreatedAt;

    private String createUserAndGetAuth(String name) {
        currentUsername = RandomData.getUsername();
        String password = RandomData.getPassword();

        CreateUserRequest createRequest = CreateUserRequest.builder()
                .username(currentUsername)
                .password(password)
                .name(name)
                .role(UserRole.USER.toString())
                .build();

        AdminSteps.createUser(createRequest);

        UserDao userDao = DataBaseSteps.getUserByUsername(currentUsername);
        currentUserId = userDao.getId();
        userCreatedAt = userDao.getCreatedAt();
        trackUser(currentUserId);

        return UserSteps.loginAndGetToken(currentUsername, password);
    }

    private ProfileResponse getProfile(String token) {
        return new CrudRequesters(
                RequestSpecs.authWithToken(token),
                ResponseSpecs.requestReturnsOK())
                .getWithValidation(Endpoint.CUSTOMER_PROFILE)
                .extract()
                .as(ProfileResponse.class);
    }

    private void updateUserName(String token, String newName) {
        UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
                .name(newName)
                .build();

        new CrudRequesters(
                RequestSpecs.authWithToken(token),
                ResponseSpecs.requestReturnsOK())
                .put(Endpoint.CUSTOMER_PROFILE, updateRequest);
    }

    private void updateUserNameAndExpectBadRequest(String token, String invalidName) {
        UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
                .name(invalidName)
                .build();

        new CrudRequesters(
                RequestSpecs.authWithToken(token),
                ResponseSpecs.requestReturnsBadRequest())
                .put(Endpoint.CUSTOMER_PROFILE, updateRequest);
    }

    @Nested
    @DisplayName("Positive Scenarios with Database Verification")
    class PositiveTests {

        @ParameterizedTest
        @MethodSource("iteration1.api.ProfileTest#provideValidNameData")
        @DisplayName("TC-PROF-001: User can change name - API and DB consistency")
        void userCanChangeNameWithValidDataTest(String oldName, String newName) {
            String token = createUserAndGetAuth(oldName);

            UserDao userBefore = DataBaseSteps.getUserByUsername(currentUsername);
            assertThat(userBefore.getName()).isEqualTo(oldName);
            LocalDateTime updatedAtBefore = userBefore.getUpdatedAt();

            updateUserName(token, newName);

            ProfileResponse profile = getProfile(token);
            assertThat(profile.getName()).isEqualTo(newName);

            UserDao userAfter = DataBaseSteps.getUserByUsername(currentUsername);

            DaoAndModelAssertions.assertThat(profile, userAfter).matches();

            assertThat(userAfter.getUpdatedAt())
                    .as("updated_at should be changed after profile update")
                    .isAfter(updatedAtBefore);

            assertThat(userAfter.getCreatedAt())
                    .as("created_at should remain unchanged")
                    .isEqualTo(userCreatedAt);
        }

        @Test
        @DisplayName("TC-PROF-002: User can get own profile - data matches database")
        void userCanGetOwnProfileTest() {
            String expectedName = "John Doe";
            String token = createUserAndGetAuth(expectedName);

            ProfileResponse profile = getProfile(token);

            assertThat(profile.getName()).isEqualTo(expectedName);
            assertThat(profile.getUsername()).isEqualTo(currentUsername);
            assertThat(profile.getId()).isEqualTo(currentUserId);

            UserDao userDao = DataBaseSteps.getUserByUsername(currentUsername);
            DaoAndModelAssertions.assertThat(profile, userDao).matches();
        }

        @Test
        @DisplayName("TC-PROF-003: User can change name multiple times - DB tracks updates")
        void userCanChangeNameMultipleTimesTest() {
            String token = createUserAndGetAuth("Initial Name");

            updateUserName(token, "First Change");
            ProfileResponse profile1 = getProfile(token);

            assertThat(profile1.getName()).isEqualTo("First Change");

            updateUserName(token, "Second Change");
            ProfileResponse profile2 = getProfile(token);

            assertThat(profile2.getName()).isEqualTo("Second Change");

            UserDao userDao = DataBaseSteps.getUserByUsername(currentUsername);
            assertThat(userDao.getName()).isEqualTo("Second Change");
            DaoAndModelAssertions.assertThat(profile2, userDao).matches();
        }
    }

    @Nested
    @DisplayName("Negative Scenarios - Validation with DB Verification")
    class NegativeTests {

        @ParameterizedTest
        @MethodSource("iteration1.api.ProfileTest#provideInvalidNameData")
        @DisplayName("TC-PROF-004: Invalid name changes are rejected - DB unchanged")
        void userCannotChangeNameWithInvalidDataTest(String oldName, String invalidName) {
            String token = createUserAndGetAuth(oldName);
            UserDao userBefore = DataBaseSteps.getUserByUsername(currentUsername);
            LocalDateTime updatedAtBefore = userBefore.getUpdatedAt();
            String originalName = userBefore.getName();

            updateUserNameAndExpectBadRequest(token, invalidName);

            ProfileResponse profile = getProfile(token);
            assertThat(profile.getName()).isEqualTo(originalName);

            UserDao userAfter = DataBaseSteps.getUserByUsername(currentUsername);

            assertThat(userAfter.getName())
                    .as("Name should not change after invalid update")
                    .isEqualTo(originalName);

            assertThat(userAfter.getUpdatedAt())
                    .as("updated_at should not change after failed update")
                    .isEqualTo(updatedAtBefore);
        }

        @Test
        @DisplayName("TC-PROF-005: Cannot update profile with null name")
        void userCannotUpdateProfileWithNullNameTest() {
            String token = createUserAndGetAuth("Original Name");
            UserDao userBefore = DataBaseSteps.getUserByUsername(currentUsername);
            String originalName = userBefore.getName();

            UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
                    .name(null)
                    .build();

            new CrudRequesters(
                    RequestSpecs.authWithToken(token),
                    ResponseSpecs.requestReturnsBadRequest())
                    .put(Endpoint.CUSTOMER_PROFILE, updateRequest);

            UserDao userAfter = DataBaseSteps.getUserByUsername(currentUsername);
            assertThat(userAfter.getName()).isEqualTo(originalName);
        }
    }

    @Nested
    @DisplayName("Security & Authorization Tests")
    class SecurityTests {

        @ParameterizedTest
        @MethodSource("iteration1.api.ProfileTest#provideUnauthorizedData")
        @DisplayName("TC-PROF-006: Unauthorized requests are rejected")
        void userCannotAccessProfileWithoutAuthTest(String authHeader, int expectedStatusCode) {
            String token = createUserAndGetAuth("Test User");

            new CrudRequesters(
                    RequestSpecs.customAuth(authHeader),
                    ResponseSpecs.custom(expectedStatusCode))
                    .getWithValidation(Endpoint.CUSTOMER_PROFILE);
        }

        @Test
        @DisplayName("TC-PROF-007: Cannot get profile without authentication")
        void userCannotGetProfileWithoutAuthTest() {
            new CrudRequesters(
                    RequestSpecs.noAuthSpec(),
                    ResponseSpecs.requestReturnsUnauthorized())
                    .getWithValidation(Endpoint.CUSTOMER_PROFILE);
        }

        @Test
        @DisplayName("TC-PROF-008: Cannot get profile with invalid token")
        void userCannotGetProfileWithInvalidTokenTest() {

            new CrudRequesters(
                    RequestSpecs.authWithToken("invalid.token.here"),
                    ResponseSpecs.requestReturnsUnauthorized())
                    .getWithValidation(Endpoint.CUSTOMER_PROFILE);
        }

        @Test
        @DisplayName("TC-PROF-009: Cannot update profile without authentication")
        void userCannotUpdateProfileWithoutAuthTest() {
            UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
                    .name("New Name")
                    .build();

            new CrudRequesters(
                    RequestSpecs.noAuthSpec(),
                    ResponseSpecs.requestReturnsUnauthorized())
                    .putWithValidation(Endpoint.CUSTOMER_PROFILE, updateRequest);
        }

        @Test
        @DisplayName("TC-PROF-010: One user cannot update another user's profile")
        void userCannotUpdateAnotherUsersProfileTest() {
            String user1Token = createUserAndGetAuth("User One");
            Long user1Id = currentUserId;

            String user2Username = RandomData.getUsername();
            String user2Password = RandomData.getPassword();
            CreateUserRequest user2Request = CreateUserRequest.builder()
                    .username(user2Username)
                    .password(user2Password)
                    .name("User Two")
                    .role(UserRole.USER.toString())
                    .build();
            AdminSteps.createUser(user2Request);
            UserDao user2Dao = DataBaseSteps.getUserByUsername(user2Username);
            trackUser(user2Dao.getId());
            String user2Token = UserSteps.loginAndGetToken(user2Username, user2Password);

            UserDao user1Before = DataBaseSteps.getUserById(user1Id);
            String originalName = user1Before.getName();
            LocalDateTime updatedAtBefore = user1Before.getUpdatedAt();

            UserDao user1After = DataBaseSteps.getUserById(user1Id);
            assertThat(user1After.getName()).isEqualTo(originalName);
            assertThat(user1After.getUpdatedAt()).isEqualTo(updatedAtBefore);
        }
    }
}