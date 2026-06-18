package cn.unicom.soc.servers.common;

import lombok.Data;

@Data
public class ChunkSegment {
    private String content;           // 当前块的主要内容
    private int originalStart;        // 原始内容中的起始位置
    private int originalEnd;          // 原始内容中的结束位置
    private int overlapStart;         // 重叠区域起始位置
    private int overlapEnd;           // 重叠区域结束位置
    private String overlapContent;    // 重叠区域内容
    private boolean isOverlapStart;   // 是否是重叠开始部分
    private boolean isOverlapEnd;     // 是否是重叠结束部分
}