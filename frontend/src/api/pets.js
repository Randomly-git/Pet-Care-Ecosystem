/**
 * 宠物管理 API 接口
 */

import request from './index'

/**
 * 获取当前用户的所有宠物
 * @param {number} userId - 用户ID
 * @returns {Promise} 宠物列表响应
 */
export const getUserPets = async (userId) => {
  try {
    const response = await request({
      url: `/pets/user/${userId}`,
      method: 'GET'
    })
    return response
  } catch (error) {
    console.error('获取用户宠物列表失败:', error)
    throw error
  }
}

/**
 * 获取所有宠物信息
 * @returns {Promise} 所有宠物列表响应
 */
export const getAllPets = async () => {
  try {
    const response = await request({
      url: '/pets',
      method: 'GET'
    })
    return response
  } catch (error) {
    console.error('获取所有宠物信息失败:', error)
    throw error
  }
}

/**
 * 创建新宠物
 * @param {Object} petData - 宠物数据
 * @param {string} petData.name - 宠物名称
 * @param {string} petData.species - 宠物类型 (dog, cat, bird, fish, other)
 * @param {string} petData.breed - 宠物品种
 * @param {string} petData.birthday - 宠物生日 (YYYY-MM-DD)
 * @param {number} petData.userId - 用户ID
 * @returns {Promise} 创建结果响应
 */
export const createPet = async (petData) => {
  try {
    // 验证必需字段
    const requiredFields = ['name', 'species', 'breed', 'userId', 'birthday']
    const missingFields = requiredFields.filter(field => {
      const value = petData[field]
      // 字段不能为null、undefined或空字符串
      return value === null || value === undefined || value === ''
    })

    if (missingFields.length > 0) {
      throw new Error(`缺少必需字段: ${missingFields.join(', ')}`)
    }

    // 验证宠物类型（现在在species字段中）
    const validTypes = ['dog', 'cat', 'bird', 'fish', 'other']
    if (!validTypes.includes(petData.species)) {
      throw new Error(`无效的宠物类型: ${petData.species}. 有效类型: ${validTypes.join(', ')}`)
    }

    const response = await request({
      url: '/pets',
      method: 'POST',
      data: petData
    })
    return response
  } catch (error) {
    console.error('创建宠物失败:', error)
    throw error
  }
}

/**
 * 更新宠物信息
 * @param {number} petId - 宠物ID
 * @param {Object} petData - 更新的宠物数据
 * @returns {Promise} 更新结果响应
 */
export const updatePet = async (petId, petData) => {
  try {
    if (!petId) {
      throw new Error('宠物ID不能为空')
    }

    // 验证宠物类型（如果提供）
    if (petData.type) {
      const validTypes = ['dog', 'cat', 'bird', 'fish', 'other']
      if (!validTypes.includes(petData.type)) {
        throw new Error(`无效的宠物类型: ${petData.type}`)
      }
    }

    // 验证性别（如果提供）
    if (petData.gender) {
      const validGenders = ['male', 'female', 'unknown']
      if (!validGenders.includes(petData.gender)) {
        throw new Error(`无效的性别: ${petData.gender}`)
      }
    }

    const response = await request({
      url: `/pets/${petId}`,
      method: 'PUT',
      data: petData
    })
    return response
  } catch (error) {
    console.error('更新宠物信息失败:', error)
    throw error
  }
}

/**
 * 删除宠物
 * @param {number} petId - 宠物ID
 * @returns {Promise} 删除结果响应
 */
export const deletePet = async (petId) => {
  try {
    if (!petId) {
      throw new Error('宠物ID不能为空')
    }

    const response = await request({
      url: `/pets/${petId}`,
      method: 'DELETE'
    })
    return response
  } catch (error) {
    console.error('删除宠物失败:', error)
    throw error
  }
}

/**
 * 根据ID获取宠物信息
 * @param {number} petId - 宠物ID
 * @returns {Promise} 宠物信息响应
 */
export const getPetById = async (petId) => {
  try {
    if (!petId) {
      throw new Error('宠物ID不能为空')
    }

    const response = await request({
      url: `/pets/${petId}`,
      method: 'GET'
    })
    return response
  } catch (error) {
    console.error('获取宠物信息失败:', error)
    throw error
  }
}

