package cn.unicom.soc.servers.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ToolSetDto {
    
    private String id;
    private String name;
    private String description;
    private String type; // internal-内部工具集, external-外部MCP Server
    private String tag; // mcp, skills, http
    private Integer status; // 0-禁用, 1-启用
    private String mcpConfig; // MCP Server JSON 配置
    private String customConfig; // 自定义工具集配置
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 关联的工具列表
    private List<ToolDto> tools;
    
    // 关联的HTTP工具列表
    private List<HttpToolConfigDto> httpTools;
}