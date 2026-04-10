<template>
  <header class="app-header" @mouseleave="handleNavLeave">
    <div class="container header-container">
      <!-- Top Row: Logo, Search, Actions -->
      <div class="header-top">
        <div class="logo-and-weather">
          <router-link to="/" class="logo">
            <span class="logo-text">笑猫の窝</span>
          </router-link>
          <!-- 贴心的宠物天气微服务面板 -->
          <div class="weather-widget" title="当前天气与遛狗指数">
            <span class="weather-icon">☀️</span>
            <div class="weather-details">
              <span class="w-temp">24℃</span>
              <span class="w-desc">极度适宜遛狗</span>
            </div>
          </div>
        </div>

        <div class="search-wrapper">
          <div class="search-box">
            <svg class="search-icon" xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
            <input
              v-model="searchQuery"
              type="text"
              class="search-input"
              placeholder="搜索功能、动态、医院..."
              @keyup.enter="handleSearch"
            >
          </div>
        </div>

        <div class="header-actions">
          <template v-if="!isLoggedIn">
            <!-- text-btn 悬浮变成蓝色，与要求一致 -->
            <button class="action-btn text-btn" @click="handleLogin">登录</button>
            <button class="action-btn outline-btn" @click="handleRegister">注册</button>
          </template>
          <template v-else>
            <div class="user-info">
              <!-- 通知中心 -->
              <el-dropdown trigger="hover" placement="bottom-end">
                <div class="notification-bell">
                  <svg class="bell-icon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg>
                  <span class="bell-badge"></span>
                </div>
                <template #dropdown>
                  <el-dropdown-menu class="apple-dropdown notification-panel">
                    <div class="notif-header">系统通知</div>
                    <el-dropdown-item divided>
                      <div class="notif-item">
                        <span class="dot blue"></span>
                        <div class="notif-text">疫苗提醒：下周三记得打狂犬疫苗啦</div>
                      </div>
                    </el-dropdown-item>
                    <el-dropdown-item>
                      <div class="notif-item">
                        <span class="dot green"></span>
                        <div class="notif-text">社区互动：有 3 人刚刚赞了您的动态</div>
                      </div>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>

              <el-dropdown trigger="hover" @command="handleCommand">
                <div class="avatar-wrapper flex items-center cursor-pointer">
                  <el-avatar :size="36" :src="userAvatar" class="header-avatar">
                    {{ userName.charAt(0) }}
                  </el-avatar>
                </div>
                <template #dropdown>
                  <el-dropdown-menu class="apple-dropdown">
                    <el-dropdown-item disabled class="dropdown-username">{{ userName }}</el-dropdown-item>
                    <el-dropdown-item divided command="settings">账号设置</el-dropdown-item>
                    <el-dropdown-item command="logout" class="danger-text">退出登录</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </div>
      </div>

      <!-- Bottom Row: Navigation Links -->
      <nav class="nav-menu">
        <div 
          class="nav-item-wrapper"
          v-for="item in menuItems" 
          :key="item.id"
          @mouseenter="handleNavEnter(item.id)"
        >
          <div
            class="nav-link"
            :class="{ 'nav-link--active': $route.path === item.path }"
            @click="router.push(item.path)"
          >
            {{ item.label }}
          </div>

          <!-- 悬浮高级下拉菜单 Mega Menu (Apple 风格) -->
          <transition name="mega-fade">
            <div 
              class="mega-menu" 
              v-if="activeMenu === item.id && item.subItems && item.subItems.length > 0"
            >
              <div class="mega-menu-content">
                <div 
                  class="mega-item" 
                  v-for="(sub, index) in item.subItems" 
                  :key="index"
                  @click.stop="router.push(sub.path || item.path)"
                >
                  <div class="mega-item-icon" v-html="sub.svg"></div>
                  <div class="mega-item-text">
                    <h4 class="mega-item-title">{{ sub.label }}</h4>
                    <p class="mega-item-desc">{{ sub.desc }}</p>
                  </div>
                </div>
              </div>
            </div>
          </transition>
        </div>
      </nav>
    </div>
  </header>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const searchQuery = ref('')
