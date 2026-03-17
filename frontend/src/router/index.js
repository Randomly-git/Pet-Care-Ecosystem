import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ROUTE_CONFIG } from '@/config'
import HomeView from '../views/HomeView.vue'
import AuthView from '../views/AuthView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/login',
      name: 'login',
      component: AuthView,
      meta: {
        requiresAuth: false,
        layout: 'auth'
      }
    },
    {
      path: '/register',
      name: 'register',
      component: AuthView,
      meta: {
        requiresAuth: false,
        layout: 'auth'
      }
    },
    // 活动记录页面
    {
      path: '/activities',
      name: 'activities',
      component: () => import('../views/ActivitiesView.vue'),
      meta: {
        requiresAuth: true,
        title: '宠物日记'
      }
    },
    {
      path: '/about',
      name: 'about',
      redirect: '/'
    },
    // 媒体上传测试页面
    {
      path: '/media-test',
      name: 'media-test',
      component: () => import('../views/MediaUploadTest.vue'),
      meta: {
        requiresAuth: true,
        title: '媒体上传测试'
      }
    },
    // 社区页面
    {
      path: '/community',
      name: 'community',
      component: () => import('../views/CommunityView.vue'),
      meta: {
        requiresAuth: true,
        title: '宠物社区'
      }
    },
    // 社区功能测试页面
    {
      path: '/community-test',
      name: 'community-test',
      component: () => import('../views/CommunityTestView.vue'),
      meta: {
        requiresAuth: true,
        title: '社区功能测试'
      }
    },
    // 朋友圈/社区动态页面
    {
      path: '/moments',
      name: 'moments',
      component: () => import('../views/MomentsView.vue'),
      meta: {
        requiresAuth: true,
        title: '宠物社区'
      }
    },
    // 宠物空间页面
    {
      path: '/space',
      name: 'space',
      component: () => import('../views/PetSpace.vue'),
      meta: {
        requiresAuth: true,
        title: '我的空间'
      }
    },
    // 医疗诊断页面（暂未实现）
    {
      path: '/medical',
      name: 'medical',
      redirect: '/'
    },
    // 购物商城页面（暂未实现）
    {
      path: '/shop',
      name: 'shop',
      redirect: '/'
    },
  ],
})

// 全局前置守卫
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()

  // 初始化认证状态
  if (!authStore.isAuthenticated && authStore.token) {
    await authStore.initAuth()
  }

  // 检查路由是否需要认证
  const requiresAuth = to.meta.requiresAuth
  const isPublicRoute = ROUTE_CONFIG.PUBLIC_ROUTES.includes(to.path)

  // 如果路由需要认证但用户未登录
  if (requiresAuth && !authStore.isAuthenticated) {
    console.log(`路由 ${to.path} 需要认证，重定向到登录页`)
    next({
      path: ROUTE_CONFIG.LOGIN_ROUTE,
      query: { redirect: to.fullPath } // 保存重定向地址
    })
    return
  }

  // 如果用户已登录且访问公共路由（如登录页），但不是主页，重定向到主页
  if (authStore.isAuthenticated && isPublicRoute && to.path !== ROUTE_CONFIG.DEFAULT_REDIRECT) {
    console.log(`用户已登录，从 ${to.path} 重定向到主页`)
    next(ROUTE_CONFIG.DEFAULT_REDIRECT)
    return
  }

  // 其他情况允许访问
  next()
})

// 全局后置钩子
router.afterEach((to, from) => {
  // 设置页面标题
  const title = to.meta.title || '笑猫の窝'
  document.title = title

  // 滚动到页面顶部
  window.scrollTo(0, 0)
})

export default router
