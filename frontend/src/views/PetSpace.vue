<template>
  <div class="pet-space-page">
    <!-- 使用统一的布局头部 -->
    <AppHeader />

    <!-- Hero Section -->
    <section class="hero-section">
      <div class="hero-container">
        <div class="hero-content">
          <div class="hero-emoji">🏠</div>
          <h2 class="hero-title">我的空间</h2>
          <p class="hero-subtitle">管理宠物和动态记录</p>
        </div>
      </div>
    </section>

    <!-- 主要内容区域 -->
    <div class="main-content-simple">
      <!-- 左侧边栏 -->
      <div class="left-sidebar">
        <!-- 用户信息 -->
        <div class="user-info-simple">
          <el-avatar :size="50" :src="authStore.user?.avatar_url" class="user-avatar">
            {{ authStore.userName.charAt(0) || 'U' }}
          </el-avatar>
          <div class="user-details">
            <div class="user-name">{{ authStore.userName }}</div>
            <div class="user-species">宠物主人</div>
          </div>
        </div>

        <!-- 当前宠物信息 -->
        <div class="current-pet-info" v-if="currentPet.id">
          <div class="pet-selector">
            <div class="pet-avatar-small">
              <el-avatar :size="32" :src="currentPet.avatar_url">
                {{ currentPet.name.charAt(0) }}
              </el-avatar>
            </div>
            <div class="pet-details-small">
              <div class="pet-name-small">{{ currentPet.name }}</div>
              <div class="pet-breed-small">{{ currentPet.type }} - {{ currentPet.breed }}</div>
            </div>
            <el-select
              v-model="currentPet.id"
              @change="handlePetChange"
              size="small"
              class="pet-select"
              placeholder="选择宠物"
            >
              <el-option
                v-for="pet in userPets"
                :key="pet.id"
                :label="pet.name"
                :value="pet.id"
              />
            </el-select>
          </div>
        </div>

        <!-- 统计数据 -->
        <div class="stats-simple">
          <div class="stat-item">
            <div class="stat-number">{{ activityRecords.length }}</div>
            <div class="stat-label">活动记录</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ statusRecords.length }}</div>
            <div class="stat-label">状态记录</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">{{ getActiveDays() }}</div>
            <div class="stat-label">活跃天数</div>
          </div>
        </div>

        <!-- 状态记录管理 -->
        <div class="status-management">
          <el-button
            type="primary"
            size="small"
            @click="showStatusDialog = true"
            class="status-btn"
          >
            <el-icon><Star /></el-icon>
            状态记录
          </el-button>
        </div>

        <!-- 导航选项 -->
        <div class="nav-options">
          <div 
            v-for="option in navOptions" 
            :key="option.id"
            class="nav-option"
            :class="{ active: activeNav === option.id }"
            @click="switchNav(option.id)"
          >
            <el-icon class="nav-icon"><component :is="option.icon" /></el-icon>
            <span class="nav-text">{{ option.name }}</span>
          </div>
        </div>

        <!-- 推荐关注 -->
        <div class="suggestions-section">
          <div class="section-title">推荐关注</div>
          <div class="suggestions-list">
            <div 
              v-for="suggestion in suggestions" 
              :key="suggestion.id"
              class="suggestion-item"
            >
              <el-avatar :size="36" :src="suggestion.avatar">
                {{ suggestion.name.charAt(0) }}
              </el-avatar>
              <div class="suggestion-info">
                <div class="suggestion-name">{{ suggestion.name }}</div>
                <div class="suggestion-desc">{{ suggestion.species }}</div>
              </div>
              <el-button size="small" type="primary" text class="follow-btn">
                关注
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧内容区域 -->
      <div class="right-content">
        <!-- 动态发布框 -->
        <el-card class="post-card-simple" v-if="activeNav !== 'footprints'">
          <el-input
            v-model="newMomentContent"
            type="textarea"
            :rows="2"
            placeholder="分享你的想法..."
            class="post-textarea"
            maxlength="500"
            show-word-limit
          />
          <div class="post-actions-simple">
            <div class="action-left">
              <el-button text @click="handleImageUpload">
                <el-icon><Picture /></el-icon>
                图片
              </el-button>
              <el-button text @click="handleVideoUpload">
                <el-icon><VideoCamera /></el-icon>
                视频
              </el-button>
            </div>
            <el-button
              type="primary"
              @click="publishMoment"
              :disabled="!newMomentContent.trim() || publishing"
              :loading="publishing"
              size="small"
            >
              发布
            </el-button>
          </div>

          <!-- 图片预览 -->
          <div v-if="uploadedImages.length" class="image-preview-simple">
            <div 
              v-for="(image, index) in uploadedImages" 
              :key="index" 
              class="preview-item"
            >
              <el-image
                :src="image.url"
                fit="cover"
                class="preview-image"
              />
              <el-icon class="remove-image" @click="removeImage(index)">
                <Close />
              </el-icon>
            </div>
          </div>
        </el-card>

        <!-- 动态时间线 -->
        <div class="moments-timeline">
          <!-- 关注的动态 -->
          <div v-if="activeNav === 'following'">
            <div v-if="loading" class="loading-state">
              <el-skeleton :rows="3" animated />
              <el-skeleton :rows="2" animated style="margin-top: 20px;" />
            </div>
            
            <div v-else-if="followingMoments.length === 0" class="empty-state">
              <el-empty description="还没有关注任何宠物，去发现更多小伙伴吧！" />
            </div>
            
            <div v-else v-for="moment in followingMoments" :key="moment.id" class="moment-item">
              <div class="simple-moment-card">
                <div class="moment-header">
                  <div class="pet-info">
                    <el-avatar :size="36" :src="moment.pet_avatar">
                      {{ moment.pet_name.charAt(0) }}
                    </el-avatar>
                    <div class="pet-details">
                      <div class="pet-name">{{ moment.pet_name }}</div>
                      <div class="moment-time">{{ new Date(moment.created_at).toLocaleString() }}</div>
                    </div>
                  </div>
                </div>
                <div class="moment-content">
                  <p>{{ moment.content }}</p>
                </div>
                <div class="moment-actions">
                  <span class="action-item">
                    <el-icon><Star /></el-icon>
                    {{ moment.like_count }}
                  </span>
                  <span class="action-item">
                    <el-icon><ChatDotRound /></el-icon>
                    {{ moment.comment_count }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- 我的动态 -->
          <div v-if="activeNav === 'my'">
            <div v-if="loading" class="loading-state">
              <el-skeleton :rows="3" animated />
            </div>
            
            <div v-else-if="moments.length === 0" class="empty-state">
              <el-empty description="还没有动态，快来分享第一个瞬间吧！" />
            </div>
            
            <div v-else v-for="moment in moments" :key="moment.id" class="moment-item">
              <div class="simple-moment-card">
                <div class="moment-header">
                  <div class="pet-info">
                    <el-avatar :size="36" :src="moment.pet_avatar">
                      {{ moment.pet_name.charAt(0) }}
                    </el-avatar>
                    <div class="pet-details">
                      <div class="pet-name">{{ moment.pet_name }}</div>
                      <div class="moment-time">{{ new Date(moment.created_at).toLocaleString() }}</div>
                    </div>
                  </div>
                </div>
                <div class="moment-content">
                  <p>{{ moment.content }}</p>
                </div>
                <div class="moment-actions">
                  <span class="action-item">
                    <el-icon><Star /></el-icon>
                    {{ moment.like_count }}
                  </span>
                  <span class="action-item">
                    <el-icon><ChatDotRound /></el-icon>
                    {{ moment.comment_count }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- 我的足迹 -->
          <div v-if="activeNav === 'footprints'" class="footprints-section">
            <el-empty description="足迹功能开发中，敬请期待..." />
          </div>
        </div>
      </div>
    </div>

    <!-- 图片上传输入 -->
    <input
      ref="imageInput"
      type="file"
      multiple
      accept="image/*"
      style="display: none"
      @change="handleImageSelect"
    >

    <!-- 视频上传输入 -->
    <input
      ref="videoInput"
      type="file"
      accept="video/*"
      style="display: none"
      @change="handleVideoSelect"
    >

    <!-- 状态记录对话框 -->
    <el-dialog
      v-model="showStatusDialog"
      title="宠物状态记录管理"
      width="600px"
      class="status-dialog"
    >
      <div class="status-content">
        <!-- 当前状态记录列表 -->
        <div class="status-records-section">
          <div class="section-header">
            <span>当前状态记录</span>
            <el-button
              size="small"
              type="primary"
              @click="showCreateStatusForm = true"
            >
              添加状态
            </el-button>
          </div>

          <div v-if="statusRecords.length === 0" class="empty-status">
            <el-empty description="暂无状态记录" />
          </div>

          <div v-else class="status-list">
            <div
              v-for="record in statusRecords"
              :key="record.statusRecordId"
              class="status-item"
            >
              <div class="status-info">
                <div class="status-name">{{ record.statusName || '未知状态' }}</div>
                <div class="status-dates">
                  <span>开始: {{ formatDate(record.startDate) }}</span>
                  <span v-if="record.endDate">结束: {{ formatDate(record.endDate) }}</span>
                  <span class="status-duration">持续: {{ calculateDuration(record.startDate, record.endDate) }}</span>
                </div>
                <div class="status-description">
                  {{ record.description || '暂无描述' }}
                </div>
              </div>
              <div class="status-actions">
                <el-button
                  v-if="!record.endDate"
                  size="small"
                  type="warning"
                  @click="stopStatus(record)"
                >
                  结束
                </el-button>
                <el-button
                  size="small"
                  type="danger"
                  @click="deleteStatus(record)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 创建状态表单对话框 -->
    <el-dialog
      v-model="showCreateStatusForm"
      title="添加状态记录"
      width="500px"
      class="create-status-dialog"
    >
      <el-form :model="statusForm" :rules="statusFormRules" ref="statusFormRef">
        <el-form-item label="状态名称" prop="statusName">
          <el-input
            v-model="statusForm.statusName"
            placeholder="例如：生病、怀孕、搬新家等"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="statusForm.description"
            type="textarea"
            :rows="3"
            placeholder="详细描述该状态的情况"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="状态类型" prop="statusType">
          <el-select
            v-model="statusForm.statusType"
            placeholder="选择状态类型"
            style="width: 100%"
          >
            <el-option label="健康状况" value="health" />
            <el-option label="行为状态" value="behavior" />
            <el-option label="生活环境" value="environment" />
            <el-option label="护理状态" value="care" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>

        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker
            v-model="statusForm.startDate"
            type="date"
            placeholder="选择开始日期"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreateStatusForm = false">取消</el-button>
        <el-button type="primary" @click="submitStatusForm" :loading="submittingStatus">
          创建
        </el-button>
      </template>
    </el-dialog>

    <!-- 使用统一的布局底部 -->
    <AppFooter />
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { markRaw } from 'vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import {
  Picture,
  VideoCamera,
  ChatDotRound,
  Star,
  Close,
  View,
  User,
  Collection,
  Setting,
  House
} from '@element-plus/icons-vue'

// 导入API服务
import { monitoredApiService } from '@/api/modules'
import { createMoment, getUserMoments } from '@/api/community'
import { uploadMultipleMedia } from '@/api/media'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// 响应式数据
const currentPet = ref({
  id: null,
  name: '',
  type: '',
  breed: '',
  age: '',
  gender: '',
  avatar_url: '',
  userId: null
})

const userPets = ref([]) // 用户的所有宠物
const activeNav = ref('following') // following, my, footprints
const moments = ref([])
const activityRecords = ref([]) // 活动记录
const statusRecords = ref([]) // 状态记录
const newMomentContent = ref('')
const publishing = ref(false)
const loading = ref(false)
const uploading = ref(false)
const uploadedImages = ref([])
const imageInput = ref(null)
const videoInput = ref(null)

// 状态记录对话框相关
const showStatusDialog = ref(false)
const showCreateStatusForm = ref(false)
const submittingStatus = ref(false)
const statusFormRef = ref()
const statusForm = ref({
  statusName: '',
  description: '',
  statusType: '',
  startDate: new Date().toISOString().split('T')[0]
})

// 状态表单验证规则
const statusFormRules = {
  statusName: [
    { required: true, message: '请输入状态名称', trigger: 'blur' }
  ],
  startDate: [
    { required: true, message: '请选择开始日期', trigger: 'change' }
  ]
}

// 导航选项
const navOptions = ref([
  { id: 'following', name: '关注的动态', icon: markRaw(View) },
  { id: 'my', name: '我的动态', icon: markRaw(User) },
  { id: 'footprints', name: '我的足迹', icon: markRaw(Collection) }
])

// 推荐关注列表（其他用户的热门宠物）
const suggestions = ref([])

// 计算属性
const currentUserId = computed(() => authStore.userId)

const followingMoments = computed(() => {
  // 这里应该从后端获取关注的动态
  // 暂时使用活动记录来模拟
  return activityRecords.value.slice(0, 5).map(record => ({
    id: record.activityRecordId,
    pet_name: currentPet.value.name,
    pet_avatar: currentPet.value.avatar_url,
    content: `${record.activityDescription}`,
    media_urls: [],
    created_at: record.activityDate,
    like_count: Math.floor(Math.random() * 20),
    comment_count: Math.floor(Math.random() * 10),
    liked: false,
    isOwn: true,
    type: 'activity'
  })).sort((a, b) => new Date(b.created_at) - new Date(a.created_at))
})

const statsData = computed(() => ({
  moments: moments.value.length,
  activities: activityRecords.value.length,
  statuses: statusRecords.value.length
}))

// 方法
const switchNav = (navId) => {
  activeNav.value = navId
}

const handleImageUpload = () => {
  imageInput.value?.click()
}

const handleVideoUpload = () => {
  videoInput.value?.click()
}

const handleImageSelect = (event) => {
  const files = event.target.files
  if (!files.length) return

  uploading.value = true
  
  Array.from(files).forEach(file => {
    if (file.type.startsWith('image/')) {
      const url = URL.createObjectURL(file)
      uploadedImages.value.push({
        file,
        url,
        name: file.name
      })
    }
  })

  uploading.value = false
  event.target.value = ''
}

const handleVideoSelect = (event) => {
  const file = event.target.files[0]
  if (file && file.type.startsWith('video/')) {
    ElMessage.info('视频上传功能开发中...')
  }
  event.target.value = ''
}

const removeImage = (index) => {
  URL.revokeObjectURL(uploadedImages.value[index].url)
  uploadedImages.value.splice(index, 1)
}

// API数据加载方法
const loadUserPets = async () => {
  if (!currentUserId.value) return

  try {
    loading.value = true
    const pets = await monitoredApiService.pets.getByUserId(currentUserId.value)
    userPets.value = pets || []

    // 如果没有当前宠物但有用户宠物，选择第一个
    if (!currentPet.value.id && userPets.value.length > 0) {
      currentPet.value = userPets.value[0]
      sessionStorage.setItem('currentPet', JSON.stringify(currentPet.value))
    }
  } catch (error) {
    console.error('加载用户宠物失败:', error)
    userPets.value = []
  }
}

const loadActivityRecords = async () => {
  if (!currentPet.value.id) return

  try {
    // 加载最近30天的活动记录
    const endDate = new Date()
    const startDate = new Date()
    startDate.setDate(startDate.getDate() - 30)

    const records = await monitoredApiService.activities.getRecords(currentPet.value.id, {
      startDate: startDate.toISOString(),
      endDate: endDate.toISOString()
    })
    activityRecords.value = records || []
  } catch (error) {
    console.error('加载活动记录失败:', error)
    activityRecords.value = []
  }
}

const loadStatusRecords = async () => {
  if (!currentPet.value.id) return

  try {
    // 加载最近的状态记录
    const records = await monitoredApiService.status.getRecords(currentPet.value.id, {
      startDate: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString(),
      endDate: new Date().toISOString()
    })
    statusRecords.value = records || []
  } catch (error) {
    console.error('加载状态记录失败:', error)
    statusRecords.value = []
  }
}

const loadSuggestions = async () => {
  try {
    // 获取所有宠物作为推荐（实际应该根据兴趣、地理位置等推荐）
    const allPetsResponse = await monitoredApiService.pets.getAll()

    // 处理API响应格式
    let allPets = []
    if (allPetsResponse && allPetsResponse.data) {
      allPets = Array.isArray(allPetsResponse.data) ? allPetsResponse.data : []
    } else if (Array.isArray(allPetsResponse)) {
      allPets = allPetsResponse
    }

    console.log('获取到的所有宠物数据:', allPets)

    // 过滤掉当前用户的宠物
    suggestions.value = allPets
      .filter(pet => pet.userId !== currentUserId.value)
      .slice(0, 6)
      .map(pet => ({
        id: pet.id,
        name: pet.name,
        species: `${pet.type} - ${pet.breed}`,
        avatar: pet.avatar_url
      }))
  } catch (error) {
    console.error('加载推荐宠物失败:', error)
    suggestions.value = []
  }
}

const loadPetData = async () => {
  if (!currentPet.value.id) return

  try {
    // 并行加载所有数据
    await Promise.all([
      loadActivityRecords(),
      loadStatusRecords()
    ])

    // 由于没有动态API，暂时用活动记录模拟
    moments.value = followingMoments.value

  } catch (error) {
    console.error('加载宠物数据失败:', error)
  }
}

const publishMoment = async () => {
  if (!newMomentContent.value.trim()) {
    ElMessage.warning('请输入动态内容')
    return
  }

  publishing.value = true

  try {
    // 上传媒体文件
    let mediaIds = []
    if (uploadedImages.value.length > 0) {
      ElMessage.info('正在上传媒体文件...')

      try {
        const files = uploadedImages.value.map(img => img.file)
        const uploadResults = await uploadMultipleMedia({
          files: files,
          relatedType: 'MOMENT',
          relatedId: currentUserId.value,
          userId: currentUserId.value,
          batchSize: 3,            // 每批上传3个文件，减少服务器压力
          delayMs: 500,            // 每批间隔500ms，给服务器缓冲时间
          onProgress: (completed, total, currentFile) => {
            // 可选：更新进度提示
            if (completed % 5 === 0 || completed === total) {
              ElMessage.info(`上传进度: ${completed}/${total}`)
            }
          }
        })

        mediaIds = uploadResults.map(result => result.mediaId)
        ElMessage.success(`成功上传 ${mediaIds.length} 个文件`)
      } catch (uploadError) {
        console.error('上传媒体文件失败:', uploadError)
        ElMessage.warning('部分媒体文件上传失败，将只发布已上传的内容')
        // 保留已成功的文件
        mediaIds = uploadResults?.map(result => result?.mediaId).filter(id => id) || []
      }
    }

    // 2. 创建动态
    const momentData = {
      userId: currentUserId.value,
      content: newMomentContent.value.trim(),
      mediaIds: mediaIds
    }

    const newMoment = await createMoment(momentData)

    ElMessage.success('发布动态成功！')
    newMomentContent.value = ''
    uploadedImages.value.forEach(img => URL.revokeObjectURL(img.url))
    uploadedImages.value = []

    // 重新加载动态列表以确保数据一致性
    await loadUserMoments()

  } catch (error) {
    console.error('发布动态失败:', error)
    ElMessage.error('发布动态失败: ' + error.message)
  } finally {
    publishing.value = false
  }
}

// 从URL参数或sessionStorage获取宠物信息
const getCurrentPet = async () => {
  console.log('getCurrentPet: 开始执行，currentUserId.value =', currentUserId.value)
  if (!currentUserId.value) {
    // 如果用户未登录，跳转到登录页
    console.log('getCurrentPet: 用户未登录，跳转到登录页')
    router.push('/login')
    return
  }

  const petId = route.query.pet_id
  const savedPet = sessionStorage.getItem('currentPet')

  if (petId) {
    // 从URL参数获取宠物ID
    await fetchPetInfo(parseInt(petId))
  } else if (savedPet) {
    // 从sessionStorage获取
    const parsedPet = JSON.parse(savedPet)
    if (parsedPet.userId === currentUserId.value) {
      currentPet.value = parsedPet
    } else {
      // 宠物不属于当前用户，重新获取
      await loadUserPets()
    }
  } else {
    // 加载用户宠物并选择第一个
    await loadUserPets()
  }

  // 加载宠物数据
  await loadPetData()
  await loadSuggestions()
  // 加载用户动态
  await loadUserMoments()
}

// 加载用户动态
const loadUserMoments = async () => {
  if (!currentUserId.value) {
    console.log('loadUserMoments: currentUserId为空，跳过加载')
    return
  }

  try {
    console.log('loadUserMoments: 开始加载用户动态，userId:', currentUserId.value)
    const userMoments = await getUserMoments(currentUserId.value)
    console.log('loadUserMoments: 从API获取到的原始数据:', userMoments)

    // 转换为前端显示格式
    moments.value = userMoments.map(moment => {
      const formatted = {
        id: moment.id,
        userId: moment.userId,
        content: moment.content,
        media_urls: moment.mediaUrls || [],
        created_at: moment.createdAt,
        like_count: moment.likeCount || 0,
        comment_count: moment.commentCount || 0,
        liked: false,
        isOwn: true
      }
      console.log('格式化动态:', formatted)
      return formatted
    })

    console.log('loadUserMoments: 最终moments数组长度:', moments.value.length)
  } catch (error) {
    console.error('加载用户动态失败:', error)
    moments.value = []
  }
}

const fetchPetInfo = async (petId) => {
  try {
    const pet = await monitoredApiService.pets.getById(petId)
    if (pet && pet.userId === currentUserId.value) {
      currentPet.value = pet
      sessionStorage.setItem('currentPet', JSON.stringify(currentPet.value))
    } else {
      ElMessage.warning('该宠物不属于您')
      await loadUserPets()
    }
  } catch (error) {
    console.error('获取宠物信息失败:', error)
    ElMessage.error('获取宠物信息失败')
    await loadUserPets()
  }
}

const switchPet = async (pet) => {
  currentPet.value = pet
  sessionStorage.setItem('currentPet', JSON.stringify(currentPet.value))

  // 更新URL参数
  router.push({
    query: { ...route.query, pet_id: pet.id }
  })

  // 重新加载数据
  await loadPetData()
}

// 处理宠物选择器变化
const handlePetChange = async (petId) => {
  const selectedPet = userPets.value.find(pet => pet.id === petId)
  if (selectedPet) {
    await switchPet(selectedPet)
  }
}

// 监听路由变化
watch(
  () => route.query.pet_id,
  async (newPetId) => {
    if (newPetId && newPetId !== currentPet.value.id) {
      await getCurrentPet()
    }
  }
)

// 计算活跃天数
const getActiveDays = () => {
  const allRecords = [...activityRecords.value, ...statusRecords.value]
  if (allRecords.length === 0) return 0

  // 获取所有记录的日期
  const dates = allRecords.map(record => {
    const dateStr = record.activityDate || record.startDate
    return dateStr ? dateStr.split('T')[0] : null
  }).filter(Boolean)

  // 去重并返回不重复的天数
  const uniqueDates = [...new Set(dates)]
  return uniqueDates.length
}

// 状态相关方法
const openStatusDialog = () => {
  showStatusDialog.value = true
  showCreateStatusForm.value = false
}

const createNewStatus = () => {
  showCreateStatusForm.value = true
}

const cancelCreateStatus = () => {
  showCreateStatusForm.value = false
  statusForm.value = {
    statusName: '',
    description: '',
    statusType: '',
    startDate: new Date().toISOString().split('T')[0]
  }
}

// 提交状态记录
const submitStatusForm = async () => {
  if (!statusFormRef.value) return

  try {
    await statusFormRef.value.validate()
    submittingStatus.value = true

    // 准备状态数据
    const statusData = {
      statusName: statusForm.value.statusName,
      description: statusForm.value.description,
      statusType: statusForm.value.statusType,
      startDate: statusForm.value.startDate,
      petId: currentPet.value.id
    }

    console.log('创建状态记录:', statusData)

    // 调用状态创建API
    const response = await fetch('http://localhost:8082/api/status-records', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: JSON.stringify(statusData)
    })

    if (!response.ok) {
      throw new Error(`创建状态记录失败: ${response.statusText}`)
    }

    ElMessage.success('状态记录创建成功')

    // 关闭表单并重置
    showCreateStatusForm.value = false
    statusForm.value = {
      statusName: '',
      description: '',
      statusType: '',
      startDate: new Date().toISOString().split('T')[0]
    }

    // 重新加载状态记录
    await loadStatusRecords()

  } catch (error) {
    console.error('创建状态记录失败:', error)
    ElMessage.error(`创建状态记录失败: ${error.message}`)
  } finally {
    submittingStatus.value = false
  }
}

