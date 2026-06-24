package cn.unicom.soc.servers.service;

import cn.unicom.soc.servers.entity.HttpToolConfig;
import cn.unicom.soc.servers.entity.HttpToolConfigEntity;
import cn.unicom.soc.servers.entity.ToolEntity;
import cn.unicom.soc.servers.entity.ToolSetEntity;
import cn.unicom.soc.servers.repository.HttpToolConfigRepository;
import cn.unicom.soc.servers.repository.ToolRepository;
import cn.unicom.soc.servers.repository.ToolSetRepository;
import cn.unicom.soc.servers.util.SqliteDBManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * HTTP 工具注册服务
 * 自动从 SQLite 单文件数据库读取 HTTP 工具配置，并生成 ToolCallback 列表
 * 同时自动注册到JPA数据库（工具集、工具、HTTP工具配置表）
 */
@Service
public class HttpToolRegistryService {

    private static final Logger logger = LoggerFactory.getLogger(HttpToolRegistryService.class);
    private final HttpToolExecutorService executorService;
    
    @Autowired(required = false)
    private ToolSetRepository toolSetRepository;
    
    @Autowired(required = false)
    private ToolRepository toolRepository;
    
    @Autowired(required = false)
    private HttpToolConfigRepository httpToolConfigRepository;

    public HttpToolRegistryService() {
        this.executorService = new HttpToolExecutorService();
    }