/**
 * 根据类型获取宠物列表
 * @param {string} type - 宠物类型 (dog, cat, bird, fish, other)
 * @returns {Promise} 指定类型的宠物列表响应
 */
export const getPetsByType = async (type) => {
  try {
    const validTypes = ['dog', 'cat', 'bird', 'fish', 'other']
    if (!validTypes.includes(type)) {
      throw new Error(`无效的宠物类型: ${type}. 有效类型: ${validTypes.join(', ')}`)
    }

    const response = await request({
      url: `/pets/type/${type}`,
      method: 'GET'
    })
    return response
  } catch (error) {
    console.error('获取指定类型宠物失败:', error)
    throw error
  }
}

/**
 * 批量获取用户宠物信息
 * @param {Array} userIds - 用户ID数组
 * @returns {Promise} 批量宠物信息响应
 */
export const getPetsByUserIds = async (userIds) => {
  try {
    if (!Array.isArray(userIds) || userIds.length === 0) {
      throw new Error('用户ID数组不能为空')
    }

    const response = await request({
      url: '/pets/batch',
      method: 'POST',
      data: { userIds }
    })
    return response
  } catch (error) {
    console.error('批量获取宠物信息失败:', error)
    throw error
  }
}

/**
 * 搜索宠物
 * @param {Object} searchParams - 搜索参数
 * @param {string} searchParams.keyword - 搜索关键词（名称、品种）
 * @param {string} searchParams.type - 宠物类型
 * @param {string} searchParams.gender - 性别
 * @param {number} searchParams.page - 页码
 * @param {number} searchParams.size - 每页数量
 * @returns {Promise} 搜索结果响应
 */
export const searchPets = async (searchParams = {}) => {
  try {
    const response = await request({
      url: '/pets/search',
      method: 'GET',
      params: searchParams
    })
    return response
  } catch (error) {
    console.error('搜索宠物失败:', error)
    throw error
  }
}

/**
 * 获取宠物统计信息
 * @param {number} userId - 用户ID（可选，如果不提供则获取全局统计）
 * @returns {Promise} 统计信息响应
 */
export const getPetStats = async (userId = null) => {
  try {
    const url = userId ? `/pets/stats/${userId}` : '/pets/stats'
    const response = await request({
      url,
      method: 'GET'
    })
    return response
  } catch (error) {
    console.error('获取宠物统计信息失败:', error)
    throw error
  }
}

/**
 * 宠物数据验证器
 */
export const petValidator = {
  /**
   * 验证宠物数据
   * @param {Object} petData - 宠物数据
   * @returns {Object} 验证结果 { isValid: boolean, errors: Array }
   */
  validate(petData) {
    const errors = []

    // 验证名称
    if (!petData.name || petData.name.trim().length === 0) {
      errors.push('宠物名称不能为空')
    } else if (petData.name.length > 50) {
      errors.push('宠物名称长度不能超过50个字符')
    }

    // 验证类型（species字段）
    const validTypes = ['dog', 'cat', 'bird', 'fish', 'other']
    if (!petData.species) {
      errors.push('宠物类型不能为空')
    } else if (!validTypes.includes(petData.species)) {
      errors.push(`无效的宠物类型: ${petData.species}`)
    }

    // 验证品种
    if (!petData.breed || petData.breed.trim().length === 0) {
      errors.push('宠物品种不能为空')
    } else if (petData.breed.length > 50) {
      errors.push('宠物品种长度不能超过50个字符')
    }

    // 验证生日
    if (!petData.birthday) {
      errors.push('宠物生日不能为空')
    }

    // 验证用户ID
    if (!petData.userId) {
      errors.push('用户ID不能为空')
    } else if (typeof petData.userId !== 'number' || petData.userId <= 0) {
      errors.push('用户ID必须是正整数')
    }

    // 验证头像URL（可选）
    if (petData.avatarUrl && petData.avatarUrl.length > 255) {
      errors.push('头像URL长度不能超过255个字符')
    }

    return {
      isValid: errors.length === 0,
      errors
    }
  }
}

// 默认导出
export default {
  getUserPets,
  getAllPets,
  createPet,
  updatePet,
  deletePet,
  getPetById,
  getPetsByType,
  getPetsByUserIds,
  searchPets,
  getPetStats,
  petValidator
}