import axios from 'axios'

/**
 * 页面 8、9 共用请求客户端。
 *
 * 请求统一使用项目已经确定的 /api 前缀，由现有 Vite 代理转发到主后端。
 * 每次请求直接读取成员一登录后保存的 JWT，因此刷新页面后也能继续访问
 * 受 Spring Security 保护的成员四接口。
 */
const memberFourClient = axios.create({
  baseURL: '/api',
  timeout: 10_000,
})

memberFourClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export default memberFourClient
