<template>
  <header class="app-header">
    <div class="container">
      <div class="header-content">
        <!-- Logo -->
        <router-link to="/" class="logo">
          <span class="logo-icon">🐾</span>
          <span class="logo-text">笑猫の窝</span>
        </router-link>

        <!-- 导航菜单 -->
        <nav class="nav-menu">
          <router-link
            v-for="item in menuItems"
            :key="item.path"
            :to="item.path"
            class="nav-link"
            active-class="nav-link--active"
          >
            <span class="nav-icon">{{ item.icon }}</span>
            <span>{{ item.label }}</span>
          </router-link>
        </nav>

        <!-- 右侧操作区 -->
        <div class="header-actions">
          <template v-if="!isLoggedIn">
            <BaseButton variant="text" size="sm" @click="handleLogin">
              登录
            </BaseButton>
            <BaseButton variant="primary" size="sm" @click="handleRegister">
              注册
            </BaseButton>
          </template>
          <template v-else>
            <div class="user-info">
              <span class="welcome-text">欢迎，{{ userName }}</span>
              <BaseButton variant="text" size="sm" @click="handleLogout">
                退出
              </BaseButton>
            </div>
          </template>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import BaseButton from '@/components/base/BaseButton.vue'

const router = useRouter()
const authStore = useAuthStore()

// 导航菜单项
const menuItems = ref([
  { path: '/', label: '首页' },
  { path: '/activities', label: '宠物日记' },
  { path: '/moments', label: '宠物社区' },
  { path: '/about', label: '关于我们' }
])

// 计算属性
const isLoggedIn = computed(() => authStore.isAuthenticated)
const userName = computed(() => authStore.userName)

// 方法
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
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--color-gray-200);
  box-shadow: var(--shadow-sm);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72px;
  gap: var(--spacing-8);
}

/* ===== Logo ===== */
.logo {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  font-weight: var(--font-bold);
  font-size: var(--text-xl);
  color: var(--color-gray-900);
  text-decoration: none;
  transition: all var(--duration-base) var(--ease-in-out);
}

.logo:hover {
  transform: scale(1.05);
}

.logo-icon {
  font-size: var(--text-3xl);
}

.logo-text {
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* ===== 导航菜单 ===== */
.nav-menu {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  flex: 1;
  justify-content: center;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  padding: var(--spacing-3) var(--spacing-5);
  font-size: var(--text-base);
  font-weight: var(--font-medium);
  color: var(--color-gray-600);
  text-decoration: none;
  border-radius: var(--radius-lg);
  transition: all var(--duration-base) var(--ease-in-out);
}

.nav-link:hover {
  background-color: var(--color-gray-100);
  color: var(--color-gray-900);
}

.nav-link--active {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  color: var(--color-primary);
}

.nav-icon {
  font-size: var(--text-lg);
}

/* ===== 操作按钮 ===== */
.header-actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
}

.welcome-text {
  font-size: var(--text-sm);
  color: var(--color-gray-600);
  font-weight: var(--font-medium);
}

/* ===== 响应式设计 ===== */
@media (max-width: 768px) {
  .nav-menu {
    display: none;
  }

  .logo-text {
    display: none;
  }
}
</style>