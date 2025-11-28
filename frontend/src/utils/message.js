/**
 * 消息提示工具类
 * 提供统一的消息显示功能
 */

// 创建消息容器
const createMessageContainer = () => {
  const container = document.createElement('div')
  container.className = 'message-container'
  container.style.cssText = `
    position: fixed;
    top: 20px;
    right: 20px;
    z-index: 9999;
    display: flex;
    flex-direction: column;
    gap: 10px;
    pointer-events: none;
  `
  document.body.appendChild(container)
  return container
}

let messageContainer = null

const getContainer = () => {
  if (!messageContainer) {
    messageContainer = createMessageContainer()
  }
  return messageContainer
}

// 创建消息元素
const createMessage = (content, type = 'info') => {
  const message = document.createElement('div')
  message.className = `message message-${type}`

  // 根据类型设置样式
  const typeStyles = {
    success: {
      background: '#52C41A',
      color: '#fff'
    },
    error: {
      background: '#FF4D4F',
      color: '#fff'
    },
    warning: {
      background: '#FAAD14',
      color: '#fff'
    },
    info: {
      background: '#1890FF',
      color: '#fff'
    }
  }

  const style = typeStyles[type] || typeStyles.info

  message.style.cssText = `
    background: ${style.background};
    color: ${style.color};
    padding: 12px 20px;
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    font-size: 14px;
    font-weight: 500;
    max-width: 300px;
    word-wrap: break-word;
    pointer-events: auto;
    cursor: pointer;
    transform: translateX(100%);
    transition: all 0.3s cubic-bezier(0.68, -0.55, 0.265, 1.55);
    position: relative;
    overflow: hidden;
  `

  // 添加关闭按钮
  const closeBtn = document.createElement('span')
  closeBtn.innerHTML = '×'
  closeBtn.style.cssText = `
    position: absolute;
    top: 50%;
    right: 8px;
    transform: translateY(-50%);
    font-size: 18px;
    font-weight: bold;
    cursor: pointer;
    opacity: 0.8;
    transition: opacity 0.2s;
  `
  closeBtn.addEventListener('mouseenter', () => {
    closeBtn.style.opacity = '1'
  })
  closeBtn.addEventListener('mouseleave', () => {
    closeBtn.style.opacity = '0.8'
  })

  // 添加内容
  const contentSpan = document.createElement('span')
  contentSpan.textContent = content
  contentSpan.style.cssText = `
    display: block;
    padding-right: 25px;
  `

  message.appendChild(contentSpan)
  message.appendChild(closeBtn)

  // 添加关闭事件
  const closeMessage = () => {
    message.style.transform = 'translateX(100%)'
    message.style.opacity = '0'
    setTimeout(() => {
      if (message.parentNode) {
        message.parentNode.removeChild(message)
      }
    }, 300)
  }

  closeBtn.addEventListener('click', closeMessage)
  message.addEventListener('click', closeMessage)

  return message
}

// 显示消息
const showMessage = (content, type = 'info', duration = 3000) => {
  const container = getContainer()
  const message = createMessage(content, type)

  container.appendChild(message)

  // 触发动画
  setTimeout(() => {
    message.style.transform = 'translateX(0)'
    message.style.opacity = '1'
  }, 10)

  // 自动关闭
  if (duration > 0) {
    setTimeout(() => {
      if (message.parentNode) {
        message.style.transform = 'translateX(100%)'
        message.style.opacity = '0'
        setTimeout(() => {
          if (message.parentNode) {
            message.parentNode.removeChild(message)
          }
        }, 300)
      }
    }, duration)
  }
}

// 便捷方法
export const showToast = (content, type = 'info', duration = 3000) => {
  showMessage(content, type, duration)
}

export const showSuccess = (content, duration = 3000) => {
  showMessage(content, 'success', duration)
}

export const showError = (content, duration = 5000) => {
  showMessage(content, 'error', duration)
}

export const showWarning = (content, duration = 4000) => {
  showMessage(content, 'warning', duration)
}

export const showInfo = (content, duration = 3000) => {
  showMessage(content, 'info', duration)
}

// 加载提示
let loadingInstance = null

export const showLoading = (content = '加载中...') => {
  hideLoading() // 先隐藏之前的loading

  const loading = document.createElement('div')
  loading.className = 'loading-overlay'
  loading.style.cssText = `
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 10000;
    font-size: 16px;
    color: #fff;
    backdrop-filter: blur(2px);
  `

  const loadingContent = document.createElement('div')
  loadingContent.style.cssText = `
    background: rgba(0, 0, 0, 0.8);
    padding: 20px 30px;
    border-radius: 8px;
    text-align: center;
  `

  const spinner = document.createElement('div')
  spinner.style.cssText = `
    width: 32px;
    height: 32px;
    border: 3px solid rgba(255, 255, 255, 0.3);
    border-top: 3px solid #fff;
    border-radius: 50%;
    animation: spin 1s linear infinite;
    margin: 0 auto 12px auto;
  `

  const style = document.createElement('style')
  style.textContent = `
    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }
  `
  document.head.appendChild(style)

  const text = document.createElement('div')
  text.textContent = content

  loadingContent.appendChild(spinner)
  loadingContent.appendChild(text)
  loading.appendChild(loadingContent)

  document.body.appendChild(loading)
  loadingInstance = loading

  return loading
}

export const hideLoading = () => {
  if (loadingInstance) {
    loadingInstance.remove()
    loadingInstance = null
  }
}

export default {
  showToast,
  showSuccess,
  showError,
  showWarning,
  showInfo,
  showLoading,
  hideLoading
}