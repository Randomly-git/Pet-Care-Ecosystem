/**
 * 认证相关的类型定义
 */

/**
 * 注册请求数据
 */
export const RegisterRequest = {
  name: {
    type: String,
    required: true,
    min: 1,
    max: 50,
    pattern: /^[a-zA-Z0-9_\-\u4e00-\u9fa5]+$/,
    message: '用户名只能包含字母、数字、下划线、短横线和中文'
  },
  password: {
    type: String,
    required: true,
    min: 1,
    max: 100,
    message: '密码长度必须在1-100个字符之间'
  }
}

/**
 * 登录请求数据
 */
export const LoginRequest = {
  name: {
    type: String,
    required: true,
    message: '用户名不能为空'
  },
  password: {
    type: String,
    required: true,
    message: '密码不能为空'
  }
}

/**
 * 用户响应数据
 */
export const UserResponse = {
  userId: {
    type: Number,
    required: true
  },
  name: {
    type: String,
    required: true
  },
  token: {
    type: String,
    required: true
  },
  message: {
    type: String,
    required: false
  }
}

/**
 * 注册响应数据
 */
export const RegisterResponse = {
  userId: {
    type: Number,
    required: true
  },
  name: {
    type: String,
    required: true
  },
  message: {
    type: String,
    required: true
  }
}

/**
 * API响应格式
 */
export const ApiResponse = {
  success: {
    type: Boolean,
    required: true
  },
  message: {
    type: String,
    required: true
  },
  data: {
    type: [Object, Array],
    required: false
  },
  timestamp: {
    type: String,
    required: true
  }
}

/**
 * 用户信息存储格式
 */
export const UserInfo = {
  userId: {
    type: Number,
    required: true
  },
  name: {
    type: String,
    required: true
  },
  username: {
    type: String,
    required: true
  },
  loginTime: {
    type: String,
    required: false
  },
  lastLoginTime: {
    type: String,
    required: false
  }
}

/**
 * 认证状态
 */
export const AuthStatus = {
  AUTHENTICATED: 'authenticated',    // 已认证
  UNAUTHENTICATED: 'unauthenticated', // 未认证
  EXPIRED: 'expired',              // token过期
  ERROR: 'error'                    // 错误
}

/**
 * 错误类型
 */
export const AuthErrorType = {
  INVALID_CREDENTIALS: 'invalid_credentials',
  TOKEN_EXPIRED: 'token_expired',
  USER_NOT_FOUND: 'user_not_found',
  VALIDATION_ERROR: 'validation_error',
  NETWORK_ERROR: 'network_error',
  UNKNOWN_ERROR: 'unknown_error'
}

export default {
  RegisterRequest,
  LoginRequest,
  UserResponse,
  RegisterResponse,
  ApiResponse,
  UserInfo,
  AuthStatus,
  AuthErrorType
}