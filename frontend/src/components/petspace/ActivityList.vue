<!-- 
  文件位置: src/components/petspace/ActivityList.vue
  活动列表 - 左侧下拉面板
-->

<template>
  <div class="sidebar-overlay" @click="$emit('close')">
    <div class="activity-list-panel" @click.stop>
      <BaseCard padding="md" shadow="lg" class="activity-card">
        <div class="panel-header">
          <div class="header-content">
            <h3 class="panel-title">📋 活动管理</h3>
            <div class="completion-stats">
              <span class="stats-text">{{ completedCount }}/{{ totalCount }} 完成</span>
              <div class="mini-progress">
                <div class="mini-progress-fill" :style="{ width: progressPercentage + '%' }"></div>
              </div>
            </div>
          </div>
          <!-- 添加关闭按钮 -->
          <button @click="$emit('close')" class="close-btn">✕</button>
        </div>

        <div class="activity-categories">
          <div v-for="category in categories" :key="category.key" class="category-section">
            <div class="category-header" @click="toggleCategory(category.key)">
              <span class="category-icon">
                {{ expandedCategories[category.key] ? '▼' : '▶' }}
              </span>
              <span class="category-name">{{ category.name }}</span>
              <span class="category-count">{{ getCategoryCount(category.key) }}</span>
            </div>

            <transition name="expand">
              <div v-show="expandedCategories[category.key]" class="activity-items">
                <div v-for="activity in getCategoryActivities(category.key)" :key="activity.id" class="activity-item"
                  @click="$emit('edit-activity', activity)" @contextmenu.prevent="$emit('settings-activity', activity)">
                  <div class="activity-checkbox">
                    <input type="checkbox" :checked="activity.completed"
                      @click.stop="handleToggleActivity(activity)" />
                  </div>
                  <div class="activity-info">
                    <div class="activity-name">{{ activity.name }}</div>
                    <div v-if="activity.time" class="activity-time">
                      {{ activity.time }}
                    </div>
                  </div>
                  <div v-if="activity.frequency !== 'once'" class="activity-badge">
                    {{ getFrequencyText(activity) }}
                  </div>
                </div>
              </div>
            </transition>
          </div>
        </div>

        <button class="add-activity-btn" @click="handleAddActivity">
          + 添加新活动
        </button>
      </BaseCard>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import BaseCard from '@/components/base/BaseCard.vue'

const props = defineProps({
  activities: {
    type: Array,
    required: true
  },
  currentDate: {
    type: Date,
    required: true
  }
})

const emit = defineEmits(['toggle-activity', 'edit-activity', 'settings-activity', 'add-activity', 'close'])

// 分类定义
const categories = [
  { key: 'diet', name: '饮食相关' },
  { key: 'health', name: '健康相关' },
  { key: 'hygiene', name: '清洁相关' },
  { key: 'activity', name: '社交娱乐' }
]

// 展开状态
const expandedCategories = reactive({
  diet: true,
  health: true,
  hygiene: true,
  activity: true
})

// 切换分类展开状态
const toggleCategory = (key) => {
  expandedCategories[key] = !expandedCategories[key]
}

// 获取分类下的活动
const getCategoryActivities = (categoryKey) => {
  return props.activities.filter(a => a.category === categoryKey)
}

// 获取分类活动数量
const getCategoryCount = (categoryKey) => {
  const activities = getCategoryActivities(categoryKey)
  const completed = activities.filter(a => a.completed).length
  return `${completed}/${activities.length}`
}

// 获取频次文本
const getFrequencyText = (activity) => {
  switch (activity.frequency) {
    case 'daily':
      return '每天'
    case 'weekly':
      return '每周'
    case 'custom':
      return `每${activity.customDays}天`
    default:
      return ''
  }
}

// 处理活动切换
const handleToggleActivity = (activity) => {
  emit('toggle-activity', activity)
}

// 计算统计信息
const completedCount = computed(() => props.activities.filter(a => a.completed).length)
const totalCount = computed(() => props.activities.length)
const progressPercentage = computed(() => totalCount.value > 0 ? (completedCount.value / totalCount.value * 100) : 0)

