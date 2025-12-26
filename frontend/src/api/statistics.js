/**
 * 统计服务 API 接口
 * 提供活动数据统计分析功能
 */

import apiClient from './index'

/**
 * 获取宠物活动统计数据（默认最近3个月）
 * @param {number} petId - 宠物ID
 * @param {string} period - 统计周期 (DAILY, WEEKLY, MONTHLY)
 * @returns {Promise} 统计数据响应
 */
export const getActivityStats = async (petId, period = 'MONTHLY') => {
  try {
    const response = await apiClient({
      url: `/stats/activity/pet/${petId}`,
      method: 'GET',
      params: { period }
    })
    return response
  } catch (error) {
    console.error('获取活动统计失败:', error)
    throw error
  }
}

/**
 * 获取指定时间段的活动统计数据
 * @param {number} petId - 宠物ID
 * @param {string} startDate - 开始时间 (ISO格式)
 * @param {string} endDate - 结束时间 (ISO格式)
 * @param {string} period - 统计周期 (DAILY, WEEKLY, MONTHLY)
 * @returns {Promise} 统计数据响应
 */
export const getActivityStatsInRange = async (petId, startDate, endDate, period = 'MONTHLY') => {
  try {
    const response = await apiClient({
      url: `/stats/activity/pet/${petId}/range`,
      method: 'GET',
      params: {
        startDate,
        endDate,
        period
      }
    })
    return response
  } catch (error) {
    console.error('获取时间段统计失败:', error)
    throw error
  }
}

/**
 * 获取完整参数的活动统计
 * @param {Object} request - 统计请求对象
 * @returns {Promise} 统计数据响应
 */
export const getFullActivityStats = async (request) => {
  try {
    const response = await apiClient({
      url: '/stats/activity',
      method: 'POST',
      data: request
    })
    return response
  } catch (error) {
    console.error('获取完整统计失败:', error)
    throw error
  }
}

// 默认导出
export default {
  getActivityStats,
  getActivityStatsInRange,
  getFullActivityStats
}
