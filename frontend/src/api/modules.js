/**
 * API 模块统一导出
 * 提供所有API模块的统一入口
 */

// 导出各个模块
export * from './auth'
export * from './pets'
export * from './activities'
export * from './status'

// 创建API服务对象，便于集中管理
const apiService = {
  // 认证相关
  auth: {
    register: async (userData) => {
      const { register } = await import('./auth')
      return register(userData)
    },
    login: async (credentials) => {
      const { login } = await import('./auth')
      return login(credentials)
    },
    logout: async () => {
      const { logout } = await import('./auth')
      return logout()
    },
    validateToken: async () => {
      const { validateToken } = await import('./auth')
      return validateToken()
    },
    refreshToken: async () => {
      const { refreshToken } = await import('./auth')
      return refreshToken()
    }
  },

  // 宠物管理
  pets: {
    getAll: async () => {
      const { getAllPets } = await import('./pets')
      return getAllPets()
    },
    getByUserId: async (userId) => {
      const { getUserPets } = await import('./pets')
      return getUserPets(userId)
    },
    getById: async (petId) => {
      const { getPetById } = await import('./pets')
      return getPetById(petId)
    },
    create: async (petData) => {
      const { createPet } = await import('./pets')
      return createPet(petData)
    },
    update: async (petId, petData) => {
      const { updatePet } = await import('./pets')
      return updatePet(petId, petData)
    },
    delete: async (petId) => {
      const { deletePet } = await import('./pets')
      return deletePet(petId)
    },
    getByType: async (type) => {
      const { getPetsByType } = await import('./pets')
      return getPetsByType(type)
    },
    search: async (params) => {
      const { searchPets } = await import('./pets')
      return searchPets(params)
    },
    getStats: async (userId) => {
      const { getPetStats } = await import('./pets')
      return getPetStats(userId)
    }
  },

  // 活动管理
  activities: {
    getAll: async () => {
      const { getAllActivities } = await import('./activities')
      return getAllActivities()
    },
    getById: async (activityId) => {
      const { getActivityById } = await import('./activities')
      return getActivityById(activityId)
    },
    create: async (activityData) => {
      const { createActivity } = await import('./activities')
      return createActivity(activityData)
    },
    update: async (activityData) => {
      const { updateActivity } = await import('./activities')
      return updateActivity(activityData)
    },
    delete: async (activityId) => {
      const { deleteActivity } = await import('./activities')
      return deleteActivity(activityId)
    },
    completeDelete: async (activityId) => {
      const { completeDeleteActivity } = await import('./activities')
      return completeDeleteActivity(activityId)
    },
    getRecords: async (petId, params) => {
      const { getActivityRecords } = await import('./activities')
      return getActivityRecords(petId, params)
    },
    createRecord: async (petId, recordData) => {
      const { createActivityRecord } = await import('./activities')
      return createActivityRecord(petId, recordData)
    },
    updateRecord: async (recordId, recordData) => {
      const { updateActivityRecord } = await import('./activities')
      return updateActivityRecord(recordId, recordData)
    },
    deleteRecord: async (recordId) => {
      const { deleteActivityRecord } = await import('./activities')
      return deleteActivityRecord(recordId)
    },
    getByUserId: async (userId) => {
      const { getActivitiesByUserId } = await import('./activities')
      return getActivitiesByUserId(userId)
    },
    getStats: async (params) => {
      const { getActivityStats } = await import('./activities')
      return getActivityStats(params)
    },
    queryActivityRecords: async (petId, startDate, endDate) => {
      const { queryActivityRecords } = await import('./activities')
      return queryActivityRecords(petId, startDate, endDate)
    }
  },

  // 状态管理
  status: {
    getByUserId: async (userId) => {
      const { getUserStatuses } = await import('./status')
      return getUserStatuses(userId)
    },
    create: async (statusData) => {
      const { createStatus } = await import('./status')
      return createStatus(statusData)
    },
    updateName: async (statusId, newName) => {
      const { updateStatusName } = await import('./status')
      return updateStatusName(statusId, newName)
    },
    delete: async (statusId) => {
      const { deleteStatus } = await import('./status')
      return deleteStatus(statusId)
    },
    deleteWithRecords: async (statusId) => {
      const { deleteStatusWithRecords } = await import('./status')
      return deleteStatusWithRecords(statusId)
    },
    getRecords: async (petId, params) => {
      const { getStatusRecords } = await import('./status')
      return getStatusRecords(petId, params)
    },
    getActiveRecord: async (petId, targetDate) => {
      const { getActiveStatusRecord } = await import('./status')
      return getActiveStatusRecord(petId, targetDate)
    },
    createRecord: async (recordData) => {
      const { createStatusRecord } = await import('./status')
      return createStatusRecord(recordData)
    },
    updateRecord: async (recordId, updateData) => {
      const { updateStatusRecord } = await import('./status')
      return updateStatusRecord(recordId, updateData)
    },
    stopRecord: async (recordId, endDate) => {
      const { stopStatusRecord } = await import('./status')
      return stopStatusRecord(recordId, endDate)
    },
    deleteRecord: async (recordId) => {
      const { deleteStatusRecord } = await import('./status')
      return deleteStatusRecord(recordId)
    },
    getStats: async (params) => {
      const { getStatusStats } = await import('./status')
      return getStatusStats(params)
    }
  }
}

