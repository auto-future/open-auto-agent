package cn.unicom.soc.servers.util;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 数据库初始化工具，确保分块相关的表存在
 */
@Component
@RequiredArgsConstructor
public class DatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    // 由于使用JPA自动建表，手动初始化不再必要
    // JPA的hibernate.ddl-auto=update会自动创建和更新表结构
    // 如需手动初始化，取消下面的注释
    /*
    @PostConstruct
    public void initializeTables() {
        System.out.println("使用JPA自动管理表结构，无需手动初始化SQLite表");
    }
    */

    @PostConstruct
    public void initializeTables() {
        logger.info("使用JPA自动管理表结构，无需手动初始化表");
    }
}