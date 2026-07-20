/**
 * JWT 前端解码工具
 *
 * JWT 三段式：Header.Payload.Signature
 * Payload 是 Base64 URL-safe 编码，不是加密 —— 前端可以直接解码读取内容
 * Signature 需要密钥才能验证，验证只能在后端做
 *
 * 所以这里只做"解码查看"，不做"验证真伪"
 */

/**
 * 解码 JWT Token 的 Payload 部分
 *
 * @param {string} token - JWT 字符串，如 "eyJhbG.eyJzdWIi.abc123"
 * @returns {object} Payload 对象，如 { sub: "admin", userId: 1, roles: "SCHOOL", exp: 1784565105 }
 */
export function decodeJwt(token) {
  try {
    // 取中间那段（payload），替换 URL-safe 字符，补全 padding
    const payload = token.split('.')[1]
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const json = atob(base64)  // 浏览器内置 Base64 解码
    return JSON.parse(json)
  } catch {
    return {}
  }
}

/**
 * 检查 Token 是否已过期（纯前端判断，仅做 UI 层提前拦截）
 *
 * @param {string} token
 * @returns {boolean}
 */
export function isTokenExpired(token) {
  const payload = decodeJwt(token)
  if (!payload.exp) return false
  // exp 是 Unix 时间戳（秒），Date.now() 是毫秒
  return Date.now() >= payload.exp * 1000
}
