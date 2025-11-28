<!--
  文件位置: src/components/base/BaseIcon.vue
  通用图标组件
-->

<template>
  <i v-if="type === 'font'" :class="['icon', name]" :style="iconStyle" />
  <span v-else-if="type === 'emoji'" :style="iconStyle">{{ name }}</span>
  <svg
    v-else
    :class="['svg-icon', { 'spin': spin }]"
    :style="iconStyle"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="currentColor"
  >
    <use :href="`#${name}`" />
  </svg>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  name: {
    type: String,
    required: true
  },
  type: {
    type: String,
    default: 'svg', // svg | font | emoji
    validator: (value) => ['svg', 'font', 'emoji'].includes(value)
  },
  size: {
    type: [String, Number],
    default: 24
  },
  color: {
    type: String,
    default: 'currentColor'
  },
  spin: {
    type: Boolean,
    default: false
  }
})

const iconStyle = computed(() => ({
  width: typeof props.size === 'number' ? `${props.size}px` : props.size,
  height: typeof props.size === 'number' ? `${props.size}px` : props.size,
  color: props.color,
  fontSize: typeof props.size === 'number' ? `${props.size}px` : props.size,
  lineHeight: 1
}))
</script>

<style scoped>
.svg-icon {
  display: inline-block;
  vertical-align: middle;
  transition: transform 0.3s ease;
}

.svg-icon.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.icon {
  display: inline-block;
  vertical-align: middle;
}
</style>