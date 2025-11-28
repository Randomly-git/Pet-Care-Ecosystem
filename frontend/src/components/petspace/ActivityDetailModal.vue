<!-- 
  文件位置: src/components/petspace/ActivityDetailModal.vue
  活动详情弹窗 - 编辑活动信息
-->

<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="modal-content" @click.stop>
      <BaseCard padding="lg" shadow="xl" class="detail-card">
        <!-- 头部 -->
        <div class="modal-header">
          <div class="header-icon">{{ getCategoryIcon(formData.category) }}</div>
          <h3 class="modal-title">{{ formData.name }}</h3>
          <button class="settings-btn" @click="handleSettings">⚙️</button>
          <button class="close-btn" @click="$emit('close')">✕</button>
        </div>

        <!-- 内容 -->
        <div class="modal-body">
          <!-- 时间和状态 -->
          <div class="info-row">
            <div class="info-item">
              <span class="info-label">时间</span>
              <span class="info-value">
                {{ formatDateTime(currentDate, formData.time) }}
              </span>
            </div>
            <div class="info-item">
              <span class="info-label">状态</span>
              <span :class="['status-badge', formData.completed ? 'completed' : 'pending']">
                {{ formData.completed ? '✓ 已完成' : '○ 未完成' }}
              </span>
            </div>
          </div>

          <!-- 描述 -->
          <div class="form-group">
            <label class="form-label">描述</label>
            <textarea
              v-model="formData.description"
              class="form-textarea"
              placeholder="记录一些备注信息..."
              rows="4"
            ></textarea>
          </div>

          <!-- 图片 -->
          <div class="form-group">
            <label class="form-label">图片</label>
            <div class="image-gallery">
              <div
                v-for="(image, index) in formData.images"
                :key="index"
                class="image-item"
              >
                <div class="image-preview">📸 {{ image }}</div>
                <button class="image-remove" @click="removeImage(index)">✕</button>
              </div>
              <div class="image-upload" @click="handleUpload">
                <span class="upload-icon">+</span>
                <span class="upload-text">上传图片</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部操作 -->
        <div class="modal-footer">
          <BaseButton variant="outline" @click="handleDelete">
            删除
          </BaseButton>
          <div class="footer-right">
            <BaseButton variant="secondary" @click="$emit('close')">
              取消
            </BaseButton>
            <BaseButton variant="primary" @click="handleSave">
              保存
            </BaseButton>
          </div>
        </div>
      </BaseCard>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseButton from '@/components/base/BaseButton.vue'

const props = defineProps({
  activity: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['save', 'delete', 'close', 'settings'])

const currentDate = new Date()

// 表单数据
const formData = reactive({
  ...props.activity
})

// 获取分类图标
const getCategoryIcon = (category) => {
  const icons = {
    diet: '🍖',
    health: '💊',
    hygiene: '🛁',
    activity: '🎮'
  }
  return icons[category] || '📌'
}

// 格式化日期时间
const formatDateTime = (date, time) => {
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  return time ? `${year}-${month}-${day} ${time}` : `${year}-${month}-${day}`
}

// 移除图片
const removeImage = (index) => {
  formData.images.splice(index, 1)
}

// 上传图片
const handleUpload = () => {
  // TODO: 实现图片上传
  alert('图片上传功能开发中...')
}

// 保存
const handleSave = () => {
  emit('save', { ...formData })
}

// 删除
const handleDelete = () => {
  if (confirm('确定要删除这个活动吗？')) {
    emit('delete', formData.id)
  }
}

// 打开设置
const handleSettings = () => {
  emit('settings', { ...formData })
  emit('close')
}
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
  max-width: 600px;
  width: 90%;
  max-height: 90vh;
  overflow-y: auto;
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
  font-size: 2rem;
}

.modal-title {
  flex: 1;
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--color-gray-900);
}

.settings-btn,
.close-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-full);
  background: var(--color-gray-100);
  font-size: var(--text-xl);
  transition: all var(--duration-base);
}

.settings-btn {
  color: var(--color-gray-600);
}

.close-btn {
  color: var(--color-gray-600);
}

.settings-btn:hover,
.close-btn:hover {
  background: var(--color-gray-200);
}

.close-btn:hover {
  transform: rotate(90deg);
}

/* ===== 内容 ===== */
.modal-body {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-6);
}

.info-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--spacing-4);
}

.info-item {
  padding: var(--spacing-4);
  background: var(--color-gray-50);
  border-radius: var(--radius-lg);
}

.info-label {
  display: block;
  font-size: var(--text-sm);
  color: var(--color-gray-600);
  margin-bottom: var(--spacing-2);
}

.info-value {
  display: block;
  font-size: var(--text-base);
  font-weight: var(--font-medium);
  color: var(--color-gray-900);
}

.status-badge {
  display: inline-block;
  padding: var(--spacing-1) var(--spacing-3);
  border-radius: var(--radius-full);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
}

.status-badge.completed {
  background: rgba(16, 185, 129, 0.1);
  color: var(--color-success);
}

.status-badge.pending {
  background: rgba(156, 163, 175, 0.1);
  color: var(--color-gray-600);
}

/* ===== 表单 ===== */
.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2);
}

.form-label {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-gray-700);
}

.form-textarea {
  width: 100%;
  padding: var(--spacing-3);
  font-size: var(--text-base);
  color: var(--color-gray-900);
  background: var(--color-white);
  border: 2px solid var(--color-gray-300);
  border-radius: var(--radius-lg);
  resize: vertical;
  font-family: inherit;
  transition: all var(--duration-base);
}

.form-textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
}

/* ===== 图片画廊 ===== */
.image-gallery {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: var(--spacing-3);
}

.image-item {
  position: relative;
  aspect-ratio: 1;
}

.image-preview {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-gray-100);
  border-radius: var(--radius-lg);
  font-size: var(--text-sm);
  color: var(--color-gray-500);
}

.image-remove {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-danger);
  color: white;
  border-radius: var(--radius-full);
  font-size: var(--text-sm);
  box-shadow: var(--shadow-sm);
}

.image-upload {
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-2);
  background: var(--color-gray-50);
  border: 2px dashed var(--color-gray-300);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--duration-base);
}

.image-upload:hover {
  background: var(--color-gray-100);
  border-color: var(--color-primary);
}

.upload-icon {
  font-size: var(--text-2xl);
  color: var(--color-gray-400);
}

.upload-text {
  font-size: var(--text-xs);
  color: var(--color-gray-500);
}

/* ===== 底部 ===== */
.modal-footer {
  display: flex;
  justify-content: space-between;
  gap: var(--spacing-3);
  padding-top: var(--spacing-6);
  border-top: 1px solid var(--color-gray-200);
  margin-top: var(--spacing-6);
}

.footer-right {
  display: flex;
  gap: var(--spacing-3);
}
</style>