package configs;

public class Config {
    private static final String BASE_URL = "http://localhost:4111";
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
        if ("admin.username".equals(key)) {
            return "admin";
        }
        if ("admin.password".equals(key)) {
            return "admin";
        }
        return null;
    }
}