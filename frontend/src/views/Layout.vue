<template>
  <!--
    统一后台布局 —— 所有9个页面的外层骨架
    规范依据：docs/高校绿色通道系统前端页面统一风格Prompt_原版定值修订.md

    结构：
    ┌──────────┬───────────────────────────┐
    │ 侧边栏    │ 顶栏 60px #FFFFFF          │
    │ 224px    ├───────────────────────────┤
    │ #123B63  │ 主内容区 #F4F6F9           │
    │          │  <router-view />          │
    └──────────┴───────────────────────────┘
  -->
  <div class="layout">
    <!-- ============ 左侧导航栏 ============ -->
    <aside class="sidebar">
      <!-- 系统 Logo + 名称 -->
      <div class="sidebar-brand">
        <div class="brand-icon">
          <svg viewBox="0 0 28 28" width="28" height="28" fill="none">
            <path d="M14 2L3 6v8.5C3 21 7.5 27.5 14 29c6.5-1.5 11-8 11-14.5V6L14 2z"
                  stroke="#fff" stroke-width="1.75" fill="none"/>
            <path d="M11 15l2 2 4-5" stroke="#fff" stroke-width="2"
                  stroke-linecap="round" stroke-linejoin="round" fill="none"/>
          </svg>
        </div>
        <span class="brand-text">高校绿色通道系统</span>
      </div>

      <!-- 导航菜单 -->
      <nav class="sidebar-nav">
        <template v-for="item in userStore.menus" :key="item.key">
          <div
            class="nav-item"
            :class="{ active: currentPath === item.path }"
            @click="navigateTo(item.path)"
          >
            <el-icon class="nav-icon"><component :is="iconMap[item.icon]" /></el-icon>
            <span>{{ item.label }}</span>
          </div>
        </template>
      </nav>

      <!-- 底部版本号 -->
      <div class="sidebar-footer">v1.0.0</div>
    </aside>

    <!-- ============ 右侧主体 ============ -->
    <div class="main-area">
      <!-- 顶部导航栏 -->
      <header class="topbar">
        <div class="topbar-left">
          <el-icon class="topbar-icon" @click="collapse = !collapse">
            <Fold />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="pageTitle">{{ pageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="topbar-right">
          <!-- 消息通知 -->
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="topbar-badge" @click="openMessages">
            <el-icon class="topbar-icon"><Bell /></el-icon>
          </el-badge>

          <!-- 用户头像 + 信息 -->
          <div class="user-info">
            <el-avatar :size="32" class="user-avatar">
              {{ userStore.loginName.charAt(0).toUpperCase() }}
            </el-avatar>
            <span class="user-name">{{ userStore.loginName }}</span>
            <span class="user-role">{{ userStore.roleLabel }}</span>
          </div>

          <!-- 退出 -->
          <el-dropdown trigger="click" @command="handleCommand">
            <el-icon class="topbar-icon dropdown-arrow"><ArrowDown /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 主内容区 -->
      <main class="content">
        <router-view />
      </main>
    </div>

    <!-- 修改密码弹窗 -->
    <FormDialog v-model:visible="pwdDialog" title="修改密码" :formData="pwdForm" :rules="pwdRules" :loading="pwdSaving" submitText="确认修改" @submit="handlePwdSave">
      <el-form-item label="旧密码" prop="oldPassword"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
      <el-form-item label="新密码" prop="newPassword"><el-input v-model="pwdForm.newPassword" type="password" show-password /></el-form-item>
      <el-form-item label="确认新密码" prop="confirmPassword"><el-input v-model="pwdForm.confirmPassword" type="password" show-password /></el-form-item>
    </FormDialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user.js'
import { ElMessage } from 'element-plus'
import { changePasswordAPI } from '../api/index.js'
import { getMessages } from '../api/approval.js'
import FormDialog from '../components/FormDialog.vue'
import {
  HomeFilled, User, School, EditPen, Document,
  CircleCheck, Flag, Coin, Plus, TrendCharts,
  Coin as Database, InfoFilled, Fold, Bell, ArrowDown, Setting
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const collapse = ref(false)
const unreadCount = ref(0)
const currentRole = computed(() => userStore.roles[0] || '')

// 修改密码弹窗
const pwdDialog = ref(false)
const pwdSaving = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入旧密码' }],
  newPassword: [{ required: true, message: '请输入新密码', min: 6 }],
  confirmPassword: [
    { required: true, message: '请确认新密码' },
    { validator: (_, v, cb) => v === pwdForm.newPassword ? cb() : cb(new Error('两次密码不一致')) }
  ]
}
async function handlePwdSave() {
  pwdSaving.value = true
  try {
    await changePasswordAPI({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    pwdDialog.value = false
    userStore.logout()
    router.push('/login')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '修改失败')
  } finally { pwdSaving.value = false }
}

// 图标名 → 组件映射
const iconMap = {
  home:         HomeFilled,
  user:         User,
  school:       School,
  'edit-pen':   EditPen,
  document:     Document,
  'check-circle': CircleCheck,
  flag:         Flag,
  coin:         Coin,
  plus:         Plus,
  'chart-line': TrendCharts,
  database:     Database,
  info:         InfoFilled,
  bell:         Bell,
  setting:      Setting
}

const currentPath = computed(() => route.path)
const pageTitle = computed(() => route.meta.title || '')

function navigateTo(path) {
  if (path) router.push(path)
}

function openMessages() {
  if (currentRole.value) router.push(`/${currentRole.value.toLowerCase()}/messages`)
}

async function loadUnreadCount() {
  try {
    const page = await getMessages({ page: 1, size: 100, read: false })
    unreadCount.value = page.total || 0
  } catch {
    unreadCount.value = 0
  }
}

onMounted(loadUnreadCount)

function handleCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } else if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'password') {
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    pwdDialog.value = true
  }
}
</script>

