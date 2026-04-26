<template>
  <div class="home-view">
    <AppHeader />

    <!-- 1. Hero 亲自然巨幅组件 -->
    <section id="hero" class="banner-section hero-section">
      <!-- 阳关径向渐变背景 -->
      <div class="sunlight-bg"></div>

      <div class="hero-container">
        <!-- 保留原始狗图片，但套用自然风阴影圆角和树叶遮罩 -->
        <div class="hero-image-wrapper fly-in-up">
          <img src="https://images.unsplash.com/photo-1639732624839-dd52e940b46c?q=80&w=1332&auto=format&fit=crop"
            alt="Happy Dog" class="hero-image" />
          <!-- 藤蔓与树叶 SVG 叠加 -->
          <div class="leaf-overlay leaf-1 animate-float-slow">
            <svg viewBox="0 0 100 100" fill="currentColor">
              <path d="M50,10 Q70,30 50,90 Q30,30 50,10" />
            </svg>
          </div>
          <div class="leaf-overlay leaf-2 animate-float">
            <svg viewBox="0 0 100 100" fill="currentColor">
              <path d="M50,10 Q30,40 50,90 Q70,40 50,10" />
            </svg>
          </div>
        </div>

        <div class="hero-content fly-in-up" style="transition-delay: 0.1s;">
          <!-- 增加顶部小文案 -->
          <div class="hero-tags">
            <span class="hero-tag">日志</span>
            <span class="hero-tag-dot">•</span>
            <span class="hero-tag">社区</span>
            <span class="hero-tag-dot">•</span>
            <span class="hero-tag">周边</span>
          </div>

          <h1 class="nature-title main-title" style="white-space: nowrap;">让宠物生活更美好。</h1>
          <p class="nature-subtitle">一个拥抱自然的宠物健康追踪流、智能全景地图与温度社区的交汇点。<br>告别繁杂的表格，在这里系统化体验生命的陪伴。</p>

          <div id="dashboard" class="hero-dashboard-container">
            <template v-if="isLoggedIn">
              <div class="user-dashboard-mini fly-in-up">
                <div class="dash-card">
                  <div class="dash-value text-sky-400">2</div>
                  <div class="dash-label">名下宠物</div>
                </div>
                <div class="dash-divider"></div>
                <div class="dash-card">
                  <div class="dash-value text-green-500">12</div>
                  <div class="dash-label">签到活动</div>
                </div>
                <div class="dash-divider"></div>
                <div class="dash-card">
                  <div class="dash-value text-amber-500">6</div>
                  <div class="dash-label">社区瞬间</div>
                </div>
              </div>
            </template>
            <template v-else>
              <div class="auth-prompt-card fly-in-up">
                <p>登录开启全域生态服务，同步您的关爱之旅。</p>
                <div class="auth-actions">
                  <button class="nature-button auth-primary-btn" @click="router.push('/login')">立刻登录</button>
                  <button class="nature-button auth-secondary-btn" @click="router.push('/register')">加入我们</button>
                </div>
              </div>
            </template>
          </div>

          <div v-if="isLoggedIn" class="hero-actions-advanced fly-in-up" style="transition-delay: 0.2s;">
            <button class="btn-premium-primary" @click="goToActivities">
              进入服务中心 
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"></line><polyline points="12 5 19 12 12 19"></polyline></svg>
            </button>
            <button class="btn-premium-secondary" @click="goToCommunity">
              发现社区
            </button>
          </div>
        </div>
      </div>

      <!-- 波浪形分隔线 (过滤至纯白) -->
      <div class="wave-divider bottom-wave">
        <svg viewBox="0 0 1440 120" preserveAspectRatio="none">
          <path fill="#ffffff" fill-opacity="1"
            d="M0,32L60,42.7C120,53,240,75,360,74.7C480,75,600,53,720,48C840,43,960,53,1080,69.3C1200,85,1320,107,1380,117.3L1440,128L1440,120L1380,120C1320,120,1200,120,1080,120C960,120,840,120,720,120C600,120,480,120,360,120C240,120,120,120,60,120L0,120Z">
          </path>
        </svg>
      </div>
    </section>

    <!-- 纯白背景过渡区包含卡片与照片墙 -->
    <div class="white-bg-container" style="background-color: #ffffff;">
      <!-- 2. 日记与社区 (纯自然卡片) -->
      <section class="split-section">
        <div class="nature-card fly-in-up" @click="goToActivities">
          <div class="card-leaf-decoration">
            <svg viewBox="0 0 100 100" fill="currentColor" class="text-mint-green">
              <path d="M50,10 Q70,30 50,90 Q30,30 50,10" />
            </svg>
          </div>
          <div class="card-content">
            <h3 class="nature-title card-title">宠物日志</h3>
            <p class="nature-text card-description">记录宠物的日常活动。</p>
            <div class="nature-link">进入 <span class="arrow-icon">→</span></div>
          </div>
        </div>

        <div class="nature-card fly-in-up" @click="goToCommunity" style="transition-delay: 0.15s;">
          <div class="card-leaf-decoration right-leaf">
            <svg viewBox="0 0 100 100" fill="currentColor" class="text-sky-blue">
              <path d="M50,10 Q70,30 50,90 Q30,30 50,10" />
            </svg>
          </div>
          <div class="card-content">
            <h3 class="nature-title card-title">宠物社区</h3>
            <p class="nature-text card-description">分享宠物的日常，交流养宠心得。</p>
            <div class="nature-link">进入 <span class="arrow-icon">→</span></div>
          </div>
        </div>

        <div class="nature-card fly-in-up" @click="goToMap" style="transition-delay: 0.3s;">
          <div class="card-leaf-decoration right-leaf">
            <svg viewBox="0 0 100 100" fill="currentColor" class="text-amber-500" style="color: #f59e0b;">
              <path d="M50,10 C50,10 20,50 20,70 A30,30 0 0,0 80,70 C80,50 50,10 50,10 Z" />
            </svg>
          </div>
          <div class="card-content">
            <h3 class="nature-title card-title">周边网络</h3>
            <p class="nature-text card-description">探索发现附近的宠物医院与服务点。</p>
            <div class="nature-link">进入 <span class="arrow-icon">→</span></div>
          </div>
        </div>
      </section>

      <!-- 3. 新增照片墙组件 (Instagram 社区动态展示) -->
      <section class="photo-wall-section max-w-container">
        <div class="photo-header fly-in-up">
          <h2 class="nature-title section-title">发现萌宠瞬间</h2>
          <p class="nature-subtitle text-center">探索更多来自动物社区的温暖日常</p>
        </div>

        <div class="photo-masonry">
          <!-- Item 1 (大图) -->
          <div class="photo-item photo-large fly-in-up">
            <div class="ins-border">
              <img src="https://images.unsplash.com/photo-1543466835-00a7907e9de1?q=80&w=1000&auto=format&fit=crop"
                alt="Cute dog">
            </div>
          </div>
          <!-- Item 2 (长竖图) -->
          <div class="photo-item photo-vertical fly-in-up" style="transition-delay: 0.1s;">
            <div class="ins-border">
              <img src="https://images.unsplash.com/photo-1517849845537-4d257902454a?q=80&w=600&auto=format&fit=crop"
                alt="Dog">
            </div>
          </div>
          <!-- Item 3 (长横图) -->
          <div class="photo-item photo-horizontal fly-in-up" style="transition-delay: 0.2s;">
            <div class="ins-border">
              <img src="https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?q=80&w=800&auto=format&fit=crop"
                alt="Cat in bag">
            </div>
          </div>
          <!-- Item 4 (小图) -->
          <div class="photo-item photo-small fly-in-up" style="transition-delay: 0.3s;">
            <div class="ins-border">
              <img src="https://images.unsplash.com/photo-1533743983669-94fa5c4338ec?q=80&w=400&auto=format&fit=crop"
                alt="Kitten">
            </div>
          </div>
          <!-- Item 5 (中图) -->
          <div class="photo-item photo-medium fly-in-up" style="transition-delay: 0.4s;">
            <div class="ins-border">
              <img src="https://images.unsplash.com/photo-1548199973-03cce0bbc87b?q=80&w=600&auto=format&fit=crop"
                alt="Running dogs">
            </div>
          </div>
        </div>
      </section>

      <!-- 倒转波浪形分隔线 (从白回石色) -->
      <div class="wave-divider top-wave">
        <svg viewBox="0 0 1440 120" preserveAspectRatio="none">
          <path fill="#fafaf9" fill-opacity="1"
            d="M0,64L60,58.7C120,53,240,43,360,53.3C480,64,600,96,720,101.3C840,107,960,85,1080,74.7C1200,64,1320,64,1380,64L1440,64L1440,120L1380,120C1320,120,1200,120,1080,120C960,120,840,120,720,120C600,120,480,120,360,120C240,120,120,120,60,120L0,120Z">
          </path>
        </svg>
      </div>
    </div>

    <!-- 4. 生态服务 -->
    <section id="services" class="features-section">
      <div class="features-header fly-in-up">
        <h2 class="nature-title section-title">专业生态服务</h2>
      </div>

      <div class="features-list">
        <div class="feature-item fly-in-up">
          <div class="feature-icon water-drop">
            <svg viewBox="0 0 100 100" fill="currentColor">
              <path d="M50,10 C50,10 20,50 20,70 A30,30 0 0,0 80,70 C80,50 50,10 50,10 Z" />
            </svg>
          </div>
          <!-- 增加一张展示图满足要求 -->
          <img src="https://images.unsplash.com/photo-1541364983171-a8ba01e95cfc?q=80&w=400&auto=format&fit=crop"
            class="feature-img" alt="Health">
          <h4 class="feature-name">健康守护</h4>
          <p class="feature-desc">为您建立完整的爱宠健康档案，日记打卡、疫苗接种一手掌握。</p>
        </div>

        <div class="feature-item fly-in-up" style="transition-delay: 0.15s;">
          <div class="feature-icon leaf-icon">
            <svg viewBox="0 0 100 100" fill="currentColor">
              <path d="M50,10 Q70,30 50,90 Q30,30 50,10" />
            </svg>
          </div>
          <img src="https://images.unsplash.com/photo-1583337130417-3346a1be7dee?q=80&w=400&auto=format&fit=crop"
            class="feature-img" alt="Community">
          <h4 class="feature-name">温馨社区</h4>
          <p class="feature-desc">在这里与天南海北的爱宠人士畅游交流，分享毛孩子们的温馨日常。</p>
        </div>

        <div class="feature-item fly-in-up" style="transition-delay: 0.3s;">
          <div class="feature-icon earth-icon">
            <svg viewBox="0 0 100 100" fill="currentColor">
              <circle cx="50" cy="50" r="40" />
            </svg>
          </div>
          <img src="https://images.unsplash.com/photo-1581888227599-779811939961?q=80&w=400&auto=format&fit=crop"
            class="feature-img" alt="Map">
          <h4 class="feature-name">周边网络</h4>
          <p class="feature-desc">一键发现附近5公里优质宠物医院与美容店，构筑安全便利的宠物生活圈。</p>
        </div>
      </div>
    </section>

    <!-- 波浪形分隔线 (过滤至浅绿色区) -->
    <div class="wave-divider bottom-wave about-wave" style="background:#fafaf9">
      <svg viewBox="0 0 1440 60" preserveAspectRatio="none">
        <path fill="#f0fdf4" fill-opacity="1"
          d="M0,32L80,26.7C160,21,320,11,480,16C640,21,800,43,960,42.7C1120,43,1280,21,1360,10.7L1440,0L1440,60L1360,60C1280,60,1120,60,960,60C800,60,640,60,480,60C320,60,160,60,80,60L0,60Z">
        </path>
      </svg>
    </div>

    <!-- 5. 结尾标语 -->
    <section class="about-section">
      <div class="about-container fly-in-up">
        <h2 class="nature-title quote-title">"每一次用心的陪伴，<br>都是对生命最好的馈赠。"</h2>
        <p class="nature-text quote-desc">
          打造系统化、无干扰的宠物科技空间，<br>
          回归健康、自由、充满呼吸感的生活绿洲。
        </p>
      </div>
    </section>

    <AppFooter />
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
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

