<template>
  <div class="register-form">
    <div class="form-header">
      <h2>注册账号</h2>
      <p class="subtitle">加入笑猫の窝，开启宠物护理之旅</p>
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
          @input="validateField('name')"
          :disabled="loading"
        />
        <span v-if="errors.name" class="error-message">{{ errors.name }}</span>
        <div class="field-hint">用户名只能包含字母、数字、下划线、短横线和中文</div>
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
            @input="validateField('password')"
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
        <div class="field-hint">密码长度必须在1-100个字符之间</div>
      </div>

      <!-- 确认密码 -->
      <div class="form-group">
        <label for="confirmPassword" class="form-label">
          <i class="icon">🔐</i>
          确认密码
        </label>
        <div class="password-input">
          <input
            id="confirmPassword"
            v-model="formData.confirmPassword"
            :type="showConfirmPassword ? 'text' : 'password'"
            class="form-input"
            :class="{ 'error': errors.confirmPassword }"
            placeholder="请再次输入密码"
            @blur="validateField('confirmPassword')"
            @input="validateField('confirmPassword')"
            :disabled="loading"
          />
          <button
            type="button"
            class="password-toggle"
            @click="showConfirmPassword = !showConfirmPassword"
            :disabled="loading"
          >
            {{ showConfirmPassword ? '👁️' : '🙈' }}
          </button>
        </div>
        <span v-if="errors.confirmPassword" class="error-message">{{ errors.confirmPassword }}</span>
      </div>

      <!-- 服务条款 -->
      <div class="form-group">
        <label class="checkbox-label">
          <input
            v-model="formData.agreed"
            type="checkbox"
            :disabled="loading"
          />
          <span class="checkmark"></span>
          我已阅读并同意
          <a href="#" class="terms-link" @click.prevent="showTerms">用户协议</a>
          和
          <a href="#" class="terms-link" @click.prevent="showPrivacy">隐私政策</a>
        </label>
        <span v-if="errors.agreed" class="error-message">{{ errors.agreed }}</span>
      </div>

      <!-- 提交按钮 -->
      <button
        type="submit"
        class="submit-btn"
        :disabled="loading || !isFormValid"
      >
        <span v-if="loading" class="loading-spinner">⏳</span>
        <span v-else>注册</span>
      </button>

      <!-- 错误信息 -->
      <div v-if="submitError" class="submit-error">
        <i class="icon">❌</i>
        {{ submitError }}
      </div>
    </form>

    <!-- 切换到登录 -->
    <div class="form-footer">
      <p>
        已有账号？
        <a href="#" class="switch-link" @click.prevent="$emit('switch-to-login')">
          立即登录
        </a>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { register as registerApi } from '@/api/auth'
import { showSuccess, showError, showInfo } from '@/utils/message'

// 定义事件
const emit = defineEmits(['switch-to-login', 'register-success'])

// 路由
const router = useRouter()

// 表单数据
const formData = reactive({
  name: '',
  password: '',
  confirmPassword: '',
  agreed: false
})

// 错误信息
const errors = reactive({
  name: '',
  password: '',
  confirmPassword: '',
  agreed: ''
})

// 状态
const loading = ref(false)
const showPassword = ref(false)
const showConfirmPassword = ref(false)
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

      // 如果确认密码已填写，同时验证确认密码
      if (formData.confirmPassword) {
        validateField('confirmPassword')
      }
      break

    case 'confirmPassword':
      if (!formData.confirmPassword) {
        errors.confirmPassword = '请确认密码'
      } else if (formData.password !== formData.confirmPassword) {
        errors.confirmPassword = '两次输入的密码不一致'
      } else {
        errors.confirmPassword = ''
      }
      break

    case 'agreed':
      if (!formData.agreed) {
        errors.agreed = '请阅读并同意用户协议和隐私政策'
      } else {
        errors.agreed = ''
      }
      break
  }
}

// 表单是否有效
const isFormValid = computed(() => {
  return formData.name.trim() &&
         formData.password &&
         formData.confirmPassword &&
         formData.agreed &&
         formData.password === formData.confirmPassword &&
         !errors.name &&
         !errors.password &&
         !errors.confirmPassword &&
         !errors.agreed
})

// 处理表单提交
const handleSubmit = async () => {
  // 验证所有字段
  validateField('name')
  validateField('password')
  validateField('confirmPassword')
  validateField('agreed')

  if (!isFormValid.value) {
    showError('请完整填写注册信息')
    return
  }

  loading.value = true
  submitError.value = ''

  try {
    const registerData = {
      name: formData.name.trim(),
      password: formData.password
    }

    // 调用注册API
    const response = await registerApi(registerData)

    // 注册成功
    showSuccess('注册成功！请登录您的账号')

    // 触发成功事件
    emit('register-success', response.data)

    // 切换到登录表单
    emit('switch-to-login')

  } catch (error) {
    console.error('注册失败:', error)
    submitError.value = error.message || '注册失败，请检查输入信息'
    showError(submitError.value)
  } finally {
    loading.value = false
  }
}

// 显示用户协议
const showTerms = () => {
  showInfo('用户协议功能正在开发中')
}

// 显示隐私政策
const showPrivacy = () => {
  showInfo('隐私政策功能正在开发中')
}
</script>

<style scoped>
.register-form {
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

.field-hint {
  color: var(--color-gray-500);
  font-size: var(--text-xs);
  margin-top: var(--spacing-1);
}

.checkbox-label {
  display: flex;
  align-items: flex-start;
  cursor: pointer;
  font-size: var(--text-sm);
  color: var(--color-gray-700);
  line-height: 1.5;
}

.checkbox-label input[type="checkbox"] {
  margin-right: var(--spacing-2);
  margin-top: 2px;
}

.terms-link {
  color: var(--color-primary);
  text-decoration: none;
  margin: 0 2px;
}

.terms-link:hover {
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
  .register-form {
    padding: var(--spacing-6);
    margin: var(--spacing-4);
  }
}
</style>