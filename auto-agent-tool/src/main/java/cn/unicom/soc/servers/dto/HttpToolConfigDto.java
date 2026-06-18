package cn.unicom.soc.servers.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HttpToolConfigDto {
    
    private String id;
    private String name;
    private String description;
    private String toolSetId;
    private String method; // GET, POST, PUT, DELETE
    private String url;
    private String headers;
    private String requestBodyTemplate;
    private String responseParsingPattern;
    private Integer status; // 0-禁用, 1-启用
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}