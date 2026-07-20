import { createRouter, createWebHistory } from 'vue-router'
import ApprovalWorkbench from '../views/approval/ApprovalWorkbench.vue'
import MyApplications from '../views/approval/MyApplications.vue'
import MessageCenter from '../views/approval/MessageCenter.vue'

const routes = [
  { path: '/', redirect: '/student/applications' },
  { path: '/student/applications', name: 'student-applications', component: MyApplications, meta: { role: 'STUDENT', menuKey: 'MY_APPLICATIONS' } },
  { path: '/counselor/approvals', name: 'counselor-approvals', component: ApprovalWorkbench, meta: { role: 'COUNSELOR', menuKey: 'APPROVAL' } },
  { path: '/college/approvals', name: 'college-approvals', component: ApprovalWorkbench, meta: { role: 'COLLEGE', menuKey: 'APPROVAL' } },
  { path: '/school/approvals', name: 'school-approvals', component: ApprovalWorkbench, meta: { role: 'SCHOOL', menuKey: 'APPROVAL' } },
  { path: '/student/messages', name: 'student-messages', component: MessageCenter, meta: { role: 'STUDENT', menuKey: 'MESSAGES' } },
  { path: '/counselor/messages', name: 'counselor-messages', component: MessageCenter, meta: { role: 'COUNSELOR', menuKey: 'MESSAGES' } },
  { path: '/college/messages', name: 'college-messages', component: MessageCenter, meta: { role: 'COLLEGE', menuKey: 'MESSAGES' } },
  { path: '/school/messages', name: 'school-messages', component: MessageCenter, meta: { role: 'SCHOOL', menuKey: 'MESSAGES' } },
]

export default createRouter({ history: createWebHistory(), routes, scrollBehavior: () => ({ top: 0 }) })
