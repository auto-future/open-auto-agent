package cn.unicom.soc.servers.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tools")
@Data
public class ToolEntity {
    
    @Id
    private String id;
    
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "tool_set_id", nullable = false)
    private String toolSetId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tool_set_id", insertable = false, updatable = false)
    private ToolSetEntity toolSet;
    
    @Column(name = "type", nullable = false, length = 50) // mcp/http/custom
    private String type;
    
    @Column(name = "input_schema", columnDefinition = "TEXT")
    private String inputSchema;
    
    @Column(name = "resource_path", length = 500)
    private String resourcePath; // skill 目录路径
    
    @Column(name = "script_content", columnDefinition = "TEXT")
    private String scriptContent; // skill 入口脚本内容
    
    @Column(name = "status", nullable = false)
    private Integer status = 1; // 0-禁用, 1-启用
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}