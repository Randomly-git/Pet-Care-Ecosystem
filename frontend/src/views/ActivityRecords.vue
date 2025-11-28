<template>
  <div class="activity-records-page">
    <!-- 使用统一的布局头部 -->
    <AppHeader />

    <!-- 页面标题区域 -->
    <div class="page-header">
      <div class="container">
        <div class="header-content">
          <h1 class="page-title">
            <span class="title-icon">🐾</span>
            活动记录
          </h1>
          <p class="page-subtitle">记录和管理宠物的日常活动</p>
        </div>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧边栏 -->
      <div class="left-sidebar">
        <!-- 用户信息 -->
        <div class="user-info-simple">
          <el-avatar :size="50" :src="authStore.user?.avatar_url" class="user-avatar">
            {{ authStore.userName.charAt(0) || 'U' }}
          </el-avatar>
          <div class="user-details">
            <div class="user-name">{{ authStore.userName }}</div>
            <div class="user-species">宠物主人</div>
          </div>
        </div>

        <!-- 宠物选择器 -->
        <div class="pet-selector-section">
          <div class="section-title">我的宠物</div>
          <div class="pet-list">
            <div
              v-for="pet in userPets"
              :key="pet.id"
              class="pet-item"
              :class="{ active: selectedPetIds.includes(pet.id) }"
              @click="togglePetSelection(pet.id)"
            >
              <el-avatar :size="40" :src="pet.avatar_url">
                {{ pet.name.charAt(0) }}
              </el-avatar>
              <div class="pet-info">
                <div class="pet-name">{{ pet.name }}</div>
                <div class="pet-type">{{ pet.type }} - {{ pet.breed }}</div>
              </div>
              <div class="pet-count">
                <el-badge :value="getPetActivityCount(pet.id)" :max="99" type="primary" />
              </div>
            </div>
          </div>
        </div>

        <!-- 统计信息 -->
        <div class="stats-section">
          <div class="section-title">统计概览</div>
          <div class="stats-grid">
            <div class="stat-item">
              <div class="stat-number">{{ totalActivities }}</div>
              <div class="stat-label">总活动数</div>
            </div>
            <div class="stat-item">
              <div class="stat-number">{{ selectedPetIds.length }}</div>
              <div class="stat-label">选中宠物</div>
            </div>
            <div class="stat-item">
              <div class="stat-number">{{ uniqueDays }}</div>
              <div class="stat-label">活跃天数</div>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="actions-section">
          <el-button type="primary" @click="showAddActivityDialog = true" block>
            <el-icon><Plus /></el-icon>
            添加活动记录
          </el-button>
          <el-button type="success" @click="showAddPetDialog = true" block>
            <el-icon><Plus /></el-icon>
            添加宠物
          </el-button>
          <el-button @click="refreshData" :loading="loading" block>
            <el-icon><Refresh /></el-icon>
            刷新数据
          </el-button>
        </div>
      </div>

      <!-- 右侧内容区域 -->
      <div class="right-content">
        <!-- 筛选栏 -->
        <div class="filter-bar">
          <div class="filter-left">
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
            />
            <el-select
              v-model="selectedActivityType"
              placeholder="活动类型"
              clearable
              @change="handleActivityTypeChange"
              size="default"
            >
              <el-option
                v-for="type in activityTypes"
                :key="type.value"
                :label="type.label"
                :value="type.value"
              />
            </el-select>
          </div>
          <div class="filter-right">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索活动内容..."
              @input="handleSearch"
              size="default"
              style="width: 250px"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
        </div>

        <!-- 活动记录列表 -->
        <div class="activity-records-list">
          <div v-if="loading" class="loading-state">
            <el-skeleton :rows="5" animated />
          </div>

          <div v-else-if="filteredRecords.length === 0" class="empty-state">
            <el-empty
              :description="selectedPetIds.length === 0 ? '请选择宠物查看活动记录' : '暂无活动记录'"
            />
          </div>

          <div v-else class="timeline">
            <div
              v-for="record in paginatedRecords"
              :key="record.activityRecordId"
              class="timeline-item"
            >
              <div class="timeline-date">
                <div class="date-day">{{ formatDate(record.activityDate).day }}</div>
                <div class="date-month">{{ formatDate(record.activityDate).month }}</div>
              </div>

              <div class="timeline-content">
                <el-card class="activity-card" shadow="hover">
                  <template #header>
                    <div class="card-header">
                      <div class="pet-info">
                        <el-avatar :size="32" :src="getPetInfo(record.petId).avatar_url">
                          {{ getPetInfo(record.petId).name.charAt(0) }}
                        </el-avatar>
                        <div class="pet-details">
                          <div class="pet-name">{{ getPetInfo(record.petId).name }}</div>
                          <div class="activity-type">{{ getActivityTypeName(record.activityId) }}</div>
                        </div>
                      </div>
                      <div class="card-actions">
                        <el-button size="small" type="primary" @click="editRecord(record)">
                          编辑
                        </el-button>
                        <el-button size="small" type="danger" @click="deleteRecord(record)">
                          删除
                        </el-button>
                      </div>
                    </div>
                  </template>

                  <div class="activity-content">
                    <p class="activity-description">{{ record.activityDescription }}</p>
                    <div class="activity-meta">
                      <span class="activity-time">
                        <el-icon><Clock /></el-icon>
                        {{ formatTime(record.activityDate) }}
                      </span>
                      <span class="activity-duration" v-if="record.duration">
                        <el-icon><Timer /></el-icon>
                        时长: {{ record.duration }}分钟
                      </span>
                    </div>
                  </div>
                </el-card>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="filteredRecords.length > 0">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="filteredRecords.length"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </div>

    <!-- 添加活动记录对话框 -->
    <el-dialog
      v-model="showAddActivityDialog"
      title="添加活动记录"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="activityFormRef"
        :model="activityForm"
        :rules="activityFormRules"
        label-width="100px"
      >
        <el-form-item label="宠物" prop="petId">
          <el-select v-model="activityForm.petId" placeholder="选择宠物" style="width: 100%">
            <el-option
              v-for="pet in userPets"
              :key="pet.id"
              :label="pet.name"
              :value="pet.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="活动类型" prop="activityId">
          <el-select v-model="activityForm.activityId" placeholder="选择活动类型" style="width: 100%">
            <el-option
              v-for="type in activityTypes"
              :key="type.value"
              :label="type.label"
              :value="type.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="活动日期" prop="activityDate">
          <el-date-picker
            v-model="activityForm.activityDate"
            type="datetime"
            placeholder="选择日期时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="活动描述" prop="description">
          <el-input
            v-model="activityForm.description"
            type="textarea"
            :rows="4"
            placeholder="请描述活动内容..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="活动时长">
          <el-input-number
            v-model="activityForm.duration"
            :min="0"
            :max="1440"
            placeholder="分钟"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showAddActivityDialog = false">取消</el-button>
          <el-button type="primary" @click="submitActivityForm" :loading="submitting">
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 添加宠物对话框 -->
    <el-dialog
      v-model="showAddPetDialog"
      title="添加宠物"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="petFormRef"
        :model="petForm"
        :rules="petFormRules"
        label-width="100px"
      >
        <el-form-item label="宠物名字" prop="name">
          <el-input
            v-model="petForm.name"
            placeholder="请输入宠物名字"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="宠物类型" prop="type">
          <el-select v-model="petForm.type" placeholder="选择宠物类型" style="width: 100%">
            <el-option label="狗" value="狗" />
            <el-option label="猫" value="猫" />
            <el-option label="鸟" value="鸟" />
            <el-option label="鱼" value="鱼" />
            <el-option label="兔子" value="兔子" />
            <el-option label="仓鼠" value="仓鼠" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>

        <el-form-item label="品种" prop="breed">
          <el-input
            v-model="petForm.breed"
            placeholder="请输入宠物品种"
            maxlength="50"
          />
        </el-form-item>

        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="petForm.gender">
            <el-radio value="雄性">雄性</el-radio>
            <el-radio value="雌性">雌性</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="生日" prop="birthday">
          <el-date-picker
            v-model="petForm.birthday"
            type="date"
            placeholder="选择宠物生日"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showAddPetDialog = false">取消</el-button>
          <el-button type="primary" @click="submitPetForm" :loading="submittingPet">
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 使用统一的布局底部 -->
    <AppFooter />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import apiService from '@/api/modules'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import {
  Plus,
  Refresh,
  Search,
  Clock,
  Timer
} from '@element-plus/icons-vue'

