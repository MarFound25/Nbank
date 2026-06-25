//package api.dao.comparison;
//
//import lombok.extern.slf4j.Slf4j;
//import org.yaml.snakeyaml.Yaml;
//
//import java.io.InputStream;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * YAML-based configuration for better readability and maintainability.
// * Supports inheritance and complex mappings.
// */
//@Slf4j
//public class DaoComparisonConfigLoader {
//
//    private final Map<Class<?>, DaoComparisonRule> rulesCache = new ConcurrentHashMap<>();
//    private final String configPath;
//
//    public DaoComparisonConfigLoader(String configPath) {
//        this.configPath = configPath;
//        loadConfiguration();
//    }
//
//    private void loadConfiguration() {
//        try (InputStream input = getClass().getClassLoader().getResourceAsStream(configPath)) {
//            if (input == null) {
//                log.warn("Configuration file not found: {}, using defaults", configPath);
//                loadDefaultConfiguration();
//                return;
//            }
//
//            // Support both .properties and .yml formats
//            if (configPath.endsWith(".yml") || configPath.endsWith(".yaml")) {
//                loadYamlConfiguration(input);
//            } else {
//                loadPropertiesConfiguration(input);
//            }
//
//        } catch (Exception e) {
//            log.error("Failed to load configuration: {}", configPath, e);
//            throw new RuntimeException("Configuration loading failed", e);
//        }
//    }
//
//    private void loadYamlConfiguration(InputStream input) {
//        Yaml yaml = new Yaml();
//        Map<String, Object> config = yaml.load(input);
//        // Parse YAML structure
//        // ... implementation
//    }
//
//    private void loadPropertiesConfiguration(InputStream input) {
//        // Current properties loading logic
//        // ... implementation
//    }
//
//    private void loadDefaultConfiguration() {
//        log.info("Loading default comparison rules");
//        // Register default rules for common entities
//        // ... implementation
//    }
//
//    public DaoComparisonRule getRuleFor(Class<?> responseClass) {
//        return rulesCache.computeIfAbsent(responseClass, this::createRule);
//    }
//
//    private DaoComparisonRule createRule(Class<?> clazz) {
//        // Smart rule creation based on annotations or naming conventions
//        // ... implementation
//        return new DaoComparisonRule();
//    }
//
//    public static class DaoComparisonRule {
//        private final Map<String, String> fieldMappings = new ConcurrentHashMap<>();
//
//        public void addMapping(String apiField, String daoField) {
//            fieldMappings.put(apiField, daoField);
//        }
//
//        public Map<String, String> getFieldMappings() {
//            return new ConcurrentHashMap<>(fieldMappings);
//        }
//    }
//}