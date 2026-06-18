# AI工具治理平台 (AutoAgent)

## 项目简介

AI工具治理平台是一个统一治理AI工具全生命周期的综合性解决方案。平台以AI为核心，以工具为边界，支持对MCP工具、HTTP API工具等进行可视化配置、动态扩展和全生命周期管理。所有外部AI操作均由AI基于上下文进行推理，然后调用相应工具执行并完成任务。

平台提供Web可视化管理界面，支持工具集的分类组织、工具的参数配置、大模型的接入管理，以及基于AI的智能参数生成，帮助开发者和运维人员高效管理和调度AI工具资源。

## 核心特性

- **统一工具治理**：统一管理MCP和HTTP工具集，支持工具的分类组织和版本管理
- **可视化配置**：提供Web界面进行拖拽式工具配置，降低使用门槛
- **动态扩展**：支持热插拔工具插件，运行时动态注册和卸载工具
- **大模型管理**：支持配置和管理多种AI大模型，用于参数生成和智能辅助
- **智能工具测试**：基于大模型智能生成工具测试参数，支持多种生成策略
- **自动化分块**：内置自动化分块系统，智能处理长文本内容，保持上下文连续性
- **实时监控**：工具调用状态监控、性能统计和调用日志追踪

## 功能模块

### 1. 总览面板
- 平台整体运行状态监控
- 工具集/工具/大模型数量统计
- 调用趋势图表和工具类型分布
- 最近调用日志和系统状态展示

### 2. 工具集管理
- 创建、编辑、删除工具集
- 支持内部/外部工具集分类
- 按来源、标签筛选和搜索
- 工具集详情查看和关联工具管理

### 3. 工具管理
- 管理单个工具的配置参数
- 支持MCP、HTTP、Skills等多种工具类型
- 工具测试和调试
- 按类型和工具集筛选查询

### 4. 大模型管理
- 配置和管理AI大模型接入
- 支持多种模型提供商
- 模型参数配置和状态监控

### 5. 智能工具测试
- 基于JSON Schema智能生成测试参数
- 支持多种生成策略：真实业务数据、边界值测试、全面覆盖
- 可视化配置和结果展示

### 6. 自动化分块系统
- 自动检测长内容并智能分块
- 重叠分块机制保持上下文连续性
- 链式存储结构支持前后导航
- LLM可通过工具按需查询分块内容

## 技术架构

```
┌─────────────────────────────────────────────────────────────┐
│                      前端管理层 (Web UI)                     │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐  │
│  │ 总览面板 │ │ 工具集管理│ │ 工具管理 │ │ 大模型管理   │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────┘  │
│  ┌──────────┐ ┌──────────────────────────────────────────┐  │
│  │ 智能工具测试│ │         自动化分块系统                   │  │
│  └──────────┘ └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                      服务治理层 (Spring Boot)                │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────────────┐ │
│  │ ToolSetService│ │ HttpToolExecutor│ │ McpToolRegistry   │ │
│  └──────────────┘ └──────────────┘ └──────────────────────┘ │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────────────┐ │
│  │ ChunkStorage │ │ AutoChunk    │ │ HttpApiManager       │ │
│  │  Service     │ │ Interceptor  │ │ Service              │ │
│  └──────────────┘ └──────────────┘ └──────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                      数据存储层                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              SQLite (统一单文件数据库)                │  │
│  │  - chunked_results / chunk_sessions (分块数据)       │  │
│  │  - tool_sets / tools / http_tool_configs (JPA管理)   │  │
│  │  - http_tools (原生JDBC管理)                          │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 4.1.0 | 核心框架 |
| Spring AI | 2.0.0 | MCP协议支持和AI工具集成 |
| Spring Data JPA | - | 数据持久化 |
| MyBatis-Plus | 3.5.3.2 | ORM增强 |
| SQLite | 3.42.0.0 | 嵌入式数据库 |
| Hibernate | - | ORM框架 |
| Lombok | - | 代码简化 |
| Jackson | - | JSON处理 |
| Bootstrap 5 | 5.3.2 | 前端UI框架 |
| ECharts | 5.4.3 | 数据可视化 |

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+

### 构建与运行

```bash
# 克隆项目
git clone <repository-url>
cd open-auto-agent/auto-agent-tool

# 编译打包
mvn clean package

