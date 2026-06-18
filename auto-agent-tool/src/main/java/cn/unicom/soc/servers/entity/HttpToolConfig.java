package cn.unicom.soc.servers.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "http_tools")
@Data
/**
 * HTTP 工具配置实体类
 * 用于存储从数据库读取的 HTTP 请求工具配置
 */
public class HttpToolConfig {

    @Id
    @GeneratedValue(generator = "increment")
    @GenericGenerator(name = "increment", strategy = "increment")
    private Integer id;
    
    @Column(name = "tool_name", nullable = false, length = 255)
    private String toolName;
    
    @Column(name = "tool_description", columnDefinition = "TEXT")
    private String toolDescription;
    
    @Column(name = "http_method", length = 10, nullable = false)
    private String httpMethod = "GET";
    
    @Column(name = "url_template", columnDefinition = "TEXT", nullable = false)
    private String urlTemplate;
    
    @Column(name = "headers", columnDefinition = "TEXT")
    private String headers;
    
    @Column(name = "request_body_template", columnDefinition = "TEXT")
    private String requestBodyTemplate;
    
    @Column(name = "params_schema", columnDefinition = "TEXT")
    private String paramsSchema;
    
    @Column(name = "auth_type", length = 50)
    private String authType;
    
    @Column(name = "auth_config", columnDefinition = "TEXT")
    private String authConfig;
    
    @Column(name = "timeout_ms")
    private Integer timeoutMs = 30000;
    
    @Column(name = "enabled")
    private Boolean enabled = true;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public HttpToolConfig() {
        this.httpMethod = "GET";
        this.timeoutMs = 30000;
        this.enabled = true;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getToolDescription() {
        return toolDescription;
    }

    public void setToolDescription(String toolDescription) {
        this.toolDescription = toolDescription;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getUrlTemplate() {
        return urlTemplate;
    }

    public void setUrlTemplate(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public String getRequestBodyTemplate() {
        return requestBodyTemplate;
    }

    public void setRequestBodyTemplate(String requestBodyTemplate) {
        this.requestBodyTemplate = requestBodyTemplate;
    }

    public String getParamsSchema() {
        return paramsSchema;
    }

    public void setParamsSchema(String paramsSchema) {
        this.paramsSchema = paramsSchema;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getAuthConfig() {
        return authConfig;
    }

    public void setAuthConfig(String authConfig) {
        this.authConfig = authConfig;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "HttpToolConfig{" +
                "id=" + id +
                ", toolName='" + toolName + '\'' +
                ", toolDescription='" + toolDescription + '\'' +
                ", httpMethod='" + httpMethod + '\'' +
                ", urlTemplate='" + urlTemplate + '\'' +
                ", headers='" + headers + '\'' +
                ", requestBodyTemplate='" + requestBodyTemplate + '\'' +
                ", paramsSchema='" + paramsSchema + '\'' +
                ", authType='" + authType + '\'' +
                ", authConfig='" + authConfig + '\'' +
                ", timeoutMs=" + timeoutMs +
                ", enabled=" + enabled +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
