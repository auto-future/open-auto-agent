// ==================== AI工具治理平台 - 管理系统核心逻辑 ====================

const API_BASE = '/api/tool-sets';
const TOOLS_API_BASE = '/api/tools';
const MODELS_API_BASE = '/api/models';
const MONITOR_API_BASE = '/api/monitor';
const MODELS_STORAGE_KEY = 'ai_models';

// ==================== 初始化 ====================

document.addEventListener('DOMContentLoaded', function() {
    loadSets();
    loadTools();
    loadToolSetFilter();
    loadModels();
    updateDashboardStats();
    initDashboardCharts();
});

// ==================== 页面切换 ====================

function switchPage(page) {
    // 更新侧边栏激活状态
    document.querySelectorAll('.sidebar-item').forEach(item => {
        item.classList.toggle('active', item.dataset.page === page);
    });
    
    // 切换页面内容
    document.querySelectorAll('.page-section').forEach(section => {
        section.classList.remove('active');
    });
    document.getElementById(`page-${page}`).classList.add('active');
    
    // 更新面包屑标题
    const titles = {
        'dashboard': '总览面板',
        'sets': '工具集管理',
        'tools': '工具管理',
        'models': '大模型管理',
        'ai-assistant': '智能工具测试',
        'http-test': 'HTTP接口测试'
    };
    document.getElementById('pageTitle').textContent = titles[page] || page;
    
    // 如果是总览面板，刷新图表
    if (page === 'dashboard') {
        updateDashboardStats();
    }
}

function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    const mainWrapper = document.getElementById('mainWrapper');
    const toggleIcon = document.getElementById('toggleIcon');
    
    sidebar.classList.toggle('sidebar-collapsed');
    mainWrapper.classList.toggle('main-wrapper-collapsed');
    toggleIcon.textContent = sidebar.classList.contains('sidebar-collapsed') ? '▶' : '◀';
}

function refreshCurrentPage() {
    const activePage = document.querySelector('.page-section.active');
    if (!activePage) return;
    
    const pageId = activePage.id.replace('page-', '');
    
    switch(pageId) {
        case 'dashboard': updateDashboardStats(); break;
        case 'sets': loadSets(); break;
        case 'tools': loadTools(); break;
        case 'models': loadModels(); break;
    }
    
    showToast('刷新成功', 'success');
}

// ==================== Dashboard 总览面板 ====================

async function updateDashboardStats() {
    try {
        const statsRes = await fetch(`${MONITOR_API_BASE}/dashboard-stats`);
        const stats = await statsRes.json();
        document.getElementById('dashTotalSets').textContent = stats.totalSets || 0;
        document.getElementById('dashTotalTools').textContent = stats.totalTools || 0;
        document.getElementById('dashTodayCalls').textContent = stats.todayCalls || 0;
    } catch (error) {
        document.getElementById('dashTotalSets').textContent = '0';
        document.getElementById('dashTotalTools').textContent = '0';
        document.getElementById('dashTodayCalls').textContent = '0';
    }

    // 模型数量（本地存储）
    try {
        const modelsRes = await fetch(MODELS_API_BASE);
        if (modelsRes.ok) {
            const models = await modelsRes.json();
            document.getElementById('dashTotalModels').textContent = models.length || 0;
        } else {
            const models = JSON.parse(localStorage.getItem(MODELS_STORAGE_KEY) || '[]');
            document.getElementById('dashTotalModels').textContent = models.length;
        }
    } catch (error) {
        const models = JSON.parse(localStorage.getItem(MODELS_STORAGE_KEY) || '[]');
        document.getElementById('dashTotalModels').textContent = models.length;
    }

    // 渲染日志和状态
    renderDashLogs();
    renderDashStatus();
}

async function initDashboardCharts() {
    // 调用趋势图
    const trendChart = echarts.init(document.getElementById('dashTrendChart'));

    let trendData = [];
    try {
        const res = await fetch(`${MONITOR_API_BASE}/call-trend`);
        trendData = await res.json();
    } catch (e) {
        trendData = Array.from({length: 24}, (_, i) => ({ hour: i + ':00', count: 0 }));
    }

    trendChart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: trendData.map(d => d.hour), axisLine: {lineStyle: {color: '#d9d9d9'}}, axisLabel: {color: '#8c8c8c'} },
        yAxis: { type: 'value', axisLine: {show: false}, splitLine: {lineStyle: {color: '#f0f0f0'}}, axisLabel: {color: '#8c8c8c'} },
        series: [{
            data: trendData.map(d => d.count),
            type: 'line',
            smooth: true,
            lineStyle: {color: '#1890ff', width: 3},
            areaStyle: {color: {type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{offset: 0, color: 'rgba(24,144,255,0.3)'}, {offset: 1, color: 'rgba(24,144,255,0.05)'}]}},
            itemStyle: {color: '#1890ff'}
        }],
        grid: {left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true}
    });

    // 工具分布饼图
    const distChart = echarts.init(document.getElementById('dashToolDistChart'));

    let distData = [];
    try {
        const res = await fetch(`${MONITOR_API_BASE}/tool-distribution`);
        distData = await res.json();
    } catch (e) {
        distData = [{type: 'MCP', count: 0}, {type: 'HTTP', count: 0}, {type: 'custom', count: 0}];
    }

    const colorMap = { 'mcp': '#1890ff', 'http': '#52c41a', 'custom': '#722ed1', 'unknown': '#8c8c8c' };
    distChart.setOption({
        tooltip: {trigger: 'item'},
        legend: {bottom: 0},
        series: [{
            type: 'pie',
            radius: ['40%', '70%'],
            avoidLabelOverlap: false,
            itemStyle: {borderRadius: 8, borderColor: '#fff', borderWidth: 2},
            label: {show: false},
            data: distData.map(d => ({
                value: d.count,
                name: d.type ? d.type.toUpperCase() : '未知'
            })),
            color: distData.map(d => colorMap[d.type] || '#8c8c8c')
        }]
    });

    window.addEventListener('resize', () => {
        trendChart.resize();
        distChart.resize();
    });
}

// ==================== Dashboard 日志和状态 ====================

async function renderDashLogs() {
    const container = document.getElementById('dashLogsContainer');
    if (!container) return;

    let logs = [];
    try {
        const res = await fetch(`${MONITOR_API_BASE}/recent-logs`);
        logs = await res.json();
    } catch (e) {
        logs = [];
    }

    if (logs.length === 0) {
        container.innerHTML = `
            <div class="empty-state" style="padding: 20px;">
                <div class="empty-text">暂无调用日志</div>
            </div>
        `;
        return;
    }

    container.innerHTML = logs.map(log => `
        <div class="log-entry ${log.status}">
            <div class="log-entry-header">
                <span class="log-entry-tool">${log.toolName || '未知工具'}</span>
                <span class="log-entry-status ${log.status}">${log.status === 'success' ? '成功' : log.status === 'error' ? '失败' : '警告'}</span>
            </div>
            <div class="log-entry-detail">
                <span>${log.message || '执行成功'}</span>
                <span>${log.duration || 0}ms · ${log.time ? new Date(log.time).toLocaleString('zh-CN') : ''}</span>
            </div>
        </div>
    `).join('');
}

function filterDashLogs(type) {
    const entries = document.querySelectorAll('.log-entry');
    entries.forEach(entry => {
        if (type === 'all') {
            entry.style.display = '';
        } else {
            entry.style.display = entry.classList.contains(type) ? '' : 'none';
        }
    });
}

async function renderDashStatus() {
    const container = document.getElementById('dashStatusList');
    if (!container) return;

    let statuses = [];
    try {
        const res = await fetch(`${MONITOR_API_BASE}/system-status`);
        statuses = await res.json();
    } catch (e) {
        statuses = [
            { name: 'API服务', status: 'online', value: '运行中' },
            { name: '数据库连接', status: 'offline', value: '检测失败' }
        ];
    }

    container.innerHTML = `
        <div class="status-list">
            ${statuses.map(s => `
                <div class="status-item">
                    <div class="status-item-label">
                        <span class="status-dot ${s.status}"></span>
                        ${s.name}
                    </div>
                    <span class="status-item-value">${s.value}</span>
                </div>
            `).join('')}
        </div>
    `;
}

// ==================== 工具集管理 ====================

