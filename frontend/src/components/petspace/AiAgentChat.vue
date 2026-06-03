<template>
  <el-drawer
    v-model="visible"
    title="AI 活动助手"
    direction="rtl"
    size="400px"
    class="ai-agent-drawer"
    :before-close="handleClose"
  >
    <div class="chat-container">
      <div class="chat-messages" ref="messagesRef">
        <div 
          v-for="(msg, index) in messages" 
          :key="index"
          :class="['message-wrapper', msg.role === 'user' ? 'is-user' : 'is-ai']"
        >
          <div class="message-bubble">
            <div class="message-content">{{ msg.content }}</div>
          </div>
          
          <!-- Tool Calls rendering -->
          <div v-if="msg.toolCalls && msg.toolCalls.length > 0" class="tool-calls">
            <div v-for="(tool, tIndex) in msg.toolCalls" :key="tIndex" class="tool-call-item">
              <el-icon><Setting /></el-icon>
              <span v-if="tool.toolName === 'create_activity_record'">🔧 AI 自动记录活动成功</span>
              <span v-else-if="tool.toolName === 'get_pet_info'">🔍 AI 获取宠物信息</span>
              <span v-else-if="tool.toolName === 'get_activity_records'">📋 AI 查询历史记录</span>
              <span v-else>🔧 执行工具: {{ tool.toolName }}</span>
            </div>
          </div>
        </div>
        
        <div v-if="loading" class="message-wrapper is-ai">
          <div class="message-bubble loading-bubble">
            <span class="dot"></span><span class="dot"></span><span class="dot"></span>
          </div>
        </div>
      </div>
      
      <div class="chat-input">
        <el-input
          v-model="inputText"
          placeholder="例如：下午5点带可乐去散步了30分钟"
          @keyup.enter="sendMessage"
          :disabled="loading"
        >
          <template #append>
            <el-button @click="sendMessage" :disabled="!inputText.trim() || loading" :loading="loading">
              发送
            </el-button>
          </template>
        </el-input>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { Setting } from '@element-plus/icons-vue'
import { aiAgentChat } from '@/api/ai'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  petId: {
    type: [String, Number],
    required: true
  }
})

const emit = defineEmits(['update:modelValue', 'record-created'])

const visible = ref(props.modelValue)
const inputText = ref('')
const loading = ref(false)
const messagesRef = ref(null)

const messages = ref([
  {
    role: 'ai',
    content: '你好！我是你的 AI 宠物助手。你可以直接用自然语言告诉我你要记录什么活动，比如“今天中午喂了100g皇室猫粮”，或者向我查询记录。'
  }
])

watch(() => props.modelValue, (newVal) => {
  visible.value = newVal
})

const handleClose = (done) => {
  emit('update:modelValue', false)
  done()
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text) return
  
  if (!props.petId) {
    ElMessage.warning('请先在左侧选中一只宠物')
    return
  }

  // Add user message
  messages.value.push({
    role: 'user',
    content: text
  })
  
  inputText.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const res = await aiAgentChat(props.petId, text)
    
    // Add AI message
    messages.value.push({
      role: 'ai',
      content: res.message,
      toolCalls: res.toolCalls || []
    })
    
    // Check if activity was created
    if (res.toolCalls && res.toolCalls.some(t => t.toolName === 'create_activity_record' && t.success)) {
      emit('record-created')
    }
  } catch (error) {
    messages.value.push({
      role: 'ai',
      content: '抱歉，我遇到了一点问题，请稍后再试。'
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: #f8f9fa;
}

.message-wrapper {
  display: flex;
  flex-direction: column;
  max-width: 85%;
}

.message-wrapper.is-user {
  align-self: flex-end;
  align-items: flex-end;
}

.message-wrapper.is-ai {
  align-self: flex-start;
  align-items: flex-start;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}

.is-user .message-bubble {
  background: #6750A4;
  color: white;
  border-bottom-right-radius: 4px;
}

.is-ai .message-bubble {
  background: white;
  color: #333;
  border-bottom-left-radius: 4px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.tool-calls {
  margin-top: 6px;
  font-size: 12px;
  color: #6750A4;
  background: rgba(103, 80, 164, 0.1);
  padding: 6px 12px;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.tool-call-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.chat-input {
  padding: 16px;
  background: white;
  border-top: 1px solid #ebeef5;
}

/* Loading dots animation */
.loading-bubble {
  display: flex;
  gap: 4px;
  align-items: center;
  height: 24px;
}

.dot {
  width: 6px;
  height: 6px;
  background: #909399;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}
</style>
