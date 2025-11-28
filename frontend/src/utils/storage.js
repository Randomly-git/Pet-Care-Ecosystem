/**
 * 本地存储工具类
 * 提供统一的localStorage和sessionStorage操作方法
 */

class Storage {
  constructor(type = 'localStorage') {
    this.storage = window[type]
  }

  /**
   * 设置存储项
   * @param {string} key 存储键
   * @param {any} value 存储值
   * @param {number} expires 过期时间（毫秒），可选
   */
  set(key, value, expires = null) {
    try {
      const data = {
        value,
        expires: expires ? Date.now() + expires : null
      }
      this.storage.setItem(key, JSON.stringify(data))
      return true
    } catch (error) {
      console.error('存储设置失败:', error)
      return false
    }
  }

  /**
   * 获取存储项
   * @param {string} key 存储键
   * @param {any} defaultValue 默认值
   * @returns {any} 存储值或默认值
   */
  get(key, defaultValue = null) {
    try {
      const item = this.storage.getItem(key)
      if (!item) return defaultValue

      const data = JSON.parse(item)

      // 检查是否过期
      if (data.expires && Date.now() > data.expires) {
        this.remove(key)
        return defaultValue
      }

      return data.value
    } catch (error) {
      console.error('存储获取失败:', error)
      return defaultValue
    }
  }

  /**
   * 移除存储项
   * @param {string} key 存储键
   */
  remove(key) {
    try {
      this.storage.removeItem(key)
      return true
    } catch (error) {
      console.error('存储删除失败:', error)
      return false
    }
  }

  /**
   * 清空所有存储项
   */
  clear() {
    try {
      this.storage.clear()
      return true
    } catch (error) {
      console.error('存储清空失败:', error)
      return false
    }
  }

  /**
   * 检查存储项是否存在
   * @param {string} key 存储键
   * @returns {boolean} 是否存在
   */
  has(key) {
    try {
      const item = this.storage.getItem(key)
      if (!item) return false

      const data = JSON.parse(item)

      // 检查是否过期
      if (data.expires && Date.now() > data.expires) {
        this.remove(key)
        return false
      }

      return true
    } catch (error) {
      console.error('存储检查失败:', error)
      return false
    }
  }

  /**
   * 获取所有存储键
   * @returns {string[]} 存储键数组
   */
  keys() {
    try {
      const keys = []
      for (let i = 0; i < this.storage.length; i++) {
        const key = this.storage.key(i)
        if (key) keys.push(key)
      }
      return keys
    } catch (error) {
      console.error('获取存储键失败:', error)
      return []
    }
  }
}

// 创建实例
export const localStorage = new Storage('localStorage')
export const sessionStorage = new Storage('sessionStorage')

// 便捷方法
export const setToken = (token) => {
  return localStorage.set('authToken', token, 24 * 60 * 60 * 1000) // 24小时过期
}

export const getToken = () => {
  return localStorage.get('authToken')
}

export const removeToken = () => {
  return localStorage.remove('authToken')
}

export const setUserInfo = (userInfo) => {
  return localStorage.set('userInfo', userInfo, 24 * 60 * 60 * 1000)
}

export const getUserInfo = () => {
  return localStorage.get('userInfo')
}

export const removeUserInfo = () => {
  return localStorage.remove('userInfo')
}

export const setCurrentPet = (pet) => {
  return sessionStorage.set('currentPet', pet)
}

export const getCurrentPet = () => {
  return sessionStorage.get('currentPet')
}

export const removeCurrentPet = () => {
  return sessionStorage.remove('currentPet')
}

export default Storage