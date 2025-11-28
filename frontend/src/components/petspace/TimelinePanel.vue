<!-- 
  文件位置: src/components/petspace/TimelinePanel.vue
  时间轴面板 - 右侧展开
-->

<template>
  <div class="sidebar-overlay" @click="$emit('close')">
    <div class="timeline-panel" @click.stop>
      <BaseCard padding="md" shadow="lg" class="timeline-card">
        <div class="panel-header">
          <h3 class="panel-title">⏱️ 时间轴</h3>
          <button @click="$emit('close')" class="close-btn">✕</button>
        </div>

        <div class="timeline-content">
          <div v-for="event in sortedEvents" :key="event.id"
            :class="['timeline-item', { 'important': event.important }]" @click="$emit('event-click', event)">
            <!-- 时间标记 -->
            <div class="timeline-marker">
              <div class="marker-dot"></div>
              <div class="marker-line"></div>
            </div>

            <!-- 事件内容 -->
            <div class="event-content">
              <div class="event-date">
                {{ formatDate(event.date) }}
              </div>
              <div class="event-time">{{ event.time }}</div>

              <div class="event-card">
                <div class="event-header">
                  <span class="event-icon">{{ getEventIcon(event.type) }}</span>
                  <span class="event-title">{{ event.title }}</span>
                  <span v-if="event.important" class="important-star">⭐</span>
                </div>

                <div v-if="event.description" class="event-description">
                  {{ event.description }}
                </div>

                <div v-if="event.thumbnail" class="event-thumbnail">
                  <div class="thumbnail-placeholder">
                    📸 {{ event.thumbnail }}
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="load-more">
            <button class="load-more-btn">
              ▼ 查看更多...
            </button>
          </div>
        </div>
      </BaseCard>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import BaseCard from '@/components/base/BaseCard.vue'

const props = defineProps({
  events: {
    type: Array,
    required: true
  },
  currentDate: {
    type: Date,
    required: true
  }
})

const emit = defineEmits(['date-select', 'event-click', 'close'])

// 按时间倒序排序事件
const sortedEvents = computed(() => {
  return [...props.events].sort((a, b) => {
    const dateA = new Date(`${a.date} ${a.time}`)
    const dateB = new Date(`${b.date} ${b.time}`)
    return dateB - dateA
  })
})

// 格式化日期
const formatDate = (dateStr) => {
  const date = new Date(dateStr)
  const today = new Date()
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)

  if (isSameDay(date, today)) {
    return '今天'
  } else if (isSameDay(date, yesterday)) {
    return '昨天'
  } else {
    const month = date.getMonth() + 1
    const day = date.getDate()
    return `${month}月${day}日`
  }
}

const isSameDay = (date1, date2) => {
  return date1.getFullYear() === date2.getFullYear() &&
    date1.getMonth() === date2.getMonth() &&
    date1.getDate() === date2.getDate()
}

// 获取事件图标
const getEventIcon = (type) => {
  const icons = {
    diet: '🍖',
    health: '💊',
    hygiene: '🛁',
    activity: '🎮',
    other: '📌'
  }
  return icons[type] || icons.other
}
</script>

<style scoped>
/* ===== 遮罩层 ===== */
.sidebar-overlay {
  position: fixed;
  top: 72px; /* 从第二栏导航下方开始，不遮挡顶部 */
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(4px);
  z-index: 150; /* 低于顶部导航栏的 200 */
}

/* ===== 时间轴面板 ===== */
.timeline-panel {
  position: fixed;
  right: 0;
  top: 72px; /* 从导航栏下方开始 */
  bottom: 0;
  width: 360px;
  max-width: 85vw; /* 响应式：最大不超过屏幕85% */
  padding: var(--spacing-6);
  background: white;
  box-shadow: var(--shadow-xl);
  overflow-y: auto;
  z-index: 150;
  animation: slideInRight 0.3s var(--ease-out);
}

@keyframes slideInRight {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.timeline-card {
  background: rgba(255, 255, 255, 0.95);
}

/* ===== 面板头部 ===== */
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-6);
  padding-bottom: var(--spacing-4);
  border-bottom: 2px solid var(--color-gray-200);
}

.panel-title {
  font-size: var(--text-xl);
  font-weight: var(--font-bold);
  color: var(--color-gray-900);
}

.close-btn {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  background: var(--color-gray-100);
  color: var(--color-gray-600);
  transition: all var(--duration-base);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-lg);
  cursor: pointer;
}

.close-btn:hover {
  background: var(--color-gray-200);
  transform: rotate(90deg);
}

/* ===== 时间轴内容 ===== */
.timeline-content {
  position: relative;
}

.timeline-item {
  display: flex;
  gap: var(--spacing-4);
  margin-bottom: var(--spacing-6);
  cursor: pointer;
}

.timeline-item:last-child .marker-line {
  display: none;
}

/* ===== 时间标记 ===== */
.timeline-marker {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.marker-dot {
  width: 12px;
  height: 12px;
  background: var(--gradient-primary);
  border-radius: var(--radius-full);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2);
  transition: all var(--duration-base);
}

.timeline-item.important .marker-dot {
  width: 16px;
  height: 16px;
  background: var(--gradient-warm);
  box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.2);
}

.timeline-item:hover .marker-dot {
  transform: scale(1.3);
}

.marker-line {
  width: 2px;
  flex: 1;
  min-height: 40px;
  background: linear-gradient(to bottom, var(--color-gray-300) 0%, transparent 100%);
}

/* ===== 事件内容 ===== */
.event-content {
  flex: 1;
  padding-bottom: var(--spacing-2);
}

.event-date {
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  color: var(--color-primary);
  margin-bottom: var(--spacing-1);
}

.event-time {
  font-size: var(--text-xs);
  color: var(--color-gray-500);
  margin-bottom: var(--spacing-2);
}

.event-card {
  padding: var(--spacing-4);
  background: white;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  transition: all var(--duration-base);
}

.timeline-item:hover .event-card {
  box-shadow: var(--shadow-md);
  transform: translateX(-4px);
}

.timeline-item.important .event-card {
  border-left: 3px solid var(--color-danger);
}

.event-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  margin-bottom: var(--spacing-2);
}

.event-icon {
  font-size: var(--text-lg);
}

.event-title {
  flex: 1;
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  color: var(--color-gray-900);
}

.important-star {
  font-size: var(--text-base);
}

.event-description {
  font-size: var(--text-sm);
  color: var(--color-gray-600);
  line-height: 1.5;
  margin-bottom: var(--spacing-2);
}

.event-thumbnail {
  margin-top: var(--spacing-3);
}

.thumbnail-placeholder {
  width: 100%;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-gray-100);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  color: var(--color-gray-500);
}

/* ===== 加载更多 ===== */
.load-more {
  text-align: center;
  margin-top: var(--spacing-6);
}

.load-more-btn {
  padding: var(--spacing-2) var(--spacing-6);
  font-size: var(--text-sm);
  color: var(--color-gray-600);
  background: var(--color-gray-100);
  border-radius: var(--radius-lg);
  transition: all var(--duration-base);
  cursor: pointer;
}

.load-more-btn:hover {
  background: var(--color-gray-200);
  color: var(--color-gray-900);
}

/* ===== 滚动条 ===== */
.timeline-panel::-webkit-scrollbar {
  width: 6px;
}

.timeline-panel::-webkit-scrollbar-thumb {
  background: var(--color-gray-400);
  border-radius: var(--radius-full);
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .timeline-panel {
    width: 300px;
    padding: var(--spacing-4);
  }
}
</style>