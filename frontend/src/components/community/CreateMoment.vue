<template>
  <div class="create-moment">
    <BaseCard class="create-card">
      <div class="create-header">
        <img
          :src="userAvatar || '/default-avatar.png'"
          :alt="userName"
          class="user-avatar"
        />
        <div class="create-input">
          <textarea
            v-model="content"
            placeholder="分享你和宠物的故事..."
            class="content-textarea"
            :maxlength="maxLength"
            rows="3"
            @input="onContentChange"
          ></textarea>
        </div>
      </div>

      <!-- 媒体预览 -->
      <div v-if="mediaFiles.length > 0" class="media-preview">
        <div class="media-grid">
          <div
            v-for="(file, index) in mediaFiles"
            :key="index"
            class="media-item"
          >
            <img
              v-if="file.type.startsWith('image/')"
              :src="file.url"
              :alt="`图片 ${index + 1}`"
              class="preview-image"
            />
            <div v-else class="preview-video">
              <BaseIcon name="video" size="32" />
              <span>{{ file.name }}</span>
            </div>
            <BaseButton
              variant="text"
              size="small"
              @click="removeMedia(index)"
              class="remove-media"
            >
              <BaseIcon name="x" size="16" />
            </BaseButton>
          </div>
        </div>
      </div>

      <!-- 功能按钮 -->
      <div class="create-actions">
        <div class="action-buttons">
          <!-- 媒体上传 -->
          <div class="media-upload">
            <input
              ref="mediaInput"
              type="file"
              :accept="acceptTypes"
              :multiple="true"
              @change="onMediaSelect"
              style="display: none"
            />
            <BaseButton
              variant="text"
              size="small"
              @click="triggerMediaInput"
              :disabled="mediaFiles.length >= maxMedia"
            >
              <BaseIcon name="image" size="18" />
              <span>图片/视频</span>
            </BaseButton>
            <span class="media-count">{{ mediaFiles.length }}/{{ maxMedia }}</span>
          </div>

          <!-- 位置标记（预留） -->
          <BaseButton variant="text" size="small" disabled>
            <BaseIcon name="map-pin" size="18" />
            <span>位置</span>
          </BaseButton>

          <!-- 话题标签（预留） -->
          <BaseButton variant="text" size="small" disabled>
            <BaseIcon name="hash" size="18" />
            <span>话题</span>
          </BaseButton>
        </div>

        <div class="submit-area">
          <span v-if="content.length > maxLength * 0.8" class="char-count">
            {{ content.length }}/{{ maxLength }}
          </span>
          <BaseButton
            variant="primary"
            size="small"
            @click="publishMoment"
            :disabled="!canPublish"
            :loading="publishing"
          >
            发布
          </BaseButton>
        </div>
      </div>
    </BaseCard>

    <!-- 通知组件 -->
    <Notifications ref="notifications" />
  </div>
</template>

<script>
import { ref, computed, watch } from 'vue'
import { communityAPI, mediaAPI } from '@/services/api'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseIcon from '@/components/base/BaseIcon.vue'
import Notifications from '@/components/base/Notifications.vue'

