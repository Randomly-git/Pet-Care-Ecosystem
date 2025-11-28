<!--
  文件位置: src/components/base/ErrorBoundary.vue
  错误边界组件 - 捕获和处理组件错误
-->

<template>
  <div class="error-boundary">
    <!-- 正常内容 -->
    <slot v-if="!hasError" />

    <!-- 错误状态 -->
    <div v-else class="error-container">
      <div class="error-card">
        <div class="error-icon">😅</div>

        <h3 class="error-title">{{ errorTitle }}</h3>
        <p class="error-message">{{ errorMessage }}</p>

        <!-- 错误详情（开发模式） -->
        <details v-if="showDetails && errorDetails" class="error-details">
          <summary class="error-details-toggle">查看错误详情</summary>
          <pre class="error-content">{{ errorDetails }}</pre>
        </details>

        <!-- 操作按钮 -->
        <div class="error-actions">
          <button @click="retry" class="error-btn primary">
            🔄 重试
          </button>
          <button @click="goHome" class="error-btn secondary">
            🏠 回到首页
          </button>
          <button v-if="showDetails" @click="reportError" class="error-btn outline">
            📧 反馈问题
          </button>
        </div>
      </div>

      <!-- 错误动画背景 -->
      <div class="error-background">
        <div class="floating-shapes">
          <div class="shape shape-1">❌</div>
          <div class="shape shape-2">⚠️</div>
          <div class="shape shape-3">🐛</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onErrorCaptured } from 'vue'

const props = defineProps({
  fallbackTitle: {
    type: String,
    default: '出错了'
  },
  fallbackMessage: {
    type: String,
    default: '应用遇到了一些问题，请稍后再试'
  },
  showDetails: {
    type: Boolean,
    default: import.meta.env.DEV
  }
})

const emit = defineEmits(['error', 'retry'])

// 错误状态
const hasError = ref(false)
const error = ref(null)

// 计算属性
const errorTitle = computed(() => {
  return error.value?.title || props.fallbackTitle
})

const errorMessage = computed(() => {
  if (error.value?.message) return error.value.message
  if (error.value?.status === 404) return '页面未找到'
  if (error.value?.status === 403) return '没有访问权限'
  if (error.value?.status === 500) return '服务器内部错误'
  if (error.value?.status === 'network') return '网络连接失败'
  return props.fallbackMessage
})

const errorDetails = computed(() => {
  if (!error.value) return null
  return JSON.stringify(error.value, null, 2)
})

// 捕获错误
onErrorCaptured((err, instance, info) => {
  console.error('ErrorBoundary捕获到错误:', { err, instance, info })

  hasError.value = true
  error.value = {
    message: err.message,
    stack: err.stack,
    component: instance?.$options?.name,
    info,
    timestamp: new Date().toISOString()
  }

  emit('error', error.value)

  // 阻止错误继续传播
  return false
})

// 方法
const retry = () => {
  hasError.value = false
  error.value = null
  emit('retry')
}

const goHome = () => {
  window.location.href = '/'
}

const reportError = () => {
  const subject = `应用错误报告 - ${new Date().toLocaleDateString()}`
  const body = `
错误信息: ${error.value.message}
组件: ${error.value.component}
详情: ${error.value.info}
时间: ${error.value.timestamp}

堆栈信息:
${error.value.stack}
  `.trim()

  window.location.href = `mailto:support@petcare.com?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`
}
</script>

<style scoped>
.error-boundary {
  position: relative;
  width: 100%;
  height: 100%;
}

/* ===== 错误容器 ===== */
.error-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  z-index: 9999;
  padding: var(--spacing-4);
}

.error-card {
  background: white;
  border-radius: var(--radius-xl);
  padding: var(--spacing-8);
  box-shadow: var(--shadow-2xl);
  max-width: 500px;
  width: 100%;
  text-align: center;
  position: relative;
  z-index: 2;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.error-icon {
  font-size: 4rem;
  margin-bottom: var(--spacing-4);
  animation: bounce 2s infinite;
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-10px);
  }
  60% {
    transform: translateY(-5px);
  }
}

.error-title {
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--color-gray-900);
  margin-bottom: var(--spacing-2);
}

.error-message {
  font-size: var(--text-base);
  color: var(--color-gray-600);
  line-height: 1.6;
  margin-bottom: var(--spacing-6);
}

/* ===== 错误详情 ===== */
.error-details {
  text-align: left;
  margin-bottom: var(--spacing-6);
}

.error-details-toggle {
  cursor: pointer;
  color: var(--color-primary);
  font-size: var(--text-sm);
  margin-bottom: var(--spacing-2);
  transition: color var(--duration-base);
}

.error-details-toggle:hover {
  color: var(--color-primary-dark);
}

.error-content {
  background: var(--color-gray-100);
  padding: var(--spacing-3);
  border-radius: var(--radius-md);
  font-size: var(--text-xs);
  color: var(--color-gray-700);
  overflow-x: auto;
  max-height: 200px;
  overflow-y: auto;
}

/* ===== 操作按钮 ===== */
.error-actions {
  display: flex;
  gap: var(--spacing-3);
  flex-wrap: wrap;
  justify-content: center;
}

.error-btn {
  padding: var(--spacing-3) var(--spacing-6);
  border-radius: var(--radius-lg);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  transition: all var(--duration-base);
  cursor: pointer;
  border: none;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: var(--spacing-2);
}

.error-btn.primary {
  background: var(--color-primary);
  color: white;
}

.error-btn.primary:hover {
  background: var(--color-primary-dark);
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
}

.error-btn.secondary {
  background: var(--color-gray-100);
  color: var(--color-gray-700);
}

.error-btn.secondary:hover {
  background: var(--color-gray-200);
}

.error-btn.outline {
  background: transparent;
  color: var(--color-primary);
  border: 2px solid var(--color-primary);
}

.error-btn.outline:hover {
  background: var(--color-primary);
  color: white;
}

/* ===== 背景动画 ===== */
.error-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
  z-index: 1;
}

.floating-shapes {
  position: relative;
  width: 100%;
  height: 100%;
}

.shape {
  position: absolute;
  font-size: 2rem;
  animation: float 6s ease-in-out infinite;
  opacity: 0.1;
}

.shape-1 {
  top: 20%;
  left: 10%;
  animation-delay: 0s;
}

.shape-2 {
  top: 60%;
  right: 15%;
  animation-delay: 2s;
}

.shape-3 {
  bottom: 30%;
  left: 20%;
  animation-delay: 4s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(10deg);
  }
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .error-card {
    padding: var(--spacing-6);
    margin: var(--spacing-4);
  }

  .error-icon {
    font-size: 3rem;
  }

  .error-title {
    font-size: var(--text-xl);
  }

  .error-actions {
    flex-direction: column;
  }

  .error-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>