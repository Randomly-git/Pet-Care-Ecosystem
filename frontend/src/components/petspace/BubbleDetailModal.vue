<!-- 
  文件位置: src/components/petspace/BubbleDetailModal.vue
  气泡详情弹窗 - 显示状态详细信息
-->

<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="modal-content" @click.stop>
      <BaseCard padding="lg" shadow="xl" class="detail-card">
        <!-- 头部 -->
        <div class="modal-header">
          <div class="header-icon">{{ bubble.icon }}</div>
          <h3 class="modal-title">{{ bubble.title }}</h3>
          <button class="close-btn" @click="$emit('close')">✕</button>
        </div>

        <!-- 内容 -->
        <div class="modal-body">
          <div class="status-main">
            <div class="status-label">当前状态</div>
            <div class="status-value">{{ data.status }}</div>
          </div>

          <div class="status-description">
            <div class="description-label">详细说明</div>
            <div class="description-text">{{ data.description }}</div>
          </div>

          <div class="status-meta">
            <div class="meta-item">
              <span class="meta-label">最后更新</span>
              <span class="meta-value">{{ data.lastUpdate }}</span>
            </div>
          </div>
        </div>

        <!-- 底部操作 -->
        <div class="modal-footer">
          <BaseButton variant="secondary" @click="$emit('close')">
            关闭
          </BaseButton>
        </div>
      </BaseCard>
    </div>
  </div>
</template>

<script setup>
import BaseCard from '@/components/base/BaseCard.vue'
import BaseButton from '@/components/base/BaseButton.vue'

const props = defineProps({
  bubble: {
    type: Object,
    required: true
  },
  data: {
    type: Object,
    required: true
  }
})

defineEmits(['close'])
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: var(--z-modal);
  animation: fadeIn 0.2s var(--ease-out);
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.modal-content {
  max-width: 500px;
  width: 90%;
  animation: scaleIn 0.3s var(--ease-out);
}

@keyframes scaleIn {
  from {
    transform: scale(0.9);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

.detail-card {
  background: white;
}

/* ===== 头部 ===== */
.modal-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  padding-bottom: var(--spacing-4);
  border-bottom: 2px solid var(--color-gray-200);
  margin-bottom: var(--spacing-6);
}

.header-icon {
  font-size: 2.5rem;
}

.modal-title {
  flex: 1;
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--color-gray-900);
}

.close-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-full);
  background: var(--color-gray-100);
  color: var(--color-gray-600);
  font-size: var(--text-xl);
  transition: all var(--duration-base);
}

.close-btn:hover {
  background: var(--color-gray-200);
  transform: rotate(90deg);
}

/* ===== 内容 ===== */
.modal-body {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-6);
}

.status-main {
  padding: var(--spacing-4);
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.1) 0%, rgba(236, 72, 153, 0.1) 100%);
  border-radius: var(--radius-lg);
}

.status-label {
  font-size: var(--text-sm);
  color: var(--color-gray-600);
  margin-bottom: var(--spacing-2);
}

.status-value {
  font-size: var(--text-xl);
  font-weight: var(--font-semibold);
  color: var(--color-gray-900);
}

.status-description {
  padding: var(--spacing-4);
  background: var(--color-gray-50);
  border-radius: var(--radius-lg);
}

.description-label {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-gray-700);
  margin-bottom: var(--spacing-2);
}

.description-text {
  font-size: var(--text-base);
  color: var(--color-gray-600);
  line-height: 1.6;
  white-space: pre-line;
}

.status-meta {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2);
}

.meta-item {
  display: flex;
  justify-content: space-between;
  padding: var(--spacing-2);
}

.meta-label {
  font-size: var(--text-sm);
  color: var(--color-gray-600);
}

.meta-value {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-gray-900);
}

/* ===== 底部 ===== */
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--spacing-3);
  padding-top: var(--spacing-6);
  border-top: 1px solid var(--color-gray-200);
  margin-top: var(--spacing-6);
}
</style>