<template>
  <footer class="app-footer">
    <div class="container">
      <div class="footer-content">
        <!-- 左侧：品牌信息 -->
        <div class="footer-section footer-brand">
          <div class="brand">
            <span class="brand-icon">🐾</span>
            <span class="brand-text">笑猫の窝</span>
          </div>
          <p class="brand-slogan">让宠物生活更美好</p>
          <p class="copyright">© 2025 Pet Care Ecosystem. All rights reserved.</p>
        </div>

        <!-- 中间：快速链接 -->
        <div class="footer-section">
          <h4 class="section-title">快速链接</h4>
          <ul class="link-list">
            <li v-for="link in quickLinks" :key="link.path">
              <router-link :to="link.path" class="footer-link">
                {{ link.label }}
              </router-link>
            </li>
          </ul>
        </div>

        <!-- 右侧：联系方式 -->
        <div class="footer-section">
          <h4 class="section-title">联系我们</h4>
          <ul class="contact-list">
            <li class="contact-item">
              <Icon icon="mdi:email" class="contact-icon" />
              <span>2351887@tongji.edu.cn</span>
            </li>
            <li class="contact-item">
              <Icon icon="mdi:phone" class="contact-icon" />
              <span>暂无</span>
            </li>
            <li class="contact-item">
              <Icon icon="mdi:map-marker" class="contact-icon" />
              <span>中国 · 上海 · 同济大学</span>
            </li>
          </ul>
        </div>

        <!-- 社交媒体 -->
        <div class="footer-section">
          <h4 class="section-title">关注我们</h4>
          <div class="social-links">
            <a
              v-for="social in socialLinks"
              :key="social.name"
              :href="social.url"
              class="social-link"
              target="_blank"
              rel="noopener noreferrer"
              :title="social.name"
            >
              <Icon :icon="social.icon" />
            </a>
          </div>
        </div>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { ref } from 'vue'
import { Icon } from '@iconify/vue'

const quickLinks = ref([
  { path: '/', label: '首页' },
  { path: '/space', label: '我的空间' },
  { path: '/medical', label: '兽医诊断' },
  { path: '/shop', label: '购物商城' },
  { path: '/about', label: '关于我们' }
])

const socialLinks = ref([
  { 
    name: '微信', 
    icon: 'mdi:wechat', 
    url: '#' 
  },
  { 
    name: 'GitHub', 
    icon: 'mdi:github', 
    url: 'https://github.com/Randomly-git/Pet-Care-Ecosystem' 
  }
])
</script>

<style scoped>
.app-footer {
  background: linear-gradient(180deg, var(--color-white) 0%, var(--color-gray-50) 100%);
  border-top: 1px solid var(--color-gray-200);
  padding: var(--spacing-16) 0 var(--spacing-8);
  margin-top: var(--spacing-20);
}

.footer-content {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr;
  gap: var(--spacing-12);
}

/* ===== 品牌区域 ===== */
.footer-brand {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3);
}

.brand {
  display: flex;
  align-items: center;
  gap: var(--spacing-3);
  font-weight: var(--font-bold);
  font-size: var(--text-xl);
}

.brand-icon {
  font-size: var(--text-3xl);
}

.brand-text {
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-slogan {
  font-size: var(--text-sm);
  color: var(--color-gray-600);
  margin: 0;
}

.copyright {
  font-size: var(--text-xs);
  color: var(--color-gray-500);
  margin: 0;
  margin-top: var(--spacing-4);
}

/* ===== 区块标题 ===== */
.section-title {
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  color: var(--color-gray-900);
  margin-bottom: var(--spacing-4);
}

/* ===== 链接列表 ===== */
.link-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3);
}

.footer-link {
  font-size: var(--text-sm);
  color: var(--color-gray-600);
  text-decoration: none;
  transition: color var(--duration-base) var(--ease-in-out);
}

.footer-link:hover {
  color: var(--color-primary);
}

/* ===== 联系方式 ===== */
.contact-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-3);
}

.contact-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  font-size: var(--text-sm);
  color: var(--color-gray-600);
}

.contact-item :deep(.contact-icon) {
  font-size: var(--text-base);
  color: var(--color-gray-500);
}

/* ===== 社交链接 ===== */
.social-links {
  display: flex;
  gap: var(--spacing-3);
}

.social-link {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  font-size: var(--text-xl);
  background-color: var(--color-gray-100);
  border-radius: var(--radius-full);
  text-decoration: none;
  transition: all var(--duration-base) var(--ease-in-out);
  color: var(--color-gray-600);
}

.social-link:hover {
  background: var(--gradient-primary);
  transform: translateY(-2px);
  color: white;
}

.social-link :deep(svg) {
  width: 1.25em;
  height: 1.25em;
}

/* ===== 响应式设计 ===== */
@media (max-width: 1024px) {
  .footer-content {
    grid-template-columns: 1fr 1fr;
    gap: var(--spacing-8);
  }
}

@media (max-width: 640px) {
  .footer-content {
    grid-template-columns: 1fr;
    gap: var(--spacing-8);
  }
}
</style>