// 停止状态记录
const stopStatus = async (record) => {
  try {
    const stopDate = new Date().toISOString().split('T')[0]

    const response = await fetch(`http://localhost:8082/api/status-records/${record.statusId}/stop`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      },
      body: JSON.stringify({ endDate: stopDate })
    })

    if (!response.ok) {
      throw new Error(`停止状态记录失败: ${response.statusText}`)
    }

    ElMessage.success('状态记录已停止')
    await loadStatusRecords()

  } catch (error) {
    console.error('停止状态记录失败:', error)
    ElMessage.error(`停止状态记录失败: ${error.message}`)
  }
}

// 删除状态记录
const deleteStatus = async (record) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除状态记录"${record.statusName}"吗？此操作不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const response = await fetch(`http://localhost:8082/api/status-records/${record.statusId}`, {
      method: 'DELETE',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    })

    if (!response.ok) {
      throw new Error(`删除状态记录失败: ${response.statusText}`)
    }

    ElMessage.success('状态记录已删除')
    await loadStatusRecords()

  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除状态记录失败:', error)
      ElMessage.error(`删除状态记录失败: ${error.message}`)
    }
  }
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '未设置'
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

// 计算持续天数
const calculateDuration = (startDate, endDate = null) => {
  if (!startDate) return 0

  const start = new Date(startDate)
  const end = endDate ? new Date(endDate) : new Date()

  // 计算天数差
  const diffTime = Math.abs(end - start)
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))

  return diffDays
}

