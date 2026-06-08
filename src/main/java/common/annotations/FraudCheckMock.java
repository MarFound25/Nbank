package common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface FraudCheckMock {

    /**
     * The fraud check status to return
     */
    String status() default "SUCCESS";

    /**
     * The fraud check decision
     */
    String decision() default "APPROVED";

    /**
     * The risk score (0.0 to 1.0)
     */
    double riskScore() default 0.2;

    /**
     * The reason for the fraud check result
     */
    String reason() default "Low risk transaction";

    /**
     * Whether manual review is required
     */
    boolean requiresManualReview() default false;

    /**
     * Whether additional verification is required
     */
    boolean additionalVerificationRequired() default false;

    /**
     * The WireMock port to use
     */
    int port() default 8080;

    /**
     * The endpoint path to mock
     */
    String endpoint() default "/fraud-check";
}