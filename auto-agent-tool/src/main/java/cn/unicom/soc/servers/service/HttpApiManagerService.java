package cn.unicom.soc.servers.service;

import cn.unicom.soc.servers.common.annotation.Tool;
import cn.unicom.soc.servers.entity.HttpToolConfig;
import cn.unicom.soc.servers.util.SqliteDBManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * HTTP API 管理服务
 * 提供通过 MCP 协议管理 HTTP API 的功能
 * 包括添加、删除、更新和列出API
 */
@Service
public class HttpApiManagerService implements McpToolManagerServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(HttpApiManagerService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private final HttpToolExecutorService executorService;
    private final HttpToolRegistryService registryService;

    private volatile Set<String> currentToolNames = new HashSet<>();
    // 用于动态更新工具列表的回调提供者
    private CustomToolCallbackProvider toolCallbackProvider;

    public HttpApiManagerService() {
        this.executorService = new HttpToolExecutorService();
        this.registryService = new HttpToolRegistryService();
    }

    /**
     * 添加或更新 HTTP API
     *
     * @param params 参数Map
     * @return 操作结果
     */
    @Tool(name = "addOrUpdateHttpApi", description = "添加或更新单个HTTP API配置", schema = """
            {
              "type": "object",
              "properties": {
                "toolConfig": {
                  "type": "object",
                  "properties": {
                    "toolName": {
                      "type": "string",
                      "description": "API名称（必需）"
                    },
                    "httpMethod": {
                      "type": "string",
                      "description": "HTTP方法（必需）",
                      "enum": ["GET", "POST", "PUT", "DELETE", "PATCH"]
                    },
                    "urlTemplate": {
                      "type": "string",
                      "description": "URL模板（必需）"
                    },
                    "toolDescription": {
                      "type": "string",
                      "description": "API描述"
                    },
                    "headers": {
                      "type": "object",
                      "description": "请求头",
                      "additionalProperties": {
                        "type": "string"
                      }
                    },
                    "requestBodyTemplate": {
                      "type": "string",
                      "description": "请求体模板"
                    },
                    "paramsSchema": {
                      "type": "object",
                      "description": "请求参数JSON Schema定义"
                    },
                    "responseSchema": {
                      "type": "object",
                      "description": "响应结果JSON Schema定义"
                    },
                    "authType": {
                      "type": "string",
                      "description": "认证类型，默认为无认证(none)",
                      "enum": ["none", "bearer", "basic", "apikey"]
                    },
                    "authConfig": {
                      "type": "object",
                      "description": "认证配置"
                    },
                    "timeoutMs": {
                      "type": "integer",
                      "description": "超时时间（毫秒）"
                    }
                  },
                  "required": ["toolName", "httpMethod", "urlTemplate"]
                }
              },
              "required": ["toolConfig"]
            }
            """)
    public String addOrUpdateHttpApi(Map<String, Object> params) {
        try {
            // 从参数Map中获取toolConfig
            Object toolConfigObj = params.get("toolConfig");

            JsonNode jsonNode;

            if (toolConfigObj instanceof Map) {
                // 如果是Map类型（如LinkedHashMap），将其转换为JSON Node
                jsonNode = objectMapper.valueToTree((Map<?, ?>) toolConfigObj);
            } else if (toolConfigObj instanceof String) {
                // 如果是字符串类型，直接解析JSON
                jsonNode = objectMapper.readTree((String) toolConfigObj);
            } else {
                // 其他情况尝试转换为字符串再解析
                String jsonString = objectMapper.writeValueAsString(toolConfigObj);
                jsonNode = objectMapper.readTree(jsonString);
            }

            // 检查是否提供了ID，如果有ID则执行更新操作（部分更新），否则执行新增操作
            HttpToolConfig config;
            if (jsonNode.has("id") && !jsonNode.get("id").isNull()) {
                int id = jsonNode.get("id").asInt();
                
                // 检查是否存在该ID的配置
                HttpToolConfig existingConfig = SqliteDBManager.findById(id);
                if (existingConfig == null) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", false);
                    result.put("error", "HTTP API not found with ID: " + id);
                    
                    logger.warn("Attempt to update non-existent HTTP API with ID: {}", id);
                    
                    return objectMapper.writeValueAsString(result);
                }
                
                // 创建一个新的配置对象，只包含需要更新的字段
                config = parseToolConfig(jsonNode);
                
                // 保留现有配置中的未更新字段
                if (config.getToolName() == null) {
                    config.setToolName(existingConfig.getToolName());
                }
                if (config.getToolDescription() == null) {
                    config.setToolDescription(existingConfig.getToolDescription());
                }
                if (config.getHttpMethod() == null) {
                    config.setHttpMethod(existingConfig.getHttpMethod());
                }
                if (config.getUrlTemplate() == null) {
                    config.setUrlTemplate(existingConfig.getUrlTemplate());
                }
                if (config.getHeaders() == null) {
                    config.setHeaders(existingConfig.getHeaders());
                }
                if (config.getRequestBodyTemplate() == null) {
                    config.setRequestBodyTemplate(existingConfig.getRequestBodyTemplate());
                }
                if (config.getParamsSchema() == null) {
                    config.setParamsSchema(existingConfig.getParamsSchema());
                }
                if (config.getResponseSchema() == null) {
                    config.setResponseSchema(existingConfig.getResponseSchema());
                }
                if (config.getAuthType() == null) {
                    config.setAuthType(existingConfig.getAuthType());
                }
                if (config.getAuthConfig() == null) {
                    config.setAuthConfig(existingConfig.getAuthConfig());
                }
                if (config.getTimeoutMs() == null) {
                    config.setTimeoutMs(existingConfig.getTimeoutMs());
                }
                if (config.getEnabled() == null) {
                    config.setEnabled(existingConfig.getEnabled());
                }

                config.setId(id);
            } else {
                // 新增操作，使用原有逻辑
                config = parseToolConfig(jsonNode);
            }

            // 保存到数据库
            int savedId = registryService.saveOrUpdate(config);

            // 自动注册到MCP工具
            if (toolCallbackProvider != null && config.getEnabled() != null && config.getEnabled()) {
                registryService.registerHttpToolToMcp(config, toolCallbackProvider, currentToolNames, executorService);
            }

            logger.info("Added/Updated HTTP API: {} with ID: {}", config.getToolName(), savedId);

            // 返回成功消息
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "HTTP API added/updated successfully");
            result.put("toolId", savedId);
            result.put("toolName", config.getToolName());

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            logger.error("Failed to add/update HTTP API", e);

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());

            try {
                return objectMapper.writeValueAsString(result);
            } catch (JsonProcessingException jsonException) {
                return "{\"success\": false, \"error\": \"Internal error occurred\"}";
            }
        }
    }

    /**
     * 删除 HTTP API
     *
     * @param params 参数Map
     * @return 操作结果
     */
    @Tool(name = "deleteHttpApi", description = "删除HTTP API配置", schema = """
            {
              "type": "object",
              "properties": {
                "id": {
                  "type": "integer",
                  "description": "要删除的API ID"
                }
              },
              "required": ["id"]
            }
            """)
    public String deleteHttpApi(Map<String, Object> params) {
        try {
            int id = Integer.parseInt(params.get("id").toString());

            // 先查询获取工具名称，以便从MCP中移除
            HttpToolConfig existingConfig = SqliteDBManager.findById(id);
            String toolName = existingConfig != null ? existingConfig.getToolName() : null;

            // 从数据库删除
            boolean deleted = registryService.deleteById(id);

            if (deleted) {
                logger.info("Deleted HTTP API with ID: {}", id);

                // 从MCP工具列表中移除
                if (toolName != null && toolCallbackProvider != null) {
                    toolCallbackProvider.removeTool(toolName);
                    currentToolNames.remove(toolName);
                    logger.info("Removed HTTP API from MCP: {}", toolName);
                }

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "HTTP API deleted successfully");
                result.put("id", id);

                return objectMapper.writeValueAsString(result);
            } else {
                logger.warn("HTTP API not found for deletion with ID: {}", id);

                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "HTTP API not found");
                result.put("id", id);

                return objectMapper.writeValueAsString(result);
            }
        } catch (NumberFormatException e) {
            logger.error("Invalid ID format for deletion: {}", params, e);

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "Invalid ID format");

            try {
                return objectMapper.writeValueAsString(result);
            } catch (JsonProcessingException jsonException) {
                return "{\"success\": false, \"error\": \"Internal error occurred\"}";
            }
        } catch (Exception e) {
            logger.error("Failed to delete HTTP API with params: {}", params, e);

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());

            try {
                return objectMapper.writeValueAsString(result);
            } catch (JsonProcessingException jsonException) {
                return "{\"success\": false, \"error\": \"Internal error occurred\"}";
            }
        }
    }

    /**
     * 列出所有 HTTP API
     * 
     * @param params 参数Map
     * @return API列表
     */
    @Tool(name = "listHttpApis", description = "列出所有HTTP API配置", schema = """
            {
              "type": "object",
              "properties": {},
              "required": [],
              "description": "获取所有已注册的HTTP API列表"
            }
            """)
    public String listHttpApis(Map<String, Object> params) {
        try {
            List<HttpToolConfig> configs = registryService.findAll();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("apis", configs);
            result.put("count", configs.size());

            logger.info("Listed {} HTTP APIs", configs.size());

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            logger.error("Failed to list HTTP APIs", e);

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());

            try {
                return objectMapper.writeValueAsString(result);
            } catch (JsonProcessingException jsonException) {
                return "{\"success\": false, \"error\": \"Internal error occurred\"}";
            }
        }
    }

    /**
     * 批量添加或更新 HTTP API
     * 
     * @param params 参数Map
     * @return 操作结果
     */
    @Tool(name = "batchAddHttpApis", description = "批量添加HTTP API配置", schema = """
            {
              "type": "object",
              "properties": {
                "toolsConfig": {
                  "type": "object",
                  "properties": {
                    "tools": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "properties": {
                          "toolName": {
                            "type": "string",
                            "description": "API名称（必需）"
                          },
                          "httpMethod": {
                            "type": "string",
                            "description": "HTTP方法（必需）",
                            "enum": ["GET", "POST", "PUT", "DELETE", "PATCH"]
                          },
                          "urlTemplate": {
                            "type": "string",
                            "description": "URL模板（必需）"
                          },
                          "toolDescription": {
                            "type": "string",
                            "description": "API描述"
                          },
                          "headers": {
                            "type": "object",
                            "description": "请求头",
                            "additionalProperties": {
                              "type": "string"
                            }
                          },
                          "requestBodyTemplate": {
                            "type": "string",
                            "description": "请求体模板"
                          },
                          "paramsSchema": {
                            "type": "object",
                            "description": "请求参数JSON Schema定义"
                          },
                          "responseSchema": {
                            "type": "object",
                            "description": "响应结果JSON Schema定义"
                          },
                          "authType": {
                            "type": "string",
                            "description": "认证类型，默认为无认证(none)",
                            "enum": ["none", "bearer", "basic", "apikey"]
                          },
                          "authConfig": {
                            "type": "object",
                            "description": "认证配置"
                          },
                          "timeoutMs": {
                            "type": "integer",
                            "description": "超时时间（毫秒）"
                          }
                        },
                        "required": ["toolName", "httpMethod", "urlTemplate"]
                      }
                    }
                  },
                  "required": ["tools"]
                }
              },
              "required": ["toolsConfig"]
            }
            """)
    public String batchAddHttpApis(Map<String, Object> params) {
        try {
            // 从参数Map中获取toolsConfig
            Object toolsConfigObj = params.get("toolsConfig");

            JsonNode jsonNode;

            if (toolsConfigObj instanceof Map) {
                jsonNode = objectMapper.valueToTree((Map<?, ?>) toolsConfigObj);
            } else if (toolsConfigObj instanceof String) {
                jsonNode = objectMapper.readTree((String) toolsConfigObj);
            } else {
                String jsonString = objectMapper.writeValueAsString(toolsConfigObj);
                jsonNode = objectMapper.readTree(jsonString);
            }

            // 检查是否有 "tools" 数组节点
            JsonNode toolsNode = jsonNode.get("tools");
            if (toolsNode == null || !toolsNode.isArray()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", "Invalid tools configuration: missing 'tools' array");
                return objectMapper.writeValueAsString(result);
            }

            List<Map<String, Object>> results = new ArrayList<>();
            int successCount = 0;
            int failCount = 0;

            for (JsonNode toolNode : toolsNode) {
                try {
                    HttpToolConfig config = parseToolConfig(toolNode);
                    // 设置ID为null，确保是添加新记录
                    config.setId(null);
                    int id = SqliteDBManager.saveOrUpdate(config);

                    // 自动注册到MCP工具
                    if (toolCallbackProvider != null && config.getEnabled() != null && config.getEnabled()) {
                        registryService.registerHttpToolToMcp(config, toolCallbackProvider, currentToolNames, executorService);
                    }

                    Map<String, Object> toolResult = new HashMap<>();
                    toolResult.put("toolName", config.getToolName());
                    toolResult.put("id", id);
                    toolResult.put("success", true);
                    results.add(toolResult);

                    successCount++;
                } catch (Exception e) {
                    Map<String, Object> toolResult = new HashMap<>();
                    toolResult.put("error", e.getMessage());
                    toolResult.put("success", false);
                    results.add(toolResult);

                    failCount++;
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Batch add operation completed");
            result.put("results", results);
            result.put("summary", Map.of(
                    "total", results.size(),
                    "success", successCount,
                    "failed", failCount
            ));

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            logger.error("Failed to batch add HTTP APIs", e);

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());

            try {
                return objectMapper.writeValueAsString(result);
            } catch (JsonProcessingException jsonException) {
                return "{\"success\": false, \"error\": \"Internal error occurred\"}";
            }
        }
    }

    @Tool(name = "batchUpdateHttpApis", description = "批量更新HTTP API配置", schema = """
            {
              "type": "object",
              "properties": {
                "toolsConfig": {
                  "type": "object",
                  "properties": {
                    "tools": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "properties": {
                          "id": {
                            "type": "integer",
                            "description": "API ID（必需）"
                          },
                          "toolName": {
                            "type": "string",
                            "description": "API名称"
                          },
                          "httpMethod": {
                            "type": "string",
                            "description": "HTTP方法",
                            "enum": ["GET", "POST", "PUT", "DELETE", "PATCH"]
                          },
                          "urlTemplate": {
                            "type": "string",
                            "description": "URL模板"
                          },
                          "toolDescription": {
                            "type": "string",
                            "description": "API描述"
                          },
                          "headers": {
                            "type": "object",
                            "description": "请求头",
                            "additionalProperties": {
                              "type": "string"
                            }
                          },
                          "requestBodyTemplate": {
                            "type": "string",
                            "description": "请求体模板"
                          },
                          "paramsSchema": {
                            "type": "object",
                            "description": "请求参数JSON Schema定义"
                          },
                          "responseSchema": {
                            "type": "object",
                            "description": "响应结果JSON Schema定义"
                          },
                          "authType": {
                            "type": "string",
                            "description": "认证类型",
                            "enum": ["none", "bearer", "basic", "apikey"]
                          },
                          "authConfig": {
                            "type": "object",
                            "description": "认证配置"
                          },
                          "timeoutMs": {
                            "type": "integer",
                            "description": "超时时间（毫秒）"
                          }
                        },
                        "required": ["id"]
                      }
                    }
                  },
                  "required": ["tools"]
                }
              },
              "required": ["toolsConfig"]
            }
            """)
    public String batchUpdateHttpApis(Map<String, Object> params) {
        try {
            // 从参数Map中获取toolsConfig
            Object toolsConfigObj = params.get("toolsConfig");

            JsonNode jsonNode;

            if (toolsConfigObj instanceof Map) {
                jsonNode = objectMapper.valueToTree((Map<?, ?>) toolsConfigObj);
            } else if (toolsConfigObj instanceof String) {
                jsonNode = objectMapper.readTree((String) toolsConfigObj);
            } else {
                String jsonString = objectMapper.writeValueAsString(toolsConfigObj);
                jsonNode = objectMapper.readTree(jsonString);
            }

            // 检查是否有 "tools" 数组节点
            JsonNode toolsNode = jsonNode.get("tools");
            if (toolsNode == null || !toolsNode.isArray()) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", "Invalid tools configuration: missing 'tools' array");
                return objectMapper.writeValueAsString(result);
            }

            List<Map<String, Object>> results = new ArrayList<>();
            int successCount = 0;
            int failCount = 0;

            for (JsonNode toolNode : toolsNode) {
                try {
                    // 首先检查是否包含ID
                    JsonNode idNode = toolNode.get("id");
                    if (idNode == null || idNode.isNull()) {
                        Map<String, Object> toolResult = new HashMap<>();
                        toolResult.put("error", "ID is required for update operation");
                        toolResult.put("success", false);
                        results.add(toolResult);
                        failCount++;
                        continue;
                    }
                    
                    int id = idNode.asInt();
                    
                    // 检查是否存在该ID的配置
                    HttpToolConfig existingConfig = SqliteDBManager.findById(id);
                    if (existingConfig == null) {
                        Map<String, Object> toolResult = new HashMap<>();
                        toolResult.put("error", "HTTP API not found with ID: " + id);
                        toolResult.put("success", false);
                        results.add(toolResult);
                        failCount++;
                        continue;
                    }
                    
                    // 创建一个新的配置对象，只包含需要更新的字段
                    HttpToolConfig updateConfig = parseToolConfig(toolNode);

                    // 保留现有配置中的未更新字段
                    if (updateConfig.getToolName() == null) {
                        updateConfig.setToolName(existingConfig.getToolName());
                    }
                    if (updateConfig.getToolDescription() == null) {
                        updateConfig.setToolDescription(existingConfig.getToolDescription());
                    }
                    if (updateConfig.getHttpMethod() == null) {
                        updateConfig.setHttpMethod(existingConfig.getHttpMethod());
                    }
                    if (updateConfig.getUrlTemplate() == null) {
                        updateConfig.setUrlTemplate(existingConfig.getUrlTemplate());
                    }
                    if (updateConfig.getHeaders() == null) {
                        updateConfig.setHeaders(existingConfig.getHeaders());
                    }
                    if (updateConfig.getRequestBodyTemplate() == null) {
                        updateConfig.setRequestBodyTemplate(existingConfig.getRequestBodyTemplate());
                    }
                    if (updateConfig.getParamsSchema() == null) {
                        updateConfig.setParamsSchema(existingConfig.getParamsSchema());
                    }
                    if (updateConfig.getResponseSchema() == null) {
                        updateConfig.setResponseSchema(existingConfig.getResponseSchema());
                    }
                    if (updateConfig.getAuthType() == null) {
                        updateConfig.setAuthType(existingConfig.getAuthType());
                    }
                    if (updateConfig.getAuthConfig() == null) {
                        updateConfig.setAuthConfig(existingConfig.getAuthConfig());
                    }
                    if (updateConfig.getTimeoutMs() == null) {
                        updateConfig.setTimeoutMs(existingConfig.getTimeoutMs());
                    }
                    if (updateConfig.getEnabled() == null) {
                        updateConfig.setEnabled(existingConfig.getEnabled());
                    }

                    // 设置ID以确保更新现有记录
                    updateConfig.setId(id);
                    int updatedId = SqliteDBManager.saveOrUpdate(updateConfig);

                    // 自动注册到MCP工具
                    if (toolCallbackProvider != null && updateConfig.getEnabled() != null && updateConfig.getEnabled()) {
                        registryService.registerHttpToolToMcp(updateConfig, toolCallbackProvider, currentToolNames, executorService);
                    }

                    Map<String, Object> toolResult = new HashMap<>();
                    toolResult.put("toolName", updateConfig.getToolName());
                    toolResult.put("id", updatedId);
                    toolResult.put("success", true);
                    results.add(toolResult);

                    successCount++;
                } catch (Exception e) {
                    Map<String, Object> toolResult = new HashMap<>();
                    toolResult.put("error", e.getMessage());
                    toolResult.put("success", false);
                    results.add(toolResult);

                    failCount++;
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Batch update operation completed");
            result.put("results", results);
            result.put("summary", Map.of(
                    "total", results.size(),
                    "success", successCount,
                    "failed", failCount
            ));

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            logger.error("Failed to batch update HTTP APIs", e);

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());

            try {
                return objectMapper.writeValueAsString(result);
            } catch (JsonProcessingException jsonException) {
                return "{\"success\": false, \"error\": \"Internal error occurred\"}";
            }
        }
    }

    /**
     * 添加新的 HTTP API
     * 
     * @param params 参数Map，包含API配置信息
     * @return 操作结果
     */
    @Tool(name = "addHttpApi", description = "添加新的HTTP API配置", schema = """
            {
              "type": "object",
              "properties": {
                "toolName": {
                  "type": "string",
                  "description": "API名称（必需）"
                },
                "httpMethod": {
                  "type": "string",
                  "description": "HTTP方法（必需）",
                  "enum": ["GET", "POST", "PUT", "DELETE", "PATCH"]
                },
                "urlTemplate": {
                  "type": "string",
                  "description": "URL模板（必需）"
                },
                "toolDescription": {
                  "type": "string",
                  "description": "API描述"
                },
                "headers": {
                  "type": "object",
                  "description": "请求头",
                  "additionalProperties": {
                    "type": "string"
                  }
                },
                "requestBodyTemplate": {
                  "type": "string",
                  "description": "请求体模板"
                },
                "paramsSchema": {
                  "type": "object",
                  "description": "请求参数JSON Schema定义"
                },
                "responseSchema": {
                  "type": "object",
                  "description": "响应结果JSON Schema定义"
                },
                "authType": {
                  "type": "string",
                  "description": "认证类型，默认为无认证(none)",
                  "enum": ["none", "bearer", "basic", "apikey"]
                },
                "authConfig": {
                  "type": "object",
 "description": "认证配置"
                },
                "timeoutMs": {
                  "type": "integer",
                  "description": "超时时间（毫秒）"
                },
                "enabled": {
                  "type": "boolean",
                  "description": "是否启用，默认为true"
                }
              },
              "required": ["toolName", "httpMethod", "urlTemplate"]
            }
            """)
    public String addHttpApi(Map<String, Object> params) {
        try {
            // 构建JsonNode对象
            JsonNode toolNode = objectMapper.valueToTree(params);
            HttpToolConfig config = parseToolConfig(toolNode);

            // 设置ID为null，确保是插入新记录
            config.setId(null);

            int id = SqliteDBManager.saveOrUpdate(config);

            // 自动注册到MCP工具
            if (toolCallbackProvider != null && config.getEnabled() != null && config.getEnabled()) {
                registryService.registerHttpToolToMcp(config, toolCallbackProvider, currentToolNames, executorService);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "HTTP API added successfully");
            result.put("id", id);
            result.put("toolName", config.getToolName());

            logger.info("Added new HTTP API with ID: {}, Name: {}", id, config.getToolName());

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            logger.error("Failed to add HTTP API with params: {}", params, e);

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());

            try {
                return objectMapper.writeValueAsString(result);
            } catch (JsonProcessingException jsonException) {
                return "{\"success\": false, \"error\": \"Internal error occurred\"}";
            }
        }
    }

    /**
     * 更新现有的 HTTP API
     * 
     * @param params 参数Map，包含API配置信息和ID
     * @return 操作结果
     */
    @Tool(name = "updateHttpApi", description = "更新现有的HTTP API配置", schema = """
            {
              "type": "object",
              "properties": {
                "id": {
                  "type": "integer",
                  "description": "API ID（必需）"
                },
                "toolName": {
                  "type": "string",
                  "description": "API名称"
                },
                "httpMethod": {
                  "type": "string",
                  "description": "HTTP方法",
                  "enum": ["GET", "POST", "PUT", "DELETE", "PATCH"]
                },
                "urlTemplate": {
                  "type": "string",
                  "description": "URL模板"
                },
                "toolDescription": {
                  "type": "string",
                  "description": "API描述"
                },
                "headers": {
                  "type": "object",
                  "description": "请求头",
                  "additionalProperties": {
                    "type": "string"
                  }
                },
                "requestBodyTemplate": {
                  "type": "string",
                  "description": "请求体模板"
                },
                "paramsSchema": {
                  "type": "object",
                  "description": "请求参数JSON Schema定义"
                },
                "responseSchema": {
                  "type": "object",
                  "description": "响应结果JSON Schema定义"
                },
                "authType": {
                  "type": "string",
                  "description": "认证类型",
                  "enum": ["none", "bearer", "basic", "apikey"]
                },
                "authConfig": {
                  "type": "object",
                  "description": "认证配置"
                },
                "timeoutMs": {
                  "type": "integer",
                  "description": "超时时间（毫秒）"
                },
                "enabled": {
                  "type": "boolean",
                  "description": "是否启用"
                }
              },
              "required": ["id"]
            }
            """)
    public String updateHttpApi(Map<String, Object> params) {
        try {
            // 首先验证ID是否存在
            int id = Integer.parseInt(params.get("id").toString());
            
            // 检查是否存在该ID的配置
            HttpToolConfig existingConfig = SqliteDBManager.findById(id);
            if (existingConfig == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", "HTTP API not found with ID: " + id);
                
                logger.warn("Attempt to update non-existent HTTP API with ID: {}", id);
                
                return objectMapper.writeValueAsString(result);
            }
            
            // 构建JsonNode对象
            JsonNode toolNode = objectMapper.valueToTree(params);
            
            // 创建一个新的配置对象，只包含需要更新的字段
            HttpToolConfig updateConfig = parseToolConfig(toolNode);
            
            // 保留现有配置中的未更新字段
            if (updateConfig.getToolName() == null) {
                updateConfig.setToolName(existingConfig.getToolName());
            }
            if (updateConfig.getToolDescription() == null) {
                updateConfig.setToolDescription(existingConfig.getToolDescription());
            }
            if (updateConfig.getHttpMethod() == null) {
                updateConfig.setHttpMethod(existingConfig.getHttpMethod());
            }
            if (updateConfig.getUrlTemplate() == null) {
                updateConfig.setUrlTemplate(existingConfig.getUrlTemplate());
            }
            if (updateConfig.getHeaders() == null) {
                updateConfig.setHeaders(existingConfig.getHeaders());
            }
            if (updateConfig.getRequestBodyTemplate() == null) {
                updateConfig.setRequestBodyTemplate(existingConfig.getRequestBodyTemplate());
            }
            if (updateConfig.getParamsSchema() == null) {
                updateConfig.setParamsSchema(existingConfig.getParamsSchema());
            }
            if (updateConfig.getResponseSchema() == null) {
                updateConfig.setResponseSchema(existingConfig.getResponseSchema());
            }
            if (updateConfig.getAuthType() == null) {
                updateConfig.setAuthType(existingConfig.getAuthType());
            }
            if (updateConfig.getAuthConfig() == null) {
                updateConfig.setAuthConfig(existingConfig.getAuthConfig());
            }
            if (updateConfig.getTimeoutMs() == null) {
                updateConfig.setTimeoutMs(existingConfig.getTimeoutMs());
            }
            if (updateConfig.getEnabled() == null) {
                updateConfig.setEnabled(existingConfig.getEnabled());
            }

            // 设置ID以确保更新现有记录
            updateConfig.setId(id);

            int updatedId = SqliteDBManager.saveOrUpdate(updateConfig);

            // 自动注册到MCP工具
            if (toolCallbackProvider != null && updateConfig.getEnabled() != null && updateConfig.getEnabled()) {
                registryService.registerHttpToolToMcp(updateConfig, toolCallbackProvider, currentToolNames, executorService);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "HTTP API updated successfully");
            result.put("id", updatedId);
            result.put("toolName", updateConfig.getToolName());

            logger.info("Updated HTTP API with ID: {}, Name: {}", updatedId, updateConfig.getToolName());

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            logger.error("Failed to update HTTP API with params: {}", params, e);

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());

            try {
                return objectMapper.writeValueAsString(result);
            } catch (JsonProcessingException jsonException) {
                return "{\"success\": false, \"error\": \"Internal error occurred\"}";
            }
        }
    }

    /**
     * 解析工具配置
     */
    private HttpToolConfig parseToolConfig(JsonNode node) throws Exception {
        HttpToolConfig config = new HttpToolConfig();
        
        // toolName: 只有在存在且不为null时才设置
        if (node.has("toolName") && !node.get("toolName").isNull()) {
            config.setToolName(node.get("toolName").asText());
        }
        
        // toolDescription: 只有在存在且不为null时才设置
        if (node.has("toolDescription") && !node.get("toolDescription").isNull()) {
            config.setToolDescription(node.get("toolDescription").asText());
        }
        
        // httpMethod: 只有在存在且不为null时才设置
        if (node.has("httpMethod") && !node.get("httpMethod").isNull()) {
            config.setHttpMethod(node.get("httpMethod").asText());
        }
        
        // urlTemplate: 只有在存在且不为null时才设置
        if (node.has("urlTemplate") && !node.get("urlTemplate").isNull()) {
            config.setUrlTemplate(node.get("urlTemplate").asText());
        }
        
        // authType: 只有在存在且不为null时才设置，否则使用默认值
        if (node.has("authType") && !node.get("authType").isNull()) {
            config.setAuthType(node.get("authType").asText());
        }
        
        // headers: JSON 对象转为字符串
        if (node.has("headers") && !node.get("headers").isNull()) {
            config.setHeaders(node.get("headers").toString());
        }

        // requestBodyTemplate
        if (node.has("requestBodyTemplate") && !node.get("requestBodyTemplate").isNull()) {
            config.setRequestBodyTemplate(node.get("requestBodyTemplate").toString());
        }

        // paramsSchema: JSON Schema 对象转为字符串
        if (node.has("paramsSchema") && !node.get("paramsSchema").isNull()) {
            config.setParamsSchema(node.get("paramsSchema").toString());
        }

        // responseSchema: JSON Schema 对象转为字符串
        if (node.has("responseSchema") && !node.get("responseSchema").isNull()) {
            config.setResponseSchema(node.get("responseSchema").toString());
        }

        // authConfig
        if (node.has("authConfig") && !node.get("authConfig").isNull()) {
            config.setAuthConfig(node.get("authConfig").toString());
        }

        // timeoutMs
        if (node.has("timeoutMs")) {
            config.setTimeoutMs(node.get("timeoutMs").asInt(30000));
        }

        // enabled
        if (node.has("enabled")) {
            config.setEnabled(node.get("enabled").asBoolean());
        }

        // 如果未提供请求schema，根据URL模板和请求体模板自动生成
        if (config.getParamsSchema() == null || config.getParamsSchema().trim().isEmpty()) {
            config.setParamsSchema(HttpToolRegistryService.generateDefaultRequestSchema(config));
        }

        return config;
    }

    private String getString(JsonNode node, String field, boolean required) {
        if (!node.has(field) || node.get(field).isNull()) {
            if (required) {
                throw new IllegalArgumentException("Missing required field: " + field);
            }
            return null;
        }
        return node.get(field).asText();
    }

    private String getStringWithDefault(JsonNode node, String field, String defaultValue) {
        if (!node.has(field) || node.get(field).isNull()) {
            return defaultValue;
        }
        return node.get(field).asText();
    }

    /**
     * 启用 HTTP API
     * 
     * @param params 参数Map
     * @return 操作结果
     */
    @Tool(name = "enableHttpApi", description = "启用HTTP API", schema = """
            {
              "type": "object",
              "properties": {
                "id": {
                  "type": "integer",
                  "description": "要启用的API ID"
                }
              },
              "required": ["id"]
            }
            """)
    public String enableHttpApi(Map<String, Object> params) {
        try {
            int id = Integer.parseInt(params.get("id").toString());

            // 从数据库更新启用状态
            boolean updated = registryService.updateEnabledStatusById(id, true);

            if (updated) {
                logger.info("Enabled HTTP API with ID: {}", id);

                // 自动注册到MCP工具
                HttpToolConfig config = SqliteDBManager.findById(id);
                if (config != null && toolCallbackProvider != null) {
                    registryService.registerHttpToolToMcp(config, toolCallbackProvider, currentToolNames, executorService);
                }

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "HTTP API enabled successfully");
                result.put("id", id);

                return objectMapper.writeValueAsString(result);
            } else {
                logger.warn("HTTP API not found for enabling with ID: {}", id);

                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "HTTP API not found");
                result.put("id", id);

                return objectMapper.writeValueAsString(result);
            }
        } catch (NumberFormatException e) {
            logger.error("Invalid ID format for enabling: {}", params, e);

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "Invalid ID format");

            try {
                return objectMapper.writeValueAsString(result);
            } catch (JsonProcessingException jsonException) {
                return "{\"success\": false, \"error\": \"Internal error occurred\"}";
            }
        } catch (Exception e) {
            logger.error("Failed to enable HTTP API with params: {}", params, e);

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());

            try {
                return objectMapper.writeValueAsString(result);
            } catch (JsonProcessingException jsonException) {
                return "{\"success\": false, \"error\": \"Internal error occurred\"}";
            }
        }
    }

    /**
     * 禁用 HTTP API
     * 
     * @param params 参数Map
     * @return 操作结果
     */
    @Tool(name = "disableHttpApi", description = "禁用HTTP API", schema = """
            {
              "type": "object",
              "properties": {
                "id": {
                  "type": "integer",
                  "description": "要禁用的API ID"
                }
              },
              "required": ["id"]
            }
            """)
    public String disableHttpApi(Map<String, Object> params) {
        try {
            int id = Integer.parseInt(params.get("id").toString());

            // 先查询获取工具名称，以便从MCP中移除
            HttpToolConfig existingConfig = SqliteDBManager.findById(id);
            String toolName = existingConfig != null ? existingConfig.getToolName() : null;

            // 从数据库更新启用状态
            boolean updated = registryService.updateEnabledStatusById(id, false);

            if (updated) {
                logger.info("Disabled HTTP API with ID: {}", id);

                // 从MCP工具列表中移除
                if (toolName != null && toolCallbackProvider != null) {
                    toolCallbackProvider.removeTool(toolName);
                    currentToolNames.remove(toolName);
                    logger.info("Removed disabled HTTP API from MCP: {}", toolName);
                }

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "HTTP API disabled successfully");
                result.put("id", id);

                return objectMapper.writeValueAsString(result);
            } else {
                logger.warn("HTTP API not found for disabling with ID: {}", id);

                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "HTTP API not found");
                result.put("id", id);

                return objectMapper.writeValueAsString(result);
            }
        } catch (NumberFormatException e) {
            logger.error("Invalid ID format for disabling: {}", params, e);

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "Invalid ID format");

            try {
                return objectMapper.writeValueAsString(result);
            } catch (JsonProcessingException jsonException) {
                return "{\"success\": false, \"error\": \"Internal error occurred\"}";
            }
        } catch (Exception e) {
            logger.error("Failed to disable HTTP API with params: {}", params, e);

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());

            try {
                return objectMapper.writeValueAsString(result);
            } catch (JsonProcessingException jsonException) {
                return "{\"success\": false, \"error\": \"Internal error occurred\"}";
            }
        }
    }

    public void setToolCallbackProvider(CustomToolCallbackProvider provider) {
        this.toolCallbackProvider = provider;
    }

    /**
     * 刷新MCP工具列表，重新加载所有启用的HTTP API
     *
     * @return 操作结果
     */
    public String refreshMcpTools() {
        try {
            logger.info("Refreshing MCP tools...");

            // 从MCP中移除所有当前已注册的HTTP工具
            if (toolCallbackProvider != null) {
                for (String toolName : new ArrayList<>(currentToolNames)) {
                    toolCallbackProvider.removeTool(toolName);
                }
            }
            currentToolNames.clear();

            // 重新加载所有启用的HTTP工具
            List<HttpToolConfig> allConfigs = registryService.findAllEnabled();
            int addedCount = 0;
            for (HttpToolConfig config : allConfigs) {
                if (toolCallbackProvider != null) {
                    registryService.registerHttpToolToMcp(config, toolCallbackProvider, currentToolNames, executorService);
                    addedCount++;
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "MCP tools refreshed successfully");
            result.put("total", allConfigs.size());
            result.put("registered", addedCount);

            logger.info("Refreshed MCP tools. Total: {}, Registered: {}", allConfigs.size(), addedCount);

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            logger.error("Failed to refresh MCP tools", e);

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());

            try {
                return objectMapper.writeValueAsString(result);
            } catch (JsonProcessingException jsonException) {
                return "{\"success\": false, \"error\": \"Internal error occurred\"}";
            }
        }
    }

    /**
     * 定时扫描数据库，加载新增或更新的API
     */
    @Scheduled(fixedDelay = 30000) // 每30秒扫描一次
    public void scanAndLoadApis() {
        try {
            logger.info("Scanning database for new or updated APIs...");

            // 获取当前数据库中的所有API
            List<HttpToolConfig> allConfigs = registryService.findAllEnabled();
            Set<String> dbApiNames = new HashSet<>();

            // 遍历数据库中的API
            for (HttpToolConfig config : allConfigs) {
                dbApiNames.add(config.getToolName());

                // 如果API不在当前列表中，则添加它
                if (!currentToolNames.contains(config.getToolName())) {
                    try {

                        logger.info("Adding new API to MCP: {}", config);
                        GenericToolCallback callback = new GenericToolCallback(
                                config.getToolName(),
                                config.getToolDescription() != null ? config.getToolDescription() : "HTTP API tool: " + config.getToolName(),
                                config.getParamsSchema() != null ? config.getParamsSchema() : HttpToolRegistryService.generateDefaultRequestSchema(config),
                                (toolInput) -> {
                                    try {
                                        return executorService.executeToolCall(config, toolInput);
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

                        // 添加到API提供者
                        if (toolCallbackProvider != null) {
                            toolCallbackProvider.addTools(callback);

                            System.out.println(toolCallbackProvider.getToolCallbacks().length);
                            logger.info("Added new API to MCP: {}", config.getToolName());
                        } else {
                            logger.warn("API callback provider is not set, cannot add API: {}", config.getToolName());
                        }

                        currentToolNames.add(config.getToolName());
                    } catch (Exception e) {
                        logger.error("Failed to create callback for API: {}", config.getToolName(), e);
                    }
                }
            }

            // 检查是否有API被删除（在内存中但在数据库中不存在）
            Set<String> apisToRemove = new HashSet<>();
            for (String apiName : currentToolNames) {
                if (!dbApiNames.contains(apiName)) {
                    apisToRemove.add(apiName);
                }
            }

            // 从当前列表中移除已删除的API
            for (String apiName : apisToRemove) {
                currentToolNames.remove(apiName);
                logger.info("Removed API from tracking: {}", apiName);
            }

            logger.info("Scan completed. Current tracked APIs: {}", currentToolNames.size());

        } catch (Exception e) {
            logger.error("Error during scanning and loading APIs", e);
        }
    }
    

}