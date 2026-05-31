import os

file_path = 'd:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/views/ActivitiesView.vue'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Update HTML
html_target = '''
          <div class="ai-report-body">
            <div class="report-status green">稳定 & 活跃</div>
            <p class="report-text">在近 7 天的日常中，进食完成度高达 <strong>95%</strong>，且状态(双排)异常，建议在周末增加 <strong>15%</strong> 的户外互动以维持活力。</p>
            <div class="report-metrics">
              <div class="metric"><div class="metric-val">98/100</div><div class="metric-label">健康评分</div></div>
              <div class="metric"><div class="metric-val">+2%</div><div class="metric-label">较周活跃度</div></div>
            </div>
          </div>
'''
html_replacement = '''
          <div class="ai-report-body" v-loading="aiReportLoading">
            <template v-if="aiReportData">
              <div class="report-status" :class="aiReportData.analysisType && aiReportData.analysisType.includes('异常') ? 'red' : 'green'">
                {{ aiReportData.analysisType }}
              </div>
              <p class="report-text" v-html="formatAiAnalysisText(aiReportData.analysis)"></p>
              
              <div class="report-metrics" v-if="aiReportData.knowledgeSources && aiReportData.knowledgeSources.length > 0">
                <div class="metric">
                  <div class="metric-val">{{ aiReportData.knowledgeSources.length }} 篇</div>
                  <div class="metric-label">参考医学知识库数目</div>
                </div>
              </div>
            </template>
            <template v-else-if="aiReportError">
              <div class="report-status" style="color: #ef4444;">获取失败</div>
              <p class="report-text" style="color: #ef4444;">无法获取宠物健康评估简报，请检查网络或后端服务状态。</p>
              <el-button size="small" type="primary" plain @click="fetchAiReport">重试获取</el-button>
            </template>
            <template v-else>
              <div class="report-status" style="color: #8c8c8c;">暂无数据</div>
              <p class="report-text">暂无针对该宠物的健康评估简报。</p>
            </template>
          </div>
'''
content = content.replace(html_target.strip(), html_replacement.strip())

# 2. Add Reactivity variables & logic
js_insert_after = 'const dateRange = ref([])'
js_logic = '''
// AI Report variables
const aiReportData = ref(null)
const aiReportLoading = ref(false)
const aiReportError = ref(false)

const formatAiAnalysisText = (text) => {
  if (!text) return ''
  // Basic markdown bold to HTML
  return text.replace(/\\*\\*(.*?)\\*\\*/g, '<strong>$1</strong>')
}

const fetchAiReport = async () => {
  if (!selectedPetId.value) return
  
  aiReportLoading.value = true
  aiReportError.value = false
  
  try {
    const { getActivityHealthAnalysis } = await import('@/api/ai')
    aiReportData.value = await getActivityHealthAnalysis(selectedPetId.value, 7, '')
  } catch (error) {
    console.error('Failed to fetch AI report:', error)
    aiReportError.value = true
    aiReportData.value = null
  } finally {
    aiReportLoading.value = false
  }
}
'''
content = content.replace(js_insert_after, js_insert_after + '\n' + js_logic)

# 3. Call fetchAiReport in loadActivityRecords
load_records_target = '''
const loadActivityRecords = async () => {
  try {
    loading.value = true
'''
load_records_replacement = '''
const loadActivityRecords = async () => {
  try {
    loading.value = true
    fetchAiReport() // 获取AI简报数据
'''
content = content.replace(load_records_target.strip(), load_records_replacement.strip())

# 4. Add CSS for 'red' status
css_insert_target = '.report-status.green { color: #22c55e; }'
css_insert_replacement = '.report-status.green { color: #22c55e; }\n.report-status.red { color: #ef4444; }'
content = content.replace(css_insert_target, css_insert_replacement)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print('AI Report integration injected successfully.')
