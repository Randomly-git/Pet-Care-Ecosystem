/**
 * 媒体文件上传 API 接口
 */

import apiClient, { createUploadClient } from './index'

// 媒体微服务的基础路径（通过前端代理服务器）
const MEDIA_BASE_URL = '/media'

// 创建专用的上传客户端（超时时间更长）
const uploadClient = createUploadClient()

/**
 * 上传单个媒体文件
 * @param {File} file - 要上传的文件
 * @param {number} userId - 用户ID
 * @param {string} [businessType] - 业务类型（如：MOMENT, AVATAR等）
 * @param {number} [businessId] - 业务ID（如：动态ID、用户ID等）
 * @returns {Promise} 上传结果，包含媒体文件ID和URL
 */
export const uploadMedia = async (file, userId, businessType = 'MOMENT', businessId = null) => {
  try {
    if (!file) {
      throw new Error('请选择要上传的文件')
    }

    if (!userId) {
      throw new Error('用户ID不能为空')
    }

    console.log('开始上传媒体文件:', {
      fileName: file.name,
      fileSize: file.size,
      fileType: file.type,
      userId,
      businessType,
      businessId
    })

    // 创建 FormData
    const formData = new FormData()
    formData.append('file', file)
    formData.append('userId', userId)

    if (businessType !== undefined && businessType !== null) {
      formData.append('relatedType', businessType)
    }

    if (businessId !== undefined && businessId !== null) {
      formData.append('relatedId', businessId)
    }

    console.log('发送媒体上传请求到:', `${MEDIA_BASE_URL}/upload`)

    // 使用专用的上传客户端（更长超时）
    const response = await uploadClient({
      url: `${MEDIA_BASE_URL}/upload`,
      method: 'POST',
      data: formData
    })

    console.log('媒体上传成功:', response)

    return response
  } catch (error) {
    console.error('上传文件失败:', error)
    console.error('文件', file?.name, '上传失败:', error)
    throw error
  }
}

/**
 * 批量上传媒体文件（分批上传，避免并发过高导致超时）
 * @param {FileList|Object} filesOrOptions - 要上传的文件列表，或包含所有参数的对象
 * @param {number} [userId] - 用户ID（当第一个参数为文件列表时使用）
 * @param {string} [businessType] - 业务类型（当第一个参数为文件列表时使用）
 * @param {number} [businessId] - 业务ID（当第一个参数为文件列表时使用）
 * @param {Object} [options] - 额外选项
 * @param {number} [options.batchSize=5] - 每批上传的文件数量
 * @param {number} [options.delayMs=300] - 每批之间的延迟时间（毫秒）
 * @param {Function} [options.onProgress] - 进度回调 (completed, total, currentFile)
 * @returns {Promise} 上传结果数组
 */
export const uploadMultipleMedia = async (filesOrOptions, userId, businessType = 'MOMENT', businessId = null, options = {}) => {
  try {
    let files, finalUserId, finalBusinessType, finalBusinessId

    // 判断参数形式
    if (Array.isArray(filesOrOptions) || (filesOrOptions && typeof filesOrOptions === 'object' && filesOrOptions.length !== undefined)) {
      // 传统的多参数形式
      files = filesOrOptions
      finalUserId = userId
      finalBusinessType = businessType
      finalBusinessId = businessId
    } else {
      // 对象参数形式
      files = filesOrOptions.files
      finalUserId = filesOrOptions.userId
      finalBusinessType = filesOrOptions.relatedType || filesOrOptions.businessType || 'MOMENT'
      finalBusinessId = filesOrOptions.relatedId || filesOrOptions.businessId || null
      // 合并选项
      options = { ...options, ...filesOrOptions }
    }

    if (!files || files.length === 0) {
      throw new Error('请选择要上传的文件')
    }

    if (!finalUserId) {
      throw new Error('用户ID不能为空')
    }

    // 分批上传配置
    const batchSize = options.batchSize || 5  // 默认每批5个
    const delayMs = options.delayMs || 300     // 默认间隔300ms
    const onProgress = options.onProgress
    const fileArray = Array.from(files)
    const total = fileArray.length
    const results = []
    const failedUploads = []

    console.log(`开始分批上传，共 ${total} 个文件，每批 ${batchSize} 个`)

    // 分批处理
    for (let i = 0; i < fileArray.length; i += batchSize) {
      const batch = fileArray.slice(i, i + batchSize)
      const batchNumber = Math.floor(i / batchSize) + 1
      const totalBatches = Math.ceil(total / batchSize)

      console.log(`上传第 ${batchNumber}/${totalBatches} 批，共 ${batch.length} 个文件`)

      // 并行上传当前批次
      const batchPromises = batch.map((file, index) => {
        const fileIndex = i + index
        return uploadMedia(file, finalUserId, finalBusinessType, finalBusinessId)
          .then(result => {
            results[fileIndex] = result
            if (onProgress) {
              onProgress(results.filter(r => r !== undefined).length, total, file.name)
            }
            return result
          })
          .catch(error => {
            console.error(`文件 ${file.name} 上传失败:`, error)
            failedUploads.push({ file, error })
            results[fileIndex] = null
            if (onProgress) {
              onProgress(results.filter(r => r !== undefined).length, total, file.name)
            }
            return null
          })
      })

      await Promise.all(batchPromises)

      // 非最后一批则等待一段时间，避免请求过于密集
      if (i + batchSize < fileArray.length) {
        await new Promise(resolve => setTimeout(resolve, delayMs))
      }
    }

    // 统计结果
    const successfulResults = results.filter(r => r !== null)
    const failedCount = failedUploads.length

    if (failedCount > 0) {
      console.warn(`批量上传完成：成功 ${successfulResults.length} 个，失败 ${failedCount} 个`)
      // 如果全部失败，抛出异常
      if (successfulResults.length === 0) {
        throw new Error(`所有文件上传失败: ${failedUploads.map(f => f.file.name).join(', ')}`)
      }
    } else {
      console.log(`批量上传完成：成功 ${successfulResults.length} 个文件`)
    }

    return successfulResults
  } catch (error) {
    console.error('批量上传文件失败:', error)
    throw error
  }
}

