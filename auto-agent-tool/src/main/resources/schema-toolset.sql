-- 工具集表
CREATE TABLE IF NOT EXISTS tool_sets (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    type VARCHAR(50) NOT NULL, -- internal-内部工具集, external-外部MCP Server
    tag VARCHAR(50), -- mcp, skills, http
    mcp_config TEXT, -- MCP Server JSON 配置
    custom_config TEXT, -- 自定义工具集配置
    status INTEGER NOT NULL DEFAULT 1, -- 0-禁用, 1-启用
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 工具表
CREATE TABLE IF NOT EXISTS tools (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    tool_set_id VARCHAR(36) NOT NULL,
    type VARCHAR(50) NOT NULL, -- mcp/http/custom/skills
    input_schema TEXT,
    resource_path VARCHAR(500), -- skill 目录路径
    script_content TEXT, -- skill 入口脚本内容
    status INTEGER NOT NULL DEFAULT 1, -- 0-禁用, 1-启用
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tool_set_id) REFERENCES tool_sets(id) ON DELETE CASCADE
);

-- HTTP工具配置表
CREATE TABLE IF NOT EXISTS http_tool_configs (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    tool_set_id VARCHAR(36) NOT NULL,
    method VARCHAR(10) NOT NULL, -- GET, POST, PUT, DELETE
    url VARCHAR(1000),
    headers TEXT,
    request_body_template TEXT,
    response_parsing_pattern TEXT,
    status INTEGER NOT NULL DEFAULT 1, -- 0-禁用, 1-启用
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tool_set_id) REFERENCES tool_sets(id) ON DELETE CASCADE
);

-- 索引
CREATE INDEX idx_tool_set_type ON tool_sets(type);
CREATE INDEX idx_tool_set_status ON tool_sets(status);
CREATE INDEX idx_tool_set_name ON tool_sets(name);

CREATE INDEX idx_tool_tool_set_id ON tools(tool_set_id);
CREATE INDEX idx_tool_type ON tools(type);
CREATE INDEX idx_tool_status ON tools(status);
CREATE INDEX idx_tool_name ON tools(name);

CREATE INDEX idx_http_tool_tool_set_id ON http_tool_configs(tool_set_id);
CREATE INDEX idx_http_tool_method ON http_tool_configs(method);
CREATE INDEX idx_http_tool_status ON http_tool_configs(status);
CREATE INDEX idx_http_tool_name ON http_tool_configs(name);
CREATE INDEX idx_http_tool_url ON http_tool_configs(url);