// 路由和状态
const router = useRouter()
const authStore = useAuthStore()

// 响应式数据
const loading = ref(false)
const submitting = ref(false)
const showAddActivityDialog = ref(false)
const showAddPetDialog = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const dateRange = ref([])
const selectedActivityType = ref('')
const searchKeyword = ref('')
const selectedPetIds = ref([])

// 数据
const userPets = ref([])
const activityRecords = ref([])
const allActivities = ref([])

// 表单
const activityFormRef = ref()
const petFormRef = ref()
const activityForm = ref({
  petId: null,
  activityId: null,
  activityDate: '',
  description: '',
  duration: 0
})

const petForm = ref({
  name: '',
  type: '',
  breed: '',
  gender: '',
  birthday: ''
})

const submittingPet = ref(false)

const activityFormRules = {
  petId: [{ required: true, message: '请选择宠物', trigger: 'change' }],
  activityId: [{ required: true, message: '请选择活动类型', trigger: 'change' }],
  activityDate: [{ required: true, message: '请选择活动日期', trigger: 'change' }],
  description: [{ required: true, message: '请输入活动描述', trigger: 'blur' }]
}

const petFormRules = {
  name: [{ required: true, message: '请输入宠物名字', trigger: 'blur' }],
  type: [{ required: true, message: '请选择宠物类型', trigger: 'change' }],
  breed: [{ required: true, message: '请输入宠物品种', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择宠物性别', trigger: 'change' }],
  birthday: [{ required: true, message: '请选择宠物生日', trigger: 'change' }]
}

// 活动类型选项
const activityTypes = ref([
  { label: '散步', value: 1 },
  { label: '喂食', value: 2 },
  { label: '洗澡', value: 3 },
  { label: '玩耍', value: 4 },
  { label: '训练', value: 5 },
  { label: '睡眠', value: 6 },
  { label: '医疗', value: 7 },
  { label: '其他', value: 8 }
])

// 计算属性
const currentUserId = computed(() => authStore.userId)

const filteredRecords = computed(() => {
  let records = [...activityRecords.value]

  // 宠物筛选
  if (selectedPetIds.value.length > 0) {
    records = records.filter(record => selectedPetIds.value.includes(record.petId))
  }

  // 日期范围筛选
  if (dateRange.value && dateRange.value.length === 2) {
    const [startDate, endDate] = dateRange.value
    records = records.filter(record => {
      const recordDate = record.activityDate.split(' ')[0]
      return recordDate >= startDate && recordDate <= endDate
    })
  }

  // 活动类型筛选
  if (selectedActivityType.value) {
    records = records.filter(record => record.activityId === selectedActivityType.value)
  }

  // 关键词搜索
  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.toLowerCase()
    records = records.filter(record =>
      record.activityDescription.toLowerCase().includes(keyword)
    )
  }

  return records.sort((a, b) => new Date(b.activityDate) - new Date(a.activityDate))
})

const paginatedRecords = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredRecords.value.slice(start, end)
})

