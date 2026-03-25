/**
 * API服务工厂 - 使用工厂模式管理各微服务的API
 * 统一管理所有API端点，提供类型安全的接口
 */

import httpClient, { MICROSERVICE_CONFIG } from './HttpClient'
import type { ResponseData } from './HttpClient'

// 定义各微服务的API端点
const API_ENDPOINTS = {
  // PetCare微服务 (8082)
  petcare: {
    auth: {
      login: '/api/auth/login',
      register: '/api/auth/register',
      logout: '/api/auth/logout'
    },
    user: {
      info: '/api/user/info',
      profile: '/api/user/profile'
    },
    pets: {
      list: '/api/pets/user',
      detail: '/api/pets',
      create: '/api/pets'
    },
    activities: {
      kinds: '/api/activities/kinds',
      list: '/api/activities/user',
      detail: '/api/activities',
      create: '/api/activities',
      records: '/api/activities/records'
    },
    status: {
      list: '/api/status/pet',
      records: '/api/status/records',
      active: '/api/status/records/active'
    },
    fixedActivities: {
      create: '/api/fixed-activities',
      list: '/api/fixed-activities/pet',
      update: '/api/fixed-activities',
      delete: '/api/fixed-activities'
    },
    reservedActivities: {
      create: '/api/reserved-activities',
      list: '/api/reserved-activities/pet',
      updateDate: '/api/reserved-activities/date',
      delete: '/api/reserved-activities'
    }
  },

  // 媒体微服务 (8081)
  media: {
    upload: '/api/v1/media/upload',
    info: '/api/v1/media',
    related: '/api/v1/media/related',
    batchUpdate: '/api/v1/media/related/batch'
  },

  // 社区微服务 (8083)
  community: {
    moments: {
      create: '/api/v1/moments',
      user: '/api/v1/moments/user',
      delete: '/api/v1/moments'
    },
    comments: {
      create: '/api/v1/comments',
      moment: '/api/v1/comments/moment',
      delete: '/api/v1/comments'
    },
    likes: {
      toggle: '/api/v1/likes'
    },
    follows: {
      toggle: '/api/v1/follows',
      followersCount: '/api/v1/follows/followers/count',
      followingCount: '/api/v1/follows/following/count'
    }
  }
} as const

// 基础API服务类
abstract class BaseApiService {
  protected baseURL: string

  constructor(baseURL: string) {
    this.baseURL = baseURL
  }

  protected async request<T = any>(
    endpoint: string,
    options?: {
      method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
      params?: Record<string, any>
      data?: any
      cache?: boolean
      cacheTime?: number
      silent?: boolean
    }
  ): Promise<T> {
    const config = {
      baseURL: this.baseURL,
      url: endpoint,
      method: options?.method || 'GET',
      params: options?.params,
      data: options?.data,
      cache: options?.cache,
      cacheTime: options?.cacheTime,
      silent: options?.silent
    }

    const response = await httpClient.request<T>(config)

    // 处理不同的响应格式
    if (response.success !== undefined) {
      if (!response.success) {
        throw new Error(response.message || '请求失败')
      }
      return response.data as T
    }

    return response as T
  }

  protected get<T = any>(endpoint: string, params?: Record<string, any>, options?: Omit<Parameters<typeof this.request>[2], 'method'>): Promise<T> {
    return this.request<T>(endpoint, { ...options, method: 'GET', params })
  }

  protected post<T = any>(endpoint: string, data?: any, options?: Omit<Parameters<typeof this.request>[2], 'method' | 'data'>): Promise<T> {
    return this.request<T>(endpoint, { ...options, method: 'POST', data })
  }

  protected put<T = any>(endpoint: string, data?: any, options?: Omit<Parameters<typeof this.request>[2], 'method' | 'data'>): Promise<T> {
    return this.request<T>(endpoint, { ...options, method: 'PUT', data })
  }

  protected delete<T = any>(endpoint: string, params?: Record<string, any>, options?: Omit<Parameters<typeof this.request>[2], 'method'>): Promise<T> {
    return this.request<T>(endpoint, { ...options, method: 'DELETE', params })
  }
}

// PetCare API服务
class PetcareApiService extends BaseApiService {
  constructor() {
    super(MICROSERVICE_CONFIG.PETCARE)
  }

  // 认证相关
  async login(credentials: { name: string; password: string }) {
    return this.post<any>(API_ENDPOINTS.petcare.auth.login, credentials)
  }

  async register(userData: { name: string; password: string }) {
    return this.post<any>(API_ENDPOINTS.petcare.auth.register, userData)
  }

  async logout() {
    return this.post(API_ENDPOINTS.petcare.auth.logout)
  }

  // 用户相关
  async getUserInfo(userId: number) {
    return this.get<any>(`${API_ENDPOINTS.petcare.user.info}/${userId}`)
  }

  async updateProfile(userId: number, data: any) {
    return this.put(`${API_ENDPOINTS.petcare.user.profile}/${userId}`, data)
  }

