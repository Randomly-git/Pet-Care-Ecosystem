<template>
  <div class="home-view">
    <AppHeader />

    <!-- 1. Hero 组件 -->
    <section class="banner-section hero-section">
      <div class="banner-bg hero-bg"></div>
      <div class="banner-content hero-content">
        <h1 class="banner-super-title">笑猫的窝</h1>
        <h2 class="banner-title">让宠物生活更美好。</h2>
        <p class="banner-subtitle">全平台的一站式宠物健康管理与社区服务，专为爱宠人士打造。</p>
        <button class="cta-button" @click="goToActivities">开始使用</button>
      </div>
    </section>

    <!-- 2. 日记与社区 组件 (Apple 式高级科技感灰卡片) -->
    <section class="split-section">
      <!-- 宠物日记 -->
      <div class="split-card tech-card" @click="goToActivities">
        <div class="card-content">
          <h3 class="card-title">宠物日记</h3>
          <p class="card-description">记录爱宠的每一天，珍藏专属于你们的美好时光。</p>
          <div class="card-link">进入 <span class="arrow-icon">→</span></div>
        </div>
      </div>

      <!-- 宠物社区 -->
      <div class="split-card tech-card" @click="goToCommunity">
        <div class="card-content">
          <h3 class="card-title">宠物社区</h3>
          <p class="card-description">分享宠物日常起居，与世界各地的同好交流经验。</p>
          <div class="card-link">进入 <span class="arrow-icon">→</span></div>
        </div>
      </div>
    </section>

    <!-- 3. 为什么选择我们 组件 -->
    <section class="banner-section intro-section">
      <div class="banner-bg intro-bg"></div>
      <div class="banner-content intro-content-wrapper">
        <div class="intro-header">
          <h2 class="banner-title intro-title">为什么选择我们？</h2>
          <p class="banner-subtitle intro-subtitle">专业、可靠的宠物生态体验</p>
        </div>

        <div class="features-list">
          <div class="feature-item">
            <h4 class="feature-name">专业健康管理</h4>
            <p class="feature-desc">完整的健康记录和提醒功能，全方位时刻守护您的宠物。</p>
          </div>
          <div class="feature-item">
            <h4 class="feature-name">精彩社区分享</h4>
            <p class="feature-desc">与其他宠物主人交流心得经验，在广场发现更多新乐趣。</p>
          </div>
          <div class="feature-item">
            <h4 class="feature-name">便捷智能服务</h4>
            <p class="feature-desc">一站式宠物生活服务生态圈，省心省力，无忧养宠。</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 4. 关于我们 (About Us) -->
    <section class="about-section">
      <div class="about-container">
        <h2 class="about-title">我们相信，<br>每一次陪伴都值得铭记。</h2>
        <p class="about-desc">
          笑猫の窝不仅是一个冰冷的记录工具，更是充满温度的宠物生态社区。<br>
          我们致力于用极简的科技与纯粹的设计，让全世界的爱宠人士紧密连接，共同见证毛孩子们生命中的每一个闪光时刻。
        </p>
      </div>
    </section>

    <AppFooter />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'

const router = useRouter()
const authStore = useAuthStore()
const isLoggedIn = computed(() => authStore.isAuthenticated)

const goToActivities = () => {
  if (isLoggedIn.value) router.push('/activities')
  else router.push('/login')
}

const goToCommunity = () => {
  if (isLoggedIn.value) router.push('/moments')
  else router.push('/login')
}
</script>

<style scoped>
/* ===== 全局区块通用设定 (等效高度与Apple巨幅海报风格) ===== */
.banner-section {
  position: relative;
  min-height: 80vh;
  /* 与 Hero 保持一样巨大的尺寸 */
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background-color: #000;
  text-align: center;
  padding: 40px 20px;
}

.banner-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0.8;
  z-index: 0;
}

/* 底部黑色渐变遮罩保护文本 */
.banner-bg::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 60%;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.85) 0%, rgba(0, 0, 0, 0) 100%);
}

.banner-content {
  position: relative;
  z-index: 2;
  max-width: 980px;
  width: 100%;
  margin: 0 auto;
}

.banner-super-title {
  font-size: 80px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #f5f5f7;
  margin-bottom: 8px;
}

