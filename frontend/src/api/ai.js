import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

// 使用统一的 axios 实例或者创建一个新的
const aiClient = axios.create({
  baseURL: '', // Vite 配置了 /graphql 直接代理到网关
  timeout: 90000, // AI 请求可能需要较长时间（加长至90s以防Qwen生成过长）
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
aiClient.interceptors.request.use(
  (config) => {
    // 获取token
    const token = localStorage.getItem('authToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
aiClient.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    console.error('AI API 请求错误:', error)
    return Promise.reject(error)
  }
)

/**
 * 获取宠物活动健康分析报告
 * @param {string|number} petId - 宠物ID
 * @param {number} days - 分析天数
 * @param {string} userRequirement - 用户额外要求（可选）
 * @returns {Promise<Object>} 分析结果
 */
export const getActivityHealthAnalysis = async (petId, days = 7, userRequirement = '') => {
  const query = `
    query activityHealthAnalysis($petId: ID!, $days: Int, $userRequirement: String) {
      activityHealthAnalysis(petId: $petId, days: $days, userRequirement: $userRequirement) {
        petId
        petName
        breed
        species
        analysis
        analysisType
        knowledgeSources {
          title
          content
          score
        }
      }
    }
  `

  const variables = {
    petId: String(petId),
    days: days,
    userRequirement: userRequirement
  }

  try {
    const response = await aiClient.post('/graphql', {
      query,
      variables
    })
    
    if (response.errors) {
      throw new Error(response.errors[0].message || '获取健康分析报告失败')
    }
    
    return response.data.activityHealthAnalysis
  } catch (error) {
    console.error('获取宠物活动健康分析报告失败:', error)
    throw error
  }
}


/**
 * Identify Cat Breed
 * @param {File} imageFile 
 * @returns {Promise<Object>}
 */
export const identifyCatBreed = async (imageFile) => {
  const formData = new FormData()
  formData.append('image', imageFile)
  
  try {
    const response = await aiClient.post('/api/cat/identify', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    return response
  } catch (error) {
    console.error('Failed to identify cat breed:', error)
    throw error
  }
}

/**
 * AI Agent Chat
 * @param {string|number} petId
 * @param {string} message
 * @param {string|null} conversationId
 * @returns {Promise<Object>}
 */
export const aiAgentChat = async (petId, message, conversationId = null) => {
  const query = `
    query aiAgent($petId: ID!, $message: String!, $conversationId: String) {
      aiAgent(petId: $petId, message: $message, conversationId: $conversationId) {
        petId
        petName
        message
        conversationId
        toolCalls {
          toolName
          arguments
          success
        }
      }
    }
  `

  const variables = {
    petId: String(petId),
    message: message,
    conversationId: conversationId
  }

  try {
    const response = await aiClient.post('/graphql', {
      query,
      variables
    })
    
    if (response.errors) {
      throw new Error(response.errors[0].message || 'Agent request failed')
    }
    
    return response.data.aiAgent
  } catch (error) {
    console.error('Agent chat failed:', error)
    throw error
  }
}

export default {
  getActivityHealthAnalysis,
  identifyCatBreed,
  aiAgentChat
}
