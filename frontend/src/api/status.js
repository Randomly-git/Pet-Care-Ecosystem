/**
 * 状态管理 API 接口
 */

import request from './index'

/**
 * 获取宠物的所有状态
 * @param {number} petId - 宠物ID
 * @returns {Promise} 宠物状态列表响应
 */
export const getUserStatuses = async (petId) => {
  try {
    if (!petId) {
      throw new Error('宠物ID不能为空')
    }

    const response = await request({
      url: `/status/pet/${petId}`,
      method: 'GET'
    })
    return response
  } catch (error) {
    console.error('获取宠物状态失败:', error)
    throw error
  }
}

/**
 * 创建新状态（为宠物创建）
 * @param {Object} statusData - 状态数据
 * @param {number} statusData.petId - 宠物ID
 * @param {string} statusData.statusName - 状态名称
 * @returns {Promise} 创建结果响应
 */
export const createStatus = async (statusData) => {
  try {
    // 验证必需字段
    const requiredFields = ['petId', 'statusName']
    const missingFields = requiredFields.filter(field => !statusData[field])

    if (missingFields.length > 0) {
      throw new Error(`缺少必需字段: ${missingFields.join(', ')}`)
    }

    // 验证宠物ID
    if (typeof statusData.petId !== 'number' || statusData.petId <= 0) {
      throw new Error('宠物ID必须是正整数')
    }

    // 验证状态名称
    if (!statusData.statusName || statusData.statusName.trim().length === 0) {
      throw new Error('状态名称不能为空')
    } else if (statusData.statusName.length > 50) {
      throw new Error('状态名称长度不能超过50个字符')
    }

    const response = await request({
      url: '/status',
      method: 'POST',
      params: { petId: statusData.petId, statusName: statusData.statusName }
    })
    return response
  } catch (error) {
    console.error('创建状态失败:', error)
    throw error
  }
}

/**
 * 更新状态名称
 * @param {number} statusId - 状态ID
 * @param {string} newName - 新的状态名称
 * @returns {Promise} 更新结果响应
 */
export const updateStatusName = async (statusId, newName) => {
  try {
    if (!statusId) {
      throw new Error('状态ID不能为空')
    }

    if (!newName || newName.trim().length === 0) {
      throw new Error('状态名称不能为空')
    } else if (newName.length > 50) {
      throw new Error('状态名称长度不能超过50个字符')
    }

    const response = await request({
      url: `/status/${statusId}/name`,
      method: 'PUT',
      params: { newName }
    })
    return response
  } catch (error) {
    console.error('更新状态名称失败:', error)
    throw error
  }
}

/**
 * 软删除状态
 * @param {number} statusId - 状态ID
 * @returns {Promise} 删除结果响应
 */
export const deleteStatus = async (statusId) => {
  try {
    if (!statusId) {
      throw new Error('状态ID不能为空')
    }

    const response = await request({
      url: `/status/${statusId}`,
      method: 'DELETE'
    })
    return response
  } catch (error) {
    console.error('删除状态失败:', error)
    throw error
  }
}

/**
 * 彻底删除状态（包括所有相关记录）
 * @param {number} statusId - 状态ID
 * @returns {Promise} 删除结果响应
 */
export const deleteStatusWithRecords = async (statusId) => {
  try {
    if (!statusId) {
      throw new Error('状态ID不能为空')
    }

    const response = await request({
      url: `/status/${statusId}/with-records`,
      method: 'DELETE'
    })
    return response
  } catch (error) {
    console.error('彻底删除状态失败:', error)
    throw error
  }
}

/**
 * 获取宠物的状态记录
 * @param {number} petId - 宠物ID
 * @param {Object} searchParams - 搜索参数
 * @param {string} [searchParams.startDate] - 开始时间 yyyy-MM-dd'T'HH:mm:ss
 * @param {string} [searchParams.endDate] - 结束时间 yyyy-MM-dd'T'HH:mm:ss
 * @param {number} [searchParams.statusId] - 状态ID
 * @returns {Promise} 状态记录列表响应
 */
export const getStatusRecords = async (petId, searchParams = {}) => {
  try {
    if (!petId) {
      throw new Error('宠物ID不能为空')
    }

    const response = await request({
      url: `/status/records/pet/${petId}`,
      method: 'GET',
      params: searchParams
    })
    return response
  } catch (error) {
    console.error('获取状态记录失败:', error)
    throw error
  }
}

/**
 * 获取宠物的活跃状态记录（在指定日期有效的记录）
 * @param {number} petId - 宠物ID
 * @param {string} targetDate - 目标日期 yyyy-MM-dd
 * @returns {Promise} 活跃状态记录响应
 */
export const getActiveStatusRecord = async (petId, targetDate) => {
  try {
    if (!petId) {
      throw new Error('宠物ID不能为空')
    }

    if (!targetDate) {
      throw new Error('目标日期不能为空')
    }

    // 验证日期格式
    const datePattern = /^\d{4}-\d{2}-\d{2}$/
    if (!datePattern.test(targetDate)) {
      throw new Error('日期格式不正确，请使用 yyyy-MM-dd 格式')
    }

    const response = await request({
      url: '/status/records/active',
      method: 'GET',
      params: { petId, targetDate }
    })
    return response
  } catch (error) {
    console.error('获取活跃状态记录失败:', error)
    throw error
  }
}

