package configs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {

    private static final Config INSTANCE = new Config();
    private final Properties properties = new Properties();

    static {
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.err.println("config.properties not found in resources — using default values");
            } else {
                INSTANCE.properties.load(input);
                System.out.println("✅ config.properties loaded successfully");
                INSTANCE.properties.list(System.out);
            }
        } catch (IOException e) {
            System.err.println("Failed to load config.properties — using default values");
        }
    }

    // Главный метод для получения свойств с приоритетом:
    // 1. Системные свойства (-D)
    // 2. Переменные окружения
    // 3. config.properties
    // 4. Дефолтные значения
    public static String getPropertyWithPriority(String key) {
        // 1. Пробуем системные свойства
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isEmpty()) {
            System.out.println("🔧 Using system property: " + key + "=" + systemValue);
            return systemValue;
        }

        // 2. Пробуем переменные окружения
        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isEmpty()) {
            System.out.println("🌍 Using env variable: " + envKey + "=" + envValue);
            return envValue;
        }

        // 3. Пробуем config.properties
        String propValue = INSTANCE.properties.getProperty(key);
        if (propValue != null && !propValue.isEmpty()) {
            System.out.println("📁 Using config.properties: " + key + "=" + propValue);
            return propValue;
        }

        // 4. Дефолтные значения (из статических полей или жестко заданные)
        return getDefaultValue(key);
    }

    private static String getDefaultValue(String key) {
        switch (key) {
            case "db.url":
                return "jdbc:postgresql://localhost:5433/nbank";
            case "db.username":
                return "postgres";
            case "db.password":
                return "postgres";
            case "admin.username":
                return "admin";
            case "admin.password":
                return "admin";
            case "api.base.url":
                return "http://localhost:4112";
            default:
                return null;
        }
    }

    // Все методы должны использовать getPropertyWithPriority
    public static String getJdbcUrl() {
        return getPropertyWithPriority("db.url");
    }

    public static String getDbUsername() {
        return getPropertyWithPriority("db.username");
    }

    public static String getDbPassword() {
        return getPropertyWithPriority("db.password");
    }

    public static String getAdminUsername() {
        return getPropertyWithPriority("admin.username");
    }

    public static String getAdminPassword() {
        return getPropertyWithPriority("admin.password");
    }

    public static String getBaseUrl() {
        return getPropertyWithPriority("api.base.url");
    }

    // Старые методы для обратной совместимости
    public static String getJdbcUrlFlexible() {
        return getJdbcUrl();
    }

    public static String getDbUsernameFlexible() {
        return getDbUsername();
    }

    public static String getDbPasswordFlexible() {
        return getDbPassword();
    }

    public static String getAdminUsernameFlexible() {
        return getAdminUsername();
    }

    public static String getAdminPasswordFlexible() {
        return getAdminPassword();
    }

    public static String getBaseUrlFlexible() {
        return getBaseUrl();
    }

    // Для обратной совместимости с существующим кодом
    public static String getProperty(String key) {
        return getPropertyWithPriority(key);
    }

    public static String getProperty(String key, String defaultValue) {
        String value = getPropertyWithPriority(key);
        return value != null ? value : defaultValue;
    }

    // Другие существующие методы
    public static String getAdminBasicAuth() {
        String username = getAdminUsername();
        String password = getAdminPassword();
        String credentials = username + ":" + password;
        return "Basic " + java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
    }
}
