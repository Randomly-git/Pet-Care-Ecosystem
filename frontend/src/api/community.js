/**
 * 社区动态 API 接口
 */

import request from './index'

// 社区微服务的基础路径（通过网关路由）
// 注意：不包含/api前缀，因为index.js已经配置了baseURL为/api
const COMMUNITY_BASE_URL = '/v1'

/**
 * 创建新动态
 * @param {Object} momentData - 动态数据
 * @param {number} momentData.userId - 用户ID
 * @param {string} momentData.content - 动态内容
 * @param {Array} momentData.mediaIds - 媒体文件ID数组
 * @returns {Promise} 创建结果响应
 */
export const createMoment = async (momentData) => {
  try {
    // 验证必需字段
    const requiredFields = ['userId', 'content']
    const missingFields = requiredFields.filter(field => !momentData[field])

    if (missingFields.length > 0) {
      throw new Error(`缺少必需字段: ${missingFields.join(', ')}`)
    }

    // 处理mediaIds参数：确保为null而不是空数组
    const requestData = {
      userId: momentData.userId,
      content: momentData.content,
      mediaIds: momentData.mediaIds && momentData.mediaIds.length > 0 ? momentData.mediaIds : null
    }

    console.log('发送创建动态请求:', requestData)

    // 使用统一的request模块
    const response = await request({
      url: `${COMMUNITY_BASE_URL}/moments`,
      method: 'POST',
      data: requestData
    })

    console.log('动态创建成功:', response)
    return response
  } catch (error) {
    console.error('创建动态失败:', error)
    throw error
  }
}

/**
 * 获取用户的动态列表
 * @param {number} userId - 用户ID
 * @returns {Promise} 用户动态列表
 */
export const getUserMoments = async (userId) => {
  try {
    if (!userId) {
      throw new Error('用户ID不能为空')
    }

    console.log('获取用户动态, userId:', userId)

    // 使用统一的request模块
    const moments = await request({
      url: `${COMMUNITY_BASE_URL}/moments/user/${userId}`,
      method: 'GET'
    })

    console.log('获取到的用户动态:', moments)
    return moments
  } catch (error) {
    console.error('获取用户动态失败:', error)
    throw error
  }
}

/**
 * 删除动态
 * @param {number} momentId - 动态ID
 * @returns {Promise} 删除结果
 */
export const deleteMoment = async (momentId) => {
  try {
    if (!momentId) {
      throw new Error('动态ID不能为空')
    }

    // 使用统一的request模块
    const response = await request({
      url: `${COMMUNITY_BASE_URL}/moments/${momentId}`,
      method: 'DELETE'
    })

    return response
  } catch (error) {
    console.error('删除动态失败:', error)
    throw error
  }
}

/**
 * 创建评论或回复
 * @param {Object} commentData - 评论数据
 * @param {number} commentData.userId - 用户ID
 * @param {number} commentData.momentId - 动态ID
 * @param {string} commentData.content - 评论内容
 * @param {number} [commentData.parentId] - 父评论ID（回复时使用）
 * @returns {Promise} 创建结果
 */
export const createComment = async (commentData) => {
  try {
    // 验证必需字段
    const requiredFields = ['userId', 'momentId', 'content']
    const missingFields = requiredFields.filter(field => !commentData[field])

    if (missingFields.length > 0) {
      throw new Error(`缺少必需字段: ${missingFields.join(', ')}`)
    }

    // 使用统一的request模块
    const response = await request({
      url: `${COMMUNITY_BASE_URL}/comments`,
      method: 'POST',
      data: commentData
    })

    return response
  } catch (error) {
    console.error('创建评论失败:', error)
    throw error
  }
}

/**
 * 获取动态下的所有评论
 * @param {number} momentId - 动态ID
 * @returns {Promise} 评论列表（含嵌套回复）
 */
export const getMomentComments = async (momentId) => {
  try {
    if (!momentId) {
      throw new Error('动态ID不能为空')
    }

    // 使用统一的request模块
    const response = await request({
      url: `${COMMUNITY_BASE_URL}/comments/moment/${momentId}`,
      method: 'GET'
    })

    return response
  } catch (error) {
    console.error('获取评论失败:', error)
    throw error
  }
}