/**
 * 创建状态记录（支持文件上传）
 * @param {Object} recordData - 状态记录数据
 * @param {number} recordData.statusId - 状态ID
 * @param {number} recordData.petId - 宠物ID
 * @param {string} recordData.startDate - 开始时间 yyyy-MM-dd
 * @param {string} [recordData.description] - 状态描述（可选）
 * @param {File} [recordData.file] - 上传的文件（可选）
 * @param {number} recordData.userId - 用户ID
 * @returns {Promise} 创建结果响应
 */
export const createStatusRecord = async (recordData) => {
  try {
    // 验证必需字段
    const requiredFields = ['statusId', 'petId', 'startDate', 'userId']
    const missingFields = requiredFields.filter(field => recordData[field] === undefined || recordData[field] === null)

    if (missingFields.length > 0) {
      throw new Error(`缺少必需字段: ${missingFields.join(', ')}`)
    }

    // 验证状态ID
    if (typeof recordData.statusId !== 'number' || recordData.statusId <= 0) {
      throw new Error('状态ID必须是正整数')
    }

    // 验证宠物ID
    if (typeof recordData.petId !== 'number' || recordData.petId <= 0) {
      throw new Error('宠物ID必须是正整数')
    }

    // 验证用户ID
    if (typeof recordData.userId !== 'number' || recordData.userId <= 0) {
      throw new Error('用户ID必须是正整数')
    }

    // 验证日期格式
    const datePattern = /^\d{4}-\d{2}-\d{2}$/
    if (!datePattern.test(recordData.startDate)) {
      throw new Error('开始时间格式不正确，请使用 yyyy-MM-dd 格式')
    }

    // 构建FormData
    const formData = new FormData()
    formData.append('statusId', recordData.statusId)
    formData.append('petId', recordData.petId)
    formData.append('startDate', recordData.startDate)
    formData.append('userId', recordData.userId)

    // 可选字段
    if (recordData.description) {
      formData.append('description', recordData.description)
    }

    if (recordData.file) {
      formData.append('file', recordData.file)
    }

    const response = await request({
      url: '/status/records',
      method: 'POST',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    return response
  } catch (error) {
    console.error('创建状态记录失败:', error)
    throw error
  }
}

/**
 * 更新状态记录（支持文件更新）
 * @param {number} statusRecordId - 状态记录ID
 * @param {Object} updateData - 更新数据
 * @param {string} [updateData.description] - 新的状态描述
 * @param {string} [updateData.startDate] - 新的开始日期 yyyy-MM-dd
 * @param {string} [updateData.endDate] - 新的结束日期 yyyy-MM-dd
 * @param {File} [updateData.file] - 新的媒体文件（可选）
 * @param {number} [updateData.userId] - 用户ID（仅上传文件时需要）
 * @returns {Promise} 更新结果响应
 */
export const updateStatusRecord = async (statusRecordId, updateData) => {
  try {
    if (!statusRecordId) {
      throw new Error('状态记录ID不能为空')
    }

    // 构建FormData
    const formData = new FormData()

    // 可选字段
    if (updateData.description !== undefined) {
      formData.append('description', updateData.description)
    }

    if (updateData.startDate) {
      // 验证日期格式
      const datePattern = /^\d{4}-\d{2}-\d{2}$/
      if (!datePattern.test(updateData.startDate)) {
        throw new Error('开始日期格式不正确，请使用 yyyy-MM-dd 格式')
      }
      formData.append('startDate', updateData.startDate)
    }

    if (updateData.endDate) {
      // 验证日期格式
      const datePattern = /^\d{4}-\d{2}-\d{2}$/
      if (!datePattern.test(updateData.endDate)) {
        throw new Error('结束日期格式不正确，请使用 yyyy-MM-dd 格式')
      }
      formData.append('endDate', updateData.endDate)
    }

    if (updateData.file) {
      formData.append('file', updateData.file)
    }

    if (updateData.userId) {
      formData.append('userId', updateData.userId)
    }

    const response = await request({
      url: `/status/records/${statusRecordId}`,
      method: 'PUT',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    return response
  } catch (error) {
    console.error('更新状态记录失败:', error)
    throw error
  }
}

/**
 * 停止状态记录（标记为结束）
 * @param {number} statusRecordId - 状态记录ID
 * @param {string} [endDate] - 结束时间 yyyy-MM-dd'T'HH:mm:ss，默认为当前时间
 * @returns {Promise} 停止结果响应
 */
export const stopStatusRecord = async (statusRecordId, endDate = null) => {
  try {
    if (!statusRecordId) {
      throw new Error('状态记录ID不能为空')
    }

    const params = {}
    if (endDate) {
      // 验证日期格式
      const datePattern = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/
      if (!datePattern.test(endDate)) {
        throw new Error('结束时间格式不正确，请使用 yyyy-MM-dd\'T\'HH:mm:ss 格式')
      }
      params.endDate = endDate
    }

    const response = await request({
      url: `/status/records/${statusRecordId}/stop`,
      method: 'PUT',
      params
    })
    return response
  } catch (error) {
    console.error('停止状态记录失败:', error)
    throw error
  }
}

/**
 * 删除状态记录
 * @param {number} statusRecordId - 状态记录ID
 * @returns {Promise} 删除结果响应
 */
export const deleteStatusRecord = async (statusRecordId) => {
  try {
    if (!statusRecordId) {
      throw new Error('状态记录ID不能为空')
    }

    const response = await request({
      url: `/status/records/${statusRecordId}`,
      method: 'DELETE'
    })
    return response
  } catch (error) {
    console.error('删除状态记录失败:', error)
    throw error
  }
}

/**
 * 批量获取状态记录
 * @param {Array} petIds - 宠物ID数组
 * @param {Object} searchParams - 搜索参数
 * @returns {Promise} 批量状态记录响应
 */
export const getStatusRecordsByPetIds = async (petIds, searchParams = {}) => {
  try {
    if (!Array.isArray(petIds) || petIds.length === 0) {
      throw new Error('宠物ID数组不能为空')
    }

    const response = await request({
      url: '/status/records/batch',
      method: 'POST',
      data: { petIds, ...searchParams }
    })
    return response
  } catch (error) {
    console.error('批量获取状态记录失败:', error)
    throw error
  }
}

/**
 * 为状态记录上传媒体文件
 * @param {number} statusRecordId - 状态记录ID
 * @param {FormData} formData 包含file和userId
 * @returns {Promise} 上传结果
 */
export const uploadStatusMedia = async (statusRecordId, formData) => {
  try {
    if (!statusRecordId) {
      throw new Error('状态记录ID不能为空')
    }

    const response = await request({
      url: `/status/records/${statusRecordId}/media`,
      method: 'POST',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    return response
  } catch (error) {
    console.error('为状态记录上传媒体文件失败:', error)
    throw error
  }
}

/**
 * 更新状态当前值
 * @param {number} statusId - 状态ID
 * @param {string} statusValue - 新的状态值
 * @returns {Promise} 更新结果响应
 */
export const updateStatusValue = async (statusId, statusValue) => {
  try {
    if (!statusId) {
      throw new Error('状态ID不能为空')
    }

    const response = await request({
      url: `/status/${statusId}/value`,
      method: 'PUT',
      params: { statusValue }
    })
    return response
  } catch (error) {
    console.error('更新状态值失败:', error)
    throw error
  }
}

/**
 * 状态数据验证器
 */
export const statusValidator = {
  /**
   * 验证状态数据
   * @param {Object} statusData - 状态数据
   * @returns {Object} 验证结果 { isValid: boolean, errors: Array }
   */
  validate(statusData) {
    const errors = []

    // 验证宠物ID
    if (!statusData.petId) {
      errors.push('宠物ID不能为空')
    } else if (typeof statusData.petId !== 'number' || statusData.petId <= 0) {
      errors.push('宠物ID必须是正整数')
    }

    // 验证状态名称
    if (!statusData.statusName || statusData.statusName.trim().length === 0) {
      errors.push('状态名称不能为空')
    } else if (statusData.statusName.length > 50) {
      errors.push('状态名称长度不能超过50个字符')
    }

    return {
      isValid: errors.length === 0,
      errors
    }
  },

  /**
   * 验证状态记录数据
   * @param {Object} recordData - 状态记录数据
   * @returns {Object} 验证结果 { isValid: boolean, errors: Array }
   */
  validateRecord(recordData) {
    const errors = []

    // 验证宠物ID
    if (!recordData.petId) {
      errors.push('宠物ID不能为空')
    } else if (typeof recordData.petId !== 'number' || recordData.petId <= 0) {
      errors.push('宠物ID必须是正整数')
    }

    // 验证状态ID
    if (!recordData.statusId) {
      errors.push('状态ID不能为空')
    } else if (typeof recordData.statusId !== 'number' || recordData.statusId <= 0) {
      errors.push('状态ID必须是正整数')
    }

    // 验证开始时间
    if (!recordData.startDate) {
      errors.push('开始时间不能为空')
    } else {
      const datePattern = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/
      if (!datePattern.test(recordData.startDate)) {
        errors.push('开始时间格式不正确，请使用 yyyy-MM-dd\'T\'HH:mm:ss 格式')
      }
    }

    // 验证描述长度（如果提供）
    if (recordData.statusDescription && recordData.statusDescription.length > 500) {
      errors.push('状态描述长度不能超过500个字符')
    }

    return {
      isValid: errors.length === 0,
      errors
    }
  }
}

// 默认导出
export default {
  getUserStatuses,
  createStatus,
  updateStatusName,
  updateStatusValue,
  deleteStatus,
  deleteStatusWithRecords,
  getStatusRecords,
  getActiveStatusRecord,
  createStatusRecord,
  updateStatusRecord,
  stopStatusRecord,
  deleteStatusRecord,
  getStatusRecordsByPetIds,
  statusValidator
}