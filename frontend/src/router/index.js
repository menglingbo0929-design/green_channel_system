import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user.js'
import Login from '../views/Login.vue'
import Layout from '../views/Layout.vue'
import Home from '../views/Home.vue'
import BaseData from '../views/BaseData.vue'

/**
 * 路由配置
 *
 * 嵌套路由结构：
 *   /login        → 独立登录页（无侧边栏顶栏）
 *   /home         → Layout > Home
 *   /base-data    → Layout > 基础数据管理（等页面建好后替换）
 *   /student-center → Layout > 学生申请中心
 *   ...其他页面同样嵌套在 Layout 下
 */
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: Layout,
    children: [
      { path: '', redirect: '/home' },
      { path: 'home', name: 'Home', component: Home, meta: { title: '首页' } },
      // 以下页面占位符：等对应 Vue 文件建好后替换 component
      { path: 'profile',     name: 'Profile',      component: () => import('../views/Home.vue'), meta: { title: '个人中心' } },
      { path: 'student-center',name:'StudentCenter',component:()=> import('../views/Home.vue'), meta: { title: '学生申请中心' } },
      { path: 'my-apply',    name: 'MyApply',      component: () => import('../views/Home.vue'), meta: { title: '我的申请' } },
      { path: 'counselor-review',name:'CounselorReview',component:()=> import('../views/Home.vue'), meta: { title: '辅导员审核' } },
      { path: 'college-review',name:'CollegeReview',component:()=> import('../views/Home.vue'), meta: { title: '学院审核' } },
      { path: 'school-process',name:'SchoolProcess',component:()=> import('../views/Home.vue'), meta: { title: '学校业务处理' } },
      { path: 'arrears',     name: 'Arrears',      component: () => import('../views/Home.vue'), meta: { title: '欠费确认' } },
      { path: 'supplement',  name: 'Supplement',   component: () => import('../views/Home.vue'), meta: { title: '申请补录' } },
      { path: 'stats',       name: 'Stats',        component: () => import('../views/Home.vue'), meta: { title: '统计报表' } },
      { path: 'base-data',   name: 'BaseData',     component: BaseData, meta: { title: '基础数据' } },
      { path: 'policy',      name: 'Policy',       component: () => import('../views/Home.vue'), meta: { title: '政策与说明' } }
    ]
  },
  // 404
  { path: '/:pathMatch(.*)*', redirect: '/home' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * 全局路由守卫 —— 控制页面访问权限
 *
 * 三条规则：
 *   1. 公开页面（登录页）→ 已登录时跳转到默认首页
 *   2. 非公开页面 → 未登录时跳转到登录页
 *   3. 所有页面 → 设置 document.title
 */
router.beforeEach((to, from, next) => {
  // Pinia 必须在 router 实例化之后才能使用，所以在这里动态导入
  const userStore = useUserStore()

  // 页面标题
  document.title = (to.meta.title ? to.meta.title + ' - ' : '') + '高校绿色通道系统'

  // 规则 1：访问登录页但已登录 → 跳转角色默认首页
  if (to.meta.public && userStore.isLoggedIn) {
    next(userStore.defaultPage)
    return
  }

  // 规则 2：访问非公开页但未登录 → 跳转登录页
  if (!to.meta.public && !userStore.isLoggedIn) {
    next('/login')
    return
  }

  next()
})

export default router
