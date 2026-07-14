package api.database;

import api.dao.AccountDao;
import api.dao.UserDao;
import configs.Config;
import lombok.Getter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class DBRequest<T> {
    private RequestType requestType;
    private String table;
    private List<Condition> conditions;
    private Function<ResultSet, T> extractor;
    private List<String> columns;

    public enum RequestType {
        SELECT, INSERT, UPDATE, DELETE
    }

    @Getter
    public static class Condition {
        private String column;
        private Object value;
        private Operator operator;

        public Condition(String column, Object value, Operator operator) {
            this.column = column;
            this.value = value;
            this.operator = operator;
        }

        public static Condition equalTo(String column, Object value) {
            return new Condition(column, value, Operator.EQUALS);
        }

        public static Condition like(String column, String pattern) {
            return new Condition(column, pattern, Operator.LIKE);
        }

        public String toSqlClause() {
            switch (operator) {
                case EQUALS: return column + " = ?";
                case LIKE: return column + " LIKE ?";
                case GREATER_THAN: return column + " > ?";
                case LESS_THAN: return column + " < ?";
                default: return column + " = ?";
            }
        }
    }

    public enum Operator {
        EQUALS, LIKE, GREATER_THAN, LESS_THAN
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {
        private RequestType requestType;
        private String table;
        private List<Condition> conditions = new ArrayList<>();
        private List<String> columns;
        private Function<ResultSet, T> extractor;

        public Builder<T> requestType(RequestType requestType) {
            this.requestType = requestType;
            return this;
        }

        public Builder<T> table(String table) {
            this.table = table;
            return this;
        }

        public Builder<T> where(Condition condition) {
            this.conditions.add(condition);
            return this;
        }

        public Builder<T> columns(List<String> columns) {
            this.columns = columns;
            return this;
        }

        public T extractAs(Class<T> clazz) {
            this.extractor = createExtractor(clazz);
            DBRequest<T> request = new DBRequest<>();
            request.requestType = this.requestType;
            request.table = this.table;
            request.conditions = this.conditions;
            request.columns = this.columns;
            request.extractor = this.extractor;
            return request.execute();
        }

        public List<T> extractAsList(Class<T> clazz) {
            this.extractor = createExtractor(clazz);
            DBRequest<T> request = new DBRequest<>();
            request.requestType = this.requestType;
            request.table = this.table;
            request.conditions = this.conditions;
            request.columns = this.columns;
            request.extractor = this.extractor;
            return request.executeList();
        }

        private Function<ResultSet, T> createExtractor(Class<T> clazz) {
            return rs -> {
                try {
                    if (clazz == UserDao.class) {
                        return (T) mapToUserDao(rs);
                    } else if (clazz == AccountDao.class) {
                        return (T) mapToAccountDao(rs);
                    }
                    return clazz.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to map ResultSet", e);
                }
            };
        }

        private UserDao mapToUserDao(ResultSet rs) throws SQLException {
            return UserDao.builder()
                    .id(rs.getLong("id"))
                    .username(rs.getString("username"))
                    .passwordHash(rs.getString("password"))
                    .role(rs.getString("role"))
                    .name(rs.getString("name"))
                    .createdAt(toLocalDateTime(rs, "created_at"))
                    .updatedAt(toLocalDateTime(rs, "updated_at"))
                    .build();
        }


        private AccountDao mapToAccountDao(ResultSet rs) throws SQLException {
            return AccountDao.builder()
                    .id(rs.getLong("id"))
                    .userId(rs.getLong("customer_id"))
                    .accountNumber(rs.getString("account_number"))
                    .balance(rs.getDouble("balance"))
                    .createdAt(toLocalDateTime(rs, "created_at"))
                    .build();
        }


        private LocalDateTime toLocalDateTime(ResultSet rs, String columnName) throws SQLException {
            Timestamp timestamp = rs.getTimestamp(columnName);
            return timestamp != null ? timestamp.toLocalDateTime() : null;
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                Config.getProperty("db.url"),
                Config.getProperty("db.username"),
                Config.getProperty("db.password")
        );
    }


    private T execute() {
        String sql = buildSql();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParameters(stmt);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractor.apply(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + sql, e);
        }
    }


    private List<T> executeList() {
        String sql = buildSql();
        List<T> results = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            setParameters(stmt);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    T item = extractor.apply(rs);
                    if (item != null) {
                        results.add(item);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Query failed: " + sql, e);
        }
        return results;
    }

    private String buildSql() {
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(columns == null || columns.isEmpty() ? "*" : String.join(", ", columns));
        sql.append(" FROM ").append(table);
        if (conditions != null && !conditions.isEmpty()) {
            sql.append(" WHERE ");
            for (int i = 0; i < conditions.size(); i++) {
                if (i > 0) {
                    sql.append(" AND ");
                }
                sql.append(conditions.get(i).toSqlClause());
            }
        }
        return sql.toString();
    }

    private void setParameters(PreparedStatement stmt) throws SQLException {
        if (conditions != null) {
            for (int i = 0; i < conditions.size(); i++) {
                stmt.setObject(i + 1, conditions.get(i).getValue());
            }
        }
    }
}