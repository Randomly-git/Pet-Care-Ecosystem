/**
 * 数据转换层 - 统一处理各微服务的响应格式差异
 * 使用适配器模式转换数据格式，确保前端组件接收统一的数据结构
 */

// 类型定义
export interface User {
  id: number
  username: string
  name?: string
  email?: string
  avatar?: string
  createdAt?: string
}

export interface Pet {
  id: number
  name: string
  species: string
  breed?: string
  birthday?: string
  userId: number
  userName: string
  avatar?: string
  statusRecordCount: number
  activityRecordCount: number
  createdAt: string
}

export interface Activity {
  id: number
  name: string
  kindId: number
  kindName: string
  userId: number
  userName: string
  state: number
  createdAt?: string
}

export interface ActivityRecord {
  id: number
  activityId: number
  activityName: string
  kindId: number
  kindName: string
  petId: number
  description?: string
  date: string
}

export interface Status {
  id: number
  name: string
  state: number
}

export interface StatusRecord {
  id: number
  statusId: number
  statusName: string
  petId: number
  startDate: string
  endDate?: string
  description?: string
}

export interface FixedActivity {
  id: number
  activityId: number
  activityName: string
  kindId: number
  kindName: string
  petId: number
  petName: string
  gapTime: number
  nextReminderDate: string
}

export interface ReservedActivity {
  id: number
  activityId: number
  activityName: string
  petId: number
  reminderDate: string
}

export interface MediaFile {
  id: number
  url: string
  type: string
  fileName: string
  uploadTime: string
  relatedType?: string
  relatedId?: number
}

export interface Moment {
  id: number
  userId: number
  userName: string
  userAvatar?: string
  content: string
  mediaFiles: MediaFile[]
  likeCount: number
  commentCount: number
  isLiked: boolean
  createdAt: string
}

export interface Comment {
  id: number
  userId: number
  userName: string
  userAvatar?: string
  momentId: number
  content: string
  parentId?: number
  replies: Comment[]
  createdAt: string
}

// PetCare服务数据转换器
export class PetcareDataTransformer {
  // 用户数据转换
  static transformUser(data: any): User {
    return {
      id: data.userId || data.id,
      username: data.name || data.username,
      email: data.email,
      avatar: data.avatar,
      createdAt: data.createdAt || data.createTime
    }
  }

  // 宠物数据转换
  static transformPet(data: any): Pet {
    return {
      id: data.petId,
      name: data.name,
      species: data.species,
      breed: data.breed,
      birthday: data.birthday,
      userId: data.userId,
      userName: data.userName,
      statusRecordCount: data.statusRecordCount || 0,
      activityRecordCount: data.activityRecordCount || 0,
      createdAt: data.createdAt
    }
  }

  // 活动数据转换
  static transformActivity(data: any): Activity {
    return {
      id: data.activityId,
      name: data.activityName,
      kindId: data.activityKindId,
      kindName: data.activityKindName,
      userId: data.userId,
      userName: data.userName,
      state: data.state,
      createdAt: data.createdAt
    }
  }

  // 活动记录转换
  static transformActivityRecord(data: any): ActivityRecord {
    return {
      id: data.activityRecordId,
      activityId: data.activityId,
      activityName: data.activityName,
      kindId: data.activityKindId,
      kindName: data.activityKindName,
      petId: data.petId,
      description: data.activityDescription,
      date: data.activityDate
    }
  }

  // 状态转换
  static transformStatus(data: any): Status {
    return {
      id: data.statusId,
      name: data.statusName,
      state: data.state
    }
  }

  // 状态记录转换
  static transformStatusRecord(data: any): StatusRecord {
    return {
      id: data.statusRecordId,
      statusId: data.statusId,
      statusName: data.statusName,
      petId: data.petId,
      startDate: data.startDate,
      endDate: data.endDate,
      description: data.statusDescription
    }
  }

  // 定时活动转换
  static transformFixedActivity(data: any): FixedActivity {
    return {
      id: data.fixedActivityId,
      activityId: data.activityId,
      activityName: data.activityName,
      kindId: data.activityKindId,
      kindName: data.activityKindName,
      petId: data.petId,
      petName: data.petName,
      gapTime: data.gapTime,
      nextReminderDate: data.nextReminderDate
    }
  }

  // 预约活动转换
  static transformReservedActivity(data: any): ReservedActivity {
    return {
      id: data.activityReminderId,
      activityId: data.activityId,
      activityName: data.activityName,
      petId: data.petId,
      reminderDate: data.reminderDate
    }
  }

  // 登录响应转换
  static transformLoginResponse(data: any): { user: User; token: string } {
    return {
      user: this.transformUser(data),
      token: data.token
    }
  }
}

