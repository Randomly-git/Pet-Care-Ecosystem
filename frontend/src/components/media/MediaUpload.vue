<template>
  <div class="media-upload">
    <div
      class="upload-area"
      :class="{
        'drag-over': isDragOver,
        'uploading': isUploading,
        'error': hasError
      }"
      @click="triggerFileInput"
      @dragover.prevent="onDragOver"
      @dragleave.prevent="onDragLeave"
      @drop.prevent="onDrop"
    >

      <!-- 上传区域内容 -->
      <div v-if="!isUploading && !uploadedFile" class="upload-placeholder">
        <BaseIcon name="upload" size="48" color="var(--color-primary-400)" />
        <p class="upload-text">点击或拖拽文件到此处上传</p>
        <p class="upload-hint">支持 JPG、PNG、GIF、MP4 格式，最大 100MB</p>
      </div>

      <!-- 上传中状态 -->
      <div v-if="isUploading" class="uploading-content">
        <LoadingSpinner size="large" />
        <p class="upload-progress">上传中... {{ uploadProgress }}%</p>
        <div class="progress-bar">
          <div
            class="progress-fill"
            :style="{ width: uploadProgress + '%' }"
          ></div>
        </div>
      </div>

      <!-- 上传完成状态 -->
      <div v-if="uploadedFile" class="uploaded-content">
        <div v-if="isImage(uploadedFile)" class="image-preview">
          <img :src="uploadedFile.fileUrl" :alt="uploadedFile.fileName" />
        </div>
        <div v-else class="video-preview">
          <BaseIcon name="video" size="32" color="var(--color-primary-400)" />
          <p>{{ uploadedFile.fileName }}</p>
        </div>
        <div class="uploaded-info">
          <p class="file-name">{{ uploadedFile.fileName }}</p>
          <p class="file-size">{{ formatFileSize(uploadedFile.fileSize) }}</p>
        </div>
        <BaseButton
          variant="text"
          size="small"
          @click.stop="removeFile"
          class="remove-btn"
        >
          移除
        </BaseButton>
      </div>

      <!-- 错误状态 -->
      <div v-if="hasError" class="error-content">
        <BaseIcon name="error" size="48" color="var(--color-error-500)" />
        <p class="error-message">{{ errorMessage }}</p>
        <BaseButton
          variant="primary"
          size="small"
          @click.stop="retryUpload"
        >
          重试
        </BaseButton>
      </div>

      <!-- 隐藏的文件输入框 -->
      <input
        ref="fileInput"
        type="file"
        :accept="acceptTypes"
        :multiple="multiple"
        @change="onFileSelect"
        style="display: none"
      />
    </div>

    <!-- 多文件上传列表 -->
    <div v-if="multiple && uploadList.length > 0" class="upload-list">
      <h4>已上传文件 ({{ uploadList.length }}/{{ maxFiles }})</h4>
      <div class="file-items">
        <div
          v-for="(file, index) in uploadList"
          :key="index"
          class="file-item"
        >
          <div class="file-preview">
            <img
              v-if="isImage(file)"
              :src="file.fileUrl"
              :alt="file.fileName"
              class="thumbnail"
            />
            <BaseIcon
              v-else
              name="video"
              size="24"
              color="var(--color-primary-400)"
            />
          </div>
          <div class="file-info">
            <p class="file-name">{{ file.fileName }}</p>
            <p class="file-size">{{ formatFileSize(file.fileSize) }}</p>
          </div>
          <BaseButton
            variant="text"
            size="small"
            @click="removeFileFromList(index)"
          >
            删除
          </BaseButton>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, watch } from 'vue'
import { mediaAPI } from '@/services/api'
import BaseIcon from '@/components/base/BaseIcon.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import LoadingSpinner from '@/components/base/LoadingSpinner.vue'