  // 宠物相关
  async getPets(userId: number) {
    return this.get<any[]>(`${API_ENDPOINTS.petcare.pets.list}/${userId}`)
  }

  async getPet(petId: number) {
    return this.get<any>(`${API_ENDPOINTS.petcare.pets.detail}/${petId}`)
  }

  async createPet(petData: any) {
    return this.post<any>(API_ENDPOINTS.petcare.pets.create, petData)
  }

  async updatePet(petId: number, petData: any) {
    return this.put(`${API_ENDPOINTS.petcare.pets.detail}/${petId}`, petData)
  }

  async deletePet(petId: number) {
    return this.delete(`${API_ENDPOINTS.petcare.pets.detail}/${petId}`)
  }

  // 活动相关
  async getActivityKinds() {
    return this.get<any[]>(API_ENDPOINTS.petcare.activities.kinds, undefined, { cache: true, cacheTime: 24 * 60 * 60 * 1000 }) // 缓存1天
  }

  async getActivities(userId: number, activityKindId?: number) {
    const params = activityKindId ? { activityKindId } : undefined
    return this.get<any[]>(`${API_ENDPOINTS.petcare.activities.list}/${userId}`, params)
  }

  async getActivity(activityId: number) {
    return this.get<any>(`${API_ENDPOINTS.petcare.activities.detail}/${activityId}`)
  }

  async createActivity(activityData: any) {
    return this.post<any>(API_ENDPOINTS.petcare.activities.create, activityData)
  }

  async updateActivity(activityId: number, activityData: any) {
    return this.put(`${API_ENDPOINTS.petcare.activities.detail}/${activityId}`, activityData)
  }

  async deleteActivity(activityId: number) {
    return this.delete(`${API_ENDPOINTS.petcare.activities.detail}/${activityId}`)
  }

  // 活动记录相关
  async getActivityRecords(petId: number, params?: {
    startDate?: string
    endDate?: string
    activityKindId?: number
  }) {
    return this.get<any[]>(`${API_ENDPOINTS.petcare.activities.records}/pet/${petId}`, params)
  }

  async createActivityRecord(petId: number, params: {
    activityId: number
    description?: string
    date: string
  }) {
    return this.post<any>(`${API_ENDPOINTS.petcare.activities.records}/pet/${petId}`, undefined, { params })
  }

  async updateActivityRecord(recordId: number, params: {
    newActivityId?: number
    description?: string
    date?: string
  }) {
    return this.put(`${API_ENDPOINTS.petcare.activities.records}/${recordId}`, undefined, { params })
  }

  async deleteActivityRecord(recordId: number) {
    return this.delete(`${API_ENDPOINTS.petcare.activities.records}/${recordId}`)
  }

  // 状态相关
  async getStatuses(petId: number) {
    return this.get<any[]>(`${API_ENDPOINTS.petcare.status.list}/${petId}`, undefined, { cache: true, cacheTime: 30 * 60 * 1000 }) // 缓存30分钟
  }

  async getStatusRecords(petId: number) {
    return this.get<any[]>(`${API_ENDPOINTS.petcare.status.records}/pet/${petId}`)
  }

  async getActiveStatusRecords(petId: number, targetDate: string) {
    return this.get<any[]>(API_ENDPOINTS.petcare.status.active, { petId, targetDate })
  }

  async createStatus(petId: number, statusName: string) {
    return this.post<any>(API_ENDPOINTS.petcare.status.list, null, { params: { petId, statusName } })
  }

  async updateStatusName(statusId: number, newName: string) {
    return this.put(`${API_ENDPOINTS.petcare.status.list}/${statusId}/name`, undefined, { params: { newName } })
  }

  async deleteStatus(statusId: number) {
    return this.delete(`${API_ENDPOINTS.petcare.status.list}/${statusId}`)
  }

  // 定时活动相关
  async createFixedActivity(data: { petId: number; activityId: number; gapTime: number }) {
    return this.post<any>(API_ENDPOINTS.petcare.fixedActivities.create, data)
  }

  async getFixedActivities(petId: number) {
    return this.get<any[]>(`${API_ENDPOINTS.petcare.fixedActivities.list}/${petId}`)
  }

  async updateFixedActivity(fixedActivityId: number, gapTime: number) {
    return this.put(`${API_ENDPOINTS.petcare.fixedActivities.update}/${fixedActivityId}`, { gapTime })
  }

  async deleteFixedActivity(fixedActivityId: number) {
    return this.delete(`${API_ENDPOINTS.petcare.fixedActivities.delete}/${fixedActivityId}`)
  }

  // 预约活动相关
  async createReservedActivity(data: { activityId: number; petId: number; reminderDate: string }) {
    return this.post<any>(API_ENDPOINTS.petcare.reservedActivities.create, data)
  }

  async getReservedActivities(petId: number) {
    return this.get<any[]>(`${API_ENDPOINTS.petcare.reservedActivities.list}/${petId}`)
  }

