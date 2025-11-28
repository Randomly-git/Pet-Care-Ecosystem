<template>
  <div :class="avatarClasses">
    <img
      v-if="src"
      :src="src"
      :alt="alt"
      @error="handleImageError"
    />
    <div v-else class="avatar-placeholder">
      {{ placeholder }}
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  // 头像图片URL
  src: {
    type: String,
    default: ''
  },
  // 图片alt文本
  alt: {
    type: String,
    default: 'Avatar'
  },
  // 大小: sm | md | lg | xl
  size: {
    type: String,
    default: 'md',
    validator: (value) => ['sm', 'md', 'lg', 'xl'].includes(value)
  },
  // 占位文本（当无图片时显示）
  placeholder: {
    type: String,
    default: '?'
  }
})

const imageError = ref(false)

const avatarClasses = computed(() => {
  return [
    'base-avatar',
    `base-avatar--${props.size}`
  ]
})

const handleImageError = () => {
  imageError.value = true
}
</script>

<style scoped>
.base-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-full);
  overflow: hidden;
  background: var(--gradient-primary);
  flex-shrink: 0;
  box-shadow: var(--shadow-sm);
}

.base-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  color: var(--color-white);
  font-weight: var(--font-semibold);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

/* ===== 大小变体 ===== */
.base-avatar--sm {
  width: 32px;
  height: 32px;
}

.base-avatar--sm .avatar-placeholder {
  font-size: var(--text-sm);
}

.base-avatar--md {
  width: 48px;
  height: 48px;
}

.base-avatar--md .avatar-placeholder {
  font-size: var(--text-lg);
}

.base-avatar--lg {
  width: 64px;
  height: 64px;
}

.base-avatar--lg .avatar-placeholder {
  font-size: var(--text-xl);
}

.base-avatar--xl {
  width: 96px;
  height: 96px;
}

.base-avatar--xl .avatar-placeholder {
  font-size: var(--text-3xl);
}
</style>