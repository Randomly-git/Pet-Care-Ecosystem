// Node.js 代理服务器 - 绕过 CORS 限制
const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');
const cors = require('cors');

const app = express();
const PORT = 3000;

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

// 代理所有 /api/* 请求到社区后端
app.use('/api', createProxyMiddleware({
    target: 'http://localhost:8083',
    changeOrigin: true,
    pathRewrite: {
        '^/api': '/api/v1',  // 将 /api 转换为 /api/v1
    },
    onProxyReq: (proxyReq, req, res) => {
        console.log(`代理请求: ${req.method} ${req.path} -> http://localhost:8083${proxyReq.path}`);
    },
    onProxyRes: (proxyRes, req, res) => {
        console.log(`代理响应: ${proxyRes.statusCode} ${req.method} ${req.path}`);
    },
    onError: (err, req, res) => {
        console.error('代理错误:', err.message);
        res.status(500).json({
            error: '代理服务器错误',
            message: err.message
        });
    }
}));

// 启动服务器
app.listen(PORT, () => {
    console.log(`========================================`);
    console.log(`🚀 代理服务器已启动`);
    console.log(`📍 地址: http://localhost:${PORT}`);
    console.log(`🎯 目标: http://localhost:8083 (社区后端)`);
    console.log(`========================================`);
    console.log('\n使用方法:');
    console.log('1. 打开浏览器访问 test_api_with_server.html');
    console.log('2. 或直接使用 API: http://localhost:3000/api/moments/user/76');
    console.log('\n按 Ctrl+C 停止服务器');
});