// 文件位置: src/stores/app.js
// 全局应用状态管理 - Pinia store

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAppStore = defineStore('app', () => {
  // ===== 状态 =====
  const theme = ref('light') // light | dark
  const language = ref('zh-CN') // zh-CN | en-US
  const sidebarOpen = ref(false)
  const notification = ref(null)
  const globalLoading = ref(false)
  const networkStatus = ref(navigator.onLine)

  // ===== 计算属性 =====
  const isDarkMode = computed(() => theme.value === 'dark')
  const isOffline = computed(() => !networkStatus.value)

  // ===== 操作方法 =====
  const setTheme = (newTheme) => {
    theme.value = newTheme
    // 更新DOM
    document.documentElement.setAttribute('data-theme', newTheme)
    localStorage.setItem('theme', newTheme)
  }

  const toggleTheme = () => {
    const newTheme = theme.value === 'light' ? 'dark' : 'light'
    setTheme(newTheme)
  }

  const setLanguage = (lang) => {
    language.value = lang
    localStorage.setItem('language', lang)
  }

  const toggleSidebar = () => {
    sidebarOpen.value = !sidebarOpen.value
  }

  const closeSidebar = () => {
    sidebarOpen.value = false
  }

  const showNotification = (message, type = 'info', duration = 3000) => {
    notification.value = {
      id: Date.now(),
      message,
      type, // success | error | warning | info
      duration
    }

    // 自动隐藏
    if (duration > 0) {
      setTimeout(() => {
        hideNotification()
      }, duration)
    }

    return notification.value.id
  }

  const hideNotification = () => {
    notification.value = null
  }

  const setGlobalLoading = (loading) => {
    globalLoading.value = loading
  }

  const updateNetworkStatus = () => {
    networkStatus.value = navigator.onLine
  }

  // ===== 初始化 =====
  const initApp = () => {
    // 恢复主题设置
    const savedTheme = localStorage.getItem('theme') || 'light'
    setTheme(savedTheme)

    // 恢复语言设置
    const savedLanguage = localStorage.getItem('language') || 'zh-CN'
    setLanguage(savedLanguage)

    // 监听网络状态变化
    window.addEventListener('online', updateNetworkStatus)
    window.addEventListener('offline', updateNetworkStatus)

    console.log('应用初始化完成')
  }

  // ===== 清理 =====
  const cleanup = () => {
    window.removeEventListener('online', updateNetworkStatus)
    window.removeEventListener('offline', updateNetworkStatus)
  }

  return {
    // 状态
    theme,
    language,
    sidebarOpen,
    notification,
    globalLoading,
    networkStatus,

    // 计算属性
    isDarkMode,
    isOffline,

    // 方法
    setTheme,
    toggleTheme,
    setLanguage,
    toggleSidebar,
    closeSidebar,
    showNotification,
    hideNotification,
    setGlobalLoading,
    updateNetworkStatus,
    initApp,
    cleanup
  }
})