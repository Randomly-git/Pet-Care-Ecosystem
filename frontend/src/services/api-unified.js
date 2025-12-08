/**
 * 统一API配置文件 - 规范化微服务架构下的API调用
 * 所有API调用都通过前端代理服务器，不再直接调用后端端口
 */

// 环境变量配置
const ENV = {
  DEV: 'development',
  PROD: 'production'
}

// API调用方式枚举
export const API_METHODS = {
  GET: 'GET',
  POST: 'POST',
  PUT: 'PUT',
  DELETE: 'DELETE'
}

// API响应状态
export const API_STATUS = {
  SUCCESS: 200,
  CREATED: 201,
  NO_CONTENT: 204,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  INTERNAL_ERROR: 500
}

/**
 * 统一的API请求封装类
 * 所有请求都通过前端代理转发到相应的微服务
 */
class UnifiedAPI {
  constructor() {
    this.timeout = 10000 // 10秒超时
  }

  /**
   * 通用请求方法
   * @param {string} endpoint - API端点（相对路径，如 /api/users/me）
   * @param {Object} options - 请求选项
   * @returns {Promise} 响应数据
   */
  async request(endpoint, options = {}) {
    // 验证端点格式
    if (!endpoint.startsWith('/')) {
      throw new Error(`API端点必须以'/'开头，当前端点: ${endpoint}`)
    }

    const config = {
      method: API_METHODS.GET,
      headers: {
        'Content-Type': 'application/json',
        ...this.getAuthHeaders(),
        ...options.headers
      },
      timeout: this.timeout,
      ...options
    }

    try {
      const controller = new AbortController()
      config.signal = controller.signal

      const timeoutId = setTimeout(() => controller.abort(), this.timeout)

      const response = await fetch(endpoint, config)
      clearTimeout(timeoutId)

      if (!response.ok) {
        const errorText = await response.text()
        throw new Error(`API请求失败 ${response.status}: ${errorText}`)
      }

      // 处理204无内容响应
      if (response.status === API_STATUS.NO_CONTENT) {
        return null
      }

      return await response.json()
    } catch (error) {
      if (error.name === 'AbortError') {
        throw new Error('API请求超时')
      }
      console.error(`API请求失败 [${endpoint}]:`, error)
      throw error
    }
  }

  /**
   * 获取认证头
   * @returns {Object} 认证头对象
   */
  getAuthHeaders() {
    const token = localStorage.getItem('token')
    return token ? { Authorization: `Bearer ${token}` } : {}
  }

  /**
   * GET请求
   * @param {string} endpoint - API端点
   * @param {Object} params - 查询参数
   * @returns {Promise} 响应数据
   */
  async get(endpoint, params = {}) {
    const queryString = new URLSearchParams(params).toString()
    const url = queryString ? `${endpoint}?${queryString}` : endpoint
    return this.request(url, { method: API_METHODS.GET })
  }

  /**
   * POST请求
   * @param {string} endpoint - API端点
   * @param {Object} data - 请求数据
   * @returns {Promise} 响应数据
   */
  async post(endpoint, data = {}) {
    return this.request(endpoint, {
      method: API_METHODS.POST,
      body: JSON.stringify(data)
    })
  }

  /**
   * PUT请求
   * @param {string} endpoint - API端点
   * @param {Object} data - 请求数据
   * @returns {Promise} 响应数据
   */
  async put(endpoint, data = {}) {
    return this.request(endpoint, {
      method: API_METHODS.PUT,
      body: JSON.stringify(data)
    })
  }

  /**
   * DELETE请求
   * @param {string} endpoint - API端点
   * @returns {Promise} 响应数据
   */
  async delete(endpoint) {
    return this.request(endpoint, { method: API_METHODS.DELETE })
  }

  /**
   * 文件上传请求
   * @param {string} endpoint - API端点
   * @param {FormData} formData - 表单数据
   * @param {Function} onProgress - 进度回调
   * @returns {Promise} 响应数据
   */
  async upload(endpoint, formData, onProgress) {
    // 验证端点格式
    if (!endpoint.startsWith('/')) {
      throw new Error(`API端点必须以'/'开头，当前端点: ${endpoint}`)
    }

    // 验证FormData
    if (!(formData instanceof FormData)) {
      throw new Error('文件上传必须使用FormData')
    }

    const config = {
      method: API_METHODS.POST,
      body: formData,
      headers: {
        // 不要设置Content-Type，让浏览器自动设置multipart/form-data
        ...this.getAuthHeaders()
      }
    }

    try {
      const response = await fetch(endpoint, config)

      if (!response.ok) {
        const errorText = await response.text()
        throw new Error(`文件上传失败 ${response.status}: ${errorText}`)
      }

      return await response.json()
    } catch (error) {
      console.error(`文件上传失败 [${endpoint}]:`, error)
      throw error
    }
  }
}

// 创建统一API实例
const api = new UnifiedAPI()

// ==================== 微服务API定义 ====================

/**
 * 用户相关API
 * 路径前缀: /api/users
 * 代理到: petcare-backend (8082)
 */
export const userAPI = {
  // 获取当前用户信息
  getCurrentUser: () => api.get('/api/users/me'),

  // 获取指定用户信息
  getUserById: (userId) => api.get(`/api/users/${userId}`),

  // 批量获取用户信息
  getUsersByIds: (userIds) => api.post('/api/users/batch', { userIds }),

  // 更新用户信息
  updateUser: (userId, data) => api.put(`/api/users/${userId}`, data),

  // 上传用户头像
  uploadAvatar: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return api.upload('/api/media/upload', formData)
  }
}

