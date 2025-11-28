// 文件位置: src/stores/user.js
// 用户状态管理 - Pinia store

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userAPI } from '@/services/api'

export const useUserStore = defineStore('user', () => {
  // ===== 状态 =====
  const userInfo = ref({
    id: null,
    name: '',
    email: '',
    avatar: '',
    pets: []
  })

  const isLoggedIn = ref(false)
  const loading = ref(false)
  const error = ref(null)

  // ===== 计算属性 =====
  const hasPets = computed(() => userInfo.value.pets && userInfo.value.pets.length > 0)
  const currentPet = computed(() => {
    return userInfo.value.pets.find(pet => pet.isDefault) || userInfo.value.pets[0]
  })

  // ===== 操作方法 =====
  const login = async (credentials) => {
    loading.value = true
    error.value = null

    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(credentials)
      })

      if (!response.ok) throw new Error('登录失败')

      const data = await response.json()
      userInfo.value = data.user
      isLoggedIn.value = true

      // 存储token
      localStorage.setItem('token', data.token)

    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const logout = () => {
    userInfo.value = {
      id: null,
      name: '',
      email: '',
      avatar: '',
      pets: []
    }
    isLoggedIn.value = false
    localStorage.removeItem('token')
  }

  const fetchUserInfo = async () => {
    loading.value = true
    error.value = null

    try {
      const data = await userAPI.getUserInfo(userInfo.value.id)
      userInfo.value = { ...userInfo.value, ...data }
    } catch (err) {
      error.value = err.message
    } finally {
      loading.value = false
    }
  }

  const updateProfile = async (profileData) => {
    loading.value = true
    error.value = null

    try {
      const updatedUser = await userAPI.updateProfile(profileData)
      userInfo.value = { ...userInfo.value, ...updatedUser }
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const setCurrentPet = (petId) => {
    userInfo.value.pets.forEach(pet => {
      pet.isDefault = pet.id === petId
    })
  }

  const addPet = (pet) => {
    userInfo.value.pets.push(pet)
  }

  const removePet = (petId) => {
    userInfo.value.pets = userInfo.value.pets.filter(pet => pet.id !== petId)
  }

  return {
    // 状态
    userInfo,
    isLoggedIn,
    loading,
    error,

    // 计算属性
    hasPets,
    currentPet,

    // 方法
    login,
    logout,
    fetchUserInfo,
    updateProfile,
    setCurrentPet,
    addPet,
    removePet
  }
})