.banner-title {
  font-size: 56px;
  font-weight: 600;
  letter-spacing: -0.015em;
  color: #f5f5f7;
  margin-bottom: 12px;
}

.banner-subtitle {
  font-size: 24px;
  font-weight: 400;
  color: #d1d1d6;
  margin-bottom: 32px;
}

/* ===== 1. Hero 专有样式 ===== */
.hero-bg {
  /* 恢复为你要求的图片 */
  background: url('https://images.unsplash.com/photo-1639732624839-dd52e940b46c?q=80&w=1332&auto=format&fit=crop') center 20%/cover no-repeat;
}

.hero-content {
  margin-bottom: -15vh;
  /* Apple主页常见文字稍篇底部排版 */
}

.cta-button {
  background-color: #f5f5f7;
  color: #1d1d1f;
  border: none;
  border-radius: 980px;
  padding: 14px 28px;
  font-size: 17px;
  font-weight: 500;
  cursor: pointer;
  transition: transform 0.2s ease, background-color 0.2s ease;
}

.cta-button:hover {
  background-color: #fff;
  transform: scale(1.02);
}

/* ===== 2. 日记与社区 (Apple 科技感无图卡片) ===== */
.split-section {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  padding: 24px;
  background-color: #ffffff;
  min-height: 80vh;
  /* 与前后巨幅区块保持视觉等高 */
}

.split-card {
  flex: 1;
  min-width: 320px;
  position: relative;
  border-radius: 28px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  background-color: #f5f5f7;
  text-align: center;
  padding: 80px 40px;
  transition: transform 0.4s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.4s ease, background-color 0.4s ease;
}

.split-card:hover {
  transform: scale(1.02);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.05);
  background-color: #ffffff;
}

.card-content {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.card-title {
  font-size: 40px;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 16px;
  letter-spacing: -0.01em;
}

.card-description {
  font-size: 20px;
  color: #86868b;
  max-width: 360px;
  margin-bottom: 24px;
  line-height: 1.4;
}

.card-link {
  font-size: 17px;
  color: #0066cc;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: color 0.2s ease;
}

.split-card:hover .card-link {
  color: #0071e3;
}

.arrow-icon {
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.split-card:hover .arrow-icon {
  transform: translateX(4px);
}

/* ===== 3. 为什么选择我们 (背景图与三栏内容) ===== */
.intro-bg {
  /* 使用干净的室内与宠物隐约背景，不喧宾夺主 */
  background: url('https://images.unsplash.com/photo-1583337130417-3346a1be7dee?q=80&w=2688&auto=format&fit=crop') center 70%/cover no-repeat;
  opacity: 0.6;
}

/* 对底层图片进行轻微模糊和极简渐变，完全凸显内容 */
.intro-bg::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.65);
  backdrop-filter: blur(8px);
}

.intro-content-wrapper {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.intro-header {
  margin-bottom: 60px;
}

.intro-title {
  font-size: 48px;
}

.intro-subtitle {
  font-size: 21px;
}

.features-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 40px;
  width: 100%;
  max-width: 900px;
}

.feature-item {
  text-align: center;
}

.feature-name {
  font-size: 21px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 12px;
}

.feature-desc {
  font-size: 17px;
  color: #a1a1a6;
  line-height: 1.5;
}

/* ===== 4. 关于我们 ===== */
.about-section {
  padding: 140px 20px;
  background-color: #fff;
  text-align: center;
}

.about-container {
  max-width: 860px;
  margin: 0 auto;
}

.about-title {
  font-size: 48px;
  font-weight: 600;
  letter-spacing: -0.015em;
  color: #1d1d1f;
  margin-bottom: 32px;
  line-height: 1.25;
}

.about-desc {
  font-size: 21px;
  color: #86868b;
  line-height: 1.6;
}

/* ===== 响应式设计 ===== */
@media (max-width: 768px) {

  .banner-title,
  .intro-title {
    font-size: 40px;
  }

  .banner-subtitle,
  .intro-subtitle {
    font-size: 19px;
  }

  .split-section {
    display: block;
  }

  .split-card {
    min-height: 50vh;
    margin-bottom: 12px;
  }

  .features-list {
    grid-template-columns: 1fr;
    gap: 32px;
  }

  .about-title {
    font-size: 36px;
  }
}
</style>