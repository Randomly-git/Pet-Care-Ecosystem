/**
 * 用户认证状态管理
 */

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'
import { setToken, getToken, removeToken, setUserInfo, getUserInfo, removeUserInfo } from '@/utils/storage'
import { showSuccess, showError, showInfo } from '@/utils/message'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const token = ref(getToken())
  const user = ref(getUserInfo())
  const loading = ref(false)
  const loginTime = ref(null)

  // 计算属性
  const isAuthenticated = computed(() => {
    return !!(token.value && user.value)
  })

  const userName = computed(() => {
    return user.value?.name || ''
  })

  // 扩展 avatar 计算属性以支持全局获取
  const avatar = computed(() => {
    return user.value?.avatar || ''
  })

  const userId = computed(() => {
    return user.value?.userId || null
  })

  const isTokenExpired = computed(() => {
    if (!loginTime.value) return true
    const now = new Date().getTime()
    const tokenAge = now - loginTime.value
    const tokenLifetime = 24 * 60 * 60 * 1000 // 24小时
    return tokenAge > tokenLifetime
  })

  /**
   * 用户登录
   */
  const login = async (loginData) => {
    loading.value = true

    try {
      const response = await authApi.login(loginData)

      if (!response.success) {
        throw new Error(response.message || '登录失败')
      }

      const { token: newToken, userId, name, nickname } = response.data

      // 保存token和用户信息（昵称用于社区等展示）
      token.value = newToken
      user.value = { userId, id: userId, name, nickname: nickname || name, username: name }
      loginTime.value = new Date().getTime()

      // 持久化存储
      setToken(newToken)
      setUserInfo(user.value)

      showSuccess(`欢迎回来，${name}！`)

      return response.data
    } catch (error) {
      console.error('登录失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 用户注册
   */
  const register = async (registerData) => {
    loading.value = true

    try {
      const response = await authApi.register(registerData)

      if (!response.success) {
        throw new Error(response.message || '注册失败')
      }

      showSuccess('注册成功！请登录您的账号')
      return response.data
    } catch (error) {
      console.error('注册失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  /**
   * 用户登出
   */
  const logout = async () => {
    loading.value = true

    try {
      // 调用后端登出接口（可选）
      try {
        await authApi.logout()
      } catch (error) {
        console.warn('后端登出请求失败:', error)
      }

      // 清理本地状态
      token.value = null
      user.value = null
      loginTime.value = null

      // 清理持久化存储
      removeToken()
      removeUserInfo()

      showSuccess('已成功退出登录')

      return true
    } catch (error) {
      console.error('登出失败:', error)
      // 即使后端登出失败，也清理本地状态
      token.value = null
      user.value = null
      loginTime.value = null
      removeToken()
      removeUserInfo()

      showInfo('已退出登录')
      return false
    } finally {
      loading.value = false
    }
  }

  /**
   * 刷新token
   */
  const refreshToken = async () => {
    if (!token.value || isTokenExpired.value) {
      return false
    }

    try {
      const response = await authApi.refreshToken()

      if (!response.success) {
        throw new Error(response.message || 'Token刷新失败')
      }

      const newToken = response.token

      // 更新token
      token.value = newToken
      loginTime.value = new Date().getTime()
      setToken(newToken)

      return true
    } catch (error) {
      console.error('Token刷新失败:', error)
      // 刷新失败，自动登出
      await logout()
      return false
    }
  }

  /**
   * 验证token有效性
   */
  const validateToken = async () => {
    if (!token.value) return false
    if (isTokenExpired.value) {
      await logout()
      return false
    }

    try {
      const isValid = await authApi.validateToken(token.value)
      return isValid
    } catch (error) {
      console.error('Token验证失败:', error)
      await logout()
      return false
    }
  }

  /**
   * 更新用户信息
   */
  const updateUser = (newUserData) => {
    if (newUserData && user.value) {
      user.value = { ...user.value, ...newUserData }
      setUserInfo(user.value)
    }
  }

  /**
   * 检查并维护认证状态
   */
  const checkAuthStatus = async () => {
    if (!token.value) return false

    // 检查token是否过期
    if (isTokenExpired.value) {
      await logout()
      return false
    }

    // 尝试验证token
    return await validateToken()
  }

  /**
   * 获取认证头
   */
  const getAuthHeader = () => {
    return token.value ? `Bearer ${token.value}` : null
  }

  /**
   * 初始化认证状态
   */
  const initAuth = async () => {
    const storedToken = getToken()
    const storedUser = getUserInfo()

    if (storedToken && storedUser) {
      token.value = storedToken
      user.value = storedUser
      loginTime.value = new Date().getTime()

      // 验证token是否仍然有效
      const isValid = await checkAuthStatus()
      return isValid
    }

    return false
  }

  /**
   * 强制刷新用户信息
   */
  const forceRefreshUser = () => {
    if (!token.value) return false

    try {
      const currentUser = authApi.getCurrentUser()
      if (currentUser) {
        user.value = currentUser
        setUserInfo(currentUser)
        return true
      }
      return false
    } catch (error) {
      console.error('刷新用户信息失败:', error)
      return false
    }
  }

  return {
    // 状态
    token,
    user,
    loading,
    loginTime,

    // 计算属性
    isAuthenticated,
    userName,
    avatar,
    userId,
    isTokenExpired,

    // 方法
    login,
    register,
    logout,
    refreshToken,
    validateToken,
    updateUser,
    checkAuthStatus,
    getAuthHeader,
    initAuth,
    forceRefreshUser
  }
})