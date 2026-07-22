<script setup>
/**
 * 成员四学校端页面的统一外壳。
 *
 * 页面八、页面九没有再各自绘制侧边栏和顶栏，而是沿用 Layout.vue 的
 * 菜单来源、图标映射、尺寸与交互规则；这样学校审核、欠费确认、补录和统计
 * 在同一角色下保持一套导航体验。
 */
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user.js'
import { ElMessage } from 'element-plus'
import { changePasswordAPI } from '../../api/index.js'
import { getMessages } from '../../api/approval.js'
import FormDialog from '../FormDialog.vue'
import {
  HomeFilled, User, School, EditPen, Document, CircleCheck, Flag, Coin,
  Plus, TrendCharts, Coin as Database, InfoFilled, Fold, Bell, ArrowDown, Setting,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const unreadCount = ref(0)
const pwdDialog = ref(false)
const pwdSaving = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入旧密码' }],
  newPassword: [{ required: true, message: '请输入新密码', min: 6 }],
  confirmPassword: [
    { required: true, message: '请确认新密码' },
    { validator: (_, value, callback) => value === pwdForm.newPassword ? callback() : callback(new Error('两次密码不一致')) },
  ],
}

const iconMap = {
  home: HomeFilled,
  user: User,
  school: School,
  'edit-pen': EditPen,
  document: Document,
  'check-circle': CircleCheck,
  flag: Flag,
  coin: Coin,
  plus: Plus,
  'chart-line': TrendCharts,
  database: Database,
  info: InfoFilled,
  bell: Bell,
  setting: Setting,
}

const currentPath = computed(() => route.path)
const pageTitle = computed(() => route.meta.title || '')

function navigateTo(path) {
  if (path) router.push(path)
}

/** 欠费确认、补录和统计在成员四页面内以页签呈现，侧栏仍按原菜单项高亮。 */
function isMenuActive(item) {
  if (currentPath.value === item.path) return true
  if (item.key === 'school-business') return route.name === 'MemberFourSchoolBusiness'
  return item.key === 'stats' && route.name === 'MemberFourStatisticsDashboard'
}

function openMessages() {
  router.push('/school/messages')
}

/** 读取当前登录学校管理员的未读消息总数，供顶栏铃铛角标显示。 */
async function loadUnreadCount() {
  try {
    const page = await getMessages({ page: 1, size: 100, read: false })
    unreadCount.value = page.total || 0
  } catch {
    unreadCount.value = 0
  }
}

function handleMessageRead() {
  loadUnreadCount()
}