export default {
  name: 'MediaUpload',
  components: {
    BaseIcon,
    BaseButton,
    LoadingSpinner
  },
  props: {
    // 是否支持多文件上传
    multiple: {
      type: Boolean,
      default: false
    },
    // 最大文件数量
    maxFiles: {
      type: Number,
      default: 9
    },
    // 最大文件大小 (MB)
    maxSize: {
      type: Number,
      default: 100
    },
    // 接受的文件类型
    accept: {
      type: String,
      default: 'image/*,video/*'
    },
    // 默认值
    modelValue: {
      type: [Array, Object, null],
      default: null
    }
  },
  emits: ['update:modelValue', 'upload-success', 'upload-error', 'progress'],
  setup(props, { emit }) {
    const fileInput = ref(null)
    const isUploading = ref(false)
    const isDragOver = ref(false)
    const hasError = ref(false)
    const errorMessage = ref('')
    const uploadProgress = ref(0)
    const uploadList = ref([])

    // 计算属性
    const acceptTypes = computed(() => props.accept)
    const uploadedFile = computed(() => {
      return props.multiple ? uploadList.value[0] : props.modelValue
    })

    // 监听 modelValue 变化
    watch(() => props.modelValue, (newValue) => {
      if (props.multiple && Array.isArray(newValue)) {
        uploadList.value = newValue
      }
    }, { immediate: true })

    // 触发文件选择
    const triggerFileInput = () => {
      if (!isUploading.value) {
        fileInput.value?.click()
      }
    }

    // 文件选择处理
    const onFileSelect = (event) => {
      const files = Array.from(event.target.files)
      handleFiles(files)
    }

    // 拖拽处理
    const onDragOver = (event) => {
      event.preventDefault()
      isDragOver.value = true
    }

    const onDragLeave = (event) => {
      event.preventDefault()
      isDragOver.value = false
    }

    const onDrop = (event) => {
      event.preventDefault()
      isDragOver.value = false
      const files = Array.from(event.dataTransfer.files)
      handleFiles(files)
    }

    // 文件处理
    const handleFiles = async (files) => {
      // 过滤和验证文件
      const validFiles = files.filter(file => validateFile(file))

      if (validFiles.length === 0) {
        return
      }

      if (props.multiple) {
        // 多文件模式
        const remainingSlots = props.maxFiles - uploadList.value.length
        const filesToUpload = validFiles.slice(0, remainingSlots)

        for (const file of filesToUpload) {
          await uploadFile(file)
        }
      } else {
        // 单文件模式
        await uploadFile(validFiles[0])
      }
    }

    // 文件验证
    const validateFile = (file) => {
      // 检查文件类型
      const validTypes = ['image/jpeg', 'image/png', 'image/gif', 'video/mp4']
      if (!validTypes.includes(file.type)) {
        showError('不支持的文件类型，请选择 JPG、PNG、GIF 或 MP4 文件')
        return false
      }

      // 检查文件大小
      const maxSizeBytes = props.maxSize * 1024 * 1024
      if (file.size > maxSizeBytes) {
        showError(`文件大小不能超过 ${props.maxSize}MB`)
        return false
      }

      // 多文件模式下检查数量限制
      if (props.multiple && uploadList.value.length >= props.maxFiles) {
        showError(`最多只能上传 ${props.maxFiles} 个文件`)
        return false
      }

      return true
    }

    // 上传文件
    const uploadFile = async (file) => {
      try {
        isUploading.value = true
        hasError.value = false
        uploadProgress.value = 0

        // 创建上传进度回调
        const onProgress = (progress) => {
          uploadProgress.value = Math.round(progress)
          emit('progress', progress)
        }

        // 调用媒体上传API
        const response = await mediaAPI.uploadFile(file, onProgress)

        if (response && response.data) {
          const uploadedMedia = {
            mediaId: response.data.mediaId,
            fileName: file.name,
            fileSize: file.size,
            fileType: file.type,
            fileUrl: response.data.fileUrl,
            uploadTime: new Date().toISOString()
          }

          if (props.multiple) {
            uploadList.value.push(uploadedMedia)
            emit('update:modelValue', uploadList.value)
          } else {
            emit('update:modelValue', uploadedMedia)
          }

          emit('upload-success', uploadedMedia)
          return uploadedMedia
        } else {
          throw new Error('上传失败：服务器响应异常')
        }

      } catch (error) {
        console.error('上传失败:', error)
        const errorMsg = error.message || '上传失败，请重试'
        showError(errorMsg)
        emit('upload-error', error)
        return null

      } finally {
        isUploading.value = false
        uploadProgress.value = 0
      }
    }

    // 显示错误
    const showError = (message) => {
      hasError.value = true
      errorMessage.value = message
      setTimeout(() => {
        hasError.value = false
        errorMessage.value = ''
      }, 5000)
    }

    // 重试上传
    const retryUpload = () => {
      hasError.value = false
      errorMessage.value = ''
      triggerFileInput()
    }

    // 移除文件
    const removeFile = () => {
      if (props.multiple) {
        uploadList.value = []
        emit('update:modelValue', [])
      } else {
        emit('update:modelValue', null)
      }

      // 重置文件输入框
      if (fileInput.value) {
        fileInput.value.value = ''
      }
    }

    // 从列表中移除文件
    const removeFileFromList = (index) => {
      uploadList.value.splice(index, 1)
      emit('update:modelValue', uploadList.value)
    }

    // 判断是否为图片
    const isImage = (file) => {
      return file.fileType && file.fileType.startsWith('image/')
    }

    // 格式化文件大小
    const formatFileSize = (bytes) => {
      if (bytes === 0) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    }

    return {
      fileInput,
      isUploading,
      isDragOver,
      hasError,
      errorMessage,
      uploadProgress,
      uploadList,
      acceptTypes,
      uploadedFile,
      triggerFileInput,
      onFileSelect,
      onDragOver,
      onDragLeave,
      onDrop,
      retryUpload,
      removeFile,
      removeFileFromList,
      isImage,
      formatFileSize
    }
  }
}
</script>

