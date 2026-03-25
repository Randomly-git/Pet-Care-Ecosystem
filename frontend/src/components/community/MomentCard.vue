<template>
  <div class="moment-card">
    <BaseCard class="moment-content">
      <!-- 用户信息头部 -->
      <div class="moment-header">
        <div class="user-info">
          <img
            :src="moment.avatarUrl || '/default-avatar.png'"
            :alt="displayAuthorName"
            class="user-avatar"
          />
          <div class="user-details">
            <h4 class="author-name">{{ displayAuthorName }}</h4>
            <p class="publish-time">{{ formatTime(moment.createdAt) }}</p>
          </div>
        </div>
        <BaseButton
          v-if="moment.userId === currentUserId"
          variant="text"
          size="small"
          @click="deleteMoment"
          class="delete-btn"
        >
          删除
        </BaseButton>
      </div>

      <!-- 动态内容 -->
      <div class="moment-body">
        <p class="moment-text">{{ moment.content }}</p>

        <!-- 媒体文件展示 -->
        <div v-if="moment.mediaUrls && moment.mediaUrls.length > 0" class="media-grid">
          <div
            v-for="(url, index) in moment.mediaUrls"
            :key="index"
            class="media-item"
            :class="{ 'single': moment.mediaUrls.length === 1 }"
          >
            <img
              v-if="isImage(url)"
              :src="url"
              :alt="`图片 ${index + 1}`"
              class="media-image"
              @click="previewImage(url)"
            />
            <div
              v-else
              class="media-video"
              @click="playVideo(url)"
            >
              <BaseIcon name="play-circle" size="32" />
              <span>视频</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 互动区域 -->
      <div class="moment-actions">
        <div class="action-item" @click="toggleLike">
          <BaseIcon
            :name="moment.isLiked ? 'heart-filled' : 'heart'"
            :color="moment.isLiked ? 'var(--color-error-500)' : 'var(--color-gray-500)'"
            size="20"
          />
          <span :class="{ 'liked': moment.isLiked }">
            {{ moment.likeCount || 0 }}
          </span>
        </div>

        <div class="action-item" @click="toggleComments">
          <BaseIcon name="message-circle" size="20" />
          <span>{{ moment.commentCount || 0 }}</span>
        </div>

        <div class="action-item" @click="shareMoment">
          <BaseIcon name="share" size="20" />
          <span>分享</span>
        </div>
      </div>

      <!-- 评论区 -->
      <div v-if="showComments" class="comments-section">
        <div class="comments-list">
          <CommentItem
            v-for="comment in comments"
            :key="comment.id"
            :comment="comment"
            @reply="handleReply"
            @delete="handleDeleteComment"
          />
        </div>

        <!-- 评论输入框 -->
        <div class="comment-input">
          <input
            v-model="newComment"
            :placeholder="replyingTo ? `回复 ${replyingTo.authorName}` : '写下你的评论...'"
            class="comment-field"
            @keyup.enter="submitComment"
          />
          <BaseButton
            variant="primary"
            size="small"
            @click="submitComment"
            :disabled="!newComment.trim()"
          >
            发送
          </BaseButton>
        </div>
      </div>
    </BaseCard>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { communityAPI } from '@/services/api'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseIcon from '@/components/base/BaseIcon.vue'
import CommentItem from './CommentItem.vue'

