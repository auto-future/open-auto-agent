package cn.unicom.soc.servers.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "tools")
@Data
public class ToolEntity {
    
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
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
    
    @Column(name = "status", nullable = false)
    private Integer status = 1; // 0-禁用, 1-启用
    
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