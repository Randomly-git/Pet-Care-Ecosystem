<!-- 
  文件位置: src/components/petspace/PetAvatarBubbles.vue
  宠物头像 + 周围浮动气泡
-->

<template>
  <div class="pet-avatar-bubbles">
    <!-- 状态气泡 -->
    <transition-group name="bubble" tag="div">
      <div
        v-for="(bubble, index) in bubbles"
        v-show="bubblesExpanded"
        :key="bubble.type"
        :class="['status-bubble', `bubble-${index}`]"
        :style="getBubblePosition(index)"
        @click="handleBubbleClick(bubble)"
      >
        <div class="bubble-icon">{{ bubble.icon }}</div>
        <div class="bubble-content">
          <div class="bubble-title">{{ bubble.title }}</div>
          <div class="bubble-status">{{ bubble.status }}</div>
        </div>
      </div>
    </transition-group>

    <!-- 中央宠物头像 -->
    <div class="pet-avatar-wrapper" @click="$emit('toggle-bubbles')">
      <div class="avatar-container">
        <BaseAvatar
          :src="pet.avatar"
          :placeholder="pet.name.charAt(0)"
          size="xl"
          class="pet-avatar"
        />
        <div class="avatar-name">{{ pet.name }}</div>
        <div class="avatar-info">{{ pet.species }} · {{ pet.breed }}</div>
        <div class="toggle-hint">
          {{ bubblesExpanded ? '点击收起' : '点击展开' }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import BaseAvatar from '@/components/base/BaseAvatar.vue'

const props = defineProps({
  pet: {
    type: Object,
    required: true
  },
  bubblesExpanded: {
    type: Boolean,
    default: true
  },
  statusData: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['toggle-bubbles', 'bubble-click'])

// 将状态数据转换为气泡数组
const bubbles = computed(() => {
  return Object.keys(props.statusData).map(key => ({
    type: key,
    ...props.statusData[key]
  }))
})

// 计算气泡位置（圆形环绕）
const getBubblePosition = (index) => {
  const total = bubbles.value.length
  const angle = (index * 360) / total - 90 // 从顶部开始
  const radius = 200 // 半径
  
  const radian = (angle * Math.PI) / 180
  const x = Math.cos(radian) * radius
  const y = Math.sin(radian) * radius
  
  return {
    transform: `translate(${x}px, ${y}px)`,
    animationDelay: `${index * 0.1}s`
  }
}

const handleBubbleClick = (bubble) => {
  emit('bubble-click', bubble)
}
</script>

<style scoped>
.pet-avatar-bubbles {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ===== 中央头像 ===== */
.pet-avatar-wrapper {
  position: relative;
  z-index: 10;
  cursor: pointer;
}

.avatar-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-3);
  padding: var(--spacing-6);
  background: rgba(255, 255, 255, 0.95);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-xl);
  transition: all var(--duration-base) var(--ease-out);
}

.avatar-container:hover {
  transform: scale(1.05);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
}

.pet-avatar {
  transition: all var(--duration-base);
}

.avatar-name {
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--color-gray-900);
}

.avatar-info {
  font-size: var(--text-sm);
  color: var(--color-gray-600);
}

.toggle-hint {
  font-size: var(--text-xs);
  color: var(--color-gray-400);
  margin-top: var(--spacing-2);
}

/* ===== 状态气泡 ===== */
.status-bubble {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 160px;
  padding: var(--spacing-4);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  cursor: pointer;
  transition: all var(--duration-base) var(--ease-out);
  z-index: 5;
}

.status-bubble:hover {
  transform: translate(var(--tx), var(--ty)) scale(1.1) !important;
  box-shadow: var(--shadow-lg);
  z-index: 15;
}

.bubble-icon {
  font-size: 2rem;
  text-align: center;
  margin-bottom: var(--spacing-2);
}

.bubble-content {
  text-align: center;
}

.bubble-title {
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  color: var(--color-gray-900);
  margin-bottom: var(--spacing-1);
}

.bubble-status {
  font-size: var(--text-sm);
  color: var(--color-gray-600);
  line-height: 1.4;
}

/* ===== 气泡动画 ===== */
.bubble-enter-active,
.bubble-leave-active {
  transition: all 0.5s var(--ease-out);
}

.bubble-enter-from,
.bubble-leave-to {
  opacity: 0;
  transform: translate(-50%, -50%) scale(0);
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .status-bubble {
    width: 120px;
    padding: var(--spacing-3);
  }

  .bubble-icon {
    font-size: 1.5rem;
  }

  .bubble-title {
    font-size: var(--text-sm);
  }

  .bubble-status {
    font-size: var(--text-xs);
  }
}
</style>