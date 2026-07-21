import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user.js'
import Login from '../views/Login.vue'
import Layout from '../views/Layout.vue'
import Home from '../views/Home.vue'
import BaseData from '../views/BaseData.vue'
import ApprovalWorkbench from '../views/approval/ApprovalWorkbench.vue'
import MyApplications from '../views/approval/MyApplications.vue'
import MessageCenter from '../views/approval/MessageCenter.vue'
import ApplicationConfig from '../views/ApplicationConfig.vue'
import StudentApplicationCenter from '../views/StudentApplicationCenter.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '登录', public: true }
  },
  {
    path: '/member4/school-business',
    name: 'MemberFourSchoolBusiness',
    component: () => import('../views/school/SchoolBusinessProcessing.vue'),
    meta: { title: '学校业务处理页', roles: ['SCHOOL'] }
  },
  {
    path: '/member4/statistics-dashboard',
    name: 'MemberFourStatisticsDashboard',
    component: () => import('../views/school/statistics/StatisticsDashboard.vue'),
    meta: { title: '统计看板页', roles: ['SCHOOL'] }
  },
  {
    path: '/',
    component: Layout,
    children: [
      { path: '', redirect: '/home' },
      { path: 'home', name: 'Home', component: Home, meta: { title: '首页' } },
      { path: 'profile', name: 'Profile', component: () => import('../views/Home.vue'), meta: { title: '个人中心' } },
      { path: 'student-center', name: 'StudentCenter', component: StudentApplicationCenter, meta: { title: '学生申请中心', roles: ['STUDENT'] } },
      { path: 'application-config', name: 'ApplicationConfig', component: ApplicationConfig, meta: { title: '批次与申请配置', roles: ['SCHOOL', 'COLLEGE'] } },
      {
        path: 'my-apply',
        alias: '/student/applications',
        name: 'MyApply',
        component: MyApplications,
        meta: { title: '我的申请', role: 'STUDENT', roles: ['STUDENT'], menuKey: 'MY_APPLICATIONS' }
      },
      {
        path: 'counselor-review',
        alias: '/counselor/approvals',
        name: 'CounselorReview',
        component: ApprovalWorkbench,
        meta: { title: '辅导员审核', role: 'COUNSELOR', roles: ['COUNSELOR'], menuKey: 'APPROVAL' }
      },
      {
        path: 'college-review',
        alias: '/college/approvals',
        name: 'CollegeReview',
        component: ApprovalWorkbench,
        meta: { title: '学院审核', role: 'COLLEGE', roles: ['COLLEGE'], menuKey: 'APPROVAL' }
      },
      {
        path: 'school-review',
        alias: '/school/approvals',
        name: 'SchoolReview',
        component: ApprovalWorkbench,
        meta: { title: '学校审核', role: 'SCHOOL', roles: ['SCHOOL'], menuKey: 'APPROVAL' }
      },
      { path: 'school-process', name: 'SchoolProcess', component: () => import('../views/Home.vue'), meta: { title: '学校业务处理', roles: ['SCHOOL'] } },
      { path: 'student/messages', name: 'StudentMessages', component: MessageCenter, meta: { title: '消息中心', role: 'STUDENT', roles: ['STUDENT'], menuKey: 'MESSAGES' } },
      { path: 'counselor/messages', name: 'CounselorMessages', component: MessageCenter, meta: { title: '消息中心', role: 'COUNSELOR', roles: ['COUNSELOR'], menuKey: 'MESSAGES' } },
      { path: 'college/messages', name: 'CollegeMessages', component: MessageCenter, meta: { title: '消息中心', role: 'COLLEGE', roles: ['COLLEGE'], menuKey: 'MESSAGES' } },
      { path: 'school/messages', name: 'SchoolMessages', component: MessageCenter, meta: { title: '消息中心', role: 'SCHOOL', roles: ['SCHOOL'], menuKey: 'MESSAGES' } },
      { path: 'arrears', name: 'Arrears', component: () => import('../views/Home.vue'), meta: { title: '欠费确认', roles: ['SCHOOL'] } },
      { path: 'supplement', name: 'Supplement', component: () => import('../views/Home.vue'), meta: { title: '申请补录', roles: ['SCHOOL'] } },
      { path: 'stats', name: 'Stats', component: () => import('../views/Home.vue'), meta: { title: '统计报表', roles: ['SCHOOL', 'COLLEGE'] } },
      { path: 'base-data', name: 'BaseData', component: BaseData, meta: { title: '基础数据', roles: ['SCHOOL'] } },
      { path: 'policy', name: 'Policy', component: () => import('../views/Home.vue'), meta: { title: '政策与说明' } },
      { path: '403', name: 'Forbidden', component: () => import('../views/Error403.vue'), meta: { title: '无权限' } },
      { path: '404', name: 'NotFound', component: () => import('../views/Error404.vue'), meta: { title: '页面不存在' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/404' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  document.title = (to.meta.title ? to.meta.title + ' - ' : '') + '高校绿色通道系统'

  if (to.meta.public && userStore.isLoggedIn) {
    next(userStore.defaultPage)
    return
  }
  if (!to.meta.public && !userStore.isLoggedIn) {
    next('/login')
    return
  }
  if (to.meta.roles && !userStore.hasAnyRole(to.meta.roles)) {
    next('/403')
    return
  }
  next()
})

export default router
