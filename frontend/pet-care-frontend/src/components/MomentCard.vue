<template>
  <el-card class="moment-card" shadow="hover">
    <div class="moment-header">
      <el-avatar :size="40" :src="moment.pet_avatar">
        {{ moment.pet_name.charAt(0) }}
      </el-avatar>
      <div class="moment-pet-info">
        <div class="pet-name">{{ moment.pet_name }}</div>
        <div class="moment-time">{{ formatTime(moment.created_at) }}</div>
      </div>
      <el-button text class="moment-more" v-if="isOwn">
        <el-icon><More /></el-icon>
      </el-button>
    </div>
    
    <div class="moment-content">
      <p class="moment-text">{{ moment.content }}</p>
      
      <!-- 媒体内容 -->
      <div v-if="moment.media_urls && moment.media_urls.length" class="moment-media">
        <div class="media-grid" :class="`media-${getMediaCount(moment.media_urls.length)}`">
          <el-image
            v-for="(url, index) in moment.media_urls"
            :key="index"
            :src="url"
            :preview-src-list="moment.media_urls"
            fit="cover"
            class="media-image"
            :preview-teleported="true"
          >
            <template #error>
              <div class="media-error">
                <el-icon><Picture /></el-icon>
                <span>图片加载失败</span>
              </div>
            </template>
          </el-image>
        </div>
      </div>
    </div>
    
    <div class="moment-actions">
      <el-button text class="action-btn" @click="$emit('like', moment.id)">
        <el-icon :color="moment.liked ? '#ff4757' : '#666'"><Star /></el-icon>
        <span class="action-text">点赞</span>
        <span class="action-count">({{ moment.like_count || 0 }})</span>
      </el-button>
      <el-button text class="action-btn" @click="$emit('comment', moment.id)">
        <el-icon><ChatDotRound /></el-icon>
        <span class="action-text">评论</span>
        <span class="action-count">({{ moment.comment_count || 0 }})</span>
      </el-button>
      <el-button text class="action-btn" @click="$emit('share', moment.id)">
        <el-icon><Share /></el-icon>
        <span class="action-text">分享</span>
      </el-button>
    </div>
  </el-card>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import {
  Picture,
  ChatDotRound,
  Star,
  Share,
  More
} from '@element-plus/icons-vue'

defineProps({
  moment: {
    type: Object,
    required: true
  },
  isOwn: {
    type: Boolean,
    default: false
  }
})

defineEmits(['like', 'comment', 'share'])

const formatTime = (timeString) => {
  if (!timeString) return '刚刚'
  
  try {
    const date = new Date(timeString)
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
      return date.toLocaleDateString('zh-CN')
    }
  } catch (e) {
    return '未知时间'
  }
}

const getMediaCount = (count) => {
  return Math.min(count, 4)
}
</script>

<style scoped>
.moment-card {
  border-radius: 12px;
  transition: all 0.2s ease;
}

.moment-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.moment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.moment-pet-info {
  flex: 1;
}

.pet-name {
  font-weight: 600;
  font-size: 0.95rem;
  color: #2c3e50;
  margin-bottom: 2px;
}

.moment-time {
  font-size: 0.8rem;
  color: #7f8c8d;
}

.moment-more {
  margin-left: auto;
  padding: 4px;
}

.moment-content {
  margin-bottom: 12px;
}

.moment-text {
  margin: 0 0 12px 0;
  line-height: 1.5;
  font-size: 0.9rem;
  color: #2c3e50;
}

.moment-media {
  margin-top: 8px;
}

.media-grid {
  display: grid;
  gap: 6px;
}

.media-1 {
  grid-template-columns: 1fr;
  max-width: 300px;
}

.media-2 {
  grid-template-columns: 1fr 1fr;
}

.media-3, .media-4 {
  grid-template-columns: 1fr 1fr;
}

.media-image {
  width: 100%;
  border-radius: 6px;
  cursor: pointer;
  transition: transform 0.2s ease;
  aspect-ratio: 1;
  object-fit: cover;
}

.media-image:hover {
  transform: scale(1.02);
}

.media-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 120px;
  background: #f5f5f5;
  border-radius: 6px;
  color: #999;
  gap: 6px;
  font-size: 0.8rem;
}

.moment-actions {
  display: flex;
  gap: 20px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 12px;
  transition: all 0.2s ease;
  font-size: 0.8rem;
}

.action-btn:hover {
  background: #f5f5f5;
}

.action-text {
  font-size: 0.8rem;
}

.action-count {
  font-size: 0.75rem;
  color: #7f8c8d;
}
</style>