const activeMenu = ref(null)

const userAvatar = computed(() => authStore.avatar || '')

const handleCommand = async (cmd) => {
  if (cmd === 'logout') {
    await handleLogout()
  } else if (cmd === 'settings') {
    router.push('/settings')
  }
}

const handleNavEnter = (id) => {
  activeMenu.value = id
}
const handleNavLeave = () => {
  activeMenu.value = null
}

// 通用低饱和度高级SVG图标
const svgDocument = `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>`
const svgHeartRate = `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M22 12h-4l-3 9L9 3l-3 9H2"></path></svg>`
const svgCamera = `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path><circle cx="12" cy="13" r="4"></circle></svg>`
const svgTrending = `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline><polyline points="17 6 23 6 23 12"></polyline></svg>`
const svgGift = `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 12 20 22 4 22 4 12"></polyline><rect x="2" y="7" width="20" height="5"></rect><line x1="12" y1="22" x2="12" y2="7"></line><path d="M12 7H7.5a2.5 2.5 0 0 1 0-5C11 2 12 7 12 7z"></path><path d="M12 7h4.5a2.5 2.5 0 0 0 0-5C13 2 12 7 12 7z"></path></svg>`
const svgUserGroup = `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>`
const svgMapMarker = `<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path><circle cx="12" cy="10" r="3"></circle></svg>`

// 导航菜单项 (移除 emoji, 增加极其高雅纯白的 Mega Dropdown 数据结构)
const menuItems = ref([
  { 
    id: 'home',
    path: '/', 
    label: '首页',
    subItems: []
  },
  { 
    id: 'activities',
    path: '/activities', 
    label: '宠物日记',
    subItems: [
      { label: '日常打卡', desc: '记录每一次洗澡与散步。', svg: svgDocument },
      { label: '健康档案', desc: '跟踪体重与疫苗记录。', svg: svgHeartRate },
      { label: '成长相片', desc: '汇总爱宠的时光碎片。', svg: svgCamera }
    ]
  },
  { 
    id: 'map',
    path: '/map', 
    label: '探索周边',
    subItems: [
      { label: '机构概览', desc: '查阅身边的优质宠物医院和门店。', svg: svgMapMarker }
    ]
  },
  { 
    id: 'moments',
    path: '/moments', 
    label: '宠物社区',
    subItems: [
      { label: '全站热榜', desc: '探索宠物圈今日最热内容。', svg: svgTrending },
      { label: '好物推荐', desc: '真实用户的饮食测评。', svg: svgGift },
      { label: '达人分享', desc: '与优秀的同好建立联系。', svg: svgUserGroup }
    ]
  },
  { 
    id: 'about',
    path: '/about', 
    label: '关于我们',
    subItems: []
  }
])

// 计算属性
const isLoggedIn = computed(() => authStore.isAuthenticated)
const userName = computed(() => authStore.userName)

// 方法
const handleSearch = () => {
  if (!searchQuery.value.trim()) return
  alert(`搜索功能开发中，搜索词：${searchQuery.value}`)
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
.app-header {
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
  /* 稍微加深一点的高级蓝毛玻璃背景 */
  background: rgba(226, 239, 255, 0.9);
  backdrop-filter: saturate(180%) blur(20px);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  border-bottom: 1px solid rgba(0, 113, 227, 0.2); 
  box-sizing: border-box;
}

.header-container {
  max-width: 980px; 
  margin: 0 auto;
  padding: 0 22px;
  display: flex;
  flex-direction: column;
}

/* 顶部：Logo、搜索、登录注册 */
.header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
}

.logo-and-weather {
  display: flex;
  align-items: center;
  gap: 20px;
}

.logo {
  display: flex;
  align-items: center;
  text-decoration: none;
}

