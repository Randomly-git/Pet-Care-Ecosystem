// 文件位置: src/stores/index.js
// Pinia stores 统一导出

export { useUserStore } from './user'
export { useActivityStore } from './activity'
export { useAppStore } from './app'

// 便捷导入
export { createPinia } from 'pinia'