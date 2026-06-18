package cn.unicom.soc.servers.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "chunked_results")
@Data
public class ChunkedResultEntity {
    @Id
    @Column(name = "chunk_id")
    private String chunkId;
    
    @Column(name = "user_id")
    private String userId;
    
    @Column(name = "session_id")
    private String sessionId;
    
    @Column(name = "total_chunks")
    private Integer totalChunks;
    
    @Column(name = "chunk_sequence")
    private Integer chunkSequence;
    
    @Column(length = 10000, columnDefinition = "TEXT")  // SQLite对TEXT的支持
    private String content;
    
    @Column(name = "prev_chunk_id")
    private String prevChunkId;
    
    @Column(name = "next_chunk_id")
    private String nextChunkId;
    
    @Column(name = "overlap_start")     // 重叠区域起始位置
    private Integer overlapStart;
    
    @Column(name = "overlap_end")       // 重叠区域结束位置
    private Integer overlapEnd;
    
    @Column(name = "overlap_content")    // 重叠区域内容
    private String overlapContent;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}