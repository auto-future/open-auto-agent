package cn.unicom.soc.servers.service;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 工具拦截器工厂
 * 用于创建和管理工具调用拦截器（带分块功能）
 */
@Component
public class ToolInterceptorFactory {
    
    @Autowired
    private AutoChunkInterceptor chunkInterceptor;
    
    /**
     * 创建工具回调拦截器
     */
    public ToolCallback createInterceptedCallback(ToolCallback originalCallback) {
        return new McpToolCallInterceptor(originalCallback, chunkInterceptor);
    }
    
    /**
     * 批量创建工具回调拦截器
     */
    public ToolCallback[] createInterceptedCallbacks(ToolCallback[] originalCallbacks) {
        ToolCallback[] interceptedCallbacks = new ToolCallback[originalCallbacks.length];
        for (int i = 0; i < originalCallbacks.length; i++) {
            interceptedCallbacks[i] = createInterceptedCallback(originalCallbacks[i]);
        }
        return interceptedCallbacks;
    }
}