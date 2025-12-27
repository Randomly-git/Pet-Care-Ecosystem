/**
 * LLM智能服务 API 接口
 * 使用 GraphQL 获取AI宠物状态分析和智能对话功能
 */

import apiClient from './index'

/**
 * 获取宠物状态AI总结 (使用 GraphQL)
 * @param {number} petId - 宠物ID
 * @returns {Promise} AI总结响应
 */
export const getPetStatusSummary = async (petId) => {
  try {
    // GraphQL 查询
    const query = `
      {
        petHealthAnalysis(petId: "${petId}") {
          petId
          name
          breed
          species
          healthAdvice
          statusRecords {
            statusName
            description
            startDate
          }
        }
      }
    `

    // 使用完整的URL绕过 baseURL
    const response = await apiClient({
      url: '/graphql',
      method: 'POST',
      data: { query },
      // 使用 baseURL: '' 来覆盖默认的 '/api'
      baseURL: '',
      timeout: 60000  // AI分析可能需要较长时间，设置60秒超时
    })

    // 返回 GraphQL 的 data 部分
    if (response && response.data && response.data.petHealthAnalysis) {
      return response.data.petHealthAnalysis
    }

    return response
  } catch (error) {
    console.error('获取AI状态总结失败:', error)
    throw error
  }
}

/**
 * 快速获取宠物状态总结（纯文本格式）
 * @param {number} petId - 宠物ID
 * @returns {Promise} 总结文本
 */
export const getQuickStatusSummary = async (petId) => {
  try {
    const result = await getPetStatusSummary(petId)

    // 提取健康建议作为纯文本返回
    if (result && result.healthAdvice) {
      return {
        data: {
          summary: result.healthAdvice,
          petName: result.name,
          breed: result.breed,
          species: result.species
        }
      }
    }

    return result
  } catch (error) {
    console.error('获取快速总结失败:', error)
    throw error
  }
}

/**
 * AI智能对话 (预留功能)
 * @param {string} question - 问题
 * @returns {Promise} AI回答
 */
export const askAI = async (question) => {
  try {
    // 预留对话功能，后续可通过 GraphQL 扩展
    const response = await apiClient({
      url: '/graphql',
      method: 'POST',
      data: {
        query: `{ chat(question: "${question}") }`
      },
      baseURL: ''
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
