package cn.unicom.soc.servers.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ToolExecutionWrapper {
    
    @Autowired
    private AutoChunkInterceptor chunkInterceptor;
    
    /**
     * 包装工具执行，自动处理分块
     */
    public Object executeToolWithAutoChunk(ToolExecutor executor, 
                                         String userId, String sessionId) {
        Object result = executor.execute();
        return chunkInterceptor.processResult(result, userId, sessionId);
    }
    
    @FunctionalInterface
    public interface ToolExecutor {
        Object execute();
    }
}