<template>
  <!--
    登录页面 —— 高校绿色通道系统
    整体布局：全屏浅灰背景 + 居中白色登录卡片
    设计风格：蓝白后台管理系统，直角、克制、正式
  -->
  <div class="login-page">
    <div class="login-card">
      <!-- 左侧品牌区：系统 Logo + 名称 -->
      <div class="login-brand">
        <div class="brand-icon">
          <!-- 盾牌图标：象征安全和保障，与绿色通道"保障入学"业务契合 -->
          <svg viewBox="0 0 64 64" width="72" height="72">
            <path d="M32 4L8 14v20c0 15.5 10.2 29.9 24 33 13.8-3.1 24-17.5 24-33V14L32 4z"
                  fill="#1677FF" stroke="#1677FF" stroke-width="1.5"/>
            <path d="M26 34l4 4 8-10" fill="none" stroke="#fff"
                  stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <h1 class="brand-title">高校绿色通道系统</h1>
        <p class="brand-subtitle">Green Channel Management System</p>
        <p class="brand-desc">保障家庭经济困难学生顺利入学</p>
      </div>

      <!-- 右侧登录表单区 -->
      <div class="login-form">
        <h2 class="form-title">用户登录</h2>

        <!-- 登录方式切换 -->
        <div class="login-tabs">
          <span :class="['tab-item', { active: loginMode === 'password' }]" @click="loginMode='password'">密码登录</span>
          <span :class="['tab-item', { active: loginMode === 'code' }]" @click="loginMode='code'">验证码登录</span>
        </div>

        <!-- 密码登录表单 -->
        <el-form v-if="loginMode==='password'" ref="formRef" :model="loginForm" :rules="formRules" label-position="top" class="login-el-form" autocomplete="off">
          <el-form-item label="用户名" prop="loginName">
            <el-input v-model="loginForm.loginName" placeholder="请输入用户名" :prefix-icon="UserIcon" size="large" autocomplete="new-password" />
            <div class="history-tags" v-if="loginHistory.length">
              <span class="history-label">最近登录：</span>
              <el-tag v-for="n in loginHistory" :key="n" size="small" class="history-tag" @click="loginForm.loginName=n">{{ n }}</el-tag>
              <el-button link type="danger" size="small" class="history-clear" @click="clearHistory">清空</el-button>
            </div>
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" :prefix-icon="LockIcon" size="large" show-password autocomplete="new-password" @keyup.enter="handleLogin" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleLogin">{{ loading ? '登录中...' : '登 录' }}</el-button>
          </el-form-item>
        </el-form>

        <!-- 验证码登录表单 -->
        <el-form v-else ref="codeFormRef" :model="codeForm" :rules="codeRules" label-position="top" class="login-el-form">
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="codeForm.phone" placeholder="请输入手机号" :prefix-icon="PhoneIcon" size="large" />
          </el-form-item>
          <el-form-item label="验证码" prop="code">
            <div class="code-row">
              <el-input v-model="codeForm.code" placeholder="请输入验证码" size="large" class="code-input" @keyup.enter="handleCodeLogin" />
              <el-button size="large" class="send-btn" :disabled="countdown>0" @click="sendCode">{{ countdown>0 ? countdown+'s' : '发送验证码' }}</el-button>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" class="login-btn" :loading="codeLoading" @click="handleCodeLogin">{{ codeLoading ? '登录中...' : '登 录' }}</el-button>
          </el-form-item>
        </el-form>

        <!-- 底部提示 -->
        <p class="login-tip">首次使用请联系管理员获取账号</p>
      </div>
    </div>

    <!-- 页脚版权 -->
    <p class="login-footer">Copyright &copy; 2026 高校绿色通道系统</p>
  </div>
</template>

<script setup>
import { ref, reactive, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user.js'
import axios from 'axios'
import { decodeJwt } from '../utils/jwt.js'

import { User, Lock, Phone } from '@element-plus/icons-vue'

const UserIcon = shallowRef(User)
const LockIcon = shallowRef(Lock)
const PhoneIcon = shallowRef(Phone)

const router = useRouter()
const userStore = useUserStore()

// ========== 登录模式 ==========
const loginMode = ref('password')

// ========== 登录历史 ==========
const HISTORY_KEY = 'login_history'
const loginHistory = ref(JSON.parse(localStorage.getItem(HISTORY_KEY) || '[]'))

function saveLoginHistory(loginName) {
  if (!loginName) return
  const list = loginHistory.value.filter(n => n !== loginName)
  list.unshift(loginName)
  loginHistory.value = list.slice(0, 10)
  localStorage.setItem(HISTORY_KEY, JSON.stringify(loginHistory.value))
}

function clearHistory() {
  loginHistory.value = []
  localStorage.removeItem(HISTORY_KEY)
}

// ========== 密码登录 ==========
const loginForm = reactive({ loginName: '', password: '' })
const formRules = {
  loginName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const formRef = ref(null)
const loading = ref(false)

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login(loginForm.loginName, loginForm.password)
    saveLoginHistory(loginForm.loginName)
    ElMessage.success(`欢迎回来，${userStore.loginName}！`)
    router.push(userStore.defaultPage)
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '登录失败')
  } finally { loading.value = false }
}

// ========== 验证码登录 ==========
const codeForm = reactive({ phone: '', code: '' })
const codeRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}
const codeFormRef = ref(null)
const codeLoading = ref(false)
const countdown = ref(0)

