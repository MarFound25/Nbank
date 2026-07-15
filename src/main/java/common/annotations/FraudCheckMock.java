package common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface FraudCheckMock {

    String status() default "SUCCESS";

    String decision() default "APPROVED";

    double riskScore() default 0.2;

    String reason() default "Low risk transaction";

    boolean requiresManualReview() default false;

    boolean additionalVerificationRequired() default false;

    int port() default 8080;

    String endpoint() default "/fraud-check";
}
