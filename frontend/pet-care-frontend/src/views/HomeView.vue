<template>
  <div class="home-container">
    <!-- 背景图片 -->
    <div class="background-image"></div>

    <!-- 用户信息侧边栏 -->
    <div class="user-sidebar">
      <!-- 当前用户信息 -->
      <div class="current-user" @click="toggleUserDropdown">
        <el-avatar :size="50" :src="currentPet.avatar" class="user-avatar">
          {{ currentPet.name.charAt(0) }}
        </el-avatar>
        <div class="user-info">
          <div class="user-name">{{ currentPet.name }}</div>
          <div class="user-species">{{ currentPet.species }}</div>
        </div>
        <el-icon class="dropdown-arrow" :class="{ rotated: showUserDropdown }">
          <ArrowDown />
        </el-icon>
      </div>

      <!-- 用户下拉菜单 -->
      <div v-show="showUserDropdown" class="user-dropdown">
        <div 
          v-for="pet in petAccounts" 
          :key="pet.id"
          class="dropdown-item"
          :class="{ active: pet.id === currentPet.id }"
          @click="switchPet(pet)"
        >
          <el-avatar :size="32" :src="pet.avatar">
            {{ pet.name.charAt(0) }}
          </el-avatar>
          <span class="pet-name">{{ pet.name }}</span>
        </div>
        <el-divider />
        <div class="dropdown-item logout-item" @click="logout">
          <el-icon><SwitchButton /></el-icon>
          <span>退出登录</span>
        </div>
      </div>
    </div>
    
    <!-- 主要内容 -->
    <div class="main-content">
      <!-- 顶部标题 -->
      <div class="title-section">
        <h1 class="main-title">笑猫の窝</h1>
      </div>

      <!-- 功能菜单 - 横向排列 -->
      <div class="feature-menu">
        <!-- 我的空间 -->
        <div class="menu-item space-item" @click="goToPetSpace">
          <div class="menu-icon">🐾</div>
          <h2 class="menu-title">我的空间</h2>
          <p class="menu-desc">"每个生命都是一个世界"</p>
        </div>

        <!-- 竖分隔线 -->
        <div class="vertical-line"></div>

        <!-- 看兽医 -->
        <div class="menu-item medical-item" @click="goToMedical">
          <div class="menu-icon">🩺</div>
          <h2 class="menu-title">看兽医</h2>
          <p class="menu-desc">"今天也要健康、开心！"</p>
        </div>

        <!-- 竖分隔线 -->
        <div class="vertical-line"></div>

        <!-- 去逛街 -->
        <div class="menu-item shopping-item" @click="goToShopping">
          <div class="menu-icon">🛍️</div>
          <h2 class="menu-title">去逛街</h2>
          <p class="menu-desc">"粮、玩具、美容什么的"</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, SwitchButton } from '@element-plus/icons-vue'

const router = useRouter()

// 用户数据
const currentPet = ref({
  id: 1,
  name: '咪咪',
  species: '英国短毛猫',
  avatar: ''
})

const petAccounts = ref([
  { id: 1, name: '咪咪', species: '英国短毛猫', avatar: '' },
  { id: 2, name: '旺财', species: '金毛犬', avatar: '' },
  { id: 3, name: '小白', species: '波斯猫', avatar: '' }
])

const showUserDropdown = ref(false)

// 方法
const toggleUserDropdown = () => {
  showUserDropdown.value = !showUserDropdown.value
}

const switchPet = (pet) => {
  currentPet.value = { ...pet }
  showUserDropdown.value = false
  ElMessage.success(`已切换到 ${pet.name} 的空间`)
  
  // 保存当前选择的宠物到本地存储
  localStorage.setItem('currentPet', JSON.stringify(pet))
}

const logout = () => {
  currentPet.value = { id: 0, name: '未登录', species: '', avatar: '' }
  showUserDropdown.value = false
  localStorage.removeItem('currentPet')
  ElMessage.info('已退出登录')
}

const goToMedical = () => {
  if (currentPet.value.id === 0) {
    ElMessage.warning('请先选择宠物账号')
    return
  }
  // 保存当前宠物信息到会话存储，供其他页面使用
  sessionStorage.setItem('currentPet', JSON.stringify(currentPet.value))
  router.push('/medical')
}

const goToPetSpace = () => {
  if (currentPet.value.id === 0) {
    ElMessage.warning('请先选择宠物账号')
    return
  }
  // 保存当前宠物信息到会话存储，供宠物空间页面使用
  sessionStorage.setItem('currentPet', JSON.stringify(currentPet.value))
  router.push(`/pet-space?pet_id=${currentPet.value.id}`)
}

const goToShopping = () => {
  if (currentPet.value.id === 0) {
    ElMessage.warning('请先选择宠物账号')
    return
  }
  // 保存当前宠物信息到会话存储
  sessionStorage.setItem('currentPet', JSON.stringify(currentPet.value))
  router.push('/shopping')
}

