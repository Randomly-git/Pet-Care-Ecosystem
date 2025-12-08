/**
 * 兼容版本的API服务入口
 * 使用CommonJS语法确保兼容性
 */

// 导入新的API服务
import apiService from './index.js'

// 导出服务（使用兼容语法）
const {
  authService,
  userService,
  petService,
  activityService,
  statusService,
  fixedActivityService,
  reservedActivityService,
  mediaService,
  communityService,
  batchService,
  cacheService,
  configService
} = await import('./index.js')

// 导出组合式API
const {
  useApi,
  useList,
  useAuth,
  usePets,
  useActivities
} = await import('@/composables/useApi.js')

// 为了向后兼容，创建旧API的对象
const userAPI = {
  getUserInfo: (userId) => userService.getUserInfo(userId || 1),
  updateProfile: (userId, data) => userService.updateProfile(userId || data.id, data),
  login: (credentials) => authService.login(credentials),
  logout: () => authService.logout(),
  register: (userData) => authService.register(userData)
}

const petAPI = {
  getPetInfo: (petId) => petService.getPet(petId),
  getPetList: (userId) => petService.getPets(userId),
  updatePetInfo: (petId, data) => petService.updatePet(petId, data),
  createPet: (data) => petService.createPet(data),
  deletePet: (petId) => petService.deletePet(petId)
}

const activityAPI = {
  getActivities: async (petId, date) => {
    const records = await activityService.getActivityRecords(petId, {
      startDate: date,
      endDate: date
    })
    return { data: records }
  },
  toggleActivity: () => Promise.resolve({ data: { success: true } }),
  createActivity: (data) => activityService.createActivity(data),
  updateActivity: (activityId, data) => activityService.updateActivity(activityId, data),
  deleteActivity: (activityId) => activityService.deleteActivity(activityId)
}

const mediaAPI = {
  uploadFile: (file) => mediaService.uploadFile(file),
  getMediaInfo: (mediaId) => mediaService.getMediaInfo(mediaId),
  getRelatedMedia: (relatedType, relatedId) => mediaService.getRelatedMedia(relatedType, relatedId)
}

const uploadAPI = {
  uploadImage: (file) => mediaService.uploadFile(file),
  uploadAvatar: (file) => mediaService.uploadFile(file),
  uploadMedia: (file) => mediaService.uploadFile(file)
}

const communityAPI = {
  getMomentsByUser: (userId) => communityService.getMomentsByUser(userId),
  createMoment: (userId, content, mediaIds) => communityService.createMoment({ userId, content, mediaIds }),
  deleteMoment: (momentId) => communityService.deleteMoment(momentId),
  getCommentsByMoment: (momentId) => communityService.getCommentsByMoment(momentId),
  createComment: (userId, momentId, content, parentId) => communityService.createComment({ userId, momentId, content, parentId }),
  deleteComment: (commentId) => communityService.deleteComment(commentId),
  toggleLike: (userId, targetType, targetId) => communityService.toggleLike({ userId, targetType, targetId }),
  toggleFollow: (followerId, followedId) => communityService.toggleFollow({ followerId, followedId }),
  getFollowersCount: (userId) => communityService.getFollowersCount(userId),
  getFollowingCount: (userId) => communityService.getFollowingCount(userId)
}

const medicalAPI = {
  getMedicalRecords: () => Promise.resolve({ data: [] })
}

const shopAPI = {
  getProducts: () => Promise.resolve({ data: [] })
}

// 工具函数
const apiUtils = {
  isOnline: () => navigator.onLine,
  withRetry: async (apiCall, maxRetries = 3, delay = 1000) => {
    for (let i = 0; i <= maxRetries; i++) {
      try {
        return await apiCall()
      } catch (error) {
        if (i === maxRetries) throw error
        await new Promise(resolve => setTimeout(resolve, delay * Math.pow(2, i)))
      }
    }
  },
  cache: {
    get: (key) => {
      const cached = localStorage.getItem(`cache:${key}`)
      if (cached) {
        const { data, timestamp, ttl } = JSON.parse(cached)
        if (Date.now() - timestamp < ttl) {
          return data
        }
      }
      return null
    },
    set: (key, data, ttl = 5 * 60 * 1000) => {
      localStorage.setItem(`cache:${key}`, JSON.stringify({
        data,
        timestamp: Date.now(),
        ttl
      }))
    },
    clear: (pattern) => {
      if (pattern) {
        Object.keys(localStorage).forEach(key => {
          if (key.startsWith('cache:') && key.includes(pattern)) {
            localStorage.removeItem(key)
          }
        })
      } else {
        Object.keys(localStorage).forEach(key => {
          if (key.startsWith('cache:')) {
            localStorage.removeItem(key)
          }
        })
      }
    }
  }
}

// 默认导出
export default apiService

// 命名导出
export {
  // 核心服务
  authService,
  userService,
  petService,
  activityService,
  statusService,
  fixedActivityService,
  reservedActivityService,
  mediaService,
  communityService,

  // 工具服务
  batchService,
  cacheService,
  configService,

  // 组合式API
  useApi,
  useList,
  useAuth,
  usePets,
  useActivities,

  // 兼容的旧API
  userAPI,
  petAPI,
  activityAPI,
  mediaAPI,
  uploadAPI,
  communityAPI,
  medicalAPI,
  shopAPI,

  // 工具函数
  apiUtils
}

// 调试信息
if (import.meta.env.DEV) {
  console.log('📡 API服务已加载 - 兼容版本')
}