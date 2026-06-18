package cn.unicom.soc.servers;

import cn.unicom.soc.servers.service.*;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@EnableScheduling
@SpringBootApplication
@EntityScan(basePackages = "cn.unicom.soc.servers.entity")
@EnableJpaRepositories(basePackages = "cn.unicom.soc.servers.repository")
public class SpringMcpServersApplication {


    public static void main(String[] args) {
        SpringApplication.run(SpringMcpServersApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Autowired
    private MonitorService monitorService;

    @Bean
    public ToolCallbackProvider myTools(ApplicationContext applicationContext) throws NoSuchMethodException, IOException {
        // 设置监控服务到拦截器
        McpToolCallInterceptor.setMonitorService(monitorService);
        List<ToolCallback> toolList = new ArrayList<>();

        // 获取分块拦截器实例
        AutoChunkInterceptor chunkInterceptor = applicationContext.getBean(AutoChunkInterceptor.class);

        // 获取MCP工具注册服务（用于同步到数据库）
        McpToolRegistryService mcpToolRegistryService = applicationContext.getBean(McpToolRegistryService.class);

        // ===== 1. 添加系统信息工具 =====
        SystemInfoService systemInfoService = new SystemInfoService();

        MethodToolCallbackProvider systemInfoProvider = MethodToolCallbackProvider.builder()
                .toolObjects(systemInfoService)
                .build();

        // 包装系统信息工具回调，确保经过分块检查
        List<ToolCallback> systemInfoTools = new ArrayList<>();
        for (ToolCallback callback : systemInfoProvider.getToolCallbacks()) {
            toolList.add(McpToolCallInterceptor.wrap(callback, chunkInterceptor));
            systemInfoTools.add(callback);
        }

        // 同步系统信息工具到数据库
        mcpToolRegistryService.syncMcpToolsToDatabase(
                "System Info",
                "System information tools for monitoring OS, CPU, memory, and disk",
                systemInfoTools
        );

        // ===== 2. 从数据库中加载HTTP工具 =====
        HttpToolRegistryService httpToolRegistryService = new HttpToolRegistryService();
        List<ToolCallback> httpToolCallbacks = httpToolRegistryService.loadHttpToolsFromDatabase();

        // 包装HTTP工具回调，确保经过分块检查
        for (ToolCallback callback : httpToolCallbacks) {
            toolList.add(McpToolCallInterceptor.wrap(callback, chunkInterceptor));
        }

        // ===== 3. 加载HTTP API管理工具 =====
        HttpApiManagerService httpApiManagerService = new HttpApiManagerService();

        // 创建自定义方法工具回调提供者，自动注册所有带有@Tool注解的方法
        CustomMethodToolCallbackProvider httpApiProvider
                = new CustomMethodToolCallbackProvider(httpApiManagerService);

        // 将自定义工具添加到工具列表，包装以确保分块检查
        List<ToolCallback> httpApiTools = new ArrayList<>();
        for (ToolCallback callback : httpApiProvider.getToolCallbacks()) {
            toolList.add(McpToolCallInterceptor.wrap(callback, chunkInterceptor));
            httpApiTools.add(callback);
        }

        // 同步HTTP API管理工具到数据库
        mcpToolRegistryService.syncMcpToolsToDatabase(
                "HTTP API Manager",
                "HTTP API management tools for dynamic API registration and execution",
                httpApiTools
        );

        // ===== 4. 注册分块查询工具 =====
        ChunkQueryTool chunkQueryTool = new ChunkQueryTool();
        CustomMethodToolCallbackProvider chunkQueryProvider
                = new CustomMethodToolCallbackProvider(chunkQueryTool);

        // 包装分块查询工具回调，确保经过分块检查
        List<ToolCallback> chunkQueryTools = new ArrayList<>();
        for (ToolCallback callback : chunkQueryProvider.getToolCallbacks()) {
            toolList.add(McpToolCallInterceptor.wrap(callback, chunkInterceptor));
            chunkQueryTools.add(callback);
        }

        // 同步分块查询工具到数据库
        mcpToolRegistryService.syncMcpToolsToDatabase(
                "Chunk Query",
                "Chunk-based query tools for large content processing",
                chunkQueryTools
        );

        System.out.println("Total registered tools: " + toolList.size());
        System.out.println("  - System Info tools: " + systemInfoTools.size());
        System.out.println("  - HTTP tools: " + httpToolCallbacks.size());
        System.out.println("  - HTTP API Manager tools: " + httpApiTools.size());
        System.out.println("  - Chunk Query tools: " + chunkQueryTools.size());

        CustomToolCallbackProvider customToolCallbackProvider = new CustomToolCallbackProvider(toolList.toArray(new ToolCallback[0]));

        // 设置工具回调提供者，以便HttpApiManagerService可以动态添加新API
        httpApiManagerService.setToolCallbackProvider(customToolCallbackProvider);

        return customToolCallbackProvider;
    }
}