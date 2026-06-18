package cn.unicom.soc.servers.service;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.Arrays;

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
}