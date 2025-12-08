<template>
  <div class="community-test">
    <div class="test-header">
      <h1>社区功能测试</h1>
      <p>测试社区微服务API集成和组件功能</p>
    </div>

    <div class="test-sections">
      <!-- API连接测试 -->
      <section class="test-section">
        <h2>微服务连接状态</h2>
        <BaseCard class="status-card">
          <div class="status-item">
            <strong>社区微服务:</strong>
            <span :class="communityStatus.color">{{ communityStatus.text }}</span>
          </div>
          <div class="status-item">
            <strong>媒体微服务:</strong>
            <span :class="mediaStatus.color">{{ mediaStatus.text }}</span>
          </div>
          <div class="status-item">
            <strong>用户微服务:</strong>
            <span :class="userStatus.color">{{ userStatus.text }}</span>
          </div>
          <BaseButton variant="outline" @click="testConnections">
            重新检测
          </BaseButton>
        </BaseCard>
      </section>

      <!-- 功能测试 -->
      <section class="test-section">
        <h2>功能测试</h2>
        <div class="test-actions">
          <BaseButton variant="primary" @click="testCreateMoment">
            测试创建动态
          </BaseButton>
          <BaseButton variant="primary" @click="testGetMoments">
            测试获取动态列表
          </BaseButton>
          <BaseButton variant="primary" @click="testUploadMedia">
            测试媒体上传
          </BaseButton>
          <BaseButton variant="outline" @click="clearResults">
            清空结果
          </BaseButton>
        </div>
      </section>

      <!-- 测试结果 -->
      <section class="test-section">
        <h2>测试结果</h2>
        <BaseCard class="results-card">
          <div v-if="testResults.length === 0" class="no-results">
            暂无测试结果
          </div>
          <div v-else class="results-list">
            <div
              v-for="(result, index) in testResults"
              :key="index"
              class="result-item"
              :class="result.type"
            >
              <div class="result-header">
                <BaseIcon :name="getIconForType(result.type)" size="20" />
                <span class="result-title">{{ result.title }}</span>
                <span class="result-time">{{ formatTime(result.timestamp) }}</span>
              </div>
              <div class="result-content">
                <p v-if="result.message">{{ result.message }}</p>
                <pre v-if="result.data" class="result-data">{{ JSON.stringify(result.data, null, 2) }}</pre>
              </div>
            </div>
          </div>
        </BaseCard>
      </section>

      <!-- 实际社区组件测试 -->
      <section class="test-section">
        <h2>社区组件测试</h2>
        <div class="component-test">
          <CreateMoment
            :user-id="testUserId"
            :user-name="'测试用户'"
            :user-avatar="''"
            @moment-created="handleTestMomentCreated"
          />
          <div class="divider"></div>
          <MomentList
            :user-id="testUserId"
            :current-user-id="testUserId"
            @moment-updated="handleTestMomentUpdated"
          />
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { communityAPI, mediaAPI, userAPI } from '@/services/api'
import { CreateMoment, MomentList } from '@/components/community'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import BaseIcon from '@/components/base/BaseIcon.vue'

