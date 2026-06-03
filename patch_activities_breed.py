import re

with open('d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/views/ActivitiesView.vue', 'r', encoding='utf-8') as f:
    content = f.read()

# Import the new api
content = content.replace("import { getActivityHealthAnalysis } from '../api/ai'", "import { getActivityHealthAnalysis, identifyCatBreed, aiAgentChat } from '../api/ai'")

# Add state for breed identification
state_injection = """
const aiRecognizing = ref(false)

const handleAiRecognize = async (file) => {
  aiRecognizing.value = true
  try {
    const res = await identifyCatBreed(file.raw)
    if (res.breed && res.breed !== '未知') {
      petForm.type = 'cat'
      petForm.breed = res.breed
      ElMessage.success(`AI 识别成功：${res.breed}`)
    } else {
      ElMessage.warning('未能识别出品种')
    }
  } catch (error) {
    ElMessage.error('识别失败，请重试')
  } finally {
    aiRecognizing.value = false
  }
}
"""
content = content.replace("const showAddPetDialog = ref(false)", "const showAddPetDialog = ref(false)\n" + state_injection)

# Add the UI to the dialog
ui_target = '<el-form ref="petFormRef" :model="petForm" :rules="petFormRules" label-width="100px">'
ui_injection = """<el-form ref="petFormRef" :model="petForm" :rules="petFormRules" label-width="100px">
        <el-form-item label="照片识别">
          <el-upload
            class="avatar-uploader"
            action="#"
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleAiRecognize"
            accept="image/*"
          >
            <el-button type="primary" plain :loading="aiRecognizing" round>
              <el-icon><MagicStick /></el-icon> 🪄 AI 智能识别猫咪品种
            </el-button>
          </el-upload>
        </el-form-item>"""
content = content.replace(ui_target, ui_injection)

with open('d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/views/ActivitiesView.vue', 'w', encoding='utf-8') as f:
    f.write(content)