/**
 * 数据验证器集合
 */
export const validators = {
  pet: () => import('./pets').then(m => m.petValidator),
  activity: () => import('./activities').then(m => m.activityValidator),
  status: () => import('./status').then(m => m.statusValidator)
}

/**
 * 批量操作工具
 */
export const batchOperations = {
  /**
   * 批量获取宠物信息
   * @param {Array} petIds - 宠物ID数组
   */
  async getPets(petIds) {
    const { getPetsByUserIds } = await import('./pets')
    // 模拟批量获取，实际可能需要调整API
    const results = []
    for (const petId of petIds) {
      try {
        const { getPetById } = await import('./pets')
        const pet = await getPetById(petId)
        results.push(pet)
      } catch (error) {
        console.error(`获取宠物 ${petId} 失败:`, error)
      }
    }
    return results
  },

  /**
   * 批量获取活动记录
   * @param {Array} petIds - 宠物ID数组
   * @param {Object} params - 搜索参数
   */
  async getActivityRecords(petIds, params = {}) {
    const { getActivityRecordsByPetIds } = await import('./activities')
    return getActivityRecordsByPetIds(petIds, params)
  },

  /**
   * 批量获取状态记录
   * @param {Array} petIds - 宠物ID数组
   * @param {Object} params - 搜索参数
   */
  async getStatusRecords(petIds, params = {}) {
    const { getStatusRecordsByPetIds } = await import('./status')
    return getStatusRecordsByPetIds(petIds, params)
  }
}

/**
 * API调用统计和监控
 */
export const apiMonitor = {
  // 统计信息
  stats: {
    totalCalls: 0,
    successfulCalls: 0,
    failedCalls: 0,
    callHistory: []
  },

  /**
   * 记录API调用
   * @param {string} method - API方法名
   * @param {boolean} success - 是否成功
   * @param {number} duration - 调用时长(ms)
   * @param {Error} error - 错误信息
   */
  recordCall(method, success, duration, error = null) {
    this.stats.totalCalls++
    if (success) {
      this.stats.successfulCalls++
    } else {
      this.stats.failedCalls++
    }

    this.stats.callHistory.push({
      method,
      success,
      duration,
      error: error?.message,
      timestamp: new Date().toISOString()
    })

    // 只保留最近100条记录
    if (this.stats.callHistory.length > 100) {
      this.stats.callHistory = this.stats.callHistory.slice(-100)
    }
  },

  /**
   * 获取统计信息
   */
  getStats() {
    return {
      ...this.stats,
      successRate: this.stats.totalCalls > 0 ?
        (this.stats.successfulCalls / this.stats.totalCalls * 100).toFixed(2) + '%' : '0%'
    }
  },

  /**
   * 清空统计信息
   */
  clearStats() {
    this.stats = {
      totalCalls: 0,
      successfulCalls: 0,
      failedCalls: 0,
      callHistory: []
    }
  }
}

// 创建包装器，自动监控API调用
const createMonitoredApi = (apiObject, prefix = '') => {
  const monitored = {}

  for (const [key, value] of Object.entries(apiObject)) {
    if (typeof value === 'function') {
      monitored[key] = async (...args) => {
        const startTime = Date.now()
        const method = prefix ? `${prefix}.${key}` : key

        try {
          const result = await value(...args)
          const duration = Date.now() - startTime
          apiMonitor.recordCall(method, true, duration)
          return result
        } catch (error) {
          const duration = Date.now() - startTime
          apiMonitor.recordCall(method, false, duration, error)
          throw error
        }
      }
    } else if (typeof value === 'object' && value !== null) {
      monitored[key] = createMonitoredApi(value, prefix ? `${prefix}.${key}` : key)
    } else {
      monitored[key] = value
    }
  }

  return monitored
}

// 导出带监控的API服务
export const monitoredApiService = createMonitoredApi(apiService)

// 默认导出
export default apiService