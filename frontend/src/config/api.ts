/**
 * API配置文件
 * 集中管理所有API相关配置
 */

// 环境类型
export type Environment = 'development' | 'production' | 'testing'

// API配置接口
export interface ApiConfig {
  baseURL: string
  timeout: number
  retryCount: number
  retryDelay: number
  cache: {
    enabled: boolean
    defaultTTL: number // 毫秒
  }
  logging: {
    enabled: boolean
    level: 'debug' | 'info' | 'warn' | 'error'
    logRequests: boolean
    logResponses: boolean
    logErrors: boolean
  }
  features: {
    retry: boolean
    cache: boolean
    loading: boolean
    message: boolean
  }
}

// 微服务配置
export interface MicroserviceConfig {
  petcare: {
    baseURL: string
    endpoints: {
      auth: string
      user: string
      pets: string
      activities: string
      status: string
    }
  }
  media: {
    baseURL: string
    endpoints: {
      upload: string
      media: string
    }
  }
  community: {
    baseURL: string
    endpoints: {
      moments: string
      comments: string
      likes: string
    }
  }
}

// 环境配置
const environments: Record<Environment, ApiConfig> = {
  development: {
    baseURL: 'http://localhost:8082',
    timeout: 30000,
    retryCount: 3,
    retryDelay: 1000,
    cache: {
      enabled: true,
      defaultTTL: 5 * 60 * 1000 // 5分钟
    },
    logging: {
      enabled: true,
      level: 'debug',
      logRequests: true,
      logResponses: true,
      logErrors: true
    },
    features: {
      retry: true,
      cache: true,
      loading: true,
      message: true
    }
  },

  testing: {
    baseURL: 'http://localhost:8082',
    timeout: 10000,
    retryCount: 1,
    retryDelay: 500,
    cache: {
      enabled: false,
      defaultTTL: 0
    },
    logging: {
      enabled: true,
      level: 'warn',
      logRequests: false,
      logResponses: false,
      logErrors: true
    },
    features: {
      retry: true,
      cache: false,
      loading: true,
      message: false
    }
  },

  production: {
    baseURL: 'https://api.petcare.com',
    timeout: 20000,
    retryCount: 2,
    retryDelay: 2000,
    cache: {
      enabled: true,
      defaultTTL: 10 * 60 * 1000 // 10分钟
    },
    logging: {
      enabled: true,
      level: 'error',
      logRequests: false,
      logResponses: false,
      logErrors: true
    },
    features: {
      retry: true,
      cache: true,
      loading: true,
      message: true
    }
  }
}

// 微服务端点配置
const microserviceEndpoints: MicroserviceConfig = {
  petcare: {
    baseURL: import.meta.env.VITE_PETCARE_SERVICE || 'http://localhost:8082',
    endpoints: {
      auth: '/api/auth',
      user: '/api/user',
      pets: '/api/pets',
      activities: '/api/activities',
      status: '/api/status'
    }
  },
  media: {
    baseURL: import.meta.env.VITE_MEDIA_SERVICE || 'http://localhost:8081',
    endpoints: {
      upload: '/api/v1/media/upload',
      media: '/api/v1/media'
    }
  },
  community: {
    baseURL: import.meta.env.VITE_COMMUNITY_SERVICE || 'http://localhost:8083',
    endpoints: {
      moments: '/api/v1/moments',
      comments: '/api/v1/comments',
      likes: '/api/v1/likes'
    }
  }
}

// 当前环境
const currentEnvironment: Environment = (
  import.meta.env.VITE_NODE_ENV ||
  import.meta.env.MODE ||
  'development'
) as Environment

// 获取当前环境配置
export const getApiConfig = (): ApiConfig => {
  return environments[currentEnvironment] || environments.development
}

// 获取微服务配置
export const getMicroserviceConfig = (): MicroserviceConfig => {
  return microserviceEndpoints
}

// 错误码映射
export const ERROR_CODES = {
  // 网络错误
  NETWORK_ERROR: 'NETWORK_ERROR',
  TIMEOUT: 'TIMEOUT',
  ABORT: 'ABORT',

  // 认证错误
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,

  // 客户端错误
  BAD_REQUEST: 400,
  NOT_FOUND: 404,

  // 服务端错误
  INTERNAL_ERROR: 500,
  SERVICE_UNAVAILABLE: 503,
  GATEWAY_TIMEOUT: 504,

  // 业务错误
  USER_NOT_FOUND: 'USER_NOT_FOUND',
  PET_NOT_FOUND: 'PET_NOT_FOUND',
  ACTIVITY_NOT_FOUND: 'ACTIVITY_NOT_FOUND',
  INVALID_CREDENTIALS: 'INVALID_CREDENTIALS',
  USER_ALREADY_EXISTS: 'USER_ALREADY_EXISTS'
} as const

