<template>
  <div class="base-input-wrapper">
    <label v-if="label" :for="inputId" class="input-label">
      {{ label }}
      <span v-if="required" class="required-mark">*</span>
    </label>
    
    <div class="input-container">
      <span v-if="prefixIcon" class="input-icon input-icon--prefix">
        {{ prefixIcon }}
      </span>
      
      <input
        :id="inputId"
        :type="type"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :class="inputClasses"
        @input="handleInput"
        @focus="handleFocus"
        @blur="handleBlur"
      />
      
      <span v-if="suffixIcon" class="input-icon input-icon--suffix">
        {{ suffixIcon }}
      </span>
    </div>
    
    <p v-if="error" class="input-error">{{ error }}</p>
    <p v-else-if="hint" class="input-hint">{{ hint }}</p>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  // v-model 绑定值
  modelValue: {
    type: [String, Number],
    default: ''
  },
  // 标签
  label: {
    type: String,
    default: ''
  },
  // 输入类型
  type: {
    type: String,
    default: 'text'
  },
  // 占位文本
  placeholder: {
    type: String,
    default: ''
  },
  // 前缀图标
  prefixIcon: {
    type: String,
    default: ''
  },
  // 后缀图标
  suffixIcon: {
    type: String,
    default: ''
  },
  // 是否禁用
  disabled: {
    type: Boolean,
    default: false
  },
  // 是否必填
  required: {
    type: Boolean,
    default: false
  },
  // 错误信息
  error: {
    type: String,
    default: ''
  },
  // 提示信息
  hint: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'focus', 'blur'])

const inputId = computed(() => `input-${Math.random().toString(36).substr(2, 9)}`)
const isFocused = ref(false)

const inputClasses = computed(() => {
  return [
    'base-input',
    {
      'base-input--error': props.error,
      'base-input--disabled': props.disabled,
      'base-input--with-prefix': props.prefixIcon,
      'base-input--with-suffix': props.suffixIcon
    }
  ]
})

const handleInput = (event) => {
  emit('update:modelValue', event.target.value)
}

const handleFocus = (event) => {
  isFocused.value = true
  emit('focus', event)
}

const handleBlur = (event) => {
  isFocused.value = false
  emit('blur', event)
}
</script>

<style scoped>
.base-input-wrapper {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-2);
}

.input-label {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-gray-700);
}

.required-mark {
  color: var(--color-danger);
  margin-left: var(--spacing-1);
}

.input-container {
  position: relative;
  display: flex;
  align-items: center;
}

.base-input {
  width: 100%;
  padding: var(--spacing-3) var(--spacing-4);
  font-size: var(--text-base);
  color: var(--color-gray-900);
  background-color: var(--color-white);
  border: 2px solid var(--color-gray-300);
  border-radius: var(--radius-lg);
  transition: all var(--duration-base) var(--ease-in-out);
  outline: none;
}

.base-input::placeholder {
  color: var(--color-gray-400);
}

.base-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
}

.base-input--with-prefix {
  padding-left: var(--spacing-10);
}

.base-input--with-suffix {
  padding-right: var(--spacing-10);
}

.base-input--error {
  border-color: var(--color-danger);
}

.base-input--error:focus {
  box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.1);
}

.base-input--disabled {
  background-color: var(--color-gray-100);
  cursor: not-allowed;
  opacity: 0.6;
}

.input-icon {
  position: absolute;
  color: var(--color-gray-400);
  font-size: var(--text-lg);
  pointer-events: none;
}

.input-icon--prefix {
  left: var(--spacing-4);
}

.input-icon--suffix {
  right: var(--spacing-4);
}

.input-error {
  font-size: var(--text-sm);
  color: var(--color-danger);
  margin: 0;
}

.input-hint {
  font-size: var(--text-sm);
  color: var(--color-gray-500);
  margin: 0;
}
</style>