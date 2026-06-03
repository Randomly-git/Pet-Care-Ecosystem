with open('d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/api/ai.js', 'r', encoding='utf-8') as f:
    content = f.read()

bad_export = "export const getActivityHealthAnalysis,\\n  identifyCatBreed,\\n  aiAgentChat = async (petId, days = 7, userRequirement = '') => {"
good_export = "export const getActivityHealthAnalysis = async (petId, days = 7, userRequirement = '') => {"

content = content.replace("export const getActivityHealthAnalysis,\n  identifyCatBreed,\n  aiAgentChat = async (petId, days = 7, userRequirement = '') => {", "export const getActivityHealthAnalysis = async (petId, days = 7, userRequirement = '') => {")

with open('d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/api/ai.js', 'w', encoding='utf-8') as f:
    f.write(content)
