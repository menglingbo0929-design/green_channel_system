<template>
  <!--
    首页 —— 登录后的默认仪表盘
    展示当前用户信息、角色和快捷入口
  -->
  <div class="home-page">
    <!-- 页面标题 -->
    <h1 class="page-title">首页</h1>

    <!-- 欢迎卡片 -->
    <div class="card welcome-card">
      <div class="welcome-left">
        <h2 class="welcome-msg">
          欢迎回来，{{ userStore.loginName }}
          <span class="welcome-role">{{ userStore.roleLabel }}</span>
        </h2>
        <p class="welcome-sub">高校绿色通道系统 · 保障家庭经济困难学生顺利入学</p>
      </div>
      <div class="welcome-right">
        <div class="stat-mini">
          <span class="stat-num">9</span>
          <span class="stat-label">业务页面</span>
        </div>
        <div class="stat-mini">
          <span class="stat-num">4</span>
          <span class="stat-label">系统角色</span>
        </div>
        <div class="stat-mini">
          <span class="stat-num">5</span>
          <span class="stat-label">通用弹窗</span>
        </div>
      </div>
    </div>

    <!-- 快速入口 -->
    <h3 class="section-title">快速入口</h3>
    <div class="quick-links">
      <div
        v-for="item in quickLinks"
        :key="item.key"
        class="card quick-card"
        @click="$router.push(item.path)"
      >
        <div class="quick-icon">
          <el-icon :size="24"><component :is="item.icon" /></el-icon>
        </div>
        <span class="quick-label">{{ item.label }}</span>
      </div>
    </div>

    <!-- 当前角色与权限 -->
    <h3 class="section-title">当前权限</h3>
    <div class="card permission-card">
      <el-table :data="roleTable" border size="small" style="width:100%">
        <el-table-column prop="role" label="角色" width="160" />
        <el-table-column prop="desc" label="说明" />
        <el-table-column prop="defaultPage" label="默认页面" width="200" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '../stores/user.js'
import {
  EditPen, Document, CircleCheck, Flag, TrendCharts, DataBoard, InfoFilled
} from '@element-plus/icons-vue'

const userStore = useUserStore()

// 快速入口：仅包含侧边栏已有的菜单项，路径与侧边栏一致
const quickLinks = computed(() => {
  const all = [
    { key: 'student',   label: '学生申请中心', path: '/student-center',   icon: EditPen,      roles: ['STUDENT'] },
    { key: 'my-apply',  label: '我的申请',     path: '/my-apply',         icon: Document,      roles: ['STUDENT'] },
    { key: 'counselor', label: '辅导员审核',   path: '/counselor-review',  icon: CircleCheck,  roles: ['COUNSELOR'] },
    { key: 'college',   label: '学院审核',     path: '/college-review',    icon: Flag,         roles: ['COLLEGE'] },
    { key: 'school',    label: '学校审核',     path: '/school-review',     icon: CircleCheck,  roles: ['SCHOOL'] },
    { key: 'stats',     label: '统计报表',     path: '/stats',             icon: TrendCharts,  roles: ['SCHOOL'] },
    { key: 'base-data', label: '基础数据',     path: '/base-data',         icon: DataBoard,    roles: ['SCHOOL'] },
    { key: 'policy',    label: '政策与说明',   path: '/policy',            icon: InfoFilled,   roles: ['STUDENT','COUNSELOR','COLLEGE','SCHOOL'] }
  ]
  return all.filter(m => m.roles.some(r => userStore.roles.includes(r)))
})

const roleTable = computed(() => {
  const map = {
    STUDENT:   { role: '学生',       desc: '申请绿色通道、补助，查看审核进度',     defaultPage: '学生申请中心' },
    COUNSELOR: { role: '辅导员',     desc: '审核负责学生申请，提交学院',          defaultPage: '辅导员审核页' },
    COLLEGE:   { role: '学院管理员', desc: '审核本学院申请，管理名额与额度',       defaultPage: '学院审核页' },
    SCHOOL:    { role: '学校管理员', desc: '管理基础数据、最终审核、欠费确认、统计', defaultPage: '学校业务处理页' }
  }
  return userStore.roles.map(r => map[r] || { role: r, desc: '', defaultPage: '' })
})
</script>

<style scoped>
.home-page {
  max-width: 1328px;               /* 规范：有效内容宽度 1328px */
}

/* —— 页面标题 —— */
.page-title {
  font-size: 20px;
  font-weight: 600;
  line-height: 28px;
  color: #1F2937;
  margin: 0 0 16px;
}

/* —— 卡片基础 —— */
.card {
  background: #FFFFFF;
  border: 1px solid #E5E7EB;
  border-radius: 4px;
  padding: 20px;
  margin-bottom: 16px;
}

/* —— 欢迎卡片 —— */
.welcome-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.welcome-msg {
  font-size: 18px;
  font-weight: 600;
  color: #1F2937;
  margin: 0 0 8px;
}

.welcome-role {
  display: inline-block;
  background: #1677FF;
  color: #fff;
  font-size: 13px;
  padding: 2px 10px;
  border-radius: 4px;
  margin-left: 12px;
  vertical-align: middle;
}

.welcome-sub {
  font-size: 14px;
  color: #6B7280;
  margin: 0;
}

.welcome-right {
  display: flex;
  gap: 32px;
}

.stat-mini {
  text-align: center;
}

.stat-num {
  display: block;
  font-size: 28px;
  font-weight: 700;
  color: #1677FF;
  line-height: 36px;
}

.stat-label {
  font-size: 12px;
  color: #6B7280;
}

/* —— 区块标题 —— */
.section-title {
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
  color: #1F2937;
  margin: 24px 0 12px;
}

/* —— 快速入口 —— */
.quick-links {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  margin-bottom: 0;
  transition: border-color 0.15s;
}

.quick-card:hover {
  border-color: #1677FF;
}

.quick-icon {
  width: 40px;
  height: 40px;
  background: #EEF2FF;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1677FF;
}

.quick-label {
  font-size: 14px;
  font-weight: 500;
  color: #1F2937;
}

/* —— 权限表格 —— */
.permission-card {
  padding: 0;
  overflow: hidden;
}
</style>
