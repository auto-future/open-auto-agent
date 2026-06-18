package cn.unicom.soc.servers.util;

import cn.unicom.soc.servers.entity.HttpToolConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite 单文件数据库管理器
 * 负责 HTTP 工具配置的持久化存储
 */
@Component
public class SqliteDBManager {

    private static final Logger logger = LoggerFactory.getLogger(SqliteDBManager.class);

    @Value("${sqlite.db.path:./data/app.db}")
    private String dbPath;

    private static String staticDbPath;
    private static volatile boolean initialized = false;

    /**
     * 设置数据库路径（用于命令行工具等非 Spring 环境）
     */
    public static synchronized void setDbPath(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Database path cannot be null or empty");
        }
        staticDbPath = path;
        initialized = false; // 强制下次 getConnection 时重新初始化
        logger.info("SQLite database path set to: {}", staticDbPath);
    }

    @PostConstruct
    public void init() {
        if (staticDbPath == null || staticDbPath.isEmpty()) {
            staticDbPath = this.dbPath;
        }
        logger.info("SQLite database path configured: {}", staticDbPath);
    }

    /**
     * 确保数据目录存在
     */
    private static void ensureDataDirectoryExists() {
        File dbFile = new File(staticDbPath);
        File parentDir = dbFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
    }

    /**
     * 懒加载初始化检查
     */
    private static synchronized void ensureInitialized() {
        if (!initialized) {
            if (staticDbPath == null || staticDbPath.isEmpty()) {
                staticDbPath = "./data/app.db";
            }
            ensureDataDirectoryExists();
            initializeDatabase();
            initialized = true;
            logger.info("SQLite database lazily initialized at: {}", staticDbPath);
        }
    }

    /**
     * 获取数据库连接
     */
    public static Connection getConnection() throws SQLException {
        ensureInitialized();
        return DriverManager.getConnection("jdbc:sqlite:" + staticDbPath);
    }

    /**
     * 初始化数据库表结构
     */
    public static void initializeDatabase() {
        String createTableSql = "CREATE TABLE IF NOT EXISTS http_tools (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "tool_name VARCHAR(255) NOT NULL UNIQUE," +
                "tool_description TEXT," +
                "http_method VARCHAR(10) NOT NULL DEFAULT 'GET'," +
                "url_template TEXT NOT NULL," +
                "headers TEXT," +
                "request_body_template TEXT," +
                "params_schema TEXT," +
                "auth_type VARCHAR(50)," +
                "auth_config TEXT," +
                "timeout_ms INTEGER DEFAULT 30000," +
                "enabled BOOLEAN DEFAULT 1," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        String createIndexSql = "CREATE INDEX IF NOT EXISTS idx_http_tools_enabled ON http_tools(enabled)";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + staticDbPath);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);
            stmt.execute(createIndexSql);
            logger.info("HTTP tools table initialized successfully");
        } catch (SQLException e) {
            logger.error("Failed to initialize SQLite database", e);
            throw new RuntimeException("Failed to initialize SQLite database", e);
        }
    }

    /**
     * 插入或更新 HTTP 工具配置
     */
    public static int saveOrUpdate(HttpToolConfig config) {
        String sql = "INSERT INTO http_tools (tool_name, tool_description, http_method, url_template, headers, " +
                "request_body_template, params_schema, auth_type, auth_config, timeout_ms, enabled, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT(tool_name) DO UPDATE SET " +
                "tool_description=excluded.tool_description, http_method=excluded.http_method, " +
                "url_template=excluded.url_template, headers=excluded.headers, " +
                "request_body_template=excluded.request_body_template, params_schema=excluded.params_schema, " +
                "auth_type=excluded.auth_type, auth_config=excluded.auth_config, " +
                "timeout_ms=excluded.timeout_ms, enabled=excluded.enabled, updated_at=CURRENT_TIMESTAMP";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, config.getToolName());
            pstmt.setString(2, config.getToolDescription());
            pstmt.setString(3, config.getHttpMethod());
            pstmt.setString(4, config.getUrlTemplate());
            pstmt.setString(5, config.getHeaders());
            pstmt.setString(6, config.getRequestBodyTemplate());
            pstmt.setString(7, config.getParamsSchema());
            pstmt.setString(8, config.getAuthType());
            pstmt.setString(9, config.getAuthConfig());
            pstmt.setInt(10, config.getTimeoutMs() != null ? config.getTimeoutMs() : 30000);
            pstmt.setBoolean(11, config.getEnabled() != null ? config.getEnabled() : true);

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to save or update HTTP tool config: {}", config.getToolName(), e);
            throw new RuntimeException("Failed to save HTTP tool config", e);
        }
        return -1;
    }

    /**
     * 根据 ID 查询 HTTP 工具配置
     */
    public static HttpToolConfig findById(int id) {
        String sql = "SELECT * FROM http_tools WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToConfig(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find HTTP tool config by id: {}", id, e);
        }
        return null;
    }

    /**
     * 根据工具名称查询 HTTP 工具配置
     */
    public static HttpToolConfig findByToolName(String toolName) {
        String sql = "SELECT * FROM http_tools WHERE tool_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, toolName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToConfig(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find HTTP tool config by name: {}", toolName, e);
        }
        return null;
    }

    /**
     * 查询所有启用的 HTTP 工具配置
     */
    public static List<HttpToolConfig> findAllEnabled() {
        List<HttpToolConfig> configs = new ArrayList<>();
        String sql = "SELECT * FROM http_tools WHERE enabled = 1 ORDER BY created_at DESC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                configs.add(mapResultSetToConfig(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find all enabled HTTP tool configs", e);
        }
        return configs;
    }

    /**
     * 查询所有 HTTP 工具配置
     */
    public static List<HttpToolConfig> findAll() {
        List<HttpToolConfig> configs = new ArrayList<>();
        String sql = "SELECT * FROM http_tools ORDER BY created_at DESC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                configs.add(mapResultSetToConfig(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find all HTTP tool configs", e);
        }
        return configs;
    }

    /**
     * 删除指定 ID 的 HTTP 工具配置
     */
    public static boolean deleteById(int id) {
        String sql = "DELETE FROM http_tools WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to delete HTTP tool config by id: {}", id, e);
        }
        return false;
    }

    /**
     * 删除指定名称的 HTTP 工具配置
     */
    public static boolean deleteByName(String toolName) {
        String sql = "DELETE FROM http_tools WHERE tool_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, toolName);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Failed to delete HTTP tool config by name: {}", toolName, e);
        }
        return false;
    }

    /**
     * 更新 HTTP 工具的启用状态（按名称）
     *
     * @param toolName 工具名称
     * @param enabled  启用状态
     * @return 是否更新成功
     */
    public static boolean updateEnabledStatus(String toolName, boolean enabled) {
        String sql = "UPDATE http_tools SET enabled = ?, updated_at = CURRENT_TIMESTAMP WHERE tool_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, enabled);
            pstmt.setString(2, toolName);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Failed to update HTTP tool enabled status for tool: {}", toolName, e);
        }
        return false;
    }

    /**
     * 更新 HTTP 工具的启用状态（按ID）
     *
     * @param id      工具ID
     * @param enabled 启用状态
     * @return 是否更新成功
     */
    public static boolean updateEnabledStatusById(int id, boolean enabled) {
        String sql = "UPDATE http_tools SET enabled = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, enabled);
            pstmt.setInt(2, id);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            logger.error("Failed to update HTTP tool enabled status for tool with ID: {}", id, e);
        }
        return false;
    }

    /**
     * 将 ResultSet 映射为 HttpToolConfig 对象
     */
    private static HttpToolConfig mapResultSetToConfig(ResultSet rs) throws SQLException {
        HttpToolConfig config = new HttpToolConfig();
        config.setId(rs.getInt("id"));
        config.setToolName(rs.getString("tool_name"));
        config.setToolDescription(rs.getString("tool_description"));
        config.setHttpMethod(rs.getString("http_method"));
        config.setUrlTemplate(rs.getString("url_template"));
        config.setHeaders(rs.getString("headers"));
        config.setRequestBodyTemplate(rs.getString("request_body_template"));
        config.setParamsSchema(rs.getString("params_schema"));
        config.setAuthType(rs.getString("auth_type"));
        config.setAuthConfig(rs.getString("auth_config"));
        config.setTimeoutMs(rs.getInt("timeout_ms"));
        config.setEnabled(rs.getBoolean("enabled"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            config.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            config.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return config;
    }
}
