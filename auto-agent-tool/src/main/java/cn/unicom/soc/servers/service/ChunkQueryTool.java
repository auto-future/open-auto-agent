package cn.unicom.soc.servers.service;

import cn.unicom.soc.servers.common.annotation.Tool;
import cn.unicom.soc.servers.common.annotation.ToolInput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ChunkQueryTool {
    
    @Autowired
    private ChunkedResultStorageService storageService;
    
    @Tool(description = "获取下一分块内容（含上下文重叠）")
    public String getNextChunk(@ToolInput(description = "当前块ID") String currentChunkId) {
        cn.unicom.soc.servers.common.ChunkData nextChunk = storageService.getNextChunk(currentChunkId);
        if (nextChunk != null) {
            StringBuilder result = new StringBuilder();
            result.append(String.format("【第%d/%d块】\n", 
                        nextChunk.getChunkSequence() + 1, 
                        nextChunk.getTotalChunks()));
            
            // 如果有重叠内容，显示上下文提示
            if (nextChunk.getOverlapContent() != null && !nextChunk.getOverlapContent().equals(nextChunk.getContent())) {
                result.append("【上下文衔接】");
                result.append(extractOverlapContext(nextChunk));
                result.append("\n");
            }
            
            result.append(nextChunk.getContent());
            return result.toString();
        }
        return "【分块结束】已到达最后一块内容。";
    }
    
    @Tool(description = "获取上一分块内容（含上下文重叠）")
    public String getPrevChunk(@ToolInput(description = "当前块ID") String currentChunkId) {
        cn.unicom.soc.servers.common.ChunkData prevChunk = storageService.getPrevChunk(currentChunkId);
        if (prevChunk != null) {
            StringBuilder result = new StringBuilder();
            result.append(String.format("【第%d/%d块】\n", 
                        prevChunk.getChunkSequence() + 1, 
                        prevChunk.getTotalChunks()));
            
            // 如果有重叠内容，显示上下文提示
            if (prevChunk.getOverlapContent() != null && !prevChunk.getOverlapContent().equals(prevChunk.getContent())) {
                result.append("【上下文衔接】");
                result.append(extractOverlapContext(prevChunk));
                result.append("\n");
            }
            
            result.append(prevChunk.getContent());
            return result.toString();
        }
        return "【分块开始】已是第一块内容。";
    }
    
    @Tool(description = "获取指定分块内容")
    public String getSpecificChunk(@ToolInput(description = "块ID") String chunkId) {
        cn.unicom.soc.servers.common.ChunkData chunk = storageService.getChunk(chunkId);
        if (chunk != null) {
            return String.format(
                "【第%d/%d块】\n%s",
                chunk.getChunkSequence() + 1,
                chunk.getTotalChunks(),
                chunk.getContent()
            );
        }
        return "【错误】指定的块ID不存在。";
    }
    
    @Tool(description = "获取分块会话信息")
    public String getSessionInfo(@ToolInput(description = "会话ID") String sessionId) {
        return storageService.getSessionInfo(sessionId);
    }
    
    private String extractOverlapContext(cn.unicom.soc.servers.common.ChunkData chunk) {
        // 提取重叠部分的关键信息，避免显示过多重复内容
        if (chunk.getOverlapContent() != null && chunk.getContent() != null) {
            String overlap = chunk.getOverlapContent();
            String content = chunk.getContent();
            
            // 找到当前块在重叠区域中的位置
            int contentStart = overlap.indexOf(content);
            if (contentStart > 0) {
                // 显示重叠区域中当前块之前的部分
                String beforeContent = overlap.substring(0, contentStart);
                return beforeContent.length() > 100 ? 
                    beforeContent.substring(Math.max(0, beforeContent.length()-100)) + "... " : beforeContent;
            }
        }
        return "";
    }
}