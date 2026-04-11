<template>
  <div class="map-view-page">
    <AppHeader />
    <div class="map-layout">

      <!-- 左侧内容面板: 搜索与列表 -->
      <div class="side-panel">
        <div class="panel-header">
          <h1 class="panel-title">周边探索</h1>
          <p class="panel-subtitle">发现身边的宝藏宠物据点</p>

          <div class="search-bar">
            <el-input v-model="searchKeyword" placeholder="搜索附近的医院、宠物店..." learable @keyup.enter="handleSearch">
              <template #prefix>
                <el-icon>
                  <Search />
                </el-icon>
              </template>
              <template #append>
                <el-button @click="handleSearch">搜索</el-button>
              </template>
            </el-input>
          </div>

          <div class="quick-filters">
            <el-button :type="activeFilter === '宠物医院' ? 'primary' : 'default'" round @click="setFilter('宠物医院')">🏥
              宠物医院</el-button>
            <el-button :type="activeFilter === '宠物店' ? 'primary' : 'default'" round @click="setFilter('宠物店')">🏬
              宠物店</el-button>
          </div>
        </div>

        <div class="poi-list" v-loading="loading">
          <div v-if="poiList.length === 0 && !loading" class="empty-state">
            暂未找到附近的 "{{ searchKeyword || activeFilter }}"
          </div>

          <div v-for="(item, index) in poiList" :key="item.id" class="poi-card"
            :class="{ active: activePoiId === item.id }" @click="focusPoi(item)">
            <div class="poi-header">
              <span class="poi-number">{{ index + 1 }}</span>
              <h3 class="poi-name">{{ item.name }}</h3>
              <span class="poi-distance" v-if="item.distance">{{ item.distance }}m</span>
            </div>
            <div class="poi-body">
              <div class="poi-rating" v-if="item.rating">
                <el-rate :model-value="Number(item.rating) || 4.5" disabled show-score text-color="#ff9900"
                  score-template="{value}分" />
              </div>
              <p class="poi-address"><el-icon>
                  <Location />
                </el-icon> {{ item.address }}</p>
              <p class="poi-phone" v-if="item.tel"><el-icon>
                  <Phone />
                </el-icon> {{ item.tel }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧地图容器 -->
      <div class="map-container" id="amap-container" v-loading="mapLoading">
        <div class="map-overlay-tools">
          <el-button 
            :type="isSelectingPoint ? 'primary' : 'default'" 
            circle 
            size="large" 
            @click="toggleSelectPoint" 
            :title="isSelectingPoint ? '取消地图选点' : '开启地图选点'"
            style="margin-bottom: 12px; display: block;"
          >
            <el-icon>
              <Position />
            </el-icon>
          </el-button>
          <el-button circle size="large" @click="returnToCurrentLocation" title="回到我的位置" style="margin-left: 0;">
            <el-icon>
              <Aim />
            </el-icon>
          </el-button>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, shallowRef } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'
import { Search, Location, Phone, Aim, Position } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AppHeader from '@/components/layout/AppHeader.vue'

// 高德地图开发者 Web API Key
const AMAP_KEY = import.meta.env.VITE_AMAP_KEY // 用户提供的 Key 现已妥善保管在环境变量(.env.development)中

// 响应式状态
const mapLoading = ref(true)
const loading = ref(false)
const searchKeyword = ref('')
const activeFilter = ref('宠物医院')
const poiList = ref([])
const activePoiId = ref(null)

// 地图内部状态 (使用 shallowRef 避免被 Vue 深度代理导致性能问题)
const map = shallowRef(null)
const AMapIns = shallowRef(null)
const placeSearch = shallowRef(null)
const markers = shallowRef([])
const infoWindow = shallowRef(null)
const centerPosition = ref([116.397428, 39.90923]) // 默认北京天安门

onMounted(() => {
  initMap()
})

