/**
 * API 测试工具
 * 用于测试和验证 API 集成
 */

import { monitoredApiService } from '@/api/modules'
import { ElMessage } from 'element-plus'

/**
 * API 测试套件
 */
export class ApiTestSuite {
  constructor() {
    this.testResults = []
    this.currentUserId = null
    this.testPetId = null
    this.testActivityId = null
    this.testStatusId = null
  }

  /**
   * 记录测试结果
   */
  logResult(testName, success, error = null, data = null) {
    const result = {
      testName,
      success,
      error: error?.message || error,
      timestamp: new Date().toISOString(),
      data: data ? JSON.stringify(data).substring(0, 200) + '...' : null
    }

    this.testResults.push(result)

    if (success) {
      console.log(`✅ ${testName}: 成功`)
    } else {
      console.error(`❌ ${testName}: 失败 - ${result.error}`)
    }

    return result
  }

  /**
   * 运行所有测试
   */
  async runAllTests() {
    console.log('🚀 开始 API 测试套件...')
    this.testResults = []

    try {
      // 1. 测试宠物管理 API
      await this.testPetAPIs()

      // 2. 测试活动管理 API
      await this.testActivityAPIs()

      // 3. 测试状态管理 API
      await this.testStatusAPIs()

      // 4. 生成测试报告
      this.generateTestReport()

    } catch (error) {
      console.error('测试套件执行失败:', error)
      this.logResult('测试套件执行', false, error)
    }
  }

  /**
   * 测试宠物管理 API
   */
  async testPetAPIs() {
    console.log('\n🐱 测试宠物管理 API...')

    try {
      // 测试获取所有宠物
      const allPets = await monitoredApiService.pets.getAll()
      this.logResult('获取所有宠物', true, null, allPets)

      if (allPets && allPets.length > 0) {
        // 选择第一个宠物进行后续测试
        const testPet = allPets[0]
        this.testPetId = testPet.id
        this.currentUserId = testPet.userId

        // 测试通过ID获取宠物
        const petById = await monitoredApiService.pets.getById(testPet.id)
        this.logResult('通过ID获取宠物', true, null, petById)

        // 测试通过用户ID获取宠物
        const petsByUser = await monitoredApiService.pets.getByUserId(testPet.userId)
        this.logResult('通过用户ID获取宠物', true, null, petsByUser)

        // 测试获取宠物统计
        const petStats = await monitoredApiService.pets.getStats(testPet.userId)
        this.logResult('获取宠物统计', true, null, petStats)
      } else {
        // 如果没有宠物，尝试创建测试宠物
        await this.createTestPet()
      }

    } catch (error) {
      this.logResult('宠物API测试', false, error)
    }
  }

  /**
   * 创建测试宠物
   */
  async createTestPet() {
    try {
      // 这里需要一个测试用户ID，实际使用时应该替换为真实用户ID
      const testUserId = 1 // 假设用户ID为1

      const newPet = await monitoredApiService.pets.create({
        name: '测试宠物_' + Date.now(),
        type: 'dog',
        breed: '测试品种',
        age: '2岁',
        gender: 'male',
        userId: testUserId,
        avatar_url: ''
      })

      this.testPetId = newPet.id
      this.currentUserId = testUserId
      this.logResult('创建测试宠物', true, null, newPet)

    } catch (error) {
      this.logResult('创建测试宠物', false, error)
    }
  }

  /**
   * 测试活动管理 API
   */
  async testActivityAPIs() {
    console.log('\n🎾 测试活动管理 API...')

    if (!this.testPetId) {
      this.logResult('活动API测试', false, '需要有效的宠物ID')
      return
    }

    try {
      // 测试获取所有活动
      const allActivities = await monitoredApiService.activities.getAll()
      this.logResult('获取所有活动', true, null, allActivities)

      if (allActivities && allActivities.length > 0) {
        this.testActivityId = allActivities[0].activityId
      }

      // 测试获取活动记录
      const activityRecords = await monitoredApiService.activities.getRecords(this.testPetId, {
        startDate: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString(),
        endDate: new Date().toISOString()
      })
      this.logResult('获取活动记录', true, null, activityRecords)

      // 测试创建活动记录（如果活动ID存在）
      if (this.testActivityId) {
        const newRecord = await monitoredApiService.activities.createRecord(this.testPetId, {
          activityId: this.testActivityId,
          description: 'API测试活动记录',
          date: new Date().toISOString()
        })
        this.logResult('创建活动记录', true, null, newRecord)
      }

      // 测试获取活动统计
      const activityStats = await monitoredApiService.activities.getStats({
        userId: this.currentUserId
      })
      this.logResult('获取活动统计', true, null, activityStats)

    } catch (error) {
      this.logResult('活动API测试', false, error)
    }
  }

