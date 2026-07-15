package requests.steps;

import models.CreateAccountResponse;
import models.DepositRequest;
import models.DepositResponse;
import models.TransferRequest;
import models.TransferResponse;
import requests.skelethon.Endpoint;
import requests.skelethon.requesters.ValidatedCrudRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;
import common.helpers.StepLogger;

import static io.restassured.RestAssured.given;

public class AccountSteps {
    private String username;
    private String password;

    public AccountSteps(String username, String password) {
        this.username = username;
        this.password = password;

    }
    public DepositResponse depositToSenderAccount(Long accountId, double amount) {
        return depositToAccount(accountId, amount);
    }

    public CreateAccountResponse createAccount() {
        return StepLogger.log("User " + username + " creates account", () -> {
            
            String token = UserSteps.loginAndGetToken(username, password);

            return given()
                    .spec(RequestSpecs.authWithToken(token))
                    .when()
                    .post(Endpoint.ACCOUNTS)
                    .then()
                    .spec(ResponseSpecs.entityWasCreated())
                    .extract()
                    .as(CreateAccountResponse.class);
        });
    }

    public DepositResponse depositToAccount(Long accountId, double amount) {
        return StepLogger.log("User " + username + " deposits " + amount + " to account " + accountId, () -> {
            DepositRequest depositRequest = DepositRequest.builder()
                    .id(Math.toIntExact(accountId))
                    .balance(amount)
                    .build();

            return new ValidatedCrudRequester<DepositResponse>(
                    RequestSpecs.authAsUser(username, password),
                    Endpoint.ACCOUNTS_DEPOSIT,
                    ResponseSpecs.requestReturnsOK()).post(depositRequest);
        });
    }

    public TransferResponse transferWithFraudCheck(Long senderAccountId, Long receiverAccountId, double amount) {
        return StepLogger.log("User " + username + " transfers " + amount + " to " + receiverAccountId + " with fraud check", () -> {
            TransferRequest transferRequest = TransferRequest.builder()
                    .senderAccountId(Math.toIntExact(senderAccountId))
                    .receiverAccountId(Math.toIntExact(receiverAccountId))
                    .amount(amount)
                    .build();

            return new ValidatedCrudRequester<TransferResponse>(
                    RequestSpecs.authAsUser(username, password),
                    Endpoint.TRANSFER_WITH_FRAUD_CHECK,
                    ResponseSpecs.requestReturnsOK()).post(transferRequest);
        });
    }
}