// 媒体服务数据转换器
export class MediaDataTransformer {
  static transformMediaFile(data: any): MediaFile {
    return {
      id: data.mediaId,
      url: data.fileUrl,
      type: data.fileType,
      fileName: data.fileName,
      uploadTime: data.uploadTime,
      relatedType: data.relatedType,
      relatedId: data.relatedId
    }
  }

  static transformMediaList(data: any[]): MediaFile[] {
    return data.map(item => this.transformMediaFile(item))
  }
}

// 社区服务数据转换器
export class CommunityDataTransformer {
  // 动态转换
  static transformMoment(data: any): Moment {
    return {
      id: data.momentId || data.id,
      userId: data.userId,
      userName: data.userName,
      userAvatar: data.userAvatar,
      content: data.content,
      mediaFiles: data.mediaFiles ? MediaDataTransformer.transformMediaList(data.mediaFiles) : [],
      likeCount: data.likeCount || 0,
      commentCount: data.commentCount || 0,
      isLiked: data.isLiked || false,
      createdAt: data.createdAt || data.createTime
    }
  }

  // 评论转换
  static transformComment(data: any): Comment {
    return {
      id: data.commentId || data.id,
      userId: data.userId,
      userName: data.userName,
      userAvatar: data.userAvatar,
      momentId: data.momentId,
      content: data.content,
      parentId: data.parentId,
      replies: data.replies ? data.replies.map((reply: any) => this.transformComment(reply)) : [],
      createdAt: data.createdAt || data.createTime
    }
  }
}

// 通用数据转换管理器
export class DataTransformer {
  // 自动检测并转换数据
  static transform(data: any, type: string): any {
    switch (type) {
      case 'user':
        return PetcareDataTransformer.transformUser(data)
      case 'pet':
        return PetcareDataTransformer.transformPet(data)
      case 'activity':
        return PetcareDataTransformer.transformActivity(data)
      case 'activityRecord':
        return PetcareDataTransformer.transformActivityRecord(data)
      case 'status':
        return PetcareDataTransformer.transformStatus(data)
      case 'statusRecord':
        return PetcareDataTransformer.transformStatusRecord(data)
      case 'fixedActivity':
        return PetcareDataTransformer.transformFixedActivity(data)
      case 'reservedActivity':
        return PetcareDataTransformer.transformReservedActivity(data)
      case 'media':
        return MediaDataTransformer.transformMediaFile(data)
      case 'mediaList':
        return MediaDataTransformer.transformMediaList(data)
      case 'moment':
        return CommunityDataTransformer.transformMoment(data)
      case 'comment':
        return CommunityDataTransformer.transformComment(data)
      case 'loginResponse':
        return PetcareDataTransformer.transformLoginResponse(data)
      default:
        console.warn(`Unknown data type: ${type}`)
        return data
    }
  }

  // 批量转换
  static transformBatch<T>(dataList: any[], type: string): T[] {
    return dataList.map(item => this.transform(item, type))
  }

  // 安全的数据提取
  static safeExtract<T>(response: any, key?: string): T {
    // 处理标准API响应格式
    if (response && typeof response === 'object') {
      if (response.success !== undefined && response.data !== undefined) {
        return response.data as T
      }

      // 直接返回数据
      if (key) {
        return response[key] as T
      }

      return response as T
    }

    return response as T
  }

  // 错误信息提取
  static extractError(error: any): string {
    if (typeof error === 'string') {
      return error
    }

    if (error?.message) {
      return error.message
    }

    if (error?.data?.message) {
      return error.data.message
    }

    if (error?.response?.data?.message) {
      return error.response.data.message
    }

    return '发生未知错误'
  }

  // API响应统一处理
  static handleApiResponse<T>(response: any, dataType?: string): T {
    try {
      const data = this.safeExtract<any>(response)

      if (dataType) {
        return this.transform(data, dataType)
      }

      return data as T
    } catch (error) {
      console.error('Data transformation error:', error)
      throw error
    }
  }
}

// 响应包装器 - 处理不同服务的响应格式
export class ResponseWrapper {
  // 包装成功响应
  static success<T>(data: T, message = 'success'): ResponseData<T> {
    return {
      success: true,
      message,
      data,
      timestamp: new Date().toISOString()
    }
  }

  // 包装失败响应
  static error(message: string, code?: number): ResponseData<null> {
    return {
      success: false,
      message,
      data: null,
      timestamp: new Date().toISOString(),
      code
    }
  }

  // 检查响应是否成功
  static isSuccess(response: any): boolean {
    return response?.success === true || response?.code === 20000
  }

  // 提取消息
  static extractMessage(response: any): string {
    return response?.message || response?.msg || '操作成功'
  }
}

export default DataTransformer