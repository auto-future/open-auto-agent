// ==================== HTTP 接口测试页面 ====================

const HTTP_TEST_PROXY = '/api/http-test/proxy';

// 初始化
document.addEventListener('DOMContentLoaded', function() {
    httpTestAddHeader();
    httpTestAddParam();
});

// ==================== Headers / Params 动态行 ====================

function httpTestAddHeader() {
    const list = document.getElementById('httpTestHeaderList');
    const item = document.createElement('div');
    item.className = 'kv-item';
    item.innerHTML = `
        <input type="text" class="form-control form-control-sm kv-key" placeholder="Header 名称">
        <input type="text" class="form-control form-control-sm kv-value" placeholder="Header 值">
        <button class="btn-icon" onclick="this.parentElement.remove()" title="删除">×</button>
    `;
    list.appendChild(item);
}

function httpTestAddParam() {
    const list = document.getElementById('httpTestParamList');
    const item = document.createElement('div');
    item.className = 'kv-item';
    item.innerHTML = `
        <input type="text" class="form-control form-control-sm kv-key" placeholder="参数名">
        <input type="text" class="form-control form-control-sm kv-value" placeholder="参数值">
        <button class="btn-icon" onclick="this.parentElement.remove()" title="删除">×</button>
    `;
    list.appendChild(item);
}

// ==================== Auth 切换 ====================

function httpTestToggleAuth() {
    const type = document.getElementById('httpTestAuthType').value;
    document.getElementById('httpTestAuthBearer').style.display = type === 'bearer' ? 'block' : 'none';
    document.getElementById('httpTestAuthBasic').style.display = type === 'basic' ? 'block' : 'none';
    document.getElementById('httpTestAuthApikey').style.display = type === 'apikey' ? 'block' : 'none';
}

// ==================== 收集请求数据 ====================

function httpTestCollectHeaders() {
    const headers = {};
    document.querySelectorAll('#httpTestHeaderList .kv-item').forEach(item => {
        const key = item.querySelector('.kv-key').value.trim();
        const value = item.querySelector('.kv-value').value.trim();
        if (key) headers[key] = value;
    });
    return headers;
}

function httpTestCollectParams() {
    const params = [];
    document.querySelectorAll('#httpTestParamList .kv-item').forEach(item => {
        const key = item.querySelector('.kv-key').value.trim();
        const value = item.querySelector('.kv-value').value.trim();
        if (key) params.push({ key, value });
    });
    return params;
}

function httpTestCollectAuth() {
    const type = document.getElementById('httpTestAuthType').value;
    const auth = { type };
    if (type === 'bearer') {
        auth.token = document.getElementById('httpTestBearerToken').value.trim();
    } else if (type === 'basic') {
        auth.username = document.getElementById('httpTestBasicUser').value.trim();
        auth.password = document.getElementById('httpTestBasicPass').value.trim();
    } else if (type === 'apikey') {
        auth.keyName = document.getElementById('httpTestApiKeyName').value.trim();
        auth.keyValue = document.getElementById('httpTestApiKeyValue').value.trim();
    }
    return auth;
}

function httpTestBuildUrl() {
    let url = document.getElementById('httpTestUrl').value.trim();
    if (!url) return '';

    // 追加 Query Params
    const params = httpTestCollectParams();
    if (params.length > 0) {
        const separator = url.includes('?') ? '&' : '?';
        const queryStr = params
            .filter(p => p.value)
            .map(p => encodeURIComponent(p.key) + '=' + encodeURIComponent(p.value))
            .join('&');
        if (queryStr) url += separator + queryStr;
    }
    return url;
}

// ==================== 发送请求 ====================

