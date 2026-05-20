# API 渗透测试报告 (Penetration Test Report)

> **测试目标**：Pet-Care-Ecosystem 微服务系统  
> **测试入口**：`http://localhost:9000`（API Gateway）  
> **测试日期**：2026-05-19  
> **测试工具**：`curl.exe`（Windows 内置）

---

## 测试结果汇总

| # | 测试项 | 攻击向量 | HTTP 状态 | 结果 | 说明 |
|---|--------|----------|-----------|------|------|
| 1 | 认证测试 | 弱口令 admin/admin | 400 | ⚠️ BAD_REQUEST | 口令被拒绝；需进一步测试暴力破解防护 |
| 2 | SQL 注入 | `GET /api/pets/1' OR '1'='1` | 400 | ✅ 安全 | Spring 类型转换拦截（`Long` 类型无法解析注入字符串） |
| 3 | 路径遍历 | `GET /api/media/file/../etc/passwd` | 400 | ✅ 安全 | Spring 路由规范化拒绝非法路径 |
| 4 | XSS 注入 | `POST /api/v1/moments content=<script>` | 400（空 mediaIds） | ⚠️ 未测试到 | 命令转义问题；XSS 风险见静态代码审查 |
| 5 | 输入验证 | `PATCH /batch/related mediaIds=[]` | 500 | ⚠️ 服务器错误 | 空列表触发 `@NotEmpty` 校验：返回 500 系统错误而非 400 参数错误 |
| 6 | 资源不存在 | `GET /api/media/99999` | 400 | ✅ 安全 | 返回 `"媒体文件不存在, ID: 99999"`，无信息泄露 |
| 7 | CORS 越权 | `Origin: https://evil.com` | 403 | ✅ 安全 | 网关正确拒绝跨域预检请求 |
| 8 | 无认证访问 | `GET /api/pets/1`（无 Token） | 200 | 🔴 **漏洞** | 宠物信息接口**无需认证即可访问**，返回完整宠物数据 |

---

## 关键发现

### 🔴 Finding 1：`GET /api/pets/{petId}` 缺少认证保护

```
HTTP 200: {"petId":1,"name":"测试宠物","species":"cat","breed":"111",...}
```

**影响**：任何人无需 Token 即可查询任意宠物信息，包括用户关联数据（`userId`、`userName`）。
**修复**：在 `petcare-backend` 的 `SecurityConfig` 中对 `/api/pets/**` 添加 JWT 认证拦截。

### ⚠️ Finding 2：`PATCH /batch/related` 空列表返回 500 而非 400

```
输入: {"mediaIds":[],...}
输出: {"code":50000,"message":"系统繁忙，请稍后再试"}
```

**影响**：`@NotEmpty` 校验失败后抛出的异常被全局异常处理器转为 500，而非返回清晰的 400 参数错误，不利于客户端调试。

### ✅ Finding 3：SQL 注入防御有效

Spring MVC 类型转换（`@PathVariable Long`）天然阻止了 SQL 注入，但**控制台日志仍应扫描异常请求模式**。

### ✅ Finding 4：CORS 策略正确

网关拒绝来自非允许源的跨域请求（403），CORS 配置合理。

---

## 静态代码审查额外发现

| 文件 | 问题 | 严重度 |
|------|------|--------|
| `PetHealthResolver.java:38` | 硬编码 `QWEN_API_KEY` | 🔴 HIGH |
| `AppHeader.vue:117` | `v-html` 可能导致 XSS | 🟡 MEDIUM |
| `AppSidebar.vue:28` | `v-html` 可能导致 XSS | 🟡 MEDIUM |

---

## 截图清单

| 截图编号 | 内容 | 命令 |
|----------|------|------|
| SS-01 | SQL 注入被 400 拦截 | `curl http://localhost:9000/api/pets/1' OR '1'='1` |
| SS-02 | 路径遍历被 400 拦截 | `curl http://localhost:9000/api/media/file/../etc/passwd` |
| SS-03 | CORS 越权被 403 拒绝 | `curl -I -H "Origin: https://evil.com" ...` |
| SS-04 | 无认证访问返回宠物数据 🔴 | `curl http://localhost:9000/api/pets/1` |
| SS-05 | 空列表返回 500 而非 400 | `curl -X PATCH ... -d '{"mediaIds":[]}'` |
