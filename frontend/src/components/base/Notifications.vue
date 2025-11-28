<!--
  文件位置: src/components/base/Notifications.vue
  通知组件 - 显示应用通知和消息
-->

<template>
  <Teleport to="body">
    <div class="notifications-container">
      <TransitionGroup name="notification" tag="div">
        <div
          v-for="notification in notifications"
          :key="notification.id"
          :class="['notification-item', notification.type]"
          @click="removeNotification(notification.id)"
        >
          <!-- 图标 -->
          <div class="notification-icon">
            <span v-if="notification.type === 'success'">✅</span>
            <span v-else-if="notification.type === 'error'">❌</span>
            <span v-else-if="notification.type === 'warning'">⚠️</span>
            <span v-else>ℹ️</span>
          </div>

          <!-- 内容 -->
          <div class="notification-content">
            <div class="notification-message">{{ notification.message }}</div>
            <div v-if="notification.details" class="notification-details">
              {{ notification.details }}
            </div>
          </div>

          <!-- 关闭按钮 -->
          <button class="notification-close" @click.stop="removeNotification(notification.id)">
            ✕
          </button>

          <!-- 进度条（自动隐藏） -->
          <div
            v-if="notification.duration > 0"
            class="notification-progress"
            :style="{ animationDuration: notification.duration + 'ms' }"
          ></div>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({
  maxNotifications: {
    type: Number,
    default: 5
  }
})

// 通知列表
const notifications = ref([])

// 添加通知
const addNotification = (message, type = 'info', options = {}) => {
  const notification = {
    id: Date.now() + Math.random(),
    message,
    type,
    duration: options.duration !== undefined ? options.duration : 3000,
    details: options.details || null,
    persistent: options.persistent || false,
    action: options.action || null
  }

  notifications.value.unshift(notification)

  // 限制通知数量
  if (notifications.value.length > props.maxNotifications) {
    notifications.value = notifications.value.slice(0, props.maxNotifications)
  }

  // 自动移除（非持久化通知）
  if (!notification.persistent && notification.duration > 0) {
    setTimeout(() => {
      removeNotification(notification.id)
    }, notification.duration)
  }

  return notification.id
}

// 移除通知
const removeNotification = (id) => {
  const index = notifications.value.findIndex(n => n.id === id)
  if (index > -1) {
    notifications.value.splice(index, 1)
  }
}

// 清空所有通知
const clearNotifications = () => {
  notifications.value = []
}

// 监听全局状态（如果有 app store）
watch(() => notifications.value, (newNotifications) => {
  console.log('通知数量:', newNotifications.length)
}, { deep: true })

// 暴露给外部使用
defineExpose({
  addNotification,
  removeNotification,
  clearNotifications,
  notifications: computed(() => notifications.value)
})
</script>

<style scoped>
.notifications-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 10000;
  pointer-events: none;
}

.notification-item {
  position: relative;
  background: white;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  padding: var(--spacing-4);
  margin-bottom: var(--spacing-3);
  min-width: 300px;
  max-width: 500px;
  pointer-events: all;
  cursor: pointer;
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-3);
  border-left: 4px solid var(--color-gray-300);
  overflow: hidden;
}

/* 通知类型样式 */
.notification-item.success {
  border-left-color: var(--color-success);
  background: linear-gradient(90deg, rgba(16, 185, 129, 0.05) 0%, white 100%);
}

.notification-item.error {
  border-left-color: var(--color-danger);
  background: linear-gradient(90deg, rgba(239, 68, 68, 0.05) 0%, white 100%);
}

.notification-item.warning {
  border-left-color: var(--color-warning);
  background: linear-gradient(90deg, rgba(245, 158, 11, 0.05) 0%, white 100%);
}

.notification-item.info {
  border-left-color: var(--color-primary);
  background: linear-gradient(90deg, rgba(99, 102, 241, 0.05) 0%, white 100%);
}

/* 图标 */
.notification-icon {
  flex-shrink: 0;
  font-size: var(--text-xl);
  line-height: 1;
  margin-top: 2px;
}

/* 内容 */
.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-message {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-gray-900);
  line-height: 1.4;
  word-break: break-word;
}

.notification-details {
  font-size: var(--text-xs);
  color: var(--color-gray-600);
  margin-top: var(--spacing-1);
  line-height: 1.3;
}

/* 关闭按钮 */
.notification-close {
  position: absolute;
  top: var(--spacing-2);
  right: var(--spacing-2);
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  color: var(--color-gray-400);
  font-size: var(--text-sm);
  cursor: pointer;
  border-radius: var(--radius-full);
  transition: all var(--duration-base);
  opacity: 0;
}

.notification-item:hover .notification-close {
  opacity: 1;
}

.notification-close:hover {
  background: var(--color-gray-100);
  color: var(--color-gray-600);
}

/* 进度条 */
.notification-progress {
  position: absolute;
  bottom: 0;
  left: 0;
  height: 3px;
  background: var(--color-primary);
  border-radius: 0 0 0 var(--radius-lg);
  animation: progressCountdown linear forwards;
}

.notification-item.success .notification-progress {
  background: var(--color-success);
}

.notification-item.error .notification-progress {
  background: var(--color-danger);
}

.notification-item.warning .notification-progress {
  background: var(--color-warning);
}

@keyframes progressCountdown {
  from {
    width: 100%;
  }
  to {
    width: 0%;
  }
}

/* 动画 */
.notification-enter-active,
.notification-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.notification-enter-from {
  transform: translateX(100%);
  opacity: 0;
}

.notification-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

.notification-move {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 响应式 */
@media (max-width: 768px) {
  .notifications-container {
    top: 10px;
    left: 10px;
    right: 10px;
  }

  .notification-item {
    min-width: auto;
    max-width: none;
  }
}
</style>