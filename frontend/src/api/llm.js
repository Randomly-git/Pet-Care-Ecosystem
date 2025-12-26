/**
 * LLM智能服务 API 接口
 * 提供AI宠物状态总结和智能对话功能
 */

import apiClient from './index'

/**
 * 获取宠物状态AI总结
 * @param {number} petId - 宠物ID
 * @returns {Promise} AI总结响应
 */
export const getPetStatusSummary = async (petId) => {
  try {
    const response = await apiClient({
      url: `/llm/pet-status/summary/${petId}`,
      method: 'GET'
    })
    return response
  } catch (error) {
    console.error('获取AI状态总结失败:', error)
    throw error
  }
}

/**
 * 快速获取宠物状态总结（纯文本）
 * @param {number} petId - 宠物ID
 * @returns {Promise} 总结文本
 */
export const getQuickStatusSummary = async (petId) => {
  try {
    const response = await apiClient({
      url: `/llm/pet-status/summary/${petId}/quick`,
      method: 'GET'
    })
    return response
  } catch (error) {
    console.error('获取快速总结失败:', error)
    throw error
  }
}

/**
 * AI智能对话
 * @param {string} question - 问题
 * @returns {Promise} AI回答
 */
export const askAI = async (question) => {
  try {
    const response = await apiClient({
      url: '/chat/ask',
      method: 'POST',
      data: question,
      headers: {
        'Content-Type': 'text/plain'
      }
    })
    return response
  } catch (error) {
    console.error('AI对话失败:', error)
    throw error
  }
}

// 默认导出
export default {
  getPetStatusSummary,
  getQuickStatusSummary,
  askAI
}