// 错误消息映射
export const ERROR_MESSAGES = {
  [ERROR_CODES.NETWORK_ERROR]: '网络连接失败，请检查网络设置',
  [ERROR_CODES.TIMEOUT]: '请求超时，请稍后重试',
  [ERROR_CODES.ABORT]: '请求已取消',
  [ERROR_CODES.UNAUTHORIZED]: '未授权，请重新登录',
  [ERROR_CODES.FORBIDDEN]: '无权限访问该资源',
  [ERROR_CODES.BAD_REQUEST]: '请求参数错误',
  [ERROR_CODES.NOT_FOUND]: '请求的资源不存在',
  [ERROR_CODES.INTERNAL_ERROR]: '服务器内部错误',
  [ERROR_CODES.SERVICE_UNAVAILABLE]: '服务暂时不可用',
  [ERROR_CODES.GATEWAY_TIMEOUT]: '网关超时',
  [ERROR_CODES.USER_NOT_FOUND]: '用户不存在',
  [ERROR_CODES.PET_NOT_FOUND]: '宠物不存在',
  [ERROR_CODES.ACTIVITY_NOT_FOUND]: '活动不存在',
  [ERROR_CODES.INVALID_CREDENTIALS]: '用户名或密码错误',
  [ERROR_CODES.USER_ALREADY_EXISTS]: '用户名已存在'
} as const

// 缓存键前缀
export const CACHE_KEYS = {
  USER: 'user:',
  PET: 'pet:',
  ACTIVITY: 'activity:',
  ACTIVITY_KINDS: 'activity:kinds',
  STATUS: 'status:',
  MOMENT: 'moment:',
  COMMENT: 'comment:'
} as const

// 请求头常量
export const HEADERS = {
  CONTENT_TYPE: 'Content-Type',
  AUTHORIZATION: 'Authorization',
  ACCEPT: 'Accept',
  X_REQUESTED_WITH: 'X-Requested-With',
  X_API_VERSION: 'X-API-Version'
} as const

// 内容类型
export const CONTENT_TYPES = {
  JSON: 'application/json',
  FORM_DATA: 'multipart/form-data',
  URL_ENCODED: 'application/x-www-form-urlencoded',
  TEXT: 'text/plain',
  HTML: 'text/html'
} as const

// HTTP方法
export const HTTP_METHODS = {
  GET: 'GET',
  POST: 'POST',
  PUT: 'PUT',
  PATCH: 'PATCH',
  DELETE: 'DELETE',
  HEAD: 'HEAD',
  OPTIONS: 'OPTIONS'
} as const

// 状态码分类
export const STATUS_CATEGORIES = {
  SUCCESS: [200, 201, 204],
  REDIRECT: [300, 301, 302, 303, 307, 308],
  CLIENT_ERROR: [400, 401, 403, 404, 405, 408, 409, 422, 429],
  SERVER_ERROR: [500, 501, 502, 503, 504]
} as const

// 默认配置
export const DEFAULT_CONFIG = {
  // 分页
  PAGINATION: {
    PAGE: 1,
    PAGE_SIZE: 10,
    PAGE_SIZES: [10, 20, 50, 100]
  },

  // 文件上传
  UPLOAD: {
    MAX_FILE_SIZE: 10 * 1024 * 1024, // 10MB
    ALLOWED_TYPES: [
      'image/jpeg',
      'image/png',
      'image/gif',
      'image/webp',
      'video/mp4',
      'video/webm'
    ]
  },

  // 请求配置
  REQUEST: {
    TIMEOUT: 30000,
    RETRY_COUNT: 3,
    RETRY_DELAY: 1000,
    CACHE_TTL: 5 * 60 * 1000 // 5分钟
  }
} as const

// 导出所有配置
export default {
  environments,
  currentEnvironment,
  microserviceEndpoints,
  getApiConfig,
  getMicroserviceConfig,
  ERROR_CODES,
  ERROR_MESSAGES,
  CACHE_KEYS,
  HEADERS,
  CONTENT_TYPES,
  HTTP_METHODS,
  STATUS_CATEGORIES,
  DEFAULT_CONFIG
}