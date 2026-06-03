with open('C:/Users/Administrator/.gemini/antigravity/brain/d8d15135-dd98-4556-8ea7-c416d5c7116b/task.md', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('- [ ] 1. 宠物异常健康预警系统 (Abnormal Alerts)', '- [x] 1. 宠物异常健康预警系统 (Abnormal Alerts)')
content = content.replace('- [ ] Update `ai.js` or `activities.js` API calls for `/abnormal` and `/ignore`.', '- [x] Update `ai.js` or `activities.js` API calls for `/abnormal` and `/ignore`.')
content = content.replace('- [ ] Update `ActivitiesView.vue` to fetch abnormal records when a pet is selected.', '- [x] Update `ActivitiesView.vue` to fetch abnormal records when a pet is selected.')
content = content.replace('- [ ] Add Alert UI in `ActivitiesView.vue` (top of timeline).', '- [x] Add Alert UI in `ActivitiesView.vue` (top of timeline).')
content = content.replace('- [ ] Implement "Ignore" button logic.', '- [x] Implement "Ignore" button logic.')

with open('C:/Users/Administrator/.gemini/antigravity/brain/d8d15135-dd98-4556-8ea7-c416d5c7116b/task.md', 'w', encoding='utf-8') as f:
    f.write(content)
