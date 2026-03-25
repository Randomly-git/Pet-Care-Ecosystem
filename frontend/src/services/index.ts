/**
 * 统一的API服务入口
 * 提供所有API服务的统一访问接口
 */

// 导入核心服务
import { petcareApi, mediaApi, communityApi, ApiServiceFactory } from './ApiServiceFactory'
import DataTransformer, { ResponseWrapper } from './DataTransformer'
import httpClient, { MICROSERVICE_CONFIG } from './HttpClient'
import type {
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
} from './DataTransformer'

// 认证服务
export const authService = {
  // 登录
  async login(credentials: { name: string; password: string }) {
    const response = await petcareApi.login(credentials)
    const result = DataTransformer.handleApiResponse(response, 'loginResponse')

    // 存储token
    if (result.token) {
      localStorage.setItem('token', result.token)
      localStorage.setItem('userInfo', JSON.stringify(result.user))
    }

    return result
  },

  // 注册
  async register(userData: { name: string; password: string }) {
    const response = await petcareApi.register(userData)
    return DataTransformer.handleApiResponse(response, 'user')
  },

  // 登出
  async logout() {
    await petcareApi.logout()
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  },

  // 获取当前用户信息
  getCurrentUser(): User | null {
    const userInfo = localStorage.getItem('userInfo')
    return userInfo ? JSON.parse(userInfo) : null
  },

  // 检查是否已登录
  isAuthenticated(): boolean {
    return !!localStorage.getItem('token')
  }
}

// 用户服务
export const userService = {
  // 获取用户信息
  async getUserInfo(userId: number) {
    const response = await petcareApi.getUserInfo(userId)
    return DataTransformer.handleApiResponse(response, 'user')
  },

  // 更新用户资料
  async updateProfile(userId: number, data: Partial<User>) {
    const response = await petcareApi.updateProfile(userId, data)
    return DataTransformer.handleApiResponse(response, 'user')
  }
}

// 宠物服务
export const petService = {
  // 获取宠物列表
  async getPets(userId: number) {
    const response = await petcareApi.getPets(userId)
    return DataTransformer.transformBatch<Pet>(response, 'pet')
  },

  // 获取宠物详情
  async getPet(petId: number) {
    const response = await petcareApi.getPet(petId)
    return DataTransformer.handleApiResponse(response, 'pet')
  },

  // 创建宠物
  async createPet(petData: Omit<Pet, 'id' | 'userName' | 'statusRecordCount' | 'activityRecordCount' | 'createdAt'>) {
    const response = await petcareApi.createPet(petData)
    return DataTransformer.handleApiResponse(response, 'pet')
  },

  // 更新宠物信息
  async updatePet(petId: number, petData: Partial<Pet>) {
    const response = await petcareApi.updatePet(petId, petData)
    return DataTransformer.handleApiResponse(response, 'pet')
  },

  // 删除宠物
  async deletePet(petId: number) {
    await petcareApi.deletePet(petId)
    return true
  }
}

// 活动服务
export const activityService = {
  // 获取活动种类
  async getActivityKinds() {
    const response = await petcareApi.getActivityKinds()
    return DataTransformer.transformBatch<any>(response, 'activity')
  },

  // 获取用户活动
  async getActivities(userId: number, activityKindId?: number) {
    const response = await petcareApi.getActivities(userId, activityKindId)
    return DataTransformer.transformBatch<Activity>(response, 'activity')
  },

  // 获取活动详情
  async getActivity(activityId: number) {
    const response = await petcareApi.getActivity(activityId)
    return DataTransformer.handleApiResponse(response, 'activity')
  },

  // 创建活动
  async createActivity(activityData: Omit<Activity, 'id' | 'userName' | 'state' | 'createdAt'>) {
    const response = await petcareApi.createActivity(activityData)
    return DataTransformer.handleApiResponse(response, 'activity')
  },

  // 更新活动
  async updateActivity(activityId: number, activityData: Partial<Activity>) {
    const response = await petcareApi.updateActivity(activityId, activityData)
    return DataTransformer.handleApiResponse(response, 'activity')
  },

  // 删除活动
  async deleteActivity(activityId: number) {
    await petcareApi.deleteActivity(activityId)
    return true
  },

  // 获取活动记录
  async getActivityRecords(petId: number, params?: {
    startDate?: string
    endDate?: string
    activityKindId?: number
  }) {
    const response = await petcareApi.getActivityRecords(petId, params)
    return DataTransformer.transformBatch<ActivityRecord>(response, 'activityRecord')
  },

  // 创建活动记录
  async createActivityRecord(petId: number, params: {
    activityId: number
    description?: string
    date: string
  }) {
    const response = await petcareApi.createActivityRecord(petId, params)
    return DataTransformer.handleApiResponse(response, 'activityRecord')
  },

  // 更新活动记录
  async updateActivityRecord(recordId: number, params: {
    newActivityId?: number
    description?: string
    date?: string
  }) {
    const response = await petcareApi.updateActivityRecord(recordId, params)
    return DataTransformer.handleApiResponse(response, 'activityRecord')
  },

  // 删除活动记录
  async deleteActivityRecord(recordId: number) {
    await petcareApi.deleteActivityRecord(recordId)
    return true
  }
}