    /**
     * 从 SQLite 数据库加载所有启用的 HTTP 工具配置，并转换为 ToolCallback
     * 同时自动注册到JPA数据库
     *
     * @return ToolCallback 列表
     */
    public List<ToolCallback> loadHttpToolsFromDatabase() {
        List<ToolCallback> callbacks = new ArrayList<>();
        try {
            List<HttpToolConfig> configs = SqliteDBManager.findAllEnabled();
            logger.info("Loaded {} HTTP tool configs from SQLite database", configs.size());

            for (HttpToolConfig config : configs) {
                try {
                    // 同步到JPA数据库
                    syncToJpaDatabase(config);
                    
                    GenericToolCallback callback = new GenericToolCallback(
                            config.getToolName(),
                            config.getToolDescription() != null ? config.getToolDescription() : "HTTP API tool: " + config.getToolName(),
                            config.getParamsSchema() != null ? config.getParamsSchema() : generateDefaultRequestSchema(config),
                            (toolInput) -> {
                                try {
                                    // 根据工具名称从数据库获取最新的配置
                                    HttpToolConfig latestConfig = SqliteDBManager.findByToolName(config.getToolName());
                                    if (latestConfig == null) {
                                        Map<String, Object> errorResult = new HashMap<>();
                                        errorResult.put("success", false);
                                        errorResult.put("error", "Tool configuration not found: " + config.getToolName());
                                        return new ObjectMapper().writeValueAsString(errorResult);
                                    }
                                    // 确保只有启用的工具才能被执行
                                    if (latestConfig.getEnabled() != null && !latestConfig.getEnabled()) {
                                        Map<String, Object> errorResult = new HashMap<>();
                                        errorResult.put("success", false);
                                        errorResult.put("error", "Tool is disabled: " + config.getToolName());
                                        return new ObjectMapper().writeValueAsString(errorResult);
                                    }
                                    return executorService.executeToolCall(latestConfig, toolInput);
                                } catch (Exception e) {
                                    Map<String, Object> errorResult = new HashMap<>();
                                    errorResult.put("success", false);
                                    errorResult.put("error", "Error executing tool: " + e.getMessage());
                                    try {
                                        return new ObjectMapper().writeValueAsString(errorResult);
                                    } catch (JsonProcessingException jsonException) {
                                        return "{\"success\": false, \"error\": \"Internal error occurred\"}";
                                    }
                                }
                            }
                    );
                    callbacks.add(callback);
                    logger.info("Registered HTTP tool: {} ({}) -> {}",
                            config.getToolName(), config.getHttpMethod(), config.getUrlTemplate());
                } catch (Exception e) {
                    logger.error("Failed to register HTTP tool: {}", config.getToolName(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load HTTP tools from database", e);
        }
        return callbacks;
    }
    
    /**
     * 同步HTTP工具配置到JPA数据库（upsert逻辑）
     * 如果工具集不存在则创建，存在则更新
     * 如果工具不存在则创建，存在则更新
     */
    @Transactional
    public void syncToJpaDatabase(HttpToolConfig config) {
        if (toolSetRepository == null || toolRepository == null || httpToolConfigRepository == null) {
            logger.warn("JPA repositories not available for tool: {}", config.getToolName());
            return;
        }
        
        try {
            logger.info("Syncing HTTP tool to JPA database: {}", config.getToolName());
            
            // 1. 查找或创建工具集（按业务分类，这里使用工具名称的前缀或默认分类）
            String toolSetName = "HTTP Tools"; // 可以改进为从配置中读取
            ToolSetEntity toolSet = toolSetRepository.findByName(toolSetName).orElse(null);
            
            if (toolSet == null) {
                // 创建默认HTTP工具集
                toolSet = new ToolSetEntity();
                toolSet.setName(toolSetName);
                toolSet.setDescription("HTTP API Tools");
                toolSet.setType("internal");
                toolSet.setTag("http");
                toolSet.setStatus(1);
                toolSet = toolSetRepository.saveAndFlush(toolSet);
                logger.info("Created default HTTP tool set: {}", toolSetName);
            }
            
            // 2. 查找或创建工具
            ToolEntity tool = toolRepository.findByNameAndToolSetId(config.getToolName(), toolSet.getId()).orElse(null);
            
            if (tool == null) {
                // 创建工具
                tool = new ToolEntity();
                tool.setName(config.getToolName());
                tool.setDescription(config.getToolDescription());
                tool.setToolSetId(toolSet.getId());
                tool.setType("http");
                tool.setInputSchema(config.getParamsSchema() != null ? config.getParamsSchema() : generateDefaultRequestSchema(config));
                tool.setStatus(config.getEnabled() != null && config.getEnabled() ? 1 : 0);
                tool = toolRepository.saveAndFlush(tool);
                logger.info("Created tool in JPA: {}", config.getToolName());
            } else {
                // 更新工具
                tool.setDescription(config.getToolDescription());
                tool.setInputSchema(config.getParamsSchema() != null ? config.getParamsSchema() : generateDefaultRequestSchema(config));
                tool.setStatus(config.getEnabled() != null && config.getEnabled() ? 1 : 0);
                tool = toolRepository.saveAndFlush(tool);
                logger.info("Updated tool in JPA: {}", config.getToolName());
            }
            
            // 3. 查找或创建HTTP工具配置
            HttpToolConfigEntity httpConfig = httpToolConfigRepository.findByNameAndToolSetId(
                config.getToolName(), toolSet.getId()).orElse(null);
            
            if (httpConfig == null) {
                // 创建HTTP工具配置
                httpConfig = new HttpToolConfigEntity();
                httpConfig.setName(config.getToolName());
                httpConfig.setDescription(config.getToolDescription());
                httpConfig.setToolSetId(toolSet.getId());
                httpConfig.setMethod(config.getHttpMethod() != null ? config.getHttpMethod() : "GET");
                httpConfig.setUrl(config.getUrlTemplate());
                httpConfig.setHeaders(config.getHeaders());
                httpConfig.setRequestBodyTemplate(config.getRequestBodyTemplate());
                httpConfig.setResponseSchema(config.getResponseSchema());
                httpConfig.setStatus(config.getEnabled() != null && config.getEnabled() ? 1 : 0);
                httpConfig = httpToolConfigRepository.saveAndFlush(httpConfig);
                logger.info("Created HTTP tool config in JPA: {}", config.getToolName());
            } else {
                // 更新HTTP工具配置
                httpConfig.setDescription(config.getToolDescription());
                httpConfig.setMethod(config.getHttpMethod() != null ? config.getHttpMethod() : "GET");
                httpConfig.setUrl(config.getUrlTemplate());
                httpConfig.setHeaders(config.getHeaders());
                httpConfig.setRequestBodyTemplate(config.getRequestBodyTemplate());
                httpConfig.setResponseSchema(config.getResponseSchema());
                httpConfig.setStatus(config.getEnabled() != null && config.getEnabled() ? 1 : 0);
                httpConfig = httpToolConfigRepository.saveAndFlush(httpConfig);
                logger.info("Updated HTTP tool config in JPA: {}", config.getToolName());
            }
            
            logger.info("Successfully synced HTTP tool to JPA: {}", config.getToolName());
            
        } catch (Exception e) {
            logger.error("Failed to sync HTTP tool to JPA database: {}", config.getToolName(), e);
            throw new RuntimeException("Failed to sync HTTP tool to JPA database: " + config.getToolName(), e);
        }
    }

    
    /**
     * 保存或更新 HTTP 工具配置到数据库
     * 
     * @param config 工具配置
     * @return 工具ID
     */
    public int saveOrUpdate(HttpToolConfig config) {
        try {
            return SqliteDBManager.saveOrUpdate(config);
        } catch (Exception e) {
            logger.error("Failed to save or update HTTP tool config: {}", config.getToolName(), e);
            throw new RuntimeException("Failed to save or update HTTP tool config: " + config.getToolName(), e);
        }
    }
    
    /**
     * 根据ID删除 HTTP 工具配置
     * 
     * @param id 工具ID
     * @return 是否删除成功
     */
    public boolean deleteById(int id) {
        try {
            return SqliteDBManager.deleteById(id);
        } catch (Exception e) {
            logger.error("Failed to delete HTTP tool config with ID: {}", id, e);
            throw new RuntimeException("Failed to delete HTTP tool config with ID: " + id, e);
        }
    }
    
    /**
     * 查询所有 HTTP 工具配置
     * 
     * @return 所有工具配置列表
     */
    public List<HttpToolConfig> findAll() {
        try {
            return SqliteDBManager.findAll();
        } catch (Exception e) {
            logger.error("Failed to find all HTTP tool configs", e);
            throw new RuntimeException("Failed to find all HTTP tool configs", e);
        }
    }
    
    /**
     * 更新工具启用状态
     * 
     * @param id 工具ID
     * @param enabled 启用状态
     * @return 是否更新成功
     */
    public boolean updateEnabledStatusById(int id, boolean enabled) {
        try {
            return SqliteDBManager.updateEnabledStatusById(id, enabled);
        } catch (Exception e) {
            logger.error("Failed to update enabled status for tool ID: {}", id, e);
            throw new RuntimeException("Failed to update enabled status for tool ID: " + id, e);
        }
    }
    
    /**
     * 根据工具名称注册HTTP工具到MCP
     *
     * @param config HTTP工具配置
     * @param toolCallbackProvider 工具回调提供者
     * @param currentToolNames 当前已注册的工具名称集合
     * @param executorService 执行器服务
     * @return 是否注册成功
     */
    public boolean registerHttpToolToMcp(HttpToolConfig config, CustomToolCallbackProvider toolCallbackProvider,
                                         Set<String> currentToolNames, HttpToolExecutorService executorService) {
        if (toolCallbackProvider == null || config == null) {
            return false;
        }
        try {
            String toolName = config.getToolName();
            if (currentToolNames != null && currentToolNames.contains(toolName)) {
                logger.info("HTTP tool already registered in MCP: {}", toolName);
                return true;
            }

            String inputSchema = config.getParamsSchema() != null ? config.getParamsSchema() : generateDefaultRequestSchema(config);

            GenericToolCallback callback = new GenericToolCallback(
                    toolName,
                    config.getToolDescription() != null ? config.getToolDescription() : "HTTP API tool: " + toolName,
                    inputSchema,
                    (toolInput) -> {
                        try {
                            HttpToolConfig latestConfig = SqliteDBManager.findByToolName(toolName);
                            if (latestConfig == null) {
                                Map<String, Object> errorResult = new HashMap<>();
                                errorResult.put("success", false);
                                errorResult.put("error", "Tool configuration not found: " + toolName);
                                return new ObjectMapper().writeValueAsString(errorResult);
                            }
                            if (latestConfig.getEnabled() != null && !latestConfig.getEnabled()) {
                                Map<String, Object> errorResult = new HashMap<>();
                                errorResult.put("success", false);
                                errorResult.put("error", "Tool is disabled: " + toolName);
                                return new ObjectMapper().writeValueAsString(errorResult);
                            }
                            return executorService.executeToolCall(latestConfig, toolInput);
                        } catch (Exception e) {
                            Map<String, Object> errorResult = new HashMap<>();
                            errorResult.put("success", false);
                            errorResult.put("error", "Error executing tool: " + e.getMessage());
                            try {
                                return new ObjectMapper().writeValueAsString(errorResult);
                            } catch (JsonProcessingException jsonException) {
                                return "{\"success\": false, \"error\": \"Internal error occurred\"}";
                            }
                        }
                    }
            );

            toolCallbackProvider.addTools(callback);
            if (currentToolNames != null) {
                currentToolNames.add(toolName);
            }
            logger.info("Registered HTTP tool to MCP: {}", toolName);
            return true;
        } catch (Exception e) {
            logger.error("Failed to register HTTP tool to MCP: {}", config.getToolName(), e);
            return false;
        }
    }

    /**
     * 生成默认的请求参数JSON Schema
     *
     * @param config HTTP工具配置
     * @return 生成的JSON Schema字符串
     */
    public static String generateDefaultRequestSchema(HttpToolConfig config) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> schema = new HashMap<>();
            schema.put("type", "object");
            Map<String, Object> properties = new LinkedHashMap<>();
            List<String> required = new ArrayList<>();

            // 从URL模板中提取路径参数 {paramName}
            String urlTemplate = config.getUrlTemplate();
            if (urlTemplate != null && !urlTemplate.isEmpty()) {
                java.util.regex.Pattern pathPattern = java.util.regex.Pattern.compile("\\{(\\w+)\\}");
                java.util.regex.Matcher pathMatcher = pathPattern.matcher(urlTemplate);
                while (pathMatcher.find()) {
                    String paramName = pathMatcher.group(1);
                    Map<String, Object> paramSchema = new HashMap<>();
                    paramSchema.put("type", "string");
                    paramSchema.put("description", "URL path parameter: " + paramName);
                    properties.put(paramName, paramSchema);
                }
            }

            // 从请求体模板中提取变量 ${variable}
            String bodyTemplate = config.getRequestBodyTemplate();
            if (bodyTemplate != null && !bodyTemplate.isEmpty()) {
                java.util.regex.Pattern bodyPattern = java.util.regex.Pattern.compile("\\$\\{(\\w+)\\}");
                java.util.regex.Matcher bodyMatcher = bodyPattern.matcher(bodyTemplate);
                while (bodyMatcher.find()) {
                    String varName = bodyMatcher.group(1);
                    if (!properties.containsKey(varName)) {
                        Map<String, Object> varSchema = new HashMap<>();
                        varSchema.put("type", "string");
                        varSchema.put("description", "Request body variable: " + varName);
                        properties.put(varName, varSchema);
                    }
                }
            }

            schema.put("properties", properties);
            if (!required.isEmpty()) {
                schema.put("required", required);
            }

            return objectMapper.writeValueAsString(schema);
        } catch (Exception e) {
            return "{\"type\": \"object\", \"properties\": {}}";
        }
    }

    /**
     * 查询所有启用的工具配置
     *
     * @return 启用的工具配置列表
     */
    public List<HttpToolConfig> findAllEnabled() {
        try {
            return SqliteDBManager.findAllEnabled();
        } catch (Exception e) {
            logger.error("Failed to find all enabled HTTP tool configs", e);
            throw new RuntimeException("Failed to find all enabled HTTP tool configs", e);
        }
    }
}