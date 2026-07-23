<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { confirmArrears, fetchPendingArrears, fetchPendingArrearsDetail } from '../../../api/confirmation'
import { pageVouchers } from '../../../api/voucher'
import BusinessConfirmDialog from '../../../components/school/BusinessConfirmDialog.vue'
import StatusBadge from '../../../components/approval/StatusBadge.vue'

const activeTab = ref('pending')
const query = reactive({ applicationNo: '', studentNo: '', studentName: '', voucherNo: '', pageNo: 1, pageSize: 10 })
const rows = ref([])
const total = ref(0)
const pendingTotal = ref(0)
const confirmedTotal = ref(0)
const loading = ref(false)
const message = ref('')
const messageType = ref('success')
const selected = ref(null)
const confirmDialogOpen = ref(false)
const confirmSubmitting = ref(false)

function newRequestId() {
  return globalThis.crypto?.randomUUID?.() ?? `confirm-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function requestErrorMessage(error, fallback) {
  return error?.response?.data?.message || error?.message || fallback
}

function resetQuery() {
  query.applicationNo = ''
  query.studentNo = ''
  query.studentName = ''
  query.voucherNo = ''
  query.pageNo = 1
  void loadList()
}

function search() {
  query.pageNo = 1
  void loadList()
}

function formatMoney(value) {
  if (value === null || value === undefined || value === '') return '—'
  return `¥${Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '—'
}

function switchTab(tab) {
  activeTab.value = tab
  query.pageNo = 1
  rows.value = []
  void loadList()
}

async function loadList() {
  loading.value = true
  message.value = ''
  try {
    const response = activeTab.value === 'pending'
      ? await fetchPendingArrears(query)
      : await pageVouchers({
          pageNo: query.pageNo,
          pageSize: query.pageSize,
          voucherNo: query.voucherNo || undefined,
        })
    const page = response.data.data
    rows.value = page.records ?? []
    total.value = page.total ?? 0
    if (activeTab.value === 'pending') pendingTotal.value = total.value
    else confirmedTotal.value = total.value
  } catch (error) {
    rows.value = []
    total.value = 0
    messageType.value = 'error'
    message.value = requestErrorMessage(error, '查询待确认申请失败，请检查后端服务和登录状态。')
  } finally {
    loading.value = false
  }
}

async function openConfirmation(row) {
  message.value = ''
  try {
    const response = await fetchPendingArrearsDetail(row.applicationId)
    selected.value = response.data.data
    confirmDialogOpen.value = true
  } catch (error) {
    messageType.value = 'error'
    message.value = requestErrorMessage(error, '读取申请详情失败。')
  }
}

async function submitConfirmation(dialogForm) {
  message.value = ''
  confirmSubmitting.value = true
  try {
    const response = await confirmArrears(selected.value.applicationId, {
      confirmedAmount: dialogForm.confirmedAmount,
      version: selected.value.version,
      requestId: newRequestId(),
    })
    messageType.value = 'success'
    message.value = `确认成功，单据号：${response.data.data.voucherNo}`
    confirmDialogOpen.value = false
    selected.value = null
    await Promise.all([loadList(), loadTabCounts()])
  } catch (error) {
    messageType.value = 'error'
    message.value = requestErrorMessage(error, '欠费确认失败，请稍后重试。')
  } finally {
    confirmSubmitting.value = false
  }
}

async function loadTabCounts() {
  const [pendingResponse, confirmedResponse] = await Promise.all([
    fetchPendingArrears({ pageNo: 1, pageSize: 1 }),
    pageVouchers({ pageNo: 1, pageSize: 1 }),
  ])
  pendingTotal.value = Number(pendingResponse.data.data?.total ?? 0)
  confirmedTotal.value = Number(confirmedResponse.data.data?.total ?? 0)
}

onMounted(async () => {
  await Promise.all([loadList(), loadTabCounts()])
})
</script>

<template>
  <section class="confirmation-page">
    <header class="module-heading"><div><h2>欠费确认</h2><p>核对学校审核通过申请的欠费金额，确认后自动生成欠费确认单。</p></div></header>
    <p v-if="message" class="notice" :class="messageType">{{ message }}</p>
    <section class="content-card records-card">
      <div class="card-tabs">
        <button type="button" :class="{ active: activeTab === 'pending' }" @click="switchTab('pending')">待确认 <span>{{ pendingTotal }}</span></button>
        <button type="button" :class="{ active: activeTab === 'confirmed' }" @click="switchTab('confirmed')">已确认 <span>{{ confirmedTotal }}</span></button>
      </div>

      <div class="standard-filter-grid arrears-filter-grid">
        <template v-if="activeTab === 'pending'">
          <div class="filter-field"><label>申请编号</label><el-input v-model.trim="query.applicationNo" clearable placeholder="请输入申请编号" @keyup.enter="search" /></div>
          <div class="filter-field"><label>学号</label><el-input v-model.trim="query.studentNo" clearable placeholder="请输入学号" @keyup.enter="search" /></div>
          <div class="filter-field"><label>学生姓名</label><el-input v-model.trim="query.studentName" clearable placeholder="请输入学生姓名" @keyup.enter="search" /></div>
        </template>
        <div v-else class="filter-field"><label>单据编号</label><el-input v-model.trim="query.voucherNo" clearable placeholder="例如 GC2026" @keyup.enter="search" /></div>
        <div class="filter-buttons"><el-button type="primary" :loading="loading" @click="search"><el-icon><Search /></el-icon>查询</el-button><el-button :disabled="loading" @click="resetQuery">重置</el-button></div>
      </div>

      <el-table v-loading="loading" :data="rows" border class="standard-table" :row-key="activeTab === 'pending' ? 'applicationId' : 'voucherNo'" :empty-text="activeTab === 'pending' ? '当前没有待确认申请' : '当前没有已确认单据'">
        <el-table-column :label="activeTab === 'pending' ? '申请编号' : '单据编号'" min-width="180"><template #default="{ row }"><strong class="primary-cell">{{ activeTab === 'pending' ? row.applicationNo : row.voucherNo }}</strong><span v-if="activeTab === 'confirmed'" class="secondary-cell">申请ID：{{ row.applicationId }}</span></template></el-table-column>
        <el-table-column label="学生信息" min-width="160"><template #default="{ row }"><strong class="primary-cell">{{ row.studentName }}</strong><span class="secondary-cell">{{ row.studentNo }}</span></template></el-table-column>
        <el-table-column label="学院" min-width="220"><template #default="{ row }">{{ row.collegeName || '—' }}</template></el-table-column>
        <el-table-column v-if="activeTab === 'pending'" label="申请类型" min-width="130"><template #default="{ row }">{{ row.applicationTypeName || row.applicationType || '绿色通道' }}</template></el-table-column>
        <el-table-column label="申报金额" width="150"><template #default="{ row }"><strong class="money-cell">{{ formatMoney(row.appliedAmount) }}</strong></template></el-table-column>
        <el-table-column v-if="activeTab === 'confirmed'" label="确认金额" width="150"><template #default="{ row }"><strong class="money-cell">{{ formatMoney(row.confirmedAmount) }}</strong></template></el-table-column>
        <el-table-column v-if="activeTab === 'confirmed'" label="确认信息" min-width="180"><template #default="{ row }"><strong class="primary-cell">{{ row.confirmUserName || '—' }}</strong><span class="secondary-cell">{{ formatTime(row.confirmedTime) }}</span></template></el-table-column>
        <el-table-column label="状态" width="160"><template #default><StatusBadge :status="activeTab === 'pending' ? 'CONFIRM_PENDING' : 'COMPLETED'" /></template></el-table-column>
        <el-table-column v-if="activeTab === 'pending'" label="操作" width="150" fixed="right"><template #default="{ row }"><div class="table-actions"><el-button size="small" type="primary" @click="openConfirmation(row)">确认金额</el-button></div></template></el-table-column>
      </el-table>

      <div class="pagination-row">
        <span>共 {{ total }} 条记录</span>
        <el-pagination v-model:current-page="query.pageNo" :page-size="query.pageSize" layout="prev, pager, next" :total="total" @current-change="loadList" />
      </div>
    </section>
    <BusinessConfirmDialog v-model="confirmDialogOpen" mode="ARREARS_CONFIRM" :business="selected ?? {}" :submitting="confirmSubmitting" @confirm="submitConfirmation" />
  </section>
</template>

<style scoped>
.confirmation-page { color: #303133; }
.module-heading { display: flex; justify-content: space-between; margin-bottom: 18px; }
.module-heading h2 { margin: 0 0 7px; color: #1f2937; font-size: 18px; }
.module-heading p { margin: 0; color: #909399; font-size: 13px; }
.arrears-filter-grid { grid-template-columns: repeat(3, minmax(180px, 1fr)) auto; }
.money-cell { color: #f56c6c; font-weight: 600; }
.notice { margin: 0 0 14px; padding: 10px 14px; border-radius: 4px; font-size: 13px; }
.success { border: 1px solid #d9ecff; color: #409eff; background: #ecf5ff; }
.error { border: 1px solid #fde2e2; color: #f56c6c; background: #fef0f0; }
@media (max-width: 1100px) { .arrears-filter-grid { grid-template-columns: 1fr; } }
</style>
