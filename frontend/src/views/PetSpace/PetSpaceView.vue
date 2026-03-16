<!--
  文件位置: src/views/PetSpace/PetSpaceView.vue
  我的空间 - 宠物状态管理主页面（完整功能版）
-->

<template>
  <div class="pet-space-view">
    <!-- Header 组件 -->
    <AppHeader />
    <!-- 顶部导航栏 -->
    <div class="top-navbar">
      <div class="navbar-content">
        <!-- 左侧：宠物信息 -->
        <div class="pet-info">
          <div class="pet-avatar-emoji">{{ petInfo.avatar }}</div>
          <div class="pet-details">
            <h1 class="pet-name">{{ petInfo.name }}</h1>
            <p class="pet-meta">{{ petInfo.species }} · {{ petInfo.breed }}</p>
          </div>
        </div>

        <!-- 右侧：操作按钮 -->
        <div class="action-buttons">
          <button @click="showCalendar = !showCalendar" class="icon-btn" title="日历">
            📅
          </button>
          <button @click="showTimeline = !showTimeline" class="icon-btn" title="时间轴">
            ⏱️
          </button>
          <button @click="showActivityList = !showActivityList" class="icon-btn" title="活动列表">
            📋
          </button>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 日期切换器 -->
      <div class="date-switcher">
        <button @click="goToPrevDay" class="nav-btn">◀</button>
        <div class="date-display">
          <div class="date-main">{{ formatDate(selectedDate) }}</div>
          <div class="date-full">{{ formatFullDate(selectedDate) }}</div>
        </div>
        <button @click="goToNextDay" class="nav-btn">▶</button>
      </div>

      <button v-if="!isToday(selectedDate)" @click="goToToday" class="today-btn">
        回到今天
      </button>

      <!-- 统计信息 -->
      <div v-if="activities.length > 0" class="stats-card">
        <div class="stats-header">
          <div>
            <h3 class="stats-title">今日概况</h3>
            <p class="stats-text">已完成 {{ completedCount }} / {{ activities.length }} 项活动</p>
          </div>
          <div class="stats-emoji">{{ completedCount === activities.length ? '🎉' : '📋' }}</div>
        </div>
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: (completedCount / activities.length * 100) + '%' }"></div>
          <div class="progress-glow" v-if="completedCount > 0 && completedCount < activities.length"></div>
        </div>
      </div>

      <!-- 活动列表 -->
      <div v-if="activities.length > 0" class="activities-list">
        <div v-for="activity in activities" :key="activity.id"
          :class="['activity-card', {
            important: activity.important,
            completed: activity.completed
          }]">
          <div class="activity-content">
            <!-- 时间线 -->
            <div class="timeline-marker">
              <div class="activity-time">{{ activity.time }}</div>
              <div :class="['marker-dot', { completed: activity.completed }]"></div>
            </div>

            <!-- 内容 -->
            <div class="activity-info">
              <div class="activity-header">
                <span class="activity-icon">{{ activity.icon }}</span>
                <h4 class="activity-title">{{ activity.title }}</h4>
                <span v-if="activity.important" class="important-star">⭐</span>
                <span v-if="activity.completed" class="completed-badge">✓ 已完成</span>
              </div>
              <p class="activity-description">{{ activity.description }}</p>
              <div v-if="activity.hasImage" class="has-image-badge">
                📷 包含图片
              </div>

              <!-- 操作按钮 -->
              <div class="activity-actions">
                <button @click="handleToggleActivity(activity)" class="action-btn toggle-btn">
                  {{ activity.completed ? '取消完成' : '标记完成' }}
                </button>
                <button @click="handleEditActivity(activity)" class="action-btn edit-btn">
                  编辑
                </button>
                <button @click="handleSettingsActivity(activity)" class="action-btn settings-btn">
                  ⚙️
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <div class="empty-icon">📅</div>
        <h3 class="empty-title">这天还没有记录</h3>
        <p class="empty-text">选择今天或昨天查看活动记录</p>
        <button @click="handleAddActivity" class="add-first-btn">
          + 添加第一个活动
        </button>
      </div>
    </div>

    <!-- 日历侧边栏 -->
    <div v-if="showCalendar" class="sidebar-overlay" @click="showCalendar = false">
      <div class="calendar-sidebar" @click.stop>
        <div class="sidebar-header">
          <h3 class="sidebar-title">📅 选择日期</h3>
          <button @click="showCalendar = false" class="close-btn">✕</button>
        </div>
        <DatePicker v-model="selectedDate" @change="handleDateChange" @close="showCalendar = false" />
      </div>
    </div>

    <!-- 活动列表侧边栏 -->
    <ActivityList v-if="showActivityList" :activities="allActivities" :current-date="selectedDate"
      @toggle-activity="handleToggleActivity" @edit-activity="handleEditActivity"
      @settings-activity="handleSettingsActivity" @add-activity="handleAddActivity" @close="showActivityList = false" />

    <!-- 时间轴侧边栏 -->
    <TimelinePanel v-if="showTimeline" :events="timelineEvents" :current-date="selectedDate"
      @date-select="handleTimelineSelect" @event-click="handleEventClick" @close="showTimeline = false" />

    <!-- 活动详情弹窗 -->
    <ActivityDetailModal v-if="selectedActivity" :activity="selectedActivity" @save="handleSaveActivity"
      @delete="handleDeleteActivity" @settings="handleSettingsActivity" @close="selectedActivity = null" />

    <!-- 活动设置弹窗 -->
    <ActivitySettingsModal v-if="settingsActivity" :activity="settingsActivity" @save="handleSaveSettings"
      @close="settingsActivity = null" />

    <!-- Footer 组件 -->
    <AppFooter />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import DatePicker from '@/components/petspace/DatePicker.vue'
