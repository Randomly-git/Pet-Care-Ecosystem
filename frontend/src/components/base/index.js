// 文件位置: src/components/base/index.js
// 通用组件库统一导出

// 基础组件
export { default as BaseButton } from './BaseButton.vue'
export { default as BaseCard } from './BaseCard.vue'
export { default as BaseIcon } from './BaseIcon.vue'

// 反馈组件
export { default as LoadingSpinner } from './LoadingSpinner.vue'
export { default as ErrorBoundary } from './ErrorBoundary.vue'
export { default as Notifications } from './Notifications.vue'
export { default as ErrorModal } from './ErrorModal.vue'

// 便捷全局注册（可选）
export const install = (app) => {
  // 基础组件
  app.component('BaseButton', BaseButton)
  app.component('BaseCard', BaseCard)
  app.component('BaseIcon', BaseIcon)

  // 反馈组件
  app.component('LoadingSpinner', LoadingSpinner)
  app.component('ErrorBoundary', ErrorBoundary)
  app.component('Notifications', Notifications)
  app.component('ErrorModal', ErrorModal)
}

// 默认导出
export default {
  install,
  BaseButton,
  BaseCard,
  BaseIcon,
  LoadingSpinner,
  ErrorBoundary,
  Notifications,
  ErrorModal
}