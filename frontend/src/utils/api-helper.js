/**
 * API响应处理工具类
 * 提供统一的API数据处理和错误处理方法
 */

/**
 * 处理API响应数据
 * @param {Object} response API响应对象
 * @returns {Object} 处理后的数据
 */
export const handleApiResponse = (response) => {
  // 处理统一的ApiResponse格式
  if (response && typeof response === 'object') {
    // 如果有success字段，说明是标准API响应
    if ('success' in response) {
      if (response.success) {
        return {
          success: true,
          data: response.data,
          message: response.message || '操作成功'
        }
      } else {
        return {
          success: false,
          data: null,
          message: response.message || '操作失败'
        }
      }
    }

    // 如果没有success字段，直接返回数据
    return {
      success: true,
      data: response,
      message: '获取数据成功'
    }
  }

  return {
    success: false,
    data: null,
    message: '响应数据格式错误'
  }
}

/**
 * 处理API错误
 * @param {Object} error 错误对象
 * @returns {Object} 格式化的错误信息
 */
export const handleApiError = (error) => {
  const result = {
    success: false,
    data: null,
    message: '网络请求失败'
  }

  if (error.response) {
    // 服务器返回了错误状态码
    const { status, data } = error.response

    result.status = status

    switch (status) {
      case 400:
        result.message = data?.message || '请求参数错误'
        break
      case 401:
        result.message = '登录已过期，请重新登录'
        result.code = 'UNAUTHORIZED'
        break
      case 403:
        result.message = '权限不足，无法访问'
        result.code = 'FORBIDDEN'
        break
      case 404:
        result.message = data?.message || '请求的资源不存在'
        result.code = 'NOT_FOUND'
        break
      case 409:
        result.message = data?.message || '数据冲突'
        result.code = 'CONFLICT'
        break
      case 500:
        result.message = '服务器内部错误'
        result.code = 'SERVER_ERROR'
        break
      default:
        result.message = data?.message || `请求失败 (${status})`
        result.code = `HTTP_${status}`
    }
  } else if (error.code) {
    // 网络错误
    switch (error.code) {
      case 'ECONNABORTED':
        result.message = '请求超时，请检查网络连接'
        result.code = 'TIMEOUT'
        break
      case 'NETWORK_ERROR':
      case 'ERR_NETWORK':
        result.message = '网络连接失败，请检查网络'
        result.code = 'NETWORK_ERROR'
        break
      default:
        result.message = error.message || '网络请求失败'
        result.code = error.code
    }
  } else {
    result.message = error.message || '未知错误'
    result.code = 'UNKNOWN_ERROR'
  }

  return result
}

/**
 * 格式化日期时间为后端API需要的格式
 * @param {Date|string} date 日期对象或字符串
 * @returns {string} 格式化后的日期字符串
 */
export const formatDateForApi = (date) => {
  if (!date) return null

  const d = new Date(date)
  if (isNaN(d.getTime())) return null

  // 返回ISO格式: yyyy-MM-dd'T'HH:mm:ss
  return d.toISOString().slice(0, 19).replace('T', 'T')
}

/**
 * 格式化日期为后端API需要的格式（LocalDate）
 * @param {Date|string} date 日期对象或字符串
 * @returns {string} 格式化后的日期字符串 (yyyy-MM-dd)
 */
export const formatDateForApiDate = (date) => {
  if (!date) return null

  const d = new Date(date)
  if (isNaN(d.getTime())) return null

  return d.toISOString().slice(0, 10)
}

/**
 * 从后端响应中提取用户信息
 * @param {Object} response API响应
 * @returns {Object} 用户信息对象
 */
export const extractUserInfo = (response) => {
  const result = handleApiResponse(response)
  if (!result.success) return null

  const { data } = result
  return {
    userId: data.userId,
    username: data.name,
    token: data.token,
    // 可以根据需要添加其他字段
  }
}

/**
 * 验证API响应数据
 * @param {Object} data 要验证的数据
 * @param {Array} requiredFields 必需的字段数组
 * @returns {boolean} 验证结果
 */
export const validateApiResponse = (data, requiredFields = []) => {
  if (!data || typeof data !== 'object') {
    return false
  }

  return requiredFields.every(field => {
    const keys = field.split('.')
    let current = data

    for (const key of keys) {
      if (current[key] === undefined || current[key] === null) {
        return false
      }
      current = current[key]
    }

    return true
  })
}

/**
 * 创建标准的API请求参数
 * @param {Object} params 原始参数对象
 * @param {Object} options 选项
 * @returns {Object} 格式化后的参数
 */
export const createApiParams = (params = {}, options = {}) => {
  const {
    removeNull = true,
    removeEmpty = true,
    formatDate = false,
    formatDateFields = []
  } = options

  let result = { ...params }

  // 移除null值
  if (removeNull) {
    Object.keys(result).forEach(key => {
      if (result[key] === null || result[key] === undefined) {
        delete result[key]
      }
    })
  }

  // 移除空字符串
  if (removeEmpty) {
    Object.keys(result).forEach(key => {
      if (result[key] === '') {
        delete result[key]
      }
    })
  }

  // 格式化日期字段
  if (formatDate) {
    formatDateFields.forEach(field => {
      if (result[field]) {
        result[field] = formatDateForApi(result[field])
      }
    })
  }

  return result
}

/**
 * 处理分页参数
 * @param {number} page 页码
 * @param {number} size 每页大小
 * @returns {Object} 分页参数对象
 */
export const createPaginationParams = (page = 1, size = 10) => {
  return {
    page: Math.max(1, parseInt(page) || 1),
    size: Math.max(1, Math.min(100, parseInt(size) || 10))
  }
}

export default {
  handleApiResponse,
  handleApiError,
  formatDateForApi,
  formatDateForApiDate,
  extractUserInfo,
  validateApiResponse,
  createApiParams,
  createPaginationParams
}