import ActivityList from '@/components/petspace/ActivityList.vue'
import TimelinePanel from '@/components/petspace/TimelinePanel.vue'
import ActivityDetailModal from '@/components/petspace/ActivityDetailModal.vue'
import ActivitySettingsModal from '@/components/petspace/ActivitySettingsModal.vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'

// ===== 状态管理 =====
const selectedDate = ref(new Date())
const showCalendar = ref(false)
const showTimeline = ref(false)
const showActivityList = ref(false)
const selectedActivity = ref(null)
const settingsActivity = ref(null)

// ===== 宠物信息 =====
const petInfo = ref({
  name: '喵喵',
  species: '猫',
  breed: '英短',
  avatar: '🐱'
})

// ===== 用户活动数据 =====
const userActivities = ref([])
const allActivities = computed(() => {
  // 将用户活动转换为ActivityList期望的格式
  return userActivities.value.map(activity => {
    // 根据activityKindId映射到ActivityList的category
    let category = 'activity' // 默认类别
    switch (activity.activityKindId) {
      case 1: // 喂养
        category = 'diet'
        break
      case 6: // 医疗
      case 7: // 生育
        category = 'health'
        break
      case 3: // 清洁
        category = 'hygiene'
        break
      case 2: // 互动
      case 4: // 外出
      case 5: // 运动
        category = 'activity'
        break
      case 8: // 异常
      case 9: // 其他
        category = 'health' // 异常和其他归为健康相关
        break
    }

    return {
      id: activity.activityId,
      name: activity.activityName,
      category: category,
      completed: activity.state === 1, // state=1表示启用
      description: `活动种类ID: ${activity.activityKindId}`,
      time: '全天',
      frequency: 'daily',
      autoMark: false,
      reminder: false,
      reminderMinutes: 15,
      importance: 'normal',
      images: []
    }
  })
  // 将用户活动与默认数据合并
  return [...userActivities.value.map(activity => {
    // 根据activityKindId映射到ActivityList的category
    let category = 'activity' // 默认类别
    switch (activity.activityKindId) {
      case 1: // 喂养
        category = 'diet'
        break
      case 6: // 医疗
      case 7: // 生育
        category = 'health'
        break
      case 3: // 清洁
        category = 'hygiene'
        break
      case 2: // 互动
      case 4: // 外出
      case 5: // 运动
        category = 'activity'
        break
      case 8: // 异常
      case 9: // 其他
        category = 'health' // 异常和其他归为健康相关
        break
    }

    return {
      id: activity.activityId,
      name: activity.activityName,
      category: category,
      completed: activity.state === 1, // state=1表示启用
      description: `活动种类ID: ${activity.activityKindId}`,
      time: '全天',
      frequency: 'daily',
      autoMark: false,
      reminder: false,
      reminderMinutes: 15,
      importance: 'normal',
      images: []
    }
  }), ...defaultActivities]
})

