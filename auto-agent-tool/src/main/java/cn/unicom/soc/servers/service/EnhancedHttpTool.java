package cn.unicom.soc.servers.service;

import cn.unicom.soc.servers.common.annotation.Tool;
import cn.unicom.soc.servers.common.annotation.ToolInput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class EnhancedHttpTool {
    
    @Autowired
    private ToolExecutionWrapper toolWrapper;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Tool(description = "增强HTTP请求工具（自动分块长响应）")
    public Object enhancedHttpRequest(@ToolInput(description = "请求URL") String url,
                                    @ToolInput(description = "请求方法") String method,
                                    @ToolInput(description = "请求体(JSON格式，可选)") String body,
                                    @ToolInput(description = "用户ID") String userId,
                                    @ToolInput(description = "会话ID") String sessionId) {
        
        // 执行HTTP请求
        Object result = toolWrapper.executeToolWithAutoChunk(() -> {
            HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = null;
            if (body != null && !body.trim().isEmpty()) {
                entity = new HttpEntity<>(body, headers);
            } else {
                entity = new HttpEntity<>(headers);
            }
            
            ResponseEntity<String> response = restTemplate.exchange(url, httpMethod, entity, String.class);
            return response.getBody();
        }, userId, sessionId);
        
        return result;
    }
}