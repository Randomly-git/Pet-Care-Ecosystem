<!-- 
  文件位置: src/components/home/FeatureCards.vue
  功能导航卡片 - 我的空间、看兽医、去逛街
-->

<template>
  <section class="feature-cards-section">
    <div class="container">
      <div class="cards-grid">
        <BaseCard
          v-for="(feature, index) in features"
          :key="feature.id"
          hoverable
          padding="lg"
          shadow="md"
          class="feature-card"
          :style="{ animationDelay: `${index * 0.1}s` }"
          @click="handleCardClick(feature.path)"
        >
          <div class="card-icon">{{ feature.icon }}</div>
          <h3 class="card-title">{{ feature.title }}</h3>
          <p class="card-description">{{ feature.description }}</p>
          <div class="card-arrow">→</div>
        </BaseCard>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseAvatar from '@/components/base/BaseAvatar.vue'

const router = useRouter()

const features = ref([
  {
    id: 1,
    icon: '🏠',
    title: '我的空间',
    description: '记录爱宠成长瞬间，分享快乐时光',
    path: '/space',
    gradient: 'var(--gradient-primary)'
  },
  {
    id: 2,
    icon: '🏥',
    title: '看兽医',
    description: '在线问诊，专业医生24小时守护',
    path: '/medical',
    gradient: 'var(--gradient-warm)'
  },
  {
    id: 3,
    icon: '🛍️',
    title: '去逛街',
    description: '精选好物，一站式购齐宠物用品',
    path: '/shop',
    gradient: 'var(--gradient-cool)'
  }
])

const handleCardClick = (path) => {
  router.push(path)
}
</script>

<style scoped>
.feature-cards-section {
  padding: var(--spacing-16) 0;
  background: var(--color-gray-50);
}

.cards-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-8);
}

.feature-card {
  position: relative;
  cursor: pointer;
  text-align: center;
  transition: all var(--duration-base) var(--ease-out);
  animation: fadeInUp 0.6s ease-out backwards;
  border: 2px solid transparent;
}

.feature-card:hover {
  border-color: var(--color-primary);
  transform: translateY(-8px) scale(1.02);
}

.card-icon {
  font-size: 4rem;
  margin-bottom: var(--spacing-6);
  animation: bounce 2s infinite;
}

.feature-card:hover .card-icon {
  animation: bounce 0.5s infinite;
}

.card-title {
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--color-gray-900);
  margin-bottom: var(--spacing-3);
}

.card-description {
  font-size: var(--text-base);
  color: var(--color-gray-600);
  margin-bottom: var(--spacing-6);
  line-height: 1.6;
}

.card-arrow {
  position: absolute;
  bottom: var(--spacing-6);
  right: var(--spacing-6);
  font-size: var(--text-2xl);
  color: var(--color-primary);
  opacity: 0;
  transform: translateX(-10px);
  transition: all var(--duration-base) var(--ease-out);
}

.feature-card:hover .card-arrow {
  opacity: 1;
  transform: translateX(0);
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

@keyframes bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

/* 响应式 */
@media (max-width: 1024px) {
  .cards-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .cards-grid {
    grid-template-columns: 1fr;
    gap: var(--spacing-6);
  }

  .card-icon {
    font-size: 3rem;
  }
}
</style>