// 默认活动数据
const defaultActivities = [
  {
    id: 2,
    category: 'diet',
    name: '午餐',
    time: '12:00',
    completed: true,
    frequency: 'daily',
    autoMark: true,
    reminder: true,
    reminderMinutes: 15,
    importance: 'normal',
    description: '',
    images: []
  },
  {
    id: 3,
    category: 'diet',
    name: '晚餐',
    time: '18:00',
    completed: false,
    frequency: 'daily',
    autoMark: true,
    reminder: true,
    reminderMinutes: 15,
    importance: 'normal',
    description: '',
    images: []
  },
  {
    id: 4,
    category: 'health',
    name: '体检',
    time: '14:00',
    completed: true,
    frequency: 'custom',
    customDays: 365,
    autoMark: false,
    reminder: true,
    reminderMinutes: 1440,
    importance: 'important',
    description: '年度体检',
    images: []
  },
  {
    id: 5,
    category: 'hygiene',
    name: '洗澡',
    time: '20:00',
    completed: true,
    frequency: 'custom',
    customDays: 7,
    autoMark: false,
    reminder: true,
    reminderMinutes: 1440,
    importance: 'normal',
    description: '每周洗澡一次',
    images: []
  },
  {
    id: 6,
    category: 'hygiene',
    name: '刷牙',
    time: '21:00',
    completed: false,
    frequency: 'daily',
    autoMark: false,
    reminder: true,
    reminderMinutes: 15,
    importance: 'normal',
    description: '保持口腔健康',
    images: []
  },
  {
    id: 7,
    category: 'activity',
    name: '玩耍',
    time: '15:30',
    completed: true,
    frequency: 'daily',
    autoMark: false,
    reminder: false,
    importance: 'normal',
    description: '每天陪宠物玩耍',
    images: ['play1.jpg']
  }
]// ===== 今天的活动数据 =====
const getTodayActivities = () => {
  // 基于allActivities生成今天的活动
  return allActivities.value
    .filter(a => a.frequency === 'daily')
    .map(a => ({
      ...a,
      icon: getCategoryIcon(a.category),
      title: a.name,
      hasImage: a.images && a.images.length > 0,
      // 保持对原对象的引用以确保响应式更新
      _originalId: a.id
    }))
}

// ===== 昨天的活动数据 =====
const getYesterdayActivities = () => [
  {
    id: 11,
    time: '08:00',
    category: 'diet',
    icon: '🍖',
    title: '早餐',
    name: '早餐',
    description: '金枪鱼罐头',
    completed: true
  },
  {
    id: 12,
    time: '12:00',
    category: 'diet',
    icon: '🍖',
    title: '午餐',
    name: '午餐',
    description: '猫粮 + 鸡肉',
    completed: true
  },
  {
    id: 13,
    time: '14:00',
    category: 'health',
    icon: '💊',
    title: '体检',
    name: '体检',
    description: '定期体检，一切正常。体重3.2kg，精神状态良好',
    completed: true,
    important: true,
    hasImage: true
  },
  {
    id: 14,
    time: '16:00',
    category: 'activity',
    icon: '🎮',
    title: '玩耍',
    name: '玩耍',
    description: '在阳台晒太阳，追逐蝴蝶',
    completed: true
  },
  {
    id: 15,
    time: '18:00',
    category: 'diet',
    icon: '🍖',
    title: '晚餐',
    name: '晚餐',
    description: '三文鱼猫粮',
    completed: true
  },
  {
    id: 16,
    time: '20:00',
    category: 'hygiene',
    icon: '🛁',
    title: '洗澡',
    name: '洗澡',
    description: '洗得香香的，使用了宠物专用沐浴露',
    completed: true,
    hasImage: true
  }
]

