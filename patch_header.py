with open('d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/components/layout/AppHeader.vue', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace("path: '/about'", "path: '/#about'")

with open('d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/components/layout/AppHeader.vue', 'w', encoding='utf-8') as f:
    f.write(content)
