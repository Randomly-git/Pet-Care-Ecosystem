/**
 * 活动管理 API 接口
 */

import request from './index'

/**
 * 获取所有活动
 * @returns {Promise} 所有活动列表响应
 */
export const getAllActivities = async () => {
  try {
    const response = await request({
      url: '/activities',
      method: 'GET'
    })
    return response
  } catch (error) {
    console.error('获取所有活动失败:', error)
    throw error
  }
}

/**
 * 获取活动详情
 * @param {number} activityId - 活动ID
 * @returns {Promise} 活动详情响应
 */
export const getActivityById = async (activityId) => {
  try {
    if (!activityId) {
      throw new Error('活动ID不能为空')
    }

    const response = await request({
      url: `/activities/${activityId}`,
      method: 'GET'
    })
    return response
  } catch (error) {
    console.error('获取活动详情失败:', error)
    throw error
  }
}

/**
 * 创建新活动
 * @param {Object} activityData - 活动数据
 * @param {string} activityData.activityName - 活动名称
 * @param {number} activityData.activityKindId - 活动种类ID
 * @param {number} activityData.userId - 用户ID
 * @returns {Promise} 创建结果响应
 */
export const createActivity = async (activityData) => {
  try {
    // 验证必需字段
    const requiredFields = ['activityName', 'activityKindId', 'userId']
    const missingFields = requiredFields.filter(field => !activityData[field])

    if (missingFields.length > 0) {
      throw new Error(`缺少必需字段: ${missingFields.join(', ')}`)
    }

    // 验证活动名称长度
    if (activityData.activityName.length > 100) {
      throw new Error('活动名称长度不能超过100个字符')
    }

    // 验证用户ID
    if (typeof activityData.userId !== 'number' || activityData.userId <= 0) {
      throw new Error('用户ID必须是正整数')
    }

    const response = await request({
      url: '/activities',
      method: 'POST',
      data: activityData
    })
    return response
  } catch (error) {
    console.error('创建活动失败:', error)
    throw error
  }
}

/**
 * 更新活动信息
 * @param {Object} activityData - 更新的活动数据
 * @param {number} activityData.activityId - 活动ID
 * @param {string} [activityData.activityName] - 新的活动名称
 * @param {number} [activityData.activityKindId] - 新的活动种类ID
 * @returns {Promise} 更新结果响应
 */
export const updateActivity = async (activityData) => {
  try {
    if (!activityData.activityId) {
      throw new Error('活动ID不能为空')
    }

    // 验证活动名称长度（如果提供）
    if (activityData.activityName && activityData.activityName.length > 100) {
      throw new Error('活动名称长度不能超过100个字符')
    }

    const response = await request({
      url: '/activities',
      method: 'PUT',
      data: activityData
    })
    return response
  } catch (error) {
    console.error('更新活动失败:', error)
    throw error
  }
}

/**
 * 软删除活动
 * @param {number} activityId - 活动ID
 * @returns {Promise} 删除结果响应
 */
export const deleteActivity = async (activityId) => {
  try {
    if (!activityId) {
      throw new Error('活动ID不能为空')
    }

    const response = await request({
      url: `/activities/${activityId}`,
      method: 'DELETE'
    })
    return response
  } catch (error) {
    console.error('删除活动失败:', error)
    throw error
  }
}

/**
 * 彻底删除活动
 * @param {number} activityId - 活动ID
 * @returns {Promise} 删除结果响应
 */
export const completeDeleteActivity = async (activityId) => {
  try {
    if (!activityId) {
      throw new Error('活动ID不能为空')
    }

    const response = await request({
      url: `/activities/${activityId}/complete`,
      method: 'DELETE'
    })
    return response
  } catch (error) {
    console.error('彻底删除活动失败:', error)
    throw error
  }
}

/**
 * 搜索活动记录
 * @param {number} petId - 宠物ID
 * @param {Object} searchParams - 搜索参数
 * @param {string} [searchParams.startDate] - 开始时间 yyyy-MM-dd'T'HH:mm:ss
 * @param {string} [searchParams.endDate] - 结束时间 yyyy-MM-dd'T'HH:mm:ss
 * @param {number} [searchParams.activityKindId] - 活动种类ID
 * @returns {Promise} 活动记录列表响应
 */
export const getActivityRecords = async (petId, searchParams = {}) => {
  try {
    if (!petId) {
      throw new Error('宠物ID不能为空')
    }

    const response = await request({
      url: `/activities/records/pet/${petId}`,
      method: 'GET',
      params: searchParams
    })
    return response
  } catch (error) {
    console.error('获取活动记录失败:', error)
    throw error
  }
}

