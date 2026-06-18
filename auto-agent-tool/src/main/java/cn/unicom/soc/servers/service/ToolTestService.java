package cn.unicom.soc.servers.service;

import cn.unicom.soc.servers.dto.ToolDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ToolTestService {

    private static final Logger logger = LoggerFactory.getLogger(ToolTestService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 执行工具测试
     */
    public Map<String, Object> executeTool(ToolDto tool, Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            logger.info("Testing tool: {} with params: {}", tool.getName(), params);

            // 根据工具类型执行测试
            String output;
            switch (tool.getType()) {
                case "mcp" -> output = executeMcpTool(tool, params);
                case "http" -> output = executeHttpTool(tool, params);
                case "skills" -> output = "Skills工具暂不支持测试";
                default -> throw new RuntimeException("不支持的工具类型: " + tool.getType());
            }

            result.put("success", true);
            result.put("toolName", tool.getName());
            result.put("output", output);
            result.put("message", "工具执行成功");

            logger.info("Tool test completed: {}", tool.getName());

        } catch (Exception e) {
            logger.error("Tool test failed: {}", tool.getName(), e);
            result.put("success", false);
            result.put("toolName", tool.getName());
            result.put("error", e.getMessage());
            result.put("message", "工具执行失败");
        }

        return result;
    }

    /**
     * 执行MCP工具
     */
    private String executeMcpTool(ToolDto tool, Map<String, Object> params) {
        try {
            // 查找已注册的ToolCallback
            ToolCallback callback = findToolCallback(tool.getName());

            if (callback == null) {
                throw new RuntimeException("工具未注册: " + tool.getName());
            }

            // 将参数转换为JSON字符串
            String paramsJson = objectMapper.writeValueAsString(params);

            // 调用工具
            String result = callback.call(paramsJson);

            return result;

        } catch (Exception e) {
            throw new RuntimeException("MCP工具执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 执行HTTP工具
     */
    private String executeHttpTool(ToolDto tool, Map<String, Object> params) {
        try {
            // 从inputSchema中获取HTTP配置
            JsonNode schemaNode = objectMapper.readTree(tool.getInputSchema());

            String url = schemaNode.has("url") ? schemaNode.get("url").asText() : null;
            String method = schemaNode.has("method") ? schemaNode.get("method").asText() : "GET";

            if (url == null || url.isEmpty()) {
                throw new RuntimeException("HTTP工具缺少URL配置");
            }

            // 构建完整的URL（替换路径参数）
            String fullUrl = buildUrl(url, params);

            // 发起HTTP请求
            return sendHttpRequest(fullUrl, method, params);

        } catch (Exception e) {
            throw new RuntimeException("HTTP工具执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查找已注册的工具回调
     */
    private ToolCallback findToolCallback(String toolName) {
        try {
            // 从Spring上下文中获取所有ToolCallbackProvider
            Map<String, ToolCallbackProvider> providers =
                    applicationContext.getBeansOfType(ToolCallbackProvider.class);

            for (ToolCallbackProvider provider : providers.values()) {
                for (ToolCallback callback : provider.getToolCallbacks()) {
                    if (callback.getToolDefinition().name().equals(toolName)) {
                        return callback;
                    }
                }
            }

            logger.warn("Tool not found: {}", toolName);
            return null;

        } catch (Exception e) {
            logger.error("Failed to find tool callback: {}", toolName, e);
            return null;
        }
    }

    /**
     * 构建URL（替换路径参数）
     */
    private String buildUrl(String url, Map<String, Object> params) {
        String result = url;

        // 替换路径参数 {param}
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            if (result.contains(placeholder)) {
                result = result.replace(placeholder, entry.getValue().toString());
            }
        }

        // 如果是GET请求且有剩余参数，添加为查询参数
        if (!result.contains("?") && !params.isEmpty()) {
            StringBuilder queryParams = new StringBuilder();
            boolean first = true;

            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                if (!url.contains(placeholder)) {
                    if (first) {
                        result += "?";
                        first = false;
                    } else {
                        result += "&";
                    }
                    result += entry.getKey() + "=" + entry.getValue();
                }
            }
        }

        return result;
    }

    /**
     * 发送HTTP请求
     */
    private String sendHttpRequest(String url, String method, Map<String, Object> params) {
        try {
            // 使用Java 11+的HttpClient
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();

            java.net.http.HttpRequest.Builder requestBuilder =
                    java.net.http.HttpRequest.newBuilder()
                            .uri(java.net.URI.create(url))
                            .method(method, getRequestBody(method, params));

            requestBuilder.header("Content-Type", "application/json");

            java.net.http.HttpRequest request = requestBuilder.build();
            java.net.http.HttpResponse<String> response =
                    client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            // 返回响应体
            return response.body();

        } catch (Exception e) {
            throw new RuntimeException("HTTP请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取请求体
     */
    private java.net.http.HttpRequest.BodyPublisher getRequestBody(
            String method, Map<String, Object> params) {

        if ("GET".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
            return java.net.http.HttpRequest.BodyPublishers.noBody();
        }

        try {
            // 转换为JSON
            String json = objectMapper.writeValueAsString(params);
            return java.net.http.HttpRequest.BodyPublishers.ofString(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("构建请求体失败: " + e.getMessage(), e);
        }
    }
}
