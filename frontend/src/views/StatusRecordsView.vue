<template>
  <div class="status-records-view">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">
          <el-icon class="title-icon"><DocumentAdd /></el-icon>
          宠物状态记录
        </h1>
        <p class="page-description">管理宠物的长期状态，如生病、怀孕、发情等</p>
      </div>
      <div class="header-right">
        <el-button @click="$router.go(-1)" icon="ArrowLeft">
          返回
        </el-button>
      </div>
    </div>

    <!-- 宠物选择区域 -->
    <div class="pet-selection-section">
      <div class="section-title">选择宠物</div>
      <div class="pet-grid">
        <div
          v-for="pet in userPets"
          :key="pet.id || pet.petId"
          class="pet-card"
          :class="{ active: selectedPetIds.includes(pet.id || pet.petId) }"
          @click="togglePetSelection(pet.id || pet.petId)"
        >
          <el-avatar :size="60" :src="pet.avatar_url">
            {{ pet.name.charAt(0) }}
          </el-avatar>
          <div class="pet-name">{{ pet.name }}</div>
          <div class="pet-status-count">
            {{ getPetStatusCount(pet.id || pet.petId) }} 条记录
          </div>
        </div>
      </div>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <div class="action-left">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="handleDateRangeChange"
          size="default"
          style="width: 280px"
        />
      </div>

      <div class="action-right">
        <el-button type="success" @click="showAddDialog = true" icon="Plus">
          添加状态记录
        </el-button>
        <el-button @click="refreshData" icon="Refresh">
          刷新
        </el-button>
      </div>
    </div>

    <!-- 状态记录列表 -->
    <div class="records-section">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="5" animated />
      </div>

      <!-- 空状态 -->
      <div v-else-if="filteredRecords.length === 0" class="empty-state">
        <el-empty description="暂无状态记录">
          <el-button type="primary" @click="showAddDialog = true" icon="Plus">
            创建第一条状态记录
          </el-button>
        </el-empty>
      </div>

      <!-- 记录时间线 -->
      <div v-else class="timeline-container">
        <div
          v-for="(group, date) in groupedRecords"
          :key="date"
          class="timeline-group"
        >
          <div class="timeline-date">
            <div class="date-badge">{{ formatDate(date) }}</div>
          </div>

          <div class="timeline-items">
            <div
              v-for="record in group"
              :key="record.statusRecordId"
              class="record-card"
              @click="editRecord(record)"
            >
              <div class="record-header">
                <div class="pet-info">
                  <el-avatar :size="32" :src="getPetInfo(record.petId).avatar_url">
                    {{ getPetInfo(record.petId).name.charAt(0) }}
                  </el-avatar>
                  <div class="pet-details">
                    <div class="pet-name">{{ getPetInfo(record.petId).name }}</div>
                    <div class="status-type">{{ record.statusName }}</div>
                  </div>
                </div>
                <div class="record-time">
                  {{ formatTime(record.startDate) }}
                  <span v-if="record.endDate" class="record-duration">
                    至 {{ formatTime(record.endDate) }}
                  </span>
                </div>
              </div>

              <div v-if="record.statusDescription" class="record-description">
                <p>{{ record.statusDescription }}</p>
              </div>

              <div v-if="record.mediaFiles && record.mediaFiles.length > 0" class="record-media">
                <div class="media-grid">
                  <div
                    v-for="(media, index) in record.mediaFiles.slice(0, 4)"
                    :key="index"
                    class="media-item"
                    @click.stop
                  >
                    <img v-if="media.fileType === 'IMAGE'" :src="media.fileUrl" :alt="media.fileName" />
                    <div v-else class="file-icon">
                      <i class="fas fa-file"></i>
                    </div>
                  </div>
                  <div v-if="record.mediaFiles.length > 4" class="more-media">
                    +{{ record.mediaFiles.length - 4 }}
                  </div>
                </div>
              </div>

              <div class="record-actions">
                <el-button size="small" @click.stop="editRecord(record)" icon="Edit">
                  编辑
                </el-button>
                <el-button
                  v-if="!record.endDate"
                  size="small"
                  type="warning"
                  @click.stop="stopRecord(record)"
                  icon="VideoPause"
                >
                  停止
                </el-button>
                <el-button size="small" type="danger" @click.stop="deleteRecord(record)" icon="Delete">
                  删除
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加状态记录对话框 -->
    <el-dialog
      v-model="showAddDialog"
      title="添加状态记录"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="addFormRef"
        :model="addForm"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="选择宠物" prop="petId">
          <el-select v-model="addForm.petId" placeholder="请选择宠物" style="width: 100%">
            <el-option
              v-for="pet in userPets"
              :key="pet.id || pet.petId"
              :label="pet.name"
              :value="pet.id || pet.petId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="状态类型" prop="statusId">
          <el-select v-model="addForm.statusId" placeholder="选择状态类型" style="width: 100%">
            <el-option
              v-for="status in userStatuses"
              :key="status.statusId"
              :label="status.statusName"
              :value="status.statusId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker
            v-model="addForm.startDate"
            type="date"
            placeholder="选择开始日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="状态描述">
          <el-input
            v-model="addForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入状态描述..."
          />
        </el-form-item>

        <el-form-item label="上传文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :on-change="handleFileChange"
            :limit="1"
            :file-list="fileList"
            action="#"
            :accept="'image/*,.pdf,.doc,.docx'"
          >
            <el-button icon="Upload">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">
                支持图片、PDF、Word文档，文件大小不超过10MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAddForm" :loading="submitting">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑状态记录对话框 -->
    <el-dialog
      v-model="showEditDialog"
      title="编辑状态记录"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="宠物名称">
          <el-input v-model="editForm.petName" disabled placeholder="宠物名称" />
        </el-form-item>

        <el-form-item label="状态类型" prop="statusId">
          <el-select v-model="editForm.statusId" placeholder="选择状态类型" style="width: 100%">
            <el-option
              v-for="status in userStatuses"
              :key="status.statusId"
              :label="status.statusName"
              :value="status.statusId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker
            v-model="editForm.startDate"
            type="date"
            placeholder="选择开始日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="结束日期">
          <el-date-picker
            v-model="editForm.endDate"
            type="date"
            placeholder="选择结束日期（可选）"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="状态描述">
          <el-input
            v-model="editForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入状态描述..."
          />
        </el-form-item>

        <el-form-item label="更新文件">
          <el-upload
            ref="editUploadRef"
            :auto-upload="false"
            :on-change="handleEditFileChange"
            :limit="1"
            :file-list="editFileList"
            action="#"
            :accept="'image/*,.pdf,.doc,.docx'"
          >
            <el-button icon="Upload">选择新文件</el-button>
            <template #tip>
              <div class="el-upload__tip">
                上传新文件将替换现有文件，支持图片、PDF、Word文档
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="submitEditForm" :loading="submitting">
          更新
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  DocumentAdd,
  Plus,
  Refresh,
  Edit,
  Delete,
  VideoPause,
  ArrowLeft,
  Upload
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { statusApi } from '@/api/status'
import { petApi } from '@/api/pets'

