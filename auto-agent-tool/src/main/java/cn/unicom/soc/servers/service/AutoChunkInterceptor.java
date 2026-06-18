package cn.unicom.soc.servers.service;

import cn.unicom.soc.servers.common.AutoChunkSummary;
import cn.unicom.soc.servers.common.ChunkResultInfo;
import cn.unicom.soc.servers.config.ChunkProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class AutoChunkInterceptor {
    
    @Autowired
    private ChunkedResultStorageService storageService;
    
    @Autowired
    private ChunkProperties chunkProperties;
    
    /**
     * 自动检查并分块长结果（带重叠）
     */
    public Object processResult(Object result, String userId, String sessionId) {
        if (result instanceof String) {
            String content = (String) result;
            if (content.length() > chunkProperties.getThreshold()) {
                // 自动分块并返回分块摘要
                try {
                    ChunkResultInfo info = storageService.storeChunkedResult(userId, sessionId, content);
                    
                    return new AutoChunkSummary(
                        info.getSessionId(), 
                        info.getTotalChunks(), 
                        info.getFirstChunkId(), 
                        info.getFirstChunkContent(),
                        chunkProperties.getOverlapRatio()
                    );
                } catch (Exception e) {
                    log.error("自动分块处理失败", e);
                    // 如果分块失败，返回原始内容
                    return result;
                }
            }
        } else if (result instanceof Map) {
            // 如果结果是Map类型，检查其中的字符串字段
            Map<?, ?> resultMap = (Map<?, ?>) result;
            Map<Object, Object> processedResult = new HashMap<>();
            boolean hasChunked = false;
            
            for (Map.Entry<?, ?> entry : resultMap.entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();
                
                if (value instanceof String) {
                    String strValue = (String) value;
                    if (strValue.length() > chunkProperties.getThreshold()) {
                        // 对长字符串字段进行分块处理
                        try {
                            ChunkResultInfo info = storageService.storeChunkedResult(userId, sessionId, strValue);
                            processedResult.put(key, new AutoChunkSummary(
                                info.getSessionId(), 
                                info.getTotalChunks(), 
                                info.getFirstChunkId(), 
                                info.getFirstChunkContent(),
                                chunkProperties.getOverlapRatio()
                            ));
                            hasChunked = true;
                        } catch (Exception e) {
                            log.error("自动分块处理失败", e);
                            processedResult.put(key, value);
                        }
                    } else {
                        processedResult.put(key, value);
                    }
                } else {
                    processedResult.put(key, value);
                }
            }
            
            return hasChunked ? processedResult : result;
        }
        return result; // 不需要分块的内容直接返回
    }
}