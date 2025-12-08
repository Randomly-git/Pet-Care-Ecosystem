<template>
  <div class="moments-page">
    <!-- 使用统一的布局头部 -->
    <AppHeader />

    <!-- Hero Section -->
    <section class="hero-section">
      <div class="hero-container">
        <div class="hero-content">
          <div class="hero-emoji">🌟</div>
          <h2 class="hero-title">宠物社区</h2>
          <p class="hero-subtitle">分享宠物的精彩瞬间，发现更多可爱故事</p>
        </div>
      </div>
    </section>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <div class="container">
        <!-- 发布动态区域 -->
        <div class="post-section">
          <div class="post-card">
            <div class="post-input-area">
              <el-avatar :size="48" :src="userAvatar">
                {{ userName.charAt(0) }}
              </el-avatar>
              <div class="post-input-wrapper">
                <el-input
                  v-model="newPostContent"
                  type="textarea"
                  :rows="3"
                  placeholder="分享你和宠物的故事..."
                  maxlength="500"
                  show-word-limit
                  resize="none"
                  @focus="showPostOptions = true"
                />

                <!-- 图片上传区域 -->
                <div v-if="showPostOptions" class="post-options">
                  <div class="upload-area">
                    <el-upload
                      ref="uploadRef"
                      :auto-upload="false"
                      :show-file-list="false"
                      accept="image/*"
                      multiple
                      @change="handleImageSelect"
                    >
                      <el-button size="small" plain>
                        <el-icon><Picture /></el-icon>
                        图片
                      </el-button>
                    </el-upload>

                    <el-button size="small" @click="showPostOptions = false">
                      取消
                    </el-button>

                    <el-button
                      type="primary"
                      size="small"
                      @click="publishPost"
                      :disabled="!newPostContent.trim() || publishing"
                      :loading="publishing"
                    >
                      发布
                    </el-button>
                  </div>

                  <!-- 图片预览 -->
                  <div v-if="uploadedImages.length" class="image-preview">
                    <div
                      v-for="(image, index) in uploadedImages"
                      :key="index"
                      class="preview-item"
                    >
                      <img :src="image.url" :alt="`预览${index + 1}`" />
                      <el-button
                        class="remove-btn"
                        type="danger"
                        size="small"
                        circle
                        @click="removeImage(index)"
                      >
                        <el-icon><Close /></el-icon>
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 动态列表 -->
        <div class="moments-list">
          <div v-if="loading" class="loading-container">
            <el-skeleton :rows="5" animated />
          </div>

          <div v-else-if="moments.length === 0" class="empty-state">
            <el-empty description="还没有动态，快来发布第一条吧！" />
          </div>

          <div v-else class="moment-cards">
            <div
              v-for="moment in moments"
              :key="moment.id"
              class="moment-card"
            >
              <!-- 用户信息 -->
              <div class="moment-header">
                <el-avatar :size="45" :src="moment.userAvatar">
                  {{ moment.userName.charAt(0) }}
                </el-avatar>
                <div class="user-info">
                  <div class="user-name">{{ moment.userName }}</div>
                  <div class="post-time">{{ formatTime(moment.created_at) }}</div>
                </div>
                <el-dropdown trigger="click" v-if="moment.isOwn">
                  <el-button type="text" :icon="MoreFilled" />
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item @click="deleteMoment(moment.id)">
                        删除动态
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>

              <!-- 动态内容 -->
              <div class="moment-content">
                <p>{{ moment.content }}</p>

                <!-- 图片展示 -->
                <div v-if="moment.media_urls && moment.media_urls.length" class="moment-images">
                  <div
                    v-for="(url, index) in moment.media_urls"
                    :key="index"
                    class="image-item"
                    :class="getImageClass(moment.media_urls.length)"
                  >
                    <img :src="url" :alt="`图片${index + 1}`" @click="previewImage(url)" />
                  </div>
                </div>
              </div>

              <!-- 互动区域 -->
              <div class="moment-actions">
                <el-button
                  type="text"
                  :class="{ 'is-liked': moment.liked }"
                  @click="toggleLike(moment)"
                >
                  <el-icon><Star /></el-icon>
                  {{ moment.like_count || 0 }}
                </el-button>

                <el-button type="text" @click="showComments(moment)">
                  <el-icon><ChatDotRound /></el-icon>
                  {{ moment.comment_count || 0 }}
                </el-button>
              </div>

              <!-- 评论区 -->
              <div v-if="moment.showComments" class="comments-section">
                <!-- 评论输入 -->
                <div class="comment-input">
                  <el-input
                    v-model="moment.commentText"
                    placeholder="写评论..."
                    @keyup.enter="submitComment(moment)"
                  >
                    <template #append>
                      <el-button @click="submitComment(moment)">发送</el-button>
                    </template>
                  </el-input>
                </div>

                <!-- 评论列表 -->
                <div v-if="moment.comments && moment.comments.length" class="comments-list">
                  <div
                    v-for="comment in moment.comments"
                    :key="comment.id"
                    class="comment-item"
                  >
                    <el-avatar :size="32" :src="comment.userAvatar">
                      {{ comment.userName.charAt(0) }}
                    </el-avatar>
                    <div class="comment-content">
                      <div class="comment-header">
                        <span class="comment-user">{{ comment.userName }}</span>
                        <span class="comment-time">{{ formatTime(comment.created_at) }}</span>
                      </div>
                      <div class="comment-text">{{ comment.content }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 图片预览对话框 -->
    <el-dialog v-model="previewVisible" title="图片预览" width="80%">
      <img :src="previewImageUrl" style="width: 100%" />
    </el-dialog>

    <!-- 使用统一的布局底部 -->
    <AppFooter />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { Picture, Close, Star, ChatDotRound, MoreFilled } from '@element-plus/icons-vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import { createMoment, deleteMoment as deleteMomentApi, toggleLike as toggleLikeApi, createComment, getMomentComments } from '@/api/community'
import { uploadMultipleMedia } from '@/api/media'
import { getUserPets } from '@/api/pets'

const authStore = useAuthStore()

// 响应式数据
const loading = ref(false)
const publishing = ref(false)
const showPostOptions = ref(false)
const previewVisible = ref(false)
const previewImageUrl = ref('')
const newPostContent = ref('')
const uploadedImages = ref([])
const moments = ref([])

// 用户信息
const userName = computed(() => authStore.userName || '用户')
const userAvatar = computed(() => authStore.avatar || '')

// 方法
const handleImageSelect = (file) => {
  if (uploadedImages.value.length >= 9) {
    ElMessage.warning('最多只能上传9张图片')
    return
  }

  const reader = new FileReader()
  reader.onload = (e) => {
    uploadedImages.value.push({
      file: file.raw,
      url: e.target.result
    })
  }
  reader.readAsDataURL(file.raw)
}

const removeImage = (index) => {
  uploadedImages.value.splice(index, 1)
}

const getImageClass = (count) => {
  if (count === 1) return 'single'
  if (count === 2 || count === 4) return 'grid-2'
  return 'grid-3'
}

const formatTime = (time) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) {
    return '刚刚'
  } else if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`
  } else if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`
  } else if (diff < 604800000) {
    return `${Math.floor(diff / 86400000)}天前`
  } else {
    return date.toLocaleDateString()
  }
}

