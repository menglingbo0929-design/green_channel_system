<script setup>
import { onMounted, reactive, ref } from 'vue'
import { confirmArrears, fetchPendingArrears, fetchPendingArrearsDetail } from '../../../api/confirmation'
import BusinessConfirmDialog from '../../../components/school/BusinessConfirmDialog.vue'

const query = reactive({ applicationNo: '', studentNo: '', studentName: '', pageNo: 1, pageSize: 10 })
const rows = ref([])
const total = ref(0)
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
  query.pageNo = 1
  void loadList()
}

function formatMoney(value) {
  if (value === null || value === undefined || value === '') return '—'
  return `¥${Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

async function loadList() {
  loading.value = true
  message.value = ''
  try {
    const response = await fetchPendingArrears(query)
    const page = response.data.data
    rows.value = page.records ?? []
    total.value = page.total ?? 0
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
    await loadList()
  } catch (error) {
    messageType.value = 'error'
    message.value = requestErrorMessage(error, '欠费确认失败，请稍后重试。')
  } finally {
    confirmSubmitting.value = false
  }
}

onMounted(() => { void loadList() })
</script>

<template>
  <section class="confirmation-page">
    <header class="module-heading"><div><h2>欠费确认</h2><p>核对学校审核通过申请的欠费金额，确认后自动生成欠费确认单。</p></div></header>
    <section class="filter-panel">
      <div class="filter-fields">
        <label><span>申请编号</span><input v-model.trim="query.applicationNo" placeholder="请输入申请编号" /></label>
        <label><span>学号</span><input v-model.trim="query.studentNo" placeholder="请输入学号" /></label>
        <label><span>姓名</span><input v-model.trim="query.studentName" placeholder="请输入姓名" /></label>
      </div>
      <div class="filter-actions"><button type="button" class="primary" :disabled="loading" @click="loadList">{{ loading ? '查询中' : '查询' }}</button><button type="button" :disabled="loading" @click="resetQuery">重置</button></div>
    </section>
    <p v-if="message" class="notice" :class="messageType">{{ message }}</p>
    <section class="table-panel">
      <header class="table-heading"><h3>待确认申请</h3><span>共 {{ total }} 条</span></header>
      <div class="table-wrap">
        <table>
          <thead><tr><th>申请编号</th><th>学生信息</th><th>学院</th><th>申请类型</th><th>申报金额</th><th>当前状态</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="row in rows" :key="row.applicationId">
              <td>{{ row.applicationNo }}</td>
              <td><strong>{{ row.studentName }}</strong><small>{{ row.studentNo }}</small></td>
              <td>{{ row.collegeName || '—' }}</td>
              <td>{{ row.applicationTypeName || row.applicationType || '绿色通道' }}</td>
              <td class="money">{{ formatMoney(row.appliedAmount) }}</td>
              <td><span class="status-tag">待欠费确认</span></td>
              <td><button type="button" class="text-button" @click="openConfirmation(row)">确认金额</button></td>
            </tr>
            <tr v-if="!loading && rows.length === 0"><td colspan="7" class="empty">当前数据库中没有进入 CONFIRM_PENDING 的待确认申请</td></tr>
          </tbody>
        </table>
      </div>
    </section>
    <BusinessConfirmDialog v-model="confirmDialogOpen" mode="ARREARS_CONFIRM" :business="selected ?? {}" :submitting="confirmSubmitting" @confirm="submitConfirmation" />
  </section>
</template>

<style scoped>
.confirmation-page { color: #303133; }.module-heading { display: flex; justify-content: space-between; margin-bottom: 18px; }.module-heading h2 { margin: 0 0 7px; font-size: 18px; }.module-heading p { margin: 0; color: #909399; font-size: 13px; }
.filter-panel { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; padding: 18px 20px; border: 1px solid #ebeef5; border-radius: 4px; background: #fafcff; }.filter-fields { display: grid; flex: 1; grid-template-columns: repeat(3, minmax(160px, 1fr)); gap: 18px; }.filter-fields label { display: grid; grid-template-columns: 70px 1fr; align-items: center; gap: 10px; color: #606266; font-size: 14px; white-space: nowrap; }.filter-fields input { width: 100%; height: 34px; padding: 0 11px; border: 1px solid #dcdfe6; border-radius: 4px; color: #303133; outline: none; }.filter-fields input:focus { border-color: #409eff; }.filter-actions { display: flex; gap: 10px; }.filter-actions button { min-width: 72px; height: 34px; border: 1px solid #dcdfe6; border-radius: 4px; color: #606266; background: #fff; cursor: pointer; }.filter-actions .primary { border-color: #409eff; color: #fff; background: #409eff; }.filter-actions button:disabled { opacity: .6; cursor: not-allowed; }
.notice { margin: 14px 0; padding: 10px 14px; border-radius: 4px; font-size: 13px; }.success { border: 1px solid #d9ecff; color: #409eff; background: #ecf5ff; }.error { border: 1px solid #fde2e2; color: #f56c6c; background: #fef0f0; }.table-panel { margin-top: 18px; border: 1px solid #ebeef5; border-radius: 4px; background: #fff; }.table-heading { display: flex; align-items: center; justify-content: space-between; height: 54px; padding: 0 18px; border-bottom: 1px solid #ebeef5; }.table-heading h3 { margin: 0; font-size: 16px; }.table-heading span { color: #909399; font-size: 13px; }.table-wrap { overflow-x: auto; }table { width: 100%; min-width: 840px; border-collapse: collapse; font-size: 14px; }th, td { height: 52px; padding: 8px 14px; border-bottom: 1px solid #ebeef5; text-align: left; }th { color: #606266; background: #f5f7fa; font-weight: 600; }tbody tr:hover { background: #f8fbff; }td strong, td small { display: block; }td small { margin-top: 3px; color: #909399; font-size: 12px; }.money { color: #f56c6c; font-weight: 600; }.status-tag { display: inline-block; padding: 4px 8px; border-radius: 3px; color: #e6a23c; background: #fdf6ec; font-size: 12px; }.text-button { border: 0; color: #409eff; background: transparent; cursor: pointer; font-size: 14px; }.empty { height: 88px; color: #909399; text-align: center; }
@media (max-width: 1100px) { .filter-panel { align-items: stretch; flex-direction: column; }.filter-fields { grid-template-columns: 1fr; }.filter-actions { justify-content: flex-end; } }
</style>
