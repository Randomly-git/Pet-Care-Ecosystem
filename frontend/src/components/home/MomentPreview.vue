<!-- 
  文件位置: src/components/home/MomentPreview.vue
  热门动态预览 - 网格布局
-->

<template>
  <section class="moment-preview-section">
    <div class="container">
      <!-- 标题区 -->
      <div class="section-header">
        <h2 class="section-title">热门动态 🔥</h2>
        <p class="section-subtitle">看看其他宠物主人在分享什么</p>
        <BaseButton variant="text">
          查看更多 →
        </BaseButton>
      </div>

      <!-- 动态网格 -->
      <div class="moments-grid">
        <BaseCard
          v-for="(moment, index) in moments"
          :key="moment.id"
          hoverable
          padding="md"
          shadow="sm"
          class="moment-card"
          :style="{ animationDelay: `${index * 0.1}s` }"
        >
          <div class="moment-image">
            <div class="image-placeholder">{{ moment.emoji }}</div>
          </div>
          
          <div class="moment-content">
            <div class="moment-author">
              <BaseAvatar 
                :placeholder="moment.author.name.charAt(0)"
                size="sm"
              />
              <span class="author-name">{{ moment.author.name }}</span>
            </div>
            
            <p class="moment-text">{{ moment.text }}</p>
            
            <div class="moment-meta">
              <span class="meta-item">
                ❤️ {{ moment.likes }}
              </span>
              <span class="meta-item">
                💬 {{ moment.comments }}
              </span>
              <span class="meta-item">
                🕒 {{ moment.time }}
              </span>
            </div>
          </div>
        </BaseCard>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseAvatar from '@/components/base/BaseAvatar.vue'

const moments = ref([
  {
    id: 1,
    emoji: '🐱',
    author: { name: '喵星人铲屎官' },
    text: '今天带主子去体检，医生说身体很健康！笑猫の窝的在线问诊真的超方便~',
    likes: 128,
    comments: 23,
    time: '2小时前'
  },
  {
    id: 2,
    emoji: '🐶',
    author: { name: '柴犬爸爸' },
    text: '在宠物商城买的狗粮收到了，质量超好！还有优惠券真香',
    likes: 256,
    comments: 45,
    time: '5小时前'
  },
  {
    id: 3,
    emoji: '🐰',
    author: { name: '兔兔守护者' },
    text: '用疫苗提醒功能再也不怕忘记啦，贴心！',
    likes: 89,
    comments: 12,
    time: '8小时前'
  },
  {
    id: 4,
    emoji: '🐹',
    author: { name: '仓鼠小屋' },
    text: '社区里认识了好多养仓鼠的朋友，交流经验超开心',
    likes: 167,
    comments: 34,
    time: '1天前'
  },
  {
    id: 5,
    emoji: '🐦',
    author: { name: '鹦鹉妈妈' },
    text: 'AI助手识别了我家鸟的品种，还给了专业的饲养建议',
    likes: 234,
    comments: 56,
    time: '1天前'
  },
  {
    id: 6,
    emoji: '🐢',
    author: { name: '乌龟爱好者' },
    text: '宠物档案功能太好用了，所有信息一目了然',
    likes: 145,
    comments: 28,
    time: '2天前'
  }
])
</script>

<style scoped>
.moment-preview-section {
  padding: var(--spacing-20) 0;
  background: var(--color-gray-50);
}

/* 标题区 */
.section-header {
  text-align: center;
  margin-bottom: var(--spacing-12);
  animation: fadeInUp 0.6s ease-out;
}

.section-title {
  font-size: var(--text-4xl);
  font-weight: var(--font-bold);
  color: var(--color-gray-900);
  margin-bottom: var(--spacing-3);
}

.section-subtitle {
  font-size: var(--text-lg);
  color: var(--color-gray-600);
  margin-bottom: var(--spacing-6);
}

/* 动态网格 */
.moments-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-6);
}

.moment-card {
  animation: fadeInUp 0.6s ease-out backwards;
  transition: all var(--duration-base) var(--ease-out);
}

.moment-image {
  margin-bottom: var(--spacing-4);
  overflow: hidden;
  border-radius: var(--radius-lg);
}

.image-placeholder {
  aspect-ratio: 16 / 9;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 4rem;
  transition: transform var(--duration-slow) var(--ease-out);
}

.moment-card:hover .image-placeholder {
  transform: scale(1.1);
}

.moment-content {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3);
}

.moment-author {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
}

.author-name {
  font-weight: var(--font-medium);
  color: var(--color-gray-900);
  font-size: var(--text-sm);
}

.moment-text {
  font-size: var(--text-base);
  color: var(--color-gray-700);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.moment-meta {
  display: flex;
  gap: var(--spacing-4);
  padding-top: var(--spacing-2);
  border-top: 1px solid var(--color-gray-200);
}

.meta-item {
  font-size: var(--text-sm);
  color: var(--color-gray-500);
}

/* 动画 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式 */
@media (max-width: 1024px) {
  .moments-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .section-title {
    font-size: var(--text-2xl);
  }

  .moments-grid {
    grid-template-columns: 1fr;
  }
}
</style>