  async updateReservedActivityDate(activityReminderId: number, reminderDate: string) {
    return this.put(API_ENDPOINTS.petcare.reservedActivities.updateDate, { activityReminderId, reminderDate })
  }

  async deleteReservedActivity(activityReminderId: number) {
    return this.delete(`${API_ENDPOINTS.petcare.reservedActivities.delete}/${activityReminderId}`)
  }
}

// 媒体API服务
class MediaApiService extends BaseApiService {
  constructor() {
    super(MICROSERVICE_CONFIG.MEDIA)
  }

  async uploadFile(file: File, onProgress?: (progress: number) => void) {
    const formData = new FormData()
    formData.append('file', file)

    // 注意：由于使用fetch，不支持进度回调。如需进度，需要使用axios
    return this.post<any>(API_ENDPOINTS.media.upload, formData)
  }

  async getMediaInfo(mediaId: number) {
    return this.get<any>(`${API_ENDPOINTS.media.info}/${mediaId}`)
  }

  async getRelatedMedia(relatedType: string, relatedId: number) {
    return this.get<any[]>(`${API_ENDPOINTS.media.related}/${relatedType}/${relatedId}`)
  }

  async batchUpdateMediaRelation(data: {
    mediaIds: number[]
    relatedType: string
    newRelatedId: number
  }) {
    return this.put(API_ENDPOINTS.media.batchUpdate, data)
  }

  async deleteRelatedMedia(relatedType: string, relatedId: number) {
    return this.delete(`${API_ENDPOINTS.media.related}/${relatedType}/${relatedId}`)
  }
}

// 社区API服务
class CommunityApiService extends BaseApiService {
  constructor() {
    super(MICROSERVICE_CONFIG.COMMUNITY)
  }

  // 动态相关
  async createMoment(data: { userId: number; content: string; mediaIds?: number[] }) {
    return this.post<any>(API_ENDPOINTS.community.moments.create, data)
  }

  async getMomentsByUser(userId: number) {
    return this.get<any[]>(`${API_ENDPOINTS.community.moments.user}/${userId}`)
  }

  async deleteMoment(momentId: number, userId: number) {
    return this.delete(`${API_ENDPOINTS.community.moments.delete}/${momentId}`, { userId })
  }

  // 评论相关
  async createComment(data: {
    userId: number
    momentId: number
    content: string
    parentId?: number
  }) {
    return this.post<any>(API_ENDPOINTS.community.comments.create, data)
  }

  async getCommentsByMoment(momentId: number) {
    return this.get<any[]>(`${API_ENDPOINTS.community.comments.moment}/${momentId}`)
  }

  async deleteComment(commentId: number) {
    return this.delete(`${API_ENDPOINTS.community.comments.delete}/${commentId}`)
  }

  // 点赞相关
  async toggleLike(data: { userId: number; targetType: string; targetId: number }) {
    return this.post(API_ENDPOINTS.community.likes.toggle, data)
  }

  // 关注相关
  async toggleFollow(data: { followerId: number; followedId: number }) {
    return this.post(API_ENDPOINTS.community.follows.toggle, data)
  }

  async getFollowersCount(userId: number): Promise<number> {
    const result = await this.get<string>(`${API_ENDPOINTS.community.follows.followersCount}/${userId}`)
    return parseInt(result)
  }

  async getFollowingCount(userId: number): Promise<number> {
    const result = await this.get<string>(`${API_ENDPOINTS.community.follows.followingCount}/${userId}`)
    return parseInt(result)
  }
}

// API服务工厂
class ApiServiceFactory {
  private static petcareService: PetcareApiService
  private static mediaService: MediaApiService
  private static communityService: CommunityApiService

  // 单例模式获取服务实例
  static getPetcareService(): PetcareApiService {
    if (!this.petcareService) {
      this.petcareService = new PetcareApiService()
    }
    return this.petcareService
  }

  static getMediaService(): MediaApiService {
    if (!this.mediaService) {
      this.mediaService = new MediaApiService()
    }
    return this.mediaService
  }

  static getCommunityService(): CommunityApiService {
    if (!this.communityService) {
      this.communityService = new CommunityApiService()
    }
    return this.communityService
  }

  // 批量操作
  static async batchRequests<T>(requests: Array<() => Promise<T>>) {
    return httpClient.batch(requests)
  }

  // 清除缓存
  static clearCache(service?: 'petcare' | 'media' | 'community') {
    if (service) {
      httpClient.clearCache(service)
    } else {
      httpClient.clearCache()
    }
  }

  // 获取缓存统计
  static getCacheStats() {
    return httpClient.getCacheStats()
  }
}

// 导出API服务
export const petcareApi = ApiServiceFactory.getPetcareService()
export const mediaApi = ApiServiceFactory.getMediaService()
export const communityApi = ApiServiceFactory.getCommunityService()

// 导出工厂类和类型
export { ApiServiceFactory, BaseApiService }
export default ApiServiceFactory