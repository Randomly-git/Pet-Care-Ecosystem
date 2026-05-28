<template>
  <div class="test-view">
    <div class="test-container">
      <h1>🧪 API配置测试页面</h1>
      <div class="test-results">
        <div v-for="(test, index) in tests" :key="index" class="test-item">
          <div class="test-status" :class="test.status">
            {{ test.status === "success" ? "✅" : test.status === "error" ? "❌" : "⏳" }}
          </div>
          <div class="test-info">
            <div class="test-name">{{ test.name }}</div>
            <div class="test-message">{{ test.message }}</div>
          </div>
        </div>
      </div>
      <div class="test-controls">
        <button @click="runAllTests" :disabled="isRunning" class="test-btn primary">
          {{ isRunning ? "测试中..." : "运行所有测试" }}
        </button>
        <button @click="testLocalStorage" class="test-btn">测试本地存储</button>
        <button @click="testMessage" class="test-btn">测试消息提示</button>
        <button @click="testAPIConnection" class="test-btn">测试API连接</button>
        <button @click="clearResults" class="test-btn secondary">清空结果</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from "vue"
import { API_CONFIG } from "@/config"

const isRunning = ref(false)
const tests = reactive([
  { name: "环境配置", status: "pending", message: "等待测试..." },
  { name: "本地存储", status: "pending", message: "等待测试..." },
  { name: "消息系统", status: "pending", message: "等待测试..." },
  { name: "API配置", status: "pending", message: "等待测试..." },
  { name: "网络连接", status: "pending", message: "等待测试..." }
])

const updateTest = (index, status, message) => {
  tests[index].status = status
  tests[index].message = message
}

const runAllTests = async () => {
  isRunning.value = true
  tests.forEach(test => {
    test.status = "pending"
    test.message = "等待测试..."
  })
  try {
    updateTest(0, "success", \`当前环境: \${import.meta.env.DEV ? "development" : "production"}\`)
    await testLocalStorage()
    await testMessage()
    updateTest(3, "success", \`API基础地址: \${API_CONFIG.BASE_URL}\`)
    await testAPIConnection()
  } catch (error) {
    console.error("测试运行出错:", error)
  } finally {
    isRunning.value = false
  }
}

const testLocalStorage = async () => {
  try {
    const testKey = "test-" + Date.now()
    const testValue = { data: "test-value" }
    localStorage.setItem(testKey, JSON.stringify(testValue))
    const retrieved = JSON.parse(localStorage.getItem(testKey))
    localStorage.removeItem(testKey)
    if (retrieved.data === "test-value") {
      updateTest(1, "success", "本地存储读写正常")
    } else {
      updateTest(1, "error", "本地存储数据不匹配")
    }
  } catch (error) {
    updateTest(1, "error", \`本地存储错误: \${error.message}\`)
  }
}

const testMessage = async () => {
  try {
    const testMsg = document.createElement("div")
    testMsg.style.cssText = \`
      position: fixed;
      top: 20px;
      right: 20px;
      background: #52C41A;
      color: white;
      padding: 12px 20px;
      border-radius: 8px;
      z-index: 9999;
      font-size: 14px;
    \`
    testMsg.textContent = "✅ 消息系统测试成功"
    document.body.appendChild(testMsg)
    setTimeout(() => {
      if (testMsg.parentNode) {
        testMsg.parentNode.removeChild(testMsg)
      }
    }, 2000)
    updateTest(2, "success", "消息提示系统正常")
  } catch (error) {
    updateTest(2, "error", \`消息系统错误: \${error.message}\`)
  }
}

const testAPIConnection = async () => {
  try {
    const response = await fetch(API_CONFIG.BASE_URL, {
      method: "GET",
      timeout: 5000
    })
    if (response.ok || response.status === 404) {
      updateTest(4, "success", "API服务器连接正常")
    } else {
      updateTest(4, "error", \`API响应异常: \${response.status}\`)
    }
  } catch (error) {
    updateTest(4, "warning", \`API连接失败: \${error.message} (可能是后端未启动)\`)
  }
}

const clearResults = () => {
  tests.forEach(test => {
    test.status = "pending"
    test.message = "等待测试..."
  })
}
</script>

<style scoped>
.test-view {
  min-height: 100vh;
  background: var(--color-gray-50);
  padding: var(--spacing-8);
}
.test-container {
  max-width: 800px;
  margin: 0 auto;
  background: var(--color-white);
  border-radius: var(--radius-xl);
  padding: var(--spacing-8);
  box-shadow: var(--shadow-lg);
}
h1 {
  text-align: center;
  color: var(--color-black);
  margin-bottom: var(--spacing-8);
  font-size: var(--text-3xl);
}
.test-results {
  margin-bottom: var(--spacing-8);
}
.test-item {
  display: flex;
  align-items: center;
  padding: var(--spacing-4);
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-md);
  margin-bottom: var(--spacing-3);
  transition: all var(--duration-base);
}
.test-item:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-sm);
}
.test-status {
  font-size: var(--text-2xl);
  margin-right: var(--spacing-4);
  width: 40px;
  text-align: center;
}
.test-info {
  flex: 1;
}
.test-name {
  font-weight: var(--font-semibold);
  color: var(--color-gray-900);
  margin-bottom: var(--spacing-1);
}
.test-message {
  font-size: var(--text-sm);
  color: var(--color-gray-600);
}
.test-controls {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-3);
  justify-content: center;
}
.test-btn {
  padding: var(--spacing-3) var(--spacing-6);
  border: none;
  border-radius: var(--radius-md);
  font-weight: var(--font-medium);
  cursor: pointer;
  transition: all var(--duration-base);
  background: var(--color-primary);
  color: var(--color-white);
}
.test-btn:hover {
  background: var(--color-primary-dark);
  transform: translateY(-1px);
}
.test-btn:disabled {
  background: var(--color-gray-400);
  cursor: not-allowed;
  transform: none;
}
.test-btn.secondary {
  background: var(--color-gray-200);
  color: var(--color-gray-700);
}
.test-btn.secondary:hover {
  background: var(--color-gray-300);
}
@media (max-width: 640px) {
  .test-container {
    padding: var(--spacing-6);
  }
  .test-controls {
    flex-direction: column;
  }
}
</style>