async function httpTestSend() {
    const url = httpTestBuildUrl();
    if (!url) {
        showToast('请输入请求 URL', 'warning');
        return;
    }

    const method = document.getElementById('httpTestMethod').value;
    const headers = httpTestCollectHeaders();
    const auth = httpTestCollectAuth();
    const timeout = parseInt(document.getElementById('httpTestTimeout').value) || 30000;
    const followRedirect = document.getElementById('httpTestFollowRedirect').value === 'true';
    const ignoreSsl = document.getElementById('httpTestIgnoreSsl').checked;
    const bodyType = document.getElementById('httpTestBodyType').value;
    const bodyContent = document.getElementById('httpTestBodyContent').value.trim();

    // 构建请求体
    let body = null;
    if (['POST', 'PUT', 'PATCH'].includes(method) && bodyType !== 'none' && bodyContent) {
        if (bodyType === 'json') {
            try {
                body = JSON.parse(bodyContent);
            } catch (e) {
                showToast('JSON 格式不正确，请检查', 'error');
                return;
            }
        } else if (bodyType === 'text') {
            body = bodyContent;
        } else if (bodyType === 'form') {
            try {
                const obj = JSON.parse(bodyContent);
                body = obj;
            } catch (e) {
                showToast('Form Data 请使用 JSON 格式填写', 'error');
                return;
            }
        }
    }

    // 设置 Content-Type
    if (body && !headers['Content-Type'] && !headers['content-type']) {
        if (bodyType === 'json') headers['Content-Type'] = 'application/json';
        else if (bodyType === 'text') headers['Content-Type'] = 'text/plain';
    }

    // 显示 loading
    const sendBtn = document.getElementById('httpTestSendBtn');
    sendBtn.disabled = true;
    sendBtn.innerHTML = '<span class="spinner-border spinner-border-sm"></span> 发送中...';

    const responseContainer = document.getElementById('httpTestResponseContainer');
    responseContainer.innerHTML = `
        <div class="http-test-loading">
            <div class="spinner-border text-primary" role="status"></div>
            <span class="ms-2">正在发送请求...</span>
        </div>
    `;

    try {
        const res = await fetch(HTTP_TEST_PROXY, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ method, url, headers, body, auth, timeout, followRedirect, ignoreSsl })
        });

        const data = await res.json();

        if (data.success) {
            httpTestRenderResponse(data);
        } else {
            httpTestRenderError(data.error || '请求失败', data.duration || 0);
        }
    } catch (e) {
        httpTestRenderError('网络错误: ' + e.message, 0);
    } finally {
        sendBtn.disabled = false;
        sendBtn.innerHTML = '<span>▶</span> 发送请求';
    }
}

// ==================== 渲染响应 ====================

function httpTestRenderResponse(data) {
    const container = document.getElementById('httpTestResponseContainer');
    const meta = document.getElementById('httpTestResponseMeta');
    meta.style.display = 'flex';

    // 状态码
    const statusCode = document.getElementById('httpTestStatusCode');
    const sc = data.statusCode;
    let statusClass = 'text-success';
    if (sc >= 300 && sc < 400) statusClass = 'text-warning';
    else if (sc >= 400) statusClass = 'text-danger';
    statusCode.className = 'http-test-status fw-bold ' + statusClass;
    statusCode.textContent = sc + ' ' + (data.statusText || '');

    // 耗时
    document.getElementById('httpTestResponseTime').textContent = (data.duration || 0) + ' ms';

    // 大小
    const bodyStr = data.body || '';
    const sizeBytes = new Blob([bodyStr]).size;
    let sizeStr;
    if (sizeBytes < 1024) sizeStr = sizeBytes + ' B';
    else if (sizeBytes < 1024 * 1024) sizeStr = (sizeBytes / 1024).toFixed(1) + ' KB';
    else sizeStr = (sizeBytes / 1024 / 1024).toFixed(2) + ' MB';
    document.getElementById('httpTestResponseSize').textContent = sizeStr;

    // 渲染响应内容
    let formattedBody = bodyStr;
    let isJson = false;
    if (data.parsedBody) {
        formattedBody = JSON.stringify(data.parsedBody, null, 2);
        isJson = true;
    } else {
        try {
            const parsed = JSON.parse(bodyStr);
            formattedBody = JSON.stringify(parsed, null, 2);
            isJson = true;
        } catch (e) { /* 非 JSON */ }
    }

    // 响应头
    let headersHtml = '';
    if (data.headers) {
        const entries = Object.entries(data.headers);
        headersHtml = `
            <div class="http-test-response-section">
                <div class="http-test-response-section-title">响应头</div>
                <div class="http-test-headers-table">
                    ${entries.map(([k, v]) => `
                        <div class="http-test-header-row">
                            <span class="http-test-header-key">${escapeHtml(k)}</span>
                            <span class="http-test-header-val">${escapeHtml(v)}</span>
                        </div>
                    `).join('')}
                </div>
            </div>
        `;
    }

    container.innerHTML = `
        <div class="http-test-response-tabs">
            <button class="btn btn-sm btn-outline-primary active" onclick="httpTestSwitchResponseTab('body', this)">Body</button>
            <button class="btn btn-sm btn-outline-secondary" onclick="httpTestSwitchResponseTab('headers', this)">Headers</button>
        </div>
        <div id="httpTestResponseBody" class="http-test-response-pane">
            <pre class="http-test-response-body ${isJson ? 'json' : 'text'}">${escapeHtml(formattedBody)}</pre>
        </div>
        <div id="httpTestResponseHeaders" class="http-test-response-pane" style="display:none;">
            ${headersHtml}
        </div>
    `;
}