export default {
  name: 'CommunityTestView',
  components: {
    CreateMoment,
    MomentList,
    BaseCard,
    BaseButton,
    BaseIcon
  },
  setup() {
    const communityStatus = ref({ text: '检测中...', color: 'status-pending' })
    const mediaStatus = ref({ text: '检测中...', color: 'status-pending' })
    const userStatus = ref({ text: '检测中...', color: 'status-pending' })
    const testResults = ref([])
    const testUserId = ref(1)

    // 测试微服务连接
    const testConnections = async () => {
      // 测试社区微服务
      try {
        await fetch('http://localhost:8083/api/v1/moments/user/1')
        communityStatus.value = { text: '正常', color: 'status-success' }
        addResult('success', '社区微服务连接', '连接成功')
      } catch (error) {
        communityStatus.value = { text: '连接失败', color: 'status-error' }
        addResult('error', '社区微服务连接', `连接失败: ${error.message}`)
      }

      // 测试媒体微服务
      try {
        await fetch('http://localhost:8081/api/media/types')
        mediaStatus.value = { text: '正常', color: 'status-success' }
        addResult('success', '媒体微服务连接', '连接成功')
      } catch (error) {
        mediaStatus.value = { text: '连接失败', color: 'status-error' }
        addResult('error', '媒体微服务连接', `连接失败: ${error.message}`)
      }

      // 测试用户微服务
      try {
        await fetch('http://localhost:8082/api/v1/users/1')
        userStatus.value = { text: '正常', color: 'status-success' }
        addResult('success', '用户微服务连接', '连接成功')
      } catch (error) {
        userStatus.value = { text: '连接失败', color: 'status-error' }
        addResult('error', '用户微服务连接', `连接失败: ${error.message}`)
      }
    }

    // 测试创建动态
    const testCreateMoment = async () => {
      try {
        const result = await communityAPI.createMoment(
          testUserId.value,
          `这是一个测试动态 - ${new Date().toLocaleString()}`,
          []
        )
        addResult('success', '创建动态测试', '动态创建成功', result)
      } catch (error) {
        addResult('error', '创建动态测试', `创建失败: ${error.message}`)
      }
    }

    // 测试获取动态列表
    const testGetMoments = async () => {
      try {
        const result = await communityAPI.getMomentsByUser(testUserId.value)
        addResult('success', '获取动态列表测试', `获取成功，共${result.length}条动态`, result)
      } catch (error) {
        addResult('error', '获取动态列表测试', `获取失败: ${error.message}`)
      }
    }

    // 测试媒体上传
    const testUploadMedia = async () => {
      // 创建一个测试图片
      const canvas = document.createElement('canvas')
      canvas.width = 200
      canvas.height = 200
      const ctx = canvas.getContext('2d')
      ctx.fillStyle = '#f0f0f0'
      ctx.fillRect(0, 0, 200, 200)
      ctx.fillStyle = '#666'
      ctx.font = '20px Arial'
      ctx.textAlign = 'center'
      ctx.fillText('测试图片', 100, 100)

      canvas.toBlob(async (blob) => {
        try {
          const file = new File([blob], 'test-image.png', { type: 'image/png' })
          const result = await mediaAPI.uploadFile(file)
          addResult('success', '媒体上传测试', '上传成功', result)
        } catch (error) {
          addResult('error', '媒体上传测试', `上传失败: ${error.message}`)
        }
      }, 'image/png')
    }

    // 添加测试结果
    const addResult = (type, title, message, data = null) => {
      testResults.value.unshift({
        type,
        title,
        message,
        data,
        timestamp: new Date()
      })

      // 限制结果数量
      if (testResults.value.length > 20) {
        testResults.value = testResults.value.slice(0, 20)
      }
    }

    // 清空测试结果
    const clearResults = () => {
      testResults.value = []
    }

    // 获取类型对应的图标
    const getIconForType = (type) => {
      const icons = {
        success: 'check-circle',
        error: 'x-circle',
        info: 'info',
        warning: 'alert-triangle'
      }
      return icons[type] || 'info'
    }

    // 格式化时间
    const formatTime = (date) => {
      return date.toLocaleTimeString()
    }

    // 处理测试动态创建
    const handleTestMomentCreated = (moment) => {
      addResult('success', '组件测试', '通过CreateMoment组件成功创建动态', moment)
    }

    // 处理测试动态更新
    const handleTestMomentUpdated = (update) => {
      addResult('info', '组件测试', '动态列表更新', update)
    }

    // 页面加载时测试连接
    onMounted(() => {
      testConnections()
    })

    return {
      communityStatus,
      mediaStatus,
      userStatus,
      testResults,
      testUserId,
      testConnections,
      testCreateMoment,
      testGetMoments,
      testUploadMedia,
      clearResults,
      getIconForType,
      formatTime,
      handleTestMomentCreated,
      handleTestMomentUpdated
    }
  }
}
</script>

<style scoped>
.community-test {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

.test-header {
  text-align: center;
  margin-bottom: 3rem;
}

.test-header h1 {
  color: var(--color-gray-800);
  margin-bottom: 0.5rem;
}

.test-header p {
  color: var(--color-gray-600);
  margin: 0;
}

.test-sections {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.test-section {
  background-color: var(--color-white);
  padding: 2rem;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.test-section h2 {
  color: var(--color-gray-800);
  margin: 0 0 1.5rem 0;
  font-size: 1.5rem;
  font-weight: 600;
}

.status-card,
.results-card {
  padding: 1.5rem;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--color-gray-100);
}

.status-item:last-child {
  border-bottom: none;
  margin-bottom: 1rem;
}

.status-success {
  color: var(--color-success-600);
  font-weight: 500;
}

.status-error {
  color: var(--color-error-600);
  font-weight: 500;
}

.status-pending {
  color: var(--color-gray-500);
  font-weight: 500;
}

.test-actions {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.no-results {
  text-align: center;
  color: var(--color-gray-500);
  padding: 2rem;
}

.results-list {
  max-height: 400px;
  overflow-y: auto;
}

.result-item {
  padding: 1rem;
  border-radius: 8px;
  margin-bottom: 1rem;
  border-left: 4px solid;
}

.result-item.success {
  border-left-color: var(--color-success-500);
  background-color: var(--color-success-50);
}

.result-item.error {
  border-left-color: var(--color-error-500);
  background-color: var(--color-error-50);
}

.result-item.info {
  border-left-color: var(--color-primary-500);
  background-color: var(--color-primary-50);
}

.result-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.5rem;
}

.result-title {
  font-weight: 600;
  color: var(--color-gray-800);
}

.result-time {
  margin-left: auto;
  font-size: 0.875rem;
  color: var(--color-gray-500);
}

.result-content p {
  margin: 0 0 0.5rem 0;
  color: var(--color-gray-700);
}

.result-data {
  background-color: var(--color-gray-100);
  border-radius: 6px;
  padding: 0.75rem;
  font-size: 0.875rem;
  color: var(--color-gray-700);
  overflow-x: auto;
  margin: 0;
}

.component-test {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.divider {
  height: 1px;
  background-color: var(--color-gray-200);
  margin: 1rem 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .community-test {
    padding: 1rem;
  }

  .test-section {
    padding: 1.5rem;
  }

  .test-section h2 {
    font-size: 1.25rem;
  }

  .test-actions {
    flex-direction: column;
  }

  .result-header {
    flex-wrap: wrap;
  }

  .result-time {
    width: 100%;
    margin-left: 0;
    margin-top: 0.25rem;
  }
}
</style>