onUnmounted(() => {
  if (map.value) {
    map.value.destroy()
  }
})

const initMap = async () => {
  window._AMapSecurityConfig = {
    // 现代应用由于跨域原因，高德强行要求设置安全密钥(securityJsCode)！只配 Key 会直接导致搜寻、定位服务报 USERKEY_PLAT_NOMATCH 错误
    securityJsCode: import.meta.env.VITE_AMAP_SECURITY_CODE || '',
  }

  try {
    const AMap = await AMapLoader.load({
      key: AMAP_KEY,
      version: '2.0',
      plugins: ['AMap.PlaceSearch', 'AMap.Geolocation', 'AMap.Scale', 'AMap.ToolBar']
    })

    AMapIns.value = AMap

    map.value = new AMap.Map('amap-container', {
      zoom: 13,
      center: centerPosition.value,
      isHotspot: true, // 开启地图自带POI点击功能
      mapStyle: 'amap://styles/macaron', // 偏向马卡龙/苹果感柔和风格
      showBuildingBlock: true, // 3D建筑
    })

    // 监听高德地图自带图层地标的点击 (例如直接点击底图上的同济大学字样)
    map.value.on('hotspotclick', (e) => {
      // e.lnglat 就是坐标 e.name 是名字
      const fakePoi = {
        name: e.name,
        location: e.lnglat,
        address: '高德地图基础地标',
        id: e.id || Math.random().toString()
      }
      openInfoWindow(fakePoi)
      map.value.setZoomAndCenter(16, e.lnglat) // 视角跳转过去
    })

    // 添加基础控件
    map.value.addControl(new AMap.Scale())
    map.value.addControl(new AMap.ToolBar({ position: 'RB' })) // 右下角缩放工具

    // 默认提供一个信息大屏
    infoWindow.value = new AMap.InfoWindow({
      offset: new AMap.Pixel(0, -30),
      isCustom: false,
      autoMove: true
    })

    // 监听地图点击选点
    map.value.on('click', handleMapClick)

    // 尝试定位
    locateCurrentUser(AMap)

  } catch (e) {
    console.error('高德地图加载失败', e)
    ElMessage.error('地图加载失败，请检查网络或配置项')
    mapLoading.value = false
  }
}

const locateCurrentUser = (AMap) => {
  const geolocation = new AMap.Geolocation({
    enableHighAccuracy: true,
    timeout: 10000,
    buttonPosition: 'RB',
    buttonOffset: new AMap.Pixel(10, 20),
    zoomToAccuracy: true
  })

  map.value.addControl(geolocation)

  geolocation.getCurrentPosition((status, result) => {
    mapLoading.value = false
    if (status === 'complete') {
      centerPosition.value = [result.position.lng, result.position.lat]
      searchNearBy()
    } else {
      ElMessage.warning('自动定位失败，将使用默认位置。如需精准查找请在浏览器允许定位。')
      searchNearBy()
    }
  })
}

const searchCenterMarker = shallowRef(null)
const isSelectingPoint = ref(false)

const toggleSelectPoint = () => {
  isSelectingPoint.value = !isSelectingPoint.value
  if (isSelectingPoint.value) {
    ElMessage.info('已开启地图选点，请直接点击地图区域')
  } else {
    ElMessage.info('已取消地图选点')
  }
}

const handleMapClick = (e) => {
  if (!isSelectingPoint.value) return // 必须开启了选点模式才处理点击

  centerPosition.value = [e.lnglat.lng, e.lnglat.lat]
  
  // 更新或创建搜索中心点标记
  if (!searchCenterMarker.value && AMapIns.value) {
    searchCenterMarker.value = new AMapIns.value.Marker({
      position: centerPosition.value,
      icon: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_r.png',
      offset: new AMapIns.value.Pixel(-9, -31),
      title: '当前搜索中心'
    })
    map.value.add(searchCenterMarker.value)
  } else if (searchCenterMarker.value) {
    searchCenterMarker.value.setPosition(centerPosition.value)
  }
  
  searchNearBy()
  ElMessage.success('已重新选定探索中心点并搜索附近')
  isSelectingPoint.value = false // 选一次自动关闭
}

