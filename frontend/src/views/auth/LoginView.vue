<!--
  文件位置: src/views/auth/LoginView.vue
  登录页面
-->

<template>
  <div class="login-view">
    <div class="login-container">
      <BaseCard shadow="lg" class="login-card">
        <div class="login-header">
          <h1 class="login-title">登录</h1>
          <p class="login-subtitle">欢迎回到宠物护理生态系统</p>
        </div>

        <form @submit.prevent="handleLogin" class="login-form">
          <div class="form-group">
            <label for="email">邮箱</label>
            <BaseInput
              id="email"
              v-model="form.email"
              type="email"
              placeholder="请输入邮箱"
              required
              size="lg"
            />
          </div>

          <div class="form-group">
            <label for="password">密码</label>
            <BaseInput
              id="password"
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              required
              size="lg"
              show-count
              :maxlength="50"
            />
          </div>

          <div class="form-actions">
            <BaseButton
              type="submit"
              variant="primary"
              size="lg"
              :loading="loading"
              block
            >
              登录
            </BaseButton>
          </div>
        </form>

        <div class="login-footer">
          <p>
            还没有账号？
            <router-link to="/register" class="link">立即注册</router-link>
          </p>
        </div>
      </BaseCard>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore, useAppStore } from '@/stores'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseInput from '@/components/base/BaseInput.vue'
import BaseButton from '@/components/base/BaseButton.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()

const loading = ref(false)
const form = reactive({
  email: '',
  password: ''
})

const handleLogin = async () => {
  if (!form.email || !form.password) {
    appStore.showNotification('请填写完整的登录信息', 'warning')
    return
  }

  loading.value = true

  try {
    await userStore.login({
      email: form.email,
      password: form.password
    })

    appStore.showNotification('登录成功', 'success')

    // 跳转到目标页面或默认页面
    const redirect = route.query.redirect || '/space'
    router.push(redirect)

  } catch (error) {
    appStore.showNotification(error.message || '登录失败', 'error')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-view {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: var(--spacing-4);
}

.login-container {
  width: 100%;
  max-width: 400px;
}

.login-card {
  padding: var(--spacing-8);
}

.login-header {
  text-align: center;
  margin-bottom: var(--spacing-8);
}

.login-title {
  font-size: var(--text-3xl);
  font-weight: var(--font-bold);
  color: var(--color-gray-900);
  margin-bottom: var(--spacing-2);
}

.login-subtitle {
  font-size: var(--text-base);
  color: var(--color-gray-600);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-6);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2);
}

.form-group label {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-gray-700);
}

.form-actions {
  margin-top: var(--spacing-4);
}

.login-footer {
  margin-top: var(--spacing-6);
  text-align: center;
  font-size: var(--text-sm);
  color: var(--color-gray-600);
}

.link {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: var(--font-medium);
}

.link:hover {
  text-decoration: underline;
}
</style>