// 状态服务
export const statusService = {
  // 获取状态列表（根据宠物ID）
  async getStatuses(petId: number) {
    const response = await petcareApi.getStatuses(petId)
    return DataTransformer.transformBatch<Status>(response, 'status')
  },

  // 获取状态记录
  async getStatusRecords(petId: number) {
    const response = await petcareApi.getStatusRecords(petId)
    return DataTransformer.transformBatch<StatusRecord>(response, 'statusRecord')
  },

  // 获取活跃状态记录
  async getActiveStatusRecords(petId: number, targetDate: string) {
    const response = await petcareApi.getActiveStatusRecords(petId, targetDate)
    return DataTransformer.transformBatch<StatusRecord>(response, 'statusRecord')
  },

  // 创建状态（为宠物创建）
  async createStatus(petId: number, statusName: string) {
    const response = await petcareApi.createStatus(petId, statusName)
    return DataTransformer.handleApiResponse(response, 'status')
  },

  // 更新状态名称
  async updateStatusName(statusId: number, newName: string) {
    await petcareApi.updateStatusName(statusId, newName)
    return true
  },

  // 删除状态
  async deleteStatus(statusId: number) {
    await petcareApi.deleteStatus(statusId)
    return true
  }
}

// 定时活动服务
export const fixedActivityService = {
  // 创建定时活动
  async createFixedActivity(data: { petId: number; activityId: number; gapTime: number }) {
    const response = await petcareApi.createFixedActivity(data)
    return DataTransformer.handleApiResponse(response, 'fixedActivity')
  },

  // 获取定时活动列表
  async getFixedActivities(petId: number) {
    const response = await petcareApi.getFixedActivities(petId)
    return DataTransformer.transformBatch<FixedActivity>(response, 'fixedActivity')
  },

  // 更新定时活动
  async updateFixedActivity(fixedActivityId: number, gapTime: number) {
    const response = await petcareApi.updateFixedActivity(fixedActivityId, gapTime)
    return DataTransformer.handleApiResponse(response, 'fixedActivity')
  },

  // 删除定时活动
  async deleteFixedActivity(fixedActivityId: number) {
    await petcareApi.deleteFixedActivity(fixedActivityId)
    return true
  }
}

// 预约活动服务
export const reservedActivityService = {
  // 创建预约活动
  async createReservedActivity(data: { activityId: number; petId: number; reminderDate: string }) {
    const response = await petcareApi.createReservedActivity(data)
    return DataTransformer.handleApiResponse(response, 'reservedActivity')
  },

  // 获取预约活动列表
  async getReservedActivities(petId: number) {
    const response = await petcareApi.getReservedActivities(petId)
    return DataTransformer.transformBatch<ReservedActivity>(response, 'reservedActivity')
  },

  // 更新预约活动日期
  async updateReservedActivityDate(activityReminderId: number, reminderDate: string) {
    const response = await petcareApi.updateReservedActivityDate(activityReminderId, reminderDate)
    return DataTransformer.handleApiResponse(response, 'reservedActivity')
  },

  // 删除预约活动
  async deleteReservedActivity(activityReminderId: number) {
    await petcareApi.deleteReservedActivity(activityReminderId)
    return true
  }
}

