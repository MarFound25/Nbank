package common.utils;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Принимаем на вход общего ретрая:
 * 1) что повторяем
 * 2) условие выхода
 * 3) максимальное количество попыток
 * 4) задержка между каждой попыткой
 */
public class RetryUtils {
    public static <T> T retry(
            Supplier<T> action,
            Predicate<T> condition,
            int maxAttempts,
            long delayMillis) {

        T result = null;
        int attempts = 0;

        while (attempts < maxAttempts) {
            attempts++;
            result = action.get();

            if (condition.test(result)) {
                return result;
            }

            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        throw new RuntimeException("Retry failed after " + maxAttempts + " attempts!");
    }
}