// 路由和认证
const router = useRouter()
const authStore = useAuthStore()

// 响应式数据
const userPets = ref([])
const userStatuses = ref([])
const statusRecords = ref([])
const loading = ref(false)
const submitting = ref(false)

// 表单数据
const addForm = ref({
  petId: null,
  statusId: null,
  startDate: '',
  description: ''
})

const editForm = ref({
  statusRecordId: null,
  petId: null,
  petName: '',
  statusId: null,
  startDate: '',
  endDate: '',
  description: ''
})

// 对话框状态
const showAddDialog = ref(false)
const showEditDialog = ref(false)

// 文件上传
const fileList = ref([])
const editFileList = ref([])

// 筛选条件
const selectedPetIds = ref([])
const dateRange = ref([])

// 表单验证规则
const formRules = {
  petId: [{ required: true, message: '请选择宠物', trigger: 'change' }],
  statusId: [{ required: true, message: '请选择状态类型', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'blur' }]
}

// 计算属性
const filteredRecords = computed(() => {
  let filtered = statusRecords.value

  // 按选择的宠物过滤
  if (selectedPetIds.value.length > 0) {
    filtered = filtered.filter(record => selectedPetIds.value.includes(record.petId))
  }

  // 按日期范围过滤
  if (dateRange.value && dateRange.value.length === 2) {
    const [startDate, endDate] = dateRange.value
    filtered = filtered.filter(record => {
      const recordDate = record.startDate
      return recordDate >= startDate && recordDate <= endDate
    })
  }

  return filtered.sort((a, b) => new Date(b.startDate) - new Date(a.startDate))
})

const groupedRecords = computed(() => {
  const groups = {}
  filteredRecords.value.forEach(record => {
    const date = record.startDate.split('T')[0] // 只取日期部分
    if (!groups[date]) {
      groups[date] = []
    }
    groups[date].push(record)
  })
  return groups
})

const currentUserId = computed(() => authStore.user?.userId)

