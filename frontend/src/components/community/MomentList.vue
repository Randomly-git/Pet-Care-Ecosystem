<template>
  <div class="moment-list">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <LoadingSpinner size="large" />
      <p>加载中...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="moments.length === 0" class="empty-state">
      <BaseIcon name="inbox" size="64" color="var(--color-gray-300)" />
      <h3>暂无动态</h3>
      <p>快来发布第一条动态吧！</p>
    </div>

    <!-- 动态列表 -->
    <div v-else class="moments-container">
      <TransitionGroup name="moment" tag="div">
        <MomentCard
          v-for="moment in moments"
          :key="moment.id"
          :moment="moment"
          :current-user-id="currentUserId"
          :current-user-display-name="currentUserDisplayName"
          @like-updated="handleLikeUpdated"
          @comment-added="handleCommentAdded"
          @moment-deleted="handleMomentDeleted"
        />
      </TransitionGroup>
    </div>

    <!-- 加载更多 -->
    <div v-if="hasMore && !loading" class="load-more">
      <BaseButton
        variant="outline"
        @click="loadMore"
        :loading="loadingMore"
      >
        加载更多
      </BaseButton>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import { communityAPI } from '@/services/api'
import MomentCard from './MomentCard.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseIcon from '@/components/base/BaseIcon.vue'
import LoadingSpinner from '@/components/base/LoadingSpinner.vue'

export default {
  name: 'MomentList',
  components: {
    MomentCard,
    BaseButton,
    BaseIcon,
    LoadingSpinner
  },
  props: {
    userId: {
      type: Number,
      required: true
    },
    currentUserId: {
      type: Number,
      required: true
    },
    /** 当前用户展示名，用于自己的帖子在刷新后仍显示昵称而非「用户X」 */
    currentUserDisplayName: {
      type: String,
      default: ''
    },
    pageSize: {
      type: Number,
      default: 10
    }
  },
  emits: ['moment-updated'],
  setup(props, { emit }) {
    const currentUserDisplayName = computed(() => props.currentUserDisplayName || '')
    const moments = ref([])
    const loading = ref(false)
    const loadingMore = ref(false)
    const currentPage = ref(0)
    const hasMore = ref(true)

    // 加载动态列表
    const loadMoments = async (page = 0, append = false) => {
      const loadingRef = page === 0 ? loading : loadingMore
      loadingRef.value = true

      try {
        // 这里需要API支持分页，目前先获取所有数据
        const momentsData = await communityAPI.getMomentsByUser(props.userId)

        if (momentsData && Array.isArray(momentsData)) {
          const sortedMoments = momentsData.sort((a, b) =>
            new Date(b.createdAt) - new Date(a.createdAt)
          )

          if (append) {
            moments.value = [...moments.value, ...sortedMoments]
          } else {
            moments.value = sortedMoments
          }

          // 简单的分页逻辑（如果API不支持分页）
          const startIndex = page * props.pageSize
          const endIndex = startIndex + props.pageSize
          hasMore.value = sortedMoments.length > endIndex

          if (append) {
            currentPage.value = page
          }
        }
      } catch (error) {
        console.error('加载动态失败:', error)
        if (page === 0) {
          moments.value = []
        }
      } finally {
        loadingRef.value = false
      }
    }

    // 加载更多
    const loadMore = async () => {
      if (!hasMore.value || loadingMore.value) {
        return
      }

      await loadMoments(currentPage.value + 1, true)
    }

    // 处理点赞更新
    const handleLikeUpdated = (likeData) => {
      const momentIndex = moments.value.findIndex(m => m.id === likeData.momentId)
      if (momentIndex !== -1) {
        moments.value[momentIndex].isLiked = likeData.isLiked
        moments.value[momentIndex].likeCount = likeData.likeCount
      }
    }

    // 处理评论添加
    const handleCommentAdded = (commentData) => {
      const momentIndex = moments.value.findIndex(m => m.id === commentData.momentId)
      if (momentIndex !== -1) {
        moments.value[momentIndex].commentCount = commentData.commentCount
      }
    }

    // 处理动态删除
    const handleMomentDeleted = (momentId) => {
      const momentIndex = moments.value.findIndex(m => m.id === momentId)
      if (momentIndex !== -1) {
        moments.value.splice(momentIndex, 1)
        emit('moment-updated', {
          type: 'deleted',
          momentId
        })
      }
    }

    // 刷新列表
    const refresh = () => {
      currentPage.value = 0
      hasMore.value = true
      return loadMoments(0, false)
    }

    // 初始化加载
    onMounted(() => {
      refresh()
    })

    return {
      moments,
      loading,
      loadingMore,
      hasMore,
      loadMore,
      currentUserId: () => props.currentUserId,
      currentUserDisplayName,
      handleLikeUpdated,
      handleCommentAdded,
      handleMomentDeleted,
      refresh
    }
  }
}
</script>

<style scoped>
.moment-list {
  width: 100%;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 0;
  gap: 1rem;
  color: var(--color-gray-500);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 0;
  text-align: center;
  gap: 1rem;
}

.empty-state h3 {
  margin: 0;
  color: var(--color-gray-600);
  font-size: 1.25rem;
  font-weight: 500;
}

.empty-state p {
  margin: 0;
  color: var(--color-gray-500);
}

.moments-container {
  width: 100%;
}

.load-more {
  display: flex;
  justify-content: center;
  padding: 2rem 0;
}

/* 动画效果 */
.moment-enter-active,
.moment-leave-active {
  transition: all 0.3s ease;
}

.moment-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.moment-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

.moment-move {
  transition: transform 0.3s ease;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .loading-container,
  .empty-state {
    padding: 2rem 0;
  }

  .empty-state h3 {
    font-size: 1.125rem;
  }

  .load-more {
    padding: 1.5rem 0;
  }
}
</style>