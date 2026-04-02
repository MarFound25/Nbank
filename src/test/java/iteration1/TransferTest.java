package iteration1;

import generators.RandomData;
import models.CreateUserRequest;
import models.LoginUserRequest;
import models.UserRole;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.within;
import requests.*;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.UUID;
import java.util.stream.Stream;

public class TransferTest extends BaseTest {

    private LoginUserRequest toLoginRequest(CreateUserRequest createRequest) {
        return LoginUserRequest.builder()
                .username(createRequest.getUsername())
                .password(createRequest.getPassword())
                .build();
    }

    private String createUserAndGetAuth(String prefix) {

        String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        String username = prefix + uniqueSuffix;

        if (username.length() > 15) {
            username = username.substring(0, 15);
        }

        CreateUserRequest createRequest = CreateUserRequest.builder()
                .username(username)
                .password("Valid1#Pass")
                .role(UserRole.USER.toString())
                .build();

        new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(createRequest);

        LoginUserRequest loginRequest = toLoginRequest(createRequest);

        return new LoginUserRequester(
                RequestSpecs.unauthSpec(),
                ResponseSpecs.requestReturnsOK())
                .getToken(loginRequest);
    }


    @ParameterizedTest
    @MethodSource("provideValidTransferData")
    public void userCanTransferValidAmountsTest(double depositAmount, double transferAmount) {
        String authToken = createUserAndGetAuth("Tr");

        Integer fromAccount = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .jsonPath()
                .getInt("id");

        Integer toAccount = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .jsonPath()
                .getInt("id");

        new DepositRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .post(fromAccount, depositAmount);

        double fromInitialBalance = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getBalance(fromAccount);

        double toInitialBalance = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getBalance(toAccount);

        new TransferRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .post(fromAccount, toAccount, transferAmount);

        double fromFinalBalance = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getBalance(fromAccount);

        double toFinalBalance = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getBalance(toAccount);

        softly.assertThat(fromFinalBalance)
                .isCloseTo(fromInitialBalance - transferAmount, within(0.01));
        softly.assertThat(toFinalBalance)
                .isCloseTo(toInitialBalance + transferAmount, within(0.01));
    }

    private static Stream<Arguments> provideValidTransferData() {
        return Stream.of(
                Arguments.of(1000.0, 300.0),
                Arguments.of(500.0, 500.0),
                Arguments.of(5000.0, 1.0),
                Arguments.of(2000.0, 1999.99)
        );
    }

    @ParameterizedTest
    @MethodSource("provideValidTransferToAnotherUserData")
    public void userCanTransferToAnotherUserTest(double depositAmount, double transferAmount) {
        String user1Auth = createUserAndGetAuth("Sender");
        String user2Auth = createUserAndGetAuth("Receiver");

        Integer fromAccount = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(user1Auth),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .jsonPath()
                .getInt("id");

        Integer toAccount = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(user2Auth),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .jsonPath()
                .getInt("id");

        new DepositRequester(
                RequestSpecs.authWithBearerToken(user1Auth),
                ResponseSpecs.requestReturnsOK())
                .post(fromAccount, depositAmount);

        double fromInitialBalance = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(user1Auth),
                ResponseSpecs.requestReturnsOK())
                .getBalance(fromAccount);

