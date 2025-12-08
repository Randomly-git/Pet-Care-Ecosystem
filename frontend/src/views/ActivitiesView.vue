<template>
  <div class="activities-page">
    <!-- 使用统一的布局头部 -->
    <AppHeader />

    <!-- Hero Section -->
    <section class="hero-section">
      <div class="hero-container">
        <div class="hero-content">
          <div class="hero-emoji">🐾</div>
          <h2 class="hero-title">活动记录</h2>
          <p class="hero-subtitle">记录和管理宠物的日常活动与健康数据</p>
        </div>
      </div>
    </section>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <div class="container">
        <!-- 宠物管理栏 -->
        <div class="pets-section">
          <div class="section-header">
            <h3 class="section-title">我的宠物</h3>
            <div class="pets-count">共 {{ userPets.length }} 只宠物</div>
          </div>

          <div class="pets-grid">
            <!-- 宠物卡片 -->
            <div
              v-for="pet in userPets"
              :key="pet.id || pet.petId"
              class="pet-card"
              :class="{ active: selectedPetIds.includes(pet.id || pet.petId) }"
              @click="togglePetSelection(pet.id || pet.petId)"
            >
              <div class="pet-avatar">
                <el-avatar :size="48" :src="pet.avatar_url">
                  {{ pet.name.charAt(0) }}
                </el-avatar>
                <div class="pet-status-dot" v-if="selectedPetIds.includes(pet.id || pet.petId)"></div>
              </div>
              <div class="pet-info">
                <div class="pet-name">{{ pet.name }}</div>
                <div class="pet-details">{{ pet.species || pet.type }} · {{ pet.breed }}</div>
              </div>
              <div class="pet-activities">
                <div class="activity-count">{{ getPetActivityCount(pet.id || pet.petId) }}</div>
                <div class="activity-label">活动</div>
              </div>
            </div>

            <!-- 添加宠物卡片 -->
            <div class="pet-card add-pet-card" @click="showAddPetDialog = true">
              <div class="add-pet-content">
                <div class="add-icon">+</div>
                <div class="add-text">添加宠物</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 操作栏 -->
        <div class="action-bar">
          <div class="action-left">
            <el-select
              v-model="selectedPetIds"
              multiple
              placeholder="选择宠物"
              style="width: 200px"
              @change="handlePetSelectionChange"
            >
              <el-option
                v-for="pet in userPets"
                :key="pet.id || pet.petId"
                :label="pet.name"
                :value="pet.id || pet.petId"
              />
            </el-select>

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
              style="width: 240px; margin-left: 12px"
            />
          </div>

          <div class="action-right">
            <el-button type="success" @click="showAddDialog = true">
              <el-icon><Plus /></el-icon>
              添加记录
            </el-button>
            <el-button @click="refreshData">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>

        <!-- 活动类型筛选 -->
        <div class="activity-filters">
          <div class="filter-header">
            <span class="filter-title">活动类型筛选：</span>
          </div>
          <div class="filter-options">
            <el-checkbox-group v-model="selectedActivityTypes" @change="handleActivityTypeFilter">
              <el-checkbox
                v-for="type in activityTypes"
                :key="type.value"
                :label="type.label"
                :value="type.value"
              >
                <span class="filter-label" :class="`filter-${getActivityTypeClass(type.value)}`">
                  {{ type.label }}
                </span>
              </el-checkbox>
            </el-checkbox-group>
            <el-button
              size="small"
              @click="clearActivityTypeFilter"
              style="margin-left: 16px;"
            >
              清除筛选
            </el-button>
          </div>
        </div>

        <!-- 活动记录时间线 -->
        <div class="timeline-section">
          <div v-if="loading" class="loading-container">
            <el-skeleton :rows="5" animated />
          </div>

          <div v-else-if="filteredRecords.length === 0" class="empty-state">
            <el-empty description="暂无活动记录">
              <el-button type="primary" @click="showAddDialog = true">
                创建第一条记录
              </el-button>
            </el-empty>
          </div>

          <div v-else class="activity-timeline">
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
                  :key="record.activityRecordId"
                  class="timeline-item"
                  @click="editRecord(record)"
                >
                  <div class="timeline-marker">
                    <div class="marker-dot" :class="getActivityTypeClass(record.activityId)"></div>
                    <div class="marker-line"></div>
                  </div>

                  <div class="timeline-content">
                    <div class="record-card">
                      <div class="record-header">
                        <div class="pet-info">
                          <el-avatar :size="32" :src="getPetInfo(record.petId).avatar_url">
                            {{ getPetInfo(record.petId).name.charAt(0) }}
                          </el-avatar>
                          <div class="pet-details">
                            <div class="pet-name">{{ getPetInfo(record.petId).name }}</div>
                            <div class="activity-type">{{ getActivityTypeName(record.activityId) }}</div>
                          </div>
                        </div>
                        <div class="record-time">
                          {{ formatTime(record.activityDate) }}
                        </div>
                      </div>

                      <div class="record-description">
                        {{ record.activityDescription }}
                      </div>

                      <div class="record-actions">
                        <el-button size="small" @click.stop="editRecord(record)">
                          编辑
                        </el-button>
                        <el-button size="small" type="danger" @click.stop="deleteRecord(record)">
                          删除
                        </el-button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

  
    <!-- 添加活动记录对话框 -->
    <el-dialog
      v-model="showAddDialog"
      title="添加活动记录"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="addFormRef"
        :model="addForm"
        :rules="addFormRules"
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

        <el-form-item label="活动类别" prop="activityKindId">
          <el-select
            v-model="addForm.activityKindId"
            placeholder="选择活动类别"
            style="width: 100%"
            @change="handleActivityKindChange"
          >
            <el-option
              v-for="type in activityTypes"
              :key="type.value"
              :label="type.label"
              :value="type.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item
          v-if="addForm.activityKindId"
          label="具体活动"
          prop="activityId"
        >
          <div style="display: flex; gap: 8px;">
            <el-select
              v-model="addForm.activityId"
              placeholder="选择具体活动（可选，也可直接使用类别）"
              style="flex: 1"
              filterable
              no-data-text="该类别下暂无活动，可直接使用类别创建记录"
            >
              <el-option
                v-for="activity in getActivitiesByKind(addForm.activityKindId)"
                :key="activity.activityId"
                :label="activity.activityName"
                :value="activity.activityId"
              />
            </el-select>
            <el-button
              type="primary"
              plain
              @click="showCreateActivityDialog = true"
              :disabled="!addForm.activityKindId"
            >
              新建活动
            </el-button>
          </div>
          <div v-if="addForm.activityKindId"
               style="margin-top: 8px; padding: 8px; background: #f0f9ff; border: 1px solid #bfdbfe; border-radius: 4px; font-size: 12px; color: #1e40af;">
            <div v-if="getActivitiesByKind(addForm.activityKindId).length === 0">
              该类别下还没有具体的活动。您可以选择：
              <ul style="margin: 4px 0; padding-left: 16px;">
                <li><strong>直接提交</strong>：系统将使用活动类别直接创建记录</li>
                <li><strong>新建活动</strong>：点击右侧"新建活动"按钮创建具体活动</li>
              </ul>
              例如，{{ getActivityKindName(addForm.activityKindId) }}类别可以包括：
              <span v-if="addForm.activityKindId === 1">吃零食、吃饭、喝水等</span>
              <span v-else-if="addForm.activityKindId === 2">玩耍、拥抱、训练等</span>
              <span v-else-if="addForm.activityKindId === 3">洗澡、刷牙、剪指甲等</span>
              <span v-else-if="addForm.activityKindId === 4">散步、公园游玩、旅行等</span>
              <span v-else-if="addForm.activityKindId === 5">跑步、爬楼梯、玩玩具等</span>
              <span v-else-if="addForm.activityKindId === 6">体检、打疫苗、吃药等</span>
              <span v-else-if="addForm.activityKindId === 7">发情、怀孕、生产等</span>
              <span v-else-if="addForm.activityKindId === 8">呕吐、腹泻、跛行等</span>
              <span v-else>其他具体活动</span>
            </div>
            <div v-else>
              既有具体活动可选，也可以直接使用活动类别创建记录
            </div>
          </div>
        </el-form-item>

        <el-form-item label="活动时间" prop="activityDate">
          <el-date-picker
            v-model="addForm.activityDate"
            type="datetime"
            placeholder="选择活动时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="活动描述" prop="description">
          <el-input
            v-model="addForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入活动描述..."
          />
        </el-form-item>

        </el-form>

      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAddForm" :loading="submitting">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑活动记录对话框 -->
    <el-dialog
      v-model="showEditDialog"
      title="编辑活动记录"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="addFormRules"
        label-width="100px"
      >
        <el-form-item label="宠物名称" prop="petId">
          <el-input v-model="editForm.petName" disabled placeholder="宠物名称" />
        </el-form-item>

        <el-form-item label="活动类型" prop="activityId">
          <el-select v-model="editForm.activityId" placeholder="选择活动类型" style="width: 100%">
            <el-option
              v-for="type in activityTypes"
              :key="type.value"
              :label="type.label"
              :value="type.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="活动时间" prop="activityDate">
          <el-date-picker
            v-model="editForm.activityDate"
            type="datetime"
            placeholder="选择活动时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="活动描述" prop="description">
          <el-input
            v-model="editForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入活动描述..."
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="submitEditForm" :loading="submitting">
          更新
        </el-button>
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
        <el-form-item label="宠物名称" prop="name">
          <el-input v-model="petForm.name" placeholder="请输入宠物名称" />
        </el-form-item>

        <el-form-item label="宠物类型" prop="type">
          <el-select v-model="petForm.type" placeholder="选择宠物类型" style="width: 100%">
            <el-option label="狗" value="dog" />
            <el-option label="猫" value="cat" />
            <el-option label="鸟" value="bird" />
            <el-option label="鱼" value="fish" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>

        <el-form-item label="品种" prop="breed">
          <el-input v-model="petForm.breed" placeholder="请输入宠物品种" />
        </el-form-item>

        <el-form-item label="性别" prop="gender">
          <el-select v-model="petForm.gender" placeholder="选择性别" style="width: 100%">
            <el-option label="雄性" value="male" />
            <el-option label="雌性" value="female" />
            <el-option label="未知" value="unknown" />
          </el-select>
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
        <el-button @click="showAddPetDialog = false">取消</el-button>
        <el-button type="primary" @click="submitPetForm" :loading="submittingPet">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 创建新活动对话框 -->
    <el-dialog
      v-model="showCreateActivityDialog"
      title="创建新活动"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="newActivityFormRef"
        :model="newActivityForm"
        :rules="newActivityFormRules"
        label-width="100px"
      >
        <el-form-item label="活动名称" prop="activityName">
          <el-input
            v-model="newActivityForm.activityName"
            placeholder="请输入活动名称，如：吃零食、散步、洗澡等"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="活动类别">
          <el-input
            :value="getActivityKindName(addForm.activityKindId)"
            disabled
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreateActivityDialog = false">取消</el-button>
        <el-button
          type="primary"
          @click="submitCreateActivity"
          :loading="submittingNewActivity"
        >
          创建活动
        </el-button>
      </template>
    </el-dialog>

    <!-- 使用统一的布局底部 -->
    <AppFooter />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import apiService from '@/api/modules'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import {
  Plus,
  Refresh
} from '@element-plus/icons-vue'

