package api.dao.comparison;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class DaoComparator {

    private static final Logger log = LoggerFactory.getLogger(DaoComparator.class);
    private static final String DEFAULT_CONFIG = "dao-comparison.properties";

    private final Map<Class<?>, Map<String, String>> rulesCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, Map<String, Field>> fieldsCache = new ConcurrentHashMap<>();
    private final String configFile;

    public DaoComparator() {
        this(DEFAULT_CONFIG);
    }

    public DaoComparator(String configFile) {
        this.configFile = configFile;
        loadConfiguration();
    }

    public void compare(Object apiResponse, Object dao) {
        long startTime = System.currentTimeMillis();

        try {
            validateInputs(apiResponse, dao);

            Map<String, String> fieldMappings = getFieldMappings(apiResponse.getClass());

            if (fieldMappings.isEmpty()) {
                log.warn("No field mappings found for {}. Skipping comparison.",
                        apiResponse.getClass().getSimpleName());
                return;
            }

            ComparisonResult result = new ComparisonResult();

            for (Map.Entry<String, String> mapping : fieldMappings.entrySet()) {
                String apiField = mapping.getKey();
                String daoField = mapping.getValue();

                compareField(apiResponse, dao, apiField, daoField, result);
            }

            if (result.hasFailures()) {
                throw new AssertionError(formatFailureMessage(result, apiResponse.getClass()));
            }

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Successfully validated {} against DAO in {}ms",
                    apiResponse.getClass().getSimpleName(), duration);

        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during comparison", e);
            throw new RuntimeException("Comparison failed: " + e.getMessage(), e);
        }
    }

    private void loadConfiguration() {
        log.info("Loading DAO comparison configuration from: {}", configFile);

        try (InputStream input = getClass().getClassLoader().getResourceAsStream(configFile)) {
            if (input == null) {
                log.warn("Configuration file {} not found. Only exact field name matching will work.", configFile);
                return;
            }

            Properties props = new Properties();
            props.load(input);

            int loadedCount = 0;
            for (String key : props.stringPropertyNames()) {
                if (key.contains(".") && !key.startsWith("models.")) {
                    log.debug("Skipping non-rule property: {}", key);
                    continue;
                }

                try {
                    Class<?> clazz = Class.forName(key);
                    String mapping = props.getProperty(key);
                    Map<String, String> fieldMappings = parseMapping(mapping);
                    rulesCache.put(clazz, fieldMappings);
                    loadedCount++;
                    log.debug("Loaded mapping for {}: {}", clazz.getSimpleName(), fieldMappings);
                } catch (ClassNotFoundException e) {
                    log.warn("Class not found: {}, skipping rule", key);
                } catch (Exception e) {
                    log.error("Failed to load rule for: {}", key, e);
                }
            }

            log.info("Successfully loaded {} comparison rules", loadedCount);

        } catch (Exception e) {
            log.error("Failed to load configuration from {}", configFile, e);
            throw new RuntimeException("Configuration loading failed", e);
        }
    }

    private Map<String, String> parseMapping(String mapping) {
        Map<String, String> result = new ConcurrentHashMap<>();
        String[] pairs = mapping.split(",");

        for (String pair : pairs) {
            String[] parts = pair.trim().split(":");
            if (parts.length == 2) {
                result.put(parts[0].trim(), parts[1].trim());
            } else {
                log.warn("Invalid mapping format: '{}' in '{}'", pair, mapping);
            }
        }

        return result;
    }

    private Map<String, String> getFieldMappings(Class<?> clazz) {
        Map<String, String> mappings = rulesCache.get(clazz);
        return mappings != null ? mappings : Map.of();
    }

    private void validateInputs(Object apiResponse, Object dao) {
        if (apiResponse == null) {
            throw new IllegalArgumentException("API response cannot be null");
        }
        if (dao == null) {
            throw new IllegalArgumentException("DAO object cannot be null");
        }
    }

    private void compareField(Object apiResponse, Object dao,
                              String apiField, String daoField,
                              ComparisonResult result) {
        try {
            Object apiValue = extractValue(apiResponse, apiField);
            Object daoValue = extractValue(dao, daoField);

            if (!Objects.equals(apiValue, daoValue)) {
                result.addFailure(apiField, apiValue, daoValue);
                log.debug("Field mismatch - {}: API='{}', DAO='{}'", apiField, apiValue, daoValue);
            }

        } catch (NoSuchFieldException e) {
            String errorMsg = String.format("Field '%s' not found in %s",
                    apiField, apiResponse.getClass().getSimpleName());
            log.error(errorMsg);
            result.addError(apiField, errorMsg);
        } catch (IllegalAccessException e) {
            String errorMsg = String.format("Cannot access field '%s' in %s",
                    apiField, apiResponse.getClass().getSimpleName());
            log.error(errorMsg, e);
            result.addError(apiField, errorMsg);
        } catch (Exception e) {
            String errorMsg = String.format("Failed to compare field '%s' -> '%s'", apiField, daoField);
            log.error(errorMsg, e);
            result.addError(apiField, errorMsg);
        }
    }

    private Object extractValue(Object obj, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {

        Map<String, Field> fieldCache = fieldsCache.computeIfAbsent(
                obj.getClass(),
                k -> new ConcurrentHashMap<>()
        );

        Field field = fieldCache.get(fieldName);
        if (field == null) {
            field = findField(obj.getClass(), fieldName);
            fieldCache.put(fieldName, field);
        }

        field.setAccessible(true);
        return field.get(obj);
    }

    private Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> currentClass = clazz;
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        throw new NoSuchFieldException(
                String.format("Field '%s' not found in class %s or its superclasses",
                        fieldName, clazz.getSimpleName())
        );
    }

    private String formatFailureMessage(ComparisonResult result, Class<?> responseClass) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append("═".repeat(60)).append("\n");
        sb.append(String.format("  VALIDATION FAILED: %s\n", responseClass.getSimpleName()));
        sb.append("═".repeat(60)).append("\n");

        if (!result.getFailures().isEmpty()) {
            sb.append("\n📋 Field Mismatches:\n");
            result.getFailures().forEach((field, values) -> {
                sb.append(String.format("  • %s\n", field));
                sb.append(String.format("    Expected (DAO): %s\n", values.getExpected()));
                sb.append(String.format("    Actual (API):  %s\n", values.getActual()));
            });
        }

        if (!result.getErrors().isEmpty()) {
            sb.append("\n⚠️  Technical Errors:\n");
            result.getErrors().forEach((field, error) ->
                    sb.append(String.format("  • %s: %s\n", field, error))
            );
        }

        sb.append("\n").append("═".repeat(60)).append("\n");
        return sb.toString();
    }

    private static class ComparisonResult {
        private final Map<String, FieldMismatch> failures = new ConcurrentHashMap<>();
        private final Map<String, String> errors = new ConcurrentHashMap<>();

        public void addFailure(String field, Object expected, Object actual) {
            failures.put(field, new FieldMismatch(expected, actual));
        }

        public void addError(String field, String message) {
            errors.put(field, message);
        }

        public boolean hasFailures() {
            return !failures.isEmpty() || !errors.isEmpty();
        }

        public Map<String, FieldMismatch> getFailures() { return failures; }
        public Map<String, String> getErrors() { return errors; }

        private static class FieldMismatch {
            private final Object expected;
            private final Object actual;

            FieldMismatch(Object expected, Object actual) {
                this.expected = expected;
                this.actual = actual;
            }

            public Object getExpected() { return expected; }
            public Object getActual() { return actual; }
        }
    }
}
