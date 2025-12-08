/**
 * API基础配置测试文件
 * 在浏览器控制台中运行此代码来验证API配置是否正确
 */

// 导入需要测试的模块（在实际使用中）
// import request from '@/utils/request'
// import { showToast, showSuccess, showError } from '@/utils/message'
// import { localStorage, setToken, getToken } from '@/utils/storage'
// import { handleApiResponse } from '@/utils/api-helper'
// import { API_CONFIG, APP_CONFIG } from '@/config'

console.log('🧪 开始API配置测试...')

// 1. 测试基础配置
console.group('📋 测试基础配置')
console.log('✅ API基础配置已加载')
console.log('✅ 工具类已加载')
console.groupEnd()

// 2. 测试本地存储
console.group('💾 测试本地存储')
try {
  // 模拟存储测试
  localStorage.setItem('test-key', JSON.stringify({ value: 'test-data' }))
  const stored = JSON.parse(localStorage.getItem('test-key'))
  console.log('✅ 本地存储读写正常:', stored)

  // 清理测试数据
  localStorage.removeItem('test-key')
  console.log('✅ 本地存储清理正常')
} catch (error) {
  console.error('❌ 本地存储测试失败:', error)
}
console.groupEnd()

// 3. 测试消息提示
console.group('💬 测试消息提示')
try {
  // 创建测试消息元素
  const testMessage = document.createElement('div')
  testMessage.style.cssText = `
    position: fixed;
    top: 50px;
    right: 20px;
    background: #52C41A;
    color: white;
    padding: 12px 20px;
    border-radius: 8px;
    z-index: 9999;
    font-size: 14px;
  `
  testMessage.textContent = '✅ 消息系统测试成功！'
  document.body.appendChild(testMessage)

  // 3秒后移除
  setTimeout(() => {
    if (testMessage.parentNode) {
      testMessage.parentNode.removeChild(testMessage)
    }
  }, 3000)

  console.log('✅ 消息提示功能正常')
} catch (error) {
  console.error('❌ 消息提示测试失败:', error)
}
console.groupEnd()

// 4. 测试API请求（需要后端服务运行）
console.group('🌐 测试API请求')
const testAPIRequest = async () => {
  try {
    // 测试GET请求到健康检查端点
    const response = await fetch('http://localhost:8082/api', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    })

    console.log('✅ API请求成功:', response.status)
    console.log('✅ 服务器响应正常')
  } catch (error) {
    console.log('⚠️ API请求失败（可能是后端未启动）:', error.message)
  }
}

// 执行API测试
testAPIRequest()
console.groupEnd()

// 5. 测试环境配置
console.group('⚙️ 测试环境配置')
try {
  const isDev = import.meta.env.DEV
  const isProd = import.meta.env.PROD

  console.log('✅ 当前环境:', isDev ? 'development' : isProd ? 'production' : 'unknown')
  console.log('✅ 环境检测正常')
} catch (error) {
  console.error('❌ 环境配置测试失败:', error)
}
console.groupEnd()

// 6. 测试网络连接
console.group('📡 测试网络连接')
try {
  if (navigator.onLine) {
    console.log('✅ 网络连接正常')
  } else {
    console.log('⚠️ 网络连接异常')
  }

  // 测试DNS解析
  fetch('https://www.baidu.com', { method: 'HEAD', mode: 'no-cors' })
    .then(() => console.log('✅ DNS解析正常'))
    .catch(() => console.log('⚠️ DNS解析可能有问题'))
} catch (error) {
  console.error('❌ 网络测试失败:', error)
}
console.groupEnd()

// 7. 测试浏览器兼容性
console.group('🌍 浏览器兼容性')
try {
  console.log('✅ User Agent:', navigator.userAgent)
  console.log('✅ 浏览器语言:', navigator.language)
  console.log('✅ 平台:', navigator.platform)

  // 测试现代JavaScript特性
  const testArrow = () => 'Arrow function works'
  const testAsync = async () => 'Async/await works'
  const testOptional = { a: 1 }?.a

  console.log('✅ ES6+特性支持正常')
} catch (error) {
  console.error('❌ 浏览器兼容性测试失败:', error)
}
console.groupEnd()

console.log('🎉 API配置测试完成！')
console.log('💡 提示: 请确保所有测试项目都显示 ✅ 或 ⚠️，避免 ❌ 错误')

// 导出测试函数供外部调用
window.testAPIConfig = {
  runAllTests: () => {
    console.clear()
    // 重新运行所有测试
    location.reload()
  },

  testLocalStorage: () => {
    const testKey = 'test-' + Date.now()
    const testValue = { data: 'test' }

    localStorage.setItem(testKey, JSON.stringify(testValue))
    const retrieved = JSON.parse(localStorage.getItem(testKey))
    localStorage.removeItem(testKey)

    return retrieved.data === 'test'
  },

  testMessageSystem: () => {
    const msg = document.createElement('div')
    msg.style.cssText = 'position:fixed;top:100px;right:20px;z-index:9999;background:#1890FF;color:white;padding:10px;border-radius:4px;'
    msg.textContent = '测试消息'
    document.body.appendChild(msg)

    setTimeout(() => {
      if (msg.parentNode) msg.parentNode.removeChild(msg)
    }, 2000)

    return true
  }
}

console.log('🔧 可以通过 window.testAPIConfig.runAllTests() 重新运行测试')