// 初始化时加载保存的宠物信息
onMounted(() => {
  const savedPet = localStorage.getItem('currentPet')
  if (savedPet) {
    currentPet.value = JSON.parse(savedPet)
  }
})
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  width: 100vw;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  margin: 0;
  padding: 0;
}

/* 背景图片 */
.background-image {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-image: url('images/homepage.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  opacity: 0.95; 
  z-index: -1;
}

/* 用户信息侧边栏 */
.user-sidebar {
  position: fixed;
  top: 20px;
  left: 20px;
  z-index: 1000;
}

.current-user {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 25px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  min-width: 180px;
}

.current-user:hover {
  background: rgba(255, 255, 255, 0.95);
  transform: translateY(-2px);
}

.user-info {
  flex: 1;
}

.user-name {
  font-weight: 600;
  color: #2c3e50;
  font-size: 1rem;
}

.user-species {
  font-size: 0.8rem;
  color: #7f8c8d;
}

.dropdown-arrow {
  transition: transform 0.3s ease;
  color: #7f8c8d;
}

.dropdown-arrow.rotated {
  transform: rotate(180deg);
}

.user-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  margin-top: 8px;
  overflow: hidden;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.3s ease;
}

.dropdown-item:hover {
  background: #f8f9fa;
}

.dropdown-item.active {
  background: #e3f2fd;
}

.pet-name {
  font-weight: 500;
}

.logout-item {
  color: #e74c3c;
}

/* 主内容容器 */
.main-content {
  text-align: center;
  padding: 40px;
  width: 100%;
  max-width: 2000px; 
  margin: 0 auto;
}

/* 标题样式 */
.title-section {
  margin-bottom: 80px;
}

.main-title {
  font-family: 'KaiTi', 'STKaiTi', serif;
  font-size: 5rem;
  font-weight: 700;
  color: #FF8C00;
  margin: 0;
  letter-spacing: 6px;
}

/* 功能菜单 - 增加间距 */
.feature-menu {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 40px; 
  padding: 0 50px; 
}

/* 菜单项 */
.menu-item {
  flex: 1;
  padding: 40px 30px; 
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: center;
  min-width: 200px; 
  margin: 0 15px; 
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.15);
  transform: translateY(-5px);
}

/* 菜单图标 */
.menu-icon {
  font-size: 3.5rem;
  margin-bottom: 25px;
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 菜单标题*/
.menu-title {
  font-family: 'SimHei', 'Microsoft YaHei', sans-serif;
  font-size: 2.0rem;
  font-weight: 550;
  margin: 0 0 15px 0;
  letter-spacing: 3px;
  transition: color 0.3s ease;
}

/* 菜单描述 */
.menu-desc {
  font-family: 'KaiTi', 'STKaiti', serif;
  font-size: 1.5rem;
  font-weight: 400;
  margin: 0;
  font-style: italic;
  line-height: 1.5;
  transition: color 0.3s ease;
}

/* 我的空间 */
.space-item .menu-title,
.space-item .menu-desc {
  color: #FF8C00; 
}

.space-item:hover .menu-title,
.space-item:hover .menu-desc {
  color: #FF6347; 
}

/* 看兽医 */
.medical-item .menu-title,
.medical-item .menu-desc {
  color: #87CEEB; 
}

.medical-item:hover .menu-title,
.medical-item:hover .menu-desc {
  color: #6495ED; 
}

/* 去逛街 */
.shopping-item .menu-title,
.shopping-item .menu-desc {
  color: #FFD700; 
}

.shopping-item:hover .menu-title,
.shopping-item:hover .menu-desc {
  color: #FFA500; 
}

/* 竖分隔线 */
.vertical-line {
  width: 1px;
  height: 300px; 
  background: linear-gradient(to bottom, transparent, #bdc3c7, transparent);
  align-self: center;
  margin: 0 10px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .user-sidebar {
    top: 10px;
    left: 10px;
  }
  
  .current-user {
    min-width: 150px;
    padding: 8px 12px;
  }
  
  .feature-menu {
    flex-direction: column;
    gap: 20px;
    padding: 0 20px;
  }
  
  .vertical-line {
    width: 80%;
    height: 1px;
    margin: 20px auto;
  }
  
  .menu-item {
    margin: 0;
    min-width: auto;
    padding: 30px 20px;
  }
  
  .main-title {
    font-size: 3rem;
  }
  
  .menu-title {
    font-size: 1.6rem;
  }
  
  .menu-desc {
    font-size: 1.2rem;
  }
  
  .menu-icon {
    font-size: 2.8rem;
    height: 60px;
    margin-bottom: 20px;
  }
}

@media (max-width: 480px) {
  .main-title {
    font-size: 2.2rem;
    letter-spacing: 3px;
  }
  
  .menu-title {
    font-size: 1.4rem;
  }
  
  .menu-desc {
    font-size: 1rem;
  }
  
  .menu-icon {
    font-size: 2.5rem;
    height: 50px;
    margin-bottom: 15px;
  }
  
  .feature-menu {
    padding: 0 10px;
  }
}
</style>