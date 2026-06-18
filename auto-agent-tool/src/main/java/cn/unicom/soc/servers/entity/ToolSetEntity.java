package cn.unicom.soc.servers.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tool_sets")
@Data
public class ToolSetEntity {
    
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;
    
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "type", nullable = false, length = 50) // internal-内部工具集, external-外部MCP Server
    private String type;
    
    @Column(name = "tag", length = 50) // mcp, skills, http
    private String tag;
    
    @Column(name = "status", nullable = false)
    private Integer status = 1; // 0-禁用, 1-启用
    
    @OneToMany(mappedBy = "toolSet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ToolEntity> tools;
    
    @OneToMany(mappedBy = "toolSet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HttpToolConfigEntity> httpTools;
    
    @Column(name = "mcp_config", columnDefinition = "TEXT")
    private String mcpConfig; // MCP Server JSON 配置
    
    @Column(name = "custom_config", columnDefinition = "TEXT")
    private String customConfig; // 自定义工具集配置
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}