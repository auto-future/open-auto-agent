package cn.unicom.soc.servers.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tool_call_logs")
@Data
public class ToolCallLogEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "tool_id", length = 36)
    private String toolId;

    @Column(name = "tool_name", length = 200)
    private String toolName;

    @Column(name = "tool_type", length = 50)
    private String toolType;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "request_params", length = 2000)
    private String requestParams;

    @Column(name = "response_summary", length = 2000)
    private String responseSummary;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
