package generators;

import java.util.Random;

public class RandomData {
    private static final Random random = new Random();

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#";
    private static final String ALL = UPPER + LOWER + DIGITS + SPECIAL;

    public static String getUsername() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(LOWER.charAt(random.nextInt(LOWER.length())));
        }
        return sb.toString();
    }

    public static String getPassword() {
        StringBuilder password = new StringBuilder();

        password.append(UPPER.charAt(random.nextInt(UPPER.length())));
        password.append(LOWER.charAt(random.nextInt(LOWER.length())));
        password.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        password.append(SPECIAL.charAt(random.nextInt(SPECIAL.length())));

        int extra = 6;
        for (int i = 0; i < extra; i++) {
            password.append(ALL.charAt(random.nextInt(ALL.length())));
        }

        return shuffleString(password.toString());
    }

    private static String shuffleString(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }

    public static String getUsernameWithDot() {
        return getRandomString(5) + "." + getRandomString(5);
    }

    public static String getUsernameWithDash() {
        return getRandomString(5) + "-" + getRandomString(5);
    }

    public static String getUsernameWithUnderscore() {
        return getRandomString(5) + "_" + getRandomString(5);
    }

    public static String getUsernameWithDigits() {
        return getRandomString(5) + getRandomDigits(3);
    }

    public static String getUsernameMinLength() {
        return getRandomString(3);
    }

    public static String getUsernameMaxLength() {
        return getRandomString(15);
    }

    private static String getRandomString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(LOWER.charAt(random.nextInt(LOWER.length())));
        }
        return sb.toString();
    }

    private static String getRandomDigits(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        }
        return sb.toString();
    }
}