// ===== 获取宠物活动的方法 =====
const loadUserActivities = async () => {
  try {
    const { useAuthStore } = await import('@/stores/auth')
    const authStore = useAuthStore()

    if (!authStore.userId) return

    const { getActivitiesByUserId } = await import('@/api/activities')
    const response = await getActivitiesByUserId(authStore.userId)

    // 处理API响应格式
    if (response && response.data) {
      userActivities.value = Array.isArray(response.data) ? response.data : []
    } else if (Array.isArray(response)) {
      userActivities.value = response
    } else {
      userActivities.value = []
    }

    console.log('PetSpace加载到的用户活动:', userActivities.value)
  } catch (error) {
    console.error('加载用户活动失败:', error)
    userActivities.value = []
  }
}

// ===== 时间轴事件数据 =====
const timelineEvents = ref([
  {
    id: 1,
    date: '2025-11-15',
    time: '15:30',
    type: 'activity',
    title: '玩耍时刻',
    description: '玩了30分钟逗猫棒',
    thumbnail: '',
    important: false
  },
  {
    id: 2,
    date: '2025-11-15',
    time: '12:00',
    type: 'diet',
    title: '吃午餐了',
    description: '今天吃了鸡肉罐头',
    thumbnail: 'lunch.jpg',
    important: false
  },
  {
    id: 3,
    date: '2025-11-14',
    time: '14:00',
    type: 'health',
    title: '体检记录',
    description: '定期体检，一切正常',
    thumbnail: '',
    important: true
  },
  {
    id: 4,
    date: '2025-11-14',
    time: '16:00',
    type: 'activity',
    title: '玩耍时刻',
    description: '在阳台晒太阳',
    thumbnail: 'sun.jpg',
    important: false
  },
  {
    id: 5,
    date: '2025-11-14',
    time: '20:00',
    type: 'hygiene',
    title: '洗澡啦',
    description: '洗得香香的',
    thumbnail: 'bath.jpg',
    important: true
  }
])

// ===== 工具函数 =====
const getCategoryIcon = (category) => {
  const icons = {
    diet: '🍖',
    health: '💊',
    hygiene: '🛁',
    activity: '🎮'
  }
  return icons[category] || '📌'
}

const isToday = (date) => {
  const today = new Date()
  return date.toDateString() === today.toDateString()
}

const isYesterday = (date) => {
  const yesterday = new Date()
  yesterday.setDate(yesterday.getDate() - 1)
  return date.toDateString() === yesterday.toDateString()
}

const getActivitiesForDate = (date) => {
  if (isToday(date)) return getTodayActivities()
  if (isYesterday(date)) return getYesterdayActivities()
  return []
}

const formatDate = (date) => {
  if (isToday(date)) return '今天'
  if (isYesterday(date)) return '昨天'
  const month = date.getMonth() + 1
  const day = date.getDate()
  return `${month}月${day}日`
}

const formatFullDate = (date) => {
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const weekday = weekdays[date.getDay()]
  return `${year}年${month}月${day}日 ${weekday}`
}

// ===== 导航函数 =====
const goToPrevDay = () => {
  const newDate = new Date(selectedDate.value)
  newDate.setDate(newDate.getDate() - 1)
  selectedDate.value = newDate
}

const goToNextDay = () => {
  const newDate = new Date(selectedDate.value)
  newDate.setDate(newDate.getDate() + 1)
  selectedDate.value = newDate
}

