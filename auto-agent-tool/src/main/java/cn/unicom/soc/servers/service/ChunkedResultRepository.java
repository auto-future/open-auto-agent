package cn.unicom.soc.servers.service;

import cn.unicom.soc.servers.entity.ChunkedResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChunkedResultRepository extends JpaRepository<ChunkedResultEntity, String> {
    
    List<ChunkedResultEntity> findByUserIdAndSessionId(@Param("userId") String userId, 
                                                      @Param("sessionId") String sessionId);
    
    void deleteByUserIdAndSessionId(@Param("userId") String userId, 
                                   @Param("sessionId") String sessionId);
    
    ChunkedResultEntity findByChunkId(@Param("chunkId") String chunkId);
    
    // 清理过期数据
    @Modifying
    @Query("DELETE FROM ChunkedResultEntity c WHERE c.createdAt < :cutoffDateTime")
    int cleanupOlderThan(@Param("cutoffDateTime") java.time.LocalDateTime cutoffDateTime);
    
    @Query("SELECT c FROM ChunkedResultEntity c WHERE c.chunkId = :chunkId")
    ChunkedResultEntity findByIdCustom(@Param("chunkId") String chunkId);
}