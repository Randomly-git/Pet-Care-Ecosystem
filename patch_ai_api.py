with open('d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/api/ai.js', 'r', encoding='utf-8') as f:
    content = f.read()

injection = """
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
 * @returns {Promise<Object>}
 */
export const aiAgentChat = async (petId, message) => {
  const query = `
    query aiAgent($petId: ID!, $message: String!) {
      aiAgent(petId: $petId, message: $message) {
        petId
        petName
        message
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
    message: message
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
"""

content = content.replace('export default {', injection + '\nexport default {')
content = content.replace('getActivityHealthAnalysis', 'getActivityHealthAnalysis,\n  identifyCatBreed,\n  aiAgentChat')

with open('d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/api/ai.js', 'w', encoding='utf-8') as f:
    f.write(content)
