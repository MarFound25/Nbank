package common.helpers;

import com.codeborne.selenide.WebDriverRunner;
import io.qameta.allure.Allure;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.ByteArrayInputStream;

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

    public static <T> T log(String title, ThrowableRunnable<T> runnable) {
        return Allure.step(title, () -> {
            long startTime = System.currentTimeMillis();
            try {
                log.debug("Starting step: {}", title);
                T result = runnable.run();
                long duration = System.currentTimeMillis() - startTime;
                log.debug("Completed step: {} in {} ms", title, duration);
                attachUiScreenshot(title);
                if (duration > 2000) {
                    Allure.addAttachment("Slow operation", title + " took " + duration + " ms");
                }
                return result;
            } catch (Throwable e) {
                log.error("Step failed: {}", title, e);
                attachUiScreenshot("FAILED: " + title);
                Allure.addAttachment("Step failed: " + title, e.getClass().getSimpleName() + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    public static void log(String title, ThrowableVoidRunnable runnable) {
        logVoid(title, runnable);
    }

    public static void logVoid(String title, ThrowableVoidRunnable runnable) {
        Allure.step(title, () -> {
            long startTime = System.currentTimeMillis();
            try {
                log.debug("Starting step: {}", title);
                runnable.run();
                long duration = System.currentTimeMillis() - startTime;
                log.debug("Completed step: {} in {} ms", title, duration);
                attachUiScreenshot(title);
                if (duration > 2000) {
                    Allure.addAttachment("Slow operation", title + " took " + duration + " ms");
                }
            } catch (Throwable e) {
                log.error("Step failed: {}", title, e);
                attachUiScreenshot("FAILED: " + title);
                Allure.addAttachment("Step failed: " + title, e.getClass().getSimpleName() + ": " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    public static <T> T logWithAttachment(String title, String attachmentName, String content, ThrowableRunnable<T> runnable) {
        return Allure.step(title, () -> {
            Allure.addAttachment(attachmentName, "text/plain", content, ".txt");
            T result = runnable.run();
            attachUiScreenshot(title);
            return result;
        });
    }

    private static void attachUiScreenshot(String title) {
        try {
            if (!WebDriverRunner.hasWebDriverStarted()) {
                return;
            }
            byte[] screenshot = ((TakesScreenshot) WebDriverRunner.getWebDriver())
                    .getScreenshotAs(OutputType.BYTES);
            Allure.addAttachment(
                    "Screenshot: " + title,
                    "image/png",
                    new ByteArrayInputStream(screenshot),
                    "png"
            );
        } catch (Exception e) {
            log.debug("Could not attach UI screenshot for step '{}': {}", title, e.getMessage());
        }
    }
}