const goToMap = () => {
  if (isLoggedIn.value) router.push('/map')
  else router.push('/login')
}

let observer = null

onMounted(() => {
  observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('fly-in-visible')
        // 触发一次后移除监听
        observer.unobserve(entry.target)
      }
    })
  }, {
    threshold: 0.1,
    rootMargin: "0px 0px -50px 0px"
  })

  // 为所有含 fly-in-up 的元素绑定
  document.querySelectorAll('.fly-in-up').forEach(el => {
    observer.observe(el)
  })
})

onUnmounted(() => {
  if (observer) {
    observer.disconnect()
  }
})
</script>

<style scoped>


/* =========== 新增主页微型图表板 =========== */
.hero-dashboard-container {
  margin-top: 24px;
  width: 100%;
}
.user-dashboard-mini, .auth-prompt-card {
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 10px 40px rgba(0,0,0,0.05); /* Apple高级弥散感 */
  padding: 16px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
}
.dash-card {
  flex: 1;
  text-align: center;
}
.dash-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 4px;
}
.dash-label {
  font-size: 13px;
  color: #4b5563;
  font-weight: 600;
}
.dash-divider {
  width: 1px;
  height: 40px;
  background: rgba(0,0,0,0.1);
}
.auth-prompt-card {
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
}
.auth-prompt-card p {
  color: #374151;
  font-size: 15px;
  margin: 0;
  font-weight: 600;
}
.auth-actions {
  display: flex;
  gap: 12px;
}
.auth-primary-btn {
  background: #38bdf8;
  color: #fff;
  padding: 8px 16px;
  font-size: 14px;
}
.auth-secondary-btn {
  background: rgba(255,255,255,0.7);
  color: #374151;
  padding: 8px 16px;
  font-size: 14px;
  box-shadow: 0 0 0 1px rgba(0,0,0,0.05) inset;
}
.auth-primary-btn:hover { background: #0ea5e9; }
.auth-secondary-btn:hover { background: #fff; }

/* ===== 高级按钮组 ===== */
.hero-actions-advanced {
  display: flex;
  gap: 16px;
  margin-top: 28px;
  align-items: center;
}

.btn-premium-primary {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f97316; /* 活力日落橙 */
  color: #ffffff;
  font-size: 16px;
  font-weight: 500;
  padding: 12px 28px;
  border-radius: 9999px;
  border: none;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 4px 14px rgba(249, 115, 22, 0.25);
}
.btn-premium-primary svg {
  margin-left: 8px;
  transition: transform 0.3s ease;
}
.btn-premium-primary:hover {
  background-color: #ea580c; /* 深砖橙 */
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(249, 115, 22, 0.4);
}
.btn-premium-primary:hover svg {
  transform: translateX(4px);
}

.btn-premium-secondary {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: transparent;
  color: #2b4c3b;
  font-size: 16px;
  font-weight: 500;
  padding: 12px 28px;
  border-radius: 9999px;
  border: 1px solid #2b4c3b;
  cursor: pointer;
  transition: all 0.3s ease;
}
.btn-premium-secondary:hover {
  background-color: rgba(43, 76, 59, 0.05);
}

.text-sky-400 { color: #38bdf8; }
.text-green-500 { color: #22c55e; }
.text-amber-500 { 
  color: #f97316; /* 替换为活力血橙 */
  text-shadow: 0 4px 14px rgba(249, 115, 22, 0.2); 
}
.mt-4 { margin-top: 16px; }
/* ==== 向下滑动进入动画 (IntersectionObserver) ==== */


/* ===== 基础排版与宏观色调设定 ===== */
.home-view {
  background-color: #fafaf9;
  color: #374151;
  font-family: "Microsoft YaHei", "PingFang SC", system-ui, -apple-system, sans-serif;
}

.nature-title {
  font-family: "Microsoft YaHei", "PingFang SC", system-ui, sans-serif;
  color: #14532d;
  font-weight: 600;
  letter-spacing: 0.05em;
}

.nature-text {
  color: #4b5563;
  line-height: 1.8;
}

/* 全新动态：向上飞入动画效果 */
.fly-in-up {
  opacity: 0;
  transform: translateY(60px);
  transition: opacity 0.8s cubic-bezier(0.16, 1, 0.3, 1), transform 0.8s cubic-bezier(0.16, 1, 0.3, 1);
}

.fly-in-visible {
  opacity: 1;
  transform: translateY(0);
}

/* 公共宽度限制拉宽 */
.max-w-container {
  max-width: 1400px;
  /* 拉大主视图宽度 */
  margin: 0 auto;
}

/* 波浪分隔线 */
.wave-divider {
  width: 100%;
  overflow: hidden;
  line-height: 0;
}

.wave-divider svg {
  display: block;
  width: calc(100% + 1.3px);
}

.bottom-wave {
  position: absolute;
  bottom: 0;
  left: 0;
  height: 80px;
}

.top-wave {
  height: 80px;
  background-color: #ffffff;
}

/* ===== Hero 区 ===== */
.hero-section {
  position: relative;
  min-height: 85vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #fafaf9;
  padding: 80px 20px 120px;
}

.sunlight-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: 
    radial-gradient(circle at 70% 30%, rgba(254, 252, 211, 0.4) 0%, rgba(250, 250, 249, 0) 60%),
    radial-gradient(circle at 0% 0%, rgba(249, 115, 22, 0.08) 0%, rgba(250, 250, 249, 0) 50%); /* 日落暖光点缀 */
  z-index: 0;
}

.hero-container {
  position: relative;
  z-index: 2;
  max-width: 1400px;
  width: 100%;
  display: flex;
  flex-direction: row-reverse;
  align-items: center;
  gap: 80px;
}

.hero-image-wrapper {
  flex: 1;
  position: relative;
  border-radius: 2rem;
  box-shadow: 0 20px 40px -10px rgba(22, 163, 74, 0.15);
  background: white;
  padding: 16px;
  transform: rotate(2deg);
  transition: transform 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

.hero-image-wrapper.fly-in-visible:hover {
  transform: rotate(0deg) scale(1.02);
  /* 兼容触发动画后的复合变换 */
}

.hero-image {
  width: 100%;
  height: auto;
  border-radius: 1.5rem;
  object-fit: cover;
  display: block;
}

.leaf-overlay {
  position: absolute;
  z-index: 3;
}

.leaf-1 {
  width: 80px;
  height: 80px;
  bottom: -30px;
  left: -30px;
  color: #15803d;
  transform-origin: center right;
}

.leaf-2 {
  width: 60px;
  height: 60px;
  top: -20px;
  right: -20px;
  color: #22c55e;
  transform-origin: bottom left;
}

.hero-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.hero-tags {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.1em;
  color: #15803d;
  background: rgba(22, 163, 74, 0.08);
  padding: 8px 16px;
  border-radius: 999px;
  text-transform: uppercase;
}

.hero-tag-dot {
  opacity: 1;
  color: #f97316; /* 橙色指示点 */
  font-size: 14px;
}

.main-title {
  font-size: 52px;
  margin-bottom: 24px;
  line-height: 1.25;
}

.nature-subtitle {
  font-size: 20px;
  color: #4b5563;
  line-height: 1.8;
  margin-bottom: 40px;
  max-width: 480px;
}

.primary-btn {
  background-color: #16a34a;
  color: white;
  border: none;
  border-radius: 9999px;
  padding: 18px 48px;
  font-size: 18px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: 0 10px 20px -5px rgba(22, 163, 74, 0.3);
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.primary-btn:hover {
  background-color: #15803d;
  transform: translateY(-2px) scale(1.02);
  box-shadow: 0 14px 24px -5px rgba(22, 163, 74, 0.4);
}

/* ===== 卡片区块 ===== */
.split-section {
  display: flex;
  flex-wrap: wrap;
  gap: 32px;
  padding: 60px 40px 80px;
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
}

.nature-card {
  flex: 1;
  min-width: 300px;
  position: relative;
  background-color: #f0fdf4;
  border-radius: 24px;
  padding: 48px 40px;
  cursor: pointer;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(22, 163, 74, 0.05);
  transition: box-shadow 0.25s, transform 0.8s, background-color 0.25s;
  /* 保留飞入时的 transform 复合 */
}

.nature-card:nth-child(2) {
  background-color: #fafaf9;
  margin-top: 40px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

/* Hover需处理与 fly-in-visible 的 translateY(0) 叠加 */
.nature-card.fly-in-visible:hover {
  transform: translateY(-8px);
  box-shadow: 0 20px 25px -5px rgba(22, 163, 74, 0.1), 0 8px 10px -6px rgba(22, 163, 74, 0.04);
}

.card-leaf-decoration {
  position: absolute;
  top: 0;
  right: 0;
  width: 120px;
  height: 120px;
  opacity: 0.08;
  color: #16a34a;
  transform: translate(20%, -20%) rotate(15deg);
  transition: all 0.5s ease;
}

.nature-card:hover .card-leaf-decoration {
  opacity: 0.15;
  transform: translate(15%, -15%) rotate(5deg) scale(1.1);
}

.text-mint-green {
  color: #84cc16;
}

.text-sky-blue {
  color: #0ea5e9;
}

.card-content {
  position: relative;
  z-index: 2;
}

.card-title {
  font-size: 32px;
  margin-bottom: 20px;
}

.card-description {
  font-size: 18px;
  margin-bottom: 32px;
}

.nature-link {
  font-size: 17px;
  color: #15803d;
  font-weight: 500;
  font-style: italic;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: color 0.2s ease;
}

.arrow-icon {
  transition: transform 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.nature-card:hover .arrow-icon {
  transform: translateX(6px);
}

/* ===== 新增：Instagram 照片墙 ===== */
.photo-wall-section {
  padding: 20px 40px 100px;
}

.photo-header {
  text-align: center;
  margin-bottom: 40px;
}

.photo-masonry {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-template-rows: repeat(2, 260px);
  gap: 24px;
}

/* Ins风格边框特效与Hover暗化层 */
.photo-item {
  position: relative;
  border-radius: 24px;
  overflow: hidden;
  cursor: pointer;
  background: white;
  transition: transform 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.photo-item.fly-in-visible:hover {
  transform: translateY(-6px) scale(1.02);
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.15);
}

.ins-border {
  position: relative;
  width: 100%;
  height: 100%;
  /* 模拟Instagram精美渐变加圈边框的风格 */
  padding: 4px;
  background: linear-gradient(45deg, #f09433 0%, #e6683c 25%, #dc2743 50%, #cc2366 75%, #bc1888 100%);
  border-radius: 24px;
}

.ins-border img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 20px;
  border: 4px solid white;
  transition: transform 0.4s ease;
}

.photo-item:hover .ins-border img {
  transform: scale(1.05);
  /* 仅图片放大，外框不动 */
}

/* 瀑布布局定义 */
.photo-large {
  grid-column: span 2;
  grid-row: span 2;
}

.photo-vertical {
  grid-row: span 2;
}

.photo-horizontal {
  grid-column: span 2;
}

.photo-small,
.photo-medium {
  grid-column: span 1;
}

/* ===== 生态服务 ===== */
.features-section {
  padding: 40px 20px 100px;
  background-color: #fafaf9;
}

.features-header {
  text-align: center;
  margin-bottom: 60px;
}

.section-title {
  font-size: 40px;
  margin-bottom: 16px;
}

.features-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 40px;
  max-width: 1400px;
  margin: 0 auto;
}

.feature-item {
  text-align: center;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.feature-icon {
  width: 64px;
  height: 64px;
  margin-bottom: 24px;
  opacity: 0.85;
}

.water-drop {
  color: #0ea5e9;
}

.leaf-icon {
  color: #16a34a;
}

.earth-icon {
  color: #92400e;
}

.feature-img {
  width: 100%;
  height: 220px;
  object-fit: cover;
  border-radius: 24px;
  margin-bottom: 24px;
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.05);
}

.feature-name {
  font-size: 22px;
  font-weight: 600;
  color: #14532d;
  margin-bottom: 12px;
}

.feature-desc {
  font-size: 17px;
  color: #6b7280;
  line-height: 1.6;
}

/* ===== 结尾标语 ===== */
.about-section {
  background-color: #f0fdf4;
  padding: 100px 20px;
  text-align: center;
}

.about-container {
  max-width: 800px;
  margin: 0 auto;
}

.quote-title {
  font-size: 40px;
  line-height: 1.5;
  margin-bottom: 32px;
  color: #15803d;
}

.quote-desc {
  font-size: 18px;
}

/* 植物动效 */
@keyframes float {

  0%,
  100% {
    transform: rotate(45deg) translateY(0);
  }

  50% {
    transform: rotate(50deg) translateY(-8px);
  }
}

@keyframes float-slow {

  0%,
  100% {
    transform: rotate(-30deg) translateY(0);
  }

  50% {
    transform: rotate(-35deg) translateY(10px);
  }
}

.animate-float {
  animation: float 5s ease-in-out infinite;
}

.animate-float-slow {
  animation: float-slow 7s ease-in-out infinite;
}

/* 响应式 */
@media (max-width: 1024px) {
  .hero-container {
    flex-direction: column;
    text-align: center;
    gap: 40px;
  }

  .hero-content {
    align-items: center;
  }

  .main-title {
    font-size: 48px;
    white-space: normal !important;
  }

  .nature-card:nth-child(2) {
    margin-top: 0;
  }

  .split-section,
  .photo-wall-section {
    flex-direction: column;
    padding: 40px 20px;
  }

  .features-list {
    grid-template-columns: 1fr;
  }

  .photo-masonry {
    grid-template-columns: 1fr;
    grid-template-rows: auto;
  }

  .photo-item {
    grid-column: span 1 !important;
    grid-row: span 1 !important;
    height: 320px;
  }
}

@media (max-width: 768px) {
  .main-title {
    font-size: 32px;
  }

  .quote-title {
    font-size: 28px;
  }

  .section-title {
    font-size: 28px;
  }
}
</style>
