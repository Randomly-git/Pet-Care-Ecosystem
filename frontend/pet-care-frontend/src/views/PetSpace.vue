<template>
  <div class="pet-space-container">
    <!-- 自定义背景图片 -->
    <div class="background-image"></div>

    <!-- 主要内容区域 -->
    <div class="main-content-simple">
      <!-- 左侧边栏 -->
      <div class="left-sidebar">
        <!-- 用户信息 -->
        <div class="user-info-simple">
          <el-avatar :size="50" :src="currentPet.avatar_url" class="user-avatar">
            {{ currentPet.name.charAt(0) }}
          </el-avatar>
          <div class="user-details">
            <div class="user-name">{{ currentPet.name }}</div>
            <div class="user-species">{{ currentPet.species }}</div>
          </div>
        </div>

        <!-- 统计数据 -->
        <div class="stats-simple">
          <div class="stat-item">
            <div class="stat-number">{{ moments.length }}</div>
            <div class="stat-label">动态</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">12</div>
            <div class="stat-label">关注</div>
          </div>
          <div class="stat-item">
            <div class="stat-number">45</div>
            <div class="stat-label">粉丝</div>
          </div>
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
              <MomentCard :moment="moment" />
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
              <MomentCard :moment="moment" :is-own="true" />
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
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  House,
  Picture,
  VideoCamera,
  ChatDotRound,
  Star,
  Share,
  Close,
  View,
  User,
  Collection
} from '@element-plus/icons-vue'

// 导入动态卡片组件
import MomentCard from '../components/MomentCard.vue'

const router = useRouter()
const route = useRoute()

// API 基础 URL
const API_BASE = 'http://localhost:5001/api/v1/pet-space'

// 响应式数据
const currentPet = ref({
  id: 1,
  name: '咪咪',
  species: '英国短毛猫',
  avatar_url: ''
})

const activeNav = ref('following') // following, my, footprints
const moments = ref([])
const newMomentContent = ref('')
const publishing = ref(false)
const loading = ref(false)
const uploading = ref(false)
const uploadedImages = ref([])
const imageInput = ref(null)
const videoInput = ref(null)

// 导航选项
const navOptions = ref([
  { id: 'following', name: '关注的动态', icon: View },
  { id: 'my', name: '我的动态', icon: User },
  { id: 'footprints', name: '我的足迹', icon: Collection }
])

// 推荐关注列表
const suggestions = ref([
  { id: 1, name: '豆豆', species: '柯基犬', avatar: '' },
  { id: 2, name: '布丁', species: '布偶猫', avatar: '' },
  { id: 3, name: '旺财', species: '金毛犬', avatar: '' },
  { id: 4, name: '小白', species: '波斯猫', avatar: '' }
])

// 关注的动态（模拟数据）
const followingMoments = computed(() => {
  // 这里应该从后端获取关注的动态
  // 暂时使用我的动态 + 模拟数据
  return [
    ...moments.value.map(m => ({ ...m, isOwn: true })),
    {
      id: 1001,
      pet_name: '豆豆',
      pet_avatar: '',
      content: '今天在公园玩得好开心！遇到了好多小伙伴～',
      media_urls: [],
      created_at: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
      like_count: 8,
      comment_count: 3,
      liked: false,
      isOwn: false
    },
    {
      id: 1002,
      pet_name: '布丁',
      pet_avatar: '',
      content: '新买的猫爬架太喜欢了，可以在上面睡一整天😴',
      media_urls: [],
      created_at: new Date(Date.now() - 4 * 60 * 60 * 1000).toISOString(),
      like_count: 15,
      comment_count: 5,
      liked: false,
      isOwn: false
    }
  ].sort((a, b) => new Date(b.created_at) - new Date(a.created_at))
})

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

const publishMoment = async () => {
  if (!newMomentContent.value.trim()) {
    ElMessage.warning('请输入动态内容')
    return
  }

  publishing.value = true

  try {
    console.log('正在发布动态，内容:', newMomentContent.value)
    console.log('目标宠物ID:', currentPet.value.id)

    const response = await fetch(`${API_BASE}/pets/${currentPet.value.id}/moments`, {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        content: newMomentContent.value,
        media_urls: uploadedImages.value.map(img => img.url) // 如果有图片
      })
    })

    console.log('响应状态:', response.status)
    
    if (response.ok) {
      const newMoment = await response.json()
      console.log('发布成功，返回数据:', newMoment)
      
      ElMessage.success('发布动态成功！')
      newMomentContent.value = ''
      uploadedImages.value.forEach(img => URL.revokeObjectURL(img.url))
      uploadedImages.value = []
      
      // 重新加载动态
      await loadMoments()
    } else {
      const errorData = await response.json().catch(() => ({ error: '未知错误' }))
      console.error('发布失败，错误信息:', errorData)
      throw new Error(errorData.error || `HTTP ${response.status}: ${response.statusText}`)
    }
  } catch (error) {
    console.error('发布动态失败:', error)
    ElMessage.error('发布动态失败: ' + error.message)
  } finally {
    publishing.value = false
  }
}

const loadMoments = async () => {
  loading.value = true
  try {
    const response = await fetch(`${API_BASE}/pets/${currentPet.value.id}/moments`)
    
    if (response.ok) {
      const momentsData = await response.json()
      moments.value = momentsData.map(moment => ({
        ...moment,
        liked: false,
        like_count: moment.like_count || 0,
        comment_count: moment.comment_count || 0,
        pet_name: currentPet.value.name,
        pet_avatar: currentPet.value.avatar_url,
        isOwn: true
      }))
    } else {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }
  } catch (error) {
    console.error('加载动态失败:', error)
    moments.value = []
  } finally {
    loading.value = false
  }
}

// 从URL参数或sessionStorage获取宠物信息
const getCurrentPet = async () => {
  const petId = route.query.pet_id || currentPet.value.id
  const savedPet = sessionStorage.getItem('currentPet')
  
  if (savedPet) {
    const parsedPet = JSON.parse(savedPet)
    if (parsedPet.id == petId) {
      currentPet.value = parsedPet
      await loadMoments()
      return
    }
  }
  
  // 从后端获取宠物信息
  await fetchPetInfo(parseInt(petId))
}

const fetchPetInfo = async (petId) => {
  try {
    const response = await fetch(`${API_BASE}/pets/${petId}`)
    if (response.ok) {
      const petData = await response.json()
      currentPet.value = petData
      sessionStorage.setItem('currentPet', JSON.stringify(currentPet.value))
      await loadMoments()
    } else {
      throw new Error('获取宠物信息失败')
    }
  } catch (error) {
    console.error('获取宠物信息失败:', error)
    await loadMoments()
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

// 生命周期
onMounted(async () => {
  await getCurrentPet()
})
</script>

<style scoped>
.pet-space-container {
  min-height: 100vh;
  width: 100vw;
  position: relative;
  background-color: #f8f9fa;
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
  font-size: 0.95rem;
  line-height: 1.5;
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
}
</style>