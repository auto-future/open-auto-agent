# 自动化分块系统使用示例

## 1. 基本分块处理

### 示例1：HTTP请求结果自动分块

当使用增强的HTTP工具发起请求时，系统会自动处理长响应：

```java
// 发起一个返回长结果的HTTP请求
Object result = enhancedHttpRequest(
    "https://api.example.com/large-document", 
    "GET", 
    null, 
    "user123", 
    "session456"
);

// 如果响应内容超过阈值，result将是一个AutoChunkSummary对象
if (result instanceof AutoChunkSummary) {
    AutoChunkSummary summary = (AutoChunkSummary) result;
    System.out.println("内容已被分块，共" + summary.getTotalChunks() + "块");
    System.out.println("首块内容：" + summary.getFirstChunkContent());
    System.out.println("使用 getNextChunk 工具获取后续内容");
}
```


## 2. LLM交互示例

### 场景：AI模型处理长文档摘要

```
用户: 请分析这份安全报告的内容

系统: 
【AUTO_CHUNKED_RESULT】
此结果已被自动分块（重叠率10%）。共5块，当前显示第1块。
如需查看后续内容，请使用get_next_chunk工具，重叠部分有助于保持上下文连续性。

【第1/5块】
安全报告摘要：
1. 系统概览
   - 检测到异常活动：...
   - 攻击时间线：...
   - 受影响系统：...

2. 威胁分析
   - 攻击类型：...
   - 攻击向量：...
   - 影响范围：...

[内容截断，使用get_next_chunk获取后续内容]

AI: 我看到这是一个被自动分块的长报告，共有5块。我已分析了第一块的内容，包含系统概览和威胁分析。
我可以使用get_next_chunk工具获取更多信息。请允许我获取下一块内容。

工具调用: getNextChunk("chunk_id_from_first_block")

工具响应:
【第2/5块】
3. 受影响资产详情
   - 服务器A：IP 192.168.1.100，操作系统...
   - 服务器B：IP 192.168.1.101，操作系统...
   
4. 攻击手法分析
   - 初始入侵点：...
   - 横向移动路径：...
   - 数据窃取方式：...

AI: 现在我获得了第二块内容，包含了受影响资产详情和攻击手法分析。
让我继续获取下一块以获得完整的修复建议...
```

## 3. 配置调整示例

### 针对不同场景的配置

```properties
# 场景1：技术文档处理（需要更多上下文）
chunk.size=1000
chunk.overlap-ratio=0.2  # 更大的重叠比例
chunk.threshold=1000     # 更低的阈值

# 场景2：日志分析（较短的块）
chunk.size=500
chunk.overlap-ratio=0.1
chunk.threshold=500

# 场景3：代码分析（需要保持语法完整性）
chunk.size=800
chunk.overlap-ratio=0.15
chunk.boundary-chars={,},;,\n  # 在代码边界处分割
```

## 4. 自定义边界字符示例

### 处理特定格式内容

```java
// 对于JSON数据，可以设置特殊边界字符
@Component
public class JsonChunkProcessor {
    
    public void processJsonData(String jsonData) {
        // 系统会在JSON对象边界处分割，避免破坏JSON结构
        // 例如：在 } 和 { 之间，或在 , 之后分割
    }
}

// 对于编程代码
@Component 
public class CodeChunkProcessor {
    
    public void processCode(String code) {
        // 系统会在函数定义、类定义等边界处分割
        // 保持代码语法完整性
    }
}
```

## 5. 错误处理示例

### 分块处理异常情况

```java
try {
    Object result = autoChunkInterceptor.processResult(longContent, userId, sessionId);
} catch (Exception e) {
    // 如果分块过程出现问题，系统会返回原始内容
    log.warn("自动分块处理失败，返回原始内容", e);
    // 仍可正常使用原始内容
}
```

## 6. 性能监控示例

### 监控分块系统性能

```java
@Component
public class ChunkPerformanceMonitor {
    
    @EventListener
    public void onChunkOperation(ChunkOperationEvent event) {
        if (event.getOperationType() == OperationType.STORE) {
            log.info("分块存储完成，内容长度: {}, 分块数: {}, 耗时: {}ms", 
                     event.getContentLength(),
                     event.getChunkCount(), 
                     event.getDuration());
        }
    }
}
```

## 7. 批量处理示例

### 同时处理多个长文档

```java
@Service
public class BatchChunkProcessor {
    
    public List<ChunkResultInfo> processBatch(List<String> documents) {
        return documents.parallelStream()
            .map(doc -> storageService.storeChunkedResult("batch-user", 
                     generateSessionId(), doc))
            .collect(Collectors.toList());
    }
}
```

## 8. 与现有系统集成示例

### 在现有工具中集成分块功能

```java
@Component
public class LegacyToolAdapter {
    
    @Autowired
    private AutoChunkInterceptor chunkInterceptor;
    
    // 包装现有的工具方法
    public Object enhancedLegacyTool(String input) {
        Object result = legacyTool.process(input);  // 原有逻辑
        
        // 应用自动分块
        return chunkInterceptor.processResult(
            result, 
            getCurrentUserId(), 
            getCurrentSessionId()
        );
    }
}
```

这些示例展示了自动化分块系统在各种场景下的应用方式，从基本的HTTP响应处理到复杂的AI交互场景，系统都能提供一致且高效的分块服务。