const returnToCurrentLocation = () => {
  if (!map.value || !centerPosition.value) return
  map.value.setZoomAndCenter(14, centerPosition.value)
}

const setFilter = (filterType) => {
  if (activeFilter.value === filterType) {
    activeFilter.value = '' // 允许再次点击时取消选择
  } else {
    activeFilter.value = filterType
  }
  searchKeyword.value = '' // 清空自定义搜索
  searchNearBy()
}

const handleSearch = () => {
  if (!searchKeyword.value.trim()) {
    searchKeyword.value = ''
    searchNearBy()
    return
  }
  // 如果用户搜了其它东西，先取消过滤高亮
  activeFilter.value = ''
  searchNearBy(searchKeyword.value)
}

const searchNearBy = (keyword = null) => {
  loading.value = true
  
  // 智能记忆搜索词汇: 优先使用刚传入的关键词，其次判断是否有自定义搜索输入，最后使用侧边栏过滤按钮状态
  const queryWord = keyword || searchKeyword.value || activeFilter.value

  if (!placeSearch.value && AMapIns.value) {
    placeSearch.value = new AMapIns.value.PlaceSearch({
      pageSize: 20,
      pageIndex: 1,
      city: '全国', // 将 city 设置为全国，保证纯文本搜索(如同济大学)时跨地域也能搜出结果
      citylimit: false,
      autoFitView: true,
      extensions: 'all', 
    })
  }

  // 清除旧点
  clearMarkers()
  infoWindow.value?.close()
  activePoiId.value = null

  if (!queryWord) {
    loading.value = false
    poiList.value = []
    return // 如果没有搜索词（比如取消选中了所有过滤），直接清空列表并返回
  }

  // 执行搜索
  const handleResponse = (status, result) => {
    loading.value = false
    if (status === 'complete' && result.info === 'OK') {
      poiList.value = result.poiList.pois
      renderMarkers(poiList.value)
    } else {
      poiList.value = []
      console.error('高德搜寻 API 拦截或失败:', status, result)
      // 在屏幕上明确抛出致命的安全密钥拦截错误！
      if (status === 'error' && (result?.info === 'USERKEY_PLAT_NOMATCH' || !result)) {
        ElMessage.error({
          message: '❌ 高德接口调用被致命回绝！缺少并匹配对应的【安全密钥(Security Code)】。请赴高德控制台复制后填入 .env.development 中',
          duration: 10000
        })
      } else {
        ElMessage.warning(`提示: 此位置暂时检索不到"${queryWord}" (${status})`)
      }
    }
  }

  // 解除严格的公里限制：如果用户手动输入了类似"杨浦"或"餐厅"，使用全国通用匹配引擎，否则如果是过滤周边设施，用巨大的 50km 半径囊括最近的一列
  if (searchKeyword.value) {
    placeSearch.value.search(queryWord, handleResponse)
  } else {
    placeSearch.value.searchNearBy(queryWord, centerPosition.value, 50000, handleResponse)
  }
}

// 渲染地图标记
const renderMarkers = (pois) => {
  const AMap = AMapIns.value

  pois.forEach((poi, index) => {
    const isHospital = poi.name.includes('医院') || poi.type?.includes('医院')
    const markerColor = isHospital ? '#0071e3' : '#34c759'

    // 自定义精美 Marker
    const markerContent = `
      <div class="custom-marker" style="background:${markerColor}">
        <div class="marker-number">${index + 1}</div>
        <div class="marker-pulse"></div>
      </div>
    `

    const marker = new AMap.Marker({
      position: poi.location,
      content: markerContent,
      offset: new AMap.Pixel(-15, -30),
      extData: { poi }
    })

    // 绑定点击事件
    marker.on('click', () => {
      activePoiId.value = poi.id
      openInfoWindow(poi)
    })

    map.value.add(marker)
    markers.value.push(marker)
  })
}

