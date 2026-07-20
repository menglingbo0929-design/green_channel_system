<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bell, Collection, DataAnalysis, Document, Expand, Fold, HomeFilled,
  OfficeBuilding, Reading, School, Setting, Tickets, User, UserFilled, Wallet,
} from '@element-plus/icons-vue'
import { ROLE_META } from './constants/approval'
import { getMessages } from './api/approval'

const route = useRoute()
const router = useRouter()
const collapsed = computed(() => false)
const activeRole = computed(() => route.meta.role || 'STUDENT')
const activeMeta = computed(() => ROLE_META[activeRole.value] || ROLE_META.STUDENT)
const unreadCount = ref(0)

const roleNavigation = {
  STUDENT: [
    { key: 'HOME', label: '首页', icon: HomeFilled },
    { key: 'APPLY', label: '绿色通道申请', icon: Document },
    { key: 'MY_APPLICATIONS', label: '我的申请', icon: Tickets, to: '/student/applications' },
    { key: 'MESSAGES', label: '消息中心', icon: Bell, to: '/student/messages' },
    { key: 'PROFILE', label: '个人中心', icon: User },
    { key: 'POLICY', label: '政策与说明', icon: School },
  ],
  COUNSELOR: [
    { key: 'HOME', label: '首页', icon: HomeFilled },
    { key: 'STUDENT_DATA', label: '学生信息管理', icon: Collection },
    { key: 'APPROVAL', label: '审核管理', icon: OfficeBuilding, to: '/counselor/approvals' },
    { key: 'MESSAGES', label: '消息中心', icon: Bell, to: '/counselor/messages' },
    { key: 'PROFILE', label: '个人中心', icon: User },
    { key: 'POLICY', label: '政策与说明', icon: School },
  ],
  COLLEGE: [
    { key: 'HOME', label: '首页', icon: HomeFilled },
    { key: 'STUDENT_DATA', label: '学院学生管理', icon: Collection },
    { key: 'APPROVAL', label: '审核管理', icon: OfficeBuilding, to: '/college/approvals' },
    { key: 'MESSAGES', label: '消息中心', icon: Bell, to: '/college/messages' },
    { key: 'REPORTS', label: '学院统计', icon: DataAnalysis },
    { key: 'PROFILE', label: '个人中心', icon: User },
  ],
  SCHOOL: [
    { key: 'HOME', label: '首页', icon: HomeFilled },
    { key: 'APPROVAL', label: '审核管理', icon: OfficeBuilding, to: '/school/approvals' },
    { key: 'MESSAGES', label: '消息中心', icon: Bell, to: '/school/messages' },
    { key: 'ARREARS', label: '欠费确认', icon: Wallet },
    { key: 'SUPPLEMENT', label: '申请补录', icon: Reading },
    { key: 'REPORTS', label: '统计报表', icon: DataAnalysis },
    { key: 'BASE_DATA', label: '基础数据', icon: Setting },
    { key: 'PROFILE', label: '个人中心', icon: User },
  ],
}

const navigation = computed(() => roleNavigation[activeRole.value] || roleNavigation.STUDENT)

const previewRoles = [
  { label: '学生端 · 我的申请', path: '/student/applications' },
  { label: '辅导员审核页', path: '/counselor/approvals' },
  { label: '学院审核页', path: '/college/approvals' },
  { label: '学校审核页', path: '/school/approvals' },
]

function switchPreview(command) {
  router.push(command)
}

function openMessages() {
  router.push(`/${activeRole.value.toLowerCase()}/messages`)
}

async function loadUnreadCount() {
  try {
    const page = await getMessages({ page: 1, size: 100, read: false })
    unreadCount.value = page.total || 0
  } catch {
    unreadCount.value = 0
  }
}

watch(activeRole, loadUnreadCount)
onMounted(loadUnreadCount)
</script>

<template>
  <div class="app-shell">
    <aside class="app-sidebar">
      <div class="brand-block">
        <div class="brand-mark"><School /></div>
        <strong>高校绿色通道系统</strong>
      </div>
      <nav class="role-navigation" aria-label="系统主导航">
        <component
          :is="item.to ? 'RouterLink' : 'span'"
          v-for="item in navigation"
          :key="item.key"
          :to="item.to"
          class="role-link"
          :class="{ active: route.meta.menuKey === item.key, disabled: !item.to }"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </component>
      </nav>
      <div class="sidebar-guide">
        <strong>{{ activeMeta.roleName }}功能导航</strong>
        <p>高亮入口由成员三负责，其余入口预留给对应模块。</p>
      </div>
    </aside>
    <main class="app-main">
      <header class="app-header">
        <div class="header-left">
          <button class="header-icon-button" type="button" aria-label="折叠菜单">
            <component :is="collapsed ? Expand : Fold" />
          </button>
          <span class="breadcrumb-home">首页</span>
          <span class="breadcrumb-separator">/</span>
          <strong>{{ activeMeta.title }}</strong>
        </div>
        <div class="header-actions">
          <button class="icon-button" type="button" aria-label="消息通知" @click="openMessages"><Bell /><span v-if="unreadCount" class="notification-badge">{{ unreadCount }}</span></button>
          <el-dropdown trigger="click" @command="switchPreview">
            <div class="user-chip">
              <div class="user-avatar"><UserFilled /></div>
              <div><strong>{{ activeMeta.userName }}</strong><span>{{ activeMeta.roleName }}</span></div>
              <span class="user-chevron">⌄</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="item in previewRoles" :key="item.path" :command="item.path">{{ item.label }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <RouterView />
    </main>
  </div>
</template>
