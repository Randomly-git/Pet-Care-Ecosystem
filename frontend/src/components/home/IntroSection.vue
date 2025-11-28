<!-- 
  文件位置: src/components/home/IntroSection.vue
  平台介绍 - 核心特性展示
-->

<template>
  <section class="intro-section">
    <div class="container">
      <!-- 标题区 -->
      <div class="section-header">
        <h2 class="section-title">为什么选择笑猫の窝？</h2>
        <p class="section-subtitle">
          科技赋能宠物健康，让养宠更简单、更科学、更有爱
        </p>
      </div>

      <!-- 核心特性 -->
      <div class="features-grid">
        <div
          v-for="(feature, index) in coreFeatures"
          :key="feature.id"
          class="feature-item"
          :style="{ animationDelay: `${index * 0.15}s` }"
        >
          <div class="feature-icon-wrapper">
            <div class="feature-icon">{{ feature.icon }}</div>
          </div>
          <h3 class="feature-title">{{ feature.title }}</h3>
          <p class="feature-description">{{ feature.description }}</p>
        </div>
      </div>

      <!-- 数据展示 -->
      <div class="data-showcase">
        <BaseCard padding="lg" shadow="lg" class="data-card">
          <div class="data-content">
            <div class="data-text">
              <h3 class="data-title">加入我们的宠物大家庭</h3>
              <p class="data-description">
                已有超过 10 万+ 宠物主人选择笑猫の窝，
                让我们一起为宠物创造更美好的生活！
              </p>
              <BaseButton variant="primary" size="lg" class="data-cta">
                立即注册
              </BaseButton>
            </div>
            <div class="data-visual">
              <div class="visual-circle">
                <span class="visual-emoji">🐾</span>
              </div>
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

const coreFeatures = ref([
  {
    id: 1,
    icon: '📱',
    title: '智能记录',
    description: '自动记录宠物成长轨迹，疫苗、体检、饮食一目了然'
  },
  {
    id: 2,
    icon: '🩺',
    title: '在线问诊',
    description: '通过宠物成长日志及时反馈宠物情况'
  },
  {
    id: 3,
    icon: '🤖',
    title: 'AI助手',
    description: 'AI助手助力宠物健康成长'
  },
  {
    id: 4,
    icon: '🌟',
    title: '社区互动',
    description: '分享养宠经验，结识同城宠友，参与线下活动'
  },
  {
    id: 5,
    icon: '🛡️',
    title: '数据安全',
    description: '数据加密保障用户的数据安全'
  },
  {
    id: 6,
    icon: '💰',
    title: '优惠福利',
    description: '没有福利'
  }
])
</script>

<style scoped>
.intro-section {
  padding: var(--spacing-20) 0;
  background: var(--color-white);
}

/* 标题区 */
.section-header {
  text-align: center;
  margin-bottom: var(--spacing-16);
  animation: fadeInUp 0.6s ease-out;
}

.section-title {
  font-size: var(--text-4xl);
  font-weight: var(--font-bold);
  color: var(--color-gray-900);
  margin-bottom: var(--spacing-4);
}

.section-subtitle {
  font-size: var(--text-lg);
  color: var(--color-gray-600);
  max-width: 700px;
  margin: 0 auto;
}

/* 特性网格 */
.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--spacing-8);
  margin-bottom: var(--spacing-16);
}

.feature-item {
  text-align: center;
  padding: var(--spacing-6);
  animation: fadeInUp 0.6s ease-out backwards;
}

.feature-icon-wrapper {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.1) 0%, rgba(236, 72, 153, 0.1) 100%);
  border-radius: var(--radius-xl);
  margin-bottom: var(--spacing-4);
  transition: all var(--duration-base) var(--ease-out);
}

.feature-item:hover .feature-icon-wrapper {
  transform: scale(1.1) rotate(5deg);
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.2) 0%, rgba(236, 72, 153, 0.2) 100%);
}

.feature-icon {
  font-size: 2.5rem;
}

.feature-title {
  font-size: var(--text-xl);
  font-weight: var(--font-semibold);
  color: var(--color-gray-900);
  margin-bottom: var(--spacing-3);
}

.feature-description {
  font-size: var(--text-base);
  color: var(--color-gray-600);
  line-height: 1.6;
}

/* 数据展示卡片 */
.data-showcase {
  animation: fadeInUp 0.6s ease-out 0.3s backwards;
}

.data-card {
  background: var(--gradient-primary);
  color: var(--color-white);
}

.data-content {
  display: flex;
  align-items: center;
  gap: var(--spacing-12);
}

.data-text {
  flex: 1;
}

.data-title {
  font-size: var(--text-3xl);
  font-weight: var(--font-bold);
  margin-bottom: var(--spacing-4);
  color: var(--color-white);
}

.data-description {
  font-size: var(--text-lg);
  margin-bottom: var(--spacing-6);
  opacity: 0.95;
  line-height: 1.8;
}

.data-cta {
  background: var(--color-white);
  color: var(--color-primary);
}

.data-cta:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(255, 255, 255, 0.3);
}

.data-visual {
  flex-shrink: 0;
}

.visual-circle {
  width: 200px;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.2);
  border-radius: var(--radius-full);
  backdrop-filter: blur(10px);
  animation: float 3s ease-in-out infinite;
}

.visual-emoji {
  font-size: 5rem;
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

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-20px);
  }
}

/* 响应式 */
@media (max-width: 1024px) {
  .features-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .data-content {
    flex-direction: column;
    text-align: center;
  }

  .visual-circle {
    width: 150px;
    height: 150px;
  }

  .visual-emoji {
    font-size: 4rem;
  }
}

@media (max-width: 640px) {
  .section-title {
    font-size: var(--text-2xl);
  }

  .features-grid {
    grid-template-columns: 1fr;
    gap: var(--spacing-6);
  }

  .data-title {
    font-size: var(--text-2xl);
  }

  .data-description {
    font-size: var(--text-base);
  }
}
</style>