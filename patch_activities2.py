import re

with open('d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/views/ActivitiesView.vue', 'r', encoding='utf-8') as f:
    content = f.read()

# Update import
import_regex = r"import \{\s*getActivitiesByPetId,\s*createActivityRecord,\s*deleteActivityRecord\s*\} from '../api/activities'"
import_replacement = "import { getActivitiesByPetId, createActivityRecord, deleteActivityRecord, getAbnormalRecords, ignoreAbnormalRecord } from '../api/activities'"
content = re.sub(import_regex, import_replacement, content)

# Inject reactive state
state_regex = r"(const activityRecords = ref\(\[\]\))"
state_replacement = r"\1\nconst abnormalRecords = ref([])\nconst loadingAbnormal = ref(false)"
content = re.sub(state_regex, state_replacement, content)

# Inject fetch function inside loadActivityRecords
load_regex = r"(const loadActivityRecords = async \(\) => \{[\s\S]*?if \(!selectedPetId\.value\) return[\s\S]*?loadingRecords\.value = true\s*try \{)"
load_replacement = r"\1\n    // 异步拉取异常记录\n    fetchAbnormalRecords()\n"
content = re.sub(load_regex, load_replacement, content)

# Inject fetchAbnormalRecords and ignore function
funcs_injection = """
const fetchAbnormalRecords = async () => {
  if (!selectedPetId.value) return
  loadingAbnormal.value = true
  try {
    const res = await getAbnormalRecords(selectedPetId.value)
    if (res.data && res.data.code === 20000) {
      abnormalRecords.value = res.data.data || []
    } else if (Array.isArray(res.data)) {
      abnormalRecords.value = res.data
    }
  } catch (error) {
    console.error('Failed to fetch abnormal records', error)
  } finally {
    loadingAbnormal.value = false
  }
}

const handleIgnoreAbnormal = async (recordId) => {
  try {
    await ignoreAbnormalRecord(recordId)
    ElMessage.success('已忽略该健康预警')
    abnormalRecords.value = abnormalRecords.value.filter(r => r.activityRecordId !== recordId)
  } catch (error) {
    ElMessage.error('操作失败')
  }
}
"""
content = content.replace('const loadActivityRecords = async () => {', funcs_injection + '\nconst loadActivityRecords = async () => {')

# Inject UI
ui_target = '<!-- 活动跨度视图 (横向日历时间轴) -->'
ui_injection = """
        <!-- === AI Abnormal Health Alerts === -->
        <div class="abnormal-alerts-container" v-if="abnormalRecords.length > 0">
          <el-alert
            v-for="alert in abnormalRecords"
            :key="alert.activityRecordId"
            type="error"
            show-icon
            :closable="false"
            style="margin-bottom: 12px; border: 1px solid #fde2e2; border-radius: 8px;"
          >
            <template #title>
              <div style="display: flex; justify-content: space-between; width: 100%; align-items: center;">
                <strong style="font-size: 15px;">健康预警 ({{ alert.bertResultName }})</strong>
                <el-button size="small" type="danger" plain @click="handleIgnoreAbnormal(alert.activityRecordId)">我知道了</el-button>
              </div>
            </template>
            <template #default>
              <p style="margin: 4px 0 0 0; color: #606266;">
                触发记录：{{ alert.activityName }} ({{ alert.activityDate }}) <br/>
                详情描述：{{ alert.activityDescription }}
              </p>
            </template>
          </el-alert>
        </div>
        
        <!-- 活动跨度视图 (横向日历时间轴) -->"""
content = content.replace(ui_target, ui_injection)

with open('d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/views/ActivitiesView.vue', 'w', encoding='utf-8') as f:
    f.write(content)