const authStore = useAuthStore()

// 响应式数据
const loading = ref(false)
const submitting = ref(false)
const showAddDialog = ref(false)
const showEditDialog = ref(false)
const showAddPetDialog = ref(false)
const submittingPet = ref(false)

// 创建新活动相关
const showCreateActivityDialog = ref(false)
const submittingNewActivity = ref(false)
const newActivityFormRef = ref()
const newActivityForm = ref({
  activityName: ''
})

const userPets = ref([])
const activityRecords = ref([])
const userActivities = ref([])
const allActivities = ref([])
const selectedPetIds = ref([])
const selectedActivityTypes = ref([1, 2, 3, 4, 5, 6, 7, 8, 9]) // 默认选择所有活动类型
const dateRange = ref([])

// 存储每个宠物的总活动记录数（用于卡片显示）
const petActivityStats = ref({})

// 表单数据
const addFormRef = ref()
const editFormRef = ref()
const addForm = ref({
  petId: null,
  activityKindId: null,
  activityId: null,
  activityDate: '',
  description: ''
})

const editForm = ref({
  activityRecordId: null,
  petId: null,
  petName: '',
  activityId: null,
  activityDate: '',
  description: ''
})

// 宠物表单数据
const petFormRef = ref()
const petForm = ref({
  name: '',
  type: '',
  breed: '',
  gender: '',
  birthday: ''
})

