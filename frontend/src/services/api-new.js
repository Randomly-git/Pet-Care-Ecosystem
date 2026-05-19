/**
 * 新的API服务入口
 * 这个文件将替换原有的 api.js
 * 导出新的API服务，同时保持向后兼容
 */

// 导入新的API服务
import apiService from './index'

// 导出新的服务接口
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

  // 默认导出
  default as api
} from './index'

// 导出组合式API
export {
  useApi,
  useList,
  useAuth,
  usePets,
  useActivities
} from '@/composables/useApi'

// 导出类型定义（TypeScript 类型导出，运行时无影响）
export {
  User,
  Pet,
  Activity,
  ActivityRecord,
  Status,
  StatusRecord,
  FixedActivity,
  ReservedActivity,
  MediaFile,
  Moment,
  Comment
} from './index'

// 为了向后兼容，也导出旧的API名称
export const userAPI = {
  getUserInfo: (userId) => userService.getUserInfo(userId || 1),
  updateProfile: (userId, data) => userService.updateProfile(userId || data.id, data),
  login: (credentials) => authService.login(credentials),
  logout: () => authService.logout(),
  register: (userData) => authService.register(userData)
}

export const petAPI = {
  getPetInfo: (petId) => petService.getPet(petId),
  getPetList: (userId) => petService.getPets(userId),
  updatePetInfo: (petId, data) => petService.updatePet(petId, data),
  createPet: (data) => petService.createPet(data),
  deletePet: (petId) => petService.deletePet(petId)
}

export const activityAPI = {
  getActivities: async (petId, date) => {
    // 兼容旧接口，返回活动记录
    const records = await activityService.getActivityRecords(petId, {
      startDate: date,
      endDate: date
    })
    return { data: records }
  },
  toggleActivity: (activityId) => {
    console.warn('toggleActivity 功能需要实现')
    return Promise.resolve({ data: { success: true } })
  },
  createActivity: (data) => activityService.createActivity(data),
  updateActivity: (activityId, data) => activityService.updateActivity(activityId, data),
  deleteActivity: (activityId) => activityService.deleteActivity(activityId)
}

export const mediaAPI = {
  uploadFile: (file) => mediaService.uploadFile(file),
  getMediaInfo: (mediaId) => mediaService.getMediaInfo(mediaId),
  getRelatedMedia: (relatedType, relatedId) => mediaService.getRelatedMedia(relatedType, relatedId)
}

export const uploadAPI = {
  uploadImage: (file) => mediaService.uploadFile(file),
  uploadAvatar: (file) => mediaService.uploadFile(file),
  uploadMedia: (file) => mediaService.uploadFile(file)
}

export const communityAPI = {
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

// 保留旧的导出接口
export const medicalAPI = {
  getMedicalRecords: (petId) => {
    console.warn('medicalAPI 功能需要实现')
    return Promise.resolve({ data: [] })
  }
}

export const shopAPI = {
  getProducts: (category, page) => {
    console.warn('shopAPI 功能需要实现')
    return Promise.resolve({ data: [] })
  }
}

// 默认导出新的API服务
export default apiService

// 工具函数
export const apiUtils = {
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

// 调试信息
if (import.meta.env.DEV) {
  console.log('📡 API服务已加载 - 新版本')
  console.log('可用的服务:', {
    authService: '✓',
    petService: '✓',
    activityService: '✓',
    mediaService: '✓',
    communityService: '✓'
  })
}