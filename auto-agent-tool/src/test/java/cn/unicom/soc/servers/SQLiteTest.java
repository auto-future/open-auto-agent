package cn.unicom.soc.servers;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = SpringMcpServersApplication.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:sqlite:./test_chunk_data.db",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SQLiteTest {

    @Test
    void testSQLiteConnection() throws Exception {
        // 测试SQLite连接
        Connection conn = DriverManager.getConnection("jdbc:sqlite:./test_connection.db");
        assertNotNull(conn, "SQLite连接应该成功");

        // 测试基本操作
        try (Statement stmt = conn.createStatement()) {
            // 创建测试表
            stmt.execute("CREATE TABLE IF NOT EXISTS test_table (id INTEGER PRIMARY KEY, name TEXT)");
            
            // 插入测试数据
            stmt.execute("INSERT INTO test_table (name) VALUES ('test')");
            
            // 查询数据
            ResultSet rs = stmt.executeQuery("SELECT name FROM test_table WHERE name='test'");
            assert rs.next();
            assert "test".equals(rs.getString("name"));
        } finally {
            conn.close();
        }
    }
}