  /**
   * 测试状态管理 API
   */
  async testStatusAPIs() {
    console.log('\n🏥 测试状态管理 API...')

    if (!this.currentUserId) {
      this.logResult('状态API测试', false, '需要有效的用户ID')
      return
    }

    try {
      // 测试获取用户状态
      const userStatuses = await monitoredApiService.status.getByUserId(this.currentUserId)
      this.logResult('获取用户状态', true, null, userStatuses)

      if (userStatuses && userStatuses.length > 0) {
        this.testStatusId = userStatuses[0].statusId
      }

      // 测试获取状态记录
      if (this.testPetId) {
        const statusRecords = await monitoredApiService.status.getRecords(this.testPetId, {
          startDate: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString(),
          endDate: new Date().toISOString()
        })
        this.logResult('获取状态记录', true, null, statusRecords)
      }

      // 测试创建状态（如果需要）
      if (!this.testStatusId) {
        const newStatus = await monitoredApiService.status.create({
          userId: this.currentUserId,
          statusName: '测试状态_' + Date.now()
        })
        this.testStatusId = newStatus.statusId
        this.logResult('创建测试状态', true, null, newStatus)
      }

      // 测试创建状态记录（如果状态ID和宠物ID都存在）
      if (this.testStatusId && this.testPetId) {
        const newStatusRecord = await monitoredApiService.status.createRecord({
          petId: this.testPetId,
          statusId: this.testStatusId,
          statusDescription: 'API测试状态记录',
          startDate: new Date().toISOString()
        })
        this.logResult('创建状态记录', true, null, newStatusRecord)
      }

      // 测试获取状态统计
      const statusStats = await monitoredApiService.status.getStats({
        userId: this.currentUserId
      })
      this.logResult('获取状态统计', true, null, statusStats)

    } catch (error) {
      this.logResult('状态API测试', false, error)
    }
  }

  /**
   * 生成测试报告
   */
  generateTestReport() {
    const totalTests = this.testResults.length
    const passedTests = this.testResults.filter(r => r.success).length
    const failedTests = totalTests - passedTests
    const successRate = totalTests > 0 ? ((passedTests / totalTests) * 100).toFixed(2) : 0

    console.log('\n📊 API 测试报告')
    console.log('='.repeat(50))
    console.log(`总测试数: ${totalTests}`)
    console.log(`通过: ${passedTests}`)
    console.log(`失败: ${failedTests}`)
    console.log(`成功率: ${successRate}%`)

    if (failedTests > 0) {
      console.log('\n❌ 失败的测试:')
      this.testResults
        .filter(r => !r.success)
        .forEach(r => {
          console.log(`  - ${r.testName}: ${r.error}`)
        })
    }

    console.log('\n📝 详细测试结果:')
    this.testResults.forEach(r => {
      const status = r.success ? '✅' : '❌'
      console.log(`  ${status} ${r.testName}`)
      if (r.data) {
        console.log(`    数据: ${r.data}`)
      }
    })

    // 显示用户友好的消息
    if (successRate === 100) {
      ElMessage.success(`所有API测试通过！共 ${totalTests} 个测试`)
    } else if (successRate >= 80) {
      ElMessage.warning(`API测试大部分通过 (${successRate}%)，${failedTests} 个测试失败`)
    } else {
      ElMessage.error(`API测试失败严重 (${successRate}%)，请检查后端连接`)
    }

    return {
      totalTests,
      passedTests,
      failedTests,
      successRate: parseFloat(successRate),
      results: this.testResults
    }
  }

  /**
   * 清理测试数据
   */
  async cleanupTestData() {
    console.log('\n🧹 清理测试数据...')

    try {
      // 这里可以添加清理逻辑，比如删除测试创建的宠物、状态等
      // 注意：实际使用时要小心，避免删除重要数据

      if (this.testPetId) {
        // 注意：这会永久删除宠物数据，仅在测试时使用
        // await monitoredApiService.pets.delete(this.testPetId)
        console.log('跳过删除测试宠物（安全考虑）')
      }

      console.log('测试数据清理完成')
      ElMessage.info('测试数据清理完成')

    } catch (error) {
      console.error('清理测试数据失败:', error)
      ElMessage.error('清理测试数据失败')
    }
  }

  /**
   * 获取测试结果摘要
   */
  getTestSummary() {
    const totalTests = this.testResults.length
    const passedTests = this.testResults.filter(r => r.success).length
    const failedTests = totalTests - passedTests
    const successRate = totalTests > 0 ? ((passedTests / totalTests) * 100).toFixed(2) : 0

    return {
      totalTests,
      passedTests,
      failedTests,
      successRate: parseFloat(successRate),
      results: this.testResults,
      testIds: {
        petId: this.testPetId,
        userId: this.currentUserId,
        activityId: this.testActivityId,
        statusId: this.testStatusId
      }
    }
  }
}

/**
 * 运行快速健康检查
 */
export async function runHealthCheck() {
  console.log('🏥 运行 API 健康检查...')

  const testSuite = new ApiTestSuite()

  try {
    // 快速检查几个关键API
    const allPets = await monitoredApiService.pets.getAll()
    const allActivities = await monitoredApiService.activities.getAll()

    return {
      status: 'healthy',
      petsCount: (allPets || []).length,
      activitiesCount: (allActivities || []).length,
      timestamp: new Date().toISOString()
    }
  } catch (error) {
    console.error('健康检查失败:', error)
    return {
      status: 'unhealthy',
      error: error.message,
      timestamp: new Date().toISOString()
    }
  }
}

// 默认导出
export default ApiTestSuite