function httpTestRenderError(errorMsg, duration) {
    const container = document.getElementById('httpTestResponseContainer');
    const meta = document.getElementById('httpTestResponseMeta');
    meta.style.display = 'flex';

    document.getElementById('httpTestStatusCode').className = 'http-test-status fw-bold text-danger';
    document.getElementById('httpTestStatusCode').textContent = 'Error';
    document.getElementById('httpTestResponseTime').textContent = duration + ' ms';
    document.getElementById('httpTestResponseSize').textContent = '-';

    container.innerHTML = `
        <div class="http-test-error">
            <div class="http-test-error-icon">❌</div>
            <div class="http-test-error-msg">${escapeHtml(errorMsg)}</div>
        </div>
    `;
}

function httpTestSwitchResponseTab(tab, btn) {
    document.querySelectorAll('.http-test-response-tabs .btn').forEach(b => {
        b.classList.remove('btn-outline-primary', 'active');
        b.classList.add('btn-outline-secondary');
    });
    btn.classList.remove('btn-outline-secondary');
    btn.classList.add('btn-outline-primary', 'active');

    document.getElementById('httpTestResponseBody').style.display = tab === 'body' ? 'block' : 'none';
    document.getElementById('httpTestResponseHeaders').style.display = tab === 'headers' ? 'block' : 'none';
}

// ==================== 清空 ====================

function httpTestClearAll() {
    document.getElementById('httpTestUrl').value = '';
    document.getElementById('httpTestMethod').value = 'GET';
    document.getElementById('httpTestHeaderList').innerHTML = '';
    document.getElementById('httpTestParamList').innerHTML = '';
    document.getElementById('httpTestBodyContent').value = '';
    document.getElementById('httpTestBodyType').value = 'json';
    document.getElementById('httpTestAuthType').value = 'none';
    httpTestToggleAuth();
    document.getElementById('httpTestTimeout').value = '30000';

    document.getElementById('httpTestResponseMeta').style.display = 'none';
    document.getElementById('httpTestResponseContainer').innerHTML = `
        <div class="empty-state" style="padding: 40px;">
            <div class="empty-icon">🌐</div>
            <div class="empty-title">等待发送请求</div>
            <div class="empty-text">输入 URL 并点击发送按钮</div>
        </div>
    `;

    httpTestAddHeader();
    httpTestAddParam();
    showToast('已清空所有配置', 'info');
}

// ==================== 工具函数 ====================

function escapeHtml(str) {
    if (!str) return '';
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