/**
 * 创建活动记录
 * @param {number} petId - 宠物ID
 * @param {Object} recordData - 活动记录数据
 * @param {number} recordData.activityId - 活动ID
 * @param {number} recordData.userId - 用户ID
 * @param {string} [recordData.description] - 活动描述（可选）
 * @param {string} [recordData.date] - 活动日期 yyyy-MM-dd'T'HH:mm:ss（可选）
 * @param {File} [recordData.file] - 上传的文件（可选）
 * @returns {Promise} 创建结果响应
 */
export const createActivityRecord = async (petId, recordData) => {
  try {
    if (!petId) {
      throw new Error('宠物ID不能为空')
    }

    // 验证必需字段
    const requiredFields = ['activityId', 'userId']
    const missingFields = requiredFields.filter(field => !recordData[field])

    if (missingFields.length > 0) {
      throw new Error(`缺少必需字段: ${missingFields.join(', ')}`)
    }

    // 如果提供了日期，验证格式
    if (recordData.date) {
      const datePattern = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/
      if (!datePattern.test(recordData.date)) {
        throw new Error('日期格式不正确，请使用 yyyy-MM-dd\'T\'HH:mm:ss 格式')
      }
    }

    // 构建FormData
    const formData = new FormData()
    formData.append('activityId', recordData.activityId)
    formData.append('userId', recordData.userId)
    
    // 可选字段
    if (recordData.description) {
      formData.append('description', recordData.description)
    }
    
    if (recordData.date) {
      formData.append('date', recordData.date)
    }
    
    if (recordData.file) {
      formData.append('file', recordData.file)
    }

    const response = await request({
      url: `/activities/records/pet/${petId}`,
      method: 'POST',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    
    return response
  } catch (error) {
    console.error('创建活动记录失败:', error)
    throw error
  }
}

/**
 * 根据活动种类ID创建活动记录
 * @param {number} petId - 宠物ID
 * @param {Object} recordData - 活动记录数据
 * @param {number} recordData.activityKindId - 活动种类ID
 * @param {string} recordData.description - 活动描述
 * @param {string} recordData.date - 活动日期 yyyy-MM-dd'T'HH:mm:ss
 * @returns {Promise} 创建结果响应
 */
export const createActivityRecordByKind = async (petId, recordData) => {
  try {
    if (!petId) {
      throw new Error('宠物ID不能为空')
    }

    // 验证必需字段
    const requiredFields = ['activityKindId', 'description', 'date']
    const missingFields = requiredFields.filter(field => !recordData[field])

    if (missingFields.length > 0) {
      throw new Error(`缺少必需字段: ${missingFields.join(', ')}`)
    }

    // 验证日期格式
    const datePattern = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/
    if (!datePattern.test(recordData.date)) {
      throw new Error('日期格式不正确，请使用 yyyy-MM-dd\'T\'HH:mm:ss 格式')
    }

    const response = await request({
      url: `/activities/records/pet/${petId}/by-kind`,
      method: 'POST',
      data: recordData
    })
    return response
  } catch (error) {
    console.error('根据活动种类ID创建活动记录失败:', error)
    throw error
  }
}

/**
 * 更新活动记录
 * @param {number} recordId - 活动记录ID
 * @param {Object} recordData - 更新的记录数据
 * @param {number} recordData.newActivityId - 新的活动ID
 * @param {string} recordData.description - 新的活动描述
 * @param {string} recordData.date - 新的活动日期 yyyy-MM-dd'T'HH:mm:ss
 * @returns {Promise} 更新结果响应
 */
export const updateActivityRecord = async (recordId, recordData) => {
  try {
    if (!recordId) {
      throw new Error('活动记录ID不能为空')
    }

    // 验证必需字段
    const requiredFields = ['newActivityId', 'description', 'date']
    const missingFields = requiredFields.filter(field => !recordData[field])

    if (missingFields.length > 0) {
      throw new Error(`缺少必需字段: ${missingFields.join(', ')}`)
    }

    // 验证日期格式
    const datePattern = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/
    if (!datePattern.test(recordData.date)) {
      throw new Error('日期格式不正确，请使用 yyyy-MM-dd\'T\'HH:mm:ss 格式')
    }

    const response = await request({
      url: `/activities/records/${recordId}`,
      method: 'PUT',
      params: recordData
    })
    return response
  } catch (error) {
    console.error('更新活动记录失败:', error)
    throw error
  }
}

/**
 * 删除活动记录
 * @param {number} recordId - 活动记录ID
 * @returns {Promise} 删除结果响应
 */
export const deleteActivityRecord = async (recordId) => {
  try {
    if (!recordId) {
      throw new Error('活动记录ID不能为空')
    }

    const response = await request({
      url: `/activities/records/${recordId}`,
      method: 'DELETE'
    })
    return response
  } catch (error) {
    console.error('删除活动记录失败:', error)
    throw error
  }
}

/**
 * 根据用户ID获取活动
 * @param {number} userId - 用户ID
 * @returns {Promise} 用户活动列表响应
 */
export const getActivitiesByUserId = async (userId) => {
  try {
    if (!userId) {
      throw new Error('用户ID不能为空')
    }

    const response = await request({
      url: `/activities/user/${userId}`,
      method: 'GET'
    })
    return response
  } catch (error) {
    console.error('获取用户活动失败:', error)
    throw error
  }
}

/**
 * 批量获取活动记录
 * @param {Array} petIds - 宠物ID数组
 * @param {Object} searchParams - 搜索参数
 * @returns {Promise} 批量活动记录响应
 */
export const getActivityRecordsByPetIds = async (petIds, searchParams = {}) => {
  try {
    if (!Array.isArray(petIds) || petIds.length === 0) {
      throw new Error('宠物ID数组不能为空')
    }

    // 由于 /batch 端点有问题，改用并行调用单个宠物端点
    const promises = petIds.map(petId =>
      request({
        url: `/activities/records/pet/${petId}`,
        method: 'GET',
        params: searchParams
      })
    )

    const responses = await Promise.all(promises)

    // 合并所有响应的数据
    let allRecords = []
    responses.forEach(response => {
      if (Array.isArray(response)) {
        allRecords = allRecords.concat(response)
      } else if (response && Array.isArray(response.data)) {
        allRecords = allRecords.concat(response.data)
      }
    })

    return allRecords
  } catch (error) {
    console.error('批量获取活动记录失败:', error)
    throw error
  }
}

/**
 * 获取活动统计信息
 * @param {number} userId - 用户ID（可选）
 * @param {number} petId - 宠物ID（可选）
 * @param {string} startDate - 开始日期（可选）
 * @param {string} endDate - 结束日期（可选）
 * @returns {Promise} 统计信息响应
 */
export const getActivityStats = async (params = {}) => {
  try {
    const response = await request({
      url: '/activities/stats',
      method: 'GET',
      params
    })
    return response
  } catch (error) {
    console.error('获取活动统计信息失败:', error)
    throw error
  }
}

/**
 * 活动数据验证器
 */
export const activityValidator = {
  /**
   * 验证活动数据
   * @param {Object} activityData - 活动数据
   * @returns {Object} 验证结果 { isValid: boolean, errors: Array }
   */
  validate(activityData) {
    const errors = []

    // 验证活动名称
    if (!activityData.activityName || activityData.activityName.trim().length === 0) {
      errors.push('活动名称不能为空')
    } else if (activityData.activityName.length > 100) {
      errors.push('活动名称长度不能超过100个字符')
    }

    // 验证活动种类ID
    if (!activityData.activityKindId) {
      errors.push('活动种类ID不能为空')
    } else if (typeof activityData.activityKindId !== 'number' || activityData.activityKindId <= 0) {
      errors.push('活动种类ID必须是正整数')
    }

    // 验证用户ID
    if (!activityData.userId) {
      errors.push('用户ID不能为空')
    } else if (typeof activityData.userId !== 'number' || activityData.userId <= 0) {
      errors.push('用户ID必须是正整数')
    }

    return {
      isValid: errors.length === 0,
      errors
    }
  },

  /**
   * 验证活动记录数据
   * @param {Object} recordData - 活动记录数据
   * @returns {Object} 验证结果 { isValid: boolean, errors: Array }
   */
  validateRecord(recordData) {
    const errors = []

    // 验证活动ID
    if (!recordData.activityId) {
      errors.push('活动ID不能为空')
    } else if (typeof recordData.activityId !== 'number' || recordData.activityId <= 0) {
      errors.push('活动ID必须是正整数')
    }

    // 验证描述
    if (!recordData.description || recordData.description.trim().length === 0) {
      errors.push('活动描述不能为空')
    } else if (recordData.description.length > 500) {
      errors.push('活动描述长度不能超过500个字符')
    }

    // 验证日期
    if (!recordData.date) {
      errors.push('活动日期不能为空')
    } else {
      const datePattern = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/
      if (!datePattern.test(recordData.date)) {
        errors.push('日期格式不正确，请使用 yyyy-MM-dd\'T\'HH:mm:ss 格式')
      }
    }

    return {
      isValid: errors.length === 0,
      errors
    }
  }
}

// 默认导出
export default {
  getAllActivities,
  getActivityById,
  createActivity,
  updateActivity,
  deleteActivity,
  completeDeleteActivity,
  getActivityRecords,
  createActivityRecord,
  createActivityRecordByKind,
  updateActivityRecord,
  deleteActivityRecord,
  getActivitiesByUserId,
  getActivityRecordsByPetIds,
  getActivityStats,
  activityValidator
}