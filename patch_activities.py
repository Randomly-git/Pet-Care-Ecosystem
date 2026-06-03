with open('d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/api/activities.js', 'a', encoding='utf-8') as f:
    f.write('''

// ================= AI Health Abnormal APIs =================

/**
 * Get abnormal health records for a pet
 * @param {number|string} petId
 * @returns {Promise}
 */
export const getAbnormalRecords = async (petId) => {
  try {
    const response = await request({
      url: `/activities/records/pet/${petId}/abnormal`,
      method: 'GET'
    })
    return response
  } catch (error) {
    console.error('获取异常健康记录失败:', error)
    throw error
  }
}

/**
 * Ignore an abnormal record
 * @param {number|string} recordId
 * @returns {Promise}
 */
export const ignoreAbnormalRecord = async (recordId) => {
  try {
    const response = await request({
      url: `/activities/records/${recordId}/ignore`,
      method: 'PUT'
    })
    return response
  } catch (error) {
    console.error('忽略异常健康记录失败:', error)
    throw error
  }
}
''')
