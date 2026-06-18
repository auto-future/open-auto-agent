package cn.unicom.soc.servers.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 通用自定义工具回调实现
 * 使用Lambda函数实现完全通用的工具调用
 */
public class GenericToolCallback implements ToolCallback {

    private final ToolDefinition definition;
    private final Function<String, String> toolFunction;
    private final ObjectMapper objectMapper;

    public GenericToolCallback(String name, String description, String inputSchema, Function<String, String> toolFunction) {
        this.definition = ToolDefinition.builder()
                .name(name)
                .description(description)
                .inputSchema(inputSchema)
                .build();
        this.toolFunction = toolFunction;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return definition;
    }

    @Override
    public String call(String toolInput) {
        try {
            // 调用外部提供的Lambda函数来执行具体的工具操作
            return toolFunction.apply(toolInput);
        } catch (Exception e) {
            // 发生错误时返回错误信息
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "Error executing tool: " + e.getMessage());

            try {
                return objectMapper.writeValueAsString(errorResult);
            } catch (JsonProcessingException jsonException) {
                return "{\"success\": false, \"error\": \"Internal error occurred\"}";
            }
        }
    }
}