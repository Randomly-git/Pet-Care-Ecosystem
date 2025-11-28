<template>
  <div class="login-form">
    <div class="form-header">
      <h2>登录账号</h2>
      <p class="subtitle">欢迎回到笑猫の窝</p>
    </div>

    <form @submit.prevent="handleSubmit" class="auth-form">
      <!-- 用户名输入 -->
      <div class="form-group">
        <label for="username" class="form-label">
          <i class="icon">👤</i>
          用户名
        </label>
        <input
          id="username"
          v-model="formData.name"
          type="text"
          class="form-input"
          :class="{ 'error': errors.name }"
          placeholder="请输入用户名"
          @blur="validateField('name')"
          :disabled="loading"
        />
        <span v-if="errors.name" class="error-message">{{ errors.name }}</span>
      </div>

      <!-- 密码输入 -->
      <div class="form-group">
        <label for="password" class="form-label">
          <i class="icon">🔒</i>
          密码
        </label>
        <div class="password-input">
          <input
            id="password"
            v-model="formData.password"
            :type="showPassword ? 'text' : 'password'"
            class="form-input"
            :class="{ 'error': errors.password }"
            placeholder="请输入密码"
            @blur="validateField('password')"
            :disabled="loading"
          />
          <button
            type="button"
            class="password-toggle"
            @click="showPassword = !showPassword"
            :disabled="loading"
          >
            {{ showPassword ? '👁️' : '🙈' }}
          </button>
        </div>
        <span v-if="errors.password" class="error-message">{{ errors.password }}</span>
      </div>

      <!-- 记住我 -->
      <div class="form-options">
        <label class="checkbox-label">
          <input
            v-model="formData.remember"
            type="checkbox"
            :disabled="loading"
          />
          <span class="checkmark"></span>
          记住我
        </label>
        <a href="#" class="forgot-password" @click.prevent="handleForgotPassword">
          忘记密码？
        </a>
      </div>

      <!-- 提交按钮 -->
      <button
        type="submit"
        class="submit-btn"
        :disabled="loading || !isFormValid"
      >
        <span v-if="loading" class="loading-spinner">⏳</span>
        <span v-else>登录</span>
      </button>

      <!-- 错误信息 -->
      <div v-if="submitError" class="submit-error">
        <i class="icon">❌</i>
        {{ submitError }}
      </div>
    </form>

    <!-- 切换到注册 -->
    <div class="form-footer">
      <p>
        还没有账号？
        <a href="#" class="switch-link" @click.prevent="$emit('switch-to-register')">
          立即注册
        </a>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { showError } from '@/utils/message'

// 定义事件
const emit = defineEmits(['switch-to-register', 'login-success'])

// 路由和状态管理
const router = useRouter()
const authStore = useAuthStore()

// 表单数据
const formData = reactive({
  name: '',
  password: '',
  remember: false
})

// 错误信息
const errors = reactive({
  name: '',
  password: ''
})

// 状态
const loading = computed(() => authStore.loading)
const showPassword = ref(false)
const submitError = ref('')

// 表单验证
const validateField = (field) => {
  submitError.value = ''

  switch (field) {
    case 'name':
      if (!formData.name.trim()) {
        errors.name = '用户名不能为空'
      } else if (formData.name.length < 1 || formData.name.length > 50) {
        errors.name = '用户名长度必须在1-50个字符之间'
      } else if (!/^[a-zA-Z0-9_\-一-龥]+$/.test(formData.name)) {
        errors.name = '用户名只能包含字母、数字、下划线、短横线和中文'
      } else {
        errors.name = ''
      }
      break

    case 'password':
      if (!formData.password) {
        errors.password = '密码不能为空'
      } else if (formData.password.length < 1 || formData.password.length > 100) {
        errors.password = '密码长度必须在1-100个字符之间'
      } else {
        errors.password = ''
      }
      break
  }
}

// 表单是否有效
const isFormValid = computed(() => {
  return formData.name.trim() &&
         formData.password &&
         !errors.name &&
         !errors.password
})

