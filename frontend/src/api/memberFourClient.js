import axios from 'axios'

/**
 * 页面 8、9 共用请求客户端。
 *
 * 页面 8、9 只访问成员四接口，因此使用 /member4-api 前缀。
 * Vite 会把该前缀改写为 /api 并转发到成员四后端 8083，
 * 不影响其他成员继续通过 /api 访问统一后端。
 * 每次请求直接读取成员一登录后保存的 JWT，因此刷新页面后也能继续访问
 * 受 Spring Security 保护的成员四接口。
 */
const memberFourClient = axios.create({
  baseURL: '/member4-api',
  timeout: 10_000,
})

memberFourClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export default memberFourClient