/**
 * 宠物相关API
 * 路径前缀: /api/pets
 * 代理到: petcare-backend (8082)
 */
export const petAPI = {
  // 获取所有宠物
  getAllPets: () => api.get('/api/pets'),

  // 获取用户的宠物
  getUserPets: (userId) => api.get('/api/pets', { userId }),

  // 获取指定宠物信息
  getPetById: (petId) => api.get(`/api/pets/${petId}`),

  // 创建宠物
  createPet: (data) => api.post('/api/pets', data),

  // 更新宠物信息
  updatePet: (petId, data) => api.put(`/api/pets/${petId}`, data),

  // 删除宠物
  deletePet: (petId) => api.delete(`/api/pets/${petId}`)
}

/**
 * 活动记录相关API
 * 路径前缀: /api/activities
 * 代理到: petcare-backend (8082)
 */
export const activityAPI = {
  // 获取宠物的活动记录
  getPetActivities: (petId, params = {}) => api.get(`/api/activities/pet/${petId}`, params),

  // 创建活动记录
  createActivity: (data) => api.post('/api/activities', data),

  // 批量获取多个宠物的活动记录
  getActivitiesByPetIds: (petIds) => api.post('/api/activities/records/batch', petIds),

  // 获取活动统计数据
  getActivityStats: (petId, period) => api.get(`/api/activities/stats/${petId}`, { period })
}

/**
 * 媒体文件相关API
 * 路径前缀: /api/media
 * 代理到: media-backend (8081)
 */
export const mediaAPI = {
  // 上传单个文件
  uploadFile: (file, userId, businessType = 'MOMENT', businessId = null) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('userId', userId)
    if (businessType) formData.append('businessType', businessType)
    if (businessId) formData.append('businessId', businessId)
    return api.upload('/api/media/upload', formData)
  },

  // 获取媒体文件信息
  getMediaInfo: (mediaId) => api.get(`/api/media/${mediaId}`),

  // 删除媒体文件
  deleteMedia: (mediaId) => api.delete(`/api/media/${mediaId}`),

  // 获取关联的媒体文件
  getRelatedMedia: (relatedType, relatedId) => api.get(`/api/media/related/${relatedType}/${relatedId}`)
}

/**
 * 社区相关API
 * 路径前缀: /api/community
 * 代理到: community-backend (8083)
 * 注意：会自动重写为 /api/v1
 */
export const communityAPI = {
  // 动态相关
  getMoments: (params = {}) => api.get('/api/community/moments', params),

  getMomentsByUser: (userId, params = {}) => api.get(`/api/community/moments/user/${userId}`, params),

  createMoment: (data) => api.post('/api/community/moments', data),

  deleteMoment: (momentId) => api.delete(`/api/community/moments/${momentId}`),

  // 评论相关
  getCommentsByMoment: (momentId, params = {}) => api.get(`/api/community/comments/moment/${momentId}`, params),

  createComment: (data) => api.post('/api/community/comments', data),

  deleteComment: (commentId) => api.delete(`/api/community/comments/${commentId}`),

  // 点赞相关
  toggleLike: (data) => api.post('/api/community/likes', data),

  getLikeStatus: (userId, targetType, targetId) =>
    api.get('/api/community/likes/status', { userId, targetType, targetId }),

  // 关注相关
  toggleFollow: (data) => api.post('/api/community/follows', data),

  getFollowersCount: (userId) => api.get(`/api/community/follows/followers/count/${userId}`),

  getFollowingCount: (userId) => api.get(`/api/community/follows/following/count/${userId}`)
}

// ==================== 工具函数 ====================

/**
 * API工具类
 */
export const apiUtils = {
  /**
   * 检查网络状态
   * @returns {boolean} 是否在线
   */
  isOnline: () => navigator.onLine,

  /**
   * 重试机制
   * @param {Function} apiCall - API调用函数
   * @param {number} maxRetries - 最大重试次数
   * @param {number} delay - 延迟时间（毫秒）
   * @returns {Promise} API结果
   */
  async withRetry(apiCall, maxRetries = 3, delay = 1000) {
    for (let i = 0; i <= maxRetries; i++) {
      try {
        return await apiCall()
      } catch (error) {
        if (i === maxRetries) throw error
        await new Promise(resolve => setTimeout(resolve, delay * Math.pow(2, i)))
      }
    }
  },

  /**
   * 缓存管理
   */
  cache: new Map(),

  /**
   * 获取缓存数据
   * @param {string} key - 缓存键
   * @param {Function} apiCall - API调用函数
   * @param {number} ttl - 缓存时间（毫秒）
   * @returns {Promise} 数据
   */
  async getCached(key, apiCall, ttl = 5 * 60 * 1000) {
    const cached = this.cache.get(key)
    if (cached && Date.now() - cached.timestamp < ttl) {
      return cached.data
    }

    try {
      const data = await apiCall()
      this.cache.set(key, {
        data,
        timestamp: Date.now()
      })
      return data
    } catch (error) {
      // 如果有缓存数据但请求失败，返回缓存数据
      if (cached) {
        return cached.data
      }
      throw error
    }
  },

  /**
   * 清除缓存
   * @param {string} pattern - 匹配模式
   */
  clearCache(pattern) {
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
}

// 导出默认API实例
export default api