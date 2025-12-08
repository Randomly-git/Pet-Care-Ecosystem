const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');
const cors = require('cors');
const axios = require('axios');

const app = express();
const PORT = 3000;

// 启用 CORS
app.use(cors({
    origin: '*',
    credentials: true
}));

// 解析 JSON 请求体
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// 日志中间件
app.use((req, res, next) => {
    console.log(`${new Date().toISOString()} - ${req.method} ${req.path}`);
    next();
});

// 健康检查端点
app.get('/api/test', (req, res) => {
    res.json({
        status: 'ok',
        message: '代理服务器正在运行',
        timestamp: new Date().toISOString()
    });
});

// 社区 API 代理 - 使用 axios 手动转发
app.all(/\/api\/community\/.*/, async (req, res) => {
    try {
        const targetPath = req.path.replace('/api/community', '/api/v1');
        const targetUrl = `http://localhost:8083${targetPath}${req.url.includes('?') ? req.url.split('?')[1] : ''}`;

        console.log(`[社区] 代理请求: ${req.method} ${req.path} -> ${targetUrl}`);

        const response = await axios({
            method: req.method,
            url: targetUrl.startsWith('http') ? targetUrl : `http://localhost:8083${targetPath}`,
            data: req.body,
            params: req.query,
            headers: {
                ...req.headers,
                host: 'localhost:8083'
            },
            transformRequest: [function (data, headers) {
                // 移除可能引起问题的 headers
                delete headers['content-length'];
                return JSON.stringify(data);
            }]
        });

        console.log(`[社区] 代理响应: ${response.status} ${req.method} ${req.path}`);
        res.status(response.status).json(response.data);
    } catch (error) {
        console.error('[社区] 代理错误:', error.message);
        if (error.response) {
            res.status(error.response.status).json(error.response.data);
        } else {
            res.status(500).json({
                error: '社区服务代理错误',
                message: error.message
            });
        }
    }
});

// 媒体 API 代理 - 使用 axios 手动转发
app.all(/\/api\/media\/.*/, async (req, res) => {
    try {
        const targetPath = req.path.replace('/api/media', '/api/v1/media');
        const targetUrl = `http://localhost:8081${targetPath}`;

        console.log(`[媒体] 代理请求: ${req.method} ${req.path} -> ${targetUrl}`);

        // 特殊处理文件上传
        if (req.headers['content-type'] && req.headers['content-type'].includes('multipart/form-data')) {
            const FormData = require('form-data');
            const form = new FormData();

            // 这里需要处理文件上传，暂时跳过
            res.status(500).json({
                error: '文件上传功能暂时不可用',
                message: '请稍后再试'
            });
            return;
        }

        const response = await axios({
            method: req.method,
            url: targetUrl,
            data: req.body,
            params: req.query,
            headers: {
                ...req.headers,
                host: 'localhost:8081'
            }
        });

        console.log(`[媒体] 代理响应: ${response.status} ${req.method} ${req.path}`);
        res.status(response.status).json(response.data);
    } catch (error) {
        console.error('[媒体] 代理错误:', error.message);
        if (error.response) {
            res.status(error.response.status).json(error.response.data);
        } else {
            res.status(500).json({
                error: '媒体服务代理错误',
                message: error.message
            });
        }
    }
});

// 默认 PetCare API 代理
app.all(/\/api\/.*/, async (req, res) => {
    try {
        const targetUrl = `http://localhost:8082${req.path}`;

        console.log(`[PetCare] 代理请求: ${req.method} ${req.path} -> ${targetUrl}`);

        const response = await axios({
            method: req.method,
            url: targetUrl,
            data: req.body,
            params: req.query,
            headers: {
                ...req.headers,
                host: 'localhost:8082'
            }
        });

        console.log(`[PetCare] 代理响应: ${response.status} ${req.method} ${req.path}`);
        res.status(response.status).json(response.data);
    } catch (error) {
        console.error('[PetCare] 代理错误:', error.message);
        if (error.response) {
            res.status(error.response.status).json(error.response.data);
        } else {
            res.status(500).json({
                error: 'PetCare服务代理错误',
                message: error.message
            });
        }
    }
});

// 启动服务器
app.listen(PORT, () => {
    console.log(`========================================`);
    console.log(`🚀 简易代理服务器已启动`);
    console.log(`📍 地址: http://localhost:${PORT}`);
    console.log(`========================================`);
    console.log('📋 路由规则:');
    console.log('  /api/community/* -> http://localhost:8083/api/v1/*');
    console.log('  /api/media/* -> http://localhost:8081/api/v1/media/*');
    console.log('  /api/* -> http://localhost:8082/*');
    console.log('========================================');
    console.log('\n✅ 前端现在可以访问所有API，无需浏览器插件');
    console.log('✅ 访问 http://localhost:5177 测试功能');
    console.log('\n按 Ctrl+C 停止服务器');
});