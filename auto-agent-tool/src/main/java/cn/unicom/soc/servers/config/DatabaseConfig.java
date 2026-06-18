package cn.unicom.soc.servers.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "cn.unicom.soc.servers.service")
@EnableTransactionManagement
public class DatabaseConfig {
    // JPA配置类
    // 配置将在application.properties中完成
}