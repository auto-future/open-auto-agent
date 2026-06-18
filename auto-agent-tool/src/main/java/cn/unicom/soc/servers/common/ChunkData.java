package cn.unicom.soc.servers.common;

import lombok.Data;

@Data
public class ChunkData {
    private String chunkId;
    private String content;
    private Integer chunkSequence;
    private Integer totalChunks;
    private String prevChunkId;
    private String nextChunkId;
    private String sessionId;
    private String userId;
    private String overlapContent; // 重叠内容
}