const clearMarkers = () => {
  if (markers.value.length > 0 && map.value) {
    map.value.remove(markers.value)
    markers.value = []
  }
}

const focusPoi = (poi) => {
  activePoiId.value = poi.id
  if (map.value) {
    map.value.setZoomAndCenter(16, poi.location) // 放大并平移
    openInfoWindow(poi)
  }
}

const openInfoWindow = (poi) => {
  const AMap = AMapIns.value
  const ratingStr = poi.rating ? `${poi.rating} 分` : '暂无评分'

  // Apple风玻璃态信息窗
  const content = `
    <div class="poi-info-window">
      <h3 class="iw-title">${poi.name}</h3>
      <div class="iw-content">
        <p><strong><span style="color:#ff9900">★</span> 评分:</strong> ${ratingStr}</p>
        <p><strong>📍 地址:</strong> ${poi.address}</p>
        ${poi.tel ? `<p><strong>📞 电话:</strong> ${poi.tel}</p>` : ''}
        ${poi.distance ? `<p><strong>🚶 距离:</strong> ${poi.distance}m</p>` : ''}
      </div>
    </div>
  `

  infoWindow.value.setContent(content)
  infoWindow.value.open(map.value, poi.location)
}
</script>

<style scoped>
.map-view-page {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: #f5f5f7;
}

.map-layout {
  display: flex;
  flex: 1;
  width: 100%;
  overflow: hidden;
}

/* 左侧面板 - 加入动态渐变边框效果 */
.side-panel {
  width: 380px;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  z-index: 10;
  position: relative;
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.04);
}

/* 侧边栏发光动态边框 */
.side-panel::after {
  content: '';
  position: absolute;
  top: 0; right: 0; bottom: 0;
  width: 3px;
  background: linear-gradient(180deg, #38bdf8, #818cf8, #34d399, #38bdf8);
  background-size: 100% 300%;
  animation: borderGlow 4s linear infinite;
}

@keyframes borderGlow {
  0% { background-position: 0% 0%; }
  100% { background-position: 0% 100%; }
}

.panel-header {
  padding: 24px 20px 16px;
  background: white;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
}

.panel-title {
  font-size: 24px;
  font-weight: 700;
  color: #1d1d1f;
  margin: 0 0 4px;
  letter-spacing: -0.01em;
}

.panel-subtitle {
  font-size: 13px;
  color: #86868b;
  margin: 0 0 16px;
}

.search-bar {
  margin-bottom: 16px;
}

.search-bar :deep(.el-input__wrapper) {
  border-radius: 12px;
  background: #f5f5f7;
  box-shadow: none !important;
  /* 覆盖element默认深边框 */
}

.search-bar :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #0071e3 inset !important;
  background: white;
}

.quick-filters {
  display: flex;
  gap: 12px;
}

/* 列表渲染区 */
.poi-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f5f7;
}

.poi-list::-webkit-scrollbar {
  width: 6px;
}

.poi-list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 10px;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #86868b;
  font-size: 14px;
}

.poi-card {
  background: white;
  padding: 16px;
  margin-bottom: 12px;
  cursor: pointer;
  border: 2px solid transparent;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
  border-radius: 12px;
  overflow: hidden;
}

.poi-card::before {
  content: '';
  position: absolute;
  top: 0; left: 0; width: 100%; height: 100%;
  border-radius: 12px;
  box-shadow: 0 0 0 2px transparent inset;
  transition: box-shadow 0.3s ease;
  pointer-events: none;
}

.poi-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(56, 189, 248, 0.2);
}

.poi-card:hover::before {
  box-shadow: 0 0 0 2px #38bdf8 inset;
}

