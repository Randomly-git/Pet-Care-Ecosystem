 <!-- 文件位置: src/components/home/HeroSection.vue -->

<template>
  <section class="hero-section">
    <!-- 使用在线图片作为背景 -->
    <div class="hero-background" :style="backgroundStyle"></div>

    <div class="container">
      <div class="hero-content">
        <!-- 主标题 -->
        <h1 class="hero-title">
          让宠物生活更美好 🐾
        </h1>
        
        <!-- 副标题 -->
        <p class="hero-subtitle">
          一站式宠物健康管理平台,记录成长、在线问诊、智能购物
        </p>
        
        <!-- 搜索框 -->
        <div class="hero-search">
          <div class="search-wrapper">
            <span class="search-icon">🔍</span>
            <input
              v-model="searchQuery"
              type="text"
              placeholder="搜索宠物用品、医生、服务..."
              class="search-input"
              @keyup.enter="handleSearch"
            />
            <BaseButton variant="primary" @click="handleSearch">
              搜索
            </BaseButton>
          </div>
        </div>
        
        <!-- CTA 按钮组 -->
        <div class="hero-actions">
          <BaseButton 
            variant="primary" 
            size="lg"
            @click="handleGetStarted"
          >
            立即体验
          </BaseButton>
          <BaseButton 
            variant="outline" 
            size="lg"
            @click="handleLearnMore"
          >
            了解更多
          </BaseButton>
        </div>
        
        <!-- 统计数据 -->
        <div class="hero-stats">
          <div class="stat-item">
            <span class="stat-number">10万+</span>
            <span class="stat-label">注册用户</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">5000+</span>
            <span class="stat-label">专业医生</span>
          </div>
          <div class="stat-item">
            <span class="stat-number">50万+</span>
            <span class="stat-label">服务次数</span>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import BaseButton from '@/components/base/BaseButton.vue'

const router = useRouter()
const searchQuery = ref('')

// 🖼️ 使用在线宠物图片
const heroBgUrl = 'https://images.unsplash.com/photo-1450778869180-41d0601e046e?w=1920&q=80'

// 或者选择其他宠物图片:
// 狗狗: https://images.unsplash.com/photo-1548199973-03cce0bbc87b?w=1920&q=80
// 猫咪: https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=1920&q=80
// 混合: https://images.unsplash.com/photo-1450778869180-41d0601e046e?w=1920&q=80

const backgroundStyle = computed(() => ({
  backgroundImage: `url(${heroBgUrl})`
}))

const handleSearch = () => {
  if (searchQuery.value.trim()) {
    console.log('搜索:', searchQuery.value)
  }
}

const handleGetStarted = () => {
  router.push('/space')
}

const handleLearnMore = () => {
  document.querySelector('.intro-section')?.scrollIntoView({ 
    behavior: 'smooth' 
  })
}
</script>

<style scoped>
.hero-section {
  position: relative;
  min-height: 600px;
  display: flex;
  align-items: center;
  padding: var(--spacing-20) 0;
  overflow: hidden;
}

/* 🖼️ 背景图片样式 */
.hero-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  z-index: 0;
}

/* 添加深色遮罩层,让白色文字清晰可见 */
.hero-background::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    135deg,
    rgba(99, 102, 241, 0.75) 0%,
    rgba(236, 72, 153, 0.65) 100%
  );
  z-index: 1;
}

/* 内容区布局 */
.hero-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: var(--spacing-8);
  position: relative;
  z-index: 2; /* 确保在背景之上 */
}

/* 标题 - 改为白色 */
.hero-title {
  font-size: var(--text-5xl);
  font-weight: var(--font-bold);
  color: white; /* 白色文字在深色背景上 */
  line-height: 1.2;
  margin: 0;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.3); /* 添加阴影增加可读性 */
  animation: fadeInUp 0.6s ease-out;
}

.hero-subtitle {
  font-size: var(--text-xl);
  color: rgba(255, 255, 255, 0.95); /* 半透明白色 */
  max-width: 600px;
  margin: 0;
  text-shadow: 0 1px 5px rgba(0, 0, 0, 0.2);
  animation: fadeInUp 0.6s ease-out 0.1s backwards;
}

/* 搜索框 */
.hero-search {
  width: 100%;
  max-width: 650px;
  animation: fadeInUp 0.6s ease-out 0.2s backwards;
}

.search-wrapper {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  padding: var(--spacing-2);
  background: rgba(255, 255, 255, 0.95); /* 半透明白色背景 */
  backdrop-filter: blur(10px); /* 毛玻璃效果 */
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-xl);
  transition: all var(--duration-base) var(--ease-out);
}

.search-wrapper:focus-within {
  background: white;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  transform: translateY(-2px);
}

.search-icon {
  font-size: var(--text-2xl);
  margin-left: var(--spacing-4);
  color: var(--color-gray-500);
}

.search-input {
  flex: 1;
  padding: var(--spacing-4);
  font-size: var(--text-base);
  border: none;
  outline: none;
  background: transparent;
  color: var(--color-gray-900);
}

.search-input::placeholder {
  color: var(--color-gray-400);
}

/* CTA 按钮 */
.hero-actions {
  display: flex;
  gap: var(--spacing-4);
  animation: fadeInUp 0.6s ease-out 0.3s backwards;
}

/* 统计数据 */
.hero-stats {
  display: flex;
  gap: var(--spacing-12);
  padding-top: var(--spacing-8);
  animation: fadeInUp 0.6s ease-out 0.4s backwards;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-2);
}

.stat-number {
  font-size: var(--text-3xl);
  font-weight: var(--font-bold);
  color: white;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.stat-label {
  font-size: var(--text-sm);
  color: rgba(255, 255, 255, 0.9);
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
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
@media (max-width: 768px) {
  .hero-section {
    min-height: 500px;
    padding: var(--spacing-12) 0;
  }

  .hero-title {
    font-size: var(--text-3xl);
  }

  .hero-subtitle {
    font-size: var(--text-lg);
  }

  .hero-actions {
    flex-direction: column;
    width: 100%;
  }

  .hero-stats {
    gap: var(--spacing-6);
  }

  .stat-number {
    font-size: var(--text-2xl);
  }
}
</style>