.logo-text {
  font-size: 24px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: #1d1d1f; 
}

/* 天气挂件 */
.weather-widget {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.6);
  padding: 6px 12px;
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
  transition: transform 0.2s ease, background 0.2s;
  cursor: pointer;
}

.weather-widget:hover {
  transform: translateY(-2px);
  background: rgba(255, 255, 255, 0.9);
}

.weather-icon {
  font-size: 16px;
  filter: drop-shadow(0 1px 2px rgba(255, 165, 0, 0.3));
}

.weather-details {
  display: flex;
  flex-direction: column;
}

.w-temp {
  font-size: 13px;
  font-weight: 700;
  color: #1d1d1f;
  line-height: 1;
}

.w-desc {
  font-size: 10px;
  color: #86868b;
  font-weight: 500;
  margin-top: 2px;
}

/* 搜索框居中 */
.search-wrapper {
  flex: 1;
  display: flex;
  justify-content: center;
  padding: 0 40px;
}

.search-box {
  display: flex;
  align-items: center;
  background-color: rgba(0, 0, 0, 0.04);
  border-radius: 8px; /* 柔和的微圆角 */
  padding: 0 16px;
  width: 100%;
  max-width: 500px;
  height: 40px;
  transition: background-color 0.2s ease, transform 0.2s ease;
}

.search-box:focus-within {
  background-color: rgba(0, 0, 0, 0.08); /* 焦点稍微加深 */
  /* Apple 喜欢微动效凸显高级感 */
  transform: scale(1.02);
}

.search-icon {
  color: #86868b;
  margin-right: 8px;
}

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 15px;
  color: #1d1d1f;
  outline: none;
}

.search-input::placeholder {
  color: #86868b;
}

/* 右侧操作区 */
.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.action-btn {
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  background: transparent;
  padding: 8px 20px;
  border-radius: 980px; /* 全圆角 */
  transition: transform 0.2s cubic-bezier(0.16, 1, 0.3, 1), color 0.2s ease, background-color 0.2s ease;
}

/* Apple 常见的次要操作文字变蓝，加上放大效果，呈现高级感 */
.text-btn {
  color: #1d1d1f;
}

.text-btn:hover {
  color: #0071e3;
  transform: scale(1.05); /* 悬浮放大 */
}

/* 带边框的登录/注册，更具质感 */
.outline-btn {
  color: #1d1d1f;
  box-shadow: 0 0 0 1px rgba(0,0,0,0.1) inset;
}

.outline-btn:hover {
  color: #0071e3;
  box-shadow: 0 0 0 1px #0071e3 inset; /* 边框也同步变蓝 */
  transform: scale(1.05);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: transform 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}
.avatar-wrapper:hover {
  transform: scale(1.05);
}
.header-avatar {
  border: 1px solid rgba(0,0,0,0.1);
  background-color: #f5f5f7;
  color: #1d1d1f;
  font-weight: 500;
}

/* 铃铛通知 */
.notification-bell {
  position: relative;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #1d1d1f;
  background: rgba(0, 0, 0, 0.03);
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}
.notification-bell:hover {
  background: rgba(0, 0, 0, 0.08);
  transform: scale(1.05);
  color: #0071e3;
}
.bell-badge {
  position: absolute;
  top: 6px;
  right: 8px;
  width: 8px;
  height: 8px;
  background: #ff3b30;
  border-radius: 50%;
  border: 2px solid white;
  animation: pulse-red 2s infinite;
}
@keyframes pulse-red {
  0% { box-shadow: 0 0 0 0 rgba(255, 59, 48, 0.7); }
  70% { box-shadow: 0 0 0 6px rgba(255, 59, 48, 0); }
  100% { box-shadow: 0 0 0 0 rgba(255, 59, 48, 0); }
}

.dropdown-username {
  font-weight: 600;
  color: #1d1d1f !important;
  font-size: 14px;
}
.apple-dropdown {
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
  border: none;
  min-width: 140px;
}
.danger-text {
  color: #ff3b30 !important;
}

