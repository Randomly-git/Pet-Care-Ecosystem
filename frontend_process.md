# 笑猫の窝 - 前端重构技术文档

## 项目概况
- **目标**：将现有 Vue 3 宠物社交平台重构为现代化、专业的前端应用
- **技术栈**：Vue 3 + Element Plus + Pinia + Vue Router
- **预计周期**：2-3周
- **API 基础**：http://localhost:5001/api/v1

---

## 重构路线图

### 🎯 Phase 1: 基础设施搭建（第1周，Day 1-3）

#### Task 1.1: 设计系统建立 ✓ [2小时]
**目标**：统一视觉语言和样式规范

**交付物**：
- [ ] `src/assets/styles/design-tokens.css` - 设计变量
- [ ] `src/assets/styles/reset.css` - CSS 重置
- [ ] `src/assets/styles/utilities.css` - 工具类

**验收标准**：
- 所有颜色、间距、字体使用 CSS 变量
- 全局样式生效

---

#### Task 1.2: 基础组件库 ✓ [1天]
**目标**：创建可复用的通用组件

**交付物**：
```
src/components/base/
├── BaseButton.vue       [核心组件]
├── BaseCard.vue         [容器组件]
├── BaseAvatar.vue       [头像组件]
├── BaseInput.vue        [输入框]
├── BaseModal.vue        [弹窗]
└── index.js             [统一导出]
```

**优先级顺序**：
1. BaseButton（最高优先级）
2. BaseCard
3. BaseAvatar
4. BaseInput
5. BaseModal

**验收标准**：
- 每个组件支持基本 props 和事件
- 支持 variant、size 等常用配置
- 有 hover、active、disabled 状态

---

#### Task 1.3: 项目结构重组 ✓ [1天]
**目标**：优化代码组织结构

**操作清单**：
- [ ] 创建 `src/composables/` 目录
- [ ] 创建 `src/services/` 目录
- [ ] 创建 `src/utils/` 目录
- [ ] 重组 `src/stores/` 目录
- [ ] 配置 `vite.config.js` 路径别名

**交付物**：
```javascript
// vite.config.js
export default {
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      '@components': fileURLToPath(new URL('./src/components', import.meta.url)),
      '@utils': fileURLToPath(new URL('./src/utils', import.meta.url)),
    }
  }
}
```

**验收标准**：
- 目录结构清晰
- 导入路径使用别名

---

### 🔧 Phase 2: 核心功能层（第1周，Day 4-5）

#### Task 2.1: API 服务层封装 ✓ [3小时]
**目标**：统一 API 调用，处理错误和拦截

**交付物**：
```
src/services/
├── api.js              [Axios 实例配置]
├── petService.js       [宠物相关 API]
├── momentService.js    [动态相关 API]
├── medicalService.js   [医疗相关 API]
└── shoppingService.js  [商城相关 API]
```

**核心代码结构**：
```javascript
// api.js
- 创建 axios 实例
- 请求拦截器（添加 token）
- 响应拦截器（错误处理）

// petService.js
- getPets()
- getPetById(id)
- createPet(data)
- updatePet(id, data)
```

**验收标准**：
- 所有 API 调用经过拦截器
- 401/403/500 错误统一处理
- 请求自动携带 token

---

#### Task 2.2: Composables 封装 ✓ [4小时]
**目标**：复用业务逻辑

**交付物**：
```
src/composables/
├── useAuth.js          [认证状态]
├── usePet.js           [宠物操作]
├── useMoment.js        [动态操作]
├── useToast.js         [消息提示]
└── useInfiniteScroll.js [无限滚动]
```

**核心功能**：
```javascript
// usePet.js
export function usePet() {
  const currentPet = ref(null)
  const loading = ref(false)
  
  const fetchPet = async (id) => { ... }
  const switchPet = (pet) => { ... }
  
  return { currentPet, loading, fetchPet, switchPet }
}
```

**验收标准**：
- 可在多个组件中复用
- 包含 loading、error 状态
- 响应式数据更新

---

#### Task 2.3: Pinia Store 设置 ✓ [3小时]
**目标**：集中管理全局状态

**交付物**：
```
src/stores/
├── auth.js             [用户认证]
├── pet.js              [宠物信息]
├── moment.js           [动态数据]
└── ui.js               [UI 状态]
```

**验收标准**：
- 每个 store 有明确职责
- 包含 state、getters、actions
- 支持持久化（localStorage）

---

### 🎨 Phase 3: 页面重构（第2周）

#### Task 3.1: 首页重构 ✓ [1天]
**目标**：实现现代化首页设计

**交付物**：
- [ ] `src/views/Home/HomeView.vue`
- [ ] `src/views/Home/components/HeroSection.vue`
- [ ] `src/views/Home/components/FeatureCards.vue`

