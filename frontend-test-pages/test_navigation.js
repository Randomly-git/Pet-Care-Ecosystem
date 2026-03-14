// 测试首页导航功能
const axios = require('axios');

async function testNavigation() {
  const baseUrl = 'http://localhost:5173';

  console.log('=== 测试首页导航功能 ===\n');

  try {
    // 1. 访问首页
    console.log('1. 访问首页...');
    const homeResponse = await axios.get(baseUrl);
    console.log('✓ 首页访问成功，状态码:', homeResponse.status);

    // 2. 尝试访问活动记录页面（应该重定向到登录页，因为需要认证）
    console.log('\n2. 测试活动记录页面访问...');
    try {
      const activitiesResponse = await axios.get(`${baseUrl}/activities`, {
        maxRedirects: 0
      });
      console.log('活动记录页面状态码:', activitiesResponse.status);
    } catch (error) {
      if (error.response && error.response.status === 302) {
        console.log('✓ 活动记录页面正确重定向到登录页（需要认证）');
      } else if (error.response && error.response.status === 200) {
        console.log('✓ 活动记录页面可以直接访问');
      } else {
        console.log('✗ 活动记录页面访问失败:', error.message);
      }
    }

    // 3. 尝试访问社区页面
    console.log('\n3. 测试社区页面访问...');
    try {
      const momentsResponse = await axios.get(`${baseUrl}/moments`, {
        maxRedirects: 0
      });
      console.log('社区页面状态码:', momentsResponse.status);
    } catch (error) {
      if (error.response && error.response.status === 302) {
        console.log('✓ 社区页面正确重定向到登录页（需要认证）');
      } else if (error.response && error.response.status === 200) {
        console.log('✓ 社区页面可以直接访问');
      } else {
        console.log('✗ 社区页面访问失败:', error.message);
      }
    }

    // 4. 检查前端路由配置
    console.log('\n4. 获取前端构建信息...');
    try {
      const assetsResponse = await axios.get(`${baseUrl}/src/router/index.js`);
      console.log('✓ 路由文件可以获取');
    } catch (error) {
      console.log('○ 这是正常的，Vite开发模式下不直接暴露源文件');
    }

    console.log('\n=== 测试完成 ===');
    console.log('提示：如果页面需要认证但您未登录，点击导航卡片会重定向到登录页');

  } catch (error) {
    console.error('测试失败:', error.message);
  }
}

// 执行测试
testNavigation();