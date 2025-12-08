/**
 * 统一的HTTP客户端 - 基于适配器模式设计
 * 解决前后端通信的痛点：错误处理、重试、缓存、调试等
 */

import { ElMessage, ElNotification } from 'element-plus'

// 类型定义
export interface RequestConfig {
  baseURL?: string
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  headers?: Record<string, string>
  params?: Record<string, any>
  data?: any
  timeout?: number
  retry?: number
  retryDelay?: number
  cache?: boolean
  cacheTime?: number
  silent?: boolean
  responseType?: 'json' | 'text' | 'blob' | 'formData'
  signal?: AbortSignal
}

export interface ResponseData<T = any> {
  success: boolean
  message: string
  data: T
  timestamp: string
  code?: number
}

export interface RequestError {
  message: string
  code?: string | number
  status?: number
  config?: RequestConfig
  response?: Response
}

// 缓存接口
interface CacheItem<T> {
  data: T
  timestamp: number
  expire: number
}

// 微服务端点配置
const MICROSERVICE_CONFIG = {
  GATEWAY: import.meta.env.VITE_API_GATEWAY || 'http://localhost:8082',
  PETCARE: import.meta.env.VITE_PETCARE_SERVICE || 'http://localhost:8082',
  MEDIA: import.meta.env.VITE_MEDIA_SERVICE || 'http://localhost:8081',
  COMMUNITY: import.meta.env.VITE_COMMUNITY_SERVICE || 'http://localhost:8083',
  timeout: 30000,
  retryCount: 3,
  retryDelay: 1000
}

// 日志级别
type LogLevel = 'debug' | 'info' | 'warn' | 'error' | 'silent'
type LogLevelNum = 0 | 1 | 2 | 3 | 4 | 5

const LOG_LEVELS: Record<LogLevel, LogLevelNum> = {
  debug: 0,
  info: 1,
  warn: 2,
  error: 3,
  silent: 5  // 用于静默模式
}

class Logger {
  private level: LogLevelNum = LOG_LEVELS.info
  private prefix = '[HttpClient]'

  setLevel(level: LogLevel) {
    this.level = LOG_LEVELS[level]
  }

  private shouldLog(level: LogLevelNum): boolean {
    return level >= this.level
  }

  debug(...args: any[]) {
    if (this.shouldLog(LOG_LEVELS.debug)) {
      console.debug(this.prefix, '[DEBUG]', ...args)
    }
  }

  info(...args: any[]) {
    if (this.shouldLog(LOG_LEVELS.info)) {
      console.info(this.prefix, '[INFO]', ...args)
    }
  }

  warn(...args: any[]) {
    if (this.shouldLog(LOG_LEVELS.warn)) {
      console.warn(this.prefix, '[WARN]', ...args)
    }
  }

  error(...args: any[]) {
    if (this.shouldLog(LOG_LEVELS.error)) {
      console.error(this.prefix, '[ERROR]', ...args)
    }
  }

  group(title: string, fn: () => void) {
    console.group(`${this.prefix} ${title}`)
    fn()
    console.groupEnd()
  }
}

// 统一的HTTP客户端类
class HttpClient {
  private logger = new Logger()
  private cache = new Map<string, CacheItem<any>>()
  private pendingRequests = new Map<string, Promise<any>>()

  // 设置日志级别
  setLogLevel(level: LogLevel) {
    this.logger.setLevel(level)
  }

  // 获取认证头
  private getAuthHeaders(): Record<string, string> {
    const token = localStorage.getItem('token')
    return token ? { Authorization: `Bearer ${token}` } : {}
  }

  // 生成缓存键
  private getCacheKey(config: RequestConfig): string {
    const { url, method = 'GET', params, data } = config
    return `${method}:${url}:${JSON.stringify(params)}:${JSON.stringify(data)}`
  }

  // 检查缓存
  private checkCache<T>(config: RequestConfig): T | null {
    if (!config.cache || config.method !== 'GET') {
      return null
    }

    const key = this.getCacheKey(config)
    const item = this.cache.get(key)

    if (!item) {
      return null
    }

    // 检查是否过期
    if (Date.now() > item.expire) {
      this.cache.delete(key)
      return null
    }

    this.logger.debug('Cache hit:', key)
    return item.data
  }

  // 设置缓存
  private setCache<T>(config: RequestConfig, data: T): void {
    if (!config.cache || config.method !== 'GET') {
      return
    }

    const key = this.getCacheKey(config)
    const cacheTime = config.cacheTime || 5 * 60 * 1000 // 默认5分钟

    this.cache.set(key, {
      data,
      timestamp: Date.now(),
      expire: Date.now() + cacheTime
    })
  }

  // 格式化URL
  private formatURL(config: RequestConfig): string {
    const baseURL = config.baseURL || MICROSERVICE_CONFIG.GATEWAY
    const url = config.url.startsWith('http') ? config.url : `${baseURL}${config.url}`

    if (config.params && Object.keys(config.params).length > 0) {
      const searchParams = new URLSearchParams()
      Object.entries(config.params).forEach(([key, value]) => {
        if (value !== null && value !== undefined) {
          searchParams.append(key, String(value))
        }
      })
      return `${url}?${searchParams.toString()}`
    }

    return url
  }