async function handlePasswordSave() {
  pwdSaving.value = true
  try {
    await changePasswordAPI({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    pwdDialog.value = false
    userStore.logout()
    router.push('/login')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '修改失败')
  } finally {
    pwdSaving.value = false
  }
}

onMounted(() => {
  loadUnreadCount()
  window.addEventListener('green-channel:message-read', handleMessageRead)
})

onBeforeUnmount(() => {
  window.removeEventListener('green-channel:message-read', handleMessageRead)
})

function handleCommand(command) {
  if (command === 'password') {
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    pwdDialog.value = true
  } else if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<template>
  <div class="workspace-shell">
    <aside class="sidebar">
      <div class="sidebar-brand">
        <div class="brand-icon">
          <svg viewBox="0 0 28 28" width="28" height="28" fill="none" aria-hidden="true">
            <path d="M14 2L3 6v8.5C3 21 7.5 27.5 14 29c6.5-1.5 11-8 11-14.5V6L14 2z" stroke="#fff" stroke-width="1.75" />
            <path d="M11 15l2 2 4-5" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </div>
        <span class="brand-text">高校绿色通道系统</span>
      </div>

      <nav class="sidebar-nav">
        <button
          v-for="item in userStore.menus"
          :key="item.key"
          type="button"
          class="nav-item"
          :class="{ active: isMenuActive(item) }"
          @click="navigateTo(item.path)"
        >
          <el-icon class="nav-icon"><component :is="iconMap[item.icon]" /></el-icon>
          <span>{{ item.label }}</span>
        </button>
      </nav>

      <div class="sidebar-footer">v1.0.0</div>
    </aside>

    <section class="main-area">
      <header class="topbar">
        <div class="topbar-left">
          <el-icon class="topbar-icon"><Fold /></el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="pageTitle">{{ pageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="topbar-right">
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="topbar-badge" @click="openMessages">
            <el-icon class="topbar-icon"><Bell /></el-icon>
          </el-badge>
          <div class="user-info">
            <el-avatar :size="32" class="user-avatar">{{ (userStore.loginName || '校').charAt(0).toUpperCase() }}</el-avatar>
            <span class="user-name">{{ userStore.loginName || '学校管理员' }}</span>
            <span class="user-role">{{ userStore.roleLabel || '学校管理员' }}</span>
          </div>
          <el-dropdown trigger="click" @command="handleCommand">
            <el-icon class="topbar-icon dropdown-arrow"><ArrowDown /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content"><slot /></main>
    </section>

    <FormDialog
      v-model:visible="pwdDialog"
      title="修改密码"
      :formData="pwdForm"
      :rules="pwdRules"
      :loading="pwdSaving"
      submitText="确认修改"
      @submit="handlePasswordSave"
    >
      <el-form-item label="旧密码" prop="oldPassword"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
      <el-form-item label="新密码" prop="newPassword"><el-input v-model="pwdForm.newPassword" type="password" show-password /></el-form-item>
      <el-form-item label="确认新密码" prop="confirmPassword"><el-input v-model="pwdForm.confirmPassword" type="password" show-password /></el-form-item>
    </FormDialog>
  </div>
</template>

<style scoped>
.workspace-shell { display: flex; min-height: 100vh; font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif; }
.sidebar { width: 224px; min-height: 100vh; display: flex; flex-direction: column; flex-shrink: 0; color: #d9e6f2; background: #123b63; }
.sidebar-brand { display: flex; align-items: center; gap: 10px; padding: 16px 16px 20px; border-bottom: 1px solid rgba(255,255,255,.08); }
.brand-icon { flex-shrink: 0; }.brand-text { color: #fff; font-size: 14px; font-weight: 600; line-height: 20px; }
.sidebar-nav { flex: 1; padding: 8px 0; overflow-y: auto; }
.nav-item { display: flex; align-items: center; gap: 10px; width: calc(100% - 16px); height: 40px; padding: 0 16px; margin: 2px 8px; border: 0; border-radius: 4px; color: #d9e6f2; background: transparent; cursor: pointer; font-size: 14px; text-align: left; transition: background .15s; }
.nav-item:hover { background: rgba(255,255,255,.08); }.nav-item.active { color: #fff; background: #1677ff; }.nav-icon { flex-shrink: 0; font-size: 18px; }
.sidebar-footer { padding: 12px 16px; border-top: 1px solid rgba(255,255,255,.08); color: rgba(255,255,255,.35); font-size: 12px; }
.main-area { display: flex; flex: 1; flex-direction: column; min-width: 0; }.topbar { display: flex; align-items: center; justify-content: space-between; height: 60px; padding: 0 20px; border-bottom: 1px solid #e5e7eb; background: #fff; }
.topbar-left, .topbar-right, .user-info { display: flex; align-items: center; }.topbar-left { gap: 16px; }.topbar-right { gap: 16px; }.topbar-icon { color: #374151; cursor: pointer; font-size: 20px; }
.user-info { gap: 8px; }.user-avatar { color: #fff; background: #1677ff; font-size: 14px; font-weight: 600; }.user-name { color: #1f2937; font-size: 14px; font-weight: 500; }.user-role { padding: 2px 8px; border-radius: 4px; color: #6b7280; background: #f3f4f6; font-size: 12px; }.dropdown-arrow { color: #6b7280; font-size: 14px; }
.content { flex: 1; min-width: 0; overflow-y: auto; padding: 24px; background: #f4f6f9; }
</style>
