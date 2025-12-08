/**
 * API服务测试文件
 * 用于验证新的API服务是否正常工作
 */

import { authService, petService, activityService } from './index'
import { useApi, useAuth, usePets } from '@/composables/useApi'

// 测试配置
const TEST_CONFIG = {
  testUser: {
    name: 'testuser',
    password: 'password123'
  },
  testPet: {
    name: '豆豆',
    species: '狗',
    breed: '金毛',
    birthday: '2020-01-01'
  }
}

// 测试工具函数
class ApiTester {
  constructor() {
    this.results = []
  }

  log(message, type = 'info') {
    const timestamp = new Date().toLocaleTimeString()
    const logEntry = { timestamp, message, type }
    this.results.push(logEntry)
    console.log(`[${timestamp}] ${type.toUpperCase()}: ${message}`)
  }

  success(message) {
    this.log(message, 'success')
  }

  error(message) {
    this.log(message, 'error')
  }

  warn(message) {
    this.log(message, 'warn')
  }

  async test(testName, testFn) {
    this.log(`开始测试: ${testName}`)
    try {
      await testFn()
      this.success(`✅ ${testName} - 通过`)
    } catch (error) {
      this.error(`❌ ${testName} - 失败: ${error.message}`)
    }
  }

  async delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms))
  }

  printResults() {
    console.log('\n=== 测试结果汇总 ===')
    this.results.forEach(result => {
      const icon = result.type === 'success' ? '✅' :
                   result.type === 'error' ? '❌' :
                   result.type === 'warn' ? '⚠️' : 'ℹ️'
      console.log(`${icon} [${result.timestamp}] ${result.message}`)
    })
  }
}

// 创建测试实例
const tester = new ApiTester()

// 测试认证服务
async function testAuthService() {
  await tester.test('用户登录', async () => {
    const result = await authService.login(TEST_CONFIG.testUser)
    if (!result.token) {
      throw new Error('登录失败：未获取到token')
    }
    console.log('登录成功，用户ID:', result.user.id)
  })

  await tester.test('获取当前用户', async () => {
    const currentUser = authService.getCurrentUser()
    if (!currentUser) {
      throw new Error('未获取到当前用户信息')
    }
    console.log('当前用户:', currentUser.name)
  })

  await tester.test('检查认证状态', async () => {
    const isAuth = authService.isAuthenticated()
    console.log('认证状态:', isAuth)
  })
}

// 测试宠物服务
async function testPetService() {
  await tester.test('获取宠物列表', async () => {
    const user = authService.getCurrentUser()
    if (!user) throw new Error('用户未登录')

    const pets = await petService.getPets(user.id)
    console.log('宠物列表:', pets.map(p => p.name))
  })

  await tester.test('创建宠物', async () => {
    const user = authService.getCurrentUser()
    if (!user) throw new Error('用户未登录')

    const petData = {
      ...TEST_CONFIG.testPet,
      userId: user.id
    }

    const newPet = await petService.createPet(petData)
    if (!newPet.id) {
      throw new Error('创建宠物失败：未获取到宠物ID')
    }
    console.log('创建宠物成功:', newPet.name)
    return newPet
  })
}

// 测试活动服务
async function testActivityService() {
  await tester.test('获取活动种类', async () => {
    const kinds = await activityService.getActivityKinds()
    if (!kinds || kinds.length === 0) {
      throw new Error('获取活动种类失败')
    }
    console.log('活动种类:', kinds.map(k => k.name))
  })

  await tester.test('获取用户活动', async () => {
    const user = authService.getCurrentUser()
    if (!user) throw new Error('用户未登录')

    const activities = await activityService.getActivities(user.id)
    console.log('用户活动:', activities.map(a => a.name))
  })
}

// 测试组合式API
async function testComposables() {
  await tester.test('useAuth Hook', async () => {
    const { user, isAuthenticated } = useAuth()
    console.log('Auth Hook - 用户:', user.value?.name)
    console.log('Auth Hook - 已认证:', isAuthenticated.value)
  })

  await tester.test('usePets Hook', async () => {
    const user = authService.getCurrentUser()
    if (!user) throw new Error('用户未登录')

    const { pets, isLoading } = usePets(user.id)
    await tester.delay(1000) // 等待数据加载
    console.log('Pets Hook - 宠物数量:', pets.value?.length)
    console.log('Pets Hook - 加载中:', isLoading.value)
  })
}

// 测试错误处理
async function testErrorHandling() {
  await tester.test('404错误处理', async () => {
    try {
      await petService.getPet(99999) // 不存在的宠物ID
      throw new Error('应该抛出404错误')
    } catch (error) {
      if (error.status === 404) {
        console.log('404错误正确处理')
      } else {
        throw error
      }
    }
  })

  await tester.test('网络错误处理', async () => {
    // 测试无效的URL
    const invalidUrl = 'http://invalid-url-that-does-not-exist.com'
    try {
      // 这里需要直接调用HTTP客户端来测试网络错误
      console.log('网络错误处理测试需要直接调用HttpClient')
    } catch (error) {
      console.log('网络错误已捕获:', error.message)
    }
  })
}

// 主测试函数
export async function runApiTests() {
  console.log('🚀 开始API服务测试...\n')

  try {
    // 测试认证服务
    await testAuthService()
    await tester.delay(500)

    // 测试宠物服务
    await testPetService()
    await tester.delay(500)

    // 测试活动服务
    await testActivityService()
    await tester.delay(500)

    // 测试组合式API
    await testComposables()
    await tester.delay(500)

    // 测试错误处理
    await testErrorHandling()

  } catch (error) {
    tester.error(`测试过程中发生错误: ${error.message}`)
  }

  // 打印测试结果
  tester.printResults()

  console.log('\n✨ API服务测试完成！')
}

// 导出测试函数供外部调用
export { tester }

// 如果直接运行此文件，自动执行测试
if (typeof window !== 'undefined') {
  // 在浏览器环境中，可以通过控制台调用
  window.runApiTests = runApiTests
  console.log('在控制台中输入 runApiTests() 来运行API测试')
}