with open('d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/components/layout/AppHeader.vue', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('@click="router.push(item.path)"', '@click="handleNavClick(item)"')

js_target = 'const router = useRouter()'
js_replacement = '''const router = useRouter()
const route = useRoute()

const handleNavClick = (item) => {
  if (item.path && item.path.includes('#')) {
    const hash = item.path.substring(item.path.indexOf('#'))
    const targetPath = item.path.split('#')[0] || '/'
    
    if (route.path === targetPath) {
      const el = document.querySelector(hash)
      if (el) {
        el.scrollIntoView({ behavior: 'smooth' })
      }
      router.push(item.path)
    } else {
      router.push(item.path)
    }
  } else {
    router.push(item.path)
  }
}'''

content = content.replace(js_target, js_replacement)
content = content.replace("import { useRouter } from 'vue-router'", "import { useRouter, useRoute } from 'vue-router'")

with open('d:/Tongji_Projects/Pet-Care-Ecosystem/frontend/src/components/layout/AppHeader.vue', 'w', encoding='utf-8') as f:
    f.write(content)