// 方法
const loadUserPets = async () => {
  try {
    const response = await petApi.getUserPets(currentUserId.value)
    if (response && response.data) {
      userPets.value = Array.isArray(response.data) ? response.data : []
    } else {
      userPets.value = []
    }

    // 默认选择所有宠物
    if (userPets.value.length > 0) {
      selectedPetIds.value = userPets.value.map(pet => pet.id || pet.petId)
    }
  } catch (error) {
    console.error('加载宠物列表失败:', error)
    userPets.value = []
  }
}

const loadUserStatuses = async () => {
  try {
    const response = await statusApi.getUserStatuses(currentUserId.value)
    if (response && response.data) {
      userStatuses.value = Array.isArray(response.data) ? response.data : []
    } else if (Array.isArray(response)) {
      userStatuses.value = response
    } else {
      userStatuses.value = []
    }
  } catch (error) {
    console.error('加载状态类型失败:', error)
    userStatuses.value = []
  }
}

const loadStatusRecords = async () => {
  if (selectedPetIds.value.length === 0) {
    statusRecords.value = []
    return
  }

  try {
    loading.value = true

    const promises = selectedPetIds.value.map(petId =>
      statusApi.getStatusRecords(petId, {
        startDate: dateRange.value && dateRange.value[0] ? new Date(dateRange.value[0]).toISOString() : null,
        endDate: dateRange.value && dateRange.value[1] ? new Date(dateRange.value[1]).toISOString() : null
      })
    )

    const responses = await Promise.all(promises)

    // 合并所有响应的数据
    let allRecords = []
    responses.forEach(response => {
      if (Array.isArray(response)) {
        allRecords = allRecords.concat(response)
      } else if (response && Array.isArray(response.data)) {
        allRecords = allRecords.concat(response.data)
      }
    })

    statusRecords.value = allRecords
  } catch (error) {
    console.error('加载状态记录失败:', error)
    statusRecords.value = []
    ElMessage.error('加载状态记录失败')
  } finally {
    loading.value = false
  }
}

const refreshData = async () => {
  await Promise.all([
    loadUserStatuses(),
    loadStatusRecords()
  ])
}

const togglePetSelection = (petId) => {
  const index = selectedPetIds.value.indexOf(petId)
  if (index > -1) {
    selectedPetIds.value.splice(index, 1)
  } else {
    selectedPetIds.value.push(petId)
  }
  loadStatusRecords()
}

const handleDateRangeChange = () => {
  loadStatusRecords()
}

const getPetInfo = (petId) => {
  return userPets.value.find(pet => (pet.id || pet.petId) === petId) || {}
}

const getPetStatusCount = (petId) => {
  return statusRecords.value.filter(record => record.petId === petId).length
}

const formatDate = (dateStr) => {
  const date = new Date(dateStr)
  const today = new Date()
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)

  if (date.toDateString() === today.toDateString()) {
    return '今天'
  } else if (date.toDateString() === yesterday.toDateString()) {
    return '昨天'
  } else {
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      weekday: 'long'
    })
  }
}