  // 创建请求配置
  private createFetchConfig(config: RequestConfig): RequestInit {
    const headers = {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      ...this.getAuthHeaders(),
      ...config.headers
    }

    const fetchConfig: RequestInit = {
      method: config.method || 'GET',
      headers,
      signal: config.signal
    }

    if (config.data && ['POST', 'PUT', 'PATCH'].includes(config.method || 'GET')) {
      if (headers['Content-Type'] === 'application/json') {
        fetchConfig.body = JSON.stringify(config.data)
      } else {
        fetchConfig.body = config.data
      }
    }

    return fetchConfig
  }

  // 处理响应
  private async handleResponse<T>(response: Response, config: RequestConfig): Promise<ResponseData<T>> {
    const contentType = response.headers.get('content-type') || ''

    let data: any
    if (contentType.includes('application/json')) {
      data = await response.json()
    } else if (contentType.includes('text/')) {
      data = await response.text()
    } else if (config.responseType === 'blob') {
      data = await response.blob()
    } else {
      data = await response.text()
    }

    // 如果响应不是标准格式，包装成标准格式
    if (typeof data === 'string' || data instanceof Blob) {
      return {
        success: response.ok,
        message: response.ok ? 'Success' : 'Error',
        data: data as T,
        timestamp: new Date().toISOString()
      }
    }

    return data
  }

  // 处理错误
  private handleError(error: any, config: RequestConfig): RequestError {
    const errorInfo: RequestError = {
      message: error.message || '请求失败',
      config
    }

    if (error.response) {
      errorInfo.status = error.response.status
      errorInfo.response = error.response
    } else if (error.status) {
      errorInfo.status = error.status
    }

    // 根据状态码设置错误信息
    if (errorInfo.status) {
      switch (errorInfo.status) {
        case 400:
          errorInfo.message = '请求参数错误'
          break
        case 401:
          errorInfo.message = '未授权，请重新登录'
          this.handleUnauthorized()
          break
        case 403:
          errorInfo.message = '拒绝访问'
          break
        case 404:
          errorInfo.message = '请求资源不存在'
          break
        case 408:
          errorInfo.message = '请求超时'
          break
        case 500:
          errorInfo.message = '服务器内部错误'
          break
        case 502:
          errorInfo.message = '网关错误'
          break
        case 503:
          errorInfo.message = '服务不可用'
          break
        case 504:
          errorInfo.message = '网关超时'
          break
        default:
          errorInfo.message = `请求失败 (${errorInfo.status})`
      }
    }

    return errorInfo
  }

  // 处理未授权
  private handleUnauthorized(): void {
    // 清除token
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')

    // 显示提示
    ElNotification({
      title: '提示',
      message: '登录已过期，请重新登录',
      type: 'warning',
      duration: 3000
    })

    // 跳转到登录页
    if (window.location.pathname !== '/login') {
      window.location.href = '/login'
    }
  }

  // 显示错误信息
  private showError(error: RequestError): void {
    if (error.config?.silent) {
      return
    }

    const message = error.message || '发生未知错误'

    // 根据错误类型选择显示方式
    if (error.status && error.status >= 500) {
      ElNotification({
        title: '服务器错误',
        message,
        type: 'error',
        duration: 5000
      })
    } else {
      ElMessage({
        message,
        type: 'error',
        duration: 3000
      })
    }
  }

