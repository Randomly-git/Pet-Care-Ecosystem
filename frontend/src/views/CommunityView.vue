<template>
  <div class="community-view">
    <div class="page-header">
      <h1>宠物社区</h1>
      <p>分享你和宠物的美好时光</p>
    </div>

    <div class="community-content">
      <!-- 创建动态区域 -->
      <CreateMoment
        :user-id="currentUserId"
        :user-name="userName"
        :user-avatar="userAvatar"
        @moment-created="handleMomentCreated"
      />

      <!-- 动态列表 -->
      <MomentList
        :user-id="currentUserId"
        :current-user-id="currentUserId"
        @moment-updated="handleMomentUpdated"
      />
    </div>

    <!-- 通知组件 -->
    <Notifications ref="notifications" />
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { CreateMoment, MomentList } from '@/components/community'
import { userAPI } from '@/services/api'
import Notifications from '@/components/base/Notifications.vue'

export default {
  name: 'CommunityView',
  components: {
    CreateMoment,
    MomentList,
    Notifications
  },
  setup() {
    const authStore = useAuthStore()
    const currentUserId = ref(1) // 临时用户ID，应该从认证状态获取
    const userName = ref('宠物爱好者')
    const userAvatar = ref('')
    const notifications = ref(null)

    // 获取用户信息
    const loadUserInfo = async () => {
      try {
        if (authStore.isAuthenticated && authStore.user) {
          currentUserId.value = authStore.user.id || 1
          userName.value = authStore.user.nickname || authStore.user.name || '宠物爱好者'
          userAvatar.value = authStore.user.avatarUrl || ''
        } else {
          // 如果没有认证信息，尝试获取用户信息
          const userInfo = await userAPI.getUserInfo(currentUserId.value)
          if (userInfo) {
            userName.value = userInfo.nickname || userInfo.name || '宠物爱好者'
            userAvatar.value = userInfo.avatarUrl || ''
          }
        }
      } catch (error) {
        console.error('获取用户信息失败:', error)
        // 使用默认值
        userName.value = '宠物爱好者'
      }
    }

    // 处理动态创建
    const handleMomentCreated = (moment) => {
      console.log('新动态已创建:', moment)
      showNotification('success', '发布成功', '动态已成功发布到社区')

      // 刷新动态列表
      // MomentList 组件会自动刷新
    }

    // 处理动态更新（点赞、评论、删除等）
    const handleMomentUpdated = (updateData) => {
      console.log('动态更新:', updateData)

      if (updateData.type === 'deleted') {
        showNotification('info', '动态已删除', '动态已成功删除')
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

    // 页面加载时获取用户信息
    onMounted(() => {
      loadUserInfo()
    })

    return {
      currentUserId,
      userName,
      userAvatar,
      notifications,
      handleMomentCreated,
      handleMomentUpdated
    }
  }
}
</script>

<style scoped>
.community-view {
  max-width: 800px;
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
  font-size: 2.5rem;
  font-weight: 700;
}

.page-header p {
  color: var(--color-gray-600);
  margin: 0;
  font-size: 1.125rem;
}

.community-content {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .community-view {
    padding: 1rem;
  }

  .page-header {
    margin-bottom: 2rem;
  }

  .page-header h1 {
    font-size: 2rem;
  }

  .page-header p {
    font-size: 1rem;
  }

  .community-content {
    gap: 1.5rem;
  }
}

/* 动画效果 */
.community-view {
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>