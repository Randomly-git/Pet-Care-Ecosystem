// 文件位置: src/services/api.js
// API服务管理 - 微服务架构下的统一接口管理

import { MockAPI, isMockEnabled } from './mock'

// 微服务端点配置
const MICROSERVICE_ENDPOINTS = {
  GATEWAY: import.meta.env.VITE_API_GATEWAY || 'http://localhost:8082', // petcare-backend
  USER_SERVICE: import.meta.env.VITE_USER_SERVICE || 'http://localhost:8082', // petcare-backend中的用户服务
  PET_SERVICE: import.meta.env.VITE_PET_SERVICE || 'http://localhost:8082', // petcare-backend中的宠物服务
  ACTIVITY_SERVICE: import.meta.env.VITE_ACTIVITY_SERVICE || 'http://localhost:8082', // petcare-backend中的活动服务
  MEDICAL_SERVICE: import.meta.env.VITE_MEDICAL_SERVICE || 'http://localhost:8082', // petcare-backend中的状态记录服务
  SHOP_SERVICE: import.meta.env.VITE_SHOP_SERVICE || 'http://localhost:8082', // 未来扩展
  // 新增的微服务
  MEDIA_SERVICE: import.meta.env.VITE_MEDIA_SERVICE || 'http://localhost:8081', // 媒体微服务
  COMMUNITY_SERVICE: import.meta.env.VITE_COMMUNITY_SERVICE || 'http://localhost:8083' // 社区微服务
}

// API请求封装
class ApiService {
  constructor() {
    this.baseURL = MICROSERVICE_ENDPOINTS.GATEWAY
    this.timeout = 10000 // 10秒超时
  }

  // 通用请求方法
  async request(endpoint, options = {}) {
    // 如果启用Mock模式，直接返回Mock数据
    if (isMockEnabled()) {
      return this.handleMockRequest(endpoint, options)
    }

    const url = `${this.baseURL}${endpoint}`
    const config = {
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

      const response = await fetch(url, config)
      clearTimeout(timeoutId)

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }

      return await response.json()
    } catch (error) {
      if (error.name === 'AbortError') {
        throw new Error('请求超时')
      }
      console.error('API请求失败:', error)
      throw error
    }
  }

  // 处理Mock请求
  async handleMockRequest(endpoint, options) {
    const method = options.method || 'GET'
    const data = options.body ? JSON.parse(options.body) : null

    // 路由到对应的Mock方法
    if (endpoint.includes('/user/')) {
      if (method === 'GET') {
        return MockAPI.getUserInfo()
      } else if (method === 'PUT') {
        return MockAPI.updateProfile(data)
      }
    }

    if (endpoint.includes('/pet/')) {
      if (method === 'GET') {
        if (endpoint.includes('/list/')) {
          return MockAPI.getPetList()
        }
        return MockAPI.getPetInfo()
      } else if (method === 'PUT') {
        return MockAPI.updatePetInfo(data)
      }
    }

    if (endpoint.includes('/activity')) {
      if (method === 'GET') {
        return MockAPI.getActivities()
      } else if (method === 'POST') {
        if (endpoint.includes('/toggle')) {
          return MockAPI.toggleActivity()
        }
        return MockAPI.createActivity(data)
      } else if (method === 'PUT') {
        return MockAPI.updateActivity()
      } else if (method === 'DELETE') {
        return MockAPI.deleteActivity()
      }
    }

    if (endpoint.includes('/medical')) {
      return MockAPI.getMedicalRecords()
    }

    if (endpoint.includes('/shop')) {
      return MockAPI.getProducts()
    }

    return { success: true, message: 'Mock response' }
  }

  // 获取认证头
  getAuthHeaders() {
    const token = localStorage.getItem('token')
    return token ? { Authorization: `Bearer ${token}` } : {}
  }

  // GET请求
  async get(endpoint, params = {}) {
    const queryString = new URLSearchParams(params).toString()
    const url = queryString ? `${endpoint}?${queryString}` : endpoint
    return this.request(url)
  }

  // POST请求
  async post(endpoint, data) {
    return this.request(endpoint, {
      method: 'POST',
      body: JSON.stringify(data)
    })
  }

  // PUT请求
  async put(endpoint, data) {
    return this.request(endpoint, {
      method: 'PUT',
      body: JSON.stringify(data)
    })
  }

  // DELETE请求
  async delete(endpoint) {
    return this.request(endpoint, {
      method: 'DELETE'
    })
  }

  // 文件上传
  async upload(endpoint, file, onProgress) {
    if (isMockEnabled()) {
      return { url: 'mock-uploaded-file.jpg' }
    }

    const formData = new FormData()
    formData.append('file', file)

    return this.request(endpoint, {
      method: 'POST',
      body: formData,
      headers: {
        // 不要设置Content-Type，让浏览器自动设置
        ...this.getAuthHeaders()
      }
    })
  }
}

// 创建API实例
const apiService = new ApiService()

// 微服务专用API
export const userAPI = {
  getUserInfo: (userId) => apiService.get('/user/info'),
  updateProfile: (data) => apiService.put('/user/profile', data),
  login: (credentials) => apiService.post('/auth/login', credentials),
  logout: () => apiService.post('/auth/logout'),
  register: (userData) => apiService.post('/auth/register', userData)
}

