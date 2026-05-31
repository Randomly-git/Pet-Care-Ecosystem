with open('d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/views/HomeView.vue', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('<section class="about-section">', '<section class="about-section" id="about">')

with open('d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/views/HomeView.vue', 'w', encoding='utf-8') as f:
    f.write(content)
