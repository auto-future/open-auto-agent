package cn.unicom.soc.servers.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "chunk_sessions")
@Data
public class ChunkSessionEntity {
    @Id
    @Column(name = "session_id")
    private String sessionId;
    
    @Column(name = "user_id")
    private String userId;
    
    @Column(name = "total_chunks")
    private Integer totalChunks;
    
    @Column(name = "status")
    private String status; // processing, completed
    
    @Column(name = "first_chunk_id")
    private String firstChunkId;
    
    @Column(name = "overlap_ratio") // 重叠比例
    private Double overlapRatio;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}