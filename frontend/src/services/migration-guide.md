# API服务迁移指南

本文档说明如何从旧的API服务迁移到新的架构。

## 迁移步骤

### 1. 安装和导入

**旧方式：**
```javascript
import { userAPI, petAPI } from '@/services/api'
```

**新方式：**
```javascript
import apiService, { authService, petService } from '@/services'
// 或使用组合式API
import { useAuth, usePets } from '@/composables/useApi'
```

### 2. 认证相关

**旧方式：**
```javascript
// 登录
const response = await userAPI.login({
  name: 'testuser',
  password: 'password123'
})

// 获取用户信息
const userInfo = await userAPI.getUserInfo()
```

**新方式：**
```javascript
// 使用服务
const result = await authService.login({
  name: 'testuser',
  password: 'password123'
})

// 使用组合式API
const { user, login, isAuthenticated } = useAuth()
await login(credentials)
```

### 3. 宠物管理

**旧方式：**
```javascript
// 获取宠物列表
const pets = await petAPI.getPetList(userId)

// 创建宠物
const newPet = await petAPI.createPet(petData)
```

**新方式：**
```javascript
// 使用服务
const pets = await petService.getPets(userId)
const newPet = await petService.createPet(petData)

// 使用组合式API
const { pets, createPet, isLoading } = usePets(userId)
await createPet(petData)
```

### 4. 错误处理

**旧方式：**
```javascript
try {
  const result = await petAPI.getPet(petId)
} catch (error) {
  console.error(error)
  // 手动处理错误
}
```

**新方式：**
```javascript
// 服务会自动处理错误并显示提示
const { data, error, execute } = useApi(
  () => petService.getPet(petId),
  {
    showMessage: true,
    onError: (error) => {
      // 自定义错误处理
    }
  }
)
```

### 5. 加载状态

**旧方式：**
```javascript
const loading = ref(false)

const fetchPets = async () => {
  loading.value = true
  try {
    pets.value = await petAPI.getPetList(userId)
  } finally {
    loading.value = false
  }
}
```

**新方式：**
```javascript
// 自动管理加载状态
const { pets, isLoading, refresh } = usePets(userId)

// 或使用通用API Hook
const { data, isLoading, execute } = useApi(
  () => petService.getPets(userId),
  { loading: true }
)
```

### 6. 缓存

**旧方式：**
```javascript
// 需要手动实现缓存
```

**新方式：**
```javascript
// 自动缓存
const { data } = await useApi(
  () => petService.getPets(userId),
  {
    cache: true,
    cacheTime: 5 * 60 * 1000 // 5分钟
  }
)

// 手动管理缓存
import { cacheService } from '@/services'
cacheService.clearPattern('pet:')
```

### 7. 批量请求

**旧方式：**
```javascript
// 需要手动管理Promise.all
const results = await Promise.all([
  petAPI.getPet(petId1),
  petAPI.getPet(petId2),
  petAPI.getPet(petId3)
])
```

**新方式：**
```javascript
// 使用批量服务
import { batchService } from '@/services'
const results = await batchService.getPetsBatch([petId1, petId2, petId3])
```

## 组件迁移示例

### 迁移前（使用旧的API）：

```vue
<template>
  <div>
    <el-button @click="fetchPets" :loading="loading">刷新</el-button>
    <el-table :data="pets">
      <!-- 表格内容 -->
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { petAPI } from '@/services/api'

const pets = ref([])
const loading = ref(false)

const fetchPets = async () => {
  loading.value = true
  try {
    const userId = 1
    pets.value = await petAPI.getPetList(userId)
  } catch (error) {
    ElMessage.error('获取宠物列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchPets()
})
</script>
```

### 迁移后（使用新的API）：

```vue
<template>
  <div>
    <el-button @click="refresh" :loading="isLoading">刷新</el-button>
    <el-table :data="pets">
      <!-- 表格内容 -->
    </el-table>
  </div>
</template>

<script setup>
import { usePets } from '@/composables/useApi'

const userId = 1
const { pets, isLoading, refresh } = usePets(userId)
</script>
```

## 主要改进

1. **统一的错误处理**：自动处理错误并显示用户友好的提示
2. **内置的加载状态**：无需手动管理loading状态
3. **自动重试机制**：网络错误时自动重试
4. **缓存支持**：减少重复请求，提高性能
5. **类型安全**：完整的TypeScript类型定义
6. **组合式API**：更好的Vue 3集成
7. **请求取消**：自动取消未完成的请求
8. **批量操作**：支持批量请求优化

## 注意事项

1. 新的API服务返回的数据格式可能略有不同，请查看类型定义
2. 错误处理是自动的，如需自定义错误处理，请使用onError回调
3. 缓存是基于URL和参数的，相同请求会返回缓存结果
4. 使用组合式API时，注意响应式数据的只读性质

## 常见问题

### Q: 如何取消请求？
A: 使用useApi返回的cancel方法，或在组件卸载时自动取消

### Q: 如何自定义错误消息？
A: 使用onError回调，或配置api.ts中的ERROR_MESSAGES

### Q: 如何禁用某个请求的缓存？
A: 传入cache: false选项

### Q: 如何设置全局的超时时间？
A: 修改config/api.ts中的DEFAULT_CONFIG

### Q: 如何在开发环境查看请求日志？
A: 配置日志级别为debug，或查看控制台输出