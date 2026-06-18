
// 工具集创建/编辑补丁：覆盖旧函数以支持 mcp/http/skills 三种外部类型

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
                <div class="alert alert-info">
                    <div class="fw-semibold">HTTP 工具集创建后将显示在详情页</div>
                    <p class="mb-0">创建完成后，请在详情页点击「添加HTTP工具」来逐个配置具体的 HTTP 接口。</p>
                </div>
            </div>

            <!-- Skills 类型配置 -->
            <div id="skillsConfigPanel" style="display:none;">
                <div class="alert alert-info">
                    <div class="fw-semibold">Skills 工具集创建后将显示在详情页</div>
                    <p class="mb-0">创建完成后，请在详情页点击「上传Skill目录」来批量上传 skill 目录(zip)。</p>
                </div>
            </div>
        `,
        onConfirm: () => createSet()
    });

    setTimeout(() => {
        selectSetType('mcp');
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
    const tagInput = document.getElementById('setTag');
    if (tagInput) tagInput.value = type;
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
        }
        // HTTP 和 Skills 不在创建时填写配置，创建后到详情页添加工具

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

// ==================== 编辑工具集（覆盖） ====================

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

    if (set.tag === 'mcp' && set.mcpConfig) {
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

    let httpConfig = { baseUrl: '', headers: {}, auth: { type: 'none' } };
    if (set.tag === 'http' && set.customConfig) {
        try {
            httpConfig = typeof set.customConfig === 'string' ? JSON.parse(set.customConfig) : set.customConfig;
        } catch (e) {}
    }
    const httpHeaders = Object.entries(httpConfig.headers || {}).map(([k, v]) => ({ key: k, value: v }));

    let skillsConfig = '';
    if (set.tag === 'skills' && set.customConfig) {
        skillsConfig = typeof set.customConfig === 'string' ? set.customConfig : JSON.stringify(set.customConfig, null, 2);
    }

    const tagDisplay = { mcp: 'MCP', http: 'HTTP', skills: 'Skills' };

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
                <label class="form-label">类型</label>
                <input type="text" class="form-control" value="${tagDisplay[set.tag] || set.tag || '未知'}（外部）" disabled>
            </div>
            <div class="mb-3">
                <label class="form-label">状态</label>
                <select class="form-select" id="editSetStatus">
                    <option value="1" ${set.status === 1 ? 'selected' : ''}>启用</option>
                    <option value="0" ${set.status === 0 ? 'selected' : ''}>禁用</option>
                </select>
            </div>

            ${set.tag === 'mcp' ? `
            <div class="tab-switch" id="editMcpTabSwitch">
                <button class="tab-switch-item active" onclick="switchEditMcpMode('form')">表单模式</button>
                <button class="tab-switch-item" onclick="switchEditMcpMode('json')">JSON模式</button>
            </div>
            <div id="editMcpFormPanel">
                <div class="grid-2">
                    <div class="mb-3">
                        <label class="form-label">服务名称</label>
                        <input type="text" class="form-control" id="editMcpServerName" value="${serverName}" disabled style="background: #f5f5f5;">
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

            ${set.tag === 'http' ? `
            <div class="alert alert-info">
                <div class="fw-semibold">HTTP 工具配置已迁移至工具级别</div>
                <p class="mb-0">请前往详情页「添加HTTP工具」来管理具体的 HTTP 接口配置。</p>
            </div>
            ` : ''}

            ${set.tag === 'skills' ? `
            <div class="alert alert-info">
                <div class="fw-semibold">Skills 工具配置已迁移至工具级别</div>
                <p class="mb-0">请前往详情页「上传Skill目录」来管理 skill 目录。</p>
            </div>
            ` : ''}
        `,
        onConfirm: () => updateSet(id)
    });
}

async function updateSet(id) {
    const set = _allSets.find(s => s.id === id);
    if (!set) return;

    const name = document.getElementById('editSetName').value.trim();
    const description = document.getElementById('editSetDescription').value.trim();
    const status = parseInt(document.getElementById('editSetStatus').value);

    if (!name) { showToast('请填写工具集名称', 'warning'); return; }

    const payload = { name, description, status };

    try {
        if (set.tag === 'mcp') {
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
        // HTTP 和 Skills 不在编辑时修改配置，配置在工具级别管理
    
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

// 覆盖列表渲染和过滤函数

function renderSets(sets) {
    const container = document.getElementById('setsContainer');

    if (!sets || sets.length === 0) {
        container.innerHTML = `
            <div class="empty-state" style="grid-column: 1 / -1;">
                <div class="empty-icon">📦</div>
                <div class="empty-title">暂无工具集</div>
                <div class="empty-text">点击"新建工具集"创建您的第一个外部工具集</div>
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
                            <span class="tool-type-tag ${set.tag || 'external'}">${getTypeLabel(set.tag || 'external')}</span>
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
                        <button class="btn btn-sm btn-danger" onclick="event.stopPropagation(); deleteSet('${set.id}')">删除</button>
                    </div>
                </div>
            </div>
        </div>
    `).join('');
}

function filterSets() {
    const type = document.getElementById('filterSetType').value;
    const name = document.getElementById('searchSetName').value.toLowerCase();

    let filtered = _allSets;
    if (type) filtered = filtered.filter(s => s.tag === type);
    if (name) filtered = filtered.filter(s => s.name.toLowerCase().includes(name));

    renderSets(filtered);
}

function viewSetDetail(id) {
    const set = _allSets.find(s => s.id === id);
    if (!set) return;

    document.getElementById('setsListView').style.display = 'none';
    document.getElementById('setDetailPage').style.display = 'block';

    document.getElementById('detailPageTitle').textContent = set.name;
    const subtitleParts = [];
    subtitleParts.push(getTypeLabel(set.tag || 'external'));
    subtitleParts.push(set.status === 1 ? '启用' : '禁用');
    document.getElementById('detailPageSubTitle').textContent = subtitleParts.join(' · ');

    document.getElementById('detailEditBtn').onclick = function() { editSet(id); };
    document.getElementById('detailDeleteBtn').style.display = '';
    document.getElementById('detailDeleteBtn').onclick = function() { deleteSet(id); };

    let configHtml = '';
    if (set.tag === 'mcp' && set.mcpConfig) {
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
    } else if (set.tag === 'http' && set.customConfig) {
        try {
            const config = typeof set.customConfig === 'string' ? JSON.parse(set.customConfig) : set.customConfig;
            configHtml = `
                <div class="card mb-3">
                    <div class="card-header bg-white fw-semibold">🌐 HTTP 配置</div>
                    <div class="card-body">
                        <pre class="mb-0" style="max-height:300px;overflow:auto;font-size:13px;">${JSON.stringify(config, null, 2)}</pre>
                    </div>
                </div>
            `;
        } catch (e) {
            configHtml = `
                <div class="card mb-3">
                    <div class="card-header bg-white fw-semibold">🌐 HTTP 配置</div>
                    <div class="card-body">
                        <pre class="mb-0" style="max-height:300px;overflow:auto;font-size:13px;">${set.customConfig}</pre>
                    </div>
                </div>
            `;
        }
    } else if (set.tag === 'skills' && set.customConfig) {
        try {
            const config = typeof set.customConfig === 'string' ? JSON.parse(set.customConfig) : set.customConfig;
            configHtml = `
                <div class="card mb-3">
                    <div class="card-header bg-white fw-semibold">🛠️ Skills 配置</div>
                    <div class="card-body">
                        <pre class="mb-0" style="max-height:300px;overflow:auto;font-size:13px;">${JSON.stringify(config, null, 2)}</pre>
                    </div>
                </div>
            `;
        } catch (e) {
            configHtml = `
                <div class="card mb-3">
                    <div class="card-header bg-white fw-semibold">🛠️ Skills 配置</div>
                    <div class="card-body">
                        <pre class="mb-0" style="max-height:300px;overflow:auto;font-size:13px;">${set.customConfig}</pre>
                    </div>
                </div>
            `;
        }
    }

    const tools = set.tools || [];
    const httpTools = set.httpTools || [];

    // 构建工具列表HTML
    let toolsHtml = '';

    // HTTP工具列表（http类型工具集）
    if (set.tag === 'http') {
        const addBtn = `<button class="btn btn-sm btn-outline-primary" onclick="event.stopPropagation(); openAddHttpToolModal('${set.id}')">+ 添加HTTP工具</button>`;
        if (httpTools.length > 0) {
            toolsHtml += `
                <div class="card mb-3">
                    <div class="card-header bg-white d-flex justify-content-between align-items-center">
                        <span class="fw-semibold">🌐 HTTP工具列表 (${httpTools.length})</span>
                        ${addBtn}
                    </div>
                    <div class="list-group list-group-flush">
                        ${httpTools.map(tool => `
                            <div class="list-group-item d-flex justify-content-between align-items-center">
                                <div>
                                    <div class="fw-semibold">${tool.name}</div>
                                    <div class="small text-muted">${tool.description || '暂无描述'}</div>
                                    <div class="small text-muted mt-1"><code>${tool.method || 'GET'}</code> ${tool.url || ''}</div>
                                </div>
                                <div class="d-flex gap-2 align-items-center">
                                    <span class="badge ${tool.status === 1 ? 'bg-success-subtle text-success' : 'bg-secondary-subtle text-secondary'}">${tool.status === 1 ? '启用' : '禁用'}</span>
                                    <button class="btn btn-sm btn-outline-secondary" onclick="event.stopPropagation(); openEditHttpToolModal('${set.id}', '${tool.id}')">编辑</button>
                                    <button class="btn btn-sm btn-outline-danger" onclick="event.stopPropagation(); deleteHttpTool('${set.id}', '${tool.id}')">删除</button>
                                </div>
                            </div>
                        `).join('')}
                    </div>
                </div>
            `;
        } else {
            toolsHtml += `
                <div class="card mb-3">
                    <div class="card-header bg-white d-flex justify-content-between align-items-center">
                        <span class="fw-semibold">🌐 HTTP工具列表</span>
                        ${addBtn}
                    </div>
                    <div class="card-body text-muted">暂无HTTP工具，点击上方按钮添加</div>
                </div>
            `;
        }
    }

    // Skills工具列表（skills类型工具集）
    if (set.tag === 'skills') {
        const skillsTools = tools.filter(t => t.type === 'skills');
        const uploadBtn = `<button class="btn btn-sm btn-outline-success" onclick="event.stopPropagation(); openUploadSkillsModal('${set.id}')">📁 上传Skill目录(zip)</button>`;
        if (skillsTools.length > 0) {
            toolsHtml += `
                <div class="card mb-3">
                    <div class="card-header bg-white d-flex justify-content-between align-items-center">
                        <span class="fw-semibold">⚡ Skills工具列表 (${skillsTools.length})</span>
                        ${uploadBtn}
                    </div>
                    <div class="list-group list-group-flush">
                        ${skillsTools.map(tool => `
                            <div class="list-group-item d-flex justify-content-between align-items-center">
                                <div>
                                    <div class="fw-semibold">${tool.name}</div>
                                    <div class="small text-muted">${tool.description || '暂无描述'}</div>
                                    ${tool.resourcePath ? `<div class="small text-muted mt-1"><code>${tool.resourcePath}</code></div>` : ''}
                                </div>
                                <div class="d-flex gap-2 align-items-center">
                                    <span class="badge ${tool.status === 1 ? 'bg-success-subtle text-success' : 'bg-secondary-subtle text-secondary'}">${tool.status === 1 ? '启用' : '禁用'}</span>
                                    <button class="btn btn-sm btn-outline-danger" onclick="event.stopPropagation(); deleteSkillsTool('${set.id}', '${tool.id}')">删除</button>
                                </div>
                            </div>
                        `).join('')}
                    </div>
                </div>
            `;
        } else {
            toolsHtml += `
                <div class="card mb-3">
                    <div class="card-header bg-white d-flex justify-content-between align-items-center">
                        <span class="fw-semibold">⚡ Skills工具列表</span>
                        ${uploadBtn}
                    </div>
                    <div class="card-body text-muted">
                        <p>暂无Skills工具，点击上方按钮上传 zip 文件</p>
                        <p class="small">zip 中每个一级子目录视为一个 skill，目录中需包含 <code>SKILL.md</code> 文件</p>
                    </div>
                </div>
            `;
        }
    }

    // MCP工具列表（只读展示）
    if (set.tag === 'mcp') {
        const mcpTools = tools.filter(t => t.type === 'mcp');
        if (mcpTools.length > 0) {
            toolsHtml += `
                <div class="card mb-3">
                    <div class="card-header bg-white fw-semibold">🤖 MCP工具列表 (${mcpTools.length})</div>
                    <div class="list-group list-group-flush">
                        ${mcpTools.map(tool => `
                            <div class="list-group-item d-flex justify-content-between align-items-center">
                                <div>
                                    <div class="fw-semibold">${tool.name}</div>
                                    <div class="small text-muted">${tool.description || '暂无描述'}</div>
                                </div>
                                <span class="badge ${tool.status === 1 ? 'bg-success-subtle text-success' : 'bg-secondary-subtle text-secondary'}">${tool.status === 1 ? '启用' : '禁用'}</span>
                            </div>
                        `).join('')}
                    </div>
                </div>
            `;
        }
    }

    const basicInfo = `
        <div class="card mb-3">
            <div class="card-header bg-white fw-semibold">📋 基本信息</div>
            <div class="card-body">
                <table class="table table-borderless mb-0">
                    <tr><td class="text-muted" style="width:120px;">名称</td><td>${set.name}</td></tr>
                    <tr><td class="text-muted">类型</td><td><span class="tool-type-tag ${set.tag || 'external'}">${getTypeLabel(set.tag || 'external')}</span></td></tr>
                    <tr><td class="text-muted">状态</td><td><span class="badge ${set.status === 1 ? 'bg-success-subtle text-success' : 'bg-secondary-subtle text-secondary'}">${set.status === 1 ? '启用' : '禁用'}</span></td></tr>
                    ${set.createdAt ? `<tr><td class="text-muted">创建时间</td><td>${new Date(set.createdAt).toLocaleString('zh-CN')}</td></tr>` : ''}
                </table>
            </div>
        </div>
    `;

    const descHtml = `
        <div class="card mb-3">
            <div class="card-header bg-white fw-semibold">📝 描述</div>
            <div class="card-body">
                <p class="text-muted mb-0">${set.description || '暂无描述'}</p>
            </div>
        </div>
    `;

    document.getElementById('setDetailContent').innerHTML = basicInfo + descHtml + configHtml + toolsHtml;
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// ==================== HTTP 工具管理 ====================

function openAddHttpToolModal(toolSetId) {
    createModal({
        title: '添加 HTTP 工具',
        size: 'lg',
        content: `
            <input type="hidden" id="httpToolSetId" value="${toolSetId}">
            <input type="hidden" id="httpToolId" value="">
            <div class="mb-3">
                <label class="form-label">工具名称 <span class="text-danger">*</span></label>
                <input type="text" class="form-control" id="httpToolName" placeholder="例如：查询用户信息">
            </div>
            <div class="mb-3">
                <label class="form-label">描述</label>
                <textarea class="form-control" id="httpToolDescription" rows="2" placeholder="工具功能描述..."></textarea>
            </div>
            <div class="row g-3 mb-3">
                <div class="col-md-3">
                    <label class="form-label">HTTP方法 <span class="text-danger">*</span></label>
                    <select class="form-select" id="httpToolMethod">
                        <option value="GET">GET</option>
                        <option value="POST">POST</option>
                        <option value="PUT">PUT</option>
                        <option value="DELETE">DELETE</option>
                        <option value="PATCH">PATCH</option>
                    </select>
                </div>
                <div class="col-md-9">
                    <label class="form-label">请求URL <span class="text-danger">*</span></label>
                    <input type="text" class="form-control" id="httpToolUrl" placeholder="https://api.example.com/users/{id}">
                </div>
            </div>
            <div class="mb-3">
                <label class="form-label">请求头 (JSON格式)</label>
                <textarea class="form-control font-monospace" id="httpToolHeaders" rows="3" placeholder='{"Content-Type": "application/json"}'></textarea>
            </div>
            <div class="mb-3">
                <label class="form-label">请求体模板</label>
                <textarea class="form-control font-monospace" id="httpToolBodyTemplate" rows="4" placeholder='{"name": "{name}"}'></textarea>
                <div class="form-hint">使用 {paramName} 作为参数占位符</div>
            </div>
            <div class="mb-3">
                <label class="form-label">响应解析模式</label>
                <input type="text" class="form-control" id="httpToolResponsePattern" placeholder="$.data[*] (JSONPath)">
            </div>
            <div class="mb-3">
                <label class="form-label">状态</label>
                <select class="form-select" id="httpToolStatus">
                    <option value="1" selected>启用</option>
                    <option value="0">禁用</option>
                </select>
            </div>
        `,
        onConfirm: () => saveHttpTool()
    });
}

function openEditHttpToolModal(toolSetId, toolId) {
    const set = _allSets.find(s => s.id === toolSetId);
    if (!set) return;
    const tool = (set.httpTools || []).find(t => t.id === toolId);
    if (!tool) { showToast('工具不存在', 'error'); return; }

    createModal({
        title: '编辑 HTTP 工具',
        size: 'lg',
        content: `
            <input type="hidden" id="httpToolSetId" value="${toolSetId}">
            <input type="hidden" id="httpToolId" value="${toolId}">
            <div class="mb-3">
                <label class="form-label">工具名称 <span class="text-danger">*</span></label>
                <input type="text" class="form-control" id="httpToolName" value="${escapeHtml(tool.name || '')}">
            </div>
            <div class="mb-3">
                <label class="form-label">描述</label>
                <textarea class="form-control" id="httpToolDescription" rows="2">${escapeHtml(tool.description || '')}</textarea>
            </div>
            <div class="row g-3 mb-3">
                <div class="col-md-3">
                    <label class="form-label">HTTP方法 <span class="text-danger">*</span></label>
                    <select class="form-select" id="httpToolMethod">
                        <option value="GET" ${tool.method === 'GET' ? 'selected' : ''}>GET</option>
                        <option value="POST" ${tool.method === 'POST' ? 'selected' : ''}>POST</option>
                        <option value="PUT" ${tool.method === 'PUT' ? 'selected' : ''}>PUT</option>
                        <option value="DELETE" ${tool.method === 'DELETE' ? 'selected' : ''}>DELETE</option>
                        <option value="PATCH" ${tool.method === 'PATCH' ? 'selected' : ''}>PATCH</option>
                    </select>
                </div>
                <div class="col-md-9">
                    <label class="form-label">请求URL <span class="text-danger">*</span></label>
                    <input type="text" class="form-control" id="httpToolUrl" value="${escapeHtml(tool.url || '')}">
                </div>
            </div>
            <div class="mb-3">
                <label class="form-label">请求头 (JSON格式)</label>
                <textarea class="form-control font-monospace" id="httpToolHeaders" rows="3">${escapeHtml(tool.headers || '')}</textarea>
            </div>
            <div class="mb-3">
                <label class="form-label">请求体模板</label>
                <textarea class="form-control font-monospace" id="httpToolBodyTemplate" rows="4">${escapeHtml(tool.requestBodyTemplate || '')}</textarea>
                <div class="form-hint">使用 {paramName} 作为参数占位符</div>
            </div>
            <div class="mb-3">
                <label class="form-label">响应解析模式</label>
                <input type="text" class="form-control" id="httpToolResponsePattern" value="${escapeHtml(tool.responseParsingPattern || '')}">
            </div>
            <div class="mb-3">
                <label class="form-label">状态</label>
                <select class="form-select" id="httpToolStatus">
                    <option value="1" ${tool.status === 1 ? 'selected' : ''}>启用</option>
                    <option value="0" ${tool.status === 0 ? 'selected' : ''}>禁用</option>
                </select>
            </div>
        `,
        onConfirm: () => saveHttpTool(toolId)
    });
}

async function saveHttpTool(toolId) {
    const toolSetId = document.getElementById('httpToolSetId').value;
    const name = document.getElementById('httpToolName').value.trim();
    const description = document.getElementById('httpToolDescription').value.trim();
    const method = document.getElementById('httpToolMethod').value;
    const url = document.getElementById('httpToolUrl').value.trim();
    const headers = document.getElementById('httpToolHeaders').value.trim();
    const requestBodyTemplate = document.getElementById('httpToolBodyTemplate').value.trim();
    const responseParsingPattern = document.getElementById('httpToolResponsePattern').value.trim();
    const status = parseInt(document.getElementById('httpToolStatus').value);

    if (!name) { showToast('请填写工具名称', 'warning'); return; }
    if (!url) { showToast('请填写请求URL', 'warning'); return; }

    // 验证Headers JSON格式
    if (headers) {
        try { JSON.parse(headers); } catch (e) { showToast('请求头 JSON 格式不正确', 'error'); return; }
    }

    const payload = { name, description, method, url, headers, requestBodyTemplate, responseParsingPattern, status };

    try {
        const isEdit = !!toolId;
        const response = await fetch(
            isEdit ? `${API_BASE}/${toolSetId}/http-tools/${toolId}` : `${API_BASE}/${toolSetId}/http-tools`,
            {
                method: isEdit ? 'PUT' : 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            }
        );

        if (response.ok) {
            showToast(isEdit ? 'HTTP工具更新成功' : 'HTTP工具添加成功', 'success');
            closeModal();
            await loadSets();
            viewSetDetail(toolSetId);
        } else {
            const err = await response.text();
            showToast((isEdit ? '更新' : '添加') + '失败: ' + err, 'error');
        }
    } catch (error) {
        showToast(error.message || '操作失败', 'error');
    }
}

async function deleteHttpTool(toolSetId, toolId) {
    if (!confirm('确定要删除这个HTTP工具吗？')) return;
    try {
        const response = await fetch(`${API_BASE}/${toolSetId}/http-tools/${toolId}`, { method: 'DELETE' });
        if (response.ok) {
            showToast('HTTP工具已删除', 'success');
            await loadSets();
            viewSetDetail(toolSetId);
        } else {
            const err = await response.text();
            showToast('删除失败: ' + err, 'error');
        }
    } catch (error) {
        showToast(error.message || '删除失败', 'error');
    }
}

// ==================== Skills 工具管理（目录上传） ====================

function openUploadSkillsModal(toolSetId) {
    createModal({
        title: '上传 Skill 目录 (zip)',
        size: 'lg',
        content: `
            <input type="hidden" id="uploadSkillSetId" value="${toolSetId}">
            <div class="mb-3">
                <label class="form-label">选择 zip 文件 <span class="text-danger">*</span></label>
                <input type="file" class="form-control" id="uploadSkillFile" accept=".zip" onchange="previewSkillZip(this)">
                <div class="form-hint">zip 中每个一级子目录视为一个 skill，目录中需包含 <code>SKILL.md</code> 文件</div>
            </div>
            <div class="mb-3">
                <label class="form-label">目录结构示例</label>
                <pre class="bg-light p-2 rounded" style="font-size:12px;">skills.zip