// 导航方法
const goSettings = () => {
  ElMessage.info('设置功能开发中...')
}

// 生命周期
onMounted(async () => {
  console.log('onMounted: currentUserId.value =', currentUserId.value)
  console.log('onMounted: authStore.userId =', authStore.userId)

  // 临时绕过用户认证检查，强制加载动态进行调试
  if (!currentUserId.value) {
    console.log('onMounted: 用户ID为空，临时设置为76进行调试')
    // 临时硬编码用户ID进行调试
    authStore.updateUser({ id: 76 })
  }

  await getCurrentPet()
})
</script>

<style scoped>
.pet-space-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
}

/* ===== Hero Section ===== */
.hero-section {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%),
              url('https://images.unsplash.com/photo-1601758225944-81c77fa65028?q=80&w=1332&auto=format&fit=crop') center/cover no-repeat;
  padding: 4rem 0;
  position: relative;
  display: flex;
  align-items: center;
}

.hero-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.2) 0%, rgba(118, 75, 162, 0.2) 100%);
  z-index: 1;
}

.hero-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
  text-align: center;
  position: relative;
  z-index: 2;
}

.hero-content {
  max-width: 800px;
  margin: 0 auto;
}

.hero-emoji {
  font-size: 4rem;
  margin-bottom: 1.5rem;
  line-height: 1;
}

