package cn.unicom.soc.servers.service;

import cn.unicom.soc.servers.entity.ToolCallLogEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.HashMap;
import java.util.Map;

/**
 * MCP工具调用拦截器
 * 拦截所有工具调用结果，检查是否需要进行分块处理
 */
public class McpToolCallInterceptor implements ToolCallback {

    private final ToolCallback originalCallback;
    private final ToolDefinition definition;
    private final AutoChunkInterceptor chunkInterceptor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static MonitorService monitorService;

    public static void setMonitorService(MonitorService service) {
        monitorService = service;
    }

    public McpToolCallInterceptor(ToolCallback originalCallback, AutoChunkInterceptor chunkInterceptor) {
        this.originalCallback = originalCallback;
        this.definition = originalCallback.getToolDefinition();
        this.chunkInterceptor = chunkInterceptor;
    }
    
    @Override
    public ToolDefinition getToolDefinition() {
        return definition;
    }
    
    @Override
    public String call(String toolInput) {
        long startTime = System.currentTimeMillis();
        String status = "success";
        String result = null;
        String errorMessage = null;

        try {
            // 调用原始工具
            result = originalCallback.call(toolInput);

            try {
                // 尝试解析输入，从中提取用户和会话信息
                String userId = extractUserId(toolInput);
                String sessionId = extractSessionId(toolInput);

                // 尝试解析结果，如果是JSON格式则提取关键字符串字段进行分块检查
                Object parsedResult = parseResult(result);

                // 使用分块拦截器处理结果
                Object processedResult = chunkInterceptor.processResult(parsedResult, userId, sessionId);

                // 如果结果被分块处理，返回处理后的结果
                if (processedResult != parsedResult) {
                    result = serializeResult(processedResult);
                }
            } catch (Exception e) {
                // 如果处理过程中出现异常，仍然返回原始结果
                errorMessage = e.getMessage();
            }

            return result;
        } catch (Exception e) {
            status = "error";
            errorMessage = e.getMessage();
            throw e;
        } finally {
            // 记录调用日志
            if (monitorService != null) {
                try {
                    ToolCallLogEntity log = new ToolCallLogEntity();
                    log.setToolId(definition.name());
                    log.setToolName(definition.name());
                    log.setToolType(definition.name().startsWith("http") ? "http" : "mcp");
                    log.setStatus(status);
                    log.setDurationMs((int) (System.currentTimeMillis() - startTime));
                    log.setRequestParams(toolInput.length() > 2000 ? toolInput.substring(0, 2000) : toolInput);
                    if (result != null) {
                        log.setResponseSummary(result.length() > 2000 ? result.substring(0, 2000) : result);
                    }
                    log.setErrorMessage(errorMessage);
                    monitorService.saveCallLog(log);
                } catch (Exception ex) {
                    // 日志记录失败不应影响主流程
                }
            }
        }
    }
    
    /**
     * 从工具输入中提取用户ID
     */
    private String extractUserId(String toolInput) {
        try {
            Map<String, Object> inputMap = objectMapper.readValue(toolInput, Map.class);
            Object userIdObj = inputMap.get("userId");
            if (userIdObj != null) {
                return userIdObj.toString();
            }
            userIdObj = inputMap.get("user_id");
            if (userIdObj != null) {
                return userIdObj.toString();
            }
            // 如果没有找到用户ID，使用默认值
            return "default_user";
        } catch (Exception e) {
            return "default_user";
        }
    }
    
    /**
     * 从工具输入中提取会话ID
     */
    private String extractSessionId(String toolInput) {
        try {
            Map<String, Object> inputMap = objectMapper.readValue(toolInput, Map.class);
            Object sessionIdObj = inputMap.get("sessionId");
            if (sessionIdObj != null) {
                return sessionIdObj.toString();
            }
            sessionIdObj = inputMap.get("session_id");
            if (sessionIdObj != null) {
                return sessionIdObj.toString();
            }
            // 如果没有找到会话ID，使用默认值
            return "default_session";
        } catch (Exception e) {
            return "default_session";
        }
    }
    
    /**
     * 解析工具调用结果
     */
    private Object parseResult(String result) {
        try {
            // 尝试解析为JSON
            Object parsed = objectMapper.readValue(result, Object.class);
            
            // 如果是字符串类型的JSON值，直接返回
            if (parsed instanceof String) {
                return parsed;
            }
            
            // 如果是Map或其他复杂类型，返回原始解析结果
            return parsed;
        } catch (JsonProcessingException e) {
            // 如果不是有效的JSON，直接返回原始字符串
            return result;
        }
    }
    
    /**
     * 序列化处理后的结果
     */
    private String serializeResult(Object result) {
        try {
            if (result instanceof String) {
                return (String) result;
            } else {
                return objectMapper.writeValueAsString(result);
            }
        } catch (JsonProcessingException e) {
            // 如果序列化失败，返回错误信息
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "Failed to serialize chunked result");
            errorResult.put("original_result", result.toString());
            
            try {
                return objectMapper.writeValueAsString(errorResult);
            } catch (JsonProcessingException ex) {
                return "{\"success\": false, \"error\": \"Critical serialization error\"}";
            }
        }
    }
    
    /**
     * 包装工具回调
     */
    public static ToolCallback wrap(ToolCallback callback, AutoChunkInterceptor chunkInterceptor) {
        return new McpToolCallInterceptor(callback, chunkInterceptor);
    }
    
}