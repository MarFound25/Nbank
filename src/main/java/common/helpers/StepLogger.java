package common.helpers;

import io.qameta.allure.Allure;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StepLogger {

    @FunctionalInterface
    public interface ThrowableRunnable<T> {
        T run() throws Throwable;
    }

    @FunctionalInterface
    public interface ThrowableVoidRunnable {
        void run() throws Throwable;
    }

    /**
     * Выполняет шаг с логированием в Allure
     * @param title название шага
     * @param runnable код шага
     * @return результат выполнения
     */
    public static <T> T log(String title, ThrowableRunnable<T> runnable) {
        // Используем Allure.step с лямбдой, которая возвращает результат
        return Allure.step(title, () -> {
            long startTime = System.currentTimeMillis();
            try {
                log.debug("Starting step: {}", title);
                T result = runnable.run();
                long duration = System.currentTimeMillis() - startTime;
                log.debug("Completed step: {} in {} ms", title, duration);

                if (duration > 2000) {
                    Allure.addAttachment("Slow operation", title + " took " + duration + " ms");
                }
                return result;
            } catch (Throwable e) {
                log.error("Step failed: {}", title, e);
                Allure.addAttachment("Step failed: " + title, e.getClass().getSimpleName() + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Выполняет void-шаг с логированием в Allure
     */
    public static void logVoid(String title, ThrowableVoidRunnable runnable) {
        Allure.step(title, () -> {
            long startTime = System.currentTimeMillis();
            try {
                log.debug("Starting step: {}", title);
                runnable.run();
                long duration = System.currentTimeMillis() - startTime;
                log.debug("Completed step: {} in {} ms", title, duration);

                if (duration > 2000) {
                    Allure.addAttachment("Slow operation", title + " took " + duration + " ms");
                }
            } catch (Throwable e) {
                log.error("Step failed: {}", title, e);
                Allure.addAttachment("Step failed: " + title, e.getClass().getSimpleName() + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Выполняет шаг и добавляет вложение
     */
    public static <T> T logWithAttachment(String title, String attachmentName, String content, ThrowableRunnable<T> runnable) {
        return Allure.step(title, () -> {
            Allure.addAttachment(attachmentName, "text/plain", content, ".txt");
            return runnable.run();
        });
    }
}