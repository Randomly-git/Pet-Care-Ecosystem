import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

// 引入全局样式
import './assets/styles/reset.css'
import './assets/styles/design-tokens.css'
import './assets/styles/utilities.css'

// 导入API服务（暂时使用原有的api.js避免错误）
import apiService, { userAPI, petAPI, activityAPI } from '@/services/api.js'

// 创建简单的认证服务（基于原有API）
const authService = {
  login: async (credentials) => {
    const result = await userAPI.login(credentials)
    if (result.data && result.data.token) {
      localStorage.setItem('token', result.data.token)
      localStorage.setItem('userInfo', JSON.stringify(result.data))
    }
    return result.data
  },
  logout: async () => {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    return userAPI.logout()
  },
  isAuthenticated: () => !!localStorage.getItem('token'),
  getCurrentUser: () => {
    const userInfo = localStorage.getItem('userInfo')
    return userInfo ? JSON.parse(userInfo) : null
  }
}

// 简单的配置服务
const configService = {
  setLogLevel: (level) => {
    console.log(`Log level set to: ${level}`)
  }
}

// 创建Vue应用实例
const app = createApp(App)

// 创建Pinia实例
const pinia = createPinia()

// 注册Element Plus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 安装插件
app.use(pinia)
app.use(router)
app.use(ElementPlus)

// 全局属性
app.config.globalProperties.$api = apiService

// 初始化检查
async function initializeApp() {
  console.log('🚀 初始化应用...')

  // 检查后端服务状态
  try {
    // 设置日志级别
    if (import.meta.env.DEV) {
      configService.setLogLevel('debug')
      console.log('📝 开发模式：已启用调试日志')
    }

    // 检查用户认证状态
    const isAuth = authService.isAuthenticated()
    console.log('🔐 用户认证状态:', isAuth ? '已登录' : '未登录')

    if (isAuth) {
      const currentUser = authService.getCurrentUser()
      console.log('👤 当前用户:', currentUser?.name)
    }

    console.log('✅ 应用初始化完成')

  } catch (error) {
    console.error('❌ 应用初始化失败:', error)
    // 继续运行应用，但标记初始化失败
    app.config.globalProperties.$initError = error
  }
}

// 全局错误处理
app.config.errorHandler = (error, instance, info) => {
  console.error('全局错误:', error)
  console.error('错误信息:', info)

  // 在开发环境中显示详细错误
  if (import.meta.env.DEV) {
    console.error('错误组件:', instance)
  }
}

// 全局警告处理
app.config.warnHandler = (msg, instance, trace) => {
  if (import.meta.env.DEV) {
    console.warn('警告:', msg)
    console.warn('追踪:', trace)
  }
}

// 性能监控（仅开发环境）
if (import.meta.env.DEV) {
  app.config.performance = true

  // 监控路由切换
  router.afterEach((to, from) => {
    console.log(`📍 路由切换: ${from.path} → ${to.path}`)
  })
}

// 挂载应用
app.mount('#app')

// 初始化应用
initializeApp()

// 导出应用实例供调试使用
if (import.meta.env.DEV) {
  window.__app__ = app

  // 提供调试工具
  window.__debug__ = {
    api: apiService,
    auth: authService,
    config: configService,
    router: router,
    userAPI: userAPI,
    petAPI: petAPI,
    activityAPI: activityAPI
  }

  console.log('🔧 调试工具已加载')
  console.log('使用 window.__debug__ 访问调试工具')
}