<!-- 
  文件位置: src/components/petspace/ActivitySettingsModal.vue
  活动设置弹窗 - 配置活动频次、时间、提醒等
-->

<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="modal-content" @click.stop>
      <BaseCard padding="lg" shadow="xl" class="settings-card">
        <!-- 头部 -->
        <div class="modal-header">
          <div class="header-icon">⚙️</div>
          <h3 class="modal-title">活动设置 - {{ formData.name }}</h3>
          <button class="close-btn" @click="$emit('close')">✕</button>
        </div>

        <!-- 内容 -->
        <div class="modal-body">
          <!-- 活动名称 -->
          <div class="form-group">
            <label class="form-label">活动名称</label>
            <BaseInput
              v-model="formData.name"
              placeholder="输入活动名称"
            />
          </div>

          <!-- 频次设置 -->
          <div class="form-group">
            <label class="form-label">频次设置</label>
            <div class="radio-group">
              <label class="radio-item">
                <input
                  type="radio"
                  v-model="formData.frequency"
                  value="daily"
                />
                <span>每天</span>
              </label>
              <label class="radio-item">
                <input
                  type="radio"
                  v-model="formData.frequency"
                  value="weekly"
                />
                <span>每周</span>
              </label>
              <label class="radio-item">
                <input
                  type="radio"
                  v-model="formData.frequency"
                  value="custom"
                />
                <span>自定义</span>
              </label>
            </div>
            <div v-if="formData.frequency === 'custom'" class="custom-days">
              <span>每</span>
              <input
                type="number"
                v-model.number="formData.customDays"
                class="days-input"
                min="1"
              />
              <span>天</span>
            </div>
          </div>

          <!-- 时间设置 -->
          <div class="form-group">
            <label class="form-label">时间设置</label>
            <div class="time-input-group">
              <input
                type="number"
                v-model.number="timeHour"
                class="time-input"
                placeholder="HH"
                min="0"
                max="23"
              />
              <span class="time-separator">:</span>
              <input
                type="number"
                v-model.number="timeMinute"
                class="time-input"
                placeholder="MM"
                min="0"
                max="59"
              />
            </div>
          </div>

          <!-- 自动标记 -->
          <div class="form-group">
            <label class="checkbox-item">
              <input
                type="checkbox"
                v-model="formData.autoMark"
              />
              <span>到时间自动标记为已完成</span>
            </label>
          </div>

          <!-- 提醒功能 -->
          <div class="form-group">
            <label class="checkbox-item">
              <input
                type="checkbox"
                v-model="formData.reminder"
              />
              <span>启用提醒</span>
            </label>
            <div v-if="formData.reminder" class="reminder-config">
              <span>提前</span>
              <input
                type="number"
                v-model.number="formData.reminderMinutes"
                class="minutes-input"
                min="1"
              />
              <span>分钟提醒</span>
            </div>
          </div>

          <!-- 重要程度 -->
          <div class="form-group">
            <label class="form-label">重要程度</label>
            <div class="radio-group">
              <label class="radio-item">
                <input
                  type="radio"
                  v-model="formData.importance"
                  value="normal"
                />
                <span>普通</span>
              </label>
              <label class="radio-item">
                <input
                  type="radio"
                  v-model="formData.importance"
                  value="important"
                />
                <span>重要</span>
              </label>
              <label class="radio-item">
                <input
                  type="radio"
                  v-model="formData.importance"
                  value="very-important"
                />
                <span>非常重要</span>
              </label>
            </div>
          </div>
        </div>

        <!-- 底部操作 -->
        <div class="modal-footer">
          <BaseButton variant="secondary" @click="$emit('close')">
            取消
          </BaseButton>
          <BaseButton variant="primary" @click="handleSave">
            保存设置
          </BaseButton>
        </div>
      </BaseCard>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseInput from '@/components/base/BaseInput.vue'

const props = defineProps({
  activity: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['save', 'close'])

// 表单数据
const formData = reactive({
  ...props.activity,
  customDays: props.activity.customDays || 1,
  reminderMinutes: props.activity.reminderMinutes || 15
})

// 时间分解
const timeHour = ref(0)
const timeMinute = ref(0)

// 初始化时间
if (formData.time) {
  const [h, m] = formData.time.split(':')
  timeHour.value = parseInt(h)
  timeMinute.value = parseInt(m)
}

// 监听时间变化
watch([timeHour, timeMinute], ([h, m]) => {
  const hour = String(h || 0).padStart(2, '0')
  const minute = String(m || 0).padStart(2, '0')
  formData.time = `${hour}:${minute}`
})

// 保存设置
const handleSave = () => {
  emit('save', { ...formData })
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
  max-width: 550px;
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

.settings-card {
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
  font-size: var(--text-xl);
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

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3);
}

.form-label {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-gray-700);
}

/* ===== 单选按钮组 ===== */
.radio-group {
  display: flex;
  gap: var(--spacing-4);
  flex-wrap: wrap;
}

.radio-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  cursor: pointer;
}

.radio-item input[type="radio"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
  accent-color: var(--color-primary);
}

.radio-item span {
  font-size: var(--text-base);
  color: var(--color-gray-700);
}

/* ===== 复选框 ===== */
.checkbox-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  cursor: pointer;
}

.checkbox-item input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
  accent-color: var(--color-primary);
}

.checkbox-item span {
  font-size: var(--text-base);
  color: var(--color-gray-700);
}

/* ===== 自定义天数 ===== */
.custom-days {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  padding: var(--spacing-3);
  background: var(--color-gray-50);
  border-radius: var(--radius-md);
}

.days-input {
  width: 60px;
  padding: var(--spacing-2);
  text-align: center;
  font-size: var(--text-base);
  border: 2px solid var(--color-gray-300);
  border-radius: var(--radius-md);
  transition: all var(--duration-base);
}

.days-input:focus {
  outline: none;
  border-color: var(--color-primary);
}

/* ===== 时间输入 ===== */
.time-input-group {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
}

.time-input {
  width: 70px;
  padding: var(--spacing-3);
  text-align: center;
  font-size: var(--text-lg);
  font-weight: var(--font-medium);
  border: 2px solid var(--color-gray-300);
  border-radius: var(--radius-md);
  transition: all var(--duration-base);
}

.time-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
}

.time-separator {
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--color-gray-400);
}

/* ===== 提醒配置 ===== */
.reminder-config {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  padding: var(--spacing-3);
  background: var(--color-gray-50);
  border-radius: var(--radius-md);
}

.minutes-input {
  width: 70px;
  padding: var(--spacing-2);
  text-align: center;
  font-size: var(--text-base);
  border: 2px solid var(--color-gray-300);
  border-radius: var(--radius-md);
  transition: all var(--duration-base);
}

.minutes-input:focus {
  outline: none;
  border-color: var(--color-primary);
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