        double toInitialBalance = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(user2Auth),
                ResponseSpecs.requestReturnsOK())
                .getBalance(toAccount);

        new TransferRequester(
                RequestSpecs.authWithBearerToken(user1Auth),
                ResponseSpecs.requestReturnsOK())
                .post(fromAccount, toAccount, transferAmount);

        double fromFinalBalance = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(user1Auth),
                ResponseSpecs.requestReturnsOK())
                .getBalance(fromAccount);

        double toFinalBalance = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(user2Auth),
                ResponseSpecs.requestReturnsOK())
                .getBalance(toAccount);

        softly.assertThat(fromFinalBalance).isEqualTo(fromInitialBalance - transferAmount);
        softly.assertThat(toFinalBalance).isEqualTo(toInitialBalance + transferAmount);
    }

    private static Stream<Arguments> provideValidTransferToAnotherUserData() {
        return Stream.of(
                Arguments.of(1000.0, 400.0),
                Arguments.of(2000.0, 1000.0),
                Arguments.of(500.0, 250.0)
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidTransferData")
    public void userCannotMakeInvalidTransferTest(double amount, String accountType, int expectedStatusCode) {
        String authToken = createUserAndGetAuth("TransferNeg");

        Integer fromAccount = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .jsonPath()
                .getInt("id");

        new DepositRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .post(fromAccount, 1000.0);

        double fromInitialBalance = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getBalance(fromAccount);

        Integer targetAccountId;
        double toInitialBalance = 0.0;
        String targetAuthToken = null;

        switch (accountType) {
            case "valid":
                targetAccountId = new CreateAccountRequester(
                        RequestSpecs.authWithBearerToken(authToken),
                        ResponseSpecs.entityWasCreated())
                        .post(null)
                        .extract()
                        .jsonPath()
                        .getInt("id");
                toInitialBalance = new GetCustomerAccountsRequester(
                        RequestSpecs.authWithBearerToken(authToken),
                        ResponseSpecs.requestReturnsOK())
                        .getBalance(targetAccountId);
                targetAuthToken = authToken;
                break;
            case "non-existent-acc":
                targetAccountId = 999999;
                break;
            default:
                targetAccountId = fromAccount;
        }

        new TransferRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.custom(expectedStatusCode))
                .post(fromAccount, targetAccountId, amount);

        double fromFinalBalance = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getBalance(fromAccount);

        softly.assertThat(fromFinalBalance).isEqualTo(fromInitialBalance);

        if ("valid".equals(accountType)) {
            double toFinalBalance = new GetCustomerAccountsRequester(
                    RequestSpecs.authWithBearerToken(targetAuthToken),
                    ResponseSpecs.requestReturnsOK())
                    .getBalance(targetAccountId);
            softly.assertThat(toFinalBalance).isEqualTo(toInitialBalance);
        }
    }

    private static Stream<Arguments> provideInvalidTransferData() {
        return Stream.of(
                Arguments.of(-50.0, "valid", HttpStatus.SC_BAD_REQUEST),
                Arguments.of(0.0, "valid", HttpStatus.SC_BAD_REQUEST),
                Arguments.of(10001.0, "valid", HttpStatus.SC_BAD_REQUEST),
                Arguments.of(999999.0, "valid", HttpStatus.SC_BAD_REQUEST),
                Arguments.of(100.0, "non-existent-acc", HttpStatus.SC_BAD_REQUEST)
        );
    }


    @Test
    public void userCannotTransferFromAnotherUsersAccountTest() {
        String user1Auth = createUserAndGetAuth("User1");
        String user2Auth = createUserAndGetAuth("User2");

        Integer user1Account = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(user1Auth),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .jsonPath()
                .getInt("id");

        Integer user2Account = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(user2Auth),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .jsonPath()
                .getInt("id");

        new DepositRequester(
                RequestSpecs.authWithBearerToken(user1Auth),
                ResponseSpecs.requestReturnsOK())
                .post(user1Account, 1000.0);

        double user1InitialBalance = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(user1Auth),
                ResponseSpecs.requestReturnsOK())
                .getBalance(user1Account);

        double user2InitialBalance = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(user2Auth),
                ResponseSpecs.requestReturnsOK())
                .getBalance(user2Account);

        new TransferRequester(
                RequestSpecs.authWithBearerToken(user2Auth),
                ResponseSpecs.requestReturnsForbidden())
                .post(user1Account, user2Account, 100.00);

        double user1FinalBalance = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(user1Auth),
                ResponseSpecs.requestReturnsOK())
                .getBalance(user1Account);

        double user2FinalBalance = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(user2Auth),
                ResponseSpecs.requestReturnsOK())
                .getBalance(user2Account);

        softly.assertThat(user1FinalBalance).isEqualTo(user1InitialBalance);
        softly.assertThat(user2FinalBalance).isEqualTo(user2InitialBalance);
    }


    @Test
    public void userCanTransferMaxLimitAmountTest() {
        String authToken = createUserAndGetAuth("Transfer");

        Integer fromAccount = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .jsonPath()
                .getInt("id");

        Integer toAccount = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .jsonPath()
                .getInt("id");

        new DepositRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .post(fromAccount, 5000.00);

        new DepositRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .post(fromAccount, 5000.00);

        new DepositRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .post(fromAccount, 5000.00);

        double fromInitialBalance = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getBalance(fromAccount);

        double toInitialBalance = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getBalance(toAccount);

        new TransferRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .post(fromAccount, toAccount, 10000.00);

        double fromFinalBalance = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getBalance(fromAccount);

        double toFinalBalance = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getBalance(toAccount);

        softly.assertThat(fromFinalBalance).isEqualTo(fromInitialBalance - 10000.0);
        softly.assertThat(toFinalBalance).isEqualTo(toInitialBalance + 10000.0);
    }

    @Test
    public void userCannotTransferAboveLimitTest() {
        String authToken = createUserAndGetAuth("Transfer");

        Integer fromAccount = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .jsonPath()
                .getInt("id");

        Integer toAccount = new CreateAccountRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .jsonPath()
                .getInt("id");

        new DepositRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .post(fromAccount, 5000.00);

        new DepositRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .post(fromAccount, 5000.00);

        new DepositRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .post(fromAccount, 5000.00);

        double fromInitialBalance = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getBalance(fromAccount);

        double toInitialBalance = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getBalance(toAccount);

        new TransferRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsBadRequest())
                .post(fromAccount, toAccount, 10000.01);

        double fromFinalBalance = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getBalance(fromAccount);

        double toFinalBalance = new GetCustomerAccountsRequester(
                RequestSpecs.authWithBearerToken(authToken),
                ResponseSpecs.requestReturnsOK())
                .getBalance(toAccount);

        softly.assertThat(fromFinalBalance).isEqualTo(fromInitialBalance);
        softly.assertThat(toFinalBalance).isEqualTo(toInitialBalance);
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
    public void userCannotTransferWithoutAuthTest(String authHeader, int expectedStatusCode) {
        new TransferRequester(
                RequestSpecs.customAuth(authHeader),
                ResponseSpecs.custom(expectedStatusCode))
                .post(1, 2, 100.00);
    }
}