const totalActivities = computed(() => activityRecords.value.length)

const uniqueDays = computed(() => {
  const dates = new Set(activityRecords.value.map(record => record.activityDate.split(' ')[0]))
  return dates.size
})

// 方法
const goSettings = () => {
  ElMessage.info('设置功能开发中...')
}

const togglePetSelection = (petId) => {
  const index = selectedPetIds.value.indexOf(petId)
  if (index > -1) {
    selectedPetIds.value.splice(index, 1)
  } else {
    selectedPetIds.value.push(petId)
  }
}

const getPetInfo = (petId) => {
  const pet = userPets.value.find(p => p.id === petId)
  return pet || { name: '未知宠物', avatar_url: '' }
}

const getActivityTypeName = (activityId) => {
  const type = activityTypes.value.find(t => t.value === activityId)
  return type ? type.label : '未知活动'
}

const getPetActivityCount = (petId) => {
  return activityRecords.value.filter(record => record.petId === petId).length
}

const formatDate = (dateStr) => {
  const date = new Date(dateStr)
  return {
    day: date.getDate(),
    month: date.toLocaleDateString('zh-CN', { month: 'short' })
  }
}

const formatTime = (dateStr) => {
  return new Date(dateStr).toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

const handleDateRangeChange = () => {
  currentPage.value = 1
}

const handleActivityTypeChange = () => {
  currentPage.value = 1
}

const handleSearch = () => {
  currentPage.value = 1
}

const handleSizeChange = () => {
  currentPage.value = 1
}

const handleCurrentChange = () => {
  // 页面变化时的处理
}

const loadUserPets = async () => {
  if (!currentUserId.value) return

  try {
    loading.value = true
    const pets = await apiService.pets.getByUserId(currentUserId.value)

    // 确保pets是数组
    userPets.value = Array.isArray(pets) ? pets : []
    console.log('加载到的宠物数据:', userPets.value)

    // 默认选中所有宠物
    selectedPetIds.value = userPets.value.map(pet => pet.id)
  } catch (error) {
    console.error('加载用户宠物失败:', error)
    userPets.value = []
  }
}

const loadActivityRecords = async () => {
  if (selectedPetIds.value.length === 0) {
    activityRecords.value = []
    return
  }

  try {
    loading.value = true
    const records = []

    // 为每个选中的宠物加载活动记录
    for (const petId of selectedPetIds.value) {
      try {
        // TODO: 需要实现活动记录API调用
        // 暂时返回空数组，等待后端API实现
        const petRecords = []
        records.push(...(Array.isArray(petRecords) ? petRecords : []))
      } catch (petError) {
        console.error(`加载宠物 ${petId} 的活动记录失败:`, petError)
      }
    }

    activityRecords.value = records
    console.log('加载到的活动记录:', activityRecords.value)
  } catch (error) {
    console.error('加载活动记录失败:', error)
    activityRecords.value = []
  } finally {
    loading.value = false
  }
}

const refreshData = async () => {
  await Promise.all([loadUserPets(), loadActivityRecords()])
}

const submitActivityForm = async () => {
  if (!activityFormRef.value) return

  try {
    await activityFormRef.value.validate()
    submitting.value = true

    const recordData = {
      activityId: activityForm.value.activityId,
      description: activityForm.value.description,
      date: activityForm.value.activityDate,
      duration: activityForm.value.duration
    }

    // TODO: 需要实现创建活动记录API
    // await apiService.activities.createRecord(activityForm.value.petId, recordData)
    console.log('创建活动记录:', recordData)

    ElMessage.success('活动记录添加成功！')
    showAddActivityDialog.value = false

    // 重置表单
    activityForm.value = {
      petId: null,
      activityId: null,
      activityDate: '',
      description: '',
      duration: 0
    }

    // 刷新数据
    await loadActivityRecords()
  } catch (error) {
    console.error('添加活动记录失败:', error)
    ElMessage.error('添加活动记录失败: ' + error.message)
  } finally {
    submitting.value = false
  }
}

const editRecord = (record) => {
  ElMessage.info('编辑功能开发中...')
}

const deleteRecord = async (record) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除这条活动记录吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    // TODO: 需要实现删除活动记录API
    // await apiService.activities.deleteRecord(record.activityRecordId)
    console.log('删除活动记录:', record.activityRecordId)
    ElMessage.success('删除成功！')

    await loadActivityRecords()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除活动记录失败:', error)
      ElMessage.error('删除失败: ' + error.message)
    }
  }
}