// 表单验证规则
const addFormRules = {
  petId: [{ required: true, message: '请选择宠物', trigger: 'change' }],
  activityKindId: [{ required: true, message: '请选择活动类别', trigger: 'change' }],
  activityDate: [
    { 
      validator: (rule, value, callback) => {
        // 现在日期是可选字段，如果不提供则使用当前时间
        if (!value) {
          callback()
        } else {
          // 验证日期格式
          try {
            new Date(value)
            callback()
          } catch (error) {
            callback(new Error('日期格式不正确'))
          }
        }
      },
      trigger: 'change'
    }
  ],
  // description 现在是可选字段，不需要必填验证
  activityId: [
    {
      validator: (rule, value, callback) => {
        // 现在允许直接使用活动种类创建记录，所以具体活动不是必需的
        // 只要选择了活动类别就可以通过验证
        if (addForm.value.activityKindId) {
          callback()
        } else {
          callback(new Error('请先选择活动类别'))
        }
      },
      trigger: 'change'
    }
  ]
}

// 宠物表单验证规则
const petFormRules = {
  name: [{ required: true, message: '请输入宠物名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择宠物类型', trigger: 'change' }],
  breed: [{ required: true, message: '请输入宠物品种', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择宠物性别', trigger: 'change' }],
  birthday: [
    { required: true, message: '请选择宠物生日', trigger: 'change' },
    {
      validator: (rule, value, callback) => {
        const selectedDate = new Date(value)
        const today = new Date()
        if (selectedDate > today) {
          callback(new Error('生日不能是未来日期'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ]
}

// 新建活动表单验证规则
const newActivityFormRules = {
  activityName: [
    { required: true, message: '请输入活动名称', trigger: 'blur' },
    { min: 1, max: 100, message: '活动名称长度应在1-100个字符之间', trigger: 'blur' }
  ]
}

// 活动类型 - 匹配数据库中的实际活动种类
const activityTypes = ref([
  { label: '喂养', value: 1 },
  { label: '互动', value: 2 },
  { label: '清洁', value: 3 },
  { label: '外出', value: 4 },
  { label: '运动', value: 5 },
  { label: '医疗', value: 6 },
  { label: '生育', value: 7 },
  { label: '异常', value: 8 },
  { label: '其他', value: 9 }
])

// 计算属性
const currentUserId = computed(() => authStore.userId)

const totalActivities = computed(() => activityRecords.value.length)

const completedToday = computed(() => {
  const today = new Date().toISOString().split('T')[0]
  return activityRecords.value.filter(record =>
    record.activityDate.startsWith(today)
  ).length
})

const selectedPetsCount = computed(() => selectedPetIds.value.length)

const filteredRecords = computed(() => {
  let filtered = activityRecords.value

  // 按选择的宠物过滤
  if (selectedPetIds.value.length > 0) {
    filtered = filtered.filter(record => selectedPetIds.value.includes(record.petId))
  }

  // 按日期范围过滤
  if (dateRange.value && dateRange.value.length === 2) {
    const [startDate, endDate] = dateRange.value
    filtered = filtered.filter(record => {
      const recordDate = record.activityDate.split(' ')[0]
      return recordDate >= startDate && recordDate <= endDate
    })
  }

  // 按活动类型筛选
  if (selectedActivityTypes.value.length > 0) {
    console.log('筛选前记录数量:', filtered.length)
    console.log('选择的活动类型:', selectedActivityTypes.value)
    console.log('用户活动数据:', userActivities.value)

    filtered = filtered.filter(record => {
      // 根据activityId找到对应的activityKindId
      const activity = userActivities.value.find(a => a.activityId === record.activityId)
      const shouldInclude = activity && selectedActivityTypes.value.includes(activity.activityKindId)

      if (!activity) {
        console.log('未找到活动，record.activityId:', record.activityId)
      }

      console.log('记录筛选结果:', {
        recordId: record.activityRecordId,
        recordActivityId: record.activityId,
        foundActivity: activity,
        shouldInclude,
        selectedTypes: selectedActivityTypes.value
      })

      return shouldInclude
    })

    console.log('筛选后记录数量:', filtered.length)
  }

  return filtered.sort((a, b) => new Date(b.activityDate) - new Date(a.activityDate))
})

const groupedRecords = computed(() => {
  const groups = {}
  filteredRecords.value.forEach(record => {
    const date = record.activityDate.split(' ')[0]
    if (!groups[date]) {
      groups[date] = []
    }
    groups[date].push(record)
  })
  return groups
})


// 方法
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
    return `${date.getMonth() + 1}月${date.getDate()}日`
  }
}

const formatTime = (dateTimeStr) => {
  const date = new Date(dateTimeStr)
  return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

const getPetInfo = (petId) => {
  const pet = userPets.value.find(p => (p.id || p.petId) === petId)
  return pet || { name: '未知宠物', avatar_url: '' }
}

// 处理活动类别变化
const handleActivityKindChange = () => {
  // 当活动类别变化时，清空具体活动选择
  addForm.value.activityId = null
}

// 根据活动类别ID获取用户的具体活动列表
const getActivitiesByKind = (activityKindId) => {
  if (!activityKindId) return []

  return userActivities.value.filter(activity =>
    activity.activityKindId === activityKindId
  )
}

const getActivityTypeName = (activityId) => {
  // 首先在用户活动列表中找到对应的活动，获取activityKindId
  const activity = userActivities.value.find(a => a.activityId === activityId)
  if (!activity) return '未知活动'

  // 然后根据activityKindId找到类型名称
  const type = activityTypes.value.find(t => t.value === activity.activityKindId)
  return type ? type.label : '未知活动'
}

// 根据活动种类ID获取活动种类名称
const getActivityKindName = (activityKindId) => {
  const type = activityTypes.value.find(t => t.value === activityKindId)
  return type ? type.label : '未知类别'
}

// 创建新活动的方法
const submitCreateActivity = async () => {
  if (!newActivityFormRef.value) return

  try {
    await newActivityFormRef.value.validate()
    submittingNewActivity.value = true

    // 导入活动创建API
    const { createActivity } = await import('@/api/activities')

    // 准备活动数据
    const activityData = {
      activityName: newActivityForm.value.activityName.trim(),
      activityKindId: addForm.value.activityKindId,
      userId: currentUserId.value
    }

    console.log('创建新活动:', activityData)

    // 调用API创建活动
    const newActivity = await createActivity(activityData)

    ElMessage.success('活动创建成功！')

    // 将新活动添加到用户活动列表
    if (newActivity) {
      userActivities.value.push(newActivity)
    }

    // 关闭对话框并重置表单
    showCreateActivityDialog.value = false
    newActivityForm.value = {
      activityName: ''
    }

    // 自动选择新创建的活动
    if (newActivity && newActivity.activityId) {
      addForm.value.activityId = newActivity.activityId
    }

  } catch (error) {
    console.error('创建活动失败:', error)
    ElMessage.error('创建活动失败: ' + error.message)
  } finally {
    submittingNewActivity.value = false
  }
}

const getActivityTypeClass = (activityId) => {
  // 首先在用户活动列表中找到对应的活动，获取activityKindId
  const activity = userActivities.value.find(a => a.activityId === activityId)
  if (!activity) return 'default'

  // 然后根据activityKindId返回对应的CSS类
  const classes = {
    1: 'diet',      // 喂养
    2: 'exercise',  // 互动
    3: 'hygiene',   // 清洁
    4: 'play',      // 外出
    5: 'training',  // 运动
    6: 'medical',   // 医疗
    7: 'breeding',  // 生育
    8: 'alert',     // 异常
    9: 'other'      // 其他
  }
  return classes[activity.activityKindId] || 'default'
}

const loadUserPets = async () => {
  if (!currentUserId.value) return

  try {
    loading.value = true
    // 使用正确的pets API
    const { getUserPets } = await import('@/api/pets')
    const pets = await getUserPets(currentUserId.value)

    // 处理API响应格式
    if (pets && pets.data) {
      userPets.value = Array.isArray(pets.data) ? pets.data : []
    } else {
      userPets.value = Array.isArray(pets) ? pets : []
    }

    console.log('加载到的宠物数据:', userPets.value)

    // 默认选中所有宠物
    if (selectedPetIds.value.length === 0 && userPets.value.length > 0) {
      selectedPetIds.value = userPets.value.map(pet => pet.petId || pet.id)
    }
  } catch (error) {
    console.error('加载用户宠物失败:', error)
    userPets.value = []
  } finally {
    loading.value = false
  }
}

const loadUserActivities = async () => {
  if (!currentUserId.value) return

  try {
    const { getActivitiesByUserId } = await import('@/api/activities')
    const response = await getActivitiesByUserId(currentUserId.value)

    // 处理API响应格式
    if (response && response.data) {
      userActivities.value = Array.isArray(response.data) ? response.data : []
    } else if (Array.isArray(response)) {
      userActivities.value = response
    } else {
      userActivities.value = []
    }

    console.log('加载到的用户活动:', userActivities.value)
  } catch (error) {
    console.error('加载用户活动失败:', error)
    userActivities.value = []
  }
}

const loadActivityRecords = async () => {
  try {
    loading.value = true

    // 获取所有选中宠物的活动记录
    if (selectedPetIds.value.length === 0) {
      activityRecords.value = []
      return
    }

    const { getActivityRecordsByPetIds } = await import('@/api/activities')

    // 批量获取活动记录
    const recordsResponse = await getActivityRecordsByPetIds(selectedPetIds.value, {
      startDate: dateRange.value[0] ? new Date(dateRange.value[0]).toISOString() : null,
      endDate: dateRange.value[1] ? new Date(dateRange.value[1]).toISOString() : null
    })

    // 处理API响应格式
    if (recordsResponse && recordsResponse.data) {
      activityRecords.value = Array.isArray(recordsResponse.data) ? recordsResponse.data : []
    } else if (Array.isArray(recordsResponse)) {
      activityRecords.value = recordsResponse
    } else {
      activityRecords.value = []
    }

    console.log('加载到的活动记录:', activityRecords.value)
  } catch (error) {
    console.error('加载活动记录失败:', error)
    activityRecords.value = []
  } finally {
    loading.value = false
  }
}

const refreshData = async () => {
  await Promise.all([loadUserPets(), loadUserActivities(), loadActivityRecords()])
  // 在加载完宠物列表后，加载所有宠物的活动统计数据
  await loadPetActivityStats()
}

const handlePetSelectionChange = () => {
  loadActivityRecords()
}

const handleDateRangeChange = () => {
  loadActivityRecords()
}

const submitAddForm = async () => {
  if (!addFormRef.value) return

  try {
    await addFormRef.value.validate()
    submitting.value = true

    // 导入API
    const { createActivityRecord, createActivityRecordByKind } = await import('@/api/activities')

    // 为每个选中的宠物创建活动记录
    const createPromises = []
    const selectedPets = userPets.value.filter(pet =>
      selectedPetIds.value.includes(pet.petId || pet.id)
    )

    // 判断使用哪种创建方式
    const availableActivities = getActivitiesByKind(addForm.value.activityKindId)
    const useDirectKindMode = availableActivities.length === 0 || !addForm.value.activityId

    for (const pet of selectedPets) {
      const petId = pet.petId || pet.id

      // 格式化日期为API要求的格式 yyyy-MM-dd'T'HH:mm:ss
      const activityDate = new Date(addForm.value.activityDate)
      const formattedDate = activityDate.toISOString().slice(0, 19) // 保留 'T'

      let recordData

      if (useDirectKindMode) {
        // 直接使用活动种类ID创建记录（新的API方式）
        recordData = {
          activityKindId: addForm.value.activityKindId,
          description: addForm.value.description,
          date: formattedDate,
          userId: Number(currentUserId.value) // 新增：添加userId
        }
        console.log(`为宠物 ${pet.name} (ID: ${petId}) 使用活动种类创建记录:`, recordData)
        createPromises.push(createActivityRecordByKind(petId, recordData))
      } else {
        // 使用具体活动ID创建记录（原有方式）
        recordData = {
          activityId: addForm.value.activityId,
          description: addForm.value.description,
          date: formattedDate,
          userId: Number(currentUserId.value) // 新增：添加userId
        }
        console.log(`为宠物 ${pet.name} (ID: ${petId}) 使用具体活动创建记录:`, recordData)
        createPromises.push(createActivityRecord(petId, recordData))
      }
    }

    // 等待所有创建操作完成
    await Promise.all(createPromises)

    ElMessage.success(`成功为 ${selectedPets.length} 只宠物添加活动记录！`)
    showAddDialog.value = false

    // 重置表单
    addForm.value = {
      petId: null,
      activityKindId: null,
      activityId: null,
      activityDate: '',
      description: ''
    }

    // 刷新活动记录列表
    await loadActivityRecords()
    // 更新宠物活动统计
    await loadPetActivityStats()
  } catch (error) {
    console.error('添加活动记录失败:', error)
    ElMessage.error('添加活动记录失败: ' + error.message)
  } finally {
    submitting.value = false
  }
}

const editRecord = (record) => {
  try {
    // 找到对应的活动记录，获取活动种类ID
    const activity = userActivities.value.find(a => a.activityId === record.activityId)
    const activityKindId = activity ? activity.activityKindId : null

    // 找到宠物名称
    const pet = getPetInfo(record.petId)

    // 填充编辑表单
    editForm.value = {
      activityRecordId: record.activityRecordId || record.id,
      petId: record.petId,
      petName: pet.name || '未知宠物',
      activityId: activityKindId, // 使用活动种类ID
      activityDate: record.activityDate ? new Date(record.activityDate).toISOString().slice(0, 19).replace('T', ' ') : '',
      description: record.activityDescription || record.description || ''
    }

    showEditDialog.value = true
  } catch (error) {
    console.error('编辑记录失败:', error)
    ElMessage.error('无法编辑该记录')
  }
}

const submitEditForm = async () => {
  if (!editFormRef.value) return

  try {
    await editFormRef.value.validate()
    submitting.value = true

    // 根据活动种类ID找到对应的实际活动ID
    const selectedActivityKindId = editForm.value.activityId
    const matchingActivity = userActivities.value.find(activity =>
      activity.activityKindId === selectedActivityKindId
    )

    if (!matchingActivity) {
      throw new Error(`找不到对应的活动记录，活动种类ID: ${selectedActivityKindId}`)
    }

    // 格式化日期为API要求的格式 yyyy-MM-dd'T'HH:mm:ss
    const activityDate = new Date(editForm.value.activityDate)
    const formattedDate = activityDate.toISOString().slice(0, 19) // 保留 'T'

      // 导入API并更新记录
    const { updateActivityRecord } = await import('@/api/activities')
    await updateActivityRecord(editForm.value.activityRecordId, {
      newActivityId: matchingActivity.activityId, // 使用实际的活动ID
      description: editForm.value.description,
      date: formattedDate
    })

    ElMessage.success('活动记录更新成功！')
    showEditDialog.value = false

    // 刷新活动记录列表
    await loadActivityRecords()
    // 更新宠物活动统计
    await loadPetActivityStats()
  } catch (error) {
    console.error('更新活动记录失败:', error)
    ElMessage.error('更新活动记录失败: ' + (error.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}

const deleteRecord = async (record) => {
  try {
    await ElMessageBox.confirm('确定要删除这条活动记录吗？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    // 导入API
    const { deleteActivityRecord } = await import('@/api/activities')

    // 删除活动记录
    const recordId = record.activityRecordId || record.id
    await deleteActivityRecord(recordId)

    ElMessage.success('删除成功！')
    await loadActivityRecords()
    // 更新宠物活动统计
    await loadPetActivityStats()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除记录失败:', error)
      ElMessage.error('删除失败: ' + error.message)
    }
  }
}

// 宠物相关方法
const togglePetSelection = (petId) => {
  const index = selectedPetIds.value.indexOf(petId)
  if (index > -1) {
    selectedPetIds.value.splice(index, 1)
  } else {
    selectedPetIds.value.push(petId)
  }
  loadActivityRecords()
}

// 加载所有宠物的活动统计数据
const loadPetActivityStats = async () => {
  if (!currentUserId.value || userPets.value.length === 0) {
    petActivityStats.value = {}
    return
  }

  try {
    const { getActivityRecordsByPetIds } = await import('@/api/activities')

    // 获取用户所有宠物的ID
    const allPetIds = userPets.value.map(pet => pet.petId || pet.id)

    // 获取所有宠物的所有活动记录（不限时间范围）
    const allRecordsResponse = await getActivityRecordsByPetIds(allPetIds, {
      startDate: null, // 不限制开始时间
      endDate: null    // 不限制结束时间
    })

    // 处理响应数据
    let allRecords = []
    if (allRecordsResponse && allRecordsResponse.data) {
      allRecords = Array.isArray(allRecordsResponse.data) ? allRecordsResponse.data : []
    } else if (Array.isArray(allRecordsResponse)) {
      allRecords = allRecordsResponse
    }

    // 统计每个宠物的活动记录数
    const stats = {}
    allPetIds.forEach(petId => {
      stats[petId] = 0
    })

    allRecords.forEach(record => {
      const petId = record.petId
      if (stats.hasOwnProperty(petId)) {
        stats[petId]++
      }
    })

    petActivityStats.value = stats
    console.log('宠物活动统计:', petActivityStats.value)

  } catch (error) {
    console.error('加载宠物活动统计失败:', error)
    // 发生错误时初始化为0
    const stats = {}
    userPets.value.forEach(pet => {
      stats[pet.petId || pet.id] = 0
    })
    petActivityStats.value = stats
  }
}

const getPetActivityCount = (petId) => {
  return petActivityStats.value[petId] || 0
}

const submitPetForm = async () => {
  if (!petFormRef.value) return

  try {
    await petFormRef.value.validate()
    submittingPet.value = true

    // 计算年龄（处理未来生日的情况）
    const today = new Date()
    const birthday = new Date(petForm.value.birthday)
    let age = today.getFullYear() - birthday.getFullYear()
    const monthDiff = today.getMonth() - birthday.getMonth()

    // 如果生日还没到，年龄减1
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthday.getDate())) {
      age -= 1
    }

    // 确保年龄不为负数，最小为0
    age = Math.max(0, age)

    // 确保age是数字类型
    if (isNaN(age) || age === null || age === undefined) {
      age = 0
    }

    console.log('宠物信息:', {
      birthday: petForm.value.birthday,
      today: today.toISOString(),
      calculatedAge: age,
      ageType: typeof age,
      userId: currentUserId.value,
      userIdType: typeof currentUserId.value
    })

    // 验证必需字段
    if (!currentUserId.value) {
      throw new Error('用户未登录，无法创建宠物')
    }

    // 构建符合API要求的宠物数据
    const petData = {
      name: petForm.value.name.trim(),
      species: petForm.value.type, // 前端type映射到后端species
      breed: petForm.value.breed.trim(),
      // 注意：后端Pet实体没有age和gender字段，只有species和birthday
      userId: Number(currentUserId.value), // 确保userId是数字类型
      birthday: petForm.value.birthday
    }

    // 最后的数据完整性验证
    const requiredFields = ['name', 'species', 'breed', 'userId', 'birthday']
    const missingFields = requiredFields.filter(field => {
      const value = petData[field]
      // 字段不能为null、undefined或空字符串
      return value === null || value === undefined || value === ''
    })

    if (missingFields.length > 0) {
      console.error('缺少必需字段:', missingFields)
      console.error('完整的petData:', petData)
      throw new Error(`缺少必需字段: ${missingFields.join(', ')}`)
    }

    // 重新验证字段存在性（修复后的逻辑）
    console.log('验证字段存在性:', {
      name: !!petData.name,
      species: !!petData.species,
      breed: !!petData.breed,
      userId: !!petData.userId,
      birthday: !!petData.birthday
    })

    console.log('发送到API的宠物数据:', petData)
    console.log('各字段类型和值:', {
      name: { value: petData.name, type: typeof petData.name },
      species: { value: petData.species, type: typeof petData.species },
      breed: { value: petData.breed, type: typeof petData.breed },
      userId: { value: petData.userId, type: typeof petData.userId },
      birthday: { value: petData.birthday, type: typeof petData.birthday }
    })

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
    // 更新宠物活动统计
    await loadPetActivityStats()
  } catch (error) {
    console.error('添加宠物失败:', error)
    ElMessage.error('添加宠物失败: ' + error.message)
  } finally {
    submittingPet.value = false
  }
}


// 生命周期
onMounted(async () => {
  await refreshData()
})

// 活动类型筛选方法
const handleActivityTypeFilter = () => {
  // 筛选逻辑已在computed属性中实现
  console.log('选择的活动类型:', selectedActivityTypes.value)
}

const clearActivityTypeFilter = () => {
  selectedActivityTypes.value = []
  console.log('清除活动类型筛选')
}

// 监听器
watch([currentUserId], () => {
  if (currentUserId.value) {
    refreshData()
  }
})
</script>

<style scoped>
.activities-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
}

/* ===== Hero Section ===== */
.hero-section {
  background: linear-gradient(135deg, rgba(251, 146, 60, 0.1) 0%, rgba(250, 204, 21, 0.1) 100%),
              url('https://images.unsplash.com/photo-1450778869188-b1d976e7e5fa?q=80&w=1332&auto=format&fit=crop') center/cover no-repeat;
  padding: 4rem 0;
  position: relative;
  display: flex;
  align-items: center;
}

.hero-section::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(251, 146, 60, 0.2) 0%, rgba(250, 204, 21, 0.2) 100%);
  z-index: 1;
}

.hero-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
  text-align: center;
  position: relative;
  z-index: 2;
}

.hero-content {
  max-width: 800px;
  margin: 0 auto;
}

.hero-emoji {
  font-size: 4rem;
  margin-bottom: 1.5rem;
  line-height: 1;
}

.hero-title {
  font-size: 3rem;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 1rem;
  line-height: 1.2;
}

.hero-subtitle {
  font-size: 1.25rem;
  color: #64748b;
  margin-bottom: 0;
  line-height: 1.6;
}

.page-header {
  background: white;
  border-bottom: 1px solid #e2e8f0;
  padding: 2rem 0;
}

.header-content {
  text-align: center;
}

.page-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.title-icon {
  font-size: 2.5rem;
}

.page-subtitle {
  color: #64748b;
  font-size: 1.125rem;
}

.main-content {
  padding: 2rem 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
}

/* 宠物管理栏 */
.pets-section {
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 1.5rem;
  margin-bottom: 2rem;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.section-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}

.pets-count {
  color: #64748b;
  font-size: 0.875rem;
}

.pets-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1rem;
}

.pet-card {
  background: #f8fafc;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  padding: 1rem;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.pet-card:hover {
  border-color: #cbd5e1;
  background: #f1f5f9;
  transform: translateY(-2px);
}

.pet-card.active {
  border-color: #3b82f6;
  background: #eff6ff;
}

.pet-avatar {
  position: relative;
}

.pet-status-dot {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 12px;
  height: 12px;
  background: #10b981;
  border: 2px solid white;
  border-radius: 50%;
}

.pet-info {
  flex: 1;
}

.pet-name {
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 0.25rem;
}

.pet-details {
  font-size: 0.875rem;
  color: #64748b;
}

.pet-activities {
  text-align: center;
}

.activity-count {
  font-size: 1.25rem;
  font-weight: 700;
  color: #3b82f6;
}

.activity-label {
  font-size: 0.75rem;
  color: #64748b;
}

.add-pet-card {
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  border: 2px dashed #cbd5e1;
  justify-content: center;
}

.add-pet-card:hover {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
}

.add-pet-content {
  text-align: center;
  color: #64748b;
}

.add-icon {
  font-size: 2rem;
  margin-bottom: 0.5rem;
  font-weight: 300;
}

.add-text {
  font-weight: 500;
}

/* 操作栏 */
.action-bar {
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  margin-bottom: 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.action-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.action-right {
  display: flex;
  gap: 0.5rem;
}

/* 活动类型筛选 */
.activity-filters {
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 16px 20px;
  margin-bottom: 20px;
}

.filter-header {
  margin-bottom: 12px;
}

.filter-title {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.filter-options {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-label {
  font-size: 13px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

/* 活动类型筛选标签样式 */
.filter-1 { background-color: #fef3c7; color: #92400e; } /* 喂养 */
.filter-2 { background-color: #dbeafe; color: #1e40af; } /* 互动 */
.filter-3 { background-color: #ede9fe; color: #5b21b6; } /* 清洁 */
.filter-4 { background-color: #fef3c7; color: #b45309; } /* 外出 */
.filter-5 { background-color: #fee2e2; color: #dc2626; } /* 运动 */
.filter-6 { background-color: #e0e7ff; color: #4f46e5; } /* 医疗 */
.filter-7 { background-color: #fce7f3; color: #a21caf; } /* 生育 */
.filter-8 { background-color: #fee2e2; color: #dc2626; } /* 异常 */
.filter-9 { background-color: #f3f4f6; color: #6b7280; } /* 其他 */
.filter-default { background-color: #f3f4f6; color: #6b7280; } /* 默认 */

/* 时间线 */
.timeline-section {
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.loading-container {
  padding: 2rem;
}

.empty-state {
  padding: 3rem;
}

.activity-timeline {
  padding: 2rem;
}

.timeline-group {
  margin-bottom: 2rem;
}

.timeline-group:last-child {
  margin-bottom: 0;
}

.timeline-date {
  margin-bottom: 1rem;
}

.date-badge {
  display: inline-block;
  background: #f1f5f9;
  color: #475569;
  padding: 0.5rem 1rem;
  border-radius: 20px;
  font-weight: 600;
  font-size: 0.875rem;
}

.timeline-item {
  display: flex;
  margin-bottom: 1.5rem;
  position: relative;
}

.timeline-marker {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-right: 1rem;
}

.marker-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #cbd5e1;
}

.marker-dot.diet {
  background: #10b981;
}

.marker-dot.exercise {
  background: #3b82f6;
}

.marker-dot.hygiene {
  background: #8b5cf6;
}

.marker-dot.play {
  background: #f59e0b;
}

.marker-dot.training {
  background: #ef4444;
}

.marker-dot.medical {
  background: #6366f1;
}

.marker-dot.breeding {
  background: #ec4899;
}

.marker-dot.alert {
  background: #dc2626;
}

.marker-dot.other {
  background: #6b7280;
}

.marker-line {
  width: 2px;
  height: 100%;
  background: #e2e8f0;
  margin-top: 0.5rem;
}

.timeline-content {
  flex: 1;
}

.record-card {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 1rem;
  cursor: pointer;
  transition: all 0.2s;
}

.record-card:hover {
  border-color: #cbd5e1;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.pet-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.pet-name {
  font-weight: 600;
  color: #1e293b;
}

.activity-type {
  font-size: 0.875rem;
  color: #64748b;
}

.record-time {
  font-size: 0.875rem;
  color: #64748b;
}

.record-description {
  color: #475569;
  margin-bottom: 1rem;
  line-height: 1.5;
}

.record-actions {
  display: flex;
  gap: 0.5rem;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-title {
    font-size: 2rem;
  }

  .action-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .action-left {
    justify-content: center;
  }

  .action-right {
    justify-content: center;
  }

  .record-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }

  .timeline-item {
    flex-direction: column;
  }

  .timeline-marker {
    margin-right: 0;
    margin-bottom: 0.5rem;
  }

  .marker-line {
    display: none;
  }
}
</style>