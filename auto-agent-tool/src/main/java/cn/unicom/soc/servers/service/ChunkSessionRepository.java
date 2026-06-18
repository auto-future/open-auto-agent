package cn.unicom.soc.servers.service;

import cn.unicom.soc.servers.entity.ChunkSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChunkSessionRepository extends JpaRepository<ChunkSessionEntity, String> {
    
    void deleteByUserIdAndSessionId(@Param("userId") String userId, 
                                   @Param("sessionId") String sessionId);
    
    @Modifying
    @Query("UPDATE ChunkSessionEntity s SET s.totalChunks = :#{#entity.totalChunks}, " +
           "s.status = :#{#entity.status}, " +
           "s.firstChunkId = :#{#entity.firstChunkId}, " +
           "s.overlapRatio = :#{#entity.overlapRatio} " +
           "WHERE s.sessionId = :#{#entity.sessionId}")
    int updateBySessionId(@Param("entity") ChunkSessionEntity entity);
}