.hero-title {
  font-size: 3rem;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 1rem;
  line-height: 1.2;
}

.hero-subtitle {
  font-size: 1.25rem;
  color: #64748b;
  margin-bottom: 0;
  line-height: 1.6;
}

/* 主要内容区域 */

/* 顶部导航栏 */
.top-navigation {
  position: sticky;
  top: 0;
  z-index: 1000;
  background: white;
  border-bottom: 1px solid #e8e8e8;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.nav-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  max-width: 100vw;
  height: 60px;
}

.back-home-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.back-home-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.back-home-btn .el-icon {
  font-size: 16px;
}

.page-title h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #2c3e50;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 背景图片 */
.background-image {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-image: url('images/pet-space-bg.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  opacity: 0.03;
  z-index: -1;
}
/* 主要内容区域 */
.main-content-simple {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 0;
  max-width: 100vw;
  margin: 0;
  min-height: 100vh;
}

/* 左侧边栏 */
.left-sidebar {
  background: white;
  border-right: 1px solid #e8e8e8;
  padding: 24px 20px;
  height: 100vh;
  position: sticky;
  top: 0;
  overflow-y: auto;
}

/* 用户信息 */
.user-info-simple {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.user-avatar {
  background: linear-gradient(135deg, #FF8C00 0%, #FFD700 100%);
}

.user-details {
  flex: 1;
}

.user-name {
  font-weight: 600;
  font-size: 1.1rem;
  color: #2c3e50;
  margin-bottom: 2px;
}

.user-species {
  font-size: 0.85rem;
  color: #7f8c8d;
}

/* 当前宠物信息 */
.current-pet-info {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.pet-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #f8f9fa;
  padding: 12px;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.pet-selector:hover {
  background: #e9ecef;
}

.pet-avatar-small {
  flex-shrink: 0;
}

.pet-details-small {
  flex: 1;
  min-width: 0;
}

.pet-name-small {
  font-weight: 600;
  font-size: 0.9rem;
  color: #2c3e50;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pet-breed-small {
  font-size: 0.75rem;
  color: #7f8c8d;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.pet-select {
  width: 80px;
  flex-shrink: 0;
}

.pet-select :deep(.el-input__inner) {
  font-size: 0.8rem;
  padding: 4px 8px;
}

/* 统计数据 */
.stats-simple {
  display: flex;
  justify-content: space-around;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.stat-item {
  text-align: center;
}

.stat-number {
  font-size: 1.2rem;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 2px;
}

.stat-label {
  font-size: 0.75rem;
  color: #7f8c8d;
}

/* 导航选项 */
.nav-options {
  margin-bottom: 24px;
}

.nav-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 4px;
}

.nav-option:hover {
  background: #f8f9fa;
}

.nav-option.active {
  background: #e3f2fd;
  color: #1890ff;
}

.nav-icon {
  font-size: 1.1rem;
}

.nav-text {
  font-weight: 500;
  font-size: 0.95rem;
}

/* 推荐关注 */
.suggestions-section {
  border-top: 1px solid #f0f0f0;
  padding-top: 20px;
}

.section-title {
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 16px;
  font-size: 0.9rem;
}

.suggestions-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
}

.suggestion-info {
  flex: 1;
}

.suggestion-name {
  font-weight: 500;
  font-size: 0.85rem;
  color: #2c3e50;
  margin-bottom: 1px;
}

.suggestion-desc {
  font-size: 0.75rem;
  color: #7f8c8d;
}

.follow-btn {
  font-size: 0.75rem;
  padding: 4px 8px;
}

/* 右侧内容区域 */
.right-content {
  padding: 24px;
  overflow-y: auto;
  height: 100vh;
}

/* 发布卡片 */
.post-card-simple {
  border-radius: 12px;
  margin-bottom: 20px;
  border: 1px solid #e8e8e8;
}

.post-textarea {
  margin-bottom: 12px;
}

.post-textarea :deep(.el-textarea__inner) {
  border: none;
  resize: none;
  font-size: 1rem;
  line-height: 1.6;
  padding: 12px 16px;
  min-height: 80px;
}

.post-actions-simple {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.action-left {
  display: flex;
  gap: 8px;
}

/* 图片预览 */
.image-preview-simple {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.preview-item {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #e8e8e8;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-image {
  position: absolute;
  top: 2px;
  right: 2px;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  border-radius: 50%;
  padding: 3px;
  cursor: pointer;
  font-size: 12px;
}

.remove-image:hover {
  background: rgba(0, 0, 0, 0.7);
}

/* 动态时间线 */
.moments-timeline {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.loading-state, .empty-state {
  padding: 40px 20px;
  text-align: center;
}

.footprints-section {
  padding: 60px 20px;
  text-align: center;
}

/* 简单动态卡片样式 */
.simple-moment-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #e8e8e8;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: box-shadow 0.2s ease;
  margin-left: 50px;
}

.simple-moment-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.moment-header {
  margin-bottom: 16px;
}

.pet-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pet-details {
  flex: 1;
}

.pet-name {
  font-weight: 600;
  font-size: 0.95rem;
  color: #2c3e50;
  margin-bottom: 2px;
}

.moment-time {
  font-size: 0.75rem;
  color: #7f8c8d;
}

.moment-content {
  margin-bottom: 16px;
}

.moment-content p {
  margin: 0;
  line-height: 1.6;
  color: #2c3e50;
  font-size: 0.95rem;
}

.moment-actions {
  display: flex;
  gap: 24px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.85rem;
  color: #7f8c8d;
  cursor: pointer;
  transition: color 0.2s ease;
}

.action-item:hover {
  color: #1890ff;
}

.action-item .el-icon {
  font-size: 1rem;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .main-content-simple {
    grid-template-columns: 1fr;
  }

  .left-sidebar {
    display: none;
  }

  .right-content {
    padding: 16px;
  }

  .simple-moment-card {
    padding: 16px;
  }

  .moment-actions {
    gap: 20px;
  }
}
</style>