export default {
  name: 'MomentCard',
  components: {
    BaseCard,
    BaseButton,
    BaseIcon,
    CommentItem
  },
  props: {
    moment: {
      type: Object,
      required: true
    },
    currentUserId: {
      type: Number,
      required: true
    },
    /** 当前用户展示名，自己的帖子刷新后优先显示此昵称 */
    currentUserDisplayName: {
      type: String,
      default: ''
    }
  },
  emits: ['like-updated', 'comment-added', 'moment-deleted'],
  setup(props, { emit }) {
    const displayAuthorName = computed(() => {
      // 统一转为数字比较，避免类型不一致导致比较失败
      const currentUid = Number(props.currentUserId)
      const momentUid = Number(props.moment?.userId)

      // 如果是当前用户的动态，且有传入显示名，则使用显示名
      if (momentUid === currentUid && props.currentUserDisplayName) {
        return props.currentUserDisplayName
      }
      return props.moment?.authorName || '宠物爱好者'
    })

    const showComments = ref(false)
    const comments = ref([])
    const newComment = ref('')
    const replyingTo = ref(null)

    // 格式化时间
    const formatTime = (dateString) => {
      const date = new Date(dateString)
      const now = new Date()
      const diff = now - date

      // 小于1分钟
      if (diff < 60000) {
        return '刚刚'
      }

      // 小于1小时
      if (diff < 3600000) {
        return `${Math.floor(diff / 60000)}分钟前`
      }

      // 小于1天
      if (diff < 86400000) {
        return `${Math.floor(diff / 3600000)}小时前`
      }

      // 大于1天
      return `${Math.floor(diff / 86400000)}天前`
    }

    // 判断是否为图片
    const isImage = (url) => {
      const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp']
      return imageExtensions.some(ext => url.toLowerCase().includes(ext))
    }

    // 删除动态（需要传递 userId 进行权限验证）
    const deleteMoment = async () => {
      if (!confirm('确定要删除这条动态吗？')) {
        return
      }

      try {
        const success = await communityAPI.deleteMoment(props.moment.id, props.currentUserId)
        if (success) {
          emit('moment-deleted', props.moment.id)
        }
      } catch (error) {
        console.error('删除动态失败:', error)
        alert('删除失败，请重试')
      }
    }

    // 切换点赞状态
    const toggleLike = async () => {
      try {
        const success = await communityAPI.toggleLike(
          props.currentUserId,
          'MOMENT',
          props.moment.id
        )

        if (success) {
          emit('like-updated', {
            momentId: props.moment.id,
            isLiked: !props.moment.isLiked,
            likeCount: props.moment.isLiked ?
              props.moment.likeCount - 1 :
              props.moment.likeCount + 1
          })
        }
      } catch (error) {
        console.error('点赞操作失败:', error)
      }
    }

    // 切换评论区显示
    const toggleComments = async () => {
      showComments.value = !showComments.value

      if (showComments.value && comments.value.length === 0) {
        await loadComments()
      }
    }

    // 加载评论
    const loadComments = async () => {
      try {
        const commentsData = await communityAPI.getCommentsByMoment(props.moment.id)
        comments.value = commentsData || []
      } catch (error) {
        console.error('加载评论失败:', error)
        comments.value = []
      }
    }

    // 提交评论
    const submitComment = async () => {
      if (!newComment.value.trim()) {
        return
      }

      try {
        const commentData = await communityAPI.createComment(
          props.currentUserId,
          props.moment.id,
          newComment.value,
          replyingTo.value?.id
        )

        if (commentData) {
          comments.value.push(commentData)
          newComment.value = ''
          replyingTo.value = null

          emit('comment-added', {
            momentId: props.moment.id,
            commentCount: props.moment.commentCount + 1
          })
        }
      } catch (error) {
        console.error('发表评论失败:', error)
        alert('发表失败，请重试')
      }
    }

    // 处理回复
    const handleReply = (comment) => {
      replyingTo.value = comment
      newComment.value = ''
      // 聚焦到输入框
      setTimeout(() => {
        const input = document.querySelector('.comment-field')
        if (input) {
          input.focus()
        }
      }, 100)
    }

    // 处理删除评论
    const handleDeleteComment = async (commentId) => {
      try {
        const success = await communityAPI.deleteComment(commentId)
        if (success) {
          comments.value = comments.value.filter(c => c.id !== commentId)
          emit('comment-added', {
            momentId: props.moment.id,
            commentCount: Math.max(0, props.moment.commentCount - 1)
          })
        }
      } catch (error) {
        console.error('删除评论失败:', error)
      }
    }

    // 预览图片
    const previewImage = (url) => {
      // 可以在这里实现图片预览功能
      window.open(url, '_blank')
    }

    // 播放视频
    const playVideo = (url) => {
      // 可以在这里实现视频播放功能
      window.open(url, '_blank')
    }

    // 分享动态
    const shareMoment = () => {
      if (navigator.share) {
        navigator.share({
          title: `${displayAuthorName.value}的动态`,
          text: props.moment.content,
          url: window.location.href
        })
      } else {
        // 复制链接到剪贴板
        navigator.clipboard.writeText(window.location.href)
        alert('链接已复制到剪贴板')
      }
    }

    return {
      displayAuthorName,
      showComments,
      comments,
      newComment,
      replyingTo,
      formatTime,
      isImage,
      deleteMoment,
      toggleLike,
      toggleComments,
      submitComment,
      handleReply,
      handleDeleteComment,
      previewImage,
      playVideo,
      shareMoment
    }
  }
}
</script>

<style scoped>
.moment-card {
  margin-bottom: 1.5rem;
}

.moment-content {
  padding: 1.5rem;
}

.moment-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--color-gray-200);
}

.user-details {
  display: flex;
  flex-direction: column;
}

.author-name {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-gray-800);
}

.publish-time {
  margin: 0;
  font-size: 0.875rem;
  color: var(--color-gray-500);
}

.delete-btn {
  color: var(--color-error-500);
}

.moment-body {
  margin-bottom: 1rem;
}

.moment-text {
  margin: 0 0 1rem 0;
  font-size: 1rem;
  line-height: 1.5;
  color: var(--color-gray-700);
  white-space: pre-wrap;
}

.media-grid {
  display: grid;
  gap: 0.5rem;
  margin-top: 1rem;
}

.media-grid.single {
  grid-template-columns: 1fr;
}

.media-grid:not(.single) {
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
}

.media-item {
  border-radius: 8px;
  overflow: hidden;
  background-color: var(--color-gray-100);
}

.media-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.media-image:hover {
  transform: scale(1.02);
}

.media-video {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  cursor: pointer;
  color: var(--color-gray-500);
  gap: 0.5rem;
  transition: background-color 0.2s ease;
}

.media-video:hover {
  background-color: var(--color-gray-200);
}

.moment-actions {
  display: flex;
  align-items: center;
  gap: 2rem;
  padding-top: 1rem;
  border-top: 1px solid var(--color-gray-200);
}

.action-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  color: var(--color-gray-500);
  transition: color 0.2s ease;
  user-select: none;
}

.action-item:hover {
  color: var(--color-primary-500);
}

.action-item span.liked {
  color: var(--color-error-500);
}

.comments-section {
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--color-gray-200);
}

.comments-list {
  margin-bottom: 1rem;
}

.comment-input {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.comment-field {
  flex: 1;
  padding: 0.75rem;
  border: 1px solid var(--color-gray-300);
  border-radius: 20px;
  outline: none;
  font-size: 0.875rem;
  transition: border-color 0.2s ease;
}

.comment-field:focus {
  border-color: var(--color-primary-500);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .moment-content {
    padding: 1rem;
  }

  .moment-header {
    margin-bottom: 0.75rem;
  }

  .user-avatar {
    width: 40px;
    height: 40px;
  }

  .author-name {
    font-size: 0.875rem;
  }

  .publish-time {
    font-size: 0.75rem;
  }

  .moment-actions {
    gap: 1.5rem;
  }

  .media-grid:not(.single) {
    grid-template-columns: repeat(2, 1fr);
  }

  .comment-input {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>