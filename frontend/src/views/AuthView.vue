<template>
  <div class="auth-view">
    <div class="auth-container">
      <!-- 应用logo和标题 -->
      <div class="auth-header">
        <div class="app-logo">
          <h1 class="logo-text">笑猫の窝</h1>
          <p class="logo-subtitle">您身边的宠物护理专家</p>
        </div>
      </div>

      <!-- 认证表单 -->
      <div class="auth-content">
        <transition name="slide-fade" mode="out-in">
          <LoginForm
            v-if="currentForm === 'login'"
            @switch-to-register="switchToRegister"
            @login-success="handleLoginSuccess"
            key="login"
          />
          <RegisterForm
            v-else
            @switch-to-login="switchToLogin"
            @register-success="handleRegisterSuccess"
            key="register"
          />
        </transition>
      </div>

      <!-- 装饰性元素 -->
      <div class="auth-decoration">
        <div class="decoration-item cat-paw">🐾</div>
        <div class="decoration-item cat-face">🐱</div>
        <div class="decoration-item heart">❤️</div>
        <div class="decoration-item bone">🦴</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import LoginForm from '@/components/auth/LoginForm.vue'
import RegisterForm from '@/components/auth/RegisterForm.vue'
import { showSuccess } from '@/utils/message'

// 路由和状态
const router = useRouter()
const authStore = useAuthStore()

// 当前表单状态
const currentForm = ref('login') // 'login' 或 'register'

// 切换到注册表单
const switchToRegister = () => {
  currentForm.value = 'register'
}

// 切换到登录表单
const switchToLogin = () => {
  currentForm.value = 'login'
}

// 处理登录成功
const handleLoginSuccess = (userData) => {
  console.log('登录成功:', userData)

  // 显示成功消息（authStore.login已经显示了成功消息）
  // showSuccess(`欢迎回来，${userData.name}！`)
}

// 处理注册成功
const handleRegisterSuccess = (userData) => {
  console.log('注册成功:', userData)

  // 显示成功消息（切换到登录表单后显示）
  setTimeout(() => {
    showSuccess(`注册成功！请登录您的账号`)
  }, 100)
}

// 组件挂载时检查是否已登录
onMounted(() => {
  // 如果已经登录，直接跳转到主页
  if (authStore.isAuthenticated) {
    router.push('/space')
  }
})
</script>

<style scoped>
.auth-view {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-4);
  position: relative;
  overflow: hidden;
}

.auth-container {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 500px;
}

.auth-header {
  text-align: center;
  margin-bottom: var(--spacing-8);
  animation: fadeInDown 0.8s ease-out;
}

.app-logo {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  padding: var(--spacing-6) var(--spacing-8);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
}

.logo-text {
  color: var(--color-primary);
  font-size: var(--text-4xl);
  font-weight: var(--font-bold);
  margin: 0 0 var(--spacing-2) 0;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
}

.logo-subtitle {
  color: var(--color-gray-600);
  font-size: var(--text-lg);
  margin: 0;
  font-weight: var(--font-medium);
}

.auth-content {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-xl);
  overflow: hidden;
  animation: fadeInUp 0.8s ease-out;
}

/* 装饰性元素 */
.auth-decoration {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  pointer-events: none;
  z-index: 1;
}

.decoration-item {
  position: absolute;
  font-size: var(--text-2xl);
  opacity: 0.3;
  animation: float 6s ease-in-out infinite;
}

.cat-paw {
  top: 10%;
  left: 10%;
  animation-delay: 0s;
}

.cat-face {
  top: 15%;
  right: 15%;
  font-size: var(--text-3xl);
  animation-delay: 1s;
}

.heart {
  bottom: 20%;
  left: 20%;
  animation-delay: 2s;
}

.bone {
  bottom: 25%;
  right: 10%;
  font-size: var(--text-xl);
  animation-delay: 3s;
}

/* 动画效果 */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
}

.slide-fade-enter-from {
  opacity: 0;
  transform: translateX(30px);
}

.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(-30px);
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

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
    transform: translateY(0px) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(10deg);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .auth-view {
    padding: var(--spacing-2);
  }

  .auth-container {
    max-width: 100%;
  }

  .logo-text {
    font-size: var(--text-3xl);
  }

  .logo-subtitle {
    font-size: var(--text-base);
  }

  .app-logo {
    padding: var(--spacing-4) var(--spacing-6);
  }

  .decoration-item {
    font-size: var(--text-lg);
  }

  .cat-face {
    font-size: var(--text-2xl);
  }
}

@media (max-width: 480px) {
  .auth-view {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    padding: var(--spacing-1);
  }

  .app-logo {
    padding: var(--spacing-3) var(--spacing-4);
    margin-bottom: var(--spacing-4);
  }

  .decoration-item {
    display: none; /* 在小屏幕上隐藏装饰元素 */
  }
}

/* 深色模式支持 */
@media (prefers-color-scheme: dark) {
  .app-logo {
    background: rgba(30, 41, 59, 0.95);
  }

  .auth-content {
    background: rgba(30, 41, 59, 0.95);
  }

  .logo-text {
    color: var(--color-primary-light);
  }

  .logo-subtitle {
    color: var(--color-gray-300);
  }
}
</style>