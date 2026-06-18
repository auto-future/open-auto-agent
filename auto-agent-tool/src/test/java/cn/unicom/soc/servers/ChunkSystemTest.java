package cn.unicom.soc.servers;

import cn.unicom.soc.servers.service.ChunkedResultStorageService;
import cn.unicom.soc.servers.service.AutoChunkInterceptor;
import cn.unicom.soc.servers.common.ChunkData;
import cn.unicom.soc.servers.common.ChunkResultInfo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SpringMcpServersApplication.class)
@TestPropertySource(properties = {
    "chunk.size=500",
    "chunk.overlap-ratio=0.1",
    "chunk.threshold=200"
})
class ChunkSystemTest {

    @org.springframework.beans.factory.annotation.Autowired
    private ChunkedResultStorageService chunkedResultStorageService;

    @org.springframework.beans.factory.annotation.Autowired
    private AutoChunkInterceptor autoChunkInterceptor;

    @Test
    void testChunkCreation() {
        // 创建一个较长的测试文本
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longText.append("这是测试文本的第").append(i + 1).append("行内容。这一行包含一些有意义的信息和内容，用来测试分块功能。 ");
        }

        String content = longText.toString();

        // 测试分块存储
        ChunkResultInfo resultInfo = chunkedResultStorageService.storeChunkedResult("test-user", "test-session", content);

        assertNotNull(resultInfo);
        assertTrue(resultInfo.getTotalChunks() > 1); // 应该分成多个块
        assertNotNull(resultInfo.getFirstChunkId());
        assertNotNull(resultInfo.getFirstChunkContent());

        System.out.println("分块总数: " + resultInfo.getTotalChunks());
        System.out.println("首块ID: " + resultInfo.getFirstChunkId());
        System.out.println("首块长度: " + resultInfo.getFirstChunkContent().length());

        // 测试获取分块
        ChunkData firstChunk = chunkedResultStorageService.getChunk(resultInfo.getFirstChunkId());
        assertNotNull(firstChunk);
        assertEquals(resultInfo.getFirstChunkId(), firstChunk.getChunkId());
    }

    @Test
    void testAutoChunkInterceptor() {
        // 创建一个较长的测试文本
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longText.append("这是用于测试自动分块拦截器的文本内容，第").append(i + 1).append("行。 ");
        }

        String content = longText.toString();

        // 测试自动分块拦截器
        Object result = autoChunkInterceptor.processResult(content, "test-user", "test-session");

        // 验证返回的是分块摘要而不是原始内容
        assertTrue(result instanceof cn.unicom.soc.servers.common.AutoChunkSummary, 
                   "结果应该是AutoChunkSummary类型");
        
        cn.unicom.soc.servers.common.AutoChunkSummary summary = 
            (cn.unicom.soc.servers.common.AutoChunkSummary) result;
        
        assertNotNull(summary.getSessionId());
        assertTrue(summary.getTotalChunks() > 0);
        assertNotNull(summary.getFirstChunkId());
        assertNotNull(summary.getFirstChunkContent());
        
        System.out.println("自动分块摘要 - 会话ID: " + summary.getSessionId() + 
                          ", 总块数: " + summary.getTotalChunks());
    }

    @Test
    void testChunkNavigation() {
        // 创建一个较长的测试文本
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 80; i++) {
            longText.append("这是测试分块导航功能的文本内容，第").append(i + 1).append("行。这部分内容会帮助验证前后分块的导航功能。 ");
        }

        String content = longText.toString();

        // 存储分块
        ChunkResultInfo resultInfo = chunkedResultStorageService.storeChunkedResult("test-user", "nav-test-session", content);

        // 获取首块
        ChunkData firstChunk = chunkedResultStorageService.getChunk(resultInfo.getFirstChunkId());
        assertNotNull(firstChunk);

        // 测试获取下一块（如果存在）
        if (firstChunk.getNextChunkId() != null) {
            ChunkData nextChunk = chunkedResultStorageService.getNextChunk(firstChunk.getNextChunkId());
            assertNotNull(nextChunk);
            assertEquals(firstChunk.getNextChunkId(), nextChunk.getChunkId());
        }

        // 测试获取上一块（应该回到当前块）
        if (firstChunk.getPrevChunkId() != null) {
            ChunkData prevChunk = chunkedResultStorageService.getPrevChunk(firstChunk.getChunkId());
            assertNotNull(prevChunk);
        }
    }
}