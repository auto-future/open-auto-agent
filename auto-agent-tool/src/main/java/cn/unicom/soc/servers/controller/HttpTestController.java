package cn.unicom.soc.servers.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.net.ssl.SSLContext;
import java.util.Base64;
import java.util.Map;

/**
 * HTTP 接口测试代理控制器
 * 提供代理请求能力，避免前端直接请求外部 API 时的 CORS 问题
 * 使用 Apache HttpClient 5 支持 HTTPS 自签名证书
 */
@RestController
@RequestMapping("/api/http-test")
@CrossOrigin(origins = "*")
public class HttpTestController {

    private static final Logger logger = LoggerFactory.getLogger(HttpTestController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Timeout DEFAULT_TIMEOUT = Timeout.ofSeconds(30);

    /**
     * 代理 HTTP 请求
     * 请求体格式：
     * {
     *   "method": "GET|POST|PUT|DELETE|PATCH",
     *   "url": "https://...",
     *   "headers": {"key": "value"},
     *   "body": "...",
     *   "auth": {"type": "none|bearer|basic|apikey", ...},
     *   "timeout": 30000,
     *   "followRedirect": true,
     *   "ignoreSsl": false
     * }
     */
    @PostMapping("/proxy")
    public ResponseEntity<String> proxyRequest(@RequestBody Map<String, Object> request) {
        long startTime = System.currentTimeMillis();
        ObjectNode result = objectMapper.createObjectNode();

        try {
            String method = (String) request.getOrDefault("method", "GET");
            String urlStr = (String) request.get("url");
            if (urlStr == null || urlStr.isBlank()) {
                result.put("success", false);
                result.put("error", "URL 不能为空");
                return ResponseEntity.badRequest().body(result.toString());
            }

            // 解析 headers
            java.util.Map<String, String> headers = new java.util.LinkedHashMap<>();
            headers.put("Accept", "application/json, text/plain, */*");
            headers.put("User-Agent", "AI-Tool-Governance/1.0");

            @SuppressWarnings("unchecked")
            Map<String, String> customHeaders = (Map<String, String>) request.get("headers");
            if (customHeaders != null) {
                headers.putAll(customHeaders);
            }

            // 处理认证
            @SuppressWarnings("unchecked")
            Map<String, Object> auth = (Map<String, Object>) request.get("auth");
            if (auth != null) {
                applyAuth(auth, headers);
            }

            // 请求体
            String body = null;
            Object bodyObj = request.get("body");
            if (bodyObj != null) {
                body = bodyObj instanceof String ? (String) bodyObj : objectMapper.writeValueAsString(bodyObj);
            }

            // 超时
            int timeoutMs = 30000;
            Object timeoutObj = request.get("timeout");
            if (timeoutObj instanceof Number) {
                timeoutMs = ((Number) timeoutObj).intValue();
            }

            // 是否忽略 SSL 证书验证
            boolean ignoreSsl = Boolean.TRUE.equals(request.get("ignoreSsl"));

            // 是否跟随重定向
            boolean followRedirect = Boolean.TRUE.equals(request.get("followRedirect"));

            // 发送请求
            return sendRequest(method, urlStr, headers, body, timeoutMs, startTime, ignoreSsl, followRedirect);

        } catch (java.net.UnknownHostException e) {
            logger.error("HTTP test proxy error: Unknown host", e);
            result.put("success", false);
            result.put("error", "无法解析域名: " + e.getMessage() + "，请检查 URL 是否正确");
            result.put("duration", System.currentTimeMillis() - startTime);
            return ResponseEntity.ok(result.toString());
        } catch (Exception e) {
            logger.error("HTTP test proxy error", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("duration", System.currentTimeMillis() - startTime);
            return ResponseEntity.ok(result.toString());
        }
    }

    private void applyAuth(Map<String, Object> auth, java.util.Map<String, String> headers) {
        String type = (String) auth.getOrDefault("type", "none");
        switch (type.toLowerCase()) {
            case "bearer":
                String token = (String) auth.get("token");
                if (token != null && !token.isBlank()) {
                    headers.put("Authorization", "Bearer " + token);
                }
                break;
            case "basic":
                String username = (String) auth.getOrDefault("username", "");
                String password = (String) auth.getOrDefault("password", "");
                String credentials = Base64.getEncoder().encodeToString(
                        (username + ":" + password).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                headers.put("Authorization", "Basic " + credentials);
                break;
            case "apikey":
                String keyName = (String) auth.getOrDefault("keyName", "X-API-Key");
                String keyValue = (String) auth.get("keyValue");
                if (keyValue != null && !keyValue.isBlank()) {
                    headers.put(keyName, keyValue);
                }
                break;
        }
    }

    private ResponseEntity<String> sendRequest(String method, String urlStr,
                                                java.util.Map<String, String> headers,
                                                String body, int timeoutMs, long startTime,
                                                boolean ignoreSsl, boolean followRedirect) throws Exception {
        ObjectNode result = objectMapper.createObjectNode();

        // 构建 HTTP 客户端
        CloseableHttpClient httpClient = buildHttpClient(ignoreSsl, followRedirect, timeoutMs);

        // 构建请求
        ClassicHttpRequest httpRequest = buildHttpRequest(method, urlStr, headers, body);

        try (CloseableHttpResponse response = httpClient.execute(httpRequest)) {
            int statusCode = response.getCode();
            String statusText = response.getReasonPhrase() != null ? response.getReasonPhrase() : "";

            // 响应头
            ObjectNode responseHeaders = objectMapper.createObjectNode();
            for (Header header : response.getHeaders()) {
                String existing = responseHeaders.has(header.getName())
                        ? responseHeaders.get(header.getName()).asText() + ", "
                        : "";
                responseHeaders.put(header.getName(), existing + header.getValue());
            }

            // 响应体
            String bodyStr = "";
            if (response.getEntity() != null) {
                bodyStr = EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);
            }

            long duration = System.currentTimeMillis() - startTime;

            result.put("success", true);
            result.put("statusCode", statusCode);
            result.put("statusText", statusText);
            result.put("duration", duration);
            result.set("headers", responseHeaders);
            result.put("body", bodyStr);

            // 尝试解析为 JSON 以便前端美化显示
            try {
                JsonNode jsonBody = objectMapper.readTree(bodyStr);
                result.set("parsedBody", jsonBody);
            } catch (Exception ignored) {
                // 非 JSON 响应，忽略
            }

            return ResponseEntity.ok(result.toString());
        } finally {
            httpClient.close();
        }
    }

    private CloseableHttpClient buildHttpClient(boolean ignoreSsl, boolean followRedirect, int timeoutMs) throws Exception {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(timeoutMs))
                .setResponseTimeout(Timeout.ofMilliseconds(timeoutMs))
                .setRedirectsEnabled(followRedirect)
                .build();

        if (ignoreSsl) {
            // 忽略 SSL 证书验证（用于自签名证书测试环境）
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(null, (chain, authType) -> true)
                    .build();

            var sslSocketFactoryBuilder = SSLConnectionSocketFactoryBuilder.create()
                    .setSslContext(sslContext)
                    .setHostnameVerifier((hostname, session) -> true);

            PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(sslSocketFactoryBuilder.build())
                    .build();

            return HttpClients.custom()
                    .setDefaultRequestConfig(requestConfig)
                    .setConnectionManager(connectionManager)
                    .build();
        }

        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    private ClassicHttpRequest buildHttpRequest(String method, String urlStr,
                                                 java.util.Map<String, String> headers,
                                                 String body) {
        ClassicHttpRequest request;
        String upperMethod = method.toUpperCase();

        switch (upperMethod) {
            case "GET":
                request = new HttpGet(urlStr);
                break;
            case "POST":
                request = new HttpPost(urlStr);
                break;
            case "PUT":
                request = new HttpPut(urlStr);
                break;
            case "DELETE":
                request = new HttpDelete(urlStr);
                break;
            case "PATCH":
                request = new HttpPatch(urlStr);
                break;
            case "HEAD":
                request = new HttpHead(urlStr);
                break;
            case "OPTIONS":
                request = new HttpOptions(urlStr);
                break;
            default:
                request = new HttpGet(urlStr);
        }

        // 设置请求头
        for (Map.Entry<String, String> header : headers.entrySet()) {
            request.setHeader(header.getKey(), header.getValue());
        }

        // 设置请求体
        if (body != null && !body.isBlank() &&
                (upperMethod.equals("POST") || upperMethod.equals("PUT") || upperMethod.equals("PATCH"))) {
            String contentType = headers.getOrDefault("Content-Type", "application/json");
            request.setEntity(new StringEntity(body, ContentType.parse(contentType)));
        }

        return request;
    }
}
