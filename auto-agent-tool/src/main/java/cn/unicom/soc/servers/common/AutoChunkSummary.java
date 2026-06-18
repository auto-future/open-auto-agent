package cn.unicom.soc.servers.common;

import lombok.Data;

@Data
public class AutoChunkSummary {
    private String summaryType = "AUTO_CHUNKED_RESULT";
    private String sessionId;
    private int totalChunks;
    private String firstChunkId;
    private String firstChunkContent;
    private double overlapRatio; // 重叠比例
    private String instruction;
    
    public AutoChunkSummary(String sessionId, int totalChunks, 
                           String firstChunkId, String firstChunkContent, 
                           double overlapRatio) {
        this.sessionId = sessionId;
        this.totalChunks = totalChunks;
        this.firstChunkId = firstChunkId;
        this.firstChunkContent = firstChunkContent;
        this.overlapRatio = overlapRatio;
        this.instruction = String.format(
            "此结果已被自动分块（重叠率%.0f%%）。共%d块，当前显示第1块。如需查看后续内容，请使用get_next_chunk工具，重叠部分有助于保持上下文连续性。",
            overlapRatio * 100, totalChunks
        );
    }
}