const submitPetForm = async () => {
  if (!petFormRef.value) return

  try {
    await petFormRef.value.validate()
    submittingPet.value = true

    // 构建符合API要求的宠物数据
    const petData = {
      name: petForm.value.name.trim(),
      type: petForm.value.type,
      breed: petForm.value.breed.trim(),
      gender: petForm.value.gender,
      age: new Date().getFullYear() - new Date(petForm.value.birthday).getFullYear(), // 计算年龄
      userId: currentUserId.value, // 添加当前用户ID
      birthday: petForm.value.birthday
    }

    // 调用真实的创建宠物API
    const { createPet } = await import('@/api/pets')
    const newPet = await createPet(petData)

    ElMessage.success('宠物添加成功！')
    showAddPetDialog.value = false

    // 重置表单
    petForm.value = {
      name: '',
      type: '',
      breed: '',
      gender: '',
      birthday: ''
    }

    // 重新加载宠物数据
    await loadUserPets()

    // 选中新添加的宠物
    if (newPet && newPet.petId) {
      selectedPetIds.value.push(newPet.petId)
    }

    await loadActivityRecords()
  } catch (error) {
    console.error('添加宠物失败:', error)
    ElMessage.error('添加宠物失败: ' + error.message)
  } finally {
    submittingPet.value = false
  }
}

// 监听宠物选择变化
watch(selectedPetIds, () => {
  loadActivityRecords()
}, { deep: true })

// 生命周期
onMounted(async () => {
  await refreshData()
})
</script>

<style scoped>
.activity-records-container {
  min-height: 100vh;
  width: 100vw;
  position: relative;
  background-color: #f8f9fa;
}

/* 顶部导航栏 */
.top-navigation {
  position: sticky;
  top: 0;
  z-index: 1000;
  background: white;
  border-bottom: 1px solid #e8e8e8;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.nav-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  max-width: 100vw;
  height: 60px;
}

.back-home-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.back-home-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.back-home-btn .el-icon {
  font-size: 16px;
}

.page-title h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #2c3e50;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 主要内容区域 */
.main-content {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 0;
  max-width: 100vw;
  margin: 0;
  min-height: calc(100vh - 60px);
}