.poi-card.active {
  background: #f0f9ff;
  border-color: #0ea5e9;
  box-shadow: 0 8px 24px rgba(14, 165, 233, 0.25);
}

.poi-card.active::before {
  box-shadow: 0 0 0 2px #0ea5e9 inset;
}

.poi-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.poi-number {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #1d1d1f;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  margin-right: 10px;
}

.active .poi-number {
  background: #0071e3;
}

.poi-name {
  font-size: 16px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.poi-distance {
  font-size: 12px;
  color: #0071e3;
  font-weight: 500;
  background: rgba(0, 113, 227, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
}

.poi-body {
  padding-left: 34px;
}

.poi-rating {
  margin-bottom: 4px;
  display: flex;
  align-items: center;
}

.poi-rating :deep(.el-rate__text) {
  font-size: 12px;
  margin-left: 4px;
}

.poi-address,
.poi-phone {
  margin: 4px 0 0;
  font-size: 13px;
  color: #86868b;
  display: flex;
  align-items: flex-start;
  gap: 4px;
  line-height: 1.4;
}

/* 右侧地图区 */
/* 地图容器 - 全局发光边框 */
.map-container {
  flex: 1;
  position: relative;
  background: #e5e5ea;
  /* 动态霓虹内阴影边框 */
  box-shadow: inset 0 0 0 4px rgba(56, 189, 248, 0.3), 
              inset 0 0 20px rgba(129, 140, 248, 0.2);
  transition: box-shadow 0.3s ease;
}

/* 给容器加一个发光动画效果 */
.map-container::after {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  pointer-events: none;
  box-shadow: inset 0 0 0 4px transparent;
  animation: mapGlow 4s alternate infinite;
  z-index: 100;
}

@keyframes mapGlow {
  0% { box-shadow: inset 0 0 0 4px rgba(56, 189, 248, 0.4); }
  50% { box-shadow: inset 0 0 0 4px rgba(52, 211, 153, 0.4); }
  100% { box-shadow: inset 0 0 0 4px rgba(129, 140, 248, 0.4); }
}

.map-overlay-tools {
  position: absolute;
  bottom: 40px;
  right: 20px;
  z-index: 100;
}

/* 全局覆盖高德地图组件的样式以贴合 Apple 风格 */
:deep(.custom-marker) {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bolder;
  font-size: 14px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  border: 2px solid white;
  position: relative;
  transition: transform 0.2s;
}

:deep(.custom-marker:hover) {
  transform: scale(1.15);
  z-index: 999 !important;
}

:deep(.marker-pulse) {
  position: absolute;
  top: -4px;
  left: -4px;
  right: -4px;
  bottom: -4px;
  border-radius: 50%;
  background: inherit;
  opacity: 0.4;
  z-index: -1;
  animation: marker-pulse 2s infinite ease-out;
}

@keyframes marker-pulse {
  0% {
    transform: scale(1);
    opacity: 0.5;
  }

  100% {
    transform: scale(1.6);
    opacity: 0;
  }
}

:deep(.poi-info-window) {
  padding: 8px;
  min-width: 220px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
}

:deep(.iw-title) {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1d1d1f;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  padding-bottom: 8px;
}

:deep(.iw-content p) {
  margin: 4px 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}

:deep(.iw-content strong) {
  color: #1d1d1f;
  font-weight: 500;
}

/* 覆盖高德原生弹窗样式使其更美观 */
:deep(.amap-info-content) {
  border-radius: 16px !important;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1) !important;
  border: none !important;
  padding: 16px !important;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
}

:deep(.amap-info-close) {
  top: 14px !important;
  right: 14px !important;
  color: #86868b !important;
}

:deep(.amap-logo),
:deep(.amap-copyright) {
  display: none !important;
  /* 隐藏高德水印以保持高度干净的UI，注:商用请遵守协议 */
}
</style>
