<template>
  <div :class="cardClasses">
    <slot></slot>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // 是否可悬停
  hoverable: {
    type: Boolean,
    default: false
  },
  // 内边距大小: none | sm | md | lg
  padding: {
    type: String,
    default: 'md',
    validator: (value) => ['none', 'sm', 'md', 'lg'].includes(value)
  },
  // 阴影大小: sm | md | lg
  shadow: {
    type: String,
    default: 'md',
    validator: (value) => ['sm', 'md', 'lg'].includes(value)
  }
})

const cardClasses = computed(() => {
  return [
    'base-card',
    `base-card--padding-${props.padding}`,
    `base-card--shadow-${props.shadow}`,
    {
      'base-card--hoverable': props.hoverable
    }
  ]
})
</script>

<style scoped>
.base-card {
  background-color: var(--color-white);
  border-radius: var(--radius-xl);
  transition: all var(--duration-base) var(--ease-out);
}

/* ===== 内边距 ===== */
.base-card--padding-none {
  padding: 0;
}

.base-card--padding-sm {
  padding: var(--spacing-4);
}

.base-card--padding-md {
  padding: var(--spacing-6);
}

.base-card--padding-lg {
  padding: var(--spacing-8);
}

/* ===== 阴影 ===== */
.base-card--shadow-sm {
  box-shadow: var(--shadow-sm);
}

.base-card--shadow-md {
  box-shadow: var(--shadow-md);
}

.base-card--shadow-lg {
  box-shadow: var(--shadow-lg);
}

/* ===== 悬停效果 ===== */
.base-card--hoverable {
  cursor: pointer;
}

.base-card--hoverable:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}
</style>