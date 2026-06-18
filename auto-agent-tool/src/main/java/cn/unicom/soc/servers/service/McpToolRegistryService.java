package cn.unicom.soc.servers.service;

import cn.unicom.soc.servers.entity.ToolEntity;
import cn.unicom.soc.servers.entity.ToolSetEntity;
import cn.unicom.soc.servers.repository.ToolRepository;
import cn.unicom.soc.servers.repository.ToolSetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * MCP工具注册服务
 * 将MCP工具（SystemInfoService、HttpApiManagerService、ChunkQueryTool等）同步到JPA数据库
 */
@Service
public class McpToolRegistryService {

    private static final Logger logger = LoggerFactory.getLogger(McpToolRegistryService.class);
    
    @Autowired(required = false)
    private ToolSetRepository toolSetRepository;
    
    @Autowired(required = false)
    private ToolRepository toolRepository;

    /**
     * 同步MCP工具集到JPA数据库
     * @param toolSetName 工具集名称（如 "System Info", "HTTP API Manager", "Chunk Query"）
     * @param toolSetDescription 工具集描述
     * @param tools 工具回调列表
     */
    @Transactional
    public void syncMcpToolsToDatabase(String toolSetName, String toolSetDescription, List<ToolCallback> tools) {
        if (toolSetRepository == null || toolRepository == null) {
            logger.warn("JPA repositories not available, skipping MCP tools sync for: {}", toolSetName);
            return;
        }
        
        try {
            logger.info("Syncing MCP tool set to JPA database: {} with {} tools", toolSetName, tools.size());
            
            // 1. 查找或创建工具集
            ToolSetEntity toolSet = toolSetRepository.findByName(toolSetName).orElse(null);
            
            if (toolSet == null) {
                // 创建工具集
                toolSet = new ToolSetEntity();
                toolSet.setName(toolSetName);
                toolSet.setDescription(toolSetDescription);
                toolSet.setType("internal");
                toolSet.setTag("mcp");
                toolSet.setStatus(1);
                toolSet = toolSetRepository.saveAndFlush(toolSet);
                logger.info("Created MCP tool set: {}", toolSetName);
            } else {
                // 更新工具集描述（如果需要）
                if (!toolSetDescription.equals(toolSet.getDescription())) {
                    toolSet.setDescription(toolSetDescription);
                    toolSet = toolSetRepository.saveAndFlush(toolSet);
                    logger.info("Updated MCP tool set description: {}", toolSetName);
                }
            }
            
            // 2. 同步每个工具
            for (ToolCallback callback : tools) {
                syncMcpToolToDatabase(toolSet, callback);
            }
            
            logger.info("Successfully synced MCP tool set: {} with {} tools", toolSetName, tools.size());
            
        } catch (Exception e) {
            logger.error("Failed to sync MCP tool set to JPA database: {}", toolSetName, e);
            throw new RuntimeException("Failed to sync MCP tool set to JPA database: " + toolSetName, e);
        }
    }
    
    /**
     * 同步单个MCP工具到数据库
     */
    @Transactional
    public void syncMcpToolToDatabase(ToolSetEntity toolSet, ToolCallback callback) {
        try {
            String toolName = callback.getToolDefinition().name();
            String toolDescription = callback.getToolDefinition().description();
            String inputSchema = callback.getToolDefinition().inputSchema();
            
            // 查找或创建工具
            ToolEntity tool = toolRepository.findByNameAndToolSetId(toolName, toolSet.getId()).orElse(null);
            
            if (tool == null) {
                // 创建工具
                tool = new ToolEntity();
                tool.setName(toolName);
                tool.setDescription(toolDescription);
                tool.setToolSetId(toolSet.getId());
                tool.setType("mcp");
                tool.setInputSchema(inputSchema);
                tool.setStatus(1); // 默认启用
                tool = toolRepository.saveAndFlush(tool);
                logger.info("Created MCP tool in JPA: {}", toolName);
            } else {
                // 更新工具
                tool.setDescription(toolDescription);
                tool.setInputSchema(inputSchema);
                tool.setStatus(1);
                tool = toolRepository.saveAndFlush(tool);
                logger.info("Updated MCP tool in JPA: {}", toolName);
            }
            
        } catch (Exception e) {
            logger.error("Failed to sync MCP tool to JPA database: {}", callback.getToolDefinition().name(), e);
        }
    }
}
