<template>
  <aside class="app-sidebar" :class="{ 'is-collapsed': isCollapsed }">
    <!-- 收放开关按钮 (使用渐变光轮) -->
    <button class="toggle-btn" @click="toggleSidebar" aria-label="Toggle Sidebar">
      <svg v-if="isCollapsed" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="3" y1="12" x2="21" y2="12"></line><line x1="3" y1="6" x2="21" y2="6"></line><line x1="3" y1="18" x2="21" y2="18"></line></svg>
      <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><polyline points="12 8 8 12 12 16"></polyline><line x1="16" y1="12" x2="8" y2="12"></line></svg>
    </button>

    <div v-show="!isCollapsed" class="sidebar-inner fade-in">
      <div class="sidebar-header">
        <h2 class="sidebar-title">系统导航</h2>
        <p class="sidebar-subtitle">快速访问菜单</p>
      </div>

      <nav class="sidebar-nav">
        <div class="menu-group" v-for="menu in menuStruct" :key="menu.id">
          <!-- 一级标题 -->
          <div class="group-header" @click="toggleGroup(menu.id)" :class="{ active: expandedGroups.includes(menu.id) || $route.path === menu.path }">
            <span class="group-label">{{ menu.label }}</span>
            <svg class="chevron" :class="{'rotated': expandedGroups.includes(menu.id)}" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg>
          </div>
          
          <!-- 二级子锚点组件列表 -->
          <transition name="collapse-list">
            <ul class="sub-menu" v-show="expandedGroups.includes(menu.id) || $route.path === menu.path">
              <li v-for="(sub, i) in menu.children" :key="i">
                <router-link :to="sub.fullPath" class="sub-link" active-class="active-sub-link">
                  <span class="sub-icon" v-html="sub.svg"></span>
                  <span class="sub-text">{{ sub.label }}</span>
                </router-link>
              </li>
            </ul>
          </transition>
        </div>
      </nav>
    </div>
  </aside>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const isCollapsed = ref(false)
const expandedGroups = ref(['home', 'activities', 'map', 'moments']) // 默认全展开

// 通用低饱和度高级SVG图标
const svgDocument = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>`
const svgDashboard = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>`
const svgCamera = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path><circle cx="12" cy="13" r="4"></circle></svg>`
const svgAI = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 2 7 12 12 22 7 12 2"></polygon><polyline points="2 17 12 22 22 17"></polyline><polyline points="2 12 12 17 22 12"></polyline></svg>`
const svgMapMarker = `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path><circle cx="12" cy="10" r="3"></circle></svg>`

const menuStruct = ref([
  {
    id: 'home',
    label: '系统首页',
    path: '/',
    children: [
      { label: '功能总览', fullPath: '/#hero', svg: svgDashboard },
      { label: '数据看板', fullPath: '/#dashboard', svg: svgDocument },
      { label: '生态服务', fullPath: '/#services', svg: svgCamera }
    ]
  },
  {
    id: 'activities',
    label: '宠物日志',
    path: '/activities',
    children: [
      { label: '活跃视图', fullPath: '/activities#heatmap', svg: svgDashboard },
      { label: 'AI智能报告', fullPath: '/activities#ai-report', svg: svgAI },
      { label: '历程时间轴', fullPath: '/activities#timeline', svg: svgDocument }
    ]
  },
  {
    id: 'map',
    label: '探索周边',
    path: '/map',
    children: [
      { label: '地图导览', fullPath: '/map#radar', svg: svgMapMarker },
      { label: '周边商户', fullPath: '/map#poi-list', svg: svgDocument }
    ]
  },
  {
    id: 'moments',
    label: '宠物社区',
    path: '/moments',
    children: [
      { label: '热门瀑布流', fullPath: '/moments#hot', svg: svgCamera }
    ]
  }
])

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}

const toggleGroup = (id) => {
  if (expandedGroups.value.includes(id)) {
    expandedGroups.value = expandedGroups.value.filter(item => item !== id)
  } else {
    expandedGroups.value.push(id)
  }
}
</script>

