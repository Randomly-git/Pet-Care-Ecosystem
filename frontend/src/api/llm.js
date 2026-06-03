/**
 * LLM智能服务 API 接口
 * 使用 GraphQL 获取AI宠物状态分析和智能对话功能
 */

import apiClient from './index'

/**
 * 获取宠物状态AI总结 (使用 GraphQL)
 * @param {number} petId - 宠物ID
 * @param {string} userRequirement - 可选的用户特别关心的问题
 * @returns {Promise} AI总结响应
 */
export const getPetStatusSummary = async (petId, userRequirement = null) => {
  try {
    const query = `
      query activityHealthAnalysis($petId: ID!, $days: Int, $userRequirement: String) {
        activityHealthAnalysis(petId: $petId, days: $days, userRequirement: $userRequirement) {
          petId
          petName
          breed
          species
          analysis
          analysisType
        }
      }
    `

    const variables = {
      petId: String(petId),
      days: 30,
      userRequirement: userRequirement && userRequirement.trim() ? userRequirement : null
    }

    console.log('发送AI分析请求，petId:', petId, 'userRequirement:', userRequirement)

    const response = await apiClient({
      url: '/graphql',
      method: 'POST',
      data: { query, variables },
      baseURL: '',
      timeout: 120000,
      headers: {
        'Cache-Control': 'no-cache',
        'Pragma': 'no-cache'
      }
    })

    console.log('AI分析API响应:', response)

    if (response && response.data && response.data.activityHealthAnalysis) {
      const data = response.data.activityHealthAnalysis
      // 映射字段以兼容前端已有代码 (healthAdvice -> analysis)
      return {
        petId: data.petId,
        name: data.petName,
        breed: data.breed,
        species: data.species,
        healthAdvice: data.analysis,
        analysisType: data.analysisType
      }
    }

    console.log('未找到activityHealthAnalysis数据，返回完整响应')
    return response
  } catch (error) {
    console.error('获取AI状态总结失败:', error)
    throw error
  }
}

/**
 * 快速获取宠物状态总结（纯文本格式）
 * @param {number} petId - 宠物ID
 * @param {string} userRequirement - 可选的用户特别关心的问题
 * @returns {Promise} 总结文本
 */
export const getQuickStatusSummary = async (petId, userRequirement = null) => {
  try {
    const result = await getPetStatusSummary(petId, userRequirement)

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

export default {
  getPetStatusSummary,
  getQuickStatusSummary,
  askAI
}
