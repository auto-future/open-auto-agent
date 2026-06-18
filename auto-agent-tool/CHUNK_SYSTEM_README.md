# 自动化分块系统

## 系统概述

自动化分块系统是一个用于处理长文本内容的解决方案，特别适用于AI模型交互过程中产生的大量文本。系统能够自动检测长内容并将其分割成较小的块，同时保持上下文连续性。

## 核心特性

### 1. 自动检测与分块
- 当内容长度超过阈值（默认2000字符）时自动触发分块
- 无需手动干预，完全自动化处理

### 2. 重叠分块机制
- 每个分块之间有10%的重叠（可配置）
- 保持上下文连续性，避免信息断裂

### 3. 链式存储结构
- 每个分块记录前一个和后一个块的ID
- 支持前后导航，方便查阅

### 4. 数据库持久化
- 使用SQLite存储分块数据（开发模式）
- 确保数据一致性和可靠性
- 便于开发和部署

## 系统架构

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   工具调用      │───▶│ 自动分块拦截器   │───▶│ 分块存储服务    │
│ (如HTTP请求)    │    │ (AutoChunkInter- │    │ (ChunkedResult- │
│                 │    │  ceptor)        │    │  StorageService)│
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                        │
                                                        ▼
                                            ┌──────────────────┐
                                            │ 数据库 (SQLite)  │
                                            │ - chunked_results│
                                            │ - chunk_sessions │
                                            └──────────────────┘
```

## 主要组件

### 1. 核心服务层
- `ChunkedResultStorageService`: 分块存储核心服务
- `AutoChunkInterceptor`: 自动分块拦截器
- `ChunkQueryTool`: 供LLM调用的分块查询工具

### 2. 数据访问层
- `ChunkedResultRepository`: 分块结果数据访问
- `ChunkSessionRepository`: 分块会话数据访问

### 3. 实体类
- `ChunkedResultEntity`: 分块结果实体
- `ChunkSessionEntity`: 分块会话实体

### 4. 数据结构
- `ChunkSegment`: 分块片段内部表示
- `ChunkData`: 分块数据传输对象
- `ChunkResultInfo`: 分块结果信息
- `AutoChunkSummary`: 自动分块摘要

## 工作流程

1. **内容检测**: 当工具返回结果时，系统自动检测内容长度
2. **分块处理**: 超过阈值的内容被自动分块并建立重叠
3. **链式存储**: 分块数据以链式结构存储到数据库
4. **摘要返回**: 向LLM返回分块摘要和首块内容
5. **按需查询**: LLM可通过工具获取后续分块

## 配置选项

在 `application.properties` 中配置:

```properties
# 分块配置
chunk.size=2000                    # 每个块的最大大小
chunk.overlap-ratio=0.1            # 重叠比例 (10%)
chunk.threshold=2000               # 自动分块阈值
chunk.enable-overlap=true          # 是否启用重叠
chunk.boundary-chars=.!?;，。！？；\n\r  # 断句字符

# 数据库配置（SQLite）
spring.datasource.url=jdbc:sqlite:./chunk_data.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLiteDialect
```

## LLM交互工具

系统提供以下工具供LLM使用:

- `getNextChunk(chunkId)`: 获取下一分块内容（含上下文重叠）
- `getPrevChunk(chunkId)`: 获取上一分块内容（含上下文重叠）
- `getSpecificChunk(chunkId)`: 获取指定分块内容
- `getSessionInfo(sessionId)`: 获取分块会话信息

## 数据管理

- **自动清理**: 每小时清理超过24小时的分块数据
- **会话覆盖**: 每个会话只保留最新结果
- **索引优化**: 为常用查询字段建立数据库索引

## 使用场景

1. **长API响应处理**: 自动分块处理长HTTP响应
2. **文档内容处理**: 分块处理长文档内容
3. **日志数据分析**: 分块处理大量日志数据
4. **报告生成**: 分块处理生成的长报告

## 优势

1. **透明性**: 对现有工具完全透明，无需修改
2. **上下文保持**: 重叠机制保持语义连续性
3. **可扩展性**: 支持配置调整适应不同场景
4. **可靠性**: 数据库存储确保数据持久性
5. **易用性**: LLM可轻松访问任意分块内容

## 注意事项

1. 分块大小和重叠比例需要根据具体应用场景调整
2. 数据库容量需考虑长期运行的分块数据存储需求
3. 对于实时性要求高的场景，需要考虑分块处理的延迟
4. 分块边界可能会切割句子，尽管有重叠机制缓解此问题

## 性能考虑

- 分块处理时间随内容长度线性增长
- 数据库存储和检索性能受分块数量影响
- 重叠机制增加了存储空间需求（增加约10%）
- 建议定期清理过期的分块数据以优化性能

这个系统为处理长文本内容提供了完整的解决方案，特别适用于AI模型交互场景，确保了内容的完整性和可访问性。