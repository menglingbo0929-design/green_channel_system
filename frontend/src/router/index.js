import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'

const routes = [
  {
    path: '/',
    redirect: '/login'       // 根路径自动跳到登录页
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '登录 - 高校绿色通道系统' }
  }
  // 后续在这里添加其他页面路由，例如：
  // { path: '/home', name: 'Home', component: () => import('../views/Home.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：设置页面标题
router.beforeEach((to) => {
  document.title = to.meta.title || '高校绿色通道系统'
})

export default router