let _allSets = [];
let _currentDetailId = null;

async function loadSets() {
    try {
        const response = await fetch(API_BASE);
        _allSets = await response.json();
        renderSets(_allSets);
    } catch (error) {
        _allSets = [];
        renderSets([]);
    }
}

function renderSets(sets) {
    const container = document.getElementById('setsContainer');
    
    if (!sets || sets.length === 0) {
        container.innerHTML = `
            <div class="empty-state" style="grid-column: 1 / -1;">
                <div class="empty-icon">📦</div>
                <div class="empty-title">暂无工具集</div>
                <div class="empty-text">点击"新建工具集"创建您的第一个工具集</div>
            </div>
        `;
        return;
    }
    
    container.innerHTML = sets.map(set => `
        <div class="col-md-6 col-lg-4">
            <div class="set-card h-100" data-id="${set.id}" onclick="viewSetDetail('${set.id}')" style="cursor: pointer;">
                    <div class="tool-card-header">
                    <div>
                        <div class="tool-card-title">${set.name}</div>
                        <div class="tag-list" style="margin-top: 6px;">
                            <span class="tool-type-tag ${set.type}">${getTypeLabel(set.type)}</span>
                            ${set.tag ? `<span class="tag-item ${set.tag}">${getTagLabel(set.tag)}</span>` : ''}
                        </div>
                    </div>
                    <span class="badge ${set.status === 1 ? 'bg-success-subtle text-success' : 'bg-secondary-subtle text-secondary'}">
                        ${set.status === 1 ? '启用' : '禁用'}
                    </span>
                </div>
                <div class="tool-card-desc">${set.description || '暂无描述'}</div>
                <div class="tool-card-footer">
                    <span class="badge bg-primary-subtle text-primary">${set.tools?.length || 0} 工具</span>
                    <div class="d-flex gap-2">
                        <button class="btn btn-sm btn-outline-secondary" onclick="event.stopPropagation(); viewSetDetail('${set.id}')">详情</button>
                        <button class="btn btn-sm btn-outline-secondary" onclick="event.stopPropagation(); editSet('${set.id}')">编辑</button>
                        ${set.type !== 'internal' ? `<button class="btn btn-sm btn-danger" onclick="event.stopPropagation(); deleteSet('${set.id}')">删除</button>` : ''}
                    </div>
                </div>
            </div>
        </div>
    `).join('');
}

function filterSets() {
    const type = document.getElementById('filterSetType').value;
    const tag = document.getElementById('filterSetTag').value;
    const name = document.getElementById('searchSetName').value.toLowerCase();
    
    let filtered = _allSets;
    if (type) filtered = filtered.filter(s => s.type === type);
    if (tag) filtered = filtered.filter(s => s.tag === tag);
    if (name) filtered = filtered.filter(s => s.name.toLowerCase().includes(name));
    
    renderSets(filtered);
}

