<template>
  <div class="media-upload-test">
    <div class="page-header">
      <h1>媒体上传功能测试</h1>
      <p>测试与媒体微服务的集成，支持图片和视频上传</p>
    </div>

    <div class="test-sections">
      <!-- 单文件上传测试 -->
      <section class="test-section">
        <h2>单文件上传测试</h2>
        <div class="upload-container">
          <MediaUpload
            v-model="singleFile"
            @upload-success="onSingleUploadSuccess"
            @upload-error="onUploadError"
            @progress="onProgress"
          />
        </div>
        <div v-if="singleFile" class="upload-result">
          <h3>上传结果:</h3>
          <BaseCard class="result-card">
            <div class="result-item">
              <strong>媒体ID:</strong> {{ singleFile.mediaId }}
            </div>
            <div class="result-item">
              <strong>文件名:</strong> {{ singleFile.fileName }}
            </div>
            <div class="result-item">
              <strong>文件大小:</strong> {{ formatFileSize(singleFile.fileSize) }}
            </div>
            <div class="result-item">
              <strong>文件类型:</strong> {{ singleFile.fileType }}
            </div>
            <div class="result-item">
              <strong>文件URL:</strong>
              <a :href="singleFile.fileUrl" target="_blank" class="file-link">
                {{ singleFile.fileUrl }}
              </a>
            </div>
            <div class="result-item">
              <strong>上传时间:</strong> {{ formatDate(singleFile.uploadTime) }}
            </div>
          </BaseCard>
        </div>
      </section>

      <!-- 多文件上传测试 -->
      <section class="test-section">
        <h2>多文件上传测试</h2>
        <div class="upload-container">
          <MediaUpload
            v-model="multipleFiles"
            :multiple="true"
            :max-files="5"
            @upload-success="onMultipleUploadSuccess"
            @upload-error="onUploadError"
            @progress="onProgress"
          />
        </div>
        <div v-if="multipleFiles.length > 0" class="upload-result">
          <h3>上传结果 ({{ multipleFiles.length }} 个文件):</h3>
          <div class="multiple-results">
            <BaseCard
              v-for="(file, index) in multipleFiles"
              :key="file.mediaId"
              class="result-card"
            >
              <div class="result-header">
                <strong>文件 {{ index + 1 }}</strong>
                <BaseButton
                  variant="text"
                  size="small"
                  @click="removeMultipleFile(index)"
                >
                  删除
                </BaseButton>
              </div>
              <div class="result-item">
                <strong>媒体ID:</strong> {{ file.mediaId }}
              </div>
              <div class="result-item">
                <strong>文件名:</strong> {{ file.fileName }}
              </div>
              <div class="result-item">
                <strong>文件大小:</strong> {{ formatFileSize(file.fileSize) }}
              </div>
              <div v-if="file.fileUrl" class="result-item">
                <strong>预览:</strong>
                <img
                  v-if="file.fileType.startsWith('image/')"
                  :src="file.fileUrl"
                  :alt="file.fileName"
                  class="preview-image"
                />
                <div v-else class="preview-placeholder">
                  <BaseIcon name="video" size="24" />
                  <span>视频文件</span>
                </div>
              </div>
            </BaseCard>
          </div>
        </div>
      </section>

      <!-- API测试结果 -->
      <section class="test-section">
        <h2>API测试结果</h2>
        <BaseCard class="api-results">
          <div class="api-result">
            <strong>上传进度:</strong> {{ uploadProgress }}%
            <div class="progress-bar">
              <div
                class="progress-fill"
                :style="{ width: uploadProgress + '%' }"
              ></div>
            </div>
          </div>
          <div v-if="lastError" class="api-result error">
            <strong>最后错误:</strong>
            <p class="error-message">{{ lastError }}</p>
          </div>
          <div class="api-result">
            <strong>成功上传:</strong> {{ successCount }} 次
          </div>
          <div class="api-result">
            <strong>失败次数:</strong> {{ errorCount }} 次
          </div>
        </BaseCard>
      </section>
    </div>

    <!-- 通知组件 -->
    <Notifications ref="notifications" />
  </div>
</template>

<script>
import { ref } from 'vue'
import { mediaAPI } from '@/services/api'
import MediaUpload from '@/components/media/MediaUpload.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseIcon from '@/components/base/BaseIcon.vue'
import Notifications from '@/components/base/Notifications.vue'

