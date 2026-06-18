package cn.unicom.soc.servers.util;

import cn.unicom.soc.servers.service.AutoChunkInterceptor;
import cn.unicom.soc.servers.service.ChunkedResultStorageService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 分块功能测试工具
 */
@Component
public class ChunkTestUtil {

    @Autowired
    private ChunkedResultStorageService storageService;

    @Autowired
    private AutoChunkInterceptor chunkInterceptor;

    @PostConstruct
    public void testChunkFunctionality() {
        System.out.println("=== 自动分块系统测试 ===");

        // 测试长文本分块
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            longText.append("这是测试文本第").append(i + 1).append("行。");
            if (i % 20 == 0) {
                longText.append("\n"); // 模拟换行
            }
        }

        System.out.println("原始文本长度: " + longText.length());

        // 测试分块功能
        try {
            var result = storageService.storeChunkedResult("test-user", "test-session", longText.toString());
            System.out.println("分块完成，总块数: " + result.getTotalChunks());
            System.out.println("首块ID: " + result.getFirstChunkId());
            System.out.println("首块长度: " + result.getFirstChunkContent().length());
        } catch (Exception e) {
            System.err.println("分块测试失败: " + e.getMessage());
        }

        // 测试自动分块拦截器
        try {
            var interceptedResult = chunkInterceptor.processResult(longText.toString(), "test-user", "test-session-2");
            System.out.println("自动分块拦截器测试完成");
            System.out.println("返回类型: " + interceptedResult.getClass().getSimpleName());
        } catch (Exception e) {
            System.err.println("自动分块拦截器测试失败: " + e.getMessage());
        }

        System.out.println("=== 测试完成 ===");
    }
}