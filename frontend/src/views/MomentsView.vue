<template>
  <div class="moments-page">
    <AppHeader />

    <!-- 居中信息流区域与两侧边栏 -->
    <div class="main-content">
      <!-- 左侧栏 (User Mini Profile) -->
      <div class="left-sidebar">
        <div class="profile-card">
          <div class="ins-gradient-border">
            <el-avatar :size="72" :src="userAvatar" class="profile-avatar">{{ userName.charAt(0) }}</el-avatar>
          </div>
          <div class="profile-name">{{ userName }}</div>
          <div class="profile-id">@user_{{ authStore.userId || 'guest' }}</div>
          <div class="profile-stats">
            <div class="stat-item">
              <span class="stat-val">{{ moments.filter(m => m.isOwn).length }}</span>
              <span class="stat-label">动态</span>
            </div>
          </div>
        </div>
      </div>

      <div class="feed-container">
        <!-- 布局切换控制器 -->
        <div class="grid-controls">
          <div class="grid-title">展示排版</div>
          <div class="grid-btn-group">
            <button :class="{ active: columnCount === 1 }" @click="columnCount = 1">
              <el-icon><Menu /></el-icon> 单排
            </button>
            <button :class="{ active: columnCount === 2 }" @click="columnCount = 2">
              <el-icon><Grid /></el-icon> 双排
            </button>
            <button :class="{ active: columnCount === 3 }" @click="columnCount = 3">
              <el-icon><Grid style="transform: scale(1.2)" /></el-icon> 三排
            </button>
          </div>
        </div>

        <!-- 动态列表 -->
        <div class="moments-list" :style="{ '--col-count': columnCount }">
          <div v-if="loading" class="loading-container">
            <el-skeleton :rows="5" animated />
          </div>

          <div v-else-if="moments.length === 0" class="empty-state">
            <div class="empty-icon">📸</div>
            <p>还没有动态，快来发布第一条吧！</p>
          </div>

          <div v-else class="moment-cards">
            <div
              v-for="moment in moments"
              :key="moment.id"
              class="moment-card"
              :class="{ 'expanded-post': moment.isExpanded }"
            >
              <!-- 帖子头部 -->
              <div class="moment-header">
                <div class="header-left">
                  <el-avatar :size="32" :src="moment.userAvatar" class="ins-avatar">
                    {{ moment.userName.charAt(0) }}
                  </el-avatar>
                  <span class="user-name">{{ moment.userName }}</span>
                  <span class="dot-separator">•</span>
                  <span class="post-time">{{ formatTime(moment.created_at) }}</span>
                </div>
                
                <div class="header-actions">
                  <!-- 缩放按钮 -->
                  <el-button link class="zoom-btn" @click="moment.isExpanded = !moment.isExpanded" title="放大/缩小">
                    <el-icon :size="18"><FullScreen v-if="!moment.isExpanded"/><Aim v-else/></el-icon>
                  </el-button>

                  <el-dropdown trigger="click" v-if="moment.isOwn">
                  <el-icon class="more-icon"><MoreFilled /></el-icon>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item @click="deleteMoment(moment.id)" class="danger-text">
                        删除
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>

              <!-- 无边框满宽图像区 (仅有媒体时才渲染) -->
              <div class="moment-media" v-if="moment.media_items && moment.media_items.length > 0">
                <template v-if="moment.media_items.length > 0">
                  <!-- 单图 -->
                  <div v-if="moment.media_items.length === 1" class="media-single">
                    <video
                      v-if="moment.media_items[0].type && moment.media_items[0].type.startsWith('video')"
                      :src="moment.media_items[0].url"
                      controls
                      class="ins-media"
                    />
                    <img
                      v-else
                      :src="moment.media_items[0].url"
                      class="ins-media"
                      @click="previewImage(moment.media_items[0].url)"
                      @error="handleMediaError"
                    />
                  </div>
                  <!-- 多图轮播 -->
                  <el-carousel v-else trigger="click" height="auto" :autoplay="false" class="ins-carousel" arrow="always">
                    <el-carousel-item v-for="(media, idx) in moment.media_items" :key="idx">
                      <video
                        v-if="media.type && media.type.startsWith('video')"
                        :src="media.url"
                        controls
                        class="ins-media"
                      />
                      <img
                        v-else
                        :src="media.url"
                        class="ins-media"
                        @click="previewImage(media.url)"
                        @error="handleMediaError"
                      />
                    </el-carousel-item>
                  </el-carousel>
                </template>
              </div>

              <!-- 底部交互与内容区 -->
              <div class="moment-footer">
                <!-- 交互图标栏 -->
                <div class="action-bar">
                  <div class="action-left">
                    <button class="icon-btn" :class="{ 'liked': moment.liked }" @click="toggleLike(moment)">
                      <!-- 自定义心形 SVG -->
                      <svg v-if="!moment.liked" aria-label="赞" class="ins-svg-icon" fill="currentColor" height="24" role="img" viewBox="0 0 24 24" width="24"><path d="M16.792 3.904A4.989 4.989 0 0 1 21.5 9.122c0 3.072-2.652 4.959-5.197 7.222-2.512 2.243-3.865 3.469-4.303 3.752-.477-.309-2.143-1.823-4.303-3.752C5.141 14.072 2.5 12.167 2.5 9.122a4.989 4.989 0 0 1 4.708-5.218 4.21 4.21 0 0 1 3.675 1.941c.84 1.175.98 1.763 1.12 1.763s.278-.588 1.11-1.766a4.17 4.17 0 0 1 3.679-1.938m0-2a6.155 6.155 0 0 0-4.89 2.368 6.162 6.162 0 0 0-4.892-2.368C3.8 1.904.5 5.214.5 9.122c0 4.177 3.454 6.307 6.183 8.784C9.435 20.395 11.23 22.185 11.662 22.56a.5.5 0 0 0 .676 0c.433-.375 2.227-2.165 4.981-4.654 2.73-2.477 6.183-4.607 6.183-8.784 0-3.908-3.3-7.218-6.71-7.218Z"></path></svg>
                      <svg v-else aria-label="取消赞" class="ins-svg-icon liked-icon" fill="#ff3040" height="24" role="img" viewBox="0 0 48 48" width="24"><path d="M34.6 3.1c-4.5 0-7.9 1.8-10.6 5.6-2.7-3.7-6.1-5.5-10.6-5.5C6 3.1 0 9.6 0 17.6c0 7.3 5.4 12 10.6 16.5.6.5 1.3 1.1 1.9 1.7l2.3 2c4.4 3.9 6.6 5.9 7.6 6.5.5.3 1.1.5 1.6.5s1.1-.2 1.6-.5c1-.6 2.8-2.2 7.8-6.8l2-1.8c.7-.6 1.3-1.2 2-1.7C42.7 29.6 48 25 48 17.6c0-8-6-14.5-13.4-14.5z"></path></svg>
                    </button>
                    <button class="icon-btn" @click="showComments(moment)">
                      <!-- 自定义评论气泡 SVG -->
                      <svg aria-label="评论" class="ins-svg-icon" fill="currentColor" height="24" role="img" viewBox="0 0 24 24" width="24"><path d="M20.656 17.008a9.993 9.993 0 1 0-3.59 3.615L22 22Z" fill="none" stroke="currentColor" stroke-linejoin="round" stroke-width="2"></path></svg>
                    </button>
                    <button class="icon-btn" @click="shareMoment(moment)">
                      <!-- 自定义分享 SVG -->
                      <svg aria-label="分享" class="ins-svg-icon" fill="currentColor" height="24" role="img" viewBox="0 0 24 24" width="24"><line fill="none" stroke="currentColor" stroke-linejoin="round" stroke-width="2" x1="22" x2="9.218" y1="3" y2="10.083"></line><polygon fill="none" points="11.698 20.334 22 3.001 2 3.001 9.218 10.084 11.698 20.334" stroke="currentColor" stroke-linejoin="round" stroke-width="2"></polygon></svg>
                    </button>
                  </div>
                  <!-- 收藏占位符 -->
                  <div class="action-right">
                    <button class="icon-btn">
                      <svg aria-label="收藏" class="ins-svg-icon" fill="currentColor" height="24" role="img" viewBox="0 0 24 24" width="24"><polygon fill="none" points="20 21 12 13.44 4 21 4 3 20 3 20 21" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2"></polygon></svg>
                    </button>
                  </div>
                </div>

                <!-- 点赞数 -->
                <div class="likes-count" v-if="moment.like_count > 0">
                  {{ moment.like_count }} 次赞
                </div>

                <!-- 正文 -->
                <div class="caption-container">
                  <span class="user-name">{{ moment.userName }}</span>
                  <span class="caption-text"> {{ moment.content }}</span>
                </div>

                <!-- 查看评论 -->
                <div class="view-comments" v-if="moment.comment_count > 0" @click="showComments(moment)">
                  查看全部 {{ moment.comment_count }} 条评论
                </div>

                <!-- 展开的评论区 -->
                <div v-if="moment.showComments" class="comments-section">
                  <div v-if="moment.comments && moment.comments.length" class="comments-list">
                    <div v-for="comment in moment.comments" :key="comment.id" class="comment-item">
                      <span class="comment-user">{{ comment.userName }}</span>
                      <span class="comment-text">{{ comment.content }}</span>
                    </div>
                  </div>
                  <!-- 评论输入框 -->
                  <div class="comment-input-wrap">
                    <input 
                      type="text" 
                      v-model="moment.commentText" 
                      class="ins-comment-input" 
                      placeholder="添加评论..." 
                      @keyup.enter="submitComment(moment)"
                    />
                    <button class="post-comment-btn" :disabled="!moment.commentText" @click="submitComment(moment)">发布</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 悬浮发帖按钮 FAB (INS风格渐变色) -->
    <button class="fab-post-btn" @click="postDialogVisible = true">
      <el-icon><Plus /></el-icon>
    </button>

    <!-- Instagram 风格发帖弹窗 -->
    <el-dialog v-model="postDialogVisible" title="创建新帖子" width="500px" center custom-class="ins-post-modal" :show-close="false">
      <div class="modal-header">
        <button class="modal-cancel" @click="postDialogVisible = false">取消</button>
        <span class="modal-title">创建新帖子</span>
        <button class="modal-share" :disabled="!newPostContent.trim() || publishing" @click="publishPost">分享</button>
      </div>
      
      <div class="post-input-area">
        <div class="user-row">
          <el-avatar :size="32" :src="userAvatar">{{ userName.charAt(0) }}</el-avatar>
          <span class="post-user-name">{{ userName }}</span>
        </div>
        
        <textarea
          class="ins-textarea"
          v-model="newPostContent"
          placeholder="分享宠物日常..."
          maxlength="2200"
        ></textarea>
        
        <!-- 上传区域 (增强的明显拖拽区域) -->
        <div class="upload-trigger">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :show-file-list="false"
            accept="image/*,video/*"
            multiple
            drag
            class="ins-drag-upload"
            @change="handleImageSelect"
          >
            <div class="upload-placeholder">
              <el-icon class="upload-icon-large"><Picture /></el-icon>
              <div class="upload-text">点击或将照片/视频拖拽至此处</div>
            </div>
          </el-upload>
        </div>

        <!-- 图片预览 -->
        <div v-if="uploadedImages.length" class="modal-image-preview">
          <div v-for="(img, index) in uploadedImages" :key="index" class="preview-item">
            <img :src="img.url" />
            <button class="remove-btn" @click="removeImage(index)"><el-icon><Close /></el-icon></button>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 图片预览全屏 -->
    <el-dialog v-model="previewVisible" custom-class="ins-fullscreen-preview" fullscreen>
      <div class="fullscreen-close" @click="previewVisible = false"><el-icon><Close /></el-icon></div>
      <img :src="previewImageUrl" class="fullscreen-img" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { Picture, Close, Star, ChatDotRound, MoreFilled, Plus, Menu, Grid, FullScreen, Aim } from '@element-plus/icons-vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import { createMoment, deleteMoment as deleteMomentApi, toggleLike as toggleLikeApi, createComment, getMomentComments, getAllMoments } from '@/api/community'
