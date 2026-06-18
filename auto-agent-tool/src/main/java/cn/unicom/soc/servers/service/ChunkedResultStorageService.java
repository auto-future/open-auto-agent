package cn.unicom.soc.servers.service;

import cn.unicom.soc.servers.common.ChunkData;
import cn.unicom.soc.servers.common.ChunkResultInfo;
import cn.unicom.soc.servers.common.ChunkSegment;
import cn.unicom.soc.servers.config.ChunkProperties;
import cn.unicom.soc.servers.entity.ChunkedResultEntity;
import cn.unicom.soc.servers.entity.ChunkSessionEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class ChunkedResultStorageService {
    
    @Autowired
    private ChunkedResultRepository chunkedResultRepository;
    
    @Autowired
    private ChunkSessionRepository chunkSessionRepository;
    
    @Autowired
    private ChunkProperties chunkProperties;
    
    /**
     * 存储带重叠的分块结果
     */
    public synchronized ChunkResultInfo storeChunkedResult(String userId, String sessionId, String content) {
        // 删除旧的分块结果
        deleteExistingChunks(userId, sessionId);
        
        // 分块处理（带重叠）
        List<ChunkSegment> chunks = splitContentWithOverlap(content, 
            chunkProperties.getSize(), chunkProperties.getOverlapRatio());
        
        // 生成唯一的session ID
        String sessionUuid = UUID.randomUUID().toString();
        
        // 存储分块
        List<String> chunkIds = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            ChunkSegment segment = chunks.get(i);
            String chunkId = generateChunkId(sessionUuid, i);
            String prevChunkId = i > 0 ? chunkIds.get(i-1) : null;
            String nextChunkId = i < chunks.size() - 1 ? generateChunkId(sessionUuid, i+1) : null;
            
            ChunkedResultEntity entity = new ChunkedResultEntity();
            entity.setChunkId(chunkId);
            entity.setUserId(userId);
            entity.setSessionId(sessionId);
            entity.setTotalChunks(chunks.size());
            entity.setChunkSequence(i);
            entity.setContent(segment.getContent());
            entity.setPrevChunkId(prevChunkId);
            entity.setNextChunkId(nextChunkId);
            // 添加重叠信息
            entity.setOverlapStart(segment.getOverlapStart());
            entity.setOverlapEnd(segment.getOverlapEnd());
            entity.setOverlapContent(segment.getOverlapContent());
            
            // 设置时间戳
            entity.setCreatedAt(java.time.LocalDateTime.now());
            entity.setUpdatedAt(java.time.LocalDateTime.now());
            
            chunkedResultRepository.save(entity);
            chunkIds.add(chunkId);
        }
        
        // 记录会话信息
        ChunkSessionEntity sessionEntity = new ChunkSessionEntity();
        sessionEntity.setSessionId(sessionId);
        sessionEntity.setUserId(userId);
        sessionEntity.setTotalChunks(chunks.size());
        sessionEntity.setStatus("completed");
        sessionEntity.setFirstChunkId(chunkIds.get(0));
        sessionEntity.setOverlapRatio(chunkProperties.getOverlapRatio());
        sessionEntity.setCreatedAt(java.time.LocalDateTime.now());
        sessionEntity.setUpdatedAt(java.time.LocalDateTime.now());
        chunkSessionRepository.save(sessionEntity);
        
        return new ChunkResultInfo(sessionId, chunks.size(), chunkIds.get(0), chunks.get(0).getContent());
    }
    
    /**
     * 获取指定分块
     */
    public ChunkData getChunk(String chunkId) {
        Optional<ChunkedResultEntity> entityOpt = chunkedResultRepository.findById(chunkId);
        if (entityOpt.isPresent()) {
            ChunkedResultEntity entity = entityOpt.get();
            ChunkData data = new ChunkData();
            data.setChunkId(entity.getChunkId());
            data.setContent(entity.getContent());
            data.setChunkSequence(entity.getChunkSequence());
            data.setTotalChunks(entity.getTotalChunks());
            data.setPrevChunkId(entity.getPrevChunkId());
            data.setNextChunkId(entity.getNextChunkId());
            data.setSessionId(entity.getSessionId());
            data.setUserId(entity.getUserId());
            data.setOverlapContent(entity.getOverlapContent());
            return data;
        }
        return null;
    }
    
    /**
     * 获取下一分块
     */
    public ChunkData getNextChunk(String chunkId) {
        ChunkData current = getChunk(chunkId);
        if (current != null && current.getNextChunkId() != null) {
            return getChunk(current.getNextChunkId());
        }
        return null;
    }
    
    /**
     * 获取上一分块
     */
    public ChunkData getPrevChunk(String chunkId) {
        ChunkData current = getChunk(chunkId);
        if (current != null && current.getPrevChunkId() != null) {
            return getChunk(current.getPrevChunkId());
        }
        return null;
    }
    
    public String getSessionInfo(String sessionId) {
        Optional<ChunkSessionEntity> sessionOpt = chunkSessionRepository.findById(sessionId);
        if (sessionOpt.isPresent()) {
            return "会话ID: " + sessionOpt.get().getSessionId() + ", 总块数: " + sessionOpt.get().getTotalChunks();
        }
        return "会话不存在";
    }
    
    private void deleteExistingChunks(String userId, String sessionId) {
        chunkedResultRepository.deleteByUserIdAndSessionId(userId, sessionId);
        chunkSessionRepository.deleteByUserIdAndSessionId(userId, sessionId);
    }
    
    private List<ChunkSegment> splitContentWithOverlap(String content, int chunkSize, double overlapRatio) {
        List<ChunkSegment> chunks = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return chunks;
        }
        
        int contentLength = content.length();
        int overlapSize = (int) (chunkSize * overlapRatio);
        int stepSize = chunkSize - overlapSize; // 实际移动的字符数
        
        int start = 0;
        while (start < contentLength) {
            int end = Math.min(start + chunkSize, contentLength);
            
            // 尝试在句子边界处断开，避免截断句子
            if (end < contentLength) {
                int boundaryEnd = findSentenceBoundary(content, start, end);
                if (boundaryEnd > start) {
                    end = boundaryEnd;
                }
            }
            
            // 确保重叠区域不超过内容边界
            int overlapStart = Math.max(0, start - overlapSize);
            int overlapEnd = Math.min(contentLength, end + overlapSize);
            
            String chunkContent = content.substring(start, end);
            
            ChunkSegment segment = new ChunkSegment();
            segment.setContent(chunkContent);
            segment.setOriginalStart(start);
            segment.setOriginalEnd(end);
            segment.setOverlapStart(overlapStart);
            segment.setOverlapEnd(overlapEnd);
            segment.setOverlapContent(content.substring(overlapStart, overlapEnd));
            
            chunks.add(segment);
            
            // 移动到下一个块的起始位置
            start = Math.min(start + stepSize, contentLength);
            
            // 如果已经到达末尾，退出循环
            if (start >= contentLength) {
                break;
            }
        }
        
        return chunks;
    }
    
    private int findSentenceBoundary(String content, int start, int suggestedEnd) {
        String boundaryChars = chunkProperties.getBoundaryChars();
        
        // 在建议的结束位置附近寻找句子边界
        for (int i = suggestedEnd; i > start; i--) {
            char c = content.charAt(i - 1);
            if (boundaryChars.indexOf(c) >= 0) {
                return i;
            }
        }
        
        // 如果没找到句子边界，尝试在单词边界断开
        for (int i = suggestedEnd; i > start; i--) {
            char c = content.charAt(i - 1);
            if (Character.isWhitespace(c)) {
                return i;
            }
        }
        
        return suggestedEnd; // 否则就用建议的位置
    }
    
    private String generateChunkId(String sessionUuid, int sequence) {
        return sessionUuid + "_" + sequence;
    }
}