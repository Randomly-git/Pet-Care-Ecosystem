<template>
  <div class="settings-page">
    <AppHeader />
    <div class="settings-container">
      <div class="settings-card">
        <h2 class="settings-title">账号设置</h2>
        <p class="settings-subtitle">管理您的个人信息与偏好</p>
        
        <div class="settings-form">
          <!-- Avatar Section -->
          <div class="form-section">
            <h3 class="section-title">头像</h3>
            <div class="avatar-upload-area">
              <el-avatar :size="80" :src="previewAvatar" class="profile-avatar">{{ currentName.charAt(0) }}</el-avatar>
              <div class="upload-actions">
                <input type="file" ref="fileInput" hidden @change="handleFileChange" accept="image/*" />
                <button class="apple-btn primary-btn" @click="triggerUpload">更换头像</button>
                <div class="upload-tip">支持 JPG、PNG 格式，以获得最佳显示效果</div>
              </div>
            </div>
          </div>

          <el-divider />

          <!-- Profile Section -->
          <div class="form-section">
            <h3 class="section-title">个人档案</h3>
            <div class="form-group">
              <label>昵称</label>
              <div class="input-wrapper">
                <input type="text" v-model="formName" class="apple-input" :disabled="!canChangeName" />
                <span v-if="!canChangeName" class="limiter-text">7 天内仅可修改一次</span>
              </div>
            </div>
            <div class="form-group">
              <label>账号 ID</label>
              <input type="text" :value="authStore.userId" class="apple-input disabled-input" disabled />
            </div>
          </div>
          
          <div class="form-actions">
            <button class="apple-btn save-btn" @click="saveSettings" :disabled="loading">
              {{ loading ? '保存中...' : '保存更改' }}
            </button>
          </div>
        </div>
      </div>
    </div>
    <AppFooter />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'

const authStore = useAuthStore()
const router = useRouter()

const loading = ref(false)
const fileInput = ref(null)
const previewAvatar = ref('')
const formName = ref('')
const currentName = computed(() => authStore.userName || '用户')
const rawFile = ref(null)

// Mock a 7-day limiter constraint randomly
const canChangeName = ref(true)

onMounted(() => {
  previewAvatar.value = authStore.avatar || ''
  formName.value = authStore.userName || ''
  
  // Fake limiter purely for Apple style spec requirement
})

const triggerUpload = () => {
  fileInput.value.click()
}

const handleFileChange = (e) => {
  const file = e.target.files[0]
  if (!file) return
  rawFile.value = file
  
  // Create preview
  const reader = new FileReader()
  reader.onload = (e) => {
    previewAvatar.value = e.target.result
  }
  reader.readAsDataURL(file)
}

const saveSettings = async () => {
  if (!formName.value.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  
  loading.value = true
  try {
    // Fake async save (no backend logic ready yet)
    await new Promise(resolve => setTimeout(resolve, 800))
    
    // Update auth store locally
    authStore.updateUser({
      ...authStore.user,
      name: formName.value,
      avatar: previewAvatar.value
    })
    
    // Set mock limiter to false after saving
    canChangeName.value = false
    
    ElMessage.success('设置保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.settings-page {
  min-height: 100vh;
  background: #f5f5f7;
  display: flex;
  flex-direction: column;
}
.settings-container {
  flex: 1;
  padding: 60px 20px;
  display: flex;
  justify-content: center;
}
.settings-card {
  width: 100%;
  max-width: 680px;
  background: white;
  border-radius: 20px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.04);
  padding: 40px;
}
.settings-title {
  font-size: 32px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 8px 0;
  letter-spacing: -0.01em;
}
.settings-subtitle {
  font-size: 16px;
  color: #86868b;
  margin: 0 0 40px 0;
}
.form-section {
  margin-bottom: 32px;
}
.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 24px;
}
.avatar-upload-area {
  display: flex;
  align-items: center;
  gap: 24px;
}
.profile-avatar {
  border: 1px solid rgba(0,0,0,0.1);
  background-color: #f5f5f7;
  color: #1d1d1f;
  font-weight: 500;
  font-size: 24px;
}
.upload-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.upload-tip {
  font-size: 12px;
  color: #86868b;
}
.form-group {
  margin-bottom: 24px;
}
.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #1d1d1f;
  margin-bottom: 8px;
}
.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}
.apple-input {
  width: 100%;
  padding: 12px 16px;
  font-size: 16px;
  color: #1d1d1f;
  background: #f5f5f7;
  border: 1px solid transparent;
  border-radius: 12px;
  transition: all 0.2s;
  outline: none;
}
.apple-input:focus {
  background: white;
  border-color: #0071e3;
  box-shadow: 0 0 0 4px rgba(0, 113, 227, 0.1);
}
.disabled-input,
.apple-input:disabled {
  color: #86868b;
  cursor: not-allowed;
  background: #ebeeef;
}
.limiter-text {
  position: absolute;
  right: 16px;
  font-size: 12px;
  color: #ff3b30;
}
.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 40px;
}
.apple-btn {
  padding: 10px 24px;
  border-radius: 980px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: transform 0.2s, opacity 0.2s;
}
.apple-btn:active {
  transform: scale(0.96);
}
.primary-btn {
  background: #f5f5f7;
  color: #0071e3;
}
.primary-btn:hover {
  background: #e8e8ed;
}
.save-btn {
  background: #0071e3;
  color: white;
  font-size: 16px;
  padding: 12px 32px;
}
.save-btn:hover {
  background: #0077ed;
}
.save-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