**功能点**：
- Hero 区域（标题、CTA）
- 功能卡片（我的空间、看兽医、去逛街）
- 热门动态预览
- 页脚

**验收标准**：
- 响应式设计（移动端适配）
- 点击功能卡片跳转正确
- 加载动画流畅

---

#### Task 3.2: 宠物空间重构 ✓ [2天]
**目标**：实现社交动态核心功能

**交付物**：
- [ ] `src/views/PetSpace/PetSpaceView.vue`
- [ ] `src/components/features/MomentCard.vue`
- [ ] `src/components/features/PublishModal.vue`

**功能点**：
- 发布动态（文字、图片）
- 动态列表（我的/关注的）
- 点赞、评论、分享
- 用户信息侧边栏

**技术要点**：
```javascript
// 发布动态
const publishMoment = async () => {
  const formData = new FormData()
  formData.append('content', content.value)
  images.forEach(img => formData.append('images', img))
  
  await momentService.create(formData)
}
```

**验收标准**：
- 发布动态成功
- 动态列表正确显示
- 图片预览和上传
- 无限滚动加载

---

#### Task 3.3: 看兽医页面 ✓ [1天]
**目标**：实现医疗服务功能

**交付物**：
- [ ] `src/views/Medical/MedicalView.vue`
- [ ] 在线咨询功能
- [ ] 预约挂号功能

**功能点**：
- 医生列表
- 预约表单
- 健康档案

**验收标准**：
- 表单验证正确
- 预约提交成功
- 数据持久化

---

#### Task 3.4: 宠物商城页面 ✓ [1天]
**目标**：实现商品浏览和购买

**交付物**：
- [ ] `src/views/Shopping/ShoppingView.vue`
- [ ] 商品列表
- [ ] 购物车功能

**功能点**：
- 商品筛选
- 加入购物车
- 订单管理

**验收标准**：
- 商品正确展示
- 购物车数量更新
- 结算流程顺畅

---

### ✨ Phase 4: 优化与完善（第3周）

#### Task 4.1: 性能优化 ✓ [1天]
**优化项**：
- [ ] 路由懒加载
- [ ] 图片懒加载
- [ ] 组件按需导入
- [ ] 代码分割（Vite）

**验收标准**：
- 首屏加载时间 < 2s
- Lighthouse 分数 > 90

---

#### Task 4.2: 用户体验优化 ✓ [1天]
**优化项**：
- [ ] Loading 状态
- [ ] 空状态提示
- [ ] 错误边界
- [ ] 骨架屏
- [ ] 动画过渡

**验收标准**：
- 所有操作有反馈
- 错误提示友好
- 动画流畅自然

---

#### Task 4.3: 响应式设计完善 ✓ [1天]
**优化项**：
- [ ] 移动端适配（<768px）
- [ ] 平板适配（768-1024px）
- [ ] 触摸手势支持

**验收标准**：
- 三种屏幕尺寸显示正常
- 移动端菜单可用

---

#### Task 4.4: 测试与部署 ✓ [1天]
**任务**：
- [ ] 单元测试（核心 composables）
- [ ] E2E 测试（关键流程）
- [ ] 构建配置优化
- [ ] 部署文档

**验收标准**：
- 核心功能有测试覆盖
- 生产构建成功
- 部署文档完整

---

## 📊 进度追踪表

| 阶段 | 任务 | 预计时间 | 状态 | 完成日期 |
|------|------|----------|------|----------|
| Phase 1 | 设计系统 | 2h | ⏳ 待开始 | - |
| Phase 1 | 基础组件 | 1d | ⏳ 待开始 | - |
| Phase 1 | 项目结构 | 1d | ⏳ 待开始 | - |
| Phase 2 | API 层 | 3h | ⏳ 待开始 | - |
| Phase 2 | Composables | 4h | ⏳ 待开始 | - |
| Phase 2 | Pinia Store | 3h | ⏳ 待开始 | - |
| Phase 3 | 首页 | 1d | ⏳ 待开始 | - |
| Phase 3 | 宠物空间 | 2d | ⏳ 待开始 | - |
| Phase 3 | 看兽医 | 1d | ⏳ 待开始 | - |
| Phase 3 | 商城 | 1d | ⏳ 待开始 | - |
| Phase 4 | 性能优化 | 1d | ⏳ 待开始 | - |
| Phase 4 | UX 优化 | 1d | ⏳ 待开始 | - |
| Phase 4 | 响应式 | 1d | ⏳ 待开始 | - |
| Phase 4 | 测试部署 | 1d | ⏳ 待开始 | - |

---

## 🚀 开始行动

### 立即开始 Task 1.1