function viewSetDetail(id) {
    const set = _allSets.find(s => s.id === id);
    if (!set) return;
    
    // 切换视图：隐藏列表，显示详情
    document.getElementById('setsListView').style.display = 'none';
    document.getElementById('setDetailPage').style.display = 'block';
    
    // 设置标题栏
    document.getElementById('detailPageTitle').textContent = set.name;
    const subtitleParts = [];
    subtitleParts.push(getTypeLabel(set.type));
    if (set.tag) subtitleParts.push(getTagLabel(set.tag));
    subtitleParts.push(set.status === 1 ? '启用' : '禁用');
    document.getElementById('detailPageSubTitle').textContent = subtitleParts.join(' · ');
    
    // 设置操作按钮（内部工具集不显示删除按钮）
    document.getElementById('detailEditBtn').onclick = function() { editSet(id); };
    if (set.type !== 'internal') {
        document.getElementById('detailDeleteBtn').style.display = '';
        document.getElementById('detailDeleteBtn').onclick = function() { deleteSet(id); };
    } else {
        document.getElementById('detailDeleteBtn').style.display = 'none';
    }
    
    // 构建配置信息展示
    let configHtml = '';
    if (set.type === 'external' && set.mcpConfig) {
        try {
            const config = typeof set.mcpConfig === 'string' ? JSON.parse(set.mcpConfig) : set.mcpConfig;
            configHtml = `
                <div class="card mb-3">
                    <div class="card-header bg-white fw-semibold">⚙️ MCP Server 配置</div>
                    <div class="card-body">
                        <pre class="mb-0" style="max-height:300px;overflow:auto;font-size:13px;">${JSON.stringify(config, null, 2)}</pre>
                    </div>
                </div>
            `;
        } catch (e) {
            configHtml = `
                <div class="card mb-3">
                    <div class="card-header bg-white fw-semibold">⚙️ MCP Server 配置</div>
                    <div class="card-body">
                        <pre class="mb-0" style="max-height:300px;overflow:auto;font-size:13px;">${set.mcpConfig}</pre>
                    </div>
                </div>
            `;
        }
    }
    
    // 构建工具列表（包含 tools 和 httpTools）
    const tools = set.tools || [];
    const httpTools = set.httpTools || [];
    const allTools = [
        ...tools.map(t => ({ ...t, toolType: t.type || 'MCP' })),
        ...httpTools.map(t => ({ ...t, toolType: 'HTTP' }))
    ];
    
    let toolsHtml = '';
    if (allTools.length > 0) {
        toolsHtml = `
            <div class="card mb-3">
                <div class="card-header bg-white fw-semibold">🔧 工具列表 (${allTools.length})</div>
                <div class="list-group list-group-flush">
                    ${allTools.map(tool => `
                        <div class="list-group-item d-flex justify-content-between align-items-center">
                            <div>
                                <div class="fw-semibold">${tool.name}</div>
                                <div class="small text-muted">${tool.description || '暂无描述'}</div>
                            </div>
                            <div class="d-flex gap-2">
                                <span class="badge ${tool.status === 1 ? 'bg-success-subtle text-success' : 'bg-secondary-subtle text-secondary'}">${tool.status === 1 ? '启用' : '禁用'}</span>
                                <span class="badge bg-primary-subtle text-primary">${tool.toolType}</span>
                            </div>
                        </div>
                    `).join('')}
                </div>
            </div>
        `;
    } else {
        toolsHtml = `
            <div class="card mb-3">
                <div class="card-header bg-white fw-semibold">🔧 工具列表</div>
                <div class="card-body text-muted">暂无工具</div>
            </div>
        `;
    }
    
    // 构建基本信息
    const basicInfo = `
        <div class="card mb-3">
            <div class="card-header bg-white fw-semibold">📋 基本信息</div>
            <div class="card-body">
                <table class="table table-borderless mb-0">
                    <tr><td class="text-muted" style="width:120px;">名称</td><td>${set.name}</td></tr>
                    <tr><td class="text-muted">来源</td><td><span class="tool-type-tag ${set.type}">${getTypeLabel(set.type)}</span></td></tr>
                    ${set.tag ? `<tr><td class="text-muted">标签</td><td><span class="tag-item ${set.tag}">${getTagLabel(set.tag)}</span></td></tr>` : ''}
                    <tr><td class="text-muted">状态</td><td><span class="badge ${set.status === 1 ? 'bg-success-subtle text-success' : 'bg-secondary-subtle text-secondary'}">${set.status === 1 ? '启用' : '禁用'}</span></td></tr>
                    ${set.createdAt ? `<tr><td class="text-muted">创建时间</td><td>${new Date(set.createdAt).toLocaleString('zh-CN')}</td></tr>` : ''}
                </table>
            </div>
        </div>
    `;
    
    // 构建描述
    const descHtml = `
        <div class="card mb-3">
            <div class="card-header bg-white fw-semibold">📝 描述</div>
            <div class="card-body">
                <p class="text-muted mb-0">${set.description || '暂无描述'}</p>
            </div>
        </div>
    `;
    
    document.getElementById('setDetailContent').innerHTML = basicInfo + descHtml + configHtml + toolsHtml;
    
    // 滚动到顶部
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function backToSetsList() {
    document.getElementById('setsListView').style.display = 'block';
    document.getElementById('setDetailPage').style.display = 'none';
}

// ==================== 创建工具集 ====================

function openCreateSetModal() {
    createModal({
        title: '新建外部工具集',
        size: 'lg',
        content: `
            <div class="mb-3">
                <label class="form-label">工具集名称 <span class="text-danger">*</span></label>
                <input type="text" class="form-control" id="setName" required placeholder="例如：支付服务">
            </div>
            <div class="mb-3">
                <label class="form-label">描述</label>
                <textarea class="form-control" id="setDescription" rows="2" placeholder="工具集功能描述..."></textarea>
            </div>
            <div class="mb-3">
                <label class="form-label">选择类型 <span class="text-danger">*</span></label>
                <div class="type-select-grid" style="grid-template-columns: repeat(3, 1fr);">
                    <div class="type-select-card active" data-type="mcp" onclick="selectSetType('mcp')">
                        <div class="type-icon">🔗</div>
                        <div class="type-name">MCP</div>
                        <div class="type-desc">对接外部 MCP Server</div>
                    </div>
                    <div class="type-select-card" data-type="http" onclick="selectSetType('http')">
                        <div class="type-icon">🌐</div>
                        <div class="type-name">HTTP</div>
                        <div class="type-desc">对接外部 HTTP API</div>
                    </div>
                    <div class="type-select-card" data-type="skills" onclick="selectSetType('skills')">
                        <div class="type-icon">🛠️</div>
                        <div class="type-name">Skills</div>
                        <div class="type-desc">自定义技能/脚本配置</div>
                    </div>
                </div>
                <input type="hidden" id="setTag" value="mcp">
            </div>

            <!-- MCP 类型配置 -->
            <div id="mcpConfigPanel">
                <div class="tab-switch">
                    <button class="tab-switch-item active" onclick="switchMcpMode('form')">表单模式</button>
                    <button class="tab-switch-item" onclick="switchMcpMode('json')">JSON模式</button>
                </div>
                <div id="mcpFormMode">
                    <div class="grid-2">
                        <div class="mb-3">
                            <label class="form-label">服务名称</label>
                            <input type="text" class="form-control" id="mcpServerName" disabled style="background: #f5f5f5;">
                            <div class="form-text">自动使用工具集名称</div>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">传输方式 <span class="text-danger">*</span></label>
                            <select class="form-select" id="mcpTransport" onchange="toggleMcpTransport()">
                                <option value="stdio">stdio (标准输入输出)</option>
                                <option value="sse">sse (服务器发送事件)</option>
                            </select>
                        </div>
                    </div>
                    <div id="mcpStdioPanel">
                        <div class="mb-3">
                            <label class="form-label">启动命令 <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="mcpCommand" placeholder="例如：npx 或 uvx">
                        </div>
                        <div class="mb-3">
                            <label class="form-label">参数 <span class="text-danger">*</span></label>
                            <div class="form-hint">每行一个参数</div>
                            <textarea class="form-control" id="mcpArgs" rows="3" placeholder="-y&#10;@modelcontextprotocol/server-filesystem&#10;/path/to/dir"></textarea>
                        </div>
                    </div>
                    <div id="mcpSsePanel" style="display:none;">
                        <div class="mb-3">
                            <label class="form-label">SSE 端点 URL <span class="text-danger">*</span></label>
                            <input type="url" class="form-control" id="mcpUrl" placeholder="http://localhost:3000/sse">
                        </div>
                    </div>
                    <div class="config-panel">
                        <div class="config-panel-title">环境变量</div>
                        <div id="mcpEnvList" class="kv-list">
                            <div class="kv-item">
                                <input type="text" class="form-control kv-key" placeholder="变量名">
                                <input type="text" class="form-control kv-value" placeholder="变量值">
                                <button class="btn btn-outline-secondary btn-sm" onclick="removeKvItem(this)">✕</button>
                            </div>
                        </div>
                        <button class="btn btn-sm btn-outline-secondary" onclick="addKvItem('mcpEnvList')" style="margin-top:8px;">+ 添加变量</button>
                    </div>
                </div>
                <div id="mcpJsonMode" style="display:none;">
                    <div class="mb-3">
                        <label class="form-label">MCP Server JSON 配置</label>
                        <textarea class="form-control font-monospace bg-light" id="mcpJsonConfig" placeholder='{\n  "mcpServers": {\n    "filesystem": {\n      "command": "npx",\n      "args": ["-y", "@modelcontextprotocol/server-filesystem", "/path/to/dir"]\n    }\n  }\n}'></textarea>
                    </div>
                </div>
            </div>

            <!-- HTTP 类型配置 -->
            <div id="httpConfigPanel" style="display:none;">
                <div class="mb-3">
                    <label class="form-label">Base URL <span class="text-danger">*</span></label>
                    <input type="url" class="form-control" id="httpBaseUrl" placeholder="https://api.example.com">
                </div>
                <div class="config-panel">
                    <div class="config-panel-title">请求头 Headers</div>
                    <div id="httpHeaderList" class="kv-list">
                        <div class="kv-item">
                            <input type="text" class="form-control kv-key" placeholder="Header 名称">
                            <input type="text" class="form-control kv-value" placeholder="Header 值">
                            <button class="btn btn-outline-secondary btn-sm" onclick="removeKvItem(this)">✕</button>
                        </div>
                    </div>
                    <button class="btn btn-sm btn-outline-secondary" onclick="addKvItem('httpHeaderList')" style="margin-top:8px;">+ 添加 Header</button>
                </div>
                <div class="mb-3 mt-3">
                    <label class="form-label">认证方式</label>
                    <select class="form-select" id="httpAuthType" onchange="toggleHttpAuth()">
                        <option value="none">无认证</option>
                        <option value="bearer">Bearer Token</option>
                        <option value="basic">Basic Auth</option>
                        <option value="apikey">API Key</option>
                    </select>
                </div>
                <div id="httpAuthPanel" style="display:none;">
                    <div class="mb-3" id="httpBearerPanel" style="display:none;">
                        <label class="form-label">Token</label>
                        <input type="text" class="form-control" id="httpBearerToken" placeholder="Bearer token...">
                    </div>
                    <div class="mb-3" id="httpBasicPanel" style="display:none;">
                        <div class="grid-2">
                            <div>
                                <label class="form-label">用户名</label>
                                <input type="text" class="form-control" id="httpBasicUser" placeholder="username">
                            </div>
                            <div>
                                <label class="form-label">密码</label>
                                <input type="password" class="form-control" id="httpBasicPass" placeholder="password">
                            </div>
                        </div>
                    </div>
                    <div class="mb-3" id="httpApiKeyPanel" style="display:none;">
                        <div class="grid-2">
                            <div>
                                <label class="form-label">Key 名称</label>
                                <input type="text" class="form-control" id="httpApiKeyName" placeholder="X-API-Key">
                            </div>
                            <div>
                                <label class="form-label">Key 值</label>
                                <input type="text" class="form-control" id="httpApiKeyValue" placeholder="your-api-key">
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Skills 类型配置 -->
            <div id="skillsConfigPanel" style="display:none;">
                <div class="mb-3">
                    <label class="form-label">自定义配置 (JSON)</label>
                    <textarea class="form-control font-monospace bg-light" id="skillsCustomConfig" rows="8" placeholder='{\n  "language": "python",\n  "entry": "main.py",\n  "dependencies": ["requests"]\n}'></textarea>
                </div>
            </div>
        `,
        onConfirm: () => createSet()
    });

    setTimeout(() => {
        selectSetType('mcp');
        // 绑定名称自动同步到 MCP 服务名
        const nameInput = document.getElementById('setName');
        const mcpNameInput = document.getElementById('mcpServerName');
        if (nameInput && mcpNameInput) {
            nameInput.addEventListener('input', () => {
                mcpNameInput.value = nameInput.value;
            });
        }
    }, 0);
}

function selectSetType(type) {
    document.getElementById('setTag').value = type;
    document.querySelectorAll('.type-select-card').forEach(card => {
        card.classList.toggle('active', card.dataset.type === type);
    });

    const mcpPanel = document.getElementById('mcpConfigPanel');
    const httpPanel = document.getElementById('httpConfigPanel');
    const skillsPanel = document.getElementById('skillsConfigPanel');
    if (mcpPanel) mcpPanel.style.display = type === 'mcp' ? 'block' : 'none';
    if (httpPanel) httpPanel.style.display = type === 'http' ? 'block' : 'none';
    if (skillsPanel) skillsPanel.style.display = type === 'skills' ? 'block' : 'none';
}

function switchMcpMode(mode) {
    document.querySelectorAll('.tab-switch-item').forEach(item => {
        item.classList.toggle('active', item.textContent.includes(mode === 'form' ? '表单' : 'JSON'));
    });
    document.getElementById('mcpFormMode').style.display = mode === 'form' ? 'block' : 'none';
    document.getElementById('mcpJsonMode').style.display = mode === 'json' ? 'block' : 'none';
}

function toggleMcpTransport() {
    const transport = document.getElementById('mcpTransport').value;
    document.getElementById('mcpStdioPanel').style.display = transport === 'stdio' ? 'block' : 'none';
    document.getElementById('mcpSsePanel').style.display = transport === 'sse' ? 'block' : 'none';
}

function toggleHttpAuth() {
    const authType = document.getElementById('httpAuthType').value;
    const authPanel = document.getElementById('httpAuthPanel');
    if (authPanel) authPanel.style.display = authType === 'none' ? 'none' : 'block';
    const bearerPanel = document.getElementById('httpBearerPanel');
    const basicPanel = document.getElementById('httpBasicPanel');
    const apiKeyPanel = document.getElementById('httpApiKeyPanel');
    if (bearerPanel) bearerPanel.style.display = authType === 'bearer' ? 'block' : 'none';
    if (basicPanel) basicPanel.style.display = authType === 'basic' ? 'block' : 'none';
    if (apiKeyPanel) apiKeyPanel.style.display = authType === 'apikey' ? 'block' : 'none';
}

function addKvItem(containerId) {
    const container = document.getElementById(containerId);
    const item = document.createElement('div');
    item.className = 'kv-item';
    item.innerHTML = `
        <input type="text" class="form-control kv-key" placeholder="变量名">
        <input type="text" class="form-control kv-value" placeholder="变量值">
        <button class="btn btn-outline-secondary btn-sm" onclick="removeKvItem(this)">✕</button>
    `;
    container.appendChild(item);
}

function removeKvItem(btn) {
    btn.closest('.kv-item').remove();
}

function getMcpConfigFromForm() {
    const serverName = document.getElementById('setName').value.trim();
    const transport = document.getElementById('mcpTransport').value;
    
    if (!serverName) throw new Error('请填写工具集名称');
    
    const config = { mcpServers: {} };
    const serverConfig = {};
    
    if (transport === 'stdio') {
        const command = document.getElementById('mcpCommand').value.trim();
        const argsText = document.getElementById('mcpArgs').value.trim();
        if (!command) throw new Error('请填写启动命令');
        serverConfig.command = command;
        serverConfig.args = argsText ? argsText.split('\n').map(a => a.trim()).filter(Boolean) : [];
    } else {
        const url = document.getElementById('mcpUrl').value.trim();
        if (!url) throw new Error('请填写 SSE 端点 URL');
        serverConfig.url = url;
    }
    
    const env = {};
    document.querySelectorAll('#mcpEnvList .kv-item').forEach(item => {
        const key = item.querySelector('.kv-key').value.trim();
        const value = item.querySelector('.kv-value').value.trim();
        if (key) env[key] = value;
    });
    if (Object.keys(env).length > 0) serverConfig.env = env;
    
    config.mcpServers[serverName] = serverConfig;
    return JSON.stringify(config);
}

function getMcpConfigFromJson() {
    const jsonText = document.getElementById('mcpJsonConfig').value.trim();
    if (!jsonText) throw new Error('请填写 MCP JSON 配置');
    try {
        JSON.parse(jsonText);
        return jsonText;
    } catch (e) {
        throw new Error('JSON 格式错误: ' + e.message);
    }
}

function getHttpConfig() {
    const baseUrl = document.getElementById('httpBaseUrl').value.trim();
    if (!baseUrl) throw new Error('请填写 Base URL');

    const headers = {};
    document.querySelectorAll('#httpHeaderList .kv-item').forEach(item => {
        const key = item.querySelector('.kv-key').value.trim();
        const value = item.querySelector('.kv-value').value.trim();
        if (key) headers[key] = value;
    });

    const authType = document.getElementById('httpAuthType').value;
    const auth = { type: authType };
    if (authType === 'bearer') {
        auth.token = document.getElementById('httpBearerToken').value.trim();
    } else if (authType === 'basic') {
        auth.username = document.getElementById('httpBasicUser').value.trim();
        auth.password = document.getElementById('httpBasicPass').value.trim();
    } else if (authType === 'apikey') {
        auth.keyName = document.getElementById('httpApiKeyName').value.trim();
        auth.keyValue = document.getElementById('httpApiKeyValue').value.trim();
    }

    return { baseUrl, headers, auth };
}

function getSkillsConfig() {
    const jsonText = document.getElementById('skillsCustomConfig').value.trim();
    if (!jsonText) throw new Error('请填写自定义配置');
    try {
        JSON.parse(jsonText);
        return jsonText;
    } catch (e) {
        throw new Error('JSON 格式错误: ' + e.message);
    }
}

async function createSet() {
    const name = document.getElementById('setName').value.trim();
    const description = document.getElementById('setDescription').value.trim();
    const tag = document.getElementById('setTag').value;

    if (!name) { showToast('请填写工具集名称', 'warning'); return; }
    if (!tag) { showToast('请选择工具集类型', 'warning'); return; }

    // 固定为外部工具集
    const payload = { name, description, type: 'external', tag, status: 1 };

    try {
        if (tag === 'mcp') {
            const isJsonMode = document.getElementById('mcpJsonMode').style.display !== 'none';
            payload.mcpConfig = isJsonMode ? getMcpConfigFromJson() : getMcpConfigFromForm();
        } else if (tag === 'http') {
            payload.customConfig = JSON.stringify(getHttpConfig());
        } else if (tag === 'skills') {
            payload.customConfig = getSkillsConfig();
        }

        const response = await fetch(API_BASE, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            showToast('工具集创建成功', 'success');
            closeModal();
            loadSets();
            loadToolSetFilter();
        } else {
            const err = await response.text();
            showToast('创建失败: ' + err, 'error');
        }
    } catch (error) {
        showToast(error.message || '创建失败', 'error');
    }
}

// ==================== 编辑工具集 ====================

async function editSet(id) {
    const set = _allSets.find(s => s.id === id);
    if (!set) { showToast('工具集不存在', 'error'); return; }
    
    let mcpConfigObj = null;
    let serverName = '';
    let mcpTransport = 'stdio';
    let mcpCommand = '';
    let mcpArgs = '';
    let mcpUrl = '';
    let mcpEnv = [];
    
    if (set.type === 'external' && set.mcpConfig) {
        try {
            mcpConfigObj = typeof set.mcpConfig === 'string' ? JSON.parse(set.mcpConfig) : set.mcpConfig;
            if (mcpConfigObj.mcpServers) {
                const names = Object.keys(mcpConfigObj.mcpServers);
                if (names.length > 0) {
                    serverName = names[0];
                    const sc = mcpConfigObj.mcpServers[serverName];
                    if (sc.url) { mcpTransport = 'sse'; mcpUrl = sc.url; }
                    else { mcpTransport = 'stdio'; mcpCommand = sc.command || ''; mcpArgs = (sc.args || []).join('\n'); }
                    if (sc.env) mcpEnv = Object.entries(sc.env).map(([k, v]) => ({ key: k, value: v }));
                }
            }
        } catch (e) {}
    }
    
    createModal({
        title: '编辑工具集',
        size: 'lg',
        content: `
            <div class="mb-3">
                <label class="form-label">工具集名称 <span class="text-danger">*</span></label>
                <input type="text" class="form-control" id="editSetName" value="${set.name}" required>
            </div>
            <div class="mb-3">
                <label class="form-label">描述</label>
                <textarea class="form-control" id="editSetDescription" rows="2">${set.description || ''}</textarea>
            </div>
            <div class="mb-3">
                <label class="form-label">来源</label>
                <input type="text" class="form-control" value="${getTypeLabel(set.type)}" disabled>
            </div>
            <div class="mb-3">
                <label class="form-label">标签</label>
                <select class="form-select" id="editSetTag">
                    <option value="" ${!set.tag ? 'selected' : ''}>请选择</option>
                    <option value="mcp" ${set.tag === 'mcp' ? 'selected' : ''}>MCP</option>
                    <option value="skills" ${set.tag === 'skills' ? 'selected' : ''}>Skills</option>
                    <option value="http" ${set.tag === 'http' ? 'selected' : ''}>HTTP</option>
                </select>
            </div>
            <div class="mb-3">
                <label class="form-label">状态</label>
                <select class="form-select" id="editSetStatus">
                    <option value="1" ${set.status === 1 ? 'selected' : ''}>启用</option>
                    <option value="0" ${set.status === 0 ? 'selected' : ''}>禁用</option>
                </select>
            </div>
            
            ${set.type === 'external' ? `
            <div class="tab-switch" id="editMcpTabSwitch">
                <button class="tab-switch-item active" onclick="switchEditMcpMode('form')">表单模式</button>
                <button class="tab-switch-item" onclick="switchEditMcpMode('json')">JSON模式</button>
            </div>
            <div id="editMcpFormPanel">
                <div class="grid-2">
                    <div class="mb-3">
                        <label class="form-label">服务名称</label>
                        <input type="text" class="form-control" id="editMcpServerName" value="${serverName}" disabled style="background: #f5f5f5;">
                        <div class="form-text">自动使用工具集名称</div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label">传输方式 <span class="text-danger">*</span></label>
                        <select class="form-select" id="editMcpTransport" onchange="toggleEditMcpTransport()">
                            <option value="stdio" ${mcpTransport === 'stdio' ? 'selected' : ''}>stdio</option>
                            <option value="sse" ${mcpTransport === 'sse' ? 'selected' : ''}>sse</option>
                        </select>
                    </div>
                </div>
                <div id="editMcpStdioPanel" style="display:${mcpTransport === 'stdio' ? 'block' : 'none'};">
                    <div class="mb-3">
                        <label class="form-label">启动命令</label>
                        <input type="text" class="form-control" id="editMcpCommand" value="${mcpCommand}">
                    </div>
                    <div class="mb-3">
                        <label class="form-label">参数</label>
                        <textarea class="form-control" id="editMcpArgs" rows="3">${mcpArgs}</textarea>
                    </div>
                </div>
                <div id="editMcpSsePanel" style="display:${mcpTransport === 'sse' ? 'block' : 'none'};">
                    <div class="mb-3">
                        <label class="form-label">SSE 端点 URL</label>
                        <input type="url" class="form-control" id="editMcpUrl" value="${mcpUrl}">
                    </div>
                </div>
                <div class="config-panel">
                    <div class="config-panel-title">环境变量</div>
                    <div id="editMcpEnvList" class="kv-list">
                        ${mcpEnv.length > 0 ? mcpEnv.map(e => `
                            <div class="kv-item">
                                <input type="text" class="form-control kv-key" value="${e.key}" placeholder="变量名">
                                <input type="text" class="form-control kv-value" value="${e.value}" placeholder="变量值">
                                <button class="btn btn-outline-secondary btn-sm" onclick="removeKvItem(this)">✕</button>
                            </div>
                        `).join('') : `
                            <div class="kv-item">
                                <input type="text" class="form-control kv-key" placeholder="变量名">
                                <input type="text" class="form-control kv-value" placeholder="变量值">
                                <button class="btn btn-outline-secondary btn-sm" onclick="removeKvItem(this)">✕</button>
                            </div>
                        `}
                    </div>
                    <button class="btn btn-sm btn-outline-secondary" onclick="addKvItem('editMcpEnvList')" style="margin-top:8px;">+ 添加变量</button>
                </div>
            </div>
            <div id="editMcpJsonPanel" style="display:none;">
                <div class="mb-3">
                    <label class="form-label">MCP Server JSON 配置</label>
                    <textarea class="form-control font-monospace bg-light" id="editMcpJsonConfig">${set.mcpConfig || ''}</textarea>
                </div>
            </div>
            ` : ''}
        `,
        onConfirm: () => updateSet(id)
    });
}

function switchEditMcpMode(mode) {
    const tabSwitch = document.getElementById('editMcpTabSwitch');
    if (tabSwitch) {
        tabSwitch.querySelectorAll('.tab-switch-item').forEach(item => {
            item.classList.toggle('active', item.textContent.includes(mode === 'form' ? '表单' : 'JSON'));
        });
    }
    const formPanel = document.getElementById('editMcpFormPanel');
    const jsonPanel = document.getElementById('editMcpJsonPanel');
    if (formPanel) formPanel.style.display = mode === 'form' ? 'block' : 'none';
    if (jsonPanel) jsonPanel.style.display = mode === 'json' ? 'block' : 'none';
}

function toggleEditMcpTransport() {
    const transport = document.getElementById('editMcpTransport').value;
    const stdioPanel = document.getElementById('editMcpStdioPanel');
    const ssePanel = document.getElementById('editMcpSsePanel');
    if (stdioPanel) stdioPanel.style.display = transport === 'stdio' ? 'block' : 'none';
    if (ssePanel) ssePanel.style.display = transport === 'sse' ? 'block' : 'none';
}

function getEditMcpConfigFromForm() {
    const serverName = document.getElementById('editSetName').value.trim();
    const transport = document.getElementById('editMcpTransport').value;
    
    if (!serverName) throw new Error('请填写工具集名称');
    
    const config = { mcpServers: {} };
    const serverConfig = {};
    
    if (transport === 'stdio') {
        const command = document.getElementById('editMcpCommand').value.trim();
        const argsText = document.getElementById('editMcpArgs').value.trim();
        if (command) serverConfig.command = command;
        serverConfig.args = argsText ? argsText.split('\n').map(a => a.trim()).filter(Boolean) : [];
    } else {
        const url = document.getElementById('editMcpUrl').value.trim();
        if (url) serverConfig.url = url;
    }
    
    const env = {};
    document.querySelectorAll('#editMcpEnvList .kv-item').forEach(item => {
        const key = item.querySelector('.kv-key').value.trim();
        const value = item.querySelector('.kv-value').value.trim();
        if (key) env[key] = value;
    });
    if (Object.keys(env).length > 0) serverConfig.env = env;
    
    config.mcpServers[serverName] = serverConfig;
    return JSON.stringify(config);
}

async function updateSet(id) {
    const set = _allSets.find(s => s.id === id);
    if (!set) return;
    
    const name = document.getElementById('editSetName').value.trim();
    const description = document.getElementById('editSetDescription').value.trim();
    const status = parseInt(document.getElementById('editSetStatus').value);
    
    if (!name) { showToast('请填写工具集名称', 'warning'); return; }
    
    const payload = { name, description, tag: document.getElementById('editSetTag')?.value || '', status };
    
    try {
        if (set.type === 'external') {
            const jsonPanel = document.getElementById('editMcpJsonPanel');
            const isJsonMode = jsonPanel && jsonPanel.style.display !== 'none';
            if (isJsonMode) {
                const jsonText = document.getElementById('editMcpJsonConfig').value.trim();
                if (jsonText) {
                    JSON.parse(jsonText);
                    payload.mcpConfig = jsonText;
                }
            } else {
                payload.mcpConfig = getEditMcpConfigFromForm();
            }
        }
        
        const response = await fetch(`${API_BASE}/${id}`, {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        });
        
        if (response.ok) {
            showToast('更新成功', 'success');
            closeModal();
            loadSets();
            loadToolSetFilter();
            // 如果在详情页编辑，刷新详情页
            if (document.getElementById('setDetailPage').style.display !== 'none') {
                setTimeout(() => viewSetDetail(id), 100);
            }
        } else {
            const err = await response.text();
            showToast('更新失败: ' + err, 'error');
        }
    } catch (error) {
        showToast(error.message || '更新失败', 'error');
    }
}

async function deleteSet(id) {
    if (!confirm('确定要删除这个工具集吗？相关工具也会被删除。')) return;
    
    try {
        const response = await fetch(`${API_BASE}/${id}`, {method: 'DELETE'});
        if (response.ok) {
            showToast('删除成功', 'success');
            backToSetsList();
            loadSets();
            loadToolSetFilter();
        }
    } catch (error) {
        showToast('删除失败', 'error');
    }
}

// ==================== 工具管理 ====================

let _toolPageState = { page: 0, size: 9, totalPages: 0, totalElements: 0 };

async function loadTools() {
    try {
        const typeFilter = document.getElementById('filterToolType').value;
        const setFilter = document.getElementById('filterToolSet').value;
        const nameFilter = document.getElementById('searchToolName').value.trim();

        const params = new URLSearchParams();
        params.append('page', _toolPageState.page);
        params.append('size', _toolPageState.size);
        if (typeFilter) params.append('type', typeFilter);
        if (setFilter) params.append('toolSetId', setFilter);
        if (nameFilter) params.append('name', nameFilter);

        const response = await fetch(`${TOOLS_API_BASE}/page?${params.toString()}`);
        const pageData = await response.json();

        _toolPageState.totalPages = pageData.totalPages || 0;
        _toolPageState.totalElements = pageData.totalElements || 0;

        renderTools(pageData.content || []);
        renderPagination();
    } catch (error) {
        renderTools([]);
        renderPagination();
    }
}

function renderTools(tools) {
    const container = document.getElementById('toolsContainer');

    if (!tools || tools.length === 0) {
        container.innerHTML = `
            <div class="empty-state" style="grid-column: 1 / -1;">
                <div class="empty-icon">🔧</div>
                <div class="empty-title">暂无工具</div>
                <div class="empty-text">在工具集中添加工具</div>
            </div>
        `;
        return;
    }

    container.innerHTML = tools.map(tool => `
        <div class="col-md-6 col-lg-4">
            <div class="tool-card h-100">
                <div class="tool-card-header">
                <div>
                    <div class="tool-card-title">${tool.name}</div>
                    <span class="tool-type-tag ${tool.type}">${getTypeLabel(tool.type)}</span>
                </div>
            </div>
            <div class="tool-card-desc">${tool.description || '暂无描述'}</div>
            <div class="tool-card-footer">
                <div class="tool-status">
                    <span class="status-dot ${tool.status === 1 ? 'active' : 'inactive'}"></span>
                    <span>${tool.status === 1 ? '启用' : '禁用'}</span>
                </div>
                    <div class="d-flex gap-2">
                        <button class="btn btn-sm btn-primary" onclick="testTool('${tool.id}')">测试</button>
                        <button class="btn btn-sm btn-outline-secondary" onclick="editTool('${tool.id}')">编辑</button>
                    </div>
                </div>
            </div>
        </div>
    `).join('');
}

function renderPagination() {
    const container = document.getElementById('toolsPagination');
    if (_toolPageState.totalPages <= 1) {
        container.innerHTML = '';
        return;
    }

    let pagesHtml = '';
    const current = _toolPageState.page;
    const total = _toolPageState.totalPages;

    // 上一页
    pagesHtml += `
        <li class="page-item ${current === 0 ? 'disabled' : ''}">
            <a class="page-link" href="javascript:void(0)" onclick="goToToolPage(${current - 1})">上一页</a>
        </li>
    `;

    // 页码
    const startPage = Math.max(0, Math.min(current - 2, total - 5));
    const endPage = Math.min(total - 1, Math.max(current + 2, 4));

    for (let i = startPage; i <= endPage; i++) {
        pagesHtml += `
            <li class="page-item ${i === current ? 'active' : ''}">
                <a class="page-link" href="javascript:void(0)" onclick="goToToolPage(${i})">${i + 1}</a>
            </li>
        `;
    }

    // 下一页
    pagesHtml += `
        <li class="page-item ${current === total - 1 ? 'disabled' : ''}">
            <a class="page-link" href="javascript:void(0)" onclick="goToToolPage(${current + 1})">下一页</a>
        </li>
    `;

    container.innerHTML = `
        <nav aria-label="工具分页">
            <ul class="pagination mb-0">
                ${pagesHtml}
            </ul>
        </nav>
        <div class="ms-3 text-muted small d-flex align-items-center">
            第 ${current + 1} / ${total} 页，共 ${_toolPageState.totalElements} 条
        </div>
    `;
}

function goToToolPage(page) {
    if (page < 0 || page >= _toolPageState.totalPages) return;
    _toolPageState.page = page;
    loadTools();
}

async function loadToolSetFilter() {
    try {
        const response = await fetch(API_BASE);
        const sets = await response.json();
        const select = document.getElementById('filterToolSet');
        while (select.options.length > 1) select.remove(1);
        sets.forEach(set => {
            const option = document.createElement('option');
            option.value = set.id;
            option.textContent = set.name;
            select.appendChild(option);
        });
    } catch (error) {}
}

function resetToolFilters() {
    document.getElementById('filterToolType').value = '';
    document.getElementById('filterToolSet').value = '';
    document.getElementById('searchToolName').value = '';
    _toolPageState.page = 0;
    loadTools();
}

async function deleteTool(id) {
    if (!confirm('确定要删除这个工具吗？')) return;
    
    try {
        const response = await fetch(`${TOOLS_API_BASE}/${id}`, {method: 'DELETE'});
        if (response.ok) {
            showToast('删除成功', 'success');
            loadTools();
        }
    } catch (error) {
        showToast('删除失败', 'error');
    }
}

// ==================== 测试工具跳转AI助手 ====================

async function testTool(toolId) {
    try {
        const response = await fetch(TOOLS_API_BASE);
        const tools = await response.json();
        const tool = tools.find(t => t.id === toolId);
        
        if (!tool) {
            showToast('未找到工具', 'error');
            return;
        }
        
        let schema = null;
        if (tool.inputSchema) {
            try { schema = JSON.parse(tool.inputSchema); }
            catch (e) { showToast('Schema格式错误', 'error'); return; }
        }
        
        window._currentTestTool = tool;
        window._currentTestSchema = schema;
        
        // 切换到智能工具测试页面
        switchPage('ai-assistant');
        
        setTimeout(() => {
            const schemaInput = document.getElementById('schemaInput');
            if (schemaInput && schema) {
                schemaInput.value = JSON.stringify(schema, null, 2);
            }
            
            document.getElementById('aiResultContainer').innerHTML = `
                <div style="padding: 16px; background: #e6f7ff; border-radius: 8px; border-left: 4px solid #1890ff; margin-bottom: 16px;">
                    <div style="font-weight: 600; margin-bottom: 8px;">🛠️ 当前测试工具: ${tool.name}</div>
                    <div style="font-size: 13px; color: #595959;">${tool.description || '暂无描述'}</div>
                </div>
                <div class="empty-state" style="padding: 40px;">
                    <div class="empty-icon">✨</div>
                    <div class="empty-title">Schema已自动填充</div>
                    <div class="empty-text">请选择大模型和生成策略，然后点击"AI生成测试参数"</div>
                </div>
            `;
        }, 100);
        
        showToast(`已加载工具 "${tool.name}"`, 'success');
    } catch (error) {
        showToast('加载失败', 'error');
    }
}

// ==================== 大模型管理 ====================

async function loadModels() {
    try {
        const response = await fetch(MODELS_API_BASE);
        if (response.ok) {
            const models = await response.json();
            renderModels(models);
            updateAIModelSelect(models);
            return;
        }
    } catch (error) {}
    
    const models = JSON.parse(localStorage.getItem(MODELS_STORAGE_KEY) || '[]');
    renderModels(models);
    updateAIModelSelect(models);
}

function renderModels(models) {
    const container = document.getElementById('modelsContainer');
    
    if (!models || models.length === 0) {
        container.innerHTML = `
            <div class="empty-state" style="grid-column: 1 / -1;">
                <div class="empty-icon">🤖</div>
                <div class="empty-title">暂无模型配置</div>
                <div class="empty-text">点击"添加模型"配置您的大模型</div>
            </div>
        `;
        return;
    }
    
    container.innerHTML = models.map(model => `
        <div class="col-md-6 col-lg-4">
            <div class="model-card h-100">
                <div class="tool-card-header">
                <div>
                    <div class="tool-card-title">${model.name}</div>
                    <span class="tool-type-tag" style="background: #f0f0f0; color: #595959;">${model.modelId}</span>
                </div>
                <span class="badge ${model.status !== 0 ? 'bg-success-subtle text-success' : 'bg-secondary-subtle text-secondary'}">
                    ${model.status !== 0 ? '启用' : '禁用'}
                </span>
            </div>
            <div class="tool-card-desc">${model.description || '暂无描述'}</div>
            <div style="font-size: 12px; color: #8c8c8c; margin-bottom: 12px;">
                📍 ${model.apiEndpoint || '未配置'}
            </div>
            <div class="tool-card-footer">
                <div style="display: flex; gap: 8px;">
                    <span style="font-size: 12px; color: #8c8c8c;">🌡️ ${model.temperature || 0.7}</span>
                    <span style="font-size: 12px; color: #8c8c8c;">🔢 ${model.maxTokens || 4096}</span>
                </div>
                    <div class="d-flex gap-2">
                        <button class="btn btn-sm btn-outline-secondary" onclick="editModel('${model.id}')">编辑</button>
                        <button class="btn btn-sm btn-danger" onclick="deleteModel('${model.id}')">删除</button>
                    </div>
                </div>
            </div>
        </div>
    `).join('');
}

function updateAIModelSelect(models) {
    const select = document.getElementById('aiModelSelect');
    const activeModels = models.filter(m => m.status !== 0);
    
    select.innerHTML = activeModels.length === 0 
        ? '<option value="">请先配置大模型</option>'
        : activeModels.map(m => `<option value="${m.id}">${m.name}</option>`).join('');
}

function openCreateModelModal() {
    createModal({
        title: '添加大模型',
        content: `
            <div class="mb-3">
                <label class="form-label">模型名称 <span class="text-danger">*</span></label>
                <input type="text" class="form-control" id="modelName" required placeholder="例如：GPT-4">
            </div>
            <div class="mb-3">
                <label class="form-label">模型ID <span class="text-danger">*</span></label>
                <input type="text" class="form-control" id="modelId" required placeholder="例如：gpt-4">
            </div>
            <div class="mb-3">
                <label class="form-label">API Endpoint <span class="text-danger">*</span></label>
                <input type="url" class="form-control" id="modelApiEndpoint" required placeholder="https://api.openai.com/v1/chat/completions">
            </div>
            <div class="mb-3">
                <label class="form-label">API Key <span class="text-danger">*</span></label>
                <input type="password" class="form-control" id="modelApiKey" required placeholder="sk-...">
            </div>
            <div class="grid-2">
                <div class="mb-3">
                    <label class="form-label">Temperature</label>
                    <input type="number" class="form-control" id="modelTemperature" value="0.7" min="0" max="2" step="0.1">
                </div>
                <div class="mb-3">
                    <label class="form-label">Max Tokens</label>
                    <input type="number" class="form-control" id="modelMaxTokens" value="4096" min="1">
                </div>
            </div>
            <div class="mb-3">
                <label class="form-label">描述</label>
                <textarea class="form-control" id="modelDescription" rows="2"></textarea>
            </div>
        `,
        onConfirm: () => saveModel()
    });
}

async function saveModel() {
    const modelData = {
        id: 'model-' + Date.now(),
        name: document.getElementById('modelName').value,
        modelId: document.getElementById('modelId').value,
        apiEndpoint: document.getElementById('modelApiEndpoint').value,
        apiKey: document.getElementById('modelApiKey').value,
        temperature: parseFloat(document.getElementById('modelTemperature').value) || 0.7,
        maxTokens: parseInt(document.getElementById('modelMaxTokens').value) || 4096,
        description: document.getElementById('modelDescription').value,
        status: 1
    };
    
    const models = JSON.parse(localStorage.getItem(MODELS_STORAGE_KEY) || '[]');
    models.push(modelData);
    localStorage.setItem(MODELS_STORAGE_KEY, JSON.stringify(models));
    
    showToast('模型添加成功', 'success');
    closeModal();
    loadModels();
}

async function editModel(modelId) {
    const models = JSON.parse(localStorage.getItem(MODELS_STORAGE_KEY) || '[]');
    const model = models.find(m => m.id === modelId);
    if (!model) { showToast('模型不存在', 'error'); return; }
    
    createModal({
        title: '编辑模型',
        content: `
            <div class="mb-3">
                <label class="form-label">模型名称 <span class="text-danger">*</span></label>
                <input type="text" class="form-control" id="editModelName" value="${model.name}" required>
            </div>
            <div class="mb-3">
                <label class="form-label">模型ID <span class="text-danger">*</span></label>
                <input type="text" class="form-control" id="editModelId" value="${model.modelId}" required>
            </div>
            <div class="mb-3">
                <label class="form-label">API Endpoint <span class="text-danger">*</span></label>
                <input type="url" class="form-control" id="editModelApiEndpoint" value="${model.apiEndpoint}" required>
            </div>
            <div class="mb-3">
                <label class="form-label">API Key <span class="text-danger">*</span></label>
                <input type="password" class="form-control" id="editModelApiKey" value="${model.apiKey}" required>
            </div>
            <div class="grid-2">
                <div class="mb-3">
                    <label class="form-label">Temperature</label>
                    <input type="number" class="form-control" id="editModelTemperature" value="${model.temperature}" min="0" max="2" step="0.1">
                </div>
                <div class="mb-3">
                    <label class="form-label">Max Tokens</label>
                    <input type="number" class="form-control" id="editModelMaxTokens" value="${model.maxTokens}" min="1">
                </div>
            </div>
            <div class="mb-3">
                <label class="form-label">描述</label>
                <textarea class="form-control" id="editModelDescription" rows="2">${model.description || ''}</textarea>
            </div>
        `,
        onConfirm: () => updateModel(modelId)
    });
}

async function updateModel(modelId) {
    const models = JSON.parse(localStorage.getItem(MODELS_STORAGE_KEY) || '[]');
    const index = models.findIndex(m => m.id === modelId);
    if (index === -1) return;
    
    models[index] = {
        ...models[index],
        name: document.getElementById('editModelName').value,
        modelId: document.getElementById('editModelId').value,
        apiEndpoint: document.getElementById('editModelApiEndpoint').value,
        apiKey: document.getElementById('editModelApiKey').value,
        temperature: parseFloat(document.getElementById('editModelTemperature').value) || 0.7,
        maxTokens: parseInt(document.getElementById('editModelMaxTokens').value) || 4096,
        description: document.getElementById('editModelDescription').value
    };
    
    localStorage.setItem(MODELS_STORAGE_KEY, JSON.stringify(models));
    showToast('模型更新成功', 'success');
    closeModal();
    loadModels();
}

async function deleteModel(modelId) {
    if (!confirm('确定要删除此模型吗？')) return;
    
    const models = JSON.parse(localStorage.getItem(MODELS_STORAGE_KEY) || '[]');
    localStorage.setItem(MODELS_STORAGE_KEY, JSON.stringify(models.filter(m => m.id !== modelId)));
    showToast('模型删除成功', 'success');
    loadModels();
}

// ==================== 智能工具测试 ====================

async function generateParamsWithAI() {
    const modelId = document.getElementById('aiModelSelect').value;
    const schemaText = document.getElementById('schemaInput').value;
    const strategy = document.getElementById('aiStrategySelect').value;
    
    if (!modelId) { showToast('请先选择大模型', 'warning'); return; }
    if (!schemaText.trim()) { showToast('请先输入JSON Schema', 'warning'); return; }
    
    let schema;
    try { schema = JSON.parse(schemaText); }
    catch (e) { showToast('JSON Schema格式错误', 'error'); return; }
    
    const container = document.getElementById('aiResultContainer');
    container.innerHTML = `
        <div class="ai-loading">
            <div class="ai-loading-spinner"></div>
            <span>AI正在生成测试参数...</span>
        </div>
    `;
    
    try {
        const models = JSON.parse(localStorage.getItem(MODELS_STORAGE_KEY) || '[]');
        const model = models.find(m => m.id === modelId);
        if (!model) throw new Error('找不到模型配置');
        
        const prompt = buildAIPrompt(schema, strategy);
        const aiResponse = await callLLMAPI(model, prompt);
        const params = parseAIResponse(aiResponse);
        
        displayAIResult({params, strategy, timestamp: new Date().toISOString(), note: '由AI自动生成'});
        showToast('参数生成成功', 'success');
    } catch (error) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">❌</div>
                <div class="empty-title">生成失败</div>
                <div class="empty-text">${error.message}</div>
            </div>
        `;
        showToast('生成失败: ' + error.message, 'error');
    }
}

async function callLLMAPI(model, prompt) {
    const response = await fetch(model.apiEndpoint, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${model.apiKey}`
        },
        body: JSON.stringify({
            model: model.modelId,
            messages: [
                {role: 'system', content: '你是一个API测试专家，根据JSON Schema生成测试数据。返回纯JSON格式。'},
                {role: 'user', content: prompt}
            ],
            temperature: model.temperature || 0.7,
            max_tokens: model.maxTokens || 4096,
            stream: false
        })
    });
    
    if (!response.ok) {
        const error = await response.text();
        throw new Error(`HTTP ${response.status}: ${error}`);
    }
    
    const data = await response.json();
    if (data.choices && data.choices.length > 0) {
        return data.choices[0].message.content;
    }
    throw new Error('AI响应格式不正确');
}

