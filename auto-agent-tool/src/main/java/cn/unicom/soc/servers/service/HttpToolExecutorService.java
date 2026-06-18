package cn.unicom.soc.servers.service;

import cn.unicom.soc.servers.entity.HttpToolConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP 工具执行服务
 * 根据数据库配置执行 HTTP 请求
 */
public class HttpToolExecutorService {

    private static final Logger logger = LoggerFactory.getLogger(HttpToolExecutorService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)\\}");

    /**
     * 执行 HTTP 请求
     *
     * @param config HTTP 工具配置
     * @param params 用户传入的参数
     * @return HTTP 响应结果
     */
    public String execute(HttpToolConfig config, Map<String, Object> params) {
        try {
            logger.info("Executing HTTP tool: {}, method: {}, params: {}",
                    config.getToolName(), config.getHttpMethod(), params);
    
            // 1. 构建最终 URL（替换模板中的占位符）
            String finalUrl = resolveUrlTemplate(config.getUrlTemplate(), params);
                
            // 1.5. 如果是GET请求，将参数追加到URL作为查询参数
            if (config.getHttpMethod().equalsIgnoreCase("GET")) {
                finalUrl = appendQueryParameters(finalUrl, params);
            }
    
            // 2. 构建请求头
            Map<String, String> headers = parseHeaders(config.getHeaders());
    
            // 3. 处理认证
            applyAuthentication(config, headers);
    
            // 4. 构建请求体
            String requestBody = null;
            if (StringUtils.hasText(config.getRequestBodyTemplate())) {
                requestBody = resolveBodyTemplate(config.getRequestBodyTemplate(), params);
            }
    
            // 5. 发送 HTTP 请求
            return sendHttpRequest(
                    config.getHttpMethod(),
                    finalUrl,
                    headers,
                    requestBody,
                    config.getTimeoutMs() != null ? config.getTimeoutMs() : 30000
            );
    
        } catch (Exception e) {
            logger.error("Failed to execute HTTP tool: {}", config.getToolName(), e);
            ObjectNode errorResult = objectMapper.createObjectNode(); 
            errorResult.put("isError", true);
            errorResult.put("error", e.getMessage());
            errorResult.put("toolName", config.getToolName());
            return errorResult.toString();
        }
    }

    /**
     * 解析 URL 模板，替换占位符
     */
    private String resolveUrlTemplate(String urlTemplate, Map<String, Object> params) {
        if (urlTemplate == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(urlTemplate);
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = params.get(key);
            if (value == null) {
                logger.warn("URL template parameter '{}' not provided, keeping placeholder", key);
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(value.toString()));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 解析请求体模板，替换占位符
     */
    private String resolveBodyTemplate(String bodyTemplate, Map<String, Object> params) {
        if (bodyTemplate == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(bodyTemplate);
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = params.get(key);
            if (value == null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(value.toString()));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }
    
    /**
     * 将参数追加到URL作为查询参数
     *
     * @param urlStr 原始URL
     * @param params 参数Map
     * @return 带查询参数的URL
     */
    private String appendQueryParameters(String urlStr, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return urlStr;
        }
        
        try {
            // 分离URL和现有查询参数
            String baseUrl = urlStr;
            String existingQuery = "";
            int queryIndex = urlStr.indexOf('?');
            if (queryIndex != -1) {
                baseUrl = urlStr.substring(0, queryIndex);
                existingQuery = urlStr.substring(queryIndex + 1);
            }
            
            // 构建查询参数字符串
            StringBuilder queryString = new StringBuilder();
            if (existingQuery.length() > 0) {
                queryString.append(existingQuery);
                if (!existingQuery.endsWith("&")) {
                    queryString.append("&");
                }
            }
            
            // 先找出URL模板中已有的占位符参数名
            java.util.Set<String> placeholderParams = new java.util.HashSet<>();
            Matcher placeholderMatcher = PLACEHOLDER_PATTERN.matcher(urlStr);
            while (placeholderMatcher.find()) {
                placeholderParams.add(placeholderMatcher.group(1));
            }
            
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                // 跳过已经在URL模板中使用的占位符参数
                if (entry.getValue() != null && !placeholderParams.contains(entry.getKey())) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    
                    // 只有不是占位符参数时才作为查询参数添加
                    if (queryString.length() > 0 && !queryString.toString().endsWith("&")) {
                        queryString.append("&");
                    }
                    queryString.append(java.net.URLEncoder.encode(key, StandardCharsets.UTF_8.name()))
                            .append("=")
                            .append(java.net.URLEncoder.encode(value.toString(), StandardCharsets.UTF_8.name()));
                }
            }
            
            // 返回带查询参数的URL
            if (queryString.length() > 0) {
                return baseUrl + "?" + queryString.toString();
            }
            
            return urlStr;
        } catch (Exception e) {
            logger.error("Failed to append query parameters to URL: {}", urlStr, e);
            return urlStr; // 返回原始URL
        }
    }

    /**
     * 解析请求头 JSON 字符串
     */
    private Map<String, String> parseHeaders(String headersJson) throws IOException {
        if (!StringUtils.hasText(headersJson)) {
            return new java.util.HashMap<>();
        }
        JsonNode headersNode = objectMapper.readTree(headersJson);
        Map<String, String> headers = new java.util.HashMap<>();
        if (headersNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = headersNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                headers.put(field.getKey(), field.getValue().asText());
            }
        }
        return headers;
    }

    /**
     * 应用认证信息到请求头
     */
    private void applyAuthentication(HttpToolConfig config, Map<String, String> headers) {
        if (!StringUtils.hasText(config.getAuthType())) {
            return;
        }
        String authType = config.getAuthType().toLowerCase();
        try {
            switch (authType) {
                case "basic":
                    if (StringUtils.hasText(config.getAuthConfig())) {
                        JsonNode authNode = objectMapper.readTree(config.getAuthConfig());
                        String username = authNode.has("username") ? authNode.get("username").asText() : "";
                        String password = authNode.has("password") ? authNode.get("password").asText() : "";
                        String credentials = Base64.getEncoder().encodeToString(
                                (username + ":" + password).getBytes(StandardCharsets.UTF_8));
                        headers.put("Authorization", "Basic " + credentials);
                    }
                    break;
                case "bearer":
                    if (StringUtils.hasText(config.getAuthConfig())) {
                        JsonNode authNode = objectMapper.readTree(config.getAuthConfig());
                        String token = authNode.has("token") ? authNode.get("token").asText() : "";
                        headers.put("Authorization", "Bearer " + token);
                    }
                    break;
                case "apikey":
                    if (StringUtils.hasText(config.getAuthConfig())) {
                        JsonNode authNode = objectMapper.readTree(config.getAuthConfig());
                        String apiKey = authNode.has("apiKey") ? authNode.get("apiKey").asText() : "";
                        String apiKeyHeader = authNode.has("headerName") ? authNode.get("headerName").asText() : "X-API-Key";
                        headers.put(apiKeyHeader, apiKey);
                    }
                    break;
                case "none":
                    // 不应用任何认证，这是默认行为
                    break;
                default:
                    logger.warn("Unsupported auth type: {}", config.getAuthType());
            }
        } catch (Exception e) {
            logger.error("Failed to apply authentication for tool: {}", config.getToolName(), e);
        }
    }

    /**
     * 发送 HTTP 请求
     */
    private String sendHttpRequest(String method, String urlStr, Map<String, String> headers,
                                   String requestBody, int timeoutMs) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method.toUpperCase());
        conn.setConnectTimeout(timeoutMs);
        conn.setReadTimeout(timeoutMs);
        conn.setDoInput(true);
        conn.setInstanceFollowRedirects(true);

        // 设置默认请求头
        conn.setRequestProperty("Accept", "application/json, text/plain, */*");
        conn.setRequestProperty("User-Agent", "MCP-HTTP-Tool/1.0");

        // 设置自定义请求头
        for (Map.Entry<String, String> header : headers.entrySet()) {
            conn.setRequestProperty(header.getKey(), header.getValue());
        }

        // 发送请求体
        if (StringUtils.hasText(requestBody) && (method.equalsIgnoreCase("POST")
                || method.equalsIgnoreCase("PUT")
                || method.equalsIgnoreCase("PATCH"))) {
            conn.setDoOutput(true);
            if (!headers.containsKey("Content-Type")) {
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            }
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }

        // 读取响应
        int responseCode = conn.getResponseCode();
        StringBuilder responseBuilder = new StringBuilder();

        BufferedReader reader;
        if (responseCode >= 200 && responseCode < 300) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream() != null
                    ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8));
        }

        String line;
        while ((line = reader.readLine()) != null) {
            responseBuilder.append(line).append("\n");
        }
        reader.close();
        conn.disconnect();

        ObjectNode result = objectMapper.createObjectNode();
        result.put("statusCode", responseCode);
        result.put("body", responseBuilder.toString().trim());
        System.out.println(result.toString());
        return result.toString();
    }
    
    /**
     * 执行 HTTP 工具调用（接受JSON字符串参数）
     *
     * @param config HTTP 工具配置
     * @param toolInput JSON格式的参数
     * @return HTTP 响应结果
     */
    public String executeToolCall(HttpToolConfig config, String toolInput) {
        try {
            // 解析传入的参数
            Map<String, Object> params = objectMapper.readValue(toolInput, Map.class);
            
            // 调用现有的execute方法
            return execute(config, params);
        } catch (Exception e) {
            logger.error("Failed to execute HTTP tool with JSON input: {}", config.getToolName(), e);
            try {
                ObjectNode errorResult = objectMapper.createObjectNode();
                errorResult.put("isError", true);
                errorResult.put("error", e.getMessage());
                errorResult.put("toolName", config.getToolName());
                return errorResult.toString();
            } catch (Exception ex) {
                return "{\"isError\": true, \"error\": \"Internal error occurred\", \"toolName\": \"" + config.getToolName() + "\"}";
            }
        }
    }
}