import { uploadMultipleMedia, getRelatedMedia } from '@/api/media'
import { getUserPets } from '@/api/pets'

const authStore = useAuthStore()

// 响应式数据
const loading = ref(false)
const publishing = ref(false)
const showPostOptions = ref(false)
const postDialogVisible = ref(false) // 新增发帖弹窗控制
const previewVisible = ref(false)
const previewImageUrl = ref('')
const newPostContent = ref('')
const uploadedImages = ref([])
const moments = ref([])

// 网格列数控制器
const columnCount = ref(1) // 默认1排

// 用户信息
const userName = computed(() => authStore.userName || '当前用户')
const userAvatar = computed(() => authStore.avatar || '')

// 处理图片加载失败的回调
const handleMediaError = (e) => {
  if (!e.target.dataset.fallbackApplied) {
    e.target.dataset.fallbackApplied = "true";
    e.target.style.display = 'none'; // 彻底不使用模拟图片，真实失败就直接隐藏
  }
}

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

// 判断URL是否为视频文件
const isVideoUrl = (url) => {
  if (!url) return false
  const videoExtensions = ['.mp4', '.avi', '.mov', '.wmv', '.flv', '.webm', '.mkv']
  const lowerUrl = url.toLowerCase()
  return videoExtensions.some(ext => lowerUrl.includes(ext))
}