// 添加新活动
const handleAddActivity = () => {
  emit('add-activity')
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

/* ===== 活动列表面板 ===== */
.activity-list-panel {
  position: fixed;
  left: 0;
  top: 72px; /* 从导航栏下方开始 */
  bottom: 0;
  width: 320px;
  max-width: 85vw; /* 响应式：最大不超过屏幕85% */
  padding: var(--spacing-6);
  background: white;
  box-shadow: var(--shadow-xl);
  overflow-y: auto;
  z-index: 150;
  animation: slideInLeft 0.3s var(--ease-out);
}

@keyframes slideInLeft {
  from {
    transform: translateX(-100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.activity-card {
  background: rgba(255, 255, 255, 0.95);
}

/* ===== 面板头部 ===== */
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--spacing-6);
  padding-bottom: var(--spacing-4);
  border-bottom: 2px solid var(--color-gray-200);
}

.header-content {
  flex: 1;
}

.completion-stats {
  margin-top: var(--spacing-2);
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
}

.stats-text {
  font-size: var(--text-sm);
  color: var(--color-gray-600);
  font-weight: var(--font-medium);
  white-space: nowrap;
}

.mini-progress {
  flex: 1;
  max-width: 80px;
  height: 6px;
  background: var(--color-gray-200);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.mini-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #10b981 0%, #3b82f6 100%);
  border-radius: var(--radius-full);
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
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

/* ===== 分类区域 ===== */
.activity-categories {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
}

.category-section {
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.category-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  padding: var(--spacing-3) var(--spacing-4);
  background: var(--color-gray-100);
  cursor: pointer;
  transition: all var(--duration-base);
  border-radius: var(--radius-md);
}

.category-header:hover {
  background: var(--color-gray-200);
}

.category-icon {
  font-size: var(--text-sm);
  color: var(--color-gray-600);
}

.category-name {
  flex: 1;
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  color: var(--color-gray-900);
}

.category-count {
  font-size: var(--text-sm);
  color: var(--color-gray-500);
  background: white;
  padding: var(--spacing-1) var(--spacing-3);
  border-radius: var(--radius-full);
}

/* ===== 活动列表 ===== */
.activity-items {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2);
  padding: var(--spacing-2);
}

.activity-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  padding: var(--spacing-3);
  background: white;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--duration-base);
}

.activity-item:hover {
  background: var(--color-gray-50);
  transform: translateX(4px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.activity-item:active {
  transform: translateX(2px);
  transition: transform 0.1s ease;
}

.activity-checkbox {
  position: relative;
}

.activity-checkbox input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
  accent-color: var(--color-primary);
  transition: transform 0.15s ease;
}

.activity-checkbox input[type="checkbox"]:active {
  transform: scale(0.95);
}

.activity-checkbox input[type="checkbox"]:checked {
  animation: checkboxCheck 0.3s ease;
}

@keyframes checkboxCheck {
  0% { transform: scale(1); }
  50% { transform: scale(1.2); }
  100% { transform: scale(1); }
}

.activity-info {
  flex: 1;
}

.activity-name {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-gray-900);
  margin-bottom: var(--spacing-1);
}

.activity-time {
  font-size: var(--text-xs);
  color: var(--color-gray-500);
}

.activity-badge {
  font-size: var(--text-xs);
  padding: var(--spacing-1) var(--spacing-2);
  background: rgba(99, 102, 241, 0.1);
  color: var(--color-primary);
  border-radius: var(--radius-full);
}

/* ===== 添加按钮 ===== */
.add-activity-btn {
  width: 100%;
  margin-top: var(--spacing-6);
  padding: var(--spacing-3);
  background: var(--gradient-primary);
  color: white;
  font-size: var(--text-base);
  font-weight: var(--font-medium);
  border-radius: var(--radius-lg);
  transition: all var(--duration-base);
  cursor: pointer;
}

.add-activity-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

/* ===== 展开动画 ===== */
.expand-enter-active,
.expand-leave-active {
  transition: all 0.3s var(--ease-out);
  overflow: hidden;
}

.expand-enter-from,
.expand-leave-to {
  max-height: 0;
  opacity: 0;
}

.expand-enter-to,
.expand-leave-from {
  max-height: 1000px;
  opacity: 1;
}

/* ===== 滚动条 ===== */
.activity-list-panel::-webkit-scrollbar {
  width: 6px;
}

.activity-list-panel::-webkit-scrollbar-thumb {
  background: var(--color-gray-400);
  border-radius: var(--radius-full);
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .activity-list-panel {
    width: 280px;
    padding: var(--spacing-4);
  }
}
</style>