const previewImage = (url) => {
  previewImageUrl.value = url
  previewVisible.value = true
}

const publishPost = async () => {
  if (!newPostContent.value.trim()) {
    ElMessage.warning('请输入动态内容')
    return
  }

  publishing.value = true

  try {
    // 暂时跳过媒体上传，只发布文本内容
    // TODO: 媒体上传功能需要后端服务正常工作
    let mediaIds = []
    if (uploadedImages.value.length > 0) {
      ElMessage.warning('媒体上传功能暂时不可用，将只发布文本内容')
    }

    // 2. 创建动态
    const momentData = {
      userId: authStore.userId,
      content: newPostContent.value.trim(),
      mediaIds: mediaIds
    }

    const newMoment = await createMoment(momentData)

    // 3. 转换为前端显示格式
    const displayMoment = {
      id: newMoment.id,
      userId: newMoment.userId,
      userName: userName.value,
      userAvatar: userAvatar.value,
      content: newMoment.content,
      media_urls: newMoment.mediaUrls || [],
      created_at: newMoment.createdAt,
      like_count: newMoment.likeCount || 0,
      comment_count: newMoment.commentCount || 0,
      liked: false,
      isOwn: true,
      showComments: false,
      commentText: '',
      comments: []
    }

    // 添加到动态列表
    moments.value.unshift(displayMoment)

    ElMessage.success('发布成功！')
    newPostContent.value = ''
    uploadedImages.value = []
    showPostOptions.value = false

  } catch (error) {
    console.error('发布动态失败:', error)
    ElMessage.error('发布失败: ' + error.message)
  } finally {
    publishing.value = false
  }
}

const toggleLike = async (moment) => {
  try {
    await toggleLikeApi({
      userId: authStore.userId,
      targetType: 'MOMENT',
      targetId: moment.id
    })

    moment.liked = !moment.liked
    moment.like_count = moment.liked ? (moment.like_count + 1) : (moment.like_count - 1)
  } catch (error) {
    console.error('点赞失败:', error)
    ElMessage.error('操作失败')
  }
}

const showComments = async (moment) => {
  moment.showComments = !moment.showComments

  if (moment.showComments && (!moment.comments || moment.comments.length === 0)) {
    try {
      const comments = await getMomentComments(moment.id)
      moment.comments = comments.map(comment => ({
        id: comment.id,
        userId: comment.userId,
        userName: `用户${comment.userId}`, // 实际应该从用户API获取
        userAvatar: '',
        content: comment.content,
        created_at: comment.createdAt,
        parentId: comment.parentId,
        replies: comment.replies || []
      }))
    } catch (error) {
      console.error('加载评论失败:', error)
    }
  }
}

