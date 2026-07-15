package common.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.AccountDTO;
import models.CreateUserResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JsonUtils {
    private static final JsonFactory FACTORY = JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder()
                    .maxNestingDepth(Integer.MAX_VALUE)
                    .maxStringLength(50_000_000)
                    .maxNumberLength(10_000)
                    .build())
            .build();

    private static final ObjectMapper MAPPER = new ObjectMapper(FACTORY)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private JsonUtils() {
    }

    public static List<AccountDTO> readAccountsShallow(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try (JsonParser parser = FACTORY.createParser(json)) {
            List<AccountDTO> accounts = new ArrayList<>();
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalArgumentException("Expected accounts JSON array");
            }
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                accounts.add(readAccountObject(parser));
            }
            return accounts;
        } catch (Exception e) {
            return readAccountsByRegex(json);
        }
    }

    public static double findAccountBalance(String json, long accountId) {
        List<AccountDTO> accounts = readAccountsShallow(json);
        return accounts.stream()
                .filter(acc -> acc.getId() == accountId)
                .map(AccountDTO::getBalance)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
    }

    private static List<AccountDTO> readAccountsByRegex(String json) {
        List<AccountDTO> accounts = new ArrayList<>();
        Pattern pattern = Pattern.compile(
                "\"id\"\\s*:\\s*(\\d+)\\s*,\\s*\"accountNumber\"\\s*:\\s*\"([^\"]*)\"\\s*,\\s*\"balance\"\\s*:\\s*([0-9.]+)");
        Matcher matcher = pattern.matcher(json);
        while (matcher.find()) {
            accounts.add(AccountDTO.builder()
                    .id(Long.parseLong(matcher.group(1)))
                    .accountNumber(matcher.group(2))
                    .balance(Double.parseDouble(matcher.group(3)))
                    .build());
        }
        return accounts;
    }

    public static List<CreateUserResponse> readUsersShallow(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try (JsonParser parser = FACTORY.createParser(json)) {
            List<CreateUserResponse> users = new ArrayList<>();
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IllegalArgumentException("Expected users JSON array");
            }
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                users.add(readUserObject(parser));
            }
            return users;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse users JSON: " + e.getMessage(), e);
        }
    }

    private static AccountDTO readAccountObject(JsonParser parser) throws Exception {
        AccountDTO account = new AccountDTO();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String field = parser.currentName();
            parser.nextToken();
            switch (field) {
                case "id" -> account.setId(parser.getLongValue());
                case "accountNumber" -> account.setAccountNumber(parser.getValueAsString());
                case "balance" -> account.setBalance(parser.getDoubleValue());
                default -> parser.skipChildren();
            }
        }
        return account;
    }

    private static CreateUserResponse readUserObject(JsonParser parser) throws Exception {
        CreateUserResponse user = new CreateUserResponse();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String field = parser.currentName();
            parser.nextToken();
            switch (field) {
                case "id" -> user.setId(parser.getLongValue());
                case "username" -> user.setUsername(parser.getValueAsString());
                case "password" -> user.setPassword(parser.getValueAsString());
                case "name" -> user.setName(parser.getValueAsString());
                case "role" -> user.setRole(parser.getValueAsString());
                default -> parser.skipChildren();
            }
        }
        return user;
    }

    public static <T> List<T> readList(String json, Class<T> itemType) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        if (itemType == AccountDTO.class) {
            return (List<T>) readAccountsShallow(json);
        }
        if (itemType == CreateUserResponse.class) {
            return (List<T>) readUsersShallow(json);
        }
        try {
            return MAPPER.readValue(json, MAPPER.getTypeFactory().constructCollectionType(List.class, itemType));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON list: " + e.getMessage(), e);
        }
    }

    public static <T> T read(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    public static <T> T read(String json, TypeReference<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON: " + e.getMessage(), e);
        }
    }
}