  // 延迟函数
  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms))
  }

  // 执行请求（带重试）
  private async executeRequest<T>(config: RequestConfig): Promise<ResponseData<T>> {
    const maxRetries = config.retry || MICROSERVICE_CONFIG.retryCount
    const retryDelay = config.retryDelay || MICROSERVICE_CONFIG.retryDelay
    let lastError: RequestError | null = null

    for (let attempt = 0; attempt <= maxRetries; attempt++) {
      try {
        // 检查缓存（仅GET请求）
        if (attempt === 0) {
          const cached = this.checkCache<T>(config)
          if (cached !== null) {
            return cached
          }
        }

        // 格式化URL和配置
        const url = this.formatURL(config)
        const fetchConfig = this.createFetchConfig(config)

        // 设置超时
        const timeout = config.timeout || MICROSERVICE_CONFIG.timeout
        const controller = new AbortController()
        const timeoutId = setTimeout(() => controller.abort(), timeout)

        // 合并signal
        if (fetchConfig.signal) {
          fetchConfig.signal = this.combineSignals(controller.signal, fetchConfig.signal)
        } else {
          fetchConfig.signal = controller.signal
        }

        // 记录请求日志
        if (attempt === 0) {
          this.logger.group(`🚀 [${config.method}] ${url}`, () => {
            this.logger.debug('Headers:', fetchConfig.headers)
            this.logger.debug('Data:', config.data)
            this.logger.debug('Params:', config.params)
          })
        }

        // 执行请求
        const response = await fetch(url, fetchConfig)
        clearTimeout(timeoutId)

        // 处理响应
        const result = await this.handleResponse<T>(response, config)

        // 记录成功日志
        this.logger.info(`✅ [${config.method}] ${url} (${response.status})`)

        // 缓存结果（仅GET请求）
        if (config.method === 'GET' || !config.method) {
          this.setCache(config, result)
        }

        return result

      } catch (error: any) {
        lastError = this.handleError(error, config)

        // 如果是最后一次尝试，抛出错误
        if (attempt === maxRetries) {
          break
        }

        // 某些错误不应该重试
        if (lastError.status && [400, 401, 403, 404].includes(lastError.status)) {
          break
        }

        // 计算延迟时间（指数退避）
        const delayTime = retryDelay * Math.pow(2, attempt)
        this.logger.warn(`🔄 重试 ${attempt + 1}/${maxRetries} ${config.url} (${delayTime}ms)`)

        await this.delay(delayTime)
      }
    }

    // 显示错误信息
    if (lastError) {
      this.showError(lastError)
    }

    throw lastError || new Error('请求失败')
  }

  // 合并信号
  private combineSignals(signal1: AbortSignal, signal2: AbortSignal): AbortSignal {
    const controller = new AbortController()

    signal1.addEventListener('abort', () => controller.abort())
    signal2.addEventListener('abort', () => controller.abort())

    return controller.signal
  }

  // 核心请求方法
  async request<T = any>(config: RequestConfig): Promise<ResponseData<T>> {
    // 防止重复请求
    const key = this.getCacheKey(config)
    if (this.pendingRequests.has(key)) {
      return this.pendingRequests.get(key)
    }

    const promise = this.executeRequest<T>(config)
    this.pendingRequests.set(key, promise)

    try {
      const result = await promise
      return result
    } finally {
      this.pendingRequests.delete(key)
    }
  }

  // GET请求
  get<T = any>(url: string, params?: Record<string, any>, options?: Partial<RequestConfig>): Promise<ResponseData<T>> {
    return this.request<T>({
      ...options,
      url,
      method: 'GET',
      params,
      cache: options?.cache !== false // 默认启用缓存
    })
  }

  // POST请求
  post<T = any>(url: string, data?: any, options?: Partial<RequestConfig>): Promise<ResponseData<T>> {
    return this.request<T>({
      ...options,
      url,
      method: 'POST',
      data
    })
  }

  // PUT请求
  put<T = any>(url: string, data?: any, options?: Partial<RequestConfig>): Promise<ResponseData<T>> {
    return this.request<T>({
      ...options,
      url,
      method: 'PUT',
      data
    })
  }

  // DELETE请求
  delete<T = any>(url: string, params?: Record<string, any>, options?: Partial<RequestConfig>): Promise<ResponseData<T>> {
    return this.request<T>({
      ...options,
      url,
      method: 'DELETE',
      params
    })
  }

  // PATCH请求
  patch<T = any>(url: string, data?: any, options?: Partial<RequestConfig>): Promise<ResponseData<T>> {
    return this.request<T>({
      ...options,
      url,
      method: 'PATCH',
      data
    })
  }

  // 文件上传
  upload<T = any>(url: string, file: File | FormData, options?: Partial<RequestConfig>): Promise<ResponseData<T>> {
    const formData = file instanceof FormData ? file : new FormData()
    if (file instanceof File) {
      formData.append('file', file)
    }

    return this.request<T>({
      ...options,
      url,
      method: 'POST',
      data: formData,
      headers: {
        // 不要设置Content-Type，让浏览器自动设置
        ...options?.headers
      },
      timeout: 60000, // 上传超时时间更长
      cache: false // 上传不缓存
    })
  }

  // 批量请求
  async batch<T = any>(requests: Array<() => Promise<any>>): Promise<{
    successful: Array<{ status: 'fulfilled'; value: any }>
    failed: Array<{ status: 'rejected'; reason: any }>
    total: number
    successCount: number
    failCount: number
  }> {
    const results = await Promise.allSettled(requests.map(req => req()))

    const successful = results.filter(result => result.status === 'fulfilled') as any
    const failed = results.filter(result => result.status === 'rejected') as any

    return {
      successful,
      failed,
      total: results.length,
      successCount: successful.length,
      failCount: failed.length
    }
  }

  // 清除缓存
  clearCache(pattern?: string): void {
    if (pattern) {
      for (const key of this.cache.keys()) {
        if (key.includes(pattern)) {
          this.cache.delete(key)
        }
      }
    } else {
      this.cache.clear()
    }
  }

  // 获取缓存统计
  getCacheStats(): { size: number; keys: string[] } {
    return {
      size: this.cache.size,
      keys: Array.from(this.cache.keys())
    }
  }
}

// 创建单例
const httpClient = new HttpClient()

// 根据环境设置日志级别
if (import.meta.env.DEV) {
  httpClient.setLogLevel('debug')
} else {
  httpClient.setLogLevel('warn')
}

export { httpClient, MICROSERVICE_CONFIG }
export default httpClient