export default {
  name: 'CreateMoment',
  components: {
    BaseCard,
    BaseButton,
    BaseIcon,
    Notifications
  },
  props: {
    userId: {
      type: Number,
      required: true
    },
    userName: {
      type: String,
      default: '用户'
    },
    userAvatar: {
      type: String,
      default: ''
    }
  },
  emits: ['moment-created'],
  setup(props, { emit }) {
    const content = ref('')
    const mediaFiles = ref([])
    const publishing = ref(false)
    const notifications = ref(null)
    const mediaInput = ref(null)

    const maxLength = 500
    const maxMedia = 9
    const acceptTypes = 'image/*,video/*'

    // 计算属性：是否可以发布
    const canPublish = computed(() => {
      return content.value.trim().length > 0 && !publishing.value
    })

    // 内容变化处理
    const onContentChange = () => {
      // 可以在这里添加自动保存草稿逻辑
    }

    // 触发媒体文件选择
    const triggerMediaInput = () => {
      mediaInput.value?.click()
    }

    // 媒体文件选择处理
    const onMediaSelect = async (event) => {
      const files = Array.from(event.target.files)
      const remainingSlots = maxMedia - mediaFiles.value.length
      const filesToProcess = files.slice(0, remainingSlots)

      for (const file of filesToProcess) {
        try {
          // 上传文件到媒体服务
          const response = await mediaAPI.uploadFile(file, (progress) => {
            console.log(`上传进度: ${progress}%`)
          })

          if (response && response.data) {
            mediaFiles.value.push({
              id: response.data.mediaId,
              name: file.name,
              type: file.type,
              size: file.size,
              url: response.data.fileUrl,
              file: file
            })
          }
        } catch (error) {
          console.error('上传文件失败:', error)
          showNotification('error', '上传失败', `${file.name} 上传失败，请重试`)
        }
      }

      // 清空input以允许重复选择相同文件
      event.target.value = ''
    }

    // 移除媒体文件
    const removeMedia = (index) => {
      mediaFiles.value.splice(index, 1)
    }

    // 发布动态
    const publishMoment = async () => {
      if (!canPublish.value) {
        return
      }

      publishing.value = true

      try {
        // 准备媒体ID列表
        const mediaIds = mediaFiles.value.map(file => file.id)

        // 调用社区API创建动态
        const response = await communityAPI.createMoment(
          props.userId,
          content.value.trim(),
          mediaIds
        )

        if (response) {
          // 清空表单
          content.value = ''
          mediaFiles.value = []

          // 显示成功通知
          showNotification('success', '发布成功', '动态已成功发布')

          // 触发事件
          emit('moment-created', response)
        } else {
          throw new Error('服务器响应异常')
        }
      } catch (error) {
        console.error('发布动态失败:', error)
        showNotification('error', '发布失败', error.message || '请重试')
      } finally {
        publishing.value = false
      }
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

    // 监听内容变化，可以用于草稿保存
    watch(content, (newContent) => {
      // 这里可以实现自动保存草稿的逻辑
      console.log('内容变化:', newContent.length)
    })

    return {
      content,
      mediaFiles,
      publishing,
      notifications,
      mediaInput,
      maxLength,
      maxMedia,
      acceptTypes,
      canPublish,
      onContentChange,
      triggerMediaInput,
      onMediaSelect,
      removeMedia,
      publishMoment
    }
  }
}
</script>

<style scoped>
.create-moment {
  width: 100%;
  margin-bottom: 1.5rem;
}

.create-card {
  padding: 1.5rem;
}

.create-header {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--color-gray-200);
  flex-shrink: 0;
}

.create-input {
  flex: 1;
}

.content-textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid var(--color-gray-200);
  border-radius: 12px;
  resize: none;
  outline: none;
  font-size: 1rem;
  line-height: 1.5;
  font-family: inherit;
  background-color: var(--color-gray-50);
  transition: border-color 0.2s ease;
}

.content-textarea:focus {
  border-color: var(--color-primary-500);
  background-color: var(--color-white);
}

.content-textarea::placeholder {
  color: var(--color-gray-400);
}

.media-preview {
  margin-bottom: 1rem;
}

.media-grid {
  display: grid;
  gap: 0.5rem;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
}

.media-item {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  background-color: var(--color-gray-100);
  aspect-ratio: 1;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-video {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--color-gray-500);
  gap: 0.5rem;
  padding: 0.5rem;
  font-size: 0.75rem;
  text-align: center;
}

.remove-media {
  position: absolute;
  top: 0.25rem;
  right: 0.25rem;
  background-color: rgba(0, 0, 0, 0.5);
  color: white;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.create-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.media-upload {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.media-count {
  font-size: 0.75rem;
  color: var(--color-gray-500);
}

.submit-area {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.char-count {
  font-size: 0.75rem;
  color: var(--color-gray-500);
}

.char-count {
  font-size: 0.75rem;
  color: var(--color-gray-500);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .create-card {
    padding: 1rem;
  }

  .create-header {
    gap: 0.75rem;
  }

  .user-avatar {
    width: 40px;
    height: 40px;
  }

  .content-textarea {
    font-size: 0.875rem;
  }

  .media-grid {
    grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  }

  .create-actions {
    flex-direction: column;
    align-items: stretch;
    gap: 1rem;
  }

  .action-buttons {
    justify-content: center;
  }

  .submit-area {
    justify-content: space-between;
  }
}

/* 动画效果 */
.create-moment {
  animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.media-item {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}
</style>