<style scoped>
/* 3-4种主色的线性径向叠加、背景渐变，白卡凸显层次，200ms过渡 */
.app-sidebar {
  width: 280px;
  height: 100vh;
  position: sticky;
  top: 0;
  flex-shrink: 0;
  background: 
    linear-gradient(135deg, rgba(240, 253, 250, 0.95) 0%, rgba(224, 242, 254, 0.95) 100%),
    url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)' opacity='0.03'/%3E%3C/svg%3E");
  border-right: 1px solid rgba(14, 165, 233, 0.1); /* 天空蓝隐约边 */
  transition: width 0.3s cubic-bezier(0.16, 1, 0.3, 1), transform 0.3s ease;
  overflow-y: auto;
  overflow-x: hidden;
  box-shadow: 4px 0 24px rgba(0,0,0,0.02);
  z-index: var(--z-fixed, 100);
}

.app-sidebar.is-collapsed {
  width: 70px;
}

/* 渐变描边按钮 */
.toggle-btn {
  position: absolute;
  top: 18px;
  right: 18px;
  background: white;
  border: none;
  cursor: pointer;
  padding: 8px;
  border-radius: 12px;
  color: #0ea5e9;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.15), inset 0 0 0 1px transparent;
  transition: all 0.25s ease;
  /* 渐变描边原理：借助 background-clip */
  background: linear-gradient(white, white) padding-box, linear-gradient(135deg, #38bdf8, #10b981) border-box;
  border: 1px solid transparent;
}

.toggle-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(14, 165, 233, 0.25);
}

.sidebar-inner {
  padding: 80px 24px 40px;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  margin-bottom: 40px;
}

/* 大字号标题与留白 */
.sidebar-title {
  font-size: 26px;
  font-weight: 700;
  background: linear-gradient(135deg, #0f766e 0%, #0369a1 100%);
  -webkit-background-clip: text;
  color: transparent;
  margin-bottom: 4px;
}
.sidebar-subtitle {
  font-size: 13px;
  color: #64748b;
  letter-spacing: 1px;
}

/* 导航组网格对齐 */
.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-radius: 14px;
  cursor: pointer;
  color: #475569;
  font-weight: 600;
  transition: background 0.2s ease, color 0.2s ease;
}
.group-header:hover {
  background: rgba(255, 255, 255, 0.6);
  color: #0ea5e9;
}
.group-header.active {
  color: #0c4a6e;
}

.group-label {
  font-size: 15px;
  letter-spacing: 0.5px;
}

.chevron {
  transition: transform 0.3s ease;
}
.rotated {
  transform: rotate(180deg);
}

/* 纯白卡片前景二级列表 */
.sub-menu {
  list-style: none;
  margin: 6px 0 0 0;
  padding: 6px;
  background: rgba(255, 255, 255, 0.85); /* 纯色/弱渐变前景卡片层，与背景拉开层次 */
  backdrop-filter: blur(10px);
  border-radius: 16px;
  box-shadow: 0 8px 30px rgba(0,0,0,0.03), inset 0 0 0 1px rgba(255,255,255,0.5);
}

.sub-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  text-decoration: none;
  color: #64748b;
  border-radius: 10px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 交互态采用色相/亮度渐变过渡 */
.sub-link:hover {
  background: linear-gradient(90deg, rgba(224, 242, 254, 0.5) 0%, rgba(204, 253, 246, 0.3) 100%);
  color: #0284c7;
  transform: translateX(4px);
}

.active-sub-link {
  background: linear-gradient(135deg, #e0f2fe 0%, #d1fae5 100%);
  color: #0369a1;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.1);
}

.sub-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  color: currentColor;
}
.sub-text {
  font-size: 14px;
}

/* 列表抽屉动画 */
.collapse-list-enter-active,
.collapse-list-leave-active {
  transition: max-height 0.3s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.3s ease;
  overflow: hidden;
}
.collapse-list-enter-from,
.collapse-list-leave-to {
  max-height: 0;
  opacity: 0;
}
.collapse-list-enter-to,
.collapse-list-leave-from {
  max-height: 200px;
  opacity: 1;
}

.fade-in {
  animation: fadeIn 0.4s ease forwards;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateX(-10px); }
  to { opacity: 1; transform: translateX(0); }
}

/* 适配移动端 */
@media (max-width: 1024px) {
  .app-sidebar {
    position: fixed;
    height: 100%;
    z-index: 1000;
  }
  .app-sidebar.is-collapsed {
    transform: translateX(-100%);
  }
}
</style>
