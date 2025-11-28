<!--
  文件位置: src/components/base/ErrorModal.vue
  简化版错误弹窗组件
-->

<template>
  <Teleport to="body">
    <div v-if="show" class="error-modal-overlay" @click="handleClose">
      <div class="error-modal" @click.stop>
        <div class="error-icon">❌</div>
        <h3>出现了问题</h3>
        <p>{{ error?.message || '未知错误' }}</p>
        <button @click="handleClose">确定</button>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  error: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close'])

const show = computed(() => !!props.error)

const handleClose = () => {
  emit('close')
}
</script>

<style scoped>
.error-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.error-modal {
  background: white;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  max-width: 400px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
}

.error-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.error-modal h3 {
  margin: 0 0 12px 0;
  font-size: 18px;
  color: #333;
}

.error-modal p {
  margin: 0 0 20px 0;
  color: #666;
  line-height: 1.5;
}

.error-modal button {
  background: #3b82f6;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.error-modal button:hover {
  background: #2563eb;
}
</style>