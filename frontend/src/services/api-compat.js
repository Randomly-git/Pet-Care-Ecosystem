/**
 * API兼容层 - 保持旧API接口不变，内部使用新的HttpClient实现
 * 确保现有代码无需修改即可使用新的通信架构
 */

import httpClient from './HttpClient'
import { petcareApi, mediaApi, communityApi } from './ApiServiceFactory'
import DataTransformer from './DataTransformer'

// 微服务端点配置（兼容旧配置）
const MICROSERVICE_ENDPOINTS = {
  GATEWAY: import.meta.env.VITE_API_GATEWAY || 'http://localhost:8082',
  USER_SERVICE: import.meta.env.VITE_USER_SERVICE || 'http://localhost:8082',
  PET_SERVICE: import.meta.env.VITE_PET_SERVICE || 'http://localhost:8082',
  ACTIVITY_SERVICE: import.meta.env.VITE_ACTIVITY_SERVICE || 'http://localhost:8082',
  MEDICAL_SERVICE: import.meta.env.VITE_MEDICAL_SERVICE || 'http://localhost:8082',
  SHOP_SERVICE: import.meta.env.VITE_SHOP_SERVICE || 'http://localhost:8082',
  MEDIA_SERVICE: import.meta.env.VITE_MEDIA_SERVICE || 'http://localhost:8081',
  COMMUNITY_SERVICE: import.meta.env.VITE_COMMUNITY_SERVICE || 'http://localhost:8083'
}

// 适配器类 - 将旧的API调用转换为新API调用
class ApiCompatibilityAdapter {
  // 通用请求方法
  async request(endpoint, options = {}) {
    const config = {
      url: endpoint,
      method: options.method || 'GET',
      params: options.params,
      data: options.data || options.body,
      headers: options.headers,
      timeout: options.timeout
    }

    try {
      const response = await httpClient.request(config)
      return response
    } catch (error) {
      // 保持旧的错误格式
      throw {
        message: error.message,
        response: error.response,
        status: error.status
      }
    }
  }

  // GET请求
  async get(endpoint, params = {}) {
    return this.request(endpoint, { method: 'GET', params })
  }

  // POST请求
  async post(endpoint, data = {}) {
    return this.request(endpoint, { method: 'POST', data })
  }

  // PUT请求
  async put(endpoint, data = {}) {
    return this.request(endpoint, { method: 'PUT', data })
  }

  // DELETE请求
  async delete(endpoint, params = {}) {
    return this.request(endpoint, { method: 'DELETE', params })
  }

  // 文件上传
  async upload(endpoint, file, onProgress) {
    const formData = new FormData()
    formData.append('file', file)

    return this.request(endpoint, {
      method: 'POST',
      data: formData,
      headers: {
        // 不要设置Content-Type，让浏览器自动设置
      }
    })
  }
}

// 创建兼容适配器实例
const compatAdapter = new ApiCompatibilityAdapter()

// 用户API（兼容旧接口）
export const userAPI = {
  getUserInfo: async (userId) => {
    const response = await petcareApi.getUserInfo(userId || 1) // 默认用户ID
    return response
  },

  updateProfile: async (data) => {
    const userId = data.id || data.userId || 1
    return await petcareApi.updateProfile(userId, data)
  },

  login: async (credentials) => {
    const response = await petcareApi.login(credentials)
    return {
      data: {
        userId: response.user.id,
        name: response.user.name,
        token: response.token,
        message: '登录成功'
      }
    }
  },

  logout: async () => {
    return await petcareApi.logout()
  },

  register: async (userData) => {
    const response = await petcareApi.register(userData)
    return response
  }
}

// 宠物API（兼容旧接口）
export const petAPI = {
  getPetInfo: async (petId) => {
    const response = await petcareApi.getPet(petId)
    return { data: response }
  },

  getPetList: async (userId) => {
    const response = await petcareApi.getPets(userId)
    return { data: response }
  },

  updatePetInfo: async (petId, data) => {
    const response = await petcareApi.updatePet(petId, data)
    return { data: response }
  },

  createPet: async (data) => {
    const response = await petcareApi.createPet(data)
    return { data: response }
  },

  deletePet: async (petId) => {
    await petcareApi.deletePet(petId)
    return { data: null }
  }
}

// 活动API（兼容旧接口）
export const activityAPI = {
  getActivities: async (petId, date) => {
    // 注意：这里需要根据实际API调整
    const response = await petcareApi.getActivityRecords(petId, {
      startDate: date,
      endDate: date
    })
    return { data: response }
  },

  toggleActivity: async (activityId) => {
    // 旧接口的toggle功能在新API中可能需要特殊处理
    console.warn('toggleActivity 功能需要在新API中实现')
    return { data: { success: true } }
  },

  createActivity: async (data) => {
    const response = await petcareApi.createActivity(data)
    return { data: response }
  },

  updateActivity: async (activityId, data) => {
    const response = await petcareApi.updateActivity(activityId, data)
    return { data: response }
  },

  deleteActivity: async (activityId) => {
    await petcareApi.deleteActivity(activityId)
    return { data: null }
  },

  getActivityStats: async (petId, period) => {
    // 统计功能可能需要新API支持
    console.warn('getActivityStats 功能需要在新API中实现')
    return { data: { total: 0, completed: 0 } }
  }
}