准备好了吗？回复 "开始 Task 1.1" 我将提供：
1. ✅ 完整的 `design-tokens.css` 文件
2. ✅ 配置说明
3. ✅ 验收检查清单

---

## 📝 注意事项

1. **每完成一个 Task**：
   - 提交 Git（`git commit -m "feat: 完成 Task X.X"`）
   - 测试功能是否正常
   - 更新进度表

2. **遇到问题**：
   - 先查看文档
   - 检查 Console 错误
   - 询问我具体问题

3. **代码规范**：
   - 使用 ESLint 检查
   - 运行 `npm run lint` 修复
   - 保持命名一致

4. **保持节奏**：
   - 不要跳跃式开发
   - 完成一个再进入下一个
   - 每个阶段结束做总结


🎨 笑猫の窝 - 现代化前端设计大纲
基于您的需求和 Rome2Rio 的设计风格，我为您设计了一个半扁平化、现代简洁的前端方案。

📋 设计理念
核心特点：

半扁平化设计：保留轻微阴影和深度感，不完全扁平
简洁现代：大量留白，清晰的视觉层次
功能导向：像 Rome2Rio 一样，首页突出核心功能入口
渐变点缀：使用柔和的渐变色增加现代感
卡片式布局：功能模块化，易于扩展


🎯 前端开发顺序（基于后端功能）
Phase 1: 基础框架搭建 ⭐ [优先实现]
1.1 设计系统建立

设计变量（颜色、字体、间距）
CSS Reset 和全局样式
半扁平化阴影系统

1.2 基础组件库

BaseButton（主要/次要/文本按钮）
BaseCard（带轻微阴影的卡片）
BaseAvatar（圆形头像）
BaseInput（表单输入）

1.3 布局框架

AppHeader（顶部导航）
AppFooter（页脚）
AppLayout（主布局容器）


Phase 2: 首页实现 ⭐ [本次重点]
2.1 Hero Section（英雄区）

标题 + Slogan
搜索框（未来扩展：搜索宠物/服务）
CTA 按钮（立即体验）

2.2 功能导航卡片
基于后端已有功能：

我的空间 - 管理宠物和动态
看兽医 - 医疗服务（占位）
去逛街 - 宠物商城（占位）

2.3 介绍区域

平台介绍
核心特性展示（卡片式）
统计数据（用户数、宠物数、动态数）

2.4 动态预览

热门动态展示（基于后端 PetMoment）
精选宠物展示（基于后端 Pet）


Phase 3: 宠物空间核心功能
3.1 宠物管理

宠物列表（基于 GET /pets）
宠物切换器
宠物资料卡片

3.2 动态系统

动态列表（基于 GET /pets/{id}/moments）
发布动态（基于 POST /pets/{id}/moments）
动态卡片组件


Phase 4: 扩展功能（占位页）

看兽医页面
宠物商城页面
个人中心页面


🚀 立即开始：Phase 1 + Phase 2
我将为您创建：
✅ 交付物清单

设计系统文件

design-tokens.css - 现代配色方案
reset.css - CSS 重置
utilities.css - 工具类


基础组件

BaseButton.vue
BaseCard.vue
BaseAvatar.vue


布局组件

AppHeader.vue - 顶部导航
AppFooter.vue - 页脚
AppLayout.vue - 主布局


首页完整实现

HomeView.vue - 首页主文件
HeroSection.vue - 英雄区
FeatureCards.vue - 功能卡片
IntroSection.vue - 介绍区
MomentPreview.vue - 动态预览




🎨 设计预览（文字描述）
首页布局结构
┌─────────────────────────────────────┐
│  Header [Logo] [导航] [登录按钮]     │
├─────────────────────────────────────┤
│                                     │
│  Hero Section                       │
│  [大标题] "让宠物生活更美好"          │
│  [副标题] 一站式宠物健康管理平台      │
│  [搜索框] 🔍                         │
│  [CTA按钮] 立即体验                  │
│                                     │
├─────────────────────────────────────┤
│  Feature Cards (3个卡片横排)         │
│  ┌────────┐ ┌────────┐ ┌────────┐  │
│  │我的空间│ │看兽医  │ │去逛街  │  │
│  │  🏠   │ │  🏥   │ │  🛍️  │  │
│  └────────┘ └────────┘ └────────┘  │
├─────────────────────────────────────┤
│  Intro Section                      │
│  [平台介绍文字]                      │
│  [核心特性 - 3个卡片]                │
├─────────────────────────────────────┤
│  Moment Preview                     │
│  [热门动态展示 - 网格布局]           │
├─────────────────────────────────────┤
│  Footer [关于我们] [联系方式]        │
└─────────────────────────────────────┘