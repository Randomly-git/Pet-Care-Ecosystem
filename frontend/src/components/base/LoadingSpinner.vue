<!--
  文件位置: src/components/base/LoadingSpinner.vue
  通用加载状态组件
-->

<template>
  <div class="loading-container" :class="[size, { fullscreen }]">
    <!-- 骨架屏加载 -->
    <div v-if="type === 'skeleton'" class="skeleton-loading">
      <div class="skeleton-item" v-for="i in items" :key="i">
        <div class="skeleton-line title"></div>
        <div class="skeleton-line subtitle"></div>
        <div class="skeleton-line content" v-for="j in 3" :key="j"></div>
      </div>
    </div>

    <!-- 脉冲加载 -->
    <div v-else-if="type === 'pulse'" class="pulse-loading">
      <div class="pulse-dot" v-for="i in 3" :key="i"></div>
    </div>

    <!-- 圆环加载 -->
    <div v-else-if="type === 'ring'" class="ring-loading">
      <div class="ring"></div>
      <div v-if="text" class="loading-text">{{ text }}</div>
    </div>

    <!-- 默认旋转加载 -->
    <div v-else class="spinner-loading">
      <div class="spinner"></div>
      <div v-if="text" class="loading-text">{{ text }}</div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  type: {
    type: String,
    default: 'spinner', // spinner | skeleton | pulse | ring
    validator: (value) => ['spinner', 'skeleton', 'pulse', 'ring'].includes(value)
  },
  size: {
    type: String,
    default: 'medium', // small | medium | large
    validator: (value) => ['small', 'medium', 'large'].includes(value)
  },
  text: {
    type: String,
    default: ''
  },
  items: {
    type: Number,
    default: 3
  },
  fullscreen: {
    type: Boolean,
    default: false
  }
})
</script>

<style scoped>
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--spacing-4);
}

.loading-container.fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);
  z-index: 1000;
}

.loading-container.small {
  padding: var(--spacing-2);
}

.loading-container.large {
  padding: var(--spacing-8);
}

/* ===== 默认旋转加载 ===== */
.spinner-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-3);
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid var(--color-gray-200);
  border-top: 4px solid var(--color-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.loading-container.small .spinner {
  width: 24px;
  height: 24px;
  border-width: 3px;
}

.loading-container.large .spinner {
  width: 56px;
  height: 56px;
  border-width: 6px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* ===== 圆环加载 ===== */
.ring-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-3);
}

.ring {
  width: 60px;
  height: 60px;
  border: 3px solid var(--color-primary);
  border-radius: 50%;
  position: relative;
}

.ring::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 40px;
  height: 40px;
  background: white;
  border-radius: 50%;
}

.ring::after {
  content: '';
  position: absolute;
  top: -3px;
  left: 50%;
  transform: translateX(-50%);
  width: 12px;
  height: 12px;
  background: var(--color-primary);
  border-radius: 50%;
  animation: ringRotate 2s linear infinite;
}

@keyframes ringRotate {
  0% {
    transform: translateX(-50%) rotate(0deg) translateY(30px);
  }
  100% {
    transform: translateX(-50%) rotate(360deg) translateY(30px);
  }
}

/* ===== 脉冲加载 ===== */
.pulse-loading {
  display: flex;
  gap: var(--spacing-2);
}

.pulse-dot {
  width: 12px;
  height: 12px;
  background: var(--color-primary);
  border-radius: 50%;
  animation: pulse 1.5s ease-in-out infinite;
}

.pulse-dot:nth-child(2) {
  animation-delay: 0.2s;
}

.pulse-dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 0.7;
  }
  50% {
    transform: scale(1.3);
    opacity: 1;
  }
}

/* ===== 骨架屏加载 ===== */
.skeleton-loading {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4);
  width: 100%;
}

.skeleton-item {
  padding: var(--spacing-4);
  background: white;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.skeleton-line {
  height: 16px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-2);
}

.skeleton-line.title {
  width: 60%;
  height: 20px;
}

.skeleton-line.subtitle {
  width: 40%;
  height: 16px;
}

.skeleton-line.content {
  width: 100%;
}

.skeleton-line:last-child {
  margin-bottom: 0;
}

@keyframes shimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}

/* ===== 加载文本 ===== */
.loading-text {
  font-size: var(--text-sm);
  color: var(--color-gray-600);
  text-align: center;
  margin-top: var(--spacing-2);
}
</style>