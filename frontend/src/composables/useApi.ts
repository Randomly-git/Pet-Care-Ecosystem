/**
 * Vue 3 Composable - API服务的响应式封装
 * 提供更友好的Vue组件集成方式
 */

import { ref, reactive, computed, onMounted, onUnmounted, watch, readonly } from 'vue'
import { ElLoading, ElMessage } from 'element-plus'
import apiService, { authService, petService, activityService } from '@/services'

// 类型定义
export interface UseApiOptions {
  immediate?: boolean // 是否立即执行
  loading?: boolean // 是否显示loading
  showMessage?: boolean // 是否显示成功/失败消息
  onSuccess?: (data: any) => void // 成功回调
  onError?: (error: any) => void // 错误回调
  cache?: boolean // 是否使用缓存
  cacheTime?: number // 缓存时间（毫秒）
}

export interface PaginationState {
  page: number
  pageSize: number
  total: number
}

export interface UseListOptions<T> extends UseApiOptions {
  pagination?: boolean
  initialParams?: any
}

// 通用的API请求Hook
export function useApi<T = any>(
  apiCall: () => Promise<T>,
  options: UseApiOptions = {}
) {
  const {
    immediate = false,
    loading = true,
    showMessage = false,
    onSuccess,
    onError,
    cache = false,
    cacheTime
  } = options

  const data = ref<T | null>(null)
  const error = ref<Error | null>(null)
  const isLoading = ref(false)
  const isReady = ref(false)

  let loadingInstance: any = null
  let controller: AbortController | null = null

  // 取消请求
  const cancel = () => {
    if (controller) {
      controller.abort()
    }
    if (loadingInstance) {
      loadingInstance.close()
    }
    isLoading.value = false
  }

  // 执行请求
  const execute = async (...args: any[]) => {
    try {
      // 取消之前的请求
      cancel()

      // 创建新的AbortController
      controller = new AbortController()

      // 设置loading状态
      isLoading.value = true
      error.value = null

      // 显示loading
      if (loading) {
        loadingInstance = ElLoading.service({
          lock: true,
          text: '加载中...',
          background: 'rgba(0, 0, 0, 0.7)'
        })
      }

      // 执行API调用
      const result = await apiCall(...args)

      // 处理成功响应
      data.value = result
      isReady.value = true

      // 显示成功消息
      if (showMessage) {
        ElMessage.success('操作成功')
      }

      // 执行成功回调
      if (onSuccess) {
        onSuccess(result)
      }

      return result

    } catch (err: any) {
      // 处理错误
      error.value = err

      // 显示错误消息
      if (showMessage) {
        ElMessage.error(err.message || '操作失败')
      }

      // 执行错误回调
      if (onError) {
        onError(err)
      }

      throw err

    } finally {
      // 清理loading
      isLoading.value = false
      if (loadingInstance) {
        loadingInstance.close()
        loadingInstance = null
      }
      controller = null
    }
  }

  // 重置状态
  const reset = () => {
    cancel()
    data.value = null
    error.value = null
    isLoading.value = false
    isReady.value = false
  }

  // 立即执行
  if (immediate) {
    onMounted(() => {
      execute()
    })
  }

  // 组件卸载时取消请求
  onUnmounted(() => {
    cancel()
  })

  return {
    data: readonly(data),
    error: readonly(error),
    isLoading: readonly(isLoading),
    isReady: readonly(isReady),
    execute,
    cancel,
    reset
  }
}

// 列表数据的Hook
export function useList<T = any>(
  apiCall: (params: any) => Promise<{ data: T[]; total?: number }>,
  options: UseListOptions<T> = {}
) {
  const {
    pagination = false,
    initialParams = {},
    ...apiOptions
  } = options

  const list = ref<T[]>([])
  const paginationState = reactive<PaginationState>({
    page: 1,
    pageSize: 10,
    total: 0
  })

  // 更新参数
  const params = ref({ ...initialParams })

  // 获取列表数据
  const { execute, isLoading, error } = useApi(
    async () => {
      const requestParams = pagination
        ? { ...params.value, page: paginationState.page, pageSize: paginationState.pageSize }
        : params.value

      const result = await apiCall(requestParams)

      // 更新列表数据
      if (Array.isArray(result)) {
        list.value = result
        paginationState.total = result.length
      } else if (result.data) {
        list.value = result.data
        if (result.total !== undefined) {
          paginationState.total = result.total
        }
      } else {
        list.value = []
        paginationState.total = 0
      }

      return result
    },
    apiOptions
  )

  // 刷新列表
  const refresh = () => {
    return execute()
  }

  // 搜索
  const search = (searchParams: any) => {
    params.value = { ...params.value, ...searchParams }
    paginationState.page = 1
    return execute()
  }

  // 重置搜索
  const resetSearch = () => {
    params.value = { ...initialParams }
    paginationState.page = 1
    return execute()
  }

  // 分页变化
  const handlePageChange = (page: number) => {
    paginationState.page = page
    return execute()
  }

  const handlePageSizeChange = (pageSize: number) => {
    paginationState.pageSize = pageSize
    paginationState.page = 1
    return execute()
  }

  return {
    list: readonly(list),
    pagination: readonly(paginationState),
    params: readonly(params),
    isLoading: readonly(isLoading),
    error: readonly(error),
    refresh,
    search,
    resetSearch,
    handlePageChange,
    handlePageSizeChange,
    execute
  }
}

