<template>
  <div class="pet-status-card">
    <!-- 宠物头像区域 -->
    <div class="pet-avatar-section">
      <div class="pet-avatar-wrapper">
        <el-avatar :size="80" :src="petInfo.avatar_url">
          {{ petInfo.name ? petInfo.name.charAt(0) : '?' }}
        </el-avatar>
      </div>
      <div class="pet-name">{{ petInfo.name }}</div>
    </div>

    <!-- 状态列表 -->
    <div class="status-list">
      <div class="status-header">
        <span class="status-title">宠物状态</span>
        <div class="status-actions">
          <el-button
            type="info"
            size="small"
            text
            @click="initDefaultStatuses"
            :loading="initializing"
            v-if="statusList.length === 0"
          >
            <el-icon><Refresh /></el-icon>
            初始化默认状态
          </el-button>
          <el-button
            type="primary"
            size="small"
            text
            @click="showAddStatusDialog = true"
          >
            <el-icon><Plus /></el-icon>
            添加
          </el-button>
        </div>
      </div>

      <div v-if="loading" class="status-loading">
        <el-skeleton :rows="3" animated />
      </div>

      <div v-else-if="statusList.length === 0" class="status-empty">
        <el-empty description="暂无状态信息" :image-size="60">
          <el-button size="small" @click="showAddStatusDialog = true">
            添加状态
          </el-button>
        </el-empty>
      </div>

      <div v-else class="status-items">
        <div
          v-for="status in statusList"
          :key="status.statusId"
          class="status-item"
        >
          <div class="status-icon">
            {{ getStatusIcon(status.statusName) }}
          </div>
          <div class="status-info">
            <div class="status-name-row">
              <span class="status-name" @click="startEditStatus(status)">
                {{ status.statusName }}
              </span>
              <el-dropdown trigger="click" @command="handleStatusCommand($event, status)">
                <el-button size="small" text type="primary">
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="edit">
                      <el-icon><Edit /></el-icon>
                      修改名称
                    </el-dropdown-item>
                    <el-dropdown-item command="delete" divided>
                      <el-icon><Delete /></el-icon>
                      删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            <div class="status-value-row">
              <el-input
                v-if="Number(editingStatusId) === Number(status.statusId)"
                v-model="editingValue"
                size="small"
                placeholder="点击添加内容"
                @blur="saveStatusValue(status.statusId)"
                @keyup.enter="saveStatusValue(status.statusId)"
                ref="valueInput"
              />
              <span
                v-else
                class="status-value"
                :class="{ 'empty-value': !status.statusValue }"
                @click="startEditValue(status)"
              >
                {{ status.statusValue || '点击添加内容' }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加状态对话框 -->
    <el-dialog
      v-model="showAddStatusDialog"
      title="添加状态"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form :model="newStatusForm" label-position="top">
        <el-form-item label="状态名称" required>
          <el-input
            v-model="newStatusForm.statusName"
            placeholder="例如：主食品牌、零食类型"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddStatusDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAddStatus" :loading="addingStatus">
          添加
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑状态名称对话框 -->
    <el-dialog
      v-model="showEditNameDialog"
      title="修改状态名称"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form :model="editNameForm" label-position="top">
        <el-form-item label="状态名称" required>
          <el-input
            v-model="editNameForm.statusName"
            placeholder="状态名称"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditNameDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateStatusName" :loading="updatingStatus">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import { Plus, MoreFilled, Edit, Delete, Refresh } from '@element-plus/icons-vue'
import { getUserStatuses, createStatus, updateStatusName, updateStatusValue, deleteStatus } from '@/api/status'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({
  petId: {
    type: Number,
    required: true
  },
  petInfo: {
    type: Object,
    default: () => ({})
  }
})

// 状态数据
const statusList = ref([])
const loading = ref(false)
const initializing = ref(false)
const showAddStatusDialog = ref(false)
const showEditNameDialog = ref(false)
const addingStatus = ref(false)
const updatingStatus = ref(false)

// 编辑状态
const editingStatusId = ref(null)
const editingValue = ref('')

// 新增状态表单
const newStatusForm = ref({
  statusName: ''
})

// 编辑名称表单
const editNameForm = ref({
  statusId: null,
  statusName: ''
})

// 默认状态列表
const defaultStatusNames = [
  '主粮', '零食', '水源', '地理位置', '居所概况', '家庭成员', '疾病', '受伤', '怀孕'
]

// 初始化默认状态
const initDefaultStatuses = async () => {
  initializing.value = true
  try {
    let successCount = 0
    let skipCount = 0
    for (const statusName of defaultStatusNames) {
      try {
        const res = await createStatus({
          petId: props.petId,
          statusName: statusName
        })
        if (isSuccess(res)) {
          successCount++
        }
      } catch (e) {
        // 如果状态已存在（错误消息包含"已存在"），则跳过
        const errorData = e.response?.data
        if (errorData && (errorData.message?.includes('已存在') || !isSuccess(errorData))) {
          skipCount++
          console.log(`状态 "${statusName}" 已存在，跳过`)
        } else {
          console.warn(`创建状态 "${statusName}" 失败:`, e)
        }
      }
    }
    if (successCount > 0) {
      ElMessage.success(`成功创建 ${successCount} 个状态`)
    }
    if (skipCount > 0) {
      ElMessage.info(`${skipCount} 个状态已存在`)
    }
    loadStatusList()
  } catch (error) {
    console.error('初始化默认状态失败:', error)
    ElMessage.error('初始化失败')
  } finally {
    initializing.value = false
  }
}

// 检查响应是否成功（兼容 success 和 code 两种格式）
const isSuccess = (res) => {
  if (res.code !== undefined) {
    return res.code === 200
  }
  return res.success === true
}

// 获取响应消息
const getMessage = (res) => {
  return res.message || res.msg || '操作成功'
}

// 加载状态列表
const loadStatusList = async () => {
  if (!props.petId) return

  loading.value = true
  try {
    const res = await getUserStatuses(props.petId)
    if (res.data) {
      statusList.value = res.data
    }
  } catch (error) {
    console.error('加载状态列表失败:', error)
    ElMessage.error('加载状态列表失败')
  } finally {
    loading.value = false
  }
}

// 获取状态图标
const getStatusIcon = (statusName) => {
  const iconMap = {
    '主粮': '🍚',
    '零食': '🍖',
    '水源': '💧',
    '地理位置': '📍',
    '居所概况': '🏠',
    '家庭成员': '👨‍👩‍👧',
    '疾病': '💊',
    '受伤': '🩹',
    '怀孕': '🐱'
  }
  return iconMap[statusName] || '📋'
}

// 添加状态
const handleAddStatus = async () => {
  if (!newStatusForm.value.statusName.trim()) {
    ElMessage.warning('请输入状态名称')
    return
  }

  addingStatus.value = true
  try {
    const res = await createStatus({
      petId: props.petId,
      statusName: newStatusForm.value.statusName.trim()
    })
    if (isSuccess(res)) {
      ElMessage.success('添加成功')
      showAddStatusDialog.value = false
      newStatusForm.value.statusName = ''
      loadStatusList()
    } else {
      ElMessage.error(getMessage(res) || '添加失败')
    }
  } catch (error) {
    console.error('添加状态失败:', error)
    ElMessage.error('添加失败')
  } finally {
    addingStatus.value = false
  }
}

// 开始编辑状态名称
const startEditStatus = (status) => {
  editNameForm.value = {
    statusId: status.statusId,
    statusName: status.statusName
  }
  showEditNameDialog.value = true
}

// 更新状态名称
const handleUpdateStatusName = async () => {
  if (!editNameForm.value.statusName.trim()) {
    ElMessage.warning('请输入状态名称')
    return
  }

  updatingStatus.value = true
  try {
    const res = await updateStatusName(
      editNameForm.value.statusId,
      editNameForm.value.statusName.trim()
    )
    if (isSuccess(res)) {
      ElMessage.success('修改成功')
      showEditNameDialog.value = false
      loadStatusList()
    } else {
      ElMessage.error(getMessage(res) || '修改失败')
    }
  } catch (error) {
    console.error('修改状态名称失败:', error)
    ElMessage.error('修改失败')
  } finally {
    updatingStatus.value = false
  }
}

// 状态操作菜单
const handleStatusCommand = async (command, status) => {
  if (command === 'edit') {
    startEditStatus(status)
  } else if (command === 'delete') {
    try {
      await ElMessageBox.confirm(
        `确定要删除状态"${status.statusName}"吗？删除后相关记录也会被清除。`,
        '警告',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
      const res = await deleteStatus(status.statusId)
      if (isSuccess(res)) {
        ElMessage.success('删除成功')
        loadStatusList()
      } else {
        ElMessage.error(getMessage(res) || '删除失败')
      }
    } catch (error) {
      if (error !== 'cancel') {
        console.error('删除状态失败:', error)
        ElMessage.error('删除失败')
      }
    }
  }
}

// 开始编辑状态值
const startEditValue = (status) => {
  editingStatusId.value = Number(status.statusId)
  editingValue.value = status.statusValue || ''
  nextTick(() => {
    const input = document.querySelector('.status-value-row .el-input input')
    if (input) {
      input.focus()
    }
  })
}

// 保存状态值
const saveStatusValue = async (statusId) => {
  if (!editingValue.value.trim()) {
    editingStatusId.value = null
    return
  }

  try {
    const res = await updateStatusValue(statusId, editingValue.value.trim())
    if (isSuccess(res)) {
      ElMessage.success('保存成功')
      loadStatusList()
    } else {
      ElMessage.error(getMessage(res) || '保存失败')
    }
  } catch (error) {
    console.error('保存状态值失败:', error)
    ElMessage.error('保存失败')
  } finally {
    editingStatusId.value = null
    editingValue.value = ''
  }
}

// 监听 petId 变化
watch(() => props.petId, () => {
  loadStatusList()
}, { immediate: true })

// 暴露刷新方法
defineExpose({
  refresh: loadStatusList
})
</script>

<style scoped>
.pet-status-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 1.5rem;
  width: 100%;
  max-width: 320px;
}

.pet-avatar-section {
  text-align: center;
  padding-bottom: 1rem;
  border-bottom: 1px solid #e2e8f0;
  margin-bottom: 1rem;
}

.pet-avatar-wrapper {
  display: inline-block;
  position: relative;
}

.pet-avatar-wrapper::after {
  content: '';
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 12px;
  height: 12px;
  background: #22c55e;
  border-radius: 50%;
  border: 2px solid white;
}

.pet-name {
  margin-top: 0.75rem;
  font-size: 1.25rem;
  font-weight: 600;
  color: #1f2937;
}

.status-list {
  width: 100%;
}

.status-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.status-actions {
  display: flex;
  gap: 0.5rem;
}

.status-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.status-loading {
  padding: 1rem 0;
}

.status-empty {
  padding: 1rem 0;
}

.status-items {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.status-item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.75rem;
  background: #f9fafb;
  border-radius: 8px;
  transition: all 0.2s ease;
  flex: 1 1 calc(33.333% - 0.75rem);
  min-width: 140px;
}

.status-item:hover {
  background: #f3f4f6;
  transform: translateY(-2px);
}

.status-icon {
  font-size: 1.25rem;
  line-height: 1;
  flex-shrink: 0;
}

.status-info {
  flex: 1;
  min-width: 0;
}

.status-name-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.25rem;
}

.status-name {
  font-size: 0.875rem;
  font-weight: 500;
  color: #374151;
  cursor: pointer;
}

.status-name:hover {
  color: #f97316;
}

.status-value-row {
  font-size: 0.8125rem;
}

.status-value {
  color: #6b7280;
  cursor: pointer;
  display: block;
  padding: 0.25rem 0;
}

.status-value.empty-value {
  color: #9ca3af;
  font-style: italic;
}

.status-value:hover {
  color: #f97316;
}

/* 输入框样式 */
.status-value-row :deep(.el-input) {
  width: 100%;
}

.status-value-row :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #f97316 inset;
  background: white;
  padding: 2px 8px;
}

.status-value-row :deep(.el-input__inner) {
  font-size: 0.8125rem;
  color: #374151;
}
</style>
