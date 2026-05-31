with open("d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/views/SettingsView.vue", "r", encoding="utf-8") as f:
    content = f.read()

# Add password form html
html_target = "<!-- Profile Section -->"
html_replacement = """<!-- Profile Section -->
          <div class="form-section">
            <h3 class="section-title">账号安全</h3>
            <div class="form-group">
              <label>原密码</label>
              <div class="input-wrapper">
                <input type="password" v-model="passwordForm.old" class="apple-input" placeholder="输入当前密码" />
              </div>
            </div>
            <div class="form-group">
              <label>新密码</label>
              <div class="input-wrapper">
                <input type="password" v-model="passwordForm.new" class="apple-input" placeholder="输入新密码" />
              </div>
            </div>
            <div class="form-group">
              <label>确认新密码</label>
              <div class="input-wrapper">
                <input type="password" v-model="passwordForm.confirm" class="apple-input" placeholder="再次输入新密码" />
              </div>
            </div>
          </div>
          
          <el-divider />

          <!-- Profile Section -->"""
content = content.replace(html_target, html_replacement)

# Add password mock logic
js_target = "const formName = ref('')"
js_replacement = """const formName = ref('')
const passwordForm = ref({
  old: '',
  new: '',
  confirm: ''
})"""
content = content.replace(js_target, js_replacement)

js_target2 = "if (!formName.value.trim()) {"
js_replacement2 = """if (passwordForm.value.new || passwordForm.value.old || passwordForm.value.confirm) {
    if (!passwordForm.value.old || !passwordForm.value.new || !passwordForm.value.confirm) {
      ElMessage.warning('请完整填写密码修改字段')
      return
    }
    if (passwordForm.value.new !== passwordForm.value.confirm) {
      ElMessage.warning('两次输入的新密码不一致')
      return
    }
  }
  
  if (!formName.value.trim()) {"""
content = content.replace(js_target2, js_replacement2)

js_target3 = "// Set mock limiter to false after saving"
js_replacement3 = """// Clear password form
    passwordForm.value = { old: '', new: '', confirm: '' }
    
    // Set mock limiter to false after saving"""
content = content.replace(js_target3, js_replacement3)

with open("d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/views/SettingsView.vue", "w", encoding="utf-8") as f:
    f.write(content)
