package cn.unicom.soc.servers.util;

import cn.unicom.soc.servers.repository.ToolSetRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * 工具集数据库初始化器
 * 用于执行schema-toolset.sql创建表结构和索引
 * 工具数据由MCP工具注册和HTTP工具注册时自动插入
 */
@Component
@RequiredArgsConstructor
public class ToolSetDatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(ToolSetDatabaseInitializer.class);

    @Autowired
    private final ToolSetRepository toolSetRepository;
    
    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void init() {
        try {
            logger.info("开始初始化工具集数据库表结构...");
            
            // 执行schema-toolset.sql脚本
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("schema-toolset.sql")
            );
            populator.setContinueOnError(true); // 表已存在时继续执行
            populator.setSeparator(";"); // SQL语句分隔符
            
            populator.execute(dataSource);
            
            logger.info("工具集数据库表结构初始化完成（包括索引创建）");
            logger.info("工具将在MCP和HTTP加载时自动注册到数据库");
        } catch (Exception e) {
            logger.error("工具集数据库表结构初始化失败: {}", e.getMessage(), e);
        }
    }
}