function buildAIPrompt(schema, strategy) {
    const strategyDesc = {
        'realistic': '真实业务数据',
        'boundary': '边界值测试',
        'comprehensive': '全面覆盖'
    };
    
    return `根据以下JSON Schema生成测试参数：
策略：${strategyDesc[strategy]}
Schema：${JSON.stringify(schema, null, 2)}
要求：
1. 为所有字段生成测试值
2. 返回纯JSON对象，不要有任何解释
3. 确保数据可直接用于API测试

返回JSON：`;
}

function parseAIResponse(aiResponse) {
    if (!aiResponse?.trim()) throw new Error('AI返回为空');
    
    let jsonStr = aiResponse.trim();
    const codeBlockMatch = jsonStr.match(/```json\s*([\s\S]*?)```/);
    if (codeBlockMatch) jsonStr = codeBlockMatch[1].trim();
    
    if (!jsonStr.startsWith('{')) {
        const match = jsonStr.match(/\{[\s\S]*\}/);
        if (match) jsonStr = match[0];
    }
    
    try { return JSON.parse(jsonStr); }
    catch (e) { throw new Error(`解析失败: ${e.message}`); }
}

function displayAIResult(result) {
    const container = document.getElementById('aiResultContainer');
    const currentTool = window._currentTestTool;
    
    container.innerHTML = `
        ${currentTool ? `
            <div style="padding: 12px; background: #e6f7ff; border-radius: 6px; border-left: 3px solid #1890ff; margin-bottom: 16px;">
                <div style="font-weight: 600; font-size: 13px;">🛠️ 测试工具: ${currentTool.name}</div>
            </div>
        ` : ''}
        <div style="margin-bottom: 12px;">
            <span style="font-size: 12px; color: #8c8c8c;">策略:</span>
            <span style="font-size: 13px; font-weight: 500; color: #1890ff; margin-left: 8px;">
                ${result.strategy === 'realistic' ? '🎯 真实业务' : result.strategy === 'boundary' ? '🔍 边界值' : '📚 全面覆盖'}
            </span>
        </div>
        <pre style="padding: 16px; background: #fafafa; border-radius: 6px; overflow-x: auto; font-size: 13px; line-height: 1.6; border: 1px solid #f0f0f0;">${JSON.stringify(result.params, null, 2)}</pre>
        <div style="display: flex; gap: 8px; justify-content: flex-end; margin-top: 16px;">
            <button class="btn btn-outline-secondary" onclick="copyAIResult()">📋 复制</button>
            ${currentTool ? `<button class="btn btn-primary" onclick="executeToolTestFromAI()">🚀 执行测试</button>` : ''}
        </div>
    `;
}