// 认证Hook
export function useAuth() {
  const user = ref(authService.getCurrentUser())
  const isAuthenticated = computed(() => authService.isAuthenticated())

  // 登录
  const { execute: login, isLoading: isLoggingIn } = useApi(
    async (credentials: { name: string; password: string }) => {
      const result = await authService.login(credentials)
      user.value = result.user
      return result
    },
    {
      showMessage: true,
      onSuccess: (result) => {
        console.log('登录成功:', result)
      }
    }
  )

  // 登出
  const logout = async () => {
    await authService.logout()
    user.value = null
  }

  // 检查登录状态
  const checkAuth = async () => {
    if (isAuthenticated.value && !user.value) {
      // 如果有token但没有用户信息，尝试获取
      try {
        const userId = JSON.parse(localStorage.getItem('userInfo') || '{}').id
        if (userId) {
          user.value = await authService.getUserInfo(userId)
        }
      } catch (error) {
        console.error('获取用户信息失败:', error)
        logout()
      }
    }
  }

  onMounted(() => {
    checkAuth()
  })

  return {
    user: readonly(user),
    isAuthenticated,
    isLoggingIn: readonly(isLoggingIn),
    login,
    logout,
    checkAuth
  }
}

// 宠物管理Hook
export function usePets(userId?: number) {
  const {
    list: pets,
    isLoading,
    error,
    refresh
  } = useList(
    async () => {
      if (!userId) return { data: [], total: 0 }
      const data = await petService.getPets(userId)
      return { data, total: data.length }
    },
    {
      immediate: !!userId,
      cache: true,
      cacheTime: 5 * 60 * 1000 // 5分钟缓存
    }
  )

  // 创建宠物
  const { execute: createPet, isLoading: isCreating } = useApi(
    async (petData: any) => {
      const result = await petService.createPet(petData)
      await refresh() // 刷新列表
      return result
    },
    {
      showMessage: true,
      onSuccess: () => {
        ElMessage.success('宠物创建成功')
      }
    }
  )

  // 更新宠物
  const { execute: updatePet, isLoading: isUpdating } = useApi(
    async ({ id, ...data }: any) => {
      const result = await petService.updatePet(id, data)
      await refresh()
      return result
    },
    {
      showMessage: true
    }
  )

  // 删除宠物
  const { execute: deletePet, isLoading: isDeleting } = useApi(
    async (petId: number) => {
      await petService.deletePet(petId)
      await refresh()
    },
    {
      showMessage: true,
      onSuccess: () => {
        ElMessage.success('宠物删除成功')
      }
    }
  )

  return {
    pets: readonly(pets),
    isLoading: readonly(isLoading),
    error: readonly(error),
    isCreating: readonly(isCreating),
    isUpdating: readonly(isUpdating),
    isDeleting: readonly(isDeleting),
    refresh,
    createPet,
    updatePet,
    deletePet
  }
}

// 活动管理Hook
export function useActivities(userId?: number) {
  const activities = ref([])
  const activityKinds = ref([])

  // 获取活动种类
  const { execute: fetchActivityKinds, isLoading: isLoadingKinds } = useApi(
    async () => {
      const kinds = await activityService.getActivityKinds()
      activityKinds.value = kinds
      return kinds
    },
    {
      immediate: true,
      cache: true,
      cacheTime: 24 * 60 * 60 * 1000 // 1天缓存
    }
  )

  // 获取活动列表
  const { execute: fetchActivities, isLoading: isLoadingActivities } = useApi(
    async (kindId?: number) => {
      if (!userId) return []
      const activityList = await activityService.getActivities(userId, kindId)
      activities.value = activityList
      return activityList
    }
  )

  // 创建活动
  const { execute: createActivity, isLoading: isCreating } = useApi(
    async (activityData: any) => {
      const result = await activityService.createActivity(activityData)
      await fetchActivities() // 刷新列表
      return result
    },
    {
      showMessage: true
    }
  )

  // 删除活动
  const { execute: deleteActivity, isLoading: isDeleting } = useApi(
    async (activityId: number) => {
      await activityService.deleteActivity(activityId)
      await fetchActivities() // 刷新列表
    },
    {
      showMessage: true
    }
  )

  return {
    activities: readonly(activities),
    activityKinds: readonly(activityKinds),
    isLoadingKinds: readonly(isLoadingKinds),
    isLoadingActivities: readonly(isLoadingActivities),
    isCreating: readonly(isCreating),
    isDeleting: readonly(isDeleting),
    fetchActivityKinds,
    fetchActivities,
    createActivity,
    deleteActivity
  }
}

// 注意：useList, useAuth, usePets, useActivities 已在定义时导出