/**
 * 获取媒体文件信息
 * @param {number} mediaId - 媒体文件ID
 * @returns {Promise} 媒体文件信息
 */
export const getMediaInfo = async (mediaId) => {
  try {
    if (!mediaId) {
      throw new Error('媒体文件ID不能为空')
    }

    // 通过代理服务器调用媒体微服务
    const response = await fetch(`${MEDIA_BASE_URL}/media/${mediaId}`)

    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(`获取媒体文件信息失败: ${response.status} - ${errorText}`)
    }

    return await response.json()
  } catch (error) {
    console.error('获取媒体文件信息失败:', error)
    throw error
  }
}

/**
 * 删除媒体文件
 * @param {number} mediaId - 媒体文件ID
 * @returns {Promise} 删除结果
 */
export const deleteMedia = async (mediaId) => {
  try {
    if (!mediaId) {
      throw new Error('媒体文件ID不能为空')
    }

    // 通过代理服务器调用媒体微服务
    const response = await fetch(`${MEDIA_BASE_URL}/media/${mediaId}`, {
      method: 'DELETE'
    })

    if (!response.ok) {
      const errorText = await response.text()
      throw new Error(`删除媒体文件失败: ${response.status} - ${errorText}`)
    }

    return await response.text()
  } catch (error) {
    console.error('删除媒体文件失败:', error)
    throw error
  }
}

/**
 * 批量关联媒体文件到业务实体
 * @param {Array} mediaIds - 媒体文件ID数组
 * @param {string} relatedType - 关联业务类型（MOMENT, USER_AVATAR, ACTIVITY, STATUS）
 * @param {number} newRelatedId - 新的业务ID
 * @returns {Promise} 关联结果响应
 */
export const batchAssociateMedia = async (mediaIds, relatedType, newRelatedId) => {
  try {
    if (!Array.isArray(mediaIds) || mediaIds.length === 0) {
      throw new Error('媒体ID数组不能为空')
    }

    if (!relatedType || !newRelatedId) {
      throw new Error('关联类型和关联ID不能为空')
    }

    const response = await apiClient({
      url: '/media/related/batch',
      method: 'PATCH',
      data: {
        mediaIds: mediaIds,
        relatedType: relatedType,
        newRelatedId: newRelatedId
      }
    })

    return response
  } catch (error) {
    console.error('批量关联媒体文件失败:', error)
    throw error
  }
}

/**
 * 获取关联的媒体文件列表
 * @param {string} relatedType - 关联业务类型（MOMENT, USER_AVATAR, ACTIVITY, STATUS）
 * @param {number} relatedId - 业务ID
 * @returns {Promise} 媒体文件列表响应
 */
export const getRelatedMedia = async (relatedType, relatedId) => {
  try {
    if (!relatedType || !relatedId) {
      throw new Error('关联类型和关联ID不能为空')
    }

    const response = await apiClient({
      url: `/media/related/${relatedType}/${relatedId}`,
      method: 'GET'
    })

    return response
  } catch (error) {
    console.error('获取关联媒体文件失败:', error)
    throw error
  }
}

/**
 * 删除关联的媒体文件
 * @param {string} relatedType - 关联业务类型（MOMENT, USER_AVATAR, ACTIVITY, STATUS）
 * @param {number} relatedId - 业务ID
 * @returns {Promise} 删除结果响应
 */
export const deleteRelatedMedia = async (relatedType, relatedId) => {
  try {
    if (!relatedType || !relatedId) {
      throw new Error('关联类型和关联ID不能为空')
    }

    const response = await apiClient({
      url: `/media/related/${relatedType}/${relatedId}`,
      method: 'DELETE'
    })

    return response
  } catch (error) {
    console.error('删除关联媒体文件失败:', error)
    throw error
  }
}

// 默认导出
export default {
  uploadMedia,
  uploadMultipleMedia,
  getMediaInfo,
  deleteMedia,
  batchAssociateMedia,
  getRelatedMedia,
  deleteRelatedMedia
}