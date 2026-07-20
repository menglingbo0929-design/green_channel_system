import { createRouter, createWebHistory } from 'vue-router'
import ApprovalWorkbench from '../views/approval/ApprovalWorkbench.vue'
import MyApplications from '../views/approval/MyApplications.vue'

const routes = [
  { path: '/', redirect: '/student/applications' },
  { path: '/student/applications', component: MyApplications, meta: { role: 'STUDENT', menuKey: 'MY_APPLICATIONS' } },
  { path: '/counselor/approvals', component: ApprovalWorkbench, meta: { role: 'COUNSELOR', menuKey: 'APPROVAL' } },
  { path: '/college/approvals', component: ApprovalWorkbench, meta: { role: 'COLLEGE', menuKey: 'APPROVAL' } },
]

export default createRouter({ history: createWebHistory(), routes, scrollBehavior: () => ({ top: 0 }) })