# 运行应用
mvn spring-boot:run
# 或直接运行jar
java -jar target/auto-agent-tool-*.jar
```

应用启动后，访问 http://localhost:17000 直接进入AI工具治理平台。

### 访问管理界面

- **AI工具治理平台（首页）**: http://localhost:17000
- **API文档**: http://localhost:17000/api-docs.html
- **工具集管理**: http://localhost:17000/tool-set-manager.html

## 项目结构

```
auto-agent-tool/
├── src/main/java/cn/unicom/soc/servers/
│   ├── SpringMcpServersApplication.java    # 应用入口
│   ├── common/                             # 公共类和注解
│   │   ├── annotation/                     # 自定义注解 (@Tool, @Resource, @Prompt)
│   │   ├── AutoChunkSummary.java           # 自动分块摘要
│   │   ├── ChunkData.java                  # 分块数据
│   │   └── ChunkSegment.java               # 分块片段
│   ├── config/                             # 配置类
│   ├── controller/                         # REST API控制器
│   │   ├── HttpToolController.java         # HTTP工具管理接口
│   │   ├── ToolController.java             # 工具管理接口
│   │   └── ToolSetController.java          # 工具集管理接口
│   ├── dto/                                # 数据传输对象
│   ├── entity/                             # 实体类
│   ├── repository/                         # JPA仓库接口
│   └── service/                            # 业务服务层
│       ├── AutoChunkInterceptor.java       # 自动分块拦截器
│       ├── ChunkedResultStorageService.java # 分块存储服务
│       ├── HttpToolExecutorService.java    # HTTP工具执行服务
│       ├── HttpToolRegistryService.java    # HTTP工具注册服务
│       ├── McpToolRegistryService.java     # MCP工具注册服务
│       └── ToolSetService.java             # 工具集管理服务
├── src/main/resources/
│   ├── static/                             # 静态资源
│   │   ├── index.html                      # AI工具治理平台首页
│   │   ├── tool-set-manager.html           # 工具集管理页面
│   │   ├── api-docs.html                   # API文档页面
│   │   ├── css/ai-tool-governance.css      # 样式文件
│   │   └── js/ai-tool-governance.js        # 前端脚本
│   ├── application.properties              # 应用配置
│   ├── schema-chunk.sql                    # 分块系统数据库脚本
│   └── schema-toolset.sql                  # 工具集数据库脚本
└── pom.xml                                 # Maven配置
```

## 配置说明

### 应用配置 (application.properties)

```properties
# 服务端口
server.port=17000

# MCP服务器配置
spring.ai.mcp.server.enabled=true
spring.ai.mcp.server.name=attack-trace-mcp-server
spring.ai.mcp.server.version=1.0.0
spring.ai.mcp.server.protocol=sse

# 分块配置
chunk.size=2000                    # 每个块的最大大小
chunk.overlap-ratio=0.1            # 重叠比例 (10%)
chunk.threshold=2000               # 自动分块阈值
chunk.enable-overlap=true          # 是否启用重叠
chunk.boundary-chars=.!?;，。！？；\n\r  # 断句字符

# 数据库配置 (统一SQLite单文件)
spring.datasource.url=jdbc:sqlite:./data/app.db
spring.datasource.driver-class-name=org.sqlite.JDBC
sqlite.db.path=./data/app.db
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.community.dialect.SQLiteDialect
```

## 使用指南

### 1. 管理工具集

进入"工具集管理"页面，可以：
- 点击"新建工具集"创建新的工具集分类
- 设置工具集名称、描述、标签和来源类型
- 查看工具集详情和关联的工具列表

### 2. 配置HTTP工具

通过HTTP工具管理接口或页面，可以动态注册HTTP API：
- 配置API地址、请求方法、参数定义
- 设置认证信息和请求头
- 测试工具调用并查看响应

### 3. 使用智能工具测试

1. 进入"智能工具测试"页面
2. 选择已配置的大模型
3. 输入JSON Schema定义参数结构
4. 选择生成策略（真实业务数据/边界值测试/全面覆盖）
5. 点击"AI生成测试参数"获取智能生成的参数

### 4. 分块系统使用

分块系统对现有工具完全透明，当工具返回结果超过阈值时自动触发：
- 系统自动将长内容分块并存储
- 向AI返回分块摘要和首块内容
- AI可通过 `getNextChunk`、`getPrevChunk` 等工具按需获取后续内容

## 相关文档

- [自动化分块系统说明](auto-agent-tool/CHUNK_SYSTEM_README.md)
- [分块系统使用示例](auto-agent-tool/EXAMPLE_USAGE.md)

## 贡献指南

欢迎提交Issue和Pull Request来改进项目。

## 许可证

[LICENSE](LICENSE)
