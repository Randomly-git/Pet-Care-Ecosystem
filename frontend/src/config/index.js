/**
 * 应用配置文件
 * 包含API地址、环境变量等配置
 */

// 环境检测
const isDevelopment = import.meta.env.DEV
const isProduction = import.meta.env.PROD

// API配置
export const API_CONFIG = {
  // 基础API地址（通过前端代理）
  BASE_URL: isDevelopment
    ? '/api'  // 开发环境使用前端代理
    : 'http://47.100.240.111:8082/api',  // 生产环境保持不变

  // 请求超时时间
  TIMEOUT: 10000,

  // 重试次数
  RETRY_COUNT: 3,

  // 重试延迟
  RETRY_DELAY: 1000,
}

// 应用配置
export const APP_CONFIG = {
  // 应用名称
  APP_NAME: '笑猫の窝',

  // 应用版本
  VERSION: '1.0.0',

  // Token存储键名
  TOKEN_KEY: 'authToken',

  // 用户信息存储键名
  USER_INFO_KEY: 'userInfo',

  // 当前宠物存储键名
  CURRENT_PET_KEY: 'currentPet',

  // Token过期时间（毫秒）
  TOKEN_EXPIRES: 24 * 60 * 60 * 1000, // 24小时

  // 请求失败重试次数
  REQUEST_RETRY_COUNT: 3,
}

// 路由配置
export const ROUTE_CONFIG = {
  // 公共路由（不需要登录）
  PUBLIC_ROUTES: [
    '/',
    '/login',
    '/register',
    '/community',  // 社区页面也设为公共，让用户可以浏览
  ],

  // 需要登录的路由
  PROTECTED_ROUTES: [
    '/space',
    '/activities',
    '/medical',
    '/shop',
  ],

  // 登录后重定向路由
  DEFAULT_REDIRECT: '/',

  // 登录页面路由
  LOGIN_ROUTE: '/login',
}

// 分页配置
export const PAGINATION_CONFIG = {
  // 默认页码
  DEFAULT_PAGE: 1,

  // 默认每页大小
  DEFAULT_SIZE: 10,

  // 最大每页大小
  MAX_SIZE: 100,

  // 页码选择器选项
  SIZE_OPTIONS: [10, 20, 50, 100],
}

// 文件上传配置
export const UPLOAD_CONFIG = {
  // 最大文件大小（字节）
  MAX_FILE_SIZE: 5 * 1024 * 1024, // 5MB

  // 支持的图片格式
  ALLOWED_IMAGE_TYPES: [
    'image/jpeg',
    'image/png',
    'image/gif',
    'image/webp'
  ],

  // 支持的文件格式
  ALLOWED_FILE_TYPES: [
    'image/jpeg',
    'image/png',
    'image/gif',
    'image/webp',
    'application/pdf',
    'text/plain'
  ],
}

// 缓存配置
export const CACHE_CONFIG = {
  // API响应缓存时间（毫秒）
  API_CACHE_TIME: 5 * 60 * 1000, // 5分钟

  // 静态资源缓存时间（毫秒）
  STATIC_CACHE_TIME: 24 * 60 * 60 * 1000, // 24小时

  // 用户信息缓存时间（毫秒）
  USER_CACHE_TIME: 30 * 60 * 1000, // 30分钟
}

// 错误码映射
export const ERROR_CODE_MAP = {
  // 网络错误
  NETWORK_ERROR: 'NETWORK_ERROR',
  TIMEOUT: 'TIMEOUT',
  CONNECT_ERROR: 'CONNECT_ERROR',

  // HTTP状态码
  BAD_REQUEST: 'BAD_REQUEST', // 400
  UNAUTHORIZED: 'UNAUTHORIZED', // 401
  FORBIDDEN: 'FORBIDDEN', // 403
  NOT_FOUND: 'NOT_FOUND', // 404
  CONFLICT: 'CONFLICT', // 409
  SERVER_ERROR: 'SERVER_ERROR', // 500

  // 业务错误码
  USER_NOT_FOUND: 'USER_NOT_FOUND',
  PET_NOT_FOUND: 'PET_NOT_FOUND',
  INVALID_CREDENTIALS: 'INVALID_CREDENTIALS',
  TOKEN_EXPIRED: 'TOKEN_EXPIRED',
  VALIDATION_ERROR: 'VALIDATION_ERROR',
}

// 日期格式配置
export const DATE_FORMAT = {
  // API日期格式
  API_DATETIME: 'YYYY-MM-DDTHH:mm:ss',
  API_DATE: 'YYYY-MM-DD',

  // 显示日期格式
  DISPLAY_DATETIME: 'YYYY-MM-DD HH:mm',
  DISPLAY_DATE: 'YYYY-MM-DD',
  DISPLAY_TIME: 'HH:mm',

  // 中文日期格式
  CN_DATETIME: 'YYYY年MM月DD日 HH:mm',
  CN_DATE: 'YYYY年MM月DD日',
}

// 调试配置
export const DEBUG_CONFIG = {
  // 是否启用调试模式
  ENABLED: isDevelopment,

  // 是否显示API请求日志
  SHOW_API_LOG: isDevelopment,

  // 是否显示网络错误详情
  SHOW_ERROR_DETAIL: isDevelopment,

  // 是否启用性能监控
  ENABLE_PERFORMANCE_MONITOR: false,
}

// 默认导出
export default {
  ...API_CONFIG,
  ...APP_CONFIG,
  ...ROUTE_CONFIG,
  ...PAGINATION_CONFIG,
  ...UPLOAD_CONFIG,
  ...CACHE_CONFIG,
  ...ERROR_CODE_MAP,
  ...DATE_FORMAT,
  ...DEBUG_CONFIG,
}

// 获取当前环境
export const getCurrentEnv = () => {
  if (isDevelopment) return 'development'
  if (isProduction) return 'production'
  return 'test'
}

// 是否为移动端
export const isMobile = () => {
  return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)
}

// 获取设备信息
export const getDeviceInfo = () => {
  const userAgent = navigator.userAgent

  return {
    userAgent,
    isMobile: isMobile(),
    isTablet: /iPad|Android/i.test(userAgent) && !/Mobile/i.test(userAgent),
    isDesktop: !isMobile() && !isTablet(),
    browser: getBrowserInfo(),
    os: getOSInfo()
  }
}

// 获取浏览器信息
const getBrowserInfo = () => {
  const userAgent = navigator.userAgent

  if (userAgent.indexOf('Chrome') > -1) return 'Chrome'
  if (userAgent.indexOf('Safari') > -1) return 'Safari'
  if (userAgent.indexOf('Firefox') > -1) return 'Firefox'
  if (userAgent.indexOf('Edge') > -1) return 'Edge'
  if (userAgent.indexOf('Opera') > -1) return 'Opera'
  return 'Unknown'
}

// 获取操作系统信息
const getOSInfo = () => {
  const userAgent = navigator.userAgent

  if (userAgent.indexOf('Windows') > -1) return 'Windows'
  if (userAgent.indexOf('Mac') > -1) return 'macOS'
  if (userAgent.indexOf('Linux') > -1) return 'Linux'
  if (userAgent.indexOf('Android') > -1) return 'Android'
  if (userAgent.indexOf('iOS') > -1) return 'iOS'
  return 'Unknown'
}