with open('C:/Users/Administrator/.gemini/antigravity/brain/d8d15135-dd98-4556-8ea7-c416d5c7116b/task.md', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('- [ ] 2. 猫咪品种智能识别 (Cat Breed ID)', '- [x] 2. 猫咪品种智能识别 (Cat Breed ID)')
content = content.replace('- [ ] Update `ai.js` or `pet.js` to add `identifyCatBreed` API call.', '- [x] Update `ai.js` or `pet.js` to add `identifyCatBreed` API call.')
content = content.replace('- [ ] Update `ActivitiesView.vue` (or `PetSpaceView.vue`) Add Pet Dialog to include image upload.', '- [x] Update `ActivitiesView.vue` (or `PetSpaceView.vue`) Add Pet Dialog to include image upload.')
content = content.replace('- [ ] Add "AI Recognize" button.', '- [x] Add "AI Recognize" button.')
content = content.replace('- [ ] Implement logic to autofill breed and type on successful recognition.', '- [x] Implement logic to autofill breed and type on successful recognition.')

with open('C:/Users/Administrator/.gemini/antigravity/brain/d8d15135-dd98-4556-8ea7-c416d5c7116b/task.md', 'w', encoding='utf-8') as f:
    f.write(content)
