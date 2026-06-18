package cn.unicom.soc.servers.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ToolDto {
    
    private String id;
    private String name;
    private String description;
    private String toolSetId;
    private String type; // mcp/http/custom
    private String inputSchema;
    private Integer status; // 0-禁用, 1-启用
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}