const formatTime = (dateStr) => {
  const date = new Date(dateStr)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

const handleFileChange = (file, fileList) => {
  fileList.value = fileList
  if (file.raw) {
    addForm.value.file = file.raw
  }
}

const handleEditFileChange = (file, fileList) => {
  editFileList.value = fileList
  if (file.raw) {
    editForm.value.file = file.raw
  }
}

const submitAddForm = async () => {
  if (!addFormRef.value) return

  try {
    await addFormRef.value.validate()
    submitting.value = true

    const recordData = {
      statusId: addForm.value.statusId,
      petId: addForm.value.petId,
      startDate: addForm.value.startDate,
      statusDescription: addForm.value.description,
      userId: currentUserId.value
    }

    if (addForm.value.file) {
      recordData.file = addForm.value.file
    }

    await statusApi.createStatusRecord(recordData)

    ElMessage.success('状态记录创建成功！')
    showAddDialog.value = false

    // 重置表单
    addForm.value = {
      petId: null,
      statusId: null,
      startDate: '',
      description: ''
    }
    fileList.value = []

    // 重新加载数据
    await loadStatusRecords()
  } catch (error) {
    console.error('创建状态记录失败:', error)
    ElMessage.error('创建状态记录失败: ' + (error.response?.data?.message || error.message))
  } finally {
    submitting.value = false
  }
}

const editRecord = (record) => {
  const pet = getPetInfo(record.petId)

  editForm.value = {
    statusRecordId: record.statusRecordId,
    petId: record.petId,
    petName: pet.name || '未知宠物',
    statusId: record.statusId,
    startDate: record.startDate,
    endDate: record.endDate,
    description: record.statusDescription || ''
  }

  editFileList.value = []
  showEditDialog.value = true
}

const submitEditForm = async () => {
  if (!editFormRef.value) return

  try {
    await editFormRef.value.validate()
    submitting.value = true

    const updateData = {
      statusDescription: editForm.value.description,
      startDate: editForm.value.startDate,
      endDate: editForm.value.endDate
    }

    if (editForm.value.file) {
      updateData.file = editForm.value.file
      updateData.userId = currentUserId.value
    }

    await statusApi.updateStatusRecord(editForm.value.statusRecordId, updateData)

    ElMessage.success('状态记录更新成功！')
    showEditDialog.value = false

    // 重新加载数据
    await loadStatusRecords()
  } catch (error) {
    console.error('更新状态记录失败:', error)
    ElMessage.error('更新状态记录失败: ' + (error.response?.data?.message || error.message))
  } finally {
    submitting.value = false
  }
}

const stopRecord = async (record) => {
  try {
    await ElMessageBox.confirm(
      `确定要停止 "${record.statusName}" 状态记录吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await statusApi.stopStatusRecord(record.statusRecordId, new Date().toISOString().split('T')[0])

    ElMessage.success('状态记录已停止')
    await loadStatusRecords()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('停止状态记录失败:', error)
      ElMessage.error('停止状态记录失败')
    }
  }
}

const deleteRecord = async (record) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除 "${record.statusName}" 状态记录吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await statusApi.deleteStatusRecord(record.statusRecordId)

    ElMessage.success('状态记录已删除')
    await loadStatusRecords()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除状态记录失败:', error)
      ElMessage.error('删除状态记录失败')
    }
  }
}

// 生命周期
onMounted(async () => {
  if (!authStore.isAuthenticated) {
    router.push('/login')
    return
  }

  await loadUserPets()
  await loadUserStatuses()
  await loadStatusRecords()
})

// 监听宠物选择变化
watch(selectedPetIds, () => {
  loadStatusRecords()
}, { deep: true })
</script>

<style scoped>
.status-records-view {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 20px;
}

.page-header {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.header-left .page-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.title-icon {
  color: #409eff;
}

.page-description {
  margin: 8px 0 0 0;
  color: #909399;
  font-size: 14px;
}

.pet-selection-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}

.pet-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 16px;
}

.pet-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: white;
}

.pet-card:hover {
  border-color: #409eff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.pet-card.active {
  border-color: #409eff;
  background-color: #ecf5ff;
}

.pet-card .pet-name {
  margin: 8px 0 4px 0;
  font-weight: 500;
  color: #303133;
}

.pet-status-count {
  font-size: 12px;
  color: #909399;
}

.action-bar {
  background: white;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.records-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.loading-container {
  padding: 40px;
}

.empty-state {
  padding: 60px 0;
}

.timeline-container {
  position: relative;
}

.timeline-group {
  margin-bottom: 32px;
  position: relative;
}

.timeline-group:not(:last-child)::after {
  content: '';
  position: absolute;
  left: 120px;
  top: 60px;
  bottom: -32px;
  width: 2px;
  background: #e4e7ed;
}

.timeline-date {
  margin-bottom: 16px;
}

.date-badge {
  display: inline-block;
  background: #409eff;
  color: white;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
}

.timeline-items {
  margin-left: 140px;
}

.record-card {
  background: #fafafa;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.record-card:hover {
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.pet-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pet-details .pet-name {
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.status-type {
  font-size: 12px;
  color: #409eff;
  background: #ecf5ff;
  padding: 2px 8px;
  border-radius: 12px;
  display: inline-block;
}

.record-time {
  font-size: 14px;
  color: #909399;
  text-align: right;
}

.record-duration {
  display: block;
  margin-top: 4px;
  color: #f56c6c;
}

.record-description {
  margin: 12px 0;
}

.record-description p {
  margin: 0;
  color: #606266;
  line-height: 1.6;
}

.record-media {
  margin: 12px 0;
}

.media-grid {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.media-item {
  width: 60px;
  height: 60px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #e4e7ed;
}

.media-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.file-icon {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #909399;
}

.more-media {
  width: 60px;
  height: 60px;
  border-radius: 6px;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 12px;
}

.record-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  justify-content: flex-end;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .status-records-view {
    padding: 12px;
  }

  .page-header {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }

  .action-bar {
    flex-direction: column;
    gap: 16px;
  }

  .pet-grid {
    grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  }

  .timeline-items {
    margin-left: 0;
  }

  .record-header {
    flex-direction: column;
    gap: 8px;
  }

  .record-time {
    text-align: left;
  }
}
</style>