// 在新标签页打开媒体文件
const openInNewTab = (url) => {
  window.open(url, '_blank')
}

const publishPost = async () => {
  if (!newPostContent.value.trim()) {
    ElMessage.warning('请输入动态内容')
    return
  }

  publishing.value = true

  try {
    // 1. 先创建动态（不包含媒体）
    const momentData = {
      userId: authStore.userId,
      content: newPostContent.value.trim(),
      mediaIds: [] // 先创建空的动态
    }

    const newMoment = await createMoment(momentData)
    console.log('动态创建成功:', newMoment)

    // 2. 上传媒体文件并关联到新创建的动态
    let mediaIds = []
    let mediaUrls = []
    let uploadResults = null
    if (uploadedImages.value.length > 0) {
      ElMessage.info('正在上传媒体文件...')
      try {
        const files = uploadedImages.value.map(img => img.file)
        uploadResults = await uploadMultipleMedia({
          files: files,
          relatedType: 'MOMENT',
          relatedId: newMoment.id, // 使用真实的动态ID
          userId: authStore.userId,
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
        // 保存媒体URL用于前端显示
        mediaUrls = uploadResults.map(result => result.data.fileUrl)
        ElMessage.success(`成功上传 ${mediaIds.length} 个文件`)
      } catch (uploadError) {
        console.error('上传媒体文件失败:', uploadError)
        ElMessage.warning('部分媒体文件上传失败，将只发布已上传的内容')
        // 保留已成功的文件
        mediaIds = uploadResults?.map(result => result?.mediaId).filter(id => id) || []
        mediaUrls = uploadResults?.map(result => result?.data?.fileUrl).filter(url => url) || []
      }
    }

    // 4. 转换为前端显示格式
    const displayMoment = {
      id: newMoment.id,
      userId: newMoment.userId,
      userName: userName.value,
      userAvatar: userAvatar.value,
      content: newMoment.content,
      // 记录带有 type 的新结构
      media_items: mediaUrls.map((url, idx) => ({ url: url, type: uploadResults[idx]?.data?.fileType || 'image/jpeg' })),
      created_at: newMoment.createdAt,
      like_count: newMoment.likeCount || 0,
      comment_count: newMoment.commentCount || 0,
      liked: false,
      isOwn: true,
      showComments: false,
      commentText: '',
      comments: [],
      isExpanded: false
    }

    // 添加到动态列表
    moments.value.unshift(displayMoment)

    ElMessage.success('发布成功！')
    newPostContent.value = ''
    uploadedImages.value = []
    showPostOptions.value = false
    postDialogVisible.value = false // 发帖成功后关闭弹窗

  } catch (error) {
    console.error('发布动态失败:', error)
    ElMessage.error('发布失败: ' + error.message)
  } finally {
    publishing.value = false
  }
}

const toggleLike = async (moment) => {
  if (moment.likeLoading) return;
  moment.likeLoading = true;

  try {
    // 本地缓存检查是否点赞（防死刷）
    const likedKey = `liked_${authStore.userId}_${moment.id}`;
    const alreadyLiked = localStorage.getItem(likedKey) === 'true';
    
    if (alreadyLiked && !moment.liked) {
      ElMessage.warning('您已经赞过这条动态啦~');
      moment.liked = true;
      return;
    }

    if (!alreadyLiked) {
      await toggleLikeApi({
        userId: authStore.userId,
        targetType: 'MOMENT',
        targetId: moment.id
      })
      moment.liked = true;
      moment.like_count++;
      localStorage.setItem(likedKey, 'true');
    } else {
      // 允许取消点赞
      await toggleLikeApi({
        userId: authStore.userId,
        targetType: 'MOMENT',
        targetId: moment.id
      })
      moment.liked = false;
      moment.like_count = Math.max(0, moment.like_count - 1);
      localStorage.removeItem(likedKey);
    }
  } catch (error) {
    console.error('点赞失败:', error)
    ElMessage.error('操作失败')
  } finally {
    moment.likeLoading = false;
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
        userName: comment.userId === authStore.userId ? userName.value : `用户${comment.userId}`, 
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

const shareMoment = (moment) => {
  ElMessage.success('链接已复制到剪贴板！')
}

const deleteMoment = async (momentId) => {
  try {
    await ElMessageBox.confirm('确定要删除这条动态吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    // 传递 userId 进行权限验证和冷库清理
    await deleteMomentApi(momentId, authStore.userId)

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
  console.log('loadMoments: 开始加载动态，authStore.userId =', authStore.userId)

  if (!authStore.userId) {
    console.log('loadMoments: 用户未登录，临时设置为76')
    // 临时硬编码用户ID
    authStore.updateUser({ id: 76 })
  }

  loading.value = true
  try {
    // 加载所有用户的动态列表
    console.log('loadMoments: 调用getAllMoments')
    const userMoments = await getAllMoments()
    console.log('loadMoments: 获取到的所有用户动态:', userMoments)

    // 获取每个动态的媒体文件（类似ActivitiesView的处理方式）
    const momentsWithMedia = await Promise.all(
      userMoments.map(async (moment) => {
        try {
          const mediaResponse = await getRelatedMedia('MOMENT', moment.id)
          const mediaFiles = (mediaResponse && mediaResponse.data) ? mediaResponse.data : []
          console.log(`动态 ${moment.id} 的媒体文件:`, mediaFiles)

          return {
            id: moment.id,
            userId: moment.userId,
            content: moment.content,
            media_items: mediaFiles.map(m => ({ url: m.fileUrl, type: m.fileType })), // 改用对象数组记录MIME
            mediaFiles: mediaFiles, // 保存完整的媒体文件信息
            created_at: moment.createdAt,
            like_count: moment.likeCount || 0,
            comment_count: moment.commentCount || 0,
            liked: localStorage.getItem(`liked_${authStore.userId}_${moment.id}`) === 'true',
            likeLoading: false,
            isOwn: moment.userId === authStore.userId,
            userName: moment.userId === authStore.userId ? userName.value : `社区用户_${moment.userId.toString().slice(-4)}`,
            userAvatar: moment.userId === authStore.userId ? userAvatar.value : '',
            isExpanded: false
          }
        } catch (mediaError) {
          console.error(`获取动态 ${moment.id} 的媒体文件失败:`, mediaError)
          return {
            id: moment.id,
            userId: moment.userId,
            content: moment.content,
            media_items: [],
            mediaFiles: [],
            created_at: moment.createdAt,
            like_count: moment.likeCount || 0,
            comment_count: moment.commentCount || 0,
            liked: localStorage.getItem(`liked_${authStore.userId}_${moment.id}`) === 'true',
            likeLoading: false,
            isOwn: moment.userId === authStore.userId,
            userName: moment.userId === authStore.userId ? userName.value : `社区用户_${moment.userId.toString().slice(-4)}`,
            userAvatar: moment.userId === authStore.userId ? userAvatar.value : '',
            isExpanded: false
          }
        }
      })
    )

    moments.value = momentsWithMedia
    console.log('loadMoments: 格式化后的moments数组长度:', moments.value.length)
  } catch (error) {
    console.error('加载动态失败:', error)
    moments.value = []
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
  /* 背景加一点淡淡的粉紫渐变色配合 INS 风格 */
  background: linear-gradient(135deg, #fdfbfb 0%, #fcebeb 50%, #f6e8fa 100%);
}

/* 居中信息流与侧边栏容器 */
.main-content {
  padding: 40px 20px;
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  gap: 32px;
  max-width: 1400px;
  margin: 0 auto;
}

/* 左侧栏 */
.left-sidebar {
  flex: 0 0 280px;
  position: sticky;
  top: 100px;
}

.ins-gradient-border {
  padding: 4px;
  background: linear-gradient(45deg, #f09433 0%, #e6683c 25%, #dc2743 50%, #cc2366 75%, #bc1888 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}
.ins-gradient-border .el-avatar {
  border: 3px solid white;
}

.profile-card {
  background: white;
  border-radius: 12px;
  border: 1px solid #dbdbdb;
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.profile-name {
  font-size: 18px;
  font-weight: 600;
  margin-top: 12px;
  color: #262626;
}

.profile-id {
  font-size: 14px;
  color: #8e8e93;
  margin-top: 4px;
  margin-bottom: 20px;
}

.profile-stats {
  border-top: 1px solid #efefef;
  width: 100%;
  padding-top: 16px;
  display: flex;
  justify-content: center;
}

.stat-item {
  display: flex;
  flex-direction: column;
}
.stat-val {
  font-size: 18px;
  font-weight: 600;
  color: #262626;
}
.stat-label {
  font-size: 12px;
  color: #8e8e93;
  margin-top: 2px;
}

/* 核心流 (响应式多列布局) */
.feed-container {
  flex: 1;
  width: 100%;
}

.grid-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: white;
  border-radius: 12px;
  border: 1px solid #dbdbdb;
  margin-bottom: 24px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}
.grid-title {
  font-weight: 600;
  font-size: 15px;
  color: #262626;
}
.grid-btn-group {
  display: flex;
  background: #f0f0f0;
  border-radius: 8px;
  padding: 4px;
}
.grid-btn-group button {
  border: none;
  background: transparent;
  padding: 6px 16px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #8e8e93;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}
.grid-btn-group button.active {
  background: white;
  color: #1d1d1f;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  /* 活动状态带点 INS 风格粉紫点缀 */
  color: #cc2366; 
}

/* 隐藏无关侧边栏的遗留CSS */
@media (max-width: 900px) {
  .left-sidebar { display: none; }
}

/* 骨架与空状态 */
.loading-container {
  padding: 40px 0;
}
.empty-state {
  text-align: center;
  padding: 100px 20px;
  color: #8e8e93;
}
.empty-icon {
  font-size: 48px;
  margin-bottom: 20px;
}

/* 帖子卡片瀑布流/多列网格 */
.moments-list {
  width: 100%;
}

.moment-cards {
  display: grid;
  grid-template-columns: repeat(var(--col-count, 1), 1fr);
  gap: 24px;
  align-items: start;
}

.moment-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #dbdbdb;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
  transition: transform 0.2s, box-shadow 0.2s;
}

.moment-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 149, 246, 0.1); 
  border-color: rgba(204, 35, 102, 0.3); /* INS 粉紫边框反馈 */
}

/* 帖子独立展开放大样式 */
.moment-card.expanded-post {
  grid-column: span 2;
  transform: scale(1.02);
  z-index: 10;
  box-shadow: 0 30px 60px rgba(0, 0, 0, 0.15);
  border: 2px solid #bc1888; /* 深紫色突出 */
}

/* 单排布局时，限制帖子的最大宽度，使其不要全屏拉伸显得过大 */
.moments-list[style*="--col-count: 1"] .moment-cards {
  display: flex !important;
  flex-direction: column;
  align-items: center;
}
.moments-list[style*="--col-count: 1"] .moment-card {
  width: 100%;
  max-width: 470px; /* Instagram feed 的标准核心流宽度 */
}
/* 如果只是一排布局，放大比例时加宽一点 */
.moments-list[style*="--col-count: 1"] .moment-card.expanded-post {
  max-width: 600px;
}

/* 头部 */
.moment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.ins-avatar {
  border: 1px solid #dbdbdb;
}
.user-name {
  font-weight: 600;
  font-size: 14px;
  color: #262626;
}
.dot-separator {
  color: #8e8e93;
  margin: 0 8px;
}
.post-time {
  color: #8e8e93;
  font-size: 14px;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.zoom-btn {
  color: #8e8e93;
  transition: color 0.2s;
}
.zoom-btn:hover {
  color: #cc2366; /* INS 风格反馈 */
}
.more-icon {
  cursor: pointer;
  color: #262626;
  font-size: 20px;
}
.danger-text {
  color: #ed4956;
  font-weight: 600;
}

/* 媒体区 - 仅有真实媒体时才会渲染 */
.moment-media {
  width: 100%;
  aspect-ratio: 4 / 5;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #111; /* 不再是纯黑观，但也不展示空白 */
}
.ins-media {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.media-single {
  width: 100%;
  height: 100%;
}

::v-deep(.ins-carousel) {
  width: 100%;
  height: 100%;
}
::v-deep(.ins-carousel .el-carousel__container) {
  height: 100% !important;
}

/* 底部交互区 */
.moment-footer {
  padding: 0 16px 16px;
}

.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
  padding-bottom: 6px;
}
.action-left {
  display: flex;
  gap: 16px;
  margin-left: -8px;
}
.icon-btn {
  background: transparent;
  border: none;
  padding: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #262626;
  transition: opacity 0.2s;
}
.icon-btn:hover {
  opacity: 0.5;
}
.icon-btn:active {
  transform: scale(0.9);
}
.liked-icon {
  color: #ff3040 !important;
}

.likes-count {
  font-weight: 600;
  font-size: 14px;
  color: #262626;
  margin-bottom: 8px;
}

.caption-container {
  font-size: 14px;
  line-height: 18px;
  word-break: break-word;
}
.caption-text {
  color: #262626;
  margin-left: 6px;
}

.view-comments {
  color: #8e8e93;
  font-size: 14px;
  margin-top: 8px;
  cursor: pointer;
}

/* 评论区 */
.comments-section {
  margin-top: 8px;
}
.comments-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 12px;
}
.comment-item {
  font-size: 14px;
  line-height: 18px;
}
.comment-user {
  font-weight: 600;
  margin-right: 6px;
}

.comment-input-wrap {
  display: flex;
  align-items: center;
  border-top: 1px solid #dbdbdb;
  padding-top: 10px;
  margin-top: 10px;
}
.ins-comment-input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 14px;
}
.post-comment-btn {
  border: none;
  background: transparent;
  color: #0095f6;
  font-weight: 600;
  cursor: pointer;
  padding: 0 8px;
}
.post-comment-btn:disabled {
  color: #82cbfb;
  cursor: default;
}

/* FAB 悬浮发帖按钮 */
.fab-post-btn {
  position: fixed;
  bottom: 40px;
  right: 40px;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background-color: #0095f6;
  color: white;
  border: none;
  box-shadow: 0 4px 12px rgba(0, 149, 246, 0.4);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  z-index: 100;
  transition: transform 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
.fab-post-btn:hover {
  transform: scale(1.1);
}

/* Ins 弹窗 */
::v-deep(.ins-post-modal) {
  border-radius: 12px;
  overflow: hidden;
}
::v-deep(.ins-post-modal .el-dialog__header) {
  display: none; /* 隐藏原生 Header，使用自定义 */
}
::v-deep(.ins-post-modal .el-dialog__body) {
  padding: 0;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #dbdbdb;
  font-weight: 600;
  font-size: 16px;
}
.modal-cancel {
  border: none;
  background: transparent;
  font-size: 16px;
  cursor: pointer;
}
.modal-title {
  color: #262626;
}
.modal-share {
  border: none;
  background: transparent;
  color: #0095f6;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
}
.modal-share:disabled {
  color: #82cbfb;
  cursor: default;
}
.post-input-area {
  padding: 16px;
}
.user-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.post-user-name {
  font-weight: 600;
}
.ins-textarea {
  width: 100%;
  height: 120px;
  border: none;
  resize: none;
  outline: none;
  font-size: 16px;
  font-family: inherit;
}
.upload-trigger {
  margin-top: 10px;
}
.modal-image-preview {
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
  overflow-x: auto;
  margin-top: 12px;
  padding-bottom: 8px;
}
.preview-item {
  position: relative;
  width: 100px;
  height: 100px;
  flex-shrink: 0;
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
  border-radius: 50%;
  background: rgba(0,0,0,0.6);
  color: white;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

/* 全屏预览 */
::v-deep(.ins-fullscreen-preview) {
  background: rgba(0,0,0,0.95);
}
.fullscreen-close {
  position: absolute;
  top: 20px;
  right: 20px;
  color: white;
  font-size: 32px;
  cursor: pointer;
  z-index: 9999;
}
.fullscreen-img {
  width: 100%;
  height: 100vh;
  object-fit: contain;
}

@media (max-width: 768px) {
  .moment-card {
    border-left: none;
    border-right: none;
    border-radius: 0;
  }
}
</style>