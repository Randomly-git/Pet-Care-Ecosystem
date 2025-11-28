/**
 * 用户认证API接口
 * 处理用户注册、登录等认证相关操作
 */

import apiClient from './index'
import { showSuccess, showError } from '@/utils/message'
import { setToken, removeToken, setUserInfo, removeUserInfo } from '@/utils/storage'

/**
 * 用户注册
 * @param {Object} registerData 注册数据
 * @param {string} registerData.name - 用户名
 * @param {string} registerData.password - 密码
 * @returns {Promise<Object>} 注册结果
 */
export const register = async (registerData) => {
  try {
    const response = await apiClient.post('/auth/register', registerData)

    // 使用统一的响应处理
    if (response.success) {
      showSuccess('注册成功！')
      return {
        success: true,
        data: response.data
      }
    } else {
      showError(response.message || '注册失败')
      return {
        success: false,
        message: response.message || '注册失败'
      }
    }
  } catch (error) {
    const errorMessage = error.message || '注册请求失败'
    showError(errorMessage)
    return {
      success: false,
      message: errorMessage
    }
  }
}

/**
 * 用户登录
 * @param {Object} loginData 登录数据
 * @param {string} loginData.name - 用户名
 * @param {string} loginData.password - 密码
 * @returns {Promise<Object>} 登录结果
 */
export const login = async (loginData) => {
  try {
    const response = await apiClient.post('/auth/login', loginData)

    if (response.success) {
      // 存储token和用户信息
      const { data } = response
      if (data.token) {
        setToken(data.token)
        setUserInfo({
          userId: data.userId,
          name: data.name,
          username: data.name
        })
      }

      showSuccess('登录成功！')
      return {
        success: true,
        data: data
      }
    } else {
      showError(response.message || '登录失败')
      return {
        success: false,
        message: response.message || '登录失败'
      }
    }
  } catch (error) {
    const errorMessage = error.message || '登录请求失败'
    showError(errorMessage)
    return {
      success: false,
      message: errorMessage
    }
  }
}

/**
 * 用户登出
 * @returns {Promise<boolean>} 登出结果
 */
export const logout = async () => {
  try {
    // 清除本地存储
    removeToken()
    removeUserInfo()

    showSuccess('已安全退出')
    return true
  } catch (error) {
    console.error('登出失败:', error)
    return false
  }
}

/**
 * 检查登录状态
 * @returns {boolean} 是否已登录
 */
export const isLoggedIn = () => {
  const token = localStorage.getItem('authToken')
  const userInfo = localStorage.getItem('userInfo')

  return !!(token && token !== '' && userInfo && userInfo !== '')
}

/**
 * 获取当前用户信息
 * @returns {Object|null} 用户信息
 */
export const getCurrentUser = () => {
  try {
    const userInfo = localStorage.getItem('userInfo')
    return userInfo ? JSON.parse(userInfo) : null
  } catch (error) {
    console.error('获取用户信息失败:', error)
    return null
  }
}

/**
 * 获取当前token
 * @returns {string|null} JWT token
 */
export const getCurrentToken = () => {
  return localStorage.getItem('authToken')
}

/**
 * 刷新token（如果需要的话）
 * @returns {Promise<Object>} 刷新结果
 */
export const refreshToken = async () => {
  try {
    const token = getCurrentToken()
    if (!token) {
      throw new Error('未找到有效的登录凭证')
    }

    // 这里可以添加刷新token的逻辑
    // 暂时返回当前token
    return {
      success: true,
      token: token
    }
  } catch (error) {
    console.error('刷新token失败:', error)
    // 如果刷新失败，清除本地存储
    removeToken()
    removeUserInfo()
    return {
      success: false,
      message: '登录已过期，请重新登录'
    }
  }
}

/**
 * 验证token是否有效
 * @param {string} token JWT token
 * @returns {Promise<boolean>} 验证结果
 */
export const validateToken = async (token = null) => {
  try {
    const currentToken = token || getCurrentToken()
    if (!currentToken) {
      return false
    }

    // 这里可以添加token验证逻辑
    // 比如调用后端验证接口
    return true
  } catch (error) {
    console.error('验证token失败:', error)
    return false
  }
}

/**
 * 用户状态检查
 * @returns {Promise<Object>} 状态检查结果
 */
export const checkAuthStatus = async () => {
  try {
    const token = getCurrentToken()

    if (!token) {
      return {
        authenticated: false,
        message: '未登录'
      }
    }

    const isValid = await validateToken(token)

    if (!isValid) {
      // token无效，清除本地存储
      removeToken()
      removeUserInfo()
      return {
        authenticated: false,
        message: '登录已过期，请重新登录'
      }
    }

    return {
      authenticated: true,
      message: '登录状态正常'
    }
  } catch (error) {
    console.error('检查认证状态失败:', error)
    return {
      authenticated: false,
      message: '检查登录状态失败'
    }
  }
}

/**
 * 重置密码
 * @param {Object} resetData 重置密码数据
 * @returns {Promise<Object>} 重置结果
 */
export const resetPassword = async (resetData) => {
  try {
    const response = await apiClient.post('/auth/reset-password', resetData)

    if (response.success) {
      showSuccess('密码重置邮件已发送')
      return {
        success: true,
        data: response.data
      }
    } else {
      showError(response.message || '密码重置失败')
      return {
        success: false,
        message: response.message || '密码重置失败'
      }
    }
  } catch (error) {
    const errorMessage = error.message || '密码重置请求失败'
    showError(errorMessage)
    return {
      success: false,
      message: errorMessage
    }
  }
}

export default {
  register,
  login,
  logout,
  isLoggedIn,
  getCurrentUser,
  getCurrentToken,
  refreshToken,
  validateToken,
  checkAuthStatus,
  resetPassword
}