export default {
  name: 'MediaUploadTest',
  components: {
    MediaUpload,
    BaseCard,
    BaseButton,
    BaseIcon,
    Notifications
  },
  setup() {
    const singleFile = ref(null)
    const multipleFiles = ref([])
    const uploadProgress = ref(0)
    const lastError = ref('')
    const successCount = ref(0)
    const errorCount = ref(0)
    const notifications = ref(null)

    // 单文件上传成功
    const onSingleUploadSuccess = (file) => {
      console.log('单文件上传成功:', file)
      successCount.value++
      showNotification('success', '文件上传成功', `媒体ID: ${file.mediaId}`)
    }

    // 多文件上传成功
    const onMultipleUploadSuccess = (file) => {
      console.log('多文件上传成功:', file)
      successCount.value++
      showNotification('success', '文件上传成功', `${file.fileName}`)
    }

    // 上传错误
    const onUploadError = (error) => {
      console.error('上传失败:', error)
      errorCount.value++
      lastError.value = error.message || '未知错误'
      showNotification('error', '上传失败', error.message || '请重试')
    }

    // 上传进度
    const onProgress = (progress) => {
      uploadProgress.value = progress
    }

    // 移除多文件列表中的文件
    const removeMultipleFile = (index) => {
      multipleFiles.value.splice(index, 1)
    }

    // 显示通知
    const showNotification = (type, title, message) => {
      if (notifications.value) {
        notifications.value.addNotification({
          type,
          title,
          message,
          duration: 3000
        })
      }
    }

    // 格式化文件大小
    const formatFileSize = (bytes) => {
      if (bytes === 0) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    }

    // 格式化日期
    const formatDate = (dateString) => {
      return new Date(dateString).toLocaleString('zh-CN')
    }

    // 测试API连接
    const testAPIConnection = async () => {
      try {
        const response = await fetch('http://localhost:8081/api/media/types')
        if (response.ok) {
          showNotification('success', 'API连接成功', '媒体微服务正常运行')
        } else {
          throw new Error(`HTTP ${response.status}`)
        }
      } catch (error) {
        console.error('API连接测试失败:', error)
        showNotification('error', 'API连接失败', '请检查媒体微服务是否启动')
      }
    }

    // 页面加载时测试API连接
    setTimeout(() => {
      testAPIConnection()
    }, 1000)

    return {
      singleFile,
      multipleFiles,
      uploadProgress,
      lastError,
      successCount,
      errorCount,
      notifications,
      onSingleUploadSuccess,
      onMultipleUploadSuccess,
      onUploadError,
      onProgress,
      removeMultipleFile,
      formatFileSize,
      formatDate
    }
  }
}
</script>

<style scoped>
.media-upload-test {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

.page-header {
  text-align: center;
  margin-bottom: 3rem;
}

.page-header h1 {
  color: var(--color-gray-800);
  margin-bottom: 0.5rem;
}

.page-header p {
  color: var(--color-gray-600);
  margin: 0;
}

.test-sections {
  display: flex;
  flex-direction: column;
  gap: 3rem;
}

.test-section {
  background-color: var(--color-white);
  padding: 2rem;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.test-section h2 {
  color: var(--color-gray-800);
  margin: 0 0 1.5rem 0;
  font-size: 1.5rem;
  font-weight: 600;
}

.upload-container {
  margin-bottom: 2rem;
}

.upload-result {
  margin-top: 2rem;
}

.upload-result h3 {
  color: var(--color-gray-700);
  margin: 0 0 1rem 0;
  font-size: 1.25rem;
  font-weight: 500;
}

.result-card {
  margin-bottom: 1rem;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.result-item {
  margin-bottom: 0.75rem;
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
}

.result-item strong {
  color: var(--color-gray-700);
  min-width: 100px;
  flex-shrink: 0;
}

.file-link {
  color: var(--color-primary-600);
  text-decoration: none;
  word-break: break-all;
}

.file-link:hover {
  text-decoration: underline;
}

.multiple-results {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
}

.preview-image {
  max-width: 100%;
  max-height: 150px;
  object-fit: cover;
  border-radius: 6px;
  margin-top: 0.5rem;
}

.preview-placeholder {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--color-gray-500);
  font-size: 0.875rem;
  margin-top: 0.5rem;
  padding: 0.5rem;
  background-color: var(--color-gray-100);
  border-radius: 6px;
}

.api-results {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.api-result {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.api-result strong {
  color: var(--color-gray-700);
  min-width: 120px;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background-color: var(--color-gray-200);
  border-radius: 4px;
  overflow: hidden;
  margin-top: 0.5rem;
}

.progress-fill {
  height: 100%;
  background-color: var(--color-primary-500);
  transition: width 0.3s ease;
}

.api-result.error {
  border-left: 4px solid var(--color-error-500);
  padding-left: 1rem;
}

.error-message {
  color: var(--color-error-600);
  margin: 0;
  font-family: monospace;
  font-size: 0.875rem;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .media-upload-test {
    padding: 1rem;
  }

  .test-section {
    padding: 1.5rem;
  }

  .test-section h2 {
    font-size: 1.25rem;
  }

  .multiple-results {
    grid-template-columns: 1fr;
  }

  .result-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }

  .result-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .result-item strong {
    min-width: auto;
  }
}

/* 动画效果 */
.test-section {
  animation: fadeInUp 0.5s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.result-card {
  animation: fadeIn 0.3s ease-out;
}
</style>