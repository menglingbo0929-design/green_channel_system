import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { decodeJwt, isTokenExpired } from '../utils/jwt.js'
import { loginAPI } from '../api/index.js'

/**
 * 用户状态 Store —— 整个前端所有页面共享的用户信息
 *
 * 职责：
 *   1. 存 token、userId、loginName、roles
 *   2. 提供 login()、logout() 操作
 *   3. 提供 isLoggedIn、hasRole 等判断
 *   4. 页面刷新时从 localStorage 恢复状态
 *
 * 使用方式（任何 Vue 组件）：
 *   import { useUserStore } from '@/stores/user'
 *   const userStore = useUserStore()
 *   userStore.login(name, pwd)
 *   userStore.hasRole('SCHOOL')
 *   userStore.logout()
 */

// 四种角色的默认首页
const ROLE_DEFAULT_PAGE = {
  STUDENT:   '/my-apply',
  COUNSELOR: '/counselor-review',
  COLLEGE:   '/college-review',
  SCHOOL:    '/school-review'
}

// 所有菜单项定义，roles 控制可见角色
const ALL_MENUS = [
  { key: 'home',        label: '首页',         icon: 'home',           roles: ['STUDENT','COUNSELOR','COLLEGE','SCHOOL'], path: '/home' },
  { key: 'profile',     label: '个人中心',     icon: 'user',           roles: ['STUDENT','COUNSELOR','COLLEGE','SCHOOL'], path: '/profile' },
  { key: 'apply-center',label: '学生申请中心', icon: 'edit-pen',        roles: ['STUDENT'],                                   path: '/student-center' },
  { key: 'my-apply',    label: '我的申请',     icon: 'document',        roles: ['STUDENT'],                                   path: '/my-apply' },
  { key: 'counselor',   label: '辅导员审核',   icon: 'check-circle',    roles: ['COUNSELOR'],                                 path: '/counselor-review' },
  { key: 'college',     label: '学院审核',     icon: 'flag',            roles: ['COLLEGE'],                                   path: '/college-review' },
  { key: 'school-review',label: '学校审核',    icon: 'check-circle',    roles: ['SCHOOL'],                                    path: '/school-review' },
  { key: 'messages',    label: '消息中心',     icon: 'bell',            roles: ['STUDENT'],                                   path: '/student/messages' },
  { key: 'messages-counselor', label: '消息中心', icon: 'bell',         roles: ['COUNSELOR'],                                 path: '/counselor/messages' },
  { key: 'messages-college', label: '消息中心', icon: 'bell',           roles: ['COLLEGE'],                                   path: '/college/messages' },
  { key: 'messages-school', label: '消息中心', icon: 'bell',            roles: ['SCHOOL'],                                    path: '/school/messages' },
  // 学校端欠费确认、代申请、线下补录和单据均在“学校业务处理页”内通过页签切换。
  { key: 'school-business', label: '业务处理', icon: 'coin',            roles: ['SCHOOL'],                                    path: '/member4/school-business' },
  // 统计看板只由学校管理员使用；学院审核端不再显示会跳转到 403 的入口。
  { key: 'stats',       label: '统计报表',     icon: 'chart-line',      roles: ['SCHOOL'],                                    path: '/stats' },
  { key: 'base-data',   label: '基础数据',     icon: 'database',        roles: ['SCHOOL'],                                    path: '/base-data' },
  { key: 'application-config', label: '批次与申请配置', icon: 'setting', roles: ['SCHOOL', 'COLLEGE'],                       path: '/application-config' },
  { key: 'policy',      label: '政策与说明',   icon: 'info',            roles: ['STUDENT','COUNSELOR','COLLEGE','SCHOOL'],    path: '/policy' }
]

export const useUserStore = defineStore('user', () => {
  // ========== 状态 ==========
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(localStorage.getItem('userId') || '')
  const loginName = ref(localStorage.getItem('loginName') || '')
  const roles = ref(JSON.parse(localStorage.getItem('roles') || '[]'))

  // ========== 计算属性（getters） ==========

  /** 是否已登录 */
  const isLoggedIn = computed(() => {
    return !!token.value && !isTokenExpired(token.value)
  })

  /** 当前角色中文名映射 */
  const roleLabel = computed(() => {
    const map = { STUDENT: '学生', COUNSELOR: '辅导员', COLLEGE: '学院管理员', SCHOOL: '学校管理员' }
    return roles.value.map(r => map[r] || r).join('、')
  })

  /** 当前用户可见的菜单（按角色过滤） */
  const menus = computed(() => {
    return ALL_MENUS.filter(m => m.roles.some(r => roles.value.includes(r)))
  })

  /** 默认跳转页面 */
  const defaultPage = computed(() => {
    if (roles.value.length === 0) return '/home'
    return ROLE_DEFAULT_PAGE[roles.value[0]] || '/home'
  })

  // ========== 操作方法（actions） ==========

  /**
   * 登录：调后端 → 存 Token → 解码角色 → 持久化
   *
   * @param {string} name 用户名
   * @param {string} pwd  密码
   * @returns {object} { success, message }
   */
  async function login(name, pwd) {
    const res = await loginAPI(name, pwd)
    const data = res.data.data  // { token, userId, loginName }

    // 1. 解码 JWT 拿到 roles（后端在 Token 里存了 roles）
    const payload = decodeJwt(data.token)
    const userRoles = payload.roles ? payload.roles.split(',') : []

    // 2. 存入 Pinia 状态
    token.value = data.token
    userId.value = data.userId
    loginName.value = data.loginName
    roles.value = userRoles

    // 3. 持久化到 localStorage（页面刷新时恢复）
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('loginName', data.loginName)
    localStorage.setItem('roles', JSON.stringify(userRoles))

    return { success: true }
  }

  /**
   * 退出登录：清空状态 + 清空 localStorage + 跳回登录页
   */
  function logout() {
    token.value = ''
    userId.value = ''
    loginName.value = ''
    roles.value = []
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('loginName')
    localStorage.removeItem('roles')
    // 保留 login_history 等其他 key
  }

  /**
   * 从 localStorage 恢复状态（页面刷新时在 App 初始化时调用）
   */
  function initFromStorage() {
    const stored = localStorage.getItem('token')
    if (stored && !isTokenExpired(stored)) {
      token.value = stored
      userId.value = localStorage.getItem('userId') || ''
      loginName.value = localStorage.getItem('loginName') || ''
      roles.value = JSON.parse(localStorage.getItem('roles') || '[]')
    } else if (stored && isTokenExpired(stored)) {
      logout()
    }
  }

  /**
   * 判断当前用户是否拥有某个角色
   */
  function hasRole(code) {
    return roles.value.includes(code)
  }

  /**
   * 判断当前用户是否拥有至少一个指定角色
   */
  function hasAnyRole(codes) {
    return codes.some(c => roles.value.includes(c))
  }

  return {
    token, userId, loginName, roles,
    isLoggedIn, roleLabel, menus, defaultPage,
    login, logout, initFromStorage, hasRole, hasAnyRole
  }
})
