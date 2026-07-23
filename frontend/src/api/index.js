import axios from 'axios'

// 创建 axios 实例，统一配置
const api = axios.create({
  baseURL: '/api',        // 所有请求以 /api 开头，Vite 代理会转发到后端
  timeout: 10000,          // 10 秒超时
  headers: {
    'Content-Type': 'application/json'
  }
})

/**
 * 请求拦截器：自动给每个请求加上 JWT Token
 *
 * 登录成功后前端把 token 存 localStorage，
 * 这里从 localStorage 取出，塞到请求头 Authorization 里，
 * 后端 JwtAuthenticationFilter 会解析这个头来识别用户
 */
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * 响应拦截器：统一处理错误
 *
 * 如果后端返回 401（Token 过期或无效），自动跳回登录页
 */
api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// ========== 用户相关 API ==========

/**
 * 登录
 * @param {string} loginName 用户名
 * @param {string} password  密码
 * @returns {Promise} { token, userId, loginName }
 */
export function loginAPI(loginName, password) {
  return api.post('/user/login', { loginName, password })
}

// ========== 用户管理 API（基础数据） ==========

/** 获取用户列表 */
export function listUsersAPI() {
  return api.get('/user/list')
}

/** 新增用户 */
export function createUserAPI(data) {
  return api.post('/user', data)
}

/** 编辑用户 */
export function updateUserAPI(id, data) {
  return api.put(`/user/${id}`, data)
}

/** 切换启用/停用 */
export function toggleUserStatusAPI(id) {
  return api.put(`/user/${id}/status`)
}

/** 修改密码 */
export function changePasswordAPI(data) {
  return api.put('/user/password', data)
}

// ========== 组织结构 API ==========

export const collegeAPI = {
  list:   ()               => api.get('/college/list'),
  create: (data)           => api.post('/college', data),
  update: (id, data)       => api.put(`/college/${id}`, data),
  toggle: (id)             => api.put(`/college/${id}/status`)
}

export const majorAPI = {
  list:   (collegeId)      => api.get('/major/list', { params: { collegeId } }),
  create: (data)           => api.post('/major', data),
  update: (id, data)       => api.put(`/major/${id}`, data),
  toggle: (id)             => api.put(`/major/${id}/status`)
}

export const gradeAPI = {
  list:   ()               => api.get('/grade/list'),
  create: (data)           => api.post('/grade', data),
  update: (id, data)       => api.put(`/grade/${id}`, data),
  toggle: (id)             => api.put(`/grade/${id}/status`)
}

// ========== 学生管理 API ==========

export const studentAPI = {
  list:   (params)         => api.get('/student/list', { params }),
  create: (data)           => api.post('/student', data),
  update: (id, data)       => api.put(`/student/${id}`, data),
  toggle: (id)             => api.put(`/student/${id}/status`),
  import: (formData)       => api.post('/student/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } }),
  template: ()             => api.get('/student/template', { responseType: 'blob' })
}

export const classAPI = {
  list:   (params)         => api.get('/class/list', { params }),
  create: (data)           => api.post('/class', data),
  update: (id, data)       => api.put(`/class/${id}`, data),
  toggle: (id)             => api.put(`/class/${id}/status`)
}

// 学院管理员-学院关联
export const collegeScopeAPI = {
  list: () => api.get('/college-scope/list'),
  add: (userId, collegeId) => api.post('/college-scope', null, { params: { userId, collegeId } }),
  remove: (id) => api.delete(`/college-scope/${id}`)
}

// 辅导员-学生关联
export const counselorScopeAPI = {
  list: () => api.get('/counselor-scope/list'),
  add: (counselorUserId, studentId) => api.post('/counselor-scope', null, { params: { counselorUserId, studentId } }),
  remove: (id) => api.delete(`/counselor-scope/${id}`)
}

export default api
