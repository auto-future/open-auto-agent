package cn.unicom.soc.servers.service;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 自定义工具回调提供者，不依赖MethodToolCallbackProvider
 */
public class CustomToolCallbackProvider implements ToolCallbackProvider {
    
    private ToolCallback[] tools;

    
    public CustomToolCallbackProvider(ToolCallback[] tools) {
        this.tools = tools;
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        return tools;
    }

    public void addTools(ToolCallback... tools) {
        ToolCallback[] newTools = Arrays.copyOf(this.tools, this.tools.length + tools.length);
        System.arraycopy(tools, 0, newTools, this.tools.length, tools.length);
        this.tools = newTools;
    }

    /**
     * 根据工具名称移除工具回调
     *
     * @param toolName 要移除的工具名称
     * @return 是否成功移除
     */
    public boolean removeTool(String toolName) {
        if (toolName == null || this.tools.length == 0) {
            return false;
        }
        List<ToolCallback> filtered = new ArrayList<>();
        boolean removed = false;
        for (ToolCallback tool : this.tools) {
            if (tool != null && toolName.equals(tool.getToolDefinition().name())) {
                removed = true;
            } else {
                filtered.add(tool);
            }
        }
        if (removed) {
            this.tools = filtered.toArray(new ToolCallback[0]);
        }
        return removed;
    }
}