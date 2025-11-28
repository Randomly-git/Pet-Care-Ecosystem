// 文件位置: src/stores/activity.js
// 活动状态管理 - Pinia store

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { activityAPI } from '@/services/api'

export const useActivityStore = defineStore('activity', () => {
  // ===== 状态 =====
  const activities = ref([])
  const currentDate = ref(new Date())
  const loading = ref(false)
  const error = ref(null)

  // ===== 计算属性 =====
  const todayActivities = computed(() => {
    return activities.value.filter(activity => {
      // 这里可以根据实际需求调整过滤逻辑
      return activity.frequency === 'daily' || isTodayActivity(activity)
    })
  })

  const completedCount = computed(() => {
    return todayActivities.value.filter(activity => activity.completed).length
  })

  const completionPercentage = computed(() => {
    if (todayActivities.value.length === 0) return 0
    return Math.round((completedCount.value / todayActivities.value.length) * 100)
  })

  const activitiesByCategory = computed(() => {
    const grouped = {}

    activities.value.forEach(activity => {
      if (!grouped[activity.category]) {
        grouped[activity.category] = []
      }
      grouped[activity.category].push(activity)
    })

    return grouped
  })

  // ===== 工具函数 =====
  const isTodayActivity = (activity) => {
    // 简化版本：假设 daily 频率的活动是今天的
    return activity.frequency === 'daily'
  }

  const formatDate = (date) => {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }

  // ===== 操作方法 =====
  const fetchActivities = async (petId, date = null) => {
    loading.value = true
    error.value = null

    try {
      const queryDate = date || formatDate(currentDate.value)
      const data = await activityAPI.getActivities(petId, queryDate)
      activities.value = data || []

    } catch (err) {
      error.value = err.message
      // 如果API不可用，使用模拟数据
      if (err.message.includes('fetch')) {
        activities.value = getMockActivities()
      }
    } finally {
      loading.value = false
    }
  }

  const toggleActivity = async (activityId) => {
    try {
      // 优先级：先更新本地状态，再同步到服务器
      const activity = activities.value.find(a => a.id === activityId)
      if (activity) {
        activity.completed = !activity.completed
      }

      // 尝试同步到服务器
      await activityAPI.toggleActivity(activityId)

    } catch (err) {
      // 如果同步失败，回滚本地状态
      const activity = activities.value.find(a => a.id === activityId)
      if (activity) {
        activity.completed = !activity.completed
      }
      error.value = err.message
    }
  }

  const createActivity = async (activityData) => {
    loading.value = true
    error.value = null

    try {
      const newActivity = await activityAPI.createActivity(activityData)
      activities.value.push(newActivity)
      return newActivity

    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const updateActivity = async (activityId, activityData) => {
    loading.value = true
    error.value = null

    try {
      const updatedActivity = await activityAPI.updateActivity(activityId, activityData)
      const index = activities.value.findIndex(a => a.id === activityId)
      if (index !== -1) {
        activities.value[index] = updatedActivity
      }
      return updatedActivity

    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const deleteActivity = async (activityId) => {
    try {
      await activityAPI.deleteActivity(activityId)
      activities.value = activities.value.filter(a => a.id !== activityId)

    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  const setCurrentDate = (date) => {
    currentDate.value = date
    // 可以在这里触发重新加载当天活动
  }

  // ===== 模拟数据（API不可用时使用） =====
  const getMockActivities = () => [
    {
      id: 1,
      category: 'diet',
      name: '早餐',
      time: '08:00',
      completed: true,
      frequency: 'daily',
      description: '每天早上的第一顿饭',
      images: []
    },
    {
      id: 2,
      category: 'diet',
      name: '午餐',
      time: '12:00',
      completed: true,
      frequency: 'daily',
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
      description: '每天陪宠物玩耍',
      images: ['play1.jpg']
    }
  ]

  return {
    // 状态
    activities,
    currentDate,
    loading,
    error,

    // 计算属性
    todayActivities,
    completedCount,
    completionPercentage,
    activitiesByCategory,

    // 方法
    fetchActivities,
    toggleActivity,
    createActivity,
    updateActivity,
    deleteActivity,
    setCurrentDate
  }
})