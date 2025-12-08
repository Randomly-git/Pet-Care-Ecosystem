// 文件位置: src/services/mock.js
// Mock数据服务 - 开发阶段API模拟

// 模拟网络延迟
const delay = (ms = 500) => new Promise(resolve => setTimeout(resolve, ms))

// 模拟用户数据
export const mockUser = {
  id: 'user_001',
  name: '宠物主人',
  email: 'pet@example.com',
  avatar: '👤',
  pets: [
    {
      id: 'pet_001',
      name: '喵喵',
      species: '猫',
      breed: '英短',
      avatar: '🐱',
      isDefault: true,
      age: 2,
      weight: 4.5,
      gender: 'male'
    },
    {
      id: 'pet_002',
      name: '旺财',
      species: '狗',
      breed: '柴犬',
      avatar: '🐕',
      isDefault: false,
      age: 3,
      weight: 12,
      gender: 'male'
    }
  ]
}

// 模拟活动数据
export const mockActivities = [
  {
    id: 1,
    category: 'diet',
    name: '早餐',
    time: '08:00',
    completed: true,
    frequency: 'daily',
    autoMark: true,
    reminder: true,
    reminderMinutes: 15,
    importance: 'normal',
    description: '每天早上的第一顿饭',
    images: [],
    createdAt: '2025-11-15T08:00:00Z'
  },
  {
    id: 2,
    category: 'diet',
    name: '午餐',
    time: '12:00',
    completed: true,
    frequency: 'daily',
    autoMark: true,
    reminder: true,
    reminderMinutes: 15,
    importance: 'normal',
    description: '',
    images: [],
    createdAt: '2025-11-15T12:00:00Z'
  },
  {
    id: 3,
    category: 'diet',
    name: '晚餐',
    time: '18:00',
    completed: false,
    frequency: 'daily',
    autoMark: true,
    reminder: true,
    reminderMinutes: 15,
    importance: 'normal',
    description: '',
    images: [],
    createdAt: '2025-11-15T18:00:00Z'
  },
  {
    id: 4,
    category: 'health',
    name: '体检',
    time: '14:00',
    completed: true,
    frequency: 'custom',
    customDays: 365,
    autoMark: false,
    reminder: true,
    reminderMinutes: 1440,
    importance: 'important',
    description: '年度体检',
    images: ['medical1.jpg'],
    createdAt: '2025-11-15T14:00:00Z'
  },
  {
    id: 5,
    category: 'hygiene',
    name: '洗澡',
    time: '20:00',
    completed: true,
    frequency: 'custom',
    customDays: 7,
    autoMark: false,
    reminder: true,
    reminderMinutes: 1440,
    importance: 'normal',
    description: '每周洗澡一次',
    images: [],
    createdAt: '2025-11-15T20:00:00Z'
  },
  {
    id: 6,
    category: 'hygiene',
    name: '刷牙',
    time: '21:00',
    completed: false,
    frequency: 'daily',
    autoMark: false,
    reminder: true,
    reminderMinutes: 15,
    importance: 'normal',
    description: '保持口腔健康',
    images: [],
    createdAt: '2025-11-15T21:00:00Z'
  },
  {
    id: 7,
    category: 'activity',
    name: '玩耍',
    time: '15:30',
    completed: true,
    frequency: 'daily',
    autoMark: false,
    reminder: false,
    importance: 'normal',
    description: '每天陪宠物玩耍',
    images: ['play1.jpg'],
    createdAt: '2025-11-15T15:30:00Z'
  }
]

// Mock API类
export class MockAPI {
  // 模拟用户相关API
  static async getUserInfo(userId) {
    await delay(300)
    return mockUser
  }

  static async updateProfile(data) {
    await delay(500)
    return { ...mockUser, ...data }
  }

  // 模拟宠物相关API
  static async getPetInfo(petId) {
    await delay(300)
    return mockUser.pets.find(pet => pet.id === petId)
  }

  static async getPetList(userId) {
    await delay(300)
    return mockUser.pets
  }

  static async updatePetInfo(petId, data) {
    await delay(500)
    const pet = mockUser.pets.find(pet => pet.id === petId)
    return pet ? { ...pet, ...data } : null
  }

  // 模拟活动相关API
  static async getActivities(petId, date) {
    await delay(400)
    return mockActivities
  }

  static async toggleActivity(activityId) {
    await delay(200)
    const activity = mockActivities.find(a => a.id === activityId)
    if (activity) {
      activity.completed = !activity.completed
    }
    return activity
  }

  static async createActivity(data) {
    await delay(600)
    const newActivity = {
      id: Date.now(),
      ...data,
      completed: false,
      createdAt: new Date().toISOString()
    }
    mockActivities.push(newActivity)
    return newActivity
  }

  static async updateActivity(activityId, data) {
    await delay(500)
    const index = mockActivities.findIndex(a => a.id === activityId)
    if (index !== -1) {
      mockActivities[index] = { ...mockActivities[index], ...data }
      return mockActivities[index]
    }
    return null
  }

  static async deleteActivity(activityId) {
    await delay(400)
    const index = mockActivities.findIndex(a => a.id === activityId)
    if (index !== -1) {
      return mockActivities.splice(index, 1)[0]
    }
    return null
  }

  // 模拟医疗相关API
  static async getMedicalRecords(petId) {
    await delay(300)
    return [
      {
        id: 1,
        type: 'vaccination',
        date: '2025-11-01',
        description: '狂犬疫苗接种',
        doctor: '李医生',
        nextDueDate: '2026-11-01'
      },
      {
        id: 2,
        type: 'checkup',
        date: '2025-10-15',
        description: '年度体检',
        doctor: '王医生',
        results: '一切正常'
      }
    ]
  }

  // 模拟商店相关API
  static async getProducts(category) {
    await delay(400)
    return [
      {
        id: 1,
        name: '优质猫粮',
        category: 'food',
        price: 89.90,
        image: 'cat-food.jpg',
        description: '营养均衡的成猫粮'
      },
      {
        id: 2,
        name: '宠物玩具',
        category: 'toy',
        price: 29.90,
        image: 'pet-toy.jpg',
        description: '耐咬的互动玩具'
      }
    ]
  }
}

// 判断是否启用Mock模式
export const isMockEnabled = () => {
  // 优先使用环境变量配置
  const envMock = import.meta.env.VITE_ENABLE_MOCK
  if (envMock !== undefined) {
    return envMock === 'true'
  }

  // 如果没有设置环境变量，根据域名判断（开发环境默认关闭）
  return false // 默认关闭Mock模式
}

export default MockAPI