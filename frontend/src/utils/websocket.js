/**
 * WebSocket 连接工具
 * 使用 STOMP + SockJS 连接 community-backend 的 /ws 端点
 * 用于实时接收冷数据就绪等推送通知
 */

import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

// WebSocket 端点（community-backend 的 STOMP 端点）
const WS_ENDPOINT = 'http://localhost:9000/ws'

let stompClient = null
let isConnected = false
const callbacks = []

/**
 * 建立 STOMP WebSocket 连接
 * @param {Function} onConnect - 连接成功后的回调，参数为 STOMP Client 实例
 * @param {Function} onError - 连接失败的回调（可选）
 * @returns {Promise} 连接结果
 */
export function connectWebSocket(onConnect, onError) {
  // 如果已有连接，直接返回
  if (isConnected && stompClient) {
    if (onConnect) onConnect(stompClient)
    return Promise.resolve(stompClient)
  }

  return new Promise((resolve, reject) => {
    try {
      stompClient = new Client({
        webSocketFactory: () => new SockJS(WS_ENDPOINT),
        reconnectDelay: 5000,      // 断线重试间隔 5 秒
        heartbeatIncoming: 10000,  // 接收心跳 10 秒
        heartbeatOutgoing: 10000,  // 发送心跳 10 秒
        debug: () => {}            // 生产环境关闭调试日志
      })

      stompClient.onConnect = (frame) => {
        isConnected = true
        console.log('🌡️ WebSocket STOMP 已连接')
        if (onConnect) onConnect(stompClient)
        resolve(stompClient)
      }

      stompClient.onStompError = (frame) => {
        console.error('🌡️ WebSocket STOMP 错误:', frame.headers['message'])
        isConnected = false
        if (onError) onError(frame)
        reject(frame)
      }

      stompClient.onWebSocketClose = () => {
        isConnected = false
        // 静默关闭，不打印日志
      }

      stompClient.activate()
    } catch (error) {
      console.error('🌡️ WebSocket 初始化失败:', error)
      isConnected = false
      if (onError) onError(error)
      reject(error)
    }
  })
}

/**
 * 断开 WebSocket 连接
 */
export function disconnectWebSocket() {
  if (stompClient) {
    try {
      stompClient.deactivate()
    } catch (e) {
      // 忽略
    }
    stompClient = null
    isConnected = false
    console.log('🌡️ WebSocket 已断开')
  }
}

/**
 * 订阅指定主题
 * @param {string} destination - 主题路径，如 /topic/cold-data-ready
 * @param {Function} callback - 收到消息的回调，参数为消息 body（已解析为 JSON）
 * @returns {Object|null} subscription 对象，可用于取消订阅
 */
export function subscribe(destination, callback) {
  if (!stompClient || !isConnected) {
    callbacks.push({ destination, callback })
    return null
  }

  try {
    const subscription = stompClient.subscribe(destination, (message) => {
      try {
        const data = JSON.parse(message.body)
        callback(data)
      } catch {
        callback(message.body)
      }
    })
    return subscription
  } catch (error) {
    console.error(`🌡️ 订阅 ${destination} 失败:`, error)
    return null
  }
}

export default {
  connectWebSocket,
  disconnectWebSocket,
  subscribe
}
