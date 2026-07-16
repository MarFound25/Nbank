package common.helpers;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.AccountDTO;
import models.AccountResponce;
import models.CreateUserResponse;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Backend serializes circular account/user graphs into corrupted deep JSON that crashes
 * Admin/Transfer UI. Installs an XHR sanitizer via CDP {@code Page.addScriptToEvaluateOnNewDocument}
 * (runs before React on every navigation) and serves shallow overrides from {@code localStorage}.
 */
public final class UiBrokenJsonMock {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String LS_ADMIN_USERS = "__nbAdminUsers";
    private static final String LS_ACCOUNTS = "__nbAccounts";

    private static final String INSTALL_SCRIPT = """
            (function() {
              if (window.__nbankJsonMockInstalled) return;
              window.__nbankJsonMockInstalled = true;
              window.__adminUsersJson = localStorage.getItem('__nbAdminUsers');
              window.__customerAccountsJson = localStorage.getItem('__nbAccounts');

              function shouldSanitize(method, url) {
                if ((method || '').toUpperCase() !== 'GET') return null;
                const u = String(url || '');
                if (u.includes('/api/v1/admin/users')) return 'users';
                if (u.includes('/api/v1/customer/accounts')) return 'accounts';
                return null;
              }

              const origOpen = XMLHttpRequest.prototype.open;
              const origSend = XMLHttpRequest.prototype.send;
              const origSetRequestHeader = XMLHttpRequest.prototype.setRequestHeader;

              XMLHttpRequest.prototype.open = function(method, url) {
                this.__nbMethod = method;
                this.__nbUrl = String(url);
                this.__nbHeaders = {};
                return origOpen.apply(this, arguments);
              };

              XMLHttpRequest.prototype.setRequestHeader = function(name, value) {
                this.__nbHeaders = this.__nbHeaders || {};
                this.__nbHeaders[name] = value;
                return origSetRequestHeader.apply(this, arguments);
              };

              XMLHttpRequest.prototype.send = function(body) {
                const kind = shouldSanitize(this.__nbMethod, this.__nbUrl);
                if (!kind) {
                  return origSend.apply(this, arguments);
                }

                const outer = this;
                let responseBody = '[]';
                if (kind === 'users') {
                  responseBody = localStorage.getItem('__nbAdminUsers') || window.__adminUsersJson || '[]';
                } else {
                  responseBody = localStorage.getItem('__nbAccounts') || window.__customerAccountsJson || '[]';
                }

                queueMicrotask(function() {
                  Object.defineProperty(outer, 'readyState', {configurable: true, get: function() { return 4; }});
                  Object.defineProperty(outer, 'status', {configurable: true, get: function() { return 200; }});
                  Object.defineProperty(outer, 'statusText', {configurable: true, get: function() { return 'OK'; }});
                  Object.defineProperty(outer, 'responseText', {configurable: true, get: function() { return responseBody; }});
                  Object.defineProperty(outer, 'response', {configurable: true, get: function() { return responseBody; }});
                  outer.getResponseHeader = function(name) {
                    return String(name).toLowerCase() === 'content-type' ? 'application/json;charset=utf-8' : null;
                  };
                  outer.getAllResponseHeaders = function() {
                    return 'content-type: application/json;charset=utf-8\\r\\n';
                  };
                  if (typeof outer.onreadystatechange === 'function') outer.onreadystatechange();
                  if (typeof outer.onload === 'function') outer.onload();
                  outer.dispatchEvent(new Event('readystatechange'));
                  outer.dispatchEvent(new Event('load'));
                  outer.dispatchEvent(new Event('loadend'));
                });
              };
            })();
            """;

    private static boolean cdpInstalled;

    private UiBrokenJsonMock() {
    }

    public static void install() {
        ensureCdpScript();
        try {
            Selenide.executeJavaScript(INSTALL_SCRIPT);
        } catch (Exception ignored) {
            // Driver may not be ready yet; CDP script covers next navigations.
        }
    }

    public static void setAdminUsersOverride(List<CreateUserResponse> users) {
        install();
        String json = toUsersJson(users);
        Selenide.executeJavaScript(
                "localStorage.setItem(arguments[0], arguments[1]); window.__adminUsersJson = arguments[1];",
                LS_ADMIN_USERS, json);
    }

    public static void setCustomerAccountsOverride(List<AccountDTO> accounts) {
        install();
        String json = toAccountsJson(accounts);
        Selenide.executeJavaScript(
                "localStorage.setItem(arguments[0], arguments[1]); window.__customerAccountsJson = arguments[1];",
                LS_ACCOUNTS, json);
    }

    public static void setCustomerAccountsOverride(String token) {
        setCustomerAccountsOverride(requests.steps.UserSteps.getAccounts(token));
    }

    public static CreateUserResponse userWithAccounts(
            long id,
            String username,
            String name,
            String role,
            List<AccountDTO> accounts
    ) {
        List<AccountResponce> mapped = new ArrayList<>();
        for (AccountDTO account : accounts) {
            mapped.add(AccountResponce.builder()
                    .id((int) account.getId())
                    .accountNumber(account.getAccountNumber())
                    .balance(account.getBalance())
                    .build());
        }
        return CreateUserResponse.builder()
                .id(id)
                .username(username)
                .name(name)
                .role(role)
                .accounts(mapped)
                .build();
    }

    private static void ensureCdpScript() {
        if (cdpInstalled || !WebDriverRunner.hasWebDriverStarted()) {
            return;
        }
        try {
            WebDriver driver = WebDriverRunner.getWebDriver();
            if (driver instanceof org.openqa.selenium.chrome.ChromeDriver chromeDriver) {
                chromeDriver.executeCdpCommand(
                        "Page.addScriptToEvaluateOnNewDocument",
                        Map.of("source", INSTALL_SCRIPT));
                cdpInstalled = true;
            }
        } catch (Exception e) {
            // Fallback: per-page install() still helps for SPA navigations.
        }
    }

    public static void resetCdpFlag() {
        cdpInstalled = false;
    }

    private static String toUsersJson(List<CreateUserResponse> users) {
        try {
            List<Map<String, Object>> shallow = new ArrayList<>();
            for (CreateUserResponse user : users) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", user.getId());
                item.put("username", user.getUsername());
                item.put("password", user.getPassword());
                item.put("name", user.getName() != null ? user.getName() : "");
                item.put("role", user.getRole());
                List<Map<String, Object>> accounts = new ArrayList<>();
                if (user.getAccounts() != null) {
                    for (AccountResponce acc : user.getAccounts()) {
                        Map<String, Object> a = new LinkedHashMap<>();
                        a.put("id", acc.getId());
                        a.put("accountNumber", acc.getAccountNumber());
                        a.put("balance", acc.getBalance() != null ? acc.getBalance() : 0);
                        a.put("transactions", List.of());
                        accounts.add(a);
                    }
                }
                item.put("accounts", accounts);
                shallow.add(item);
            }
            return MAPPER.writeValueAsString(shallow);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize shallow admin users", e);
        }
    }

    private static String toAccountsJson(List<AccountDTO> accounts) {
        try {
            List<Map<String, Object>> shallow = new ArrayList<>();
            for (AccountDTO account : accounts) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", account.getId());
                item.put("accountNumber", account.getAccountNumber());
                item.put("balance", account.getBalance());
                item.put("transactions", List.of());
                shallow.add(item);
            }
            return MAPPER.writeValueAsString(shallow);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize shallow accounts", e);
        }
    }
}
