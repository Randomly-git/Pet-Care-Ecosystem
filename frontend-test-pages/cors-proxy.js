// Node.js 代理服务器 - 绕过 CORS 限制
const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');
const cors = require('cors');

const app = express();
const PORT = 3001;

// 启用 CORS
app.use(cors({
    origin: '*',
    credentials: true
}));

// 解析 JSON 请求体
app.use(express.json());

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

// 社区 API 代理
app.use('/api/community', createProxyMiddleware({
    target: 'http://localhost:8083',
    changeOrigin: true,
    pathRewrite: {
        '^/api/community': '/api/v1'
    },
    onProxyReq: (proxyReq, req, res) => {
        console.log(`[社区] 代理请求: ${req.method} ${req.path} -> http://localhost:8083${proxyReq.path}`);
    },
    onProxyRes: (proxyRes, req, res) => {
        console.log(`[社区] 代理响应: ${proxyRes.statusCode} ${req.method} ${req.path}`);
    },
    onError: (err, req, res) => {
        console.error('[社区] 代理错误:', err.message);
        res.status(500).json({
            error: '社区服务代理错误',
            message: err.message
        });
    }
}));

// 媒体 API 代理
app.use('/api/media', createProxyMiddleware({
    target: 'http://localhost:8081',
    changeOrigin: true,
    pathRewrite: {
        '^/api/media': '/api/v1/media'
    },
    onProxyReq: (proxyReq, req, res) => {
        console.log(`[媒体] 代理请求: ${req.method} ${req.path} -> http://localhost:8081${proxyReq.path}`);
    },
    onProxyRes: (proxyRes, req, res) => {
        console.log(`[媒体] 代理响应: ${proxyRes.statusCode} ${req.method} ${req.path}`);
    },
    onError: (err, req, res) => {
        console.error('[媒体] 代理错误:', err.message);
        res.status(500).json({
            error: '媒体服务代理错误',
            message: err.message
        });
    }
}));

// 默认 PetCare API 代理（其他所有 /api 请求）
app.use('/api', createProxyMiddleware({
    target: 'http://localhost:8082',
    changeOrigin: true,
    onProxyReq: (proxyReq, req, res) => {
        console.log(`[PetCare] 代理请求: ${req.method} ${req.path} -> http://localhost:8082${proxyReq.path}`);
    },
    onProxyRes: (proxyRes, req, res) => {
        console.log(`[PetCare] 代理响应: ${proxyRes.statusCode} ${req.method} ${req.path}`);
    },
    onError: (err, req, res) => {
        console.error('[PetCare] 代理错误:', err.message);
        res.status(500).json({
            error: 'PetCare服务代理错误',
            message: err.message
        });
    }
}));

// 启动服务器
app.listen(PORT, () => {
    console.log(`========================================`);
    console.log(`🚀 CORS代理服务器已启动`);
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