// 媒体服务
export const mediaService = {
  // 上传文件
  async uploadFile(file: File) {
    const response = await mediaApi.uploadFile(file)
    return DataTransformer.handleApiResponse(response, 'media')
  },

  // 获取媒体信息
  async getMediaInfo(mediaId: number) {
    const response = await mediaApi.getMediaInfo(mediaId)
    return DataTransformer.handleApiResponse(response, 'media')
  },

  // 获取关联的媒体文件
  async getRelatedMedia(relatedType: string, relatedId: number) {
    const response = await mediaApi.getRelatedMedia(relatedType, relatedId)
    return DataTransformer.transformBatch<MediaFile>(response, 'media')
  },

  // 批量更新媒体关联
  async batchUpdateMediaRelation(data: {
    mediaIds: number[]
    relatedType: string
    newRelatedId: number
  }) {
    await mediaApi.batchUpdateMediaRelation(data)
    return true
  },

  // 删除关联的媒体文件
  async deleteRelatedMedia(relatedType: string, relatedId: number) {
    await mediaApi.deleteRelatedMedia(relatedType, relatedId)
    return true
  }
}

// 社区服务
export const communityService = {
  // 创建动态
  async createMoment(data: { userId: number; content: string; mediaIds?: number[] }) {
    const response = await communityApi.createMoment(data)
    return DataTransformer.handleApiResponse(response, 'moment')
  },

  // 获取用户动态
  async getMomentsByUser(userId: number) {
    const response = await communityApi.getMomentsByUser(userId)
    return DataTransformer.transformBatch<Moment>(response, 'moment')
  },

  // 删除动态（需要 userId 进行权限验证和冷库清理）
  async deleteMoment(momentId: number, userId: number) {
    const response = await communityApi.deleteMoment(momentId, userId)
    return response
  },

  // 创建评论
  async createComment(data: {
    userId: number
    momentId: number
    content: string
    parentId?: number
  }) {
    const response = await communityApi.createComment(data)
    return DataTransformer.handleApiResponse(response, 'comment')
  },

  // 获取动态评论
  async getCommentsByMoment(momentId: number) {
    const response = await communityApi.getCommentsByMoment(momentId)
    return DataTransformer.transformBatch<Comment>(response, 'comment')
  },

  // 删除评论
  async deleteComment(commentId: number) {
    await communityApi.deleteComment(commentId)
    return true
  },

  // 点赞/取消点赞
  async toggleLike(data: { userId: number; targetType: string; targetId: number }) {
    await communityApi.toggleLike(data)
    return true
  },

  // 关注/取消关注
  async toggleFollow(data: { followerId: number; followedId: number }) {
    await communityApi.toggleFollow(data)
    return true
  },

  // 获取粉丝数
  async getFollowersCount(userId: number): Promise<number> {
    return await communityApi.getFollowersCount(userId)
  },

  // 获取关注数
  async getFollowingCount(userId: number): Promise<number> {
    return await communityApi.getFollowingCount(userId)
  }
}

// 批量操作服务
export const batchService = {
  // 批量执行请求
  async executeRequests<T>(requests: Array<() => Promise<T>>) {
    return await ApiServiceFactory.batchRequests(requests)
  },

  // 批量上传文件
  async uploadFiles(files: File[]) {
    const requests = files.map(file => () => mediaService.uploadFile(file))
    return await this.executeRequests(requests)
  },

  // 批量获取宠物信息
  async getPetsBatch(petIds: number[]) {
    const requests = petIds.map(id => () => petService.getPet(id))
    return await this.executeRequests(requests)
  }
}

// 缓存管理服务
export const cacheService = {
  // 清除所有缓存
  clearAll() {
    ApiServiceFactory.clearCache()
  },

  // 清除指定服务缓存
  clearService(service: 'petcare' | 'media' | 'community') {
    ApiServiceFactory.clearCache(service)
  },

  // 清除指定模式缓存
  clearPattern(pattern: string) {
    httpClient.clearCache(pattern)
  },

  // 获取缓存统计
  getStats() {
    return ApiServiceFactory.getCacheStats()
  }
}

// 请求配置服务
export const configService = {
  // 获取微服务配置
  getMicroserviceConfig() {
    return MICROSERVICE_CONFIG
  },

  // 设置日志级别
  setLogLevel(level: 'debug' | 'info' | 'warn' | 'error') {
    httpClient.setLogLevel(level)
  },

  // 设置请求超时
  setTimeout(timeout: number) {
    // 这里可以通过HttpClient实例设置
    console.log('Setting timeout to', timeout)
  }
}

// 导出所有服务
export default {
  auth: authService,
  user: userService,
  pet: petService,
  activity: activityService,
  status: statusService,
  fixedActivity: fixedActivityService,
  reservedActivity: reservedActivityService,
  media: mediaService,
  community: communityService,
  batch: batchService,
  cache: cacheService,
  config: configService
}

// 导出类型
export type {
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
}