package cn.unicom.soc.servers.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;

/**
 * HTTP 接口测试代理控制器
 * 提供代理请求能力，避免前端直接请求外部 API 时的 CORS 问题
 */
@RestController
@RequestMapping("/api/http-test")
@CrossOrigin(origins = "*")
public class HttpTestController {

    private static final Logger logger = LoggerFactory.getLogger(HttpTestController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 代理 HTTP 请求
     * 请求体格式：
     * {
     *   "method": "GET|POST|PUT|DELETE|PATCH",
     *   "url": "https://...",
     *   "headers": {"key": "value"},
     *   "body": "...",
     *   "auth": {"type": "none|bearer|basic|apikey", ...}
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
            int timeout = 30000;
            Object timeoutObj = request.get("timeout");
            if (timeoutObj instanceof Number) {
                timeout = ((Number) timeoutObj).intValue();
            }

            // 是否忽略 SSL 证书验证（用于自签名证书测试）
            boolean ignoreSsl = Boolean.TRUE.equals(request.get("ignoreSsl"));

            // 是否跟随重定向
            boolean followRedirect = Boolean.TRUE.equals(request.get("followRedirect"));

            // 发送请求
            return sendRequest(method, urlStr, headers, body, timeout, startTime, ignoreSsl, followRedirect);

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
                        (username + ":" + password).getBytes(StandardCharsets.UTF_8));
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

    private static final TrustManager[] TRUST_ALL_CERTS = new TrustManager[] {
        new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
        }
    };

    private static final HostnameVerifier VERIFY_ALL_HOSTS = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) { return true; }
    };

    private ResponseEntity<String> sendRequest(String method, String urlStr,
                                                java.util.Map<String, String> headers,
                                                String body, int timeout, long startTime,
                                                boolean ignoreSsl, boolean followRedirect) throws Exception {
        ObjectNode result = objectMapper.createObjectNode();

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method.toUpperCase());
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        conn.setDoInput(true);
        conn.setInstanceFollowRedirects(followRedirect);

        // 处理 HTTPS 忽略证书验证
        if (conn instanceof HttpsURLConnection && ignoreSsl) {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, TRUST_ALL_CERTS, new java.security.SecureRandom());
            HttpsURLConnection httpsConn = (HttpsURLConnection) conn;
            httpsConn.setSSLSocketFactory(sc.getSocketFactory());
            httpsConn.setHostnameVerifier(VERIFY_ALL_HOSTS);
        }

        // 设置请求头
        for (Map.Entry<String, String> header : headers.entrySet()) {
            conn.setRequestProperty(header.getKey(), header.getValue());
        }

        // 发送请求体
        if (body != null && !body.isBlank() &&
                (method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT") || method.equalsIgnoreCase("PATCH"))) {
            conn.setDoOutput(true);
            if (!headers.containsKey("Content-Type")) {
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            }
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }

        // 读取响应
        int statusCode = conn.getResponseCode();
        String statusText = conn.getResponseMessage() != null ? conn.getResponseMessage() : "";

        // 响应头
        ObjectNode responseHeaders = objectMapper.createObjectNode();
        conn.getHeaderFields().forEach((key, values) -> {
            if (key != null && values != null) {
                responseHeaders.put(key, String.join(", ", values));
            }
        });

        // 响应体
        StringBuilder responseBody = new StringBuilder();
        BufferedReader reader;
        if (statusCode >= 200 && statusCode < 300) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            reader = new BufferedReader(new InputStreamReader(
                    conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream(),
                    StandardCharsets.UTF_8));
        }
        String line;
        while ((line = reader.readLine()) != null) {
            responseBody.append(line).append("\n");
        }
        reader.close();
        conn.disconnect();

        long duration = System.currentTimeMillis() - startTime;

        result.put("success", true);
        result.put("statusCode", statusCode);
        result.put("statusText", statusText);
        result.put("duration", duration);
        result.set("headers", responseHeaders);

        String bodyStr = responseBody.toString().trim();
        result.put("body", bodyStr);

        // 尝试解析为 JSON 以便前端美化显示
        try {
            JsonNode jsonBody = objectMapper.readTree(bodyStr);
            result.set("parsedBody", jsonBody);
        } catch (Exception ignored) {
            // 非 JSON 响应，忽略
        }

        return ResponseEntity.ok(result.toString());
    }
}