// 处理表单提交
const handleSubmit = async () => {
  // 验证所有字段
  validateField('name')
  validateField('password')

  if (!isFormValid.value) {
    showError('请填写完整的登录信息')
    return
  }

  submitError.value = ''

  try {
    const loginData = {
      name: formData.name.trim(),
      password: formData.password
    }

    // 使用authStore进行登录
    await authStore.login(loginData)

    // 触发成功事件
    emit('login-success', authStore.user)

    // 跳转到主页
    router.push('/')

  } catch (error) {
    console.error('登录失败:', error)
    submitError.value = error.message || '登录失败，请检查用户名和密码'
    showError(submitError.value)
  }
}

// 忘记密码
const handleForgotPassword = () => {
  // TODO: 实现忘记密码功能
  showInfo('密码重置功能正在开发中，请联系管理员')
}
</script>

<style scoped>
.login-form {
  max-width: 400px;
  margin: 0 auto;
  padding: var(--spacing-8);
  background: var(--color-white);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
}

.form-header {
  text-align: center;
  margin-bottom: var(--spacing-8);
}

.form-header h2 {
  color: var(--color-gray-900);
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  margin-bottom: var(--spacing-2);
}

.subtitle {
  color: var(--color-gray-600);
  font-size: var(--text-base);
}

.auth-form {
  margin-bottom: var(--spacing-6);
}

.form-group {
  margin-bottom: var(--spacing-5);
}

.form-label {
  display: flex;
  align-items: center;
  font-weight: var(--font-medium);
  color: var(--color-gray-700);
  margin-bottom: var(--spacing-2);
  font-size: var(--text-sm);
}

.form-label .icon {
  margin-right: var(--spacing-2);
  font-size: var(--text-base);
}

.form-input {
  width: 100%;
  padding: var(--spacing-3) var(--spacing-4);
  border: 2px solid var(--color-gray-300);
  border-radius: var(--radius-md);
  font-size: var(--text-base);
  transition: all var(--duration-base);
  background: var(--color-white);
}

.form-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-input.error {
  border-color: var(--color-error);
}

.form-input:disabled {
  background-color: var(--color-gray-100);
  cursor: not-allowed;
}

.password-input {
  position: relative;
}

.password-toggle {
  position: absolute;
  right: var(--spacing-3);
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  font-size: var(--text-lg);
  opacity: 0.6;
  transition: opacity var(--duration-base);
}

.password-toggle:hover {
  opacity: 1;
}

.error-message {
  color: var(--color-error);
  font-size: var(--text-sm);
  margin-top: var(--spacing-1);
  display: block;
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-6);
}

.checkbox-label {
  display: flex;
  align-items: center;
  cursor: pointer;
  font-size: var(--text-sm);
  color: var(--color-gray-700);
}

.checkbox-label input[type="checkbox"] {
  margin-right: var(--spacing-2);
}

.forgot-password {
  color: var(--color-primary);
  text-decoration: none;
  font-size: var(--text-sm);
  transition: color var(--duration-base);
}

.forgot-password:hover {
  color: var(--color-primary-dark);
  text-decoration: underline;
}

.submit-btn {
  width: 100%;
  padding: var(--spacing-4);
  background: var(--color-primary);
  color: var(--color-white);
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  cursor: pointer;
  transition: all var(--duration-base);
  display: flex;
  align-items: center;
  justify-content: center;
}

.submit-btn:hover:not(:disabled) {
  background: var(--color-primary-dark);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.submit-btn:disabled {
  background: var(--color-gray-400);
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.loading-spinner {
  margin-right: var(--spacing-2);
}

.submit-error {
  margin-top: var(--spacing-4);
  padding: var(--spacing-3);
  background: rgba(239, 68, 68, 0.1);
  color: var(--color-error);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  display: flex;
  align-items: center;
}

.submit-error .icon {
  margin-right: var(--spacing-2);
}

.form-footer {
  text-align: center;
  color: var(--color-gray-600);
  font-size: var(--text-sm);
}

.switch-link {
  color: var(--color-primary);
  text-decoration: none;
  font-weight: var(--font-medium);
  transition: color var(--duration-base);
}

.switch-link:hover {
  color: var(--color-primary-dark);
  text-decoration: underline;
}

@media (max-width: 640px) {
  .login-form {
    padding: var(--spacing-6);
    margin: var(--spacing-4);
  }

  .form-options {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-2);
  }
}
</style>