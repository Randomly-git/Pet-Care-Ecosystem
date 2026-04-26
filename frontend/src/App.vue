<template>
  <div class="app-layout">
    <AppSidebar />
    <div class="main-container">
      <RouterView />
    </div>
  </div>
</template>

<script setup>
import { RouterView } from 'vue-router'
import AppSidebar from '@/components/layout/AppSidebar.vue'
</script>

<style>
/* App级别的样式已在reset.css中处理 */
.app-layout {
  display: flex;
  min-height: 100vh;
  width: 100%;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0; /* 防止子元素撑破 Flex 容器 */
  position: relative;
}
</style>