export const petAPI = {
  getPetInfo: (petId) => apiService.get(`/pet/${petId}`),
  getPetList: (userId) => apiService.get(`/pet/list/${userId}`),
  updatePetInfo: (petId, data) => apiService.put(`/pet/${petId}`, data),
  createPet: (data) => apiService.post('/pet', data),
  deletePet: (petId) => apiService.delete(`/pet/${petId}`)
}

export const activityAPI = {
  getActivities: (petId, date) => apiService.get('/activity', { petId, date }),
  toggleActivity: (activityId) => apiService.post(`/activity/${activityId}/toggle`),
  createActivity: (data) => apiService.post('/activity', data),
  updateActivity: (activityId, data) => apiService.put(`/activity/${activityId}`, data),
  deleteActivity: (activityId) => apiService.delete(`/activity/${activityId}`),
  getActivityStats: (petId, period) => apiService.get('/activity/stats', { petId, period })
}

export const medicalAPI = {
  getMedicalRecords: (petId) => apiService.get(`/medical/${petId}`),
  createMedicalRecord: (data) => apiService.post('/medical', data),
  updateMedicalRecord: (recordId, data) => apiService.put(`/medical/${recordId}`, data),
  scheduleAppointment: (data) => apiService.post('/medical/appointment', data),
  getVaccinationSchedule: (petId) => apiService.get(`/medical/${petId}/vaccinations`)
}

export const shopAPI = {
  getProducts: (category, page = 1) => apiService.get('/shop/products', { category, page }),
  getProductDetail: (productId) => apiService.get(`/shop/products/${productId}`),
  createOrder: (data) => apiService.post('/shop/orders', data),
  getOrders: (userId, status) => apiService.get('/shop/orders', { userId, status }),
  updateOrderStatus: (orderId, status) => apiService.put(`/shop/orders/${orderId}`, { status })
}

// 媒体服务API
export const mediaAPI = {
  // 上传文件到媒体微服务（通过前端代理）
  uploadFile: async (file, onProgress) => {
    const endpoint = '/api/media/upload'
    return apiService.upload(endpoint, file, onProgress)
  },

  // 获取媒体信息（通过前端代理）
  getMediaInfo: (mediaId) => {
    const endpoint = `/api/media/${mediaId}`
    return fetch(endpoint).then(res => res.json())
  },

  // 获取关联的媒体文件（通过前端代理）
  getRelatedMedia: (relatedType, relatedId) => {
    const endpoint = `/api/media/related/${relatedType}/${relatedId}`
    return fetch(endpoint).then(res => res.json())
  }
}

// 保留原有的文件上传API（向后兼容）
export const uploadAPI = {
  uploadImage: (file, onProgress) => mediaAPI.uploadFile(file, onProgress),
  uploadAvatar: (file) => mediaAPI.uploadFile(file),
  uploadMedia: (file, onProgress) => mediaAPI.uploadFile(file, onProgress)
}

// 社区服务API
export const communityAPI = {
  // 动态相关（通过前端代理）
  getMomentsByUser: (userId) => {
    const endpoint = `/api/community/moments/user/${userId}`
    return fetch(endpoint).then(res => res.json())
  },

  createMoment: (userId, content, mediaIds = []) => {
    const endpoint = '/api/community/moments'
    return fetch(endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        userId,
        content,
        mediaIds
      })
    }).then(res => res.json())
  },

  deleteMoment: (momentId) => {
    const endpoint = `/api/community/moments/${momentId}`
    return fetch(endpoint, {
      method: 'DELETE'
    }).then(res => res.ok)
  },

  // 评论相关
  getCommentsByMoment: (momentId) => {
    const endpoint = `/api/community/comments/moment/${momentId}`
    return fetch(endpoint).then(res => res.json())
  },

  createComment: (userId, momentId, content, parentId = null) => {
    const endpoint = '/api/community/comments'
    const requestBody = {
      userId,
      momentId,
      content
    }
    if (parentId) {
      requestBody.parentId = parentId
    }
    return fetch(endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(requestBody)
    }).then(res => res.json())
  },

  deleteComment: (commentId) => {
    const endpoint = `/api/community/comments/${commentId}`
    return fetch(endpoint, {
      method: 'DELETE'
    }).then(res => res.ok)
  },

  // 点赞相关
  toggleLike: (userId, targetType, targetId) => {
    const endpoint = '/api/community/likes'
    return fetch(endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        userId,
        targetType,
        targetId
      })
    }).then(res => res.ok)
  },

  // 关注相关
  toggleFollow: (followerId, followedId) => {
    const endpoint = '/api/community/follows'
    return fetch(endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        followerId,
        followedId
      })
    }).then(res => res.ok)
  },

  getFollowersCount: (userId) => {
    const endpoint = `/api/community/follows/followers/count/${userId}`
    return fetch(endpoint).then(res => res.text()).then(text => parseInt(text))
  },

  getFollowingCount: (userId) => {
    const endpoint = `/api/community/follows/following/count/${userId}`
    return fetch(endpoint).then(res => res.text()).then(text => parseInt(text))
  }
}

// 工具函数
export const apiUtils = {
  // 检查网络状态
  isOnline: () => navigator.onLine,

  // 重试机制
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

  // 缓存管理
  cache: new Map(),

  async getCached(key, apiCall, ttl = 5 * 60 * 1000) { // 默认5分钟缓存
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

  // 清除缓存
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

export default apiService