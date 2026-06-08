package endpoints;

public class Endpoint {

    public static final String ADMIN_USERS = "/api/v1/admin/users";
    public static final String ADMIN_USER_BY_ID = "/api/v1/admin/users/{id}";

    public static final String AUTH_LOGIN = "/api/v1/auth/login";

    public static final String ACCOUNTS = "/api/v1/accounts";
    public static final String ACCOUNTS_DEPOSIT = "/api/v1/accounts/deposit";
    public static final String ACCOUNTS_TRANSFER = "/api/v1/accounts/transfer";
    public static final String ACCOUNTS_TRANSACTIONS = "/api/v1/accounts/{accountId}/transactions";

    public static final String CUSTOMER_PROFILE = "/api/v1/customer/profile";
    public static final String CUSTOMER_ACCOUNTS = "/api/v1/customer/accounts";
}