async function executeToolTestFromAI() {
    const currentTool = window._currentTestTool;
    const pre = document.querySelector('#aiResultContainer pre');
    
    if (!currentTool || !pre) { showToast('缺少测试数据', 'warning'); return; }
    
    let params;
    try { params = JSON.parse(pre.textContent); }
    catch (e) { showToast('参数格式错误', 'error'); return; }
    
    const container = document.getElementById('aiResultContainer');
    container.innerHTML = `
        <div class="ai-loading">
            <div class="ai-loading-spinner"></div>
            <span>正在执行工具测试...</span>
        </div>
    `;
    
    try {
        const response = await fetch(`${TOOLS_API_BASE}/${currentTool.id}/test`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(params)
        });
        
        const result = await response.json();
        displayTestResult(currentTool, result);
        showToast(result.success ? '测试成功' : '测试失败', result.success ? 'success' : 'error');
    } catch (error) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">❌</div>
                <div class="empty-title">测试失败</div>
                <div class="empty-text">${error.message}</div>
            </div>
        `;
        showToast('测试失败', 'error');
    }
}

function displayTestResult(tool, result) {
    const container = document.getElementById('aiResultContainer');
    container.innerHTML = `
        <div style="padding: 12px; background: ${result.success ? '#f6ffed' : '#fff1f0'}; border-radius: 6px; border-left: 3px solid ${result.success ? '#52c41a' : '#ff4d4f'}; margin-bottom: 16px;">
            <div style="font-weight: 600; color: ${result.success ? '#389e0d' : '#cf1322'};">
                ${result.success ? '✅ 测试成功' : '❌ 测试失败'}
            </div>
            <div style="font-size: 12px; color: #595959; margin-top: 4px;">${tool.name}</div>
        </div>
        ${result.output ? `<pre style="padding: 12px; background: #fafafa; border-radius: 6px; font-size: 12px; overflow-x: auto; margin-bottom: 12px;">${typeof result.output === 'string' ? result.output : JSON.stringify(result.output, null, 2)}</pre>` : ''}
        ${result.error ? `<pre style="padding: 12px; background: #fafafa; border-radius: 6px; font-size: 12px; color: #cf1322; overflow-x: auto; margin-bottom: 12px;">${result.error}</pre>` : ''}
        <div style="display: flex; gap: 8px; justify-content: flex-end;">
            <button class="btn btn-outline-secondary" onclick="resetAITest()">🔄 重新测试</button>
        </div>
    `;
}

function copyAIResult() {
    const pre = document.querySelector('#aiResultContainer pre');
    if (pre) {
        navigator.clipboard.writeText(pre.textContent).then(() => showToast('已复制', 'success'));
    }
}

function resetAITest() {
    const currentTool = window._currentTestTool;
    if (currentTool) testTool(currentTool.id);
}

// ==================== 工具函数 ====================

function getTypeLabel(type) {
    const labels = {'internal': '内部', 'external': '外部', 'mcp': 'MCP', 'http': 'HTTP', 'skills': 'Skills', 'custom': '自定义'};
    return labels[type] || type;
}

function getTagLabel(tag) {
    const labels = {'mcp': 'MCP', 'skills': 'Skills', 'http': 'HTTP'};
    return labels[tag] || tag;
}

function editTool(id) {
    showToast('编辑工具: ' + id, 'info');
}

// ==================== 模态框 ====================

let _mainModal = null;
let _currentOnConfirm = null;

function createModal({title, content, onConfirm, size}) {
    _currentOnConfirm = onConfirm;
    document.getElementById('mainModalTitle').textContent = title;
    document.getElementById('mainModalBody').innerHTML = content;
    
    const dialog = document.getElementById('mainModalDialog');
    dialog.className = 'modal-dialog' + (size === 'lg' ? ' modal-lg' : '');
    
    const confirmBtn = document.getElementById('mainModalConfirm');
    confirmBtn.onclick = onConfirm || closeModal;
    
    _mainModal = new bootstrap.Modal(document.getElementById('mainModal'));
    _mainModal.show();
}

function closeModal() {
    if (_mainModal) {
        _mainModal.hide();
        _mainModal = null;
    }
}

function showToast(message, type = 'info') {
    const container = document.querySelector('.toast-container');
    const icons = {success: '✅', error: '❌', warning: '⚠️', info: 'ℹ️'};
    const bgMap = {success: 'bg-success', error: 'bg-danger', warning: 'bg-warning text-dark', info: 'bg-primary'};
    
    const toastEl = document.createElement('div');
    toastEl.className = `toast align-items-center ${bgMap[type]} text-white border-0`;
    toastEl.setAttribute('role', 'alert');
    toastEl.innerHTML = `
        <div class="d-flex">
            <div class="toast-body"><span class="me-2">${icons[type]}</span>${message}</div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
    `;
    
    container.appendChild(toastEl);
    const toast = new bootstrap.Toast(toastEl, {delay: 3000});
    toast.show();
    toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove());
}
