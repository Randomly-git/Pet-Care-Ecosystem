/**
 * HTTP请求工具类
 * 基于axios封装的更高级的请求方法
 */

import axios from 'axios'
import { showToast } from './message'
import { API_CONFIG, APP_CONFIG, DEBUG_CONFIG } from '@/config'
import { handleApiError, formatDateForApi, createApiParams } from './api-helper'

class Request {
  constructor() {
    this.instance = this.createInstance()
  }

  /**
   * 创建axios实例
   */
  createInstance() {
    const instance = axios.create({
      baseURL: API_CONFIG.BASE_URL,
      timeout: API_CONFIG.TIMEOUT,
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      }
    })

    // 请求拦截器
    instance.interceptors.request.use(
      (config) => {
        // 添加认证token
        const token = localStorage.getItem(APP_CONFIG.TOKEN_KEY)
        if (token) {
          config.headers.Authorization = `Bearer ${token}`
        }

        // 添加请求元数据
        config.metadata = {
          startTime: new Date(),
          retryCount: 0
        }

        // 调试模式下打印请求信息
        if (DEBUG_CONFIG.SHOW_API_LOG) {
          console.group(`🚀 API请求 [${config.method?.toUpperCase()}] ${config.url}`)
          console.log('Headers:', config.headers)
          console.log('Params:', config.params)
          console.log('Data:', config.data)
          console.groupEnd()
        }

        return config
      },
      (error) => {
        console.error('❌ 请求拦截器错误:', error)
        return Promise.reject(error)
      }
    )

    // 响应拦截器
    instance.interceptors.response.use(
      (response) => {
        const { config } = response
        const duration = new Date() - config.metadata.startTime

        // 调试模式下打印响应信息
        if (DEBUG_CONFIG.SHOW_API_LOG) {
          console.group(`✅ API响应 [${config.method?.toUpperCase()}] ${config.url} (${duration}ms)`)
          console.log('Status:', response.status)
          console.log('Headers:', response.headers)
          console.log('Data:', response.data)
          console.groupEnd()
        }

        return response
      },
      async (error) => {
        const { config } = error
        const duration = config?.metadata ? new Date() - config.metadata.startTime : 0

        // 调试模式下打印错误信息
        if (DEBUG_CONFIG.SHOW_API_LOG) {
          console.group(`❌ API错误 [${config?.method?.toUpperCase()}] ${config?.url} (${duration}ms)`)
          console.log('Error:', error)
          if (error.response) {
            console.log('Status:', error.response.status)
            console.log('Data:', error.response.data)
          }
          console.groupEnd()
        }

        // 重试机制
        if (!config || !config.retry) {
          return this.handleRequestError(error)
        }

        // 增加重试次数
        config.metadata.retryCount = config.metadata.retryCount || 0

        // 如果重试次数达到上限，则返回错误
        if (config.metadata.retryCount >= API_CONFIG.RETRY_COUNT) {
          return this.handleRequestError(error)
        }

        // 增加重试计数
        config.metadata.retryCount += 1

        // 计算重试延迟（指数退避）
        const retryDelay = API_CONFIG.RETRY_DELAY * Math.pow(2, config.metadata.retryCount - 1)

        console.log(`🔄 重试请求 (${config.metadata.retryCount}/${API_CONFIG.RETRY_COUNT}) ${config.url}`)

        // 延迟后重试
        await new Promise(resolve => setTimeout(resolve, retryDelay))

        return this.instance(config)
      }
    )

    return instance
  }

  /**
   * 处理请求错误
   */
  handleRequestError(error) {
    const errorInfo = handleApiError(error)

    // 显示错误提示（静默模式不显示）
    if (!error.config?.silent) {
      showToast(errorInfo.message, 'error')
    }

    return Promise.reject(errorInfo)
  }

  /**
   * GET请求
   */
  get(url, params = {}, options = {}) {
    const config = {
      url,
      method: 'GET',
      params: createApiParams(params, options.paramsOptions),
      ...options
    }

    return this.instance.request(config)
  }

  /**
   * POST请求
   */
  post(url, data = {}, options = {}) {
    const config = {
      url,
      method: 'POST',
      data: createApiParams(data, options.dataOptions),
      ...options
    }

    return this.instance.request(config)
  }

  /**
   * PUT请求
   */
  put(url, data = {}, options = {}) {
    const config = {
      url,
      method: 'PUT',
      data: createApiParams(data, options.dataOptions),
      ...options
    }

    return this.instance.request(config)
  }

  /**
   * DELETE请求
   */
  delete(url, params = {}, options = {}) {
    const config = {
      url,
      method: 'DELETE',
      params: createApiParams(params, options.paramsOptions),
      ...options
    }

    return this.instance.request(config)
  }

  /**
   * PATCH请求
   */
  patch(url, data = {}, options = {}) {
    const config = {
      url,
      method: 'PATCH',
      data: createApiParams(data, options.dataOptions),
      ...options
    }

    return this.instance.request(config)
  }

  /**
   * 文件上传
   */
  upload(url, formData, options = {}) {
    const config = {
      url,
      method: 'POST',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data',
        ...options.headers
      },
      timeout: 60000, // 上传超时时间更长
      ...options
    }

    return this.instance.request(config)
  }

  /**
   * 下载文件
   */
  download(url, params = {}, filename = null, options = {}) {
    return this.get(url, params, {
      ...options,
      responseType: 'blob'
    }).then(response => {
      // 创建下载链接
      const blob = new Blob([response])
      const downloadUrl = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = downloadUrl

      // 设置文件名
      if (filename) {
        link.download = filename
      } else {
        // 尝试从响应头获取文件名
        const contentDisposition = response.headers['content-disposition']
        if (contentDisposition) {
          const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
          if (filenameMatch && filenameMatch[1]) {
            link.download = filenameMatch[1].replace(/['"]/g, '')
          }
        }
      }

      // 触发下载
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(downloadUrl)

      return response
    })
  }

  /**
   * 批量请求
   */
  async batch(requests) {
    try {
      const results = await Promise.allSettled(
        requests.map(request => {
          if (typeof request === 'function') {
            return request()
          }
          return request
        })
      )

      const successful = results.filter(result => result.status === 'fulfilled')
      const failed = results.filter(result => result.status === 'rejected')

      return {
        successful,
        failed,
        total: results.length,
        successCount: successful.length,
        failCount: failed.length
      }
    } catch (error) {
      console.error('批量请求失败:', error)
      throw error
    }
  }

  /**
   * 取消请求
   */
  cancel(message = '请求已取消') {
    if (this.source) {
      this.source.cancel(message)
    }
  }

  /**
   * 创建可取消的请求源
   */
  createCancelSource() {
    return axios.CancelToken.source()
  }
}

// 创建请求实例
const request = new Request()

// 导出常用的方法
export const get = (url, params, options) => request.get(url, params, options)
export const post = (url, data, options) => request.post(url, data, options)
export const put = (url, data, options) => request.put(url, data, options)
export const del = (url, params, options) => request.delete(url, params, options)
export const patch = (url, data, options) => request.patch(url, data, options)
export const upload = (url, formData, options) => request.upload(url, formData, options)
export const download = (url, params, filename, options) => request.download(url, params, filename, options)
export const batch = (requests) => request.batch(requests)

export default request