├── skill-a/
│   ├── SKILL.md
│   └── ...
└── skill-b/
    ├── SKILL.md
    └── ...</pre>
            </div>
            <div class="mb-3">
                <label class="form-label">SKILL.md 格式示例</label>
                <pre class="bg-light p-2 rounded" style="font-size:12px;">---
name: 数据分析
description: 对数据进行分析和统计
---

# 数据分析

## 使用场景
当需要对数据进行统计和分析时使用...

## 操作步骤
1. 读取数据文件
2. 执行统计分析
3. 输出结果</pre>
            </div>
            <div id="uploadSkillPreview" class="d-none">
                <label class="form-label">待导入的 Skills</label>
                <div class="list-group list-group-flush border" id="uploadSkillPreviewList"></div>
            </div>
        `,
        onConfirm: () => uploadSkills(toolSetId)
    });
}

async function previewSkillZip(input) {
    const file = input.files[0];
    if (!file) return;

    const previewDiv = document.getElementById('uploadSkillPreview');
    const previewList = document.getElementById('uploadSkillPreviewList');
    previewDiv.classList.remove('d-none');
    previewList.innerHTML = '<div class="list-group-item text-muted">正在解析 zip 文件...</div>';

    try {
        // 使用 JSZip 解析（如果页面已加载）
        if (typeof JSZip === 'undefined') {
            previewList.innerHTML = '<div class="list-group-item text-warning">⚠️ 无法预览目录结构，请直接上传</div>';
            return;
        }

        const zip = await JSZip.loadAsync(file);
        const topLevelDirs = new Set();
        zip.forEach((relativePath, zipEntry) => {
            const parts = relativePath.split('/');
            if (parts.length > 1 && parts[0]) {
                topLevelDirs.add(parts[0]);
            }
        });

        const dirs = Array.from(topLevelDirs);
        if (dirs.length === 0) {
            previewList.innerHTML = '<div class="list-group-item text-danger">❌ zip 中没有找到子目录</div>';
            return;
        }

        previewList.innerHTML = dirs.map(dir => {
            const hasSkillMd = zip.file(dir + '/SKILL.md') !== null;
            return `
                <div class="list-group-item d-flex justify-content-between align-items-center">
                    <span>📁 ${dir}</span>
                    <span class="badge ${hasSkillMd ? 'bg-success-subtle text-success' : 'bg-warning-subtle text-warning'}">
                        ${hasSkillMd ? '✓ 包含 SKILL.md' : '⚠️ 缺少 SKILL.md'}
                    </span>
                </div>
            `;
        }).join('');
    } catch (e) {
        previewList.innerHTML = '<div class="list-group-item text-warning">⚠️ 预览解析失败，不影响上传</div>';
    }
}

async function uploadSkills(toolSetId) {
    const fileInput = document.getElementById('uploadSkillFile');
    const file = fileInput.files[0];
    if (!file) { showToast('请选择 zip 文件', 'warning'); return; }

    const formData = new FormData();
    formData.append('file', file);

    const sendBtn = document.getElementById('mainModalConfirm');
    if (sendBtn) {
        sendBtn.disabled = true;
        sendBtn.innerHTML = '<span class="spinner-border spinner-border-sm"></span> 上传中...';
    }

    try {
        const response = await fetch(`${API_BASE}/${toolSetId}/skills/upload`, {
            method: 'POST',
            body: formData
        });

        const result = await response.json();
        if (result.success) {
            showToast(result.message || '上传成功', 'success');
            closeModal();
            await loadSets();
            viewSetDetail(toolSetId);
        } else {
            showToast(result.error || '上传失败', 'error');
        }
    } catch (error) {
        showToast(error.message || '上传失败', 'error');
    } finally {
        if (sendBtn) {
            sendBtn.disabled = false;
            sendBtn.innerHTML = '确认';
        }
    }
}

async function deleteSkillsTool(toolSetId, toolId) {
    if (!confirm('确定要删除这个Skills工具吗？')) return;
    try {
        const response = await fetch(`${TOOLS_API_BASE}/${toolId}`, { method: 'DELETE' });
        if (response.ok) {
            showToast('Skills工具已删除', 'success');
            await loadSets();
            viewSetDetail(toolSetId);
        } else {
            const err = await response.text();
            showToast('删除失败: ' + err, 'error');
        }
    } catch (error) {
        showToast(error.message || '删除失败', 'error');
    }
}

// HTML转义辅助函数
function escapeHtml(str) {
    if (!str) return '';
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