const goToToday = () => {
  selectedDate.value = new Date()
}

const handleDateChange = (date) => {
  selectedDate.value = date
  showCalendar.value = false
}

const handleTimelineSelect = (date) => {
  selectedDate.value = new Date(date)
  showTimeline.value = false
}

// ===== 活动操作函数 =====
const handleToggleActivity = (activity) => {
  const index = allActivities.value.findIndex(a => a.id === activity.id)
  if (index !== -1) {
    // 直接修改原对象以保持响应式
    allActivities.value[index].completed = !allActivities.value[index].completed
    // 强制触发视图更新
    allActivities.value = [...allActivities.value]
  }
}

const handleEditActivity = (activity) => {
  selectedActivity.value = { ...activity }
}

const handleSettingsActivity = (activity) => {
  settingsActivity.value = { ...activity }
  selectedActivity.value = null
}

const handleSaveActivity = (updatedActivity) => {
  const index = allActivities.value.findIndex(a => a.id === updatedActivity.id)
  if (index !== -1) {
    allActivities.value[index] = { ...updatedActivity }
  }
  selectedActivity.value = null
}

const handleDeleteActivity = (activityId) => {
  allActivities.value = allActivities.value.filter(a => a.id !== activityId)
  selectedActivity.value = null
}

const handleSaveSettings = (updatedSettings) => {
  const index = allActivities.value.findIndex(a => a.id === updatedSettings.id)
  if (index !== -1) {
    allActivities.value[index] = { ...updatedSettings }
  }
  settingsActivity.value = null
}

const handleAddActivity = () => {
  // TODO: 打开添加活动的弹窗
  alert('添加活动功能开发中...')
}

const handleEventClick = (event) => {
  // 从时间轴点击事件，找到对应的活动并打开详情
  const activity = allActivities.value.find(a => a.name === event.title)
  if (activity) {
    handleEditActivity(activity)
  }
}

// ===== 计算属性 =====
const activities = computed(() => getActivitiesForDate(selectedDate.value))
const completedCount = computed(() => activities.value.filter(a => a.completed).length)

// ===== 初始化 =====
loadUserActivities()
</script>

<style scoped>
.pet-space-view {
  min-height: 100vh;
  background: var(--color-gray-50);
  padding-bottom: var(--spacing-8);
}

/* ===== 顶部导航栏 ===== */
.top-navbar {
  background: var(--color-white);
  border-bottom: 1px solid var(--color-gray-200);
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
}

.navbar-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--spacing-4);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pet-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
}

.pet-avatar-emoji {
  font-size: 3rem;
  line-height: 1;
}

.pet-name {
  font-size: var(--text-xl);
  font-weight: var(--font-bold);
  color: var(--color-gray-900);
  margin-bottom: var(--spacing-1);
}

.pet-meta {
  font-size: var(--text-sm);
  color: var(--color-gray-600);
}

.action-buttons {
  display: flex;
  gap: var(--spacing-2);
}

.icon-btn {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-lg);
  background: transparent;
  font-size: var(--text-2xl);
  transition: all var(--duration-base);
  cursor: pointer;
}

.icon-btn:hover {
  background: var(--color-gray-100);
}

/* ===== 主内容区 ===== */
.main-content {
  max-width: 900px;
  margin: 0 auto;
  padding: var(--spacing-8) var(--spacing-4);
}

/* ===== 日期切换器 ===== */
.date-switcher {
  background: var(--color-white);
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-lg);
  padding: var(--spacing-6);
  margin-bottom: var(--spacing-4);
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: var(--shadow-sm);
}

.nav-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  background: var(--color-gray-50);
  border: 1px solid var(--color-gray-200);
  font-size: var(--text-lg);
  color: var(--color-gray-700);
  transition: all var(--duration-base);
  cursor: pointer;
}

.nav-btn:hover {
  background: var(--color-primary);
  color: var(--color-white);
  border-color: var(--color-primary);
}

.date-display {
  text-align: center;
}