const submitComment = async (moment) => {
  if (!moment.commentText.trim()) {
    return
  }

  try {
    const newComment = await createComment({
      userId: authStore.userId,
      momentId: moment.id,
      content: moment.commentText.trim()
    })

    moment.comments.unshift({
      id: newComment.id,
      userId: authStore.userId,
      userName: userName.value,
      userAvatar: userAvatar.value,
      content: newComment.content,
      created_at: new Date().toISOString()
    })

    moment.comment_count++
    moment.commentText = ''
  } catch (error) {
    console.error('评论失败:', error)
    ElMessage.error('评论失败')
  }
}

const deleteMoment = async (momentId) => {
  try {
    await ElMessageBox.confirm('确定要删除这条动态吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteMomentApi(momentId)

    const index = moments.value.findIndex(m => m.id === momentId)
    if (index > -1) {
      moments.value.splice(index, 1)
    }

    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const loadMoments = async () => {
  // 这里应该加载所有用户的动态，目前先加载自己的
  // TODO: 实现获取所有关注用户的动态
  loading.value = true
  try {
    // 暂时显示自己的动态
    const userPets = await getUserPets(authStore.userId)
    // 可以从其他地方获取动态列表
  } catch (error) {
    console.error('加载动态失败:', error)
  } finally {
    loading.value = false
  }
}

// 生命周期
onMounted(() => {
  loadMoments()
})
</script>

<style scoped>
.moments-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
}

/* Hero Section */
.hero-section {
  background: linear-gradient(135deg, rgba(251, 146, 60, 0.1) 0%, rgba(250, 204, 21, 0.1) 100%),
              url('https://images.unsplash.com/photo-1601758228847-18c8c8d2689e?q=80&w=1332&auto=format&fit=crop') center/cover no-repeat;
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
  background: linear-gradient(135deg, rgba(251, 146, 60, 0.2) 0%, rgba(250, 204, 21, 0.2) 100%);
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

/* Main Content */
.main-content {
  padding: 2rem 0;
}

.container {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 1rem;
}

/* Post Section */
.post-section {
  margin-bottom: 2rem;
}

.post-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 1.5rem;
}

.post-input-area {
  display: flex;
  gap: 1rem;
}

.post-input-wrapper {
  flex: 1;
}

.post-options {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #e2e8f0;
}

.upload-area {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.image-preview {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 0.5rem;
}

.preview-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
}

.preview-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 24px;
  height: 24px;
  padding: 0;
}

/* Moments List */
.loading-container {
  padding: 2rem;
}

.empty-state {
  padding: 3rem;
  background: white;
  border-radius: 12px;
  text-align: center;
}

.moment-cards {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.moment-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 1.5rem;
  transition: all 0.2s;
}

.moment-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* Moment Header */
.moment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

.user-info {
  flex: 1;
  margin-left: 0.75rem;
}

.user-name {
  font-weight: 600;
  color: #1e293b;
}

.post-time {
  font-size: 0.875rem;
  color: #64748b;
  margin-top: 0.25rem;
}

/* Moment Content */
.moment-content {
  margin-bottom: 1rem;
}

.moment-content p {
  color: #475569;
  line-height: 1.6;
  margin-bottom: 1rem;
}

.moment-images {
  display: grid;
  gap: 0.25rem;
}

.moment-images.single {
  grid-template-columns: 1fr;
  max-width: 400px;
}

.moment-images.grid-2 {
  grid-template-columns: repeat(2, 1fr);
}

.moment-images.grid-3 {
  grid-template-columns: repeat(3, 1fr);
}

.image-item img {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.2s;
}

.image-item img:hover {
  transform: scale(1.02);
}

.moment-images.grid-2 img,
.moment-images.grid-3 img {
  height: 150px;
}

/* Moment Actions */
.moment-actions {
  display: flex;
  gap: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #f1f5f9;
}

.moment-actions .el-button {
  color: #64748b;
}

.moment-actions .el-button.is-liked {
  color: #f59e0b;
}

/* Comments Section */
.comments-section {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #f1f5f9;
}

.comment-input {
  margin-bottom: 1rem;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.comment-item {
  display: flex;
  gap: 0.75rem;
}

.comment-content {
  flex: 1;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}

.comment-user {
  font-weight: 500;
  color: #1e293b;
}

.comment-time {
  font-size: 0.75rem;
  color: #64748b;
}

.comment-text {
  color: #475569;
  line-height: 1.5;
}

/* Responsive Design */
@media (max-width: 768px) {
  .hero-title {
    font-size: 2rem;
  }

  .post-input-area {
    flex-direction: column;
  }

  .upload-area {
    flex-wrap: wrap;
  }

  .moment-actions {
    justify-content: space-around;
  }

  .moment-images.grid-3 {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>