/* 左侧边栏 */
.left-sidebar {
  background: white;
  border-right: 1px solid #e8e8e8;
  padding: 24px 20px;
  height: calc(100vh - 60px);
  position: sticky;
  top: 60px;
  overflow-y: auto;
}

/* 用户信息 */
.user-info-simple {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.user-avatar {
  background: linear-gradient(135deg, #FF8C00 0%, #FFD700 100%);
}

.user-details {
  flex: 1;
}

.user-name {
  font-weight: 600;
  font-size: 1.1rem;
  color: #2c3e50;
  margin-bottom: 2px;
}

.user-species {
  font-size: 0.85rem;
  color: #7f8c8d;
}

/* 宠物选择器 */
.pet-selector-section {
  margin-bottom: 24px;
}

.section-title {
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 16px;
  font-size: 0.9rem;
}

.pet-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pet-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 2px solid transparent;
}

.pet-item:hover {
  background: #f8f9fa;
}

.pet-item.active {
  background: #e3f2fd;
  border-color: #1890ff;
}

.pet-info {
  flex: 1;
  min-width: 0;
}

.pet-name {
  font-weight: 600;
  font-size: 0.9rem;
  color: #2c3e50;
  margin-bottom: 2px;
}

.pet-type {
  font-size: 0.75rem;
  color: #7f8c8d;
}

.pet-count {
  flex-shrink: 0;
}

/* 统计信息 */
.stats-section {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.stat-item {
  text-align: center;
  padding: 8px;
  background: #f8f9fa;
  border-radius: 6px;
}

.stat-number {
  font-size: 1.1rem;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 2px;
}

.stat-label {
  font-size: 0.7rem;
  color: #7f8c8d;
}

/* 操作按钮 */
.actions-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 右侧内容区域 */
.right-content {
  padding: 24px;
  overflow-y: auto;
  height: calc(100vh - 60px);
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.filter-left {
  display: flex;
  gap: 16px;
  align-items: center;
}

.filter-right {
  display: flex;
  align-items: center;
}

/* 活动记录列表 */
.activity-records-list {
  min-height: 400px;
}

.loading-state, .empty-state {
  padding: 40px 20px;
  text-align: center;
}

/* 时间线 */
.timeline {
  position: relative;
  padding-left: 40px;
}

.timeline::before {
  content: '';
  position: absolute;
  left: 15px;
  top: 0;
  bottom: 0;
  width: 2px;
  background: #e8e8e8;
}

.timeline-item {
  position: relative;
  margin-bottom: 24px;
}

.timeline-date {
  position: absolute;
  left: -40px;
  top: 8px;
  text-align: center;
  background: white;
  padding: 4px 8px;
  border-radius: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.date-day {
  font-size: 1.1rem;
  font-weight: 600;
  color: #2c3e50;
  line-height: 1;
}

.date-month {
  font-size: 0.7rem;
  color: #7f8c8d;
  line-height: 1;
  margin-top: 2px;
}

.timeline-content {
  position: relative;
}

.timeline-item::before {
  content: '';
  position: absolute;
  left: -35px;
  top: 12px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #1890ff;
  border: 3px solid white;
  box-shadow: 0 0 0 3px #e8e8e8;
}

/* 活动卡片 */
.activity-card {
  border: 1px solid #e8e8e8;
  transition: all 0.2s ease;
}

.activity-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pet-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pet-details {
  flex: 1;
}

.pet-name {
  font-weight: 600;
  font-size: 0.9rem;
  color: #2c3e50;
  margin-bottom: 2px;
}

.activity-type {
  font-size: 0.75rem;
  color: #7f8c8d;
}

.card-actions {
  display: flex;
  gap: 8px;
}

.activity-content {
  margin-top: 12px;
}

.activity-description {
  margin: 0 0 12px 0;
  line-height: 1.6;
  color: #2c3e50;
  font-size: 0.95rem;
}

.activity-meta {
  display: flex;
  gap: 16px;
  font-size: 0.8rem;
  color: #7f8c8d;
}

.activity-time, .activity-duration {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding: 20px 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .main-content {
    grid-template-columns: 1fr;
  }

  .left-sidebar {
    display: none;
  }

  .right-content {
    padding: 16px;
  }

  .filter-bar {
    flex-direction: column;
    gap: 16px;
  }

  .filter-left {
    width: 100%;
    flex-direction: column;
    gap: 12px;
  }

  .timeline {
    padding-left: 20px;
  }

  .timeline-date {
    left: -30px;
    padding: 2px 6px;
  }

  .timeline-item::before {
    left: -25px;
  }
}
</style>