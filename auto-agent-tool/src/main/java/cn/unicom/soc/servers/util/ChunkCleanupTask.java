package cn.unicom.soc.servers.util;

import cn.unicom.soc.servers.service.ChunkedResultRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
@Slf4j
public class ChunkCleanupTask {
    
    @Autowired
    private ChunkedResultRepository chunkedResultRepository;
    
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    @Transactional
    public void cleanupExpiredChunks() {
        // 清理超过24小时的分块数据
        LocalDateTime cutoffDateTime = LocalDateTime.now().minus(24, ChronoUnit.HOURS);
        
        try {
            int deletedCount = chunkedResultRepository.cleanupOlderThan(cutoffDateTime);
            log.info("清理了 {} 个过期的分块数据", deletedCount);
        } catch (Exception e) {
            log.error("清理过期分块数据时发生错误", e);
        }
    }
}