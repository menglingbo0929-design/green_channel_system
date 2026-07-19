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
                  fill="#2563EB" stroke="#1D4ED8" stroke-width="1.5"/>
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

        <!--
          Element Plus 表单
          :model   → 表单数据对象
          :rules   → 校验规则
          ref      → 用于调用 validate() 方法
          label-position="top" → 标签在输入框上方
        -->
        <el-form
          ref="formRef"
          :model="loginForm"
          :rules="formRules"
          label-position="top"
          class="login-el-form"
        >
          <!-- 用户名输入框 -->
          <el-form-item label="用户名" prop="loginName">
            <el-input
              v-model="loginForm.loginName"
              placeholder="请输入用户名"
              :prefix-icon="UserIcon"
              size="large"
            />
          </el-form-item>

          <!-- 密码输入框 -->
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="LockIcon"
              size="large"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <!-- 登录按钮 -->
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="login-btn"
              :loading="loading"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
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
import { loginAPI } from '../api/index.js'

// Element Plus 图标（按需引用，保持包体积小）
import { User, Lock } from '@element-plus/icons-vue'

// shallowRef 适用于不会变化的对象引用，性能更好
const UserIcon = shallowRef(User)
const LockIcon = shallowRef(Lock)

const router = useRouter()

// ========== 表单数据 ==========
const loginForm = reactive({
  loginName: '',
  password: ''
})

// ========== 表单校验规则 ==========
const formRules = {
  loginName: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

// ========== 表单引用（用于调用 validate） ==========
const formRef = ref(null)

// ========== 加载状态 ==========
const loading = ref(false)

// ========== 登录处理 ==========
const handleLogin = async () => {
  // 校验未通过时直接返回，不发送请求
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    // 调用后端 POST /api/user/login
    const res = await loginAPI(loginForm.loginName, loginForm.password)
    const data = res.data.data  // JsonResponse 的结构：{ status, data: {token, userId, loginName} }

    // Token 存入 localStorage，后续请求由 api/index.js 的拦截器自动携带
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('loginName', data.loginName)

    // 登录成功提示，然后跳转首页（后续可改为 /home）
    ElMessage.success(`欢迎回来，${data.loginName}！`)
    // TODO: 等首页做好后改成 router.push('/home')
    // router.push('/home')
  } catch (err) {
    // 网络错误或用户名密码错误
    const msg = err.response?.data?.message || '登录失败，请检查网络连接'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
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
  background: #1A3A5C;   /* 深藏蓝色，与左侧导航栏同色系 */
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
  margin: 0 0 32px 0;
  padding-bottom: 16px;
  border-bottom: 2px solid #2563EB;
  display: inline-block;
}

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
  border-color: #2563EB;
}

.login-el-form :deep(.el-input__wrapper.is-focus) {
  border-color: #2563EB;
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
  background-color: #2563EB;
  border-color: #2563EB;
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
