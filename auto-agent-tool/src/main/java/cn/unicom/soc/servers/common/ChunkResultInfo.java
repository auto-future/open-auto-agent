package cn.unicom.soc.servers.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChunkResultInfo {
    private String sessionId;
    private int totalChunks;
    private String firstChunkId;
    private String firstChunkContent;
}