package generators;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RandomData {
    private RandomData() {}

    public static String getUsername() {
        return RandomStringUtils.randomAlphabetic(10);
    }

    public static String getPassword() {
        String upper = RandomStringUtils.randomAlphabetic(2).toUpperCase();
        String lower = RandomStringUtils.randomAlphabetic(2).toLowerCase();
        String digits = RandomStringUtils.randomNumeric(2);
        String special = RandomStringUtils.random(2, "!@#$%^&");

        String password = upper + lower + digits + special;
        return shuffleString(password);
    }

    private static String shuffleString(String input) {
        List<Character> chars = input.chars()
                .mapToObj(c -> (char) c)
                .collect(Collectors.toList());
        Collections.shuffle(chars);
        return chars.stream()
                .map(String::valueOf)
                .collect(Collectors.joining());
    }

    public static String getUsernameWithDot() {
        return RandomStringUtils.randomAlphabetic(5) + "." + RandomStringUtils.randomAlphabetic(5);
    }

    public static String getUsernameWithDash() {
        return RandomStringUtils.randomAlphabetic(5) + "-" + RandomStringUtils.randomAlphabetic(5);
    }

    public static String getUsernameWithUnderscore() {
        return RandomStringUtils.randomAlphabetic(5) + "_" + RandomStringUtils.randomAlphabetic(5);
    }

    public static String getUsernameWithDigits() {
        return RandomStringUtils.randomAlphabetic(5) + RandomStringUtils.randomNumeric(3);
    }

    public static String getUsernameMinLength() {
        return RandomStringUtils.randomAlphabetic(3);
    }

    public static String getUsernameMaxLength() {
        return RandomStringUtils.randomAlphabetic(15);
    }
}