.date-main {
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--color-gray-900);
  margin-bottom: var(--spacing-1);
}

.date-full {
  font-size: var(--text-sm);
  color: var(--color-gray-500);
}

.today-btn {
  width: 100%;
  padding: var(--spacing-3);
  margin-bottom: var(--spacing-6);
  background: var(--gradient-primary);
  color: white;
  border-radius: var(--radius-lg);
  font-weight: var(--font-medium);
  box-shadow: var(--shadow-md);
  transition: all var(--duration-base);
  cursor: pointer;
}

.today-btn:hover {
  box-shadow: var(--shadow-lg);
  transform: translateY(-2px);
}

/* ===== 统计卡片 ===== */
.stats-card {
  background: var(--color-white);
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-lg);
  padding: var(--spacing-6);
  margin-bottom: var(--spacing-6);
  box-shadow: var(--shadow-sm);
}

.stats-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-4);
}

.stats-title {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--color-gray-900);
  margin-bottom: var(--spacing-1);
}

.stats-text {
  font-size: var(--text-sm);
  color: var(--color-gray-600);
}

.stats-emoji {
  font-size: 3rem;
  line-height: 1;
}

.progress-bar {
  width: 100%;
  height: 12px;
  background: var(--color-gray-200);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #10b981 0%, #3b82f6 100%);
  border-radius: var(--radius-full);
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 0 0 1px rgba(16, 185, 129, 0.1);
  position: relative;
}

.progress-glow {
  position: absolute;
  top: 0;
  right: 0;
  width: 8px;
  height: 100%;
  background: linear-gradient(90deg, transparent 0%, rgba(16, 185, 129, 0.3) 50%, transparent 100%);
  border-radius: var(--radius-full);
  animation: glowPulse 2s infinite;
}

@keyframes glowPulse {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 0.8; }
}

/* ===== 活动列表 ===== */
.activities-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
  margin-left: 50px;
}

.activity-card {
  background: var(--color-white);
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-lg);
  padding: var(--spacing-6);
  transition: all var(--duration-base);
  box-shadow: var(--shadow-sm);
}

.activity-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.activity-card.important {
  border-left: 4px solid var(--color-warning);
  background: linear-gradient(90deg, rgba(250, 173, 20, 0.05) 0%, var(--color-white) 100%);
}

.activity-card.completed {
  background: var(--color-gray-50);
  opacity: 0.8;
}

.activity-card.completed .activity-title,
.activity-card.completed .activity-description {
  color: var(--color-gray-500);
  text-decoration: line-through;
  text-decoration-color: rgba(16, 185, 129, 0.3);
  text-decoration-thickness: 2px;
}

.activity-card.completed .marker-dot {
  animation: completedPulse 0.5s ease;
}

@keyframes completedPulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.3); }
  100% { transform: scale(1); }
}

.activity-content {
  display: flex;
  gap: var(--spacing-4);
}

.timeline-marker {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-2);
}

.activity-time {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-gray-600);
}

.marker-dot {
  width: 12px;
  height: 12px;
  border-radius: var(--radius-full);
  background: var(--color-gray-300);
}

.marker-dot.completed {
  background: var(--color-accent);
  box-shadow: 0 0 0 3px rgba(82, 196, 26, 0.2);
}

.activity-info {
  flex: 1;
}

.activity-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  margin-bottom: var(--spacing-2);
  flex-wrap: wrap;
}

.activity-icon {
  font-size: var(--text-3xl);
  line-height: 1;
}

.activity-title {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--color-gray-900);
}

.important-star {
  font-size: var(--text-lg);
}

.completed-badge {
  font-size: var(--text-sm);
  color: var(--color-success);
}

.activity-description {
  font-size: var(--text-base);
  color: var(--color-gray-600);
  line-height: 1.6;
  margin-bottom: var(--spacing-3);
}