const sendCode = async () => {
  const phone = codeForm.phone
  if (!/^1[3-9]\d{9}$/.test(phone)) { ElMessage.warning('请先输入正确的手机号'); return }
  try {
    await axios.post('/api/verification-code/send', null, { params: { phone } })
    ElMessage.success('验证码已发送')
    countdown.value = 60
    const timer = setInterval(() => {
      if (--countdown.value <= 0) { clearInterval(timer); countdown.value = 0 }
    }, 1000)
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '发送失败')
  }
}

const handleCodeLogin = async () => {
  const valid = await codeFormRef.value.validate().catch(() => false)
  if (!valid) return
  codeLoading.value = true
  try {
    const res = await axios.post('/api/user/login-by-code', { phone: codeForm.phone, code: codeForm.code })
    const data = res.data.data
    const payload = decodeJwt(data.token)
    const roles = payload.roles ? payload.roles.split(',') : []

    userStore.token = data.token
    userStore.userId = data.userId
    userStore.loginName = data.loginName
    userStore.roles = roles

    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('loginName', data.loginName)
    localStorage.setItem('roles', JSON.stringify(roles))

    saveLoginHistory(userStore.loginName)
    ElMessage.success(`欢迎回来，${userStore.loginName}！`)
    router.push(userStore.defaultPage)
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '登录失败')
  } finally { codeLoading.value = false }
}
</script>

<style scoped>
/* =================================================================
   登录页面样式 —— 严格遵循蓝白后台管理系统设计规范
   ================================================================= */

/* —— 全屏背景 —— */
.login-page {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  /* 极浅灰背景，符合规范 #F4F6F9 */
  background-color: #F4F6F9;
  font-family: 'Microsoft YaHei', 'PingFang SC', 'Noto Sans CJK SC', sans-serif;
  margin: 0;
  padding: 0;
}

/* —— 登录卡片 —— */
.login-card {
  display: flex;
  width: 880px;
  min-height: 480px;
  background: #fff;
  /* 1px 浅灰边框，无大阴影，符合规范 */
  border: 1px solid #E4E7ED;
  /* 1px 圆角，接近直角 */
  border-radius: 2px;
  overflow: hidden;
}

/* ==================== 左侧品牌区 ==================== */
.login-brand {
  width: 400px;
  background: #123B63;   /* 规范：侧边栏深藏蓝 #123B63 */
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 60px 40px;
  text-align: center;
}

.brand-icon {
  margin-bottom: 24px;
}

.brand-title {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 2px;
  margin: 0 0 8px 0;
  color: #fff;
}

.brand-subtitle {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.55);
  margin: 0 0 16px 0;
  letter-spacing: 1px;
}

.brand-desc {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  margin: 0;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.15);
  width: 100%;
}

/* ==================== 右侧表单区 ==================== */
.login-form {
  flex: 1;
  padding: 60px 48px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 20px 0;
  padding-bottom: 12px;
  border-bottom: 2px solid #1677FF;
  display: inline-block;
}

/* —— 登录方式切换 —— */
.login-tabs {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
}
.tab-item {
  font-size: 14px;
  color: #909399;
  cursor: pointer;
  padding-bottom: 4px;
  border-bottom: 2px solid transparent;
  transition: color .2s, border-color .2s;
}
.tab-item.active {
  color: #1677FF;
  border-bottom-color: #1677FF;
  font-weight: 500;
}
.tab-item:hover { color: #1677FF; }

/* —— 验证码行 —— */
.code-row {
  display: flex;
  gap: 8px;
}
.code-input {
  flex: 1;
}
.send-btn {
  min-width: 120px;
  border-radius: 2px;
}
.login-autocomplete { width: 100%; }
.history-tags { margin-top: 6px; display: flex; align-items: center; flex-wrap: wrap; gap: 4px; }
.history-label { font-size: 12px; color: #909399; }
.history-tag { cursor: pointer; }
.history-tag:hover { opacity: 0.8; }
.history-clear { font-size: 12px; margin-left: 4px; }

/* 覆盖 Element Plus 表单样式，使其更符合系统规范 */
.login-el-form :deep(.el-form-item__label) {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
  /* 必填星号为红色 */
}

.login-el-form :deep(.el-form-item.is-required .el-form-item__label::before) {
  color: #F56C6C;
}

/* 输入框：直角或 2px 圆角，统一高度 */
.login-el-form :deep(.el-input__wrapper) {
  border-radius: 2px;
  box-shadow: none;
  border: 1px solid #DCDFE6;
}

.login-el-form :deep(.el-input__wrapper:hover) {
  border-color: #1677FF;
}

.login-el-form :deep(.el-input__wrapper.is-focus) {
  border-color: #1677FF;
  box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.15);
}

/* —— 登录按钮 —— */
.login-btn {
  width: 100%;
  /* 直角矩形按钮，2px 圆角，符合规范 */
  border-radius: 2px;
  height: 44px;
  font-size: 16px;
  letter-spacing: 4px;
  /* 科技蓝主色 */
  background-color: #1677FF;
  border-color: #1677FF;
}

.login-btn:hover {
  background-color: #1D4ED8;
  border-color: #1D4ED8;
}

.login-btn:active {
  background-color: #1E40AF;
  border-color: #1E40AF;
}

/* —— 底部提示 —— */
.login-tip {
  text-align: center;
  font-size: 12px;
  color: #909399;
  margin: 8px 0 0 0;
}

/* —— 页脚 —— */
.login-footer {
  text-align: center;
  font-size: 12px;
  color: #C0C4CC;
  margin-top: 24px;
}
</style>
