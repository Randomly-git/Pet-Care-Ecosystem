import re

with open('d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/views/ActivitiesView.vue', 'r', encoding='utf-8') as f:
    content = f.read()

# Import AiAgentChat
import_target = "import PetStatusCard from '../components/petspace/PetStatusCard.vue'"
import_replacement = import_target + "\nimport AiAgentChat from '../components/petspace/AiAgentChat.vue'\nimport { ChatDotRound } from '@element-plus/icons-vue'"
content = content.replace(import_target, import_replacement)

# Add state for chat
state_regex = r"(const abnormalRecords = ref\(\[\]\))"
state_replacement = r"\1\nconst showAiAgent = ref(false)"
content = re.sub(state_regex, state_replacement, content)

# Add FAB and Component to template
ui_target = '</div>\n  </div>\n</template>'
ui_injection = """
    <!-- AI Agent Chat Drawer -->
    <AiAgentChat
      v-model="showAiAgent"
      :pet-id="selectedPetId"
      @record-created="loadActivityRecords"
    />

    <!-- AI Agent FAB -->
    <el-button
      type="primary"
      class="ai-fab"
      circle
      size="large"
      @click="showAiAgent = true"
    >
      <el-icon :size="24"><ChatDotRound /></el-icon>
    </el-button>
  </div>
</template>"""
content = content.replace(ui_target, ui_injection)

# Add CSS for FAB
css_target = '</style>'
css_injection = """
.ai-fab {
  position: fixed;
  bottom: 40px;
  right: 40px;
  width: 60px;
  height: 60px;
  box-shadow: 0 4px 16px rgba(103, 80, 164, 0.4);
  z-index: 1000;
  transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.ai-fab:hover {
  transform: scale(1.1) rotate(5deg);
}
</style>"""
content = content.replace(css_target, css_injection)

with open('d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/views/ActivitiesView.vue', 'w', encoding='utf-8') as f:
    f.write(content)