.has-image-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-2);
  padding: var(--spacing-1) var(--spacing-3);
  background: rgba(59, 130, 246, 0.1);
  color: var(--color-primary);
  border-radius: var(--radius-lg);
  font-size: var(--text-sm);
  margin-bottom: var(--spacing-3);
}

/* ===== 活动操作按钮 ===== */
.activity-actions {
  display: flex;
  gap: var(--spacing-2);
  margin-top: var(--spacing-3);
}

.action-btn {
  padding: var(--spacing-2) var(--spacing-4);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  transition: all var(--duration-base);
  cursor: pointer;
}

.toggle-btn {
  background: var(--color-gray-50);
  color: var(--color-gray-700);
  border: 1px solid var(--color-gray-200);
  transition: all var(--duration-base);
}

.toggle-btn:hover {
  background: var(--color-accent);
  color: var(--color-white);
  border-color: var(--color-accent);
  transform: translateY(-1px);
}

.edit-btn {
  background: var(--color-primary);
  color: var(--color-white);
  border: 1px solid var(--color-primary);
}

.edit-btn:hover {
  background: var(--color-primary-dark);
  border-color: var(--color-primary-dark);
  transform: translateY(-1px);
}

.settings-btn {
  background: var(--color-gray-50);
  color: var(--color-gray-700);
  border: 1px solid var(--color-gray-200);
  padding: var(--spacing-2);
  min-width: 36px;
}

.settings-btn:hover {
  background: var(--color-gray-100);
  border-color: var(--color-gray-300);
}

/* ===== 空状态 ===== */
.empty-state {
  background: var(--color-white);
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-lg);
  padding: var(--spacing-12);
  text-align: center;
  box-shadow: var(--shadow-sm);
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: var(--spacing-4);
}

.empty-title {
  font-size: var(--text-xl);
  font-weight: var(--font-semibold);
  color: var(--color-gray-900);
  margin-bottom: var(--spacing-2);
}

.empty-text {
  font-size: var(--text-base);
  color: var(--color-gray-600);
  margin-bottom: var(--spacing-4);
}

.add-first-btn {
  padding: var(--spacing-3) var(--spacing-6);
  background: var(--color-primary);
  color: var(--color-white);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
  font-weight: var(--font-medium);
  transition: all var(--duration-base);
  cursor: pointer;
}

.add-first-btn:hover {
  background: var(--color-primary-dark);
  border-color: var(--color-primary-dark);
  transform: translateY(-1px);
}

/* ===== 侧边栏公共样式 ===== */
.sidebar-overlay {
  position: fixed;
  top: 72px;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(4px);
  z-index: 150;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-6);
  padding-bottom: var(--spacing-4);
  border-bottom: 2px solid var(--color-gray-200);
}

.sidebar-title {
  font-size: var(--text-xl);
  font-weight: var(--font-bold);
  color: var(--color-gray-900);
}

.close-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-full);
  background: var(--color-gray-100);
  color: var(--color-gray-600);
  font-size: var(--text-xl);
  transition: all var(--duration-base);
  cursor: pointer;
}

.close-btn:hover {
  background: var(--color-gray-200);
  transform: rotate(90deg);
}

/* ===== 日历侧边栏 ===== */
.calendar-sidebar {
  position: fixed;
  right: 0;
  top: 72px;
  bottom: 0;
  width: 420px;
  max-width: 90vw;
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

/* 滚动条样式 */
.calendar-sidebar::-webkit-scrollbar {
  width: 6px;
}

.calendar-sidebar::-webkit-scrollbar-thumb {
  background: var(--color-gray-400);
  border-radius: var(--radius-full);
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .main-content {
    padding: var(--spacing-4);
  }

  .pet-avatar-emoji {
    font-size: 2.5rem;
  }

  .pet-name {
    font-size: var(--text-lg);
  }

  .activity-content {
    flex-direction: column;
  }

  .timeline-marker {
    flex-direction: row;
    align-items: center;
  }

  .activity-actions {
    flex-wrap: wrap;
  }

  .calendar-sidebar {
    width: 340px;
  }
}
</style>