/**
 * 删除评论
 * @param {number} commentId - 评论ID
 * @returns {Promise} 删除结果
 */
export const deleteComment = async (commentId) => {
  try {
    if (!commentId) {
      throw new Error('评论ID不能为空')
    }

    // 使用统一的request模块
    const response = await request({
      url: `${COMMUNITY_BASE_URL}/comments/${commentId}`,
      method: 'DELETE'
    })

    return response
  } catch (error) {
    console.error('删除评论失败:', error)
    throw error
  }
}

/**
 * 切换点赞状态
 * @param {Object} likeData - 点赞数据
 * @param {number} likeData.userId - 用户ID
 * @param {string} likeData.targetType - 目标类型（MOMENT 或 COMMENT）
 * @param {number} likeData.targetId - 目标ID
 * @returns {Promise} 操作结果
 */
export const toggleLike = async (likeData) => {
  try {
    // 验证必需字段
    const requiredFields = ['userId', 'targetType', 'targetId']
    const missingFields = requiredFields.filter(field => !likeData[field])

    if (missingFields.length > 0) {
      throw new Error(`缺少必需字段: ${missingFields.join(', ')}`)
    }

    // 使用统一的request模块
    const response = await request({
      url: `${COMMUNITY_BASE_URL}/likes`,
      method: 'POST',
      data: likeData
    })

    return response
  } catch (error) {
    console.error('点赞操作失败:', error)
    throw error
  }
}

/**
 * 关注或取消关注用户
 * @param {Object} followData - 关注数据
 * @param {number} followData.followerId - 关注者ID
 * @param {number} followData.followedId - 被关注者ID
 * @returns {Promise} 操作结果
 */
export const toggleFollow = async (followData) => {
  try {
    // 验证必需字段
    const requiredFields = ['followerId', 'followedId']
    const missingFields = requiredFields.filter(field => !followData[field])

    if (missingFields.length > 0) {
      throw new Error(`缺少必需字段: ${missingFields.join(', ')}`)
    }

    // 使用统一的request模块
    const response = await request({
      url: `${COMMUNITY_BASE_URL}/follows`,
      method: 'POST',
      data: followData
    })

    return response
  } catch (error) {
    console.error('关注操作失败:', error)
    throw error
  }
}

/**
 * 获取用户粉丝数
 * @param {number} userId - 用户ID
 * @returns {Promise} 粉丝数
 */
export const getFollowersCount = async (userId) => {
  try {
    if (!userId) {
      throw new Error('用户ID不能为空')
    }

    // 使用统一的request模块
    const countText = await request({
      url: `${COMMUNITY_BASE_URL}/follows/followers/count/${userId}`,
      method: 'GET'
    })

    return parseInt(countText, 10)
  } catch (error) {
    console.error('获取粉丝数失败:', error)
    throw error
  }
}

/**
 * 获取用户关注数
 * @param {number} userId - 用户ID
 * @returns {Promise} 关注数
 */
export const getFollowingCount = async (userId) => {
  try {
    if (!userId) {
      throw new Error('用户ID不能为空')
    }

    // 使用统一的request模块
    const countText = await request({
      url: `${COMMUNITY_BASE_URL}/follows/following/count/${userId}`,
      method: 'GET'
    })

    return parseInt(countText, 10)
  } catch (error) {
    console.error('获取关注数失败:', error)
    throw error
  }
}

/**
 * 获取所有用户的动态（分页）
 * @param {number} page - 页码，从0开始
 * @param {number} size - 每页大小，默认20
 * @returns {Promise} 所有用户的动态列表
 */
export const getAllMoments = async (page = 0, size = 20) => {
  try {
    console.log('获取所有用户动态, page:', page, 'size:', size)

    // 使用统一的request模块
    const moments = await request({
      url: `${COMMUNITY_BASE_URL}/moments/all`,
      method: 'GET',
      params: { page, size }
    })

    console.log('获取所有用户动态成功:', moments)
    return moments
  } catch (error) {
    console.error('获取所有用户动态失败:', error)
    throw error
  }
}

// 默认导出
export default {
  createMoment,
  getUserMoments,
  getAllMoments,
  deleteMoment,
  createComment,
  getMomentComments,
  deleteComment,
  toggleLike,
  toggleFollow,
  getFollowersCount,
  getFollowingCount
}