package common.extensions;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import common.annotations.FraudCheckMock;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class FraudCheckWireMockExtension implements BeforeEachCallback, AfterEachCallback {

    private WireMockServer wireMockServer;
    private static final int DEFAULT_PORT = 8082;

    @Override
    public void beforeEach(ExtensionContext context) {
        FraudCheckMock mockConfig = findFraudCheckMock(context);

        if (mockConfig != null) {
            int port = mockConfig.port() > 0 ? mockConfig.port() : DEFAULT_PORT;
            setupWireMock(port, mockConfig);
        }
    }

    private FraudCheckMock findFraudCheckMock(ExtensionContext context) {
        return context.getTestMethod()
                .map(method -> method.getAnnotation(FraudCheckMock.class))
                .orElseGet(() -> context.getTestClass()
                        .map(clazz -> clazz.getAnnotation(FraudCheckMock.class))
                        .orElse(null));
    }

    private void setupWireMock(int port, FraudCheckMock config) {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(port));
        wireMockServer.start();
        WireMock.configureFor("localhost", port);

        int httpStatus = "ERROR".equals(config.status()) ? 500 : 200;

        if (httpStatus == 500) {
            stubFor(post(urlPathMatching(config.endpoint()))
                    .willReturn(aResponse()
                            .withStatus(500)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"error\": \"Fraud service unavailable\"}")));
        } else if ("TIMEOUT".equals(config.status())) {
            stubFor(post(urlPathMatching(config.endpoint()))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withFixedDelay(10000)
                            .withBody("{}")));
        } else {
            String responseBody = String.format("{\n" +
                            "  \"status\": \"%s\",\n" +
                            "  \"decision\": \"%s\",\n" +
                            "  \"riskScore\": %.1f,\n" +
                            "  \"reason\": \"%s\",\n" +
                            "  \"requiresManualReview\": %s,\n" +
                            "  \"additionalVerificationRequired\": %s\n" +
                            "}",
                    config.status(),
                    config.decision(),
                    config.riskScore(),
                    config.reason(),
                    config.requiresManualReview(),
                    config.additionalVerificationRequired());

            stubFor(post(urlPathMatching(config.endpoint()))
                    .willReturn(aResponse()
                            .withStatus(httpStatus)
                            .withHeader("Content-Type", "application/json")
                            .withBody(responseBody)));
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    public String getBaseUrl() {
        if (wireMockServer != null) {
            return "http://localhost:" + wireMockServer.port();
        }
        return null;
    }
}