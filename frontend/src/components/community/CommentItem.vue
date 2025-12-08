<template>
  <div class="comment-item" :class="{ 'reply': isReply }">
    <div class="comment-content">
      <div class="comment-header">
        <img
          :src="comment.avatarUrl || '/default-avatar.png'"
          :alt="comment.authorName"
          class="comment-avatar"
        />
        <div class="comment-info">
          <h5 class="comment-author">{{ comment.authorName }}</h5>
          <p class="comment-time">{{ formatTime(comment.createdAt) }}</p>
        </div>
        <BaseButton
          v-if="comment.userId === currentUserId"
          variant="text"
          size="small"
          @click="deleteComment"
          class="delete-btn"
        >
          删除
        </BaseButton>
      </div>

      <div class="comment-body">
        <p v-if="comment.replyToUserName" class="reply-to">
          回复 <span class="reply-target">{{ comment.replyToUserName }}</span>
        </p>
        <p class="comment-text">{{ comment.content }}</p>
      </div>

      <div class="comment-actions">
        <div class="action-item" @click="toggleLike">
          <BaseIcon
            :name="comment.isLiked ? 'heart-filled' : 'heart'"
            :color="comment.isLiked ? 'var(--color-error-500)' : 'var(--color-gray-500)'"
            size="16"
          />
          <span :class="{ 'liked': comment.isLiked }">
            {{ comment.likeCount || 0 }}
          </span>
        </div>

        <div class="action-item" @click="reply">
          <BaseIcon name="message-circle" size="16" />
          <span>回复</span>
        </div>
      </div>
    </div>

    <!-- 嵌套回复 -->
    <div v-if="comment.replies && comment.replies.length > 0" class="replies">
      <CommentItem
        v-for="reply in comment.replies"
        :key="reply.id"
        :comment="reply"
        :current-user-id="currentUserId"
        :is-reply="true"
        @like="handleLike"
        @reply="$emit('reply', reply)"
        @delete="$emit('delete', reply.id)"
      />
    </div>
  </div>
</template>

<script>
import { computed } from 'vue'
import { communityAPI } from '@/services/api'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseIcon from '@/components/base/BaseIcon.vue'

export default {
  name: 'CommentItem',
  components: {
    BaseButton,
    BaseIcon
  },
  props: {
    comment: {
      type: Object,
      required: true
    },
    currentUserId: {
      type: Number,
      required: true
    },
    isReply: {
      type: Boolean,
      default: false
    }
  },
  emits: ['like', 'reply', 'delete'],
  setup(props, { emit }) {
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

    // 删除评论
    const deleteComment = async () => {
      if (!confirm('确定要删除这条评论吗？')) {
        return
      }

      try {
        const success = await communityAPI.deleteComment(props.comment.id)
        if (success) {
          emit('delete', props.comment.id)
        }
      } catch (error) {
        console.error('删除评论失败:', error)
        alert('删除失败，请重试')
      }
    }

    // 切换点赞状态
    const toggleLike = async () => {
      try {
        const success = await communityAPI.toggleLike(
          props.currentUserId,
          'COMMENT',
          props.comment.id
        )

        if (success) {
          emit('like', {
            commentId: props.comment.id,
            isLiked: !props.comment.isLiked,
            likeCount: props.comment.isLiked ?
              props.comment.likeCount - 1 :
              props.comment.likeCount + 1
          })
        }
      } catch (error) {
        console.error('点赞操作失败:', error)
      }
    }

    // 回复评论
    const reply = () => {
      emit('reply', props.comment)
    }

    // 处理嵌套评论的点赞
    const handleLike = (likeData) => {
      emit('like', likeData)
    }

    return {
      formatTime,
      deleteComment,
      toggleLike,
      reply,
      handleLike
    }
  }
}
</script>

<style scoped>
.comment-item {
  margin-bottom: 1rem;
}

.comment-item.reply {
  margin-left: 2rem;
  margin-top: 0.75rem;
  padding-left: 1rem;
  border-left: 2px solid var(--color-gray-200);
}

.comment-content {
  background-color: var(--color-gray-50);
  border-radius: 8px;
  padding: 1rem;
}

.comment-header {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--color-gray-200);
}

.comment-info {
  flex: 1;
}

.comment-author {
  margin: 0;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-gray-800);
}

.comment-time {
  margin: 0;
  font-size: 0.75rem;
  color: var(--color-gray-500);
}

.delete-btn {
  color: var(--color-error-500);
  font-size: 0.75rem;
}

.comment-body {
  margin-bottom: 0.75rem;
}

.reply-to {
  margin: 0 0 0.25rem 0;
  font-size: 0.875rem;
  color: var(--color-gray-500);
}

.reply-target {
  color: var(--color-primary-500);
  font-weight: 500;
}

.comment-text {
  margin: 0;
  font-size: 0.875rem;
  line-height: 1.4;
  color: var(--color-gray-700);
  white-space: pre-wrap;
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  cursor: pointer;
  color: var(--color-gray-500);
  font-size: 0.75rem;
  transition: color 0.2s ease;
  user-select: none;
}

.action-item:hover {
  color: var(--color-primary-500);
}

.action-item span.liked {
  color: var(--color-error-500);
}

.replies {
  margin-top: 0.75rem;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .comment-item.reply {
    margin-left: 1rem;
    padding-left: 0.5rem;
  }

  .comment-content {
    padding: 0.75rem;
  }

  .comment-avatar {
    width: 32px;
    height: 32px;
  }

  .comment-author {
    font-size: 0.8rem;
  }

  .comment-time {
    font-size: 0.7rem;
  }

  .comment-text {
    font-size: 0.8rem;
  }

  .action-item {
    gap: 0.25rem;
  }
}
</style>