<style scoped>
/* =================================================================
   统一后台布局样式 — 严格遵循设计规范
   ================================================================= */

.layout {
  display: flex;
  min-height: 100vh;
  font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif;
}

/* ==================== 左侧导航栏 ==================== */
.sidebar {
  width: 224px;                    /* 规范：224px */
  min-height: 100vh;
  background: #123B63;             /* 规范：#123B63 */
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  color: #D9E6F2;                  /* 规范：未选中文字 #D9E6F2 */
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 16px 20px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}

.brand-icon {
  flex-shrink: 0;
}

.brand-text {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  line-height: 20px;
}

/* 导航菜单 */
.sidebar-nav {
  flex: 1;
  padding: 8px 0;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 40px;
  padding: 0 16px;
  margin: 2px 8px;
  border-radius: 4px;              /* 规范：4px */
  color: #D9E6F2;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.15s;
}

.nav-item:hover {
  background: rgba(255,255,255,0.08);
}

.nav-item.active {
  background: #1677FF;             /* 规范：选中背景 #1677FF */
  color: #FFFFFF;
}

.nav-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.sidebar-footer {
  padding: 12px 16px;
  font-size: 12px;
  color: rgba(255,255,255,0.35);
  border-top: 1px solid rgba(255,255,255,0.08);
}

/* ==================== 右侧主体 ==================== */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

/* ==================== 顶部导航栏 ==================== */
.topbar {
  height: 60px;                    /* 规范：60px */
  background: #FFFFFF;
  border-bottom: 1px solid #E5E7EB; /* 规范：1px #E5E7EB */
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 16px;                       /* 规范：相邻元素间距 16px */
}

.topbar-icon {
  font-size: 20px;
  color: #374151;
  cursor: pointer;
}

.topbar-badge {
  margin-right: 0;
}

/* 用户信息区 */
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  background: #1677FF;
  color: #fff;
  font-weight: 600;
  font-size: 14px;
}

.user-name {
  font-size: 14px;
  color: #1F2937;
  font-weight: 500;
}

.user-role {
  font-size: 12px;
  color: #6B7280;                  /* 规范：次要信息 #6B7280 */
  background: #F3F4F6;
  padding: 2px 8px;
  border-radius: 4px;
}

.dropdown-arrow {
  font-size: 14px;
  color: #6B7280;
}

/* ==================== 主内容区 ==================== */
.content {
  flex: 1;
  background: #F4F6F9;             /* 规范：#F4F6F9 */
  padding: 24px;                   /* 规范：左右边距 24px */
  overflow-y: auto;
}
</style>
