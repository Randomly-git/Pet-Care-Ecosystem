<template>
  <button
    :class="buttonClasses"
    :disabled="disabled"
    @click="handleClick"
  >
    <slot></slot>
  </button>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 按钮类型: primary | secondary | text | outline
  variant: {
    type: String,
    default: 'primary',
    validator: (value) => ['primary', 'secondary', 'text', 'outline'].includes(value)
  },
  // 按钮大小: sm | md | lg
  size: {
    type: String,
    default: 'md',
    validator: (value) => ['sm', 'md', 'lg'].includes(value)
  },
  // 是否禁用
  disabled: {
    type: Boolean,
    default: false
  },
  // 是否全宽
  fullWidth: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['click'])

const buttonClasses = computed(() => {
  return [
    'base-button',
    `base-button--${props.variant}`,
    `base-button--${props.size}`,
    {
      'base-button--full-width': props.fullWidth,
      'base-button--disabled': props.disabled
    }
  ]
})

const handleClick = (event) => {
  if (!props.disabled) {
    emit('click', event)
  }
}
</script>

<style scoped>
.base-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-2);
  font-weight: var(--font-medium);
  border-radius: var(--radius-lg);
  transition: all var(--duration-base) var(--ease-in-out);
  cursor: pointer;
  border: none;
  outline: none;
}

/* ===== 按钮大小 ===== */
.base-button--sm {
  padding: var(--spacing-2) var(--spacing-4);
  font-size: var(--text-sm);
  min-height: 36px;
}

.base-button--md {
  padding: var(--spacing-3) var(--spacing-6);
  font-size: var(--text-base);
  min-height: 44px;
}

.base-button--lg {
  padding: var(--spacing-4) var(--spacing-8);
  font-size: var(--text-lg);
  min-height: 52px;
}

/* ===== Primary 按钮 ===== */
.base-button--primary {
  background: var(--gradient-primary);
  color: var(--color-white);
  box-shadow: var(--shadow-sm);
}

.base-button--primary:hover:not(.base-button--disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.base-button--primary:active:not(.base-button--disabled) {
  transform: translateY(0);
  box-shadow: var(--shadow-sm);
}

/* ===== Secondary 按钮 ===== */
.base-button--secondary {
  background-color: var(--color-gray-100);
  color: var(--color-gray-700);
  box-shadow: var(--shadow-xs);
}

.base-button--secondary:hover:not(.base-button--disabled) {
  background-color: var(--color-gray-200);
  transform: translateY(-2px);
}

/* ===== Outline 按钮 ===== */
.base-button--outline {
  background-color: transparent;
  color: var(--color-primary);
  border: 2px solid var(--color-primary);
}

.base-button--outline:hover:not(.base-button--disabled) {
  background-color: rgba(99, 102, 241, 0.05);
  transform: translateY(-2px);
}

/* ===== Text 按钮 ===== */
.base-button--text {
  background-color: transparent;
  color: var(--color-primary);
  padding: var(--spacing-2) var(--spacing-3);
}

.base-button--text:hover:not(.base-button--disabled) {
  background-color: rgba(99, 102, 241, 0.05);
}

/* ===== 禁用状态 ===== */
.base-button--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* ===== 全宽按钮 ===== */
.base-button--full-width {
  width: 100%;
}
</style>