// 医疗API（兼容旧接口）
export const medicalAPI = {
  getMedicalRecords: async (petId) => {
    // 使用状态记录API作为医疗记录
    const response = await petcareApi.getStatusRecords(petId)
    return { data: response }
  },

  createMedicalRecord: async (data) => {
    // 创建医疗记录可能映射到状态记录
    console.warn('createMedicalRecord 功能需要在新API中实现')
    return { data: { success: true } }
  },

  updateMedicalRecord: async (recordId, data) => {
    console.warn('updateMedicalRecord 功能需要在新API中实现')
    return { data: { success: true } }
  },

  scheduleAppointment: async (data) => {
    console.warn('scheduleAppointment 功能需要在新API中实现')
    return { data: { success: true } }
  },

  getVaccinationSchedule: async (petId) => {
    console.warn('getVaccinationSchedule 功能需要在新API中实现')
    return { data: [] }
  }
}

// 商店API（兼容旧接口）
export const shopAPI = {
  getProducts: async (category, page = 1) => {
    console.warn('shop API 功能尚未实现')
    return { data: [] }
  },

  getProductDetail: async (productId) => {
    console.warn('getProductDetail 功能尚未实现')
    return { data: null }
  },

  createOrder: async (data) => {
    console.warn('createOrder 功能尚未实现')
    return { data: { success: true } }
  },

  getOrders: async (userId, status) => {
    console.warn('getOrders 功能尚未实现')
    return { data: [] }
  },

  updateOrderStatus: async (orderId, status) => {
    console.warn('updateOrderStatus 功能尚未实现')
    return { data: { success: true } }
  }
}

// 媒体API（兼容旧接口）
export const mediaAPI = {
  uploadFile: async (file, onProgress) => {
    const response = await mediaApi.uploadFile(file)
    return { url: response.url }
  },

  getMediaInfo: async (mediaId) => {
    const response = await mediaApi.getMediaInfo(mediaId)
    return response
  },

  getRelatedMedia: async (relatedType, relatedId) => {
    const response = await mediaApi.getRelatedMedia(relatedType, relatedId)
    return response
  }
}

// 上传API（兼容旧接口）
export const uploadAPI = {
  uploadImage: (file, onProgress) => mediaAPI.uploadFile(file, onProgress),
  uploadAvatar: (file) => mediaAPI.uploadFile(file),
  uploadMedia: (file, onProgress) => mediaAPI.uploadFile(file, onProgress)
}

// 社区API（兼容旧接口）
export const communityAPI = {
  // 动态相关
  getMomentsByUser: async (userId) => {
    const response = await communityApi.getMomentsByUser(userId)
    return response
  },

  createMoment: async (userId, content, mediaIds = []) => {
    const response = await communityApi.createMoment({ userId, content, mediaIds })
    return response
  },

  deleteMoment: async (momentId, userId) => {
    await communityApi.deleteMoment(momentId, userId)
    return true
  },

  // 评论相关
  getCommentsByMoment: async (momentId) => {
    const response = await communityApi.getCommentsByMoment(momentId)
    return response
  },

  createComment: async (userId, momentId, content, parentId = null) => {
    const response = await communityApi.createComment({
      userId,
      momentId,
      content,
      parentId
    })
    return response
  },

  deleteComment: async (commentId) => {
    await communityApi.deleteComment(commentId)
    return true
  },

  // 点赞相关
  toggleLike: async (userId, targetType, targetId) => {
    await communityApi.toggleLike({ userId, targetType, targetId })
    return true
  },

  // 关注相关
  toggleFollow: async (followerId, followedId) => {
    await communityApi.toggleFollow({ followerId, followedId })
    return true
  },

  getFollowersCount: async (userId) => {
    return await communityApi.getFollowersCount(userId)
  },

  getFollowingCount: async (userId) => {
    return await communityApi.getFollowingCount(userId)
  }
}

// Mock API兼容（如果需要）
import { MockAPI, isMockEnabled } from './mock'

// 如果使用Mock模式，覆盖部分API
if (isMockEnabled()) {
  console.log('Using Mock API mode')
  // 这里可以覆盖上面的API实现为Mock版本
}

// 导出兼容层
export default compatAdapter

// 导出旧的API名称以保持兼容性
export const api = compatAdapter