/* 铃铛下拉通知面板 */
.notification-panel {
  min-width: 260px;
  padding: 8px;
}
.notif-header {
  padding: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #86868b;
  border-bottom: 1px solid rgba(0,0,0,0.05);
  margin-bottom: 4px;
}
.notif-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 4px 2px;
  white-space: normal;
}
.notif-item .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 5px;
  flex-shrink: 0;
}
.notif-item .dot.blue { background: #0071e3; }
.notif-item .dot.green { background: #34c759; }
.notif-item .notif-text {
  font-size: 13px;
  color: #1d1d1f;
  line-height: 1.4;
}

.welcome-text {
  font-size: 14px;
  color: #86868b;
}

/* 底部功能导航 (带悬浮交互) */
.nav-menu {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 48px;
  height: 60px;
  position: relative; 
}

.nav-item-wrapper {
  position: relative; /* 为下方 Mega Menu 定位 */
  height: 100%;
  display: flex;
  align-items: center;
}

.nav-link {
  font-size: 17px; /* 增大了字体 */
  font-weight: 400;
  letter-spacing: -0.01em;
  color: #1d1d1f;
  text-decoration: none;
  opacity: 0.8;
  /* 增加悬浮变蓝和放大 */
  transition: opacity 0.2s ease, color 0.2s ease, transform 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  transform-origin: center bottom;
}

.nav-item-wrapper:hover .nav-link {
  opacity: 1;
  color: #0071e3;
  transform: scale(1.08); /* 触发放大 */
}

.nav-link--active {
  opacity: 1;
  font-weight: 500;
}

/* ===== Mega Menu Apple 高级面板特效 ===== */
.mega-menu {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  width: 380px;
  /* 非常高端的毛玻璃白板效果 */
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: saturate(180%) blur(30px);
  -webkit-backdrop-filter: saturate(180%) blur(30px);
  border-radius: 18px;
  /* Apple式高层级弥散阴影 */
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08), 0 0 0 1px rgba(0,0,0,0.02);
  padding: 16px;
  z-index: 999;
  margin-top: 4px;
  /* 让鼠标平滑划入时不中断 */
  cursor: default;
}

.mega-menu-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.mega-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 12px 14px;
  border-radius: 12px;
  cursor: pointer;
  transition: background-color 0.2s ease, transform 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.mega-item:hover {
  background-color: rgba(0, 113, 227, 0.06); /* 背景出现极其微妙的淡蓝色 */
  transform: translateX(4px); /* 高级微位移侧滑反馈 */
}

.mega-item-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0071e3; /* 图标同样使用主蓝色 */
  background: rgba(0, 113, 227, 0.1);
  border-radius: 8px;
  flex-shrink: 0;
}

.mega-item-text {
  flex: 1;
}

.mega-item-title {
  font-size: 14px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 4px 0;
  transition: color 0.2s ease;
}

.mega-item:hover .mega-item-title {
  color: #0071e3; /* Hover时标题也会变蓝关联感 */
}

.mega-item-desc {
  font-size: 12px;
  color: #86868b;
  margin: 0;
  line-height: 1.4;
}

/* Mega Menu 弹簧进出场动画（Apple式舒缓弹性） */
.mega-fade-enter-active,
.mega-fade-leave-active {
  transition: opacity 0.35s ease, transform 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}
.mega-fade-enter-from,
.mega-fade-leave-to {
  opacity: 0;
  transform: translate(-50%, -10px) scale(0.98); /* 微微上移缩小退场 */
}

/* ===== 响应式设计 ===== */
@media (max-width: 768px) {
  .search-wrapper {
    display: none; 
  }
  .nav-menu {
    gap: 16px;
    overflow-x: auto;
    justify-content: flex-start;
  }
  .mega-menu {
    display: none; /* 移动端暂且隐藏悬浮面板，也可做抽屉形态 */
  }
}
</style>