<style scoped>
.media-upload {
  width: 100%;
}

.upload-area {
  border: 2px dashed var(--color-gray-300);
  border-radius: 8px;
  padding: 2rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  background-color: var(--color-gray-50);
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.upload-area:hover {
  border-color: var(--color-primary-400);
  background-color: var(--color-primary-50);
}

.upload-area.drag-over {
  border-color: var(--color-primary-500);
  background-color: var(--color-primary-100);
  transform: scale(1.02);
}

.upload-area.uploading {
  border-color: var(--color-primary-400);
  background-color: var(--color-primary-50);
  cursor: not-allowed;
}

.upload-area.error {
  border-color: var(--color-error-500);
  background-color: var(--color-error-50);
}

.upload-placeholder {
  color: var(--color-gray-600);
}

.upload-text {
  font-size: 1rem;
  font-weight: 500;
  margin: 1rem 0 0.5rem 0;
  color: var(--color-gray-700);
}

.upload-hint {
  font-size: 0.875rem;
  color: var(--color-gray-500);
  margin: 0;
}

.uploading-content {
  text-align: center;
}

.upload-progress {
  margin: 1rem 0 0.5rem 0;
  font-weight: 500;
  color: var(--color-primary-600);
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

.uploaded-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  width: 100%;
}

.image-preview img {
  max-width: 100%;
  max-height: 200px;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.video-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem;
  background-color: var(--color-gray-100);
  border-radius: 8px;
  min-width: 200px;
}

.uploaded-info {
  text-align: center;
}

.file-name {
  font-weight: 500;
  color: var(--color-gray-800);
  margin: 0 0 0.25rem 0;
  word-break: break-all;
}

.file-size {
  font-size: 0.875rem;
  color: var(--color-gray-500);
  margin: 0;
}

.remove-btn {
  margin-top: 0.5rem;
}

.error-content {
  text-align: center;
}

.error-message {
  color: var(--color-error-600);
  margin: 1rem 0;
  font-weight: 500;
}

.upload-list {
  margin-top: 2rem;
}

.upload-list h4 {
  margin: 0 0 1rem 0;
  color: var(--color-gray-700);
  font-size: 1rem;
  font-weight: 500;
}

.file-items {
  display: grid;
  gap: 1rem;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
}

.file-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  background-color: var(--color-white);
  border: 1px solid var(--color-gray-200);
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.file-preview {
  flex-shrink: 0;
}

.thumbnail {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 6px;
}

.file-info {
  flex: 1;
  min-width: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .upload-area {
    padding: 1.5rem 1rem;
    min-height: 150px;
  }

  .upload-text {
    font-size: 0.875rem;
  }

  .upload-hint {
    font-size: 0.75rem;
  }

  .file-items {
    grid-template-columns: 1fr;
  }
}

/* 动画效果 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.uploaded-content {
  animation: fadeIn 0.3s ease-out;
}

.error-content {
  animation: fadeIn 0.3s ease-out;
}
</style>