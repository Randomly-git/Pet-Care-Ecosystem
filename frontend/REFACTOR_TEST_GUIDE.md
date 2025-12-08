# 前端重构测试指南

## 快速测试步骤

### 1. 启动开发服务器

```bash
# 进入前端目录
cd frontend

# 安装依赖（如果还没安装）
npm install

# 启动开发服务器
npm run dev
```

### 2. 检查浏览器控制台

打开浏览器访问 `http://localhost:5173`，查看控制台输出：

**成功标志：**
- ✅ 看到 "🚀 初始化应用..."
- ✅ 看到 "🔐 用户认证状态: 未登录"
- ✅ 看到 "✅ 应用初始化完成"
- ✅ 看到 "📝 开发模式：已启用调试日志"
- ✅ 看到 "🔧 调试工具已加载"
- ✅ 看到 "📡 API服务已加载 - 新版本"

**如果有错误：**
- ❌ 检查错误信息并参考下面的故障排除

### 3. 测试API功能

在浏览器控制台中运行以下命令：

```javascript
// 1. 测试用户注册
window.__debug__.auth.register({
  name: 'testuser_' + Date.now(),
  password: 'password123'
}).then(result => console.log('注册结果:', result))

// 2. 测试用户登录
window.__debug__.auth.login({
  name: 'testuser',
  password: 'password123'
}).then(result => console.log('登录结果:', result))

// 3. 测试获取宠物列表（需要先登录）
window.__debug__.api.pet.getPets(1).then(pets => console.log('宠物列表:', pets))

// 4. 测试获取活动种类
window.__debug__.api.activity.getActivityKinds().then(kinds => console.log('活动种类:', kinds))
```

### 4. 测试路由导航

点击页面上的导航链接，观察控制台输出：
- 应该看到 "📍 路由切换: /old-path → /new-path"

### 5. 测试错误处理

故意使用错误的数据：

```javascript
// 测试获取不存在的宠物
window.__debug__.api.pet.getPet(99999).catch(error => console.log('错误处理:', error.message))
```

## 故障排除

### 问题1: Module not found错误

**错误信息：** `Failed to resolve import "@/services/api-new"`

**解决方案：**
1. 检查 `vite.config.js` 中的路径别名配置
2. 确保所有新创建的文件都在正确位置

### 问题2: TypeError: Cannot read properties of undefined

**错误信息：** `Cannot read properties of undefined (reading 'getPets')`

**解决方案：**
1. 检查是否正确导入了API服务
2. 确认 `api-new.js` 文件没有语法错误

### 问题3: CORS错误

**错误信息：** `Access to fetch at 'http://localhost:8082' has been blocked by CORS policy`

**解决方案：**
1. 确保后端服务已启动
2. 检查后端的CORS配置

### 问题4: 网络连接错误

**错误信息：** `Failed to fetch` 或 `NetworkError`

**解决方案：**
1. 确认后端服务正在运行
2. 检查端口号是否正确
3. 尝试直接访问后端API

## 调试技巧

### 1. 使用浏览器开发者工具

```javascript
// 查看所有调试工具
console.log(window.__debug__)

// 查看当前配置
console.log(window.__debug__.config.getMicroserviceConfig())

// 清除所有缓存
window.__debug__.api.cache.clearAll()
```

### 2. 启用详细日志

```javascript
// 设置为调试级别
window.__debug__.config.setLogLevel('debug')

// 查看缓存统计
console.log(window.__debug__.api.cache.getStats())
```

### 3. 测试组合式API

在Vue组件中使用：

```javascript
import { useAuth, usePets } from '@/composables/useApi'

setup() {
  const { user, login, isAuthenticated } = useAuth()
  const { pets, isLoading } = usePets(1)

  return { user, login, isAuthenticated, pets, isLoading }
}
```

## 性能检查

### 1. 检查网络请求

在浏览器开发者工具的Network标签页：
- 观察API请求是否正常发送
- 检查响应时间
- 查看是否有重复请求

### 2. 检查内存使用

```javascript
// 查看缓存占用
window.__debug__.api.cache.getStats()
```

## 回滚方案

如果新API出现问题，可以快速回滚到旧版本：

1. 修改 `main.js` 中的导入语句：

```javascript
// 从新版本（当前）
import apiService from '@/services/api-new'

// 改为旧版本
import apiService from '@/services/api'
```

2. 重启开发服务器

## 预期改进

使用新API后，你应该能看到：

1. **更少的控制台错误** - 统一的错误处理
2. **更快的响应速度** - 智能缓存
3. **更好的调试体验** - 详细的日志
4. **自动重试** - 网络错误时自动重试
5. **加载状态** - 自动的loading状态管理

## 下一步

1. 逐步将组件中的旧API调用替换为新API
2. 使用组合式API重构组件逻辑
3. 利用缓存机制优化性能
4. 根据实际需求调整配置

## 需要帮助？

如果遇到问题，请查看：
1. 浏览器控制台的具体错误信息
2. 浏览器开发者工具的Network标签页
3. 后端服务的日志

并提供以下信息：
- 完整的错误堆栈
- 相关的代码片段
- 复现步骤