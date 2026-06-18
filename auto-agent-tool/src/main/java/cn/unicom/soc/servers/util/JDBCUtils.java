package cn.unicom.soc.servers.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.List;

/**
 * @Description
 * @Author chenss
 * @CreateTime 2025-06-18 09:09:27
 * @ModifyTime
 */
@Component
public class JDBCUtils {

    @Value("${mysql.url}")
    private String mysqlUrl;

    @Value("${mysql.driver}")
    private String mysqlDriver;

    @Value("${mysql.username}")
    private String mysqlUsername;

    @Value("${mysql.password}")
    private String mysqlPassword;

    private static volatile boolean initialized = false;
    private static String dbUrl;
    private static String dbDriver;
    private static String dbUsername;
    private static String dbPassword;

    @PostConstruct
    public void init() {
        dbUrl = this.mysqlUrl;
        dbDriver = this.mysqlDriver;
        dbUsername = this.mysqlUsername;
        dbPassword = this.mysqlPassword;
        initialized = true;
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static ObjectNode executeSingleSQL(String sql, Object paramList) throws SQLException {
        ObjectNode result = objectMapper.createObjectNode();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            setParameters(pstmt, paramList);

            boolean isQuery = pstmt.execute();
            if (isQuery) {
                try (ResultSet rs = pstmt.getResultSet()) {
                    result.put("data", processResultSet(rs));
                }
            } else {
                if (sql.contains("insert") || sql.contains("update")) {
                    result.put("affectedRows", pstmt.getUpdateCount());
                }
            }
        } catch (ClassNotFoundException e) {
            result.put("exception", e.getMessage());
        }

        return result;
    }

    public static ObjectNode executeInTransaction(String sql, Object paramList) {
        ObjectNode result = objectMapper.createObjectNode();
        Connection conn = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                setParameters(pstmt, paramList);
                pstmt.execute();
                conn.commit();

                if (pstmt.getResultSet() != null) {
                    try (ResultSet rs = pstmt.getResultSet()) {
                        ArrayNode objects = processResultSet(rs);
                        if (!objects.isEmpty()) {
                            result.put("data", objects);
                        }
                    }
                } else {
                    if (sql.contains("insert") || sql.contains("update")) {
                        result.put("affectedRows", pstmt.getUpdateCount());
                    }
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            rollbackTransaction(conn, e);
            e.printStackTrace();
            result.put("error", e.getMessage());
        } finally {
            closeConnection(conn);
        }

        return result;
    }

    public static ArrayNode executeBatch(ArrayNode queries, boolean transaction) {
        ArrayNode results = objectMapper.createArrayNode();
        Connection conn = null;

        try {
            conn = getConnection();
            if (transaction) {
                conn.setAutoCommit(false);
            }

            for (JsonNode queryObj : queries) {
                ObjectNode query = (ObjectNode) queryObj;
                ObjectNode result = objectMapper.createObjectNode();
                result.put("sql", query.get("sql").asText());

                try (PreparedStatement pstmt = conn.prepareStatement(query.get("sql").asText())) {
                    setParameters(pstmt, query.get("params").toString());

                    boolean isQuery = pstmt.execute();
                    if (isQuery) {
                        try (ResultSet rs = pstmt.getResultSet()) {
                            ArrayNode objects = processResultSet(rs);
                            if (!objects.isEmpty()) {
                                result.put("data", objects);
                            }

                        }
                    } else {
                        result.put("affectedRows", pstmt.getUpdateCount());
                    }

                    result.put("success", true);
                } catch (SQLException e) {
                    result.put("success", false);
                    result.put("error", e.getMessage());

                    if (transaction) {
                        throw e;
                    }
                }

                results.add(result);
            }

            if (transaction) {
                conn.commit();
            }
        } catch (SQLException | ClassNotFoundException e) {
            if (transaction) {
                rollbackTransaction(conn, e);
            }
            throw new RuntimeException("Batch execution failed", e);
        } finally {
            if (transaction) {
                closeConnection(conn);
            }
        }

        return results;
    }

    public static void checkInitialization() {
        if (!initialized) {
            throw new RuntimeException("JDBCUtils 未初始化，请确保 Spring 容器已正确加载");
        }
    }

    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        checkInitialization();
        Class.forName(dbDriver);

        // 2. 获取数据库连接
        return DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
    }

    private static void setParameters(PreparedStatement pstmt, Object paramList) throws SQLException {
        if (paramList != null && paramList instanceof List) {
            List<?> params = (List<?>) paramList;
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
        }
    }

    private static ArrayNode processResultSet(ResultSet rs) throws SQLException {
        ArrayNode result = objectMapper.createArrayNode();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
            ObjectNode row = objectMapper.createObjectNode();
            for (int i = 1; i <= columnCount; i++) {
                Object value = rs.getObject(i);
                if (value != null) {
                    row.putPOJO(metaData.getColumnName(i), value);
                } else {
                    row.putNull(metaData.getColumnName(i));
                }
            }
            result.add(row);
        }
        return result;
    }

    private static void rollbackTransaction(Connection conn, Exception e) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                logError("rollbackTransaction", ex);
            }
        }
        logError("transaction", e);
    }

    private static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.getAutoCommit()) {
                    conn.setAutoCommit(true);
                }
                conn.close();
            } catch (SQLException e) {
                logError("closeConnection", e);
            }
        }
    }

    private static void logError(String methodName, Exception e) {
//        System.err.println("Error in " + methodName + ": " + JSONObject.toJSONString(e));
//        e.printStackTrace();
    }

    public static void main(String[] args) {

    }
}
