<template>
  <div class="home-view">
    <!-- Header 组件 -->
    <AppHeader />

    <!-- Hero Section -->
    <section class="hero-section">
      <div class="hero-container">
        <div class="hero-content">
          <div class="hero-emoji">🐾</div>
          <h2 class="hero-title">让宠物生活更美好</h2>
          <p class="hero-subtitle">一站式宠物健康管理平台</p>

          <!-- 搜索框 -->
          <div class="search-container">
            <div class="search-box">
              <div class="search-icon">🔍</div>
              <input
                v-model="searchQuery"
                type="text"
                class="search-input"
                placeholder="搜索宠物名称、健康记录..."
                @keyup.enter="handleSearch"
              >
            </div>
          </div>

          <!-- CTA按钮 -->
          <button class="cta-button" @click="goToPetSpace">
            开始使用
            <div class="cta-arrow">→</div>
          </button>
        </div>
      </div>
    </section>

    <!-- 功能导航卡片 -->
    <section class="features-section">
      <div class="features-container">
        <div class="features-grid">
          <!-- 我的空间 -->
          <div class="feature-card" @click="goToPetSpace">
            <div class="card-icon">🏠</div>
            <h3 class="card-title">我的空间</h3>
            <p class="card-description">管理宠物和动态记录</p>
            <div class="card-arrow">
              <span>进入</span>
              <div class="arrow-icon">→</div>
            </div>
          </div>

          <!-- 活动记录 -->
          <div class="feature-card" @click="goToActivities">
            <div class="card-icon">🐾</div>
            <h3 class="card-title">活动记录</h3>
            <p class="card-description">记录宠物日常活动和健康</p>
            <div class="card-arrow">
              <span>进入</span>
              <div class="arrow-icon">→</div>
            </div>
          </div>

          <!-- 看兽医 -->
          <div class="feature-card" @click="goToMedical">
            <div class="card-icon">🏥</div>
            <h3 class="card-title">看兽医</h3>
            <p class="card-description">在线问诊和健康咨询</p>
            <div class="card-arrow">
              <span>进入</span>
              <div class="arrow-icon">→</div>
            </div>
          </div>

          <!-- 去逛街 -->
          <div class="feature-card" @click="goToShopping">
            <div class="card-icon">🛍️</div>
            <h3 class="card-title">去逛街</h3>
            <p class="card-description">宠物用品和精选商城</p>
            <div class="card-arrow">
              <span>进入</span>
              <div class="arrow-icon">→</div>
            </div>
          </div>

          <!-- 宠物社区 -->
          <div class="feature-card" @click="goToCommunity">
            <div class="card-icon">💬</div>
            <h3 class="card-title">宠物社区</h3>
            <p class="card-description">分享宠物生活和交流经验</p>
            <div class="card-arrow">
              <span>进入</span>
              <div class="arrow-icon">→</div>
            </div>
          </div>

          </div>
      </div>
    </section>

    <!-- 平台介绍 -->
    <section class="intro-section">
      <div class="intro-container">
        <div class="intro-header">
          <h2 class="intro-title">为什么选择笑猫の窝？</h2>
          <p class="intro-subtitle">专业的宠物健康管理解决方案</p>
        </div>

        <div class="features-list">
          <div class="feature-item">
            <div class="feature-icon">✅</div>
            <div class="feature-content">
              <h4 class="feature-name">专业健康管理</h4>
              <p class="feature-desc">完整的健康记录和提醒功能</p>
            </div>
          </div>

          <div class="feature-item">
            <div class="feature-icon">✅</div>
            <div class="feature-content">
              <h4 class="feature-name">社区分享</h4>
              <p class="feature-desc">与其他宠物主人交流经验</p>
            </div>
          </div>

          <div class="feature-item">
            <div class="feature-icon">✅</div>
            <div class="feature-content">
              <h4 class="feature-name">便捷服务</h4>
              <p class="feature-desc">一站式宠物生活服务平台</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Footer 组件 -->
    <AppFooter />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'

const router = useRouter()
const authStore = useAuthStore()

// 响应式数据
const searchQuery = ref('')

// 计算属性
const isLoggedIn = computed(() => authStore.isAuthenticated)

