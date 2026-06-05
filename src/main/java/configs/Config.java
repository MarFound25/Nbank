package configs;
import lombok.Getter;

public class Config {

    @Getter
    private static final String jdbcUrl = System.getProperty(
            "DB_URL",
            System.getenv().getOrDefault("JDBC_URL", "jdbc:postgresql://localhost:5433/nbank")
    );

    @Getter
    private static final String dbUsername = System.getProperty(
            "DB_USERNAME",
            System.getenv().getOrDefault("SPRING_DATASOURCE_USERNAME", "postgres")
    );

    @Getter
    private static final String dbPassword = System.getProperty(
            "DB_PASSWORD",
            System.getenv().getOrDefault("SPRING_DATASOURCE_PASSWORD", "postgres")
    );

    @Getter
    private static final String adminUsername = System.getenv()
            .getOrDefault("ADMIN_USERNAME", "admin");

    @Getter
    private static final String adminPassword = System.getenv()
            .getOrDefault("ADMIN_PASSWORD", "admin123");

    private static final String BASE_URL = "http://localhost:4112"; // меняем 4111 на 4112
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";
    private static final String ADMIN_BASIC_AUTH = "Basic YWRtaW46YWRtaW4=";

    public static String getBaseUrl() {
        return BASE_URL;
    }

    public static String getAdminUsername() {
        return ADMIN_USERNAME;
    }

    public static String getAdminPassword() {
        return ADMIN_PASSWORD;
    }

    public static String getAdminBasicAuth() {
        return ADMIN_BASIC_AUTH;
    }

    public static String getProperty(String key) {
        switch (key) {
            case "db.url":
                return getJdbcUrl();
            case "db.username":
                return getDbUsername();
            case "db.password":
                return getDbPassword();
            case "admin.username":
                return getAdminUsername();
            case "admin.password":
                return getAdminPassword();
            default:
                return null;


        }
    }
    public static String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value != null ? value : defaultValue;
    }
}