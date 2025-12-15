import axios from 'axios'
import { showToast } from '@/utils/message'

// API基础配置（通过前端代理）
const API_BASE_URL = '/api'  // 使用前端代理，不再直接指定端口

// 创建axios实例
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  }
})

// 请求拦截器
apiClient.interceptors.request.use(
  (config) => {
    // 获取token
    const token = localStorage.getItem('authToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // 处理FormData - 移除Content-Type让浏览器自动设置
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
    }

    // 添加请求时间戳
    config.metadata = { startTime: new Date() }

    console.log(`🚀 API请求 [${config.method?.toUpperCase()}] ${config.url}`, {
      params: config.params,
      data: config.data
    })

    return config
  },
  (error) => {
    console.error('❌ 请求拦截器错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
apiClient.interceptors.response.use(
  (response) => {
    const { config } = response
    const duration = new Date() - config.metadata.startTime

    console.log(`✅ API响应 [${config.method?.toUpperCase()}] ${config.url} (${duration}ms)`, {
      status: response.status,
      data: response.data
    })

    // 统一处理API响应格式
    if (response.data && typeof response.data === 'object') {
      // 处理统一的ApiResponse格式
      if ('success' in response.data) {
        return response.data
      }
      // 处理直接返回的数据格式
      return response.data
    }

    return response.data
  },
  (error) => {
    const { config, response } = error
    const duration = config?.metadata ? new Date() - config.metadata.startTime : 0

    console.error(`❌ API错误 [${config?.method?.toUpperCase()}] ${config?.url} (${duration}ms)`, {
      status: response?.status,
      message: error.message,
      data: response?.data
    })

    // 统一错误处理
    let errorMessage = '网络请求失败，请稍后重试'

    if (response) {
      switch (response.status) {
        case 400:
          errorMessage = response.data?.message || '请求参数错误'
          break
        case 401:
          errorMessage = '登录已过期，请重新登录'
          // 清除本地token
          localStorage.removeItem('authToken')
          // 可以在这里跳转到登录页
          break
        case 403:
          errorMessage = '权限不足，无法访问'
          break
        case 404:
          errorMessage = '请求的资源不存在'
          break
        case 409:
          errorMessage = response.data?.message || '数据冲突'
          break
        case 500:
          errorMessage = '服务器内部错误'
          break
        default:
          errorMessage = response.data?.message || `请求失败 (${response.status})`
      }
    } else if (error.code === 'ECONNABORTED') {
      errorMessage = '请求超时，请检查网络连接'
    } else if (error.code === 'NETWORK_ERROR') {
      errorMessage = '网络连接失败，请检查网络'
    }

    // 显示错误提示
    showToast(errorMessage, 'error')

    return Promise.reject({
      message: errorMessage,
      status: response?.status,
      data: response?.data
    })
  }
)

export default apiClient

// 导出基础URL
export { API_BASE_URL }