// 方法
const handleSearch = () => {
  if (!searchQuery.value.trim()) return
  // TODO: 实现搜索功能
  alert(`搜索功能开发中，搜索词：${searchQuery.value}`)
}

const goToPetSpace = () => {
  if (isLoggedIn.value) {
    router.push('/space')
  } else {
    router.push('/login')
  }
}

const goToMedical = () => {
  if (isLoggedIn.value) {
    router.push('/medical')
  } else {
    router.push('/login')
  }
}

const goToShopping = () => {
  if (isLoggedIn.value) {
    router.push('/shop')
  } else {
    router.push('/login')
  }
}

const goToActivities = () => {
  if (isLoggedIn.value) {
    router.push('/activities')
  } else {
    router.push('/login')
  }
}

const goToCommunity = () => {
  if (isLoggedIn.value) {
    router.push('/moments')  // 跳转到朋友圈页面
  } else {
    router.push('/login')
  }
}

const handleLogin = () => {
  router.push('/login')
}

const handleRegister = () => {
  router.push('/register')
}

const handleLogout = async () => {
  await authStore.logout()
}
</script>

<style scoped>
/* ===== Hero Section ===== */
.hero-section {
  background: linear-gradient(135deg, rgba(255, 247, 237, 0.3) 0%, rgba(254, 215, 170, 0.3) 100%),
              url('https://images.unsplash.com/photo-1639732624839-dd52e940b46c?q=80&w=1332&auto=format&fit=crop') center/cover no-repeat;
  padding: var(--spacing-20) 0;
  position: relative;
  min-height: 600px;
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
  background: linear-gradient(135deg, rgba(255, 247, 237, 0.4) 0%, rgba(254, 215, 170, 0.4) 100%);
  z-index: 1;
}

.hero-container {
  max-width: var(--container-lg);
  margin: 0 auto;
  padding: 0 var(--spacing-6);
  text-align: center;
  position: relative;
  z-index: 2;
  flex: 1;
  display: flex;
  align-items: center;
}

.hero-content {
  max-width: 600px;
  margin: 0 auto;
  position: relative;
  z-index: 2;
}

.hero-emoji {
  font-size: 4rem;
  margin-bottom: var(--spacing-6);
}

.hero-title {
  font-size: var(--text-5xl);
  font-weight: var(--font-bold);
  color: var(--color-black);
  margin-bottom: var(--spacing-4);
  line-height: 1.2;
}

.hero-subtitle {
  font-size: var(--text-xl);
  color: var(--color-gray-600);
  margin-bottom: var(--spacing-8);
}

/* ===== 搜索框 ===== */
.search-container {
  margin-bottom: var(--spacing-8);
}

.search-box {
  display: flex;
  align-items: center;
  background: var(--color-white);
  border: 2px solid var(--color-gray-200);
  border-radius: var(--radius-xl);
  padding: var(--spacing-2) var(--spacing-4);
  transition: all var(--duration-base);
}

.search-box:focus-within {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(255, 140, 0, 0.1);
}

.search-icon {
  font-size: var(--text-lg);
  color: var(--color-gray-400);
  margin-right: var(--spacing-3);
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: var(--text-base);
  color: var(--color-gray-700);
  background: transparent;
}

.search-input::placeholder {
  color: var(--color-gray-400);
}

/* ===== CTA按钮 ===== */
.cta-button {
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-2);
  background: var(--gradient-primary);
  color: var(--color-white);
  border: none;
  border-radius: var(--radius-lg);
  padding: var(--spacing-4) var(--spacing-8);
  font-size: var(--text-lg);
  font-weight: var(--font-medium);
  cursor: pointer;
  transition: all var(--duration-base);
  box-shadow: var(--shadow-md);
}

.cta-button:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

.cta-arrow {
  font-size: var(--text-xl);
  font-weight: var(--font-bold);
  transition: transform var(--duration-base);
}

.cta-button:hover .cta-arrow {
  transform: translateX(4px);
}

/* ===== 功能导航卡片 ===== */
.features-section {
  padding: var(--spacing-20) 0;
  background: var(--color-white);
}

.features-container {
  max-width: var(--container-xl);
  margin: 0 auto;
  padding: 0 var(--spacing-6);
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: var(--spacing-8);
}

.feature-card {
  background: var(--color-white);
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-xl);
  padding: var(--spacing-8);
  cursor: pointer;
  transition: all var(--duration-base);
  box-shadow: var(--shadow-sm);
  position: relative;
  overflow: hidden;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: var(--color-primary-light);
}

.primary-card {
  background: var(--gradient-primary);
  color: var(--color-white);
  border-color: transparent;
}

.card-icon {
  font-size: 3rem;
  margin-bottom: var(--spacing-4);
  line-height: 1;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  background: var(--color-gray-50);
  border-radius: var(--radius-xl);
  margin: 0 auto var(--spacing-4);
}

.primary-card .card-icon {
  background: rgba(255, 255, 255, 0.2);
  filter: brightness(1.2);
}

.feature-card:nth-child(2) .card-icon {
  background: linear-gradient(135deg, rgba(74, 144, 226, 0.1) 0%, rgba(24, 144, 255, 0.1) 100%);
}

.feature-card:nth-child(3) .card-icon {
  background: linear-gradient(135deg, rgba(82, 196, 26, 0.1) 0%, rgba(115, 209, 61, 0.1) 100%);
}

.card-title {
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  margin-bottom: var(--spacing-2);
  color: inherit;
}

.card-description {
  font-size: var(--text-base);
  color: var(--color-gray-600);
  margin-bottom: var(--spacing-4);
  line-height: 1.6;
}

.primary-card .card-description {
  color: rgba(255, 255, 255, 0.9);
}

.card-arrow {
  display: flex;
  align-items: center;
  gap: var(--spacing-1);
  font-weight: var(--font-medium);
  color: var(--color-primary);
}

.primary-card .card-arrow {
  color: var(--color-white);
}

.arrow-icon {
  font-size: var(--text-lg);
  transition: transform var(--duration-base);
}

.feature-card:hover .arrow-icon {
  transform: translateX(4px);
}

/* ===== 平台介绍 ===== */
.intro-section {
  padding: var(--spacing-20) 0;
  background: linear-gradient(135deg, rgba(250, 251, 252, 0.85) 0%, rgba(245, 247, 250, 0.85) 100%),
              url('https://images.unsplash.com/photo-1587300003388-592b2dc77263?q=80&w=1920&auto=format&fit=crop') center/cover no-repeat;
  position: relative;
  min-height: 400px;
}

.intro-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(250, 251, 252, 0.9) 0%, rgba(245, 247, 250, 0.9) 100%);
  z-index: 1;
}

.intro-container {
  max-width: var(--container-lg);
  margin: 0 auto;
  padding: 0 var(--spacing-6);
  text-align: center;
  position: relative;
  z-index: 2;
}

.intro-header {
  margin-bottom: var(--spacing-12);
  position: relative;
  z-index: 2;
  padding: var(--spacing-8) 0;
}

.intro-title {
  font-size: var(--text-4xl);
  font-weight: var(--font-bold);
  color: var(--color-black);
  margin-bottom: var(--spacing-4);
}

.intro-subtitle {
  font-size: var(--text-xl);
  color: var(--color-gray-600);
}

.features-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--spacing-8);
  text-align: left;
  position: relative;
  z-index: 2;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-4);
}

.feature-icon {
  font-size: var(--text-2xl);
  line-height: 1;
  margin-top: 2px;
}

.feature-content {
  flex: 1;
}

.feature-name {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--color-black);
  margin-bottom: var(--spacing-1);
}

.feature-desc {
  font-size: var(--text-base);
  color: var(--color-gray-600);
  line-height: 1.6;
}

/* ===== 响应式设计 ===== */
@media (max-width: 768px) {
  .hero-title {
    font-size: var(--text-4xl);
  }

  .hero-subtitle {
    font-size: var(--text-lg);
  }

  .features-grid {
    grid-template-columns: 1fr;
    gap: var(--spacing-6);
  }

  .features-list {
    grid-template-columns: 1fr;
    gap: var(--spacing-6);
  }
}

@media (max-width: 480px) {
  .hero-container,
  .features-container,
  .intro-container {
    padding: 0 var(--spacing-4);
  }

  .hero-title {
    font-size: var(--text-3xl);
  }

  .cta-button {
    padding: var(--spacing-3) var(--spacing-6);
    font-size: var(--text-base);
  }

  .feature-card {
    padding: var(--spacing-6);
  }
}
</style>