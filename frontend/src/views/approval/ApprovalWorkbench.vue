<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { CircleCheck, Clock, Refresh, Search, Tickets, UploadFilled, View, Warning } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ApprovalDetailDrawer from '../../components/approval/ApprovalDetailDrawer.vue'
import ApprovalEditableFieldsDialog from '../../components/approval/ApprovalEditableFieldsDialog.vue'
import ReviewDialog from '../../components/approval/ReviewDialog.vue'
import StatusBadge from '../../components/approval/StatusBadge.vue'
import BusinessConfirmDialog from '../../components/school/BusinessConfirmDialog.vue'
import { cancelApplication, editApprovalFields, getApprovalDashboard, getApprovalDetail, getApprovalList, getSubmissionStatus, reviewApplication, submitInitialBatch, submitReturnResubmit } from '../../api/approval'
import { APPLICATION_TYPE_META, createRequestId, formatDateTime, ROLE_META } from '../../constants/approval'
import { formatBatchLabel } from '../../constants/batch'
import { batchAPI } from '../../api/application.js'

const props = defineProps({
  embedded: { type: Boolean, default: false },
})

const route = useRoute()
const role = computed(() => route.meta.role || 'COUNSELOR')
const roleMeta = computed(() => ROLE_META[role.value])
const usesBatchSubmission = computed(() => ['COUNSELOR', 'COLLEGE'].includes(role.value))
const currentTab = ref('pending')
const loading = ref(false)
const list = ref([])
const total = ref(0)
const dashboard = ref({ pending: 0, approvedWaitingSubmit: 0, returned: 0, processed: 0, deadline: '—' })
const submission = ref(null)
const batchOptions = ref([])
const filters = reactive({ page: 1, size: 10, batchType: '', batchId: null, applicationType: '', applicationNo: '', studentNo: '', studentName: '', status: '' })
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const reviewOpen = ref(false)
const reviewSubmitting = ref(false)
const reviewTarget = ref(null)
const batchSubmitting = ref(false)
const resubmittingId = ref(null)
const cancellationOpen = ref(false)
const cancellationSubmitting = ref(false)
const cancellationTarget = ref(null)
const editOpen = ref(false)
const editSubmitting = ref(false)
const errorMessage = (error, fallback) => error.response?.data?.message || error.message || fallback
const hasSelectedBatch = computed(() => Boolean(filters.batchType) && Number(filters.batchId) > 0)
const cancellableStatuses = new Set(['APPROVED', 'CONFIRM_PENDING', 'COMPLETED'])
const submissionBlockReason = computed(() => {
  if (!submission.value) return ''
  if (submission.value.initialSubmitted) return '首次批量提交已完成。后续退回申请请逐条补交。'
  if (submission.value.pendingReviewCount > 0) return `仍有 ${submission.value.pendingReviewCount} 条申请未给出审核结论。`
  if (submission.value.approvedWaitingSubmitCount === 0) return '当前没有审核通过且等待提交的申请。'
  const now = Date.now()
  if (role.value === 'COLLEGE' && submission.value.collegeDeadline && now > new Date(submission.value.collegeDeadline).getTime()) return '学院上报截止时间已过，无法首次批量提交。'
  return '当前批次暂不满足提交条件，请刷新后重试。'
})

const metrics = computed(() => {
  const common = [
    { label: '待审核', value: dashboard.value.pending, hint: '需要逐条给出审核结论', icon: Tickets, tone: 'blue' },
    { label: '已处理', value: dashboard.value.processed, hint: '当前审核层级已产生结论', icon: CircleCheck, tone: 'success' },
    { label: '退回修改', value: dashboard.value.returned, hint: '学生修改后将重新进入审核流程', icon: Warning, tone: 'danger' },
  ]
  if (!usesBatchSubmission.value) {
    return [...common, { label: '审核范围', value: '全校', hint: '学校管理员查看全校申请', icon: Clock, tone: 'warning' }]
  }
  return [
    common[0],
    { label: '通过待上报', value: dashboard.value.approvedWaitingSubmit, hint: `审核通过后提交至${roleMeta.value.nextLevel}`, icon: CircleCheck, tone: 'success' },
    common[2],
    { label: '提交截止时间', value: dashboard.value.deadline, hint: '请预留统一提交时间', icon: Clock, tone: 'warning' },
  ]
})

function queryParams() {
  return Object.fromEntries(Object.entries(filters).filter(([, value]) => value !== '' && value !== null))
}

async function loadWorkspace() {
  loading.value = true
  try {
    const countParams = { ...queryParams(), page: 1, size: 1 }
    const [pageData, dashboardData, submissionData, pendingPage, processedPage, returnedPage] = await Promise.all([
      getApprovalList(role.value, currentTab.value, queryParams()),
      getApprovalDashboard(role.value, queryParams()),
      usesBatchSubmission.value && hasSelectedBatch.value
        ? getSubmissionStatus(role.value, { batchType: filters.batchType, batchId: Number(filters.batchId) })
        : Promise.resolve(null),
      getApprovalList(role.value, 'pending', countParams),
      getApprovalList(role.value, 'processed', countParams),
      getApprovalList(role.value, 'returned', countParams),
    ])
    list.value = pageData.records
    total.value = pageData.total
    dashboard.value = {
      ...dashboardData,
      pending: pendingPage.total,
      processed: processedPage.total,
      returned: returnedPage.total,
    }
    submission.value = submissionData
  } catch (error) {
    ElMessage.error(errorMessage(error, '审核数据加载失败'))
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  Object.assign(filters, { page: 1, size: 10, batchType: '', batchId: null, applicationType: '', applicationNo: '', studentNo: '', studentName: '', status: '' })
  loadWorkspace()
}

async function loadBatchOptions(batchType) {
  batchOptions.value = batchType ? (await batchAPI.open(batchType) || []) : []
}

function search() {
  filters.page = 1
  loadWorkspace()
}

function switchTab(tab) {
  currentTab.value = tab
  filters.page = 1
  loadWorkspace()
}

async function openDetail(row) {
  detailOpen.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getApprovalDetail(row.applicationId, role.value)
  } catch (error) {
    ElMessage.error(errorMessage(error, '详情加载失败'))
    detailOpen.value = false
  } finally {
    detailLoading.value = false
  }
}

function openReview(application) {
  // 列表行自带 version，但从详情抽屉发起审核时版本号位于
  // detail.version。统一补齐，避免审核请求携带 null 版本号。
  const detailVersion = detail.value?.application?.applicationId === application?.applicationId
    ? detail.value.version
    : null
  reviewTarget.value = {
    ...application,
    version: application?.version ?? detailVersion,
  }
  reviewOpen.value = true
}

function canCancel(application) {
  return role.value === 'SCHOOL' && cancellableStatuses.has(application?.status)
}

function openCancellation(application) {
  if (!canCancel(application)) return
  const detailVersion = detail.value?.application?.applicationId === application.applicationId
    ? detail.value.version
    : null
  cancellationTarget.value = {
    ...application,
    version: application.version ?? detailVersion,
  }
  cancellationOpen.value = true
}

async function submitCancellation({ reason }) {
  if (!cancellationTarget.value) return
  cancellationSubmitting.value = true
  try {
    const result = await cancelApplication(cancellationTarget.value.applicationId, {
      reason,
      version: cancellationTarget.value.version,
      requestId: createRequestId(),
    })
    ElMessage.success(`申请已取消，当前状态：${result.statusName || result.status || '已取消'}`)
    cancellationOpen.value = false
    detailOpen.value = false
    cancellationTarget.value = null
    await loadWorkspace()
  } catch (error) {
    ElMessage.error(errorMessage(error, '取消申请失败'))
  } finally {
    cancellationSubmitting.value = false
  }
}

async function submitReview(payload) {
  reviewSubmitting.value = true
  try {
    const result = await reviewApplication(role.value, reviewTarget.value.applicationId, payload)
    ElMessage.success(`审核意见已提交，当前状态：${result.statusName || result.status || '已更新'}`)
    reviewOpen.value = false
    detailOpen.value = false
    await loadWorkspace()
  } catch (error) {
    ElMessage.error(errorMessage(error, '审核提交失败'))
  } finally {
    reviewSubmitting.value = false
  }
}

async function submitFieldEdit(payload) {
  if (!detail.value?.application?.applicationId) return
  editSubmitting.value = true
  try {
    await editApprovalFields(detail.value.application.applicationId, {
      ...payload,
      version: detail.value.version,
      requestId: createRequestId(),
    })
    ElMessage.success('申请字段已修改并写入审核记录')
    editOpen.value = false
    detail.value = await getApprovalDetail(detail.value.application.applicationId, role.value)
    await loadWorkspace()
  } catch (error) {
    ElMessage.error(errorMessage(error, '修改申请字段失败'))
  } finally {
    editSubmitting.value = false
  }
}

async function submitBatch() {
  if (!hasSelectedBatch.value) {
    ElMessage.warning('请先选择批次类型和申请批次')
    return
  }
  try {
    await ElMessageBox.confirm(
      `系统将自动提交当前范围内 ${submission.value.approvedWaitingSubmitCount} 条已通过申请，任一失败将整批回滚。`,
      `首次提交${roleMeta.value.nextLevel}`,
      { confirmButtonText: '确认提交', cancelButtonText: '取消', type: 'warning' },
    )
    batchSubmitting.value = true
    const result = await submitInitialBatch(role.value, {
      batchType: filters.batchType,
      batchId: Number(filters.batchId),
      requestId: createRequestId(),
    })
    ElMessage.success(`提交成功，共推进 ${result.submittedCount} 条申请`)
    await loadWorkspace()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(errorMessage(error, '批量提交失败'))
  } finally {
    batchSubmitting.value = false
  }
}

async function resubmitReturned(row) {
  try {
    await ElMessageBox.confirm(
      `确认将 ${row.studentName} 的申请逐条补交至${roleMeta.value.nextLevel}？`,
      '退回申请补交',
      { confirmButtonText: '确认补交', cancelButtonText: '取消', type: 'warning' },
    )
    resubmittingId.value = row.applicationId
    const result = await submitReturnResubmit(role.value, { applicationId: row.applicationId, version: row.version, requestId: createRequestId() })
    ElMessage.success(`补交成功，当前状态：${result.statusName || result.status || '已更新'}`)
    await loadWorkspace()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(errorMessage(error, '补交失败'))
  } finally {
    resubmittingId.value = null
  }
}

watch(role, () => {
  currentTab.value = 'pending'
  resetFilters()
})
watch(() => filters.batchType, async (value) => {
  filters.batchId = null
  submission.value = null
  try { await loadBatchOptions(value) } catch (error) { batchOptions.value = []; ElMessage.error(errorMessage(error, '批次列表加载失败')) }
})
onMounted(loadWorkspace)
</script>

<template>
  <div class="page-container workbench-page">
    <section class="page-heading-row">
      <div>
        <h1>{{ roleMeta.title }}</h1>
        <p>{{ role === 'COUNSELOR' ? '核验学生材料、确认补助金额；审核通过后将直接进入学院审核。' : role === 'COLLEGE' ? '复核本学院申请、检查名额额度；审核通过后将直接进入学校审核。' : '对全校申请进行最终审核，审核结论将直接进入办结或欠费确认流程。' }}</p>
      </div>
      <div v-if="!props.embedded && role !== 'COLLEGE'" class="page-actions">
        <el-button @click="loadWorkspace"><el-icon><Refresh /></el-icon>刷新</el-button>
      </div>
    </section>

    <section v-if="!props.embedded && role !== 'COLLEGE'" class="metric-grid" aria-label="审核数量概览">
      <article v-for="metric in metrics" :key="metric.label" class="summary-card" :class="`summary-${metric.tone}`">
        <div class="summary-icon"><component :is="metric.icon" /></div>
        <div><span>{{ metric.label }}</span><strong>{{ metric.value }}</strong><small>{{ metric.hint }}</small></div>
      </article>
    </section>

    <section v-if="usesBatchSubmission && submission && total > 0" class="submission-bar" :class="{ completed: submission.initialSubmitted }">
      <div class="submission-icon"><UploadFilled /></div>
      <div class="submission-copy">
        <strong>{{ submission.initialSubmitted ? `本批次已提交${roleMeta.nextLevel}` : `当前有 ${submission.approvedWaitingSubmitCount} 条申请通过待提交` }}</strong>
        <p v-if="submission.initialSubmitted">提交时间：{{ formatDateTime(submission.submittedAt) }}，首次批量提交已完成。</p>
        <p v-else>{{ submission.canSubmit ? `已完成逐条审核，可统一提交 ${submission.approvedWaitingSubmitCount} 条申请。` : submissionBlockReason }}</p>
      </div>
      <span class="submission-deadline">截止：{{ dashboard.deadline }}</span>
      <el-button v-if="!submission.initialSubmitted" type="primary" :disabled="!submission.canSubmit" :loading="batchSubmitting" @click="submitBatch">首次提交{{ roleMeta.nextLevel }}</el-button>
      <span v-else class="submission-complete"><CircleCheck />已完成</span>
    </section>

    <section class="content-card records-card">
      <div class="card-tabs">
        <button type="button" :class="{ active: currentTab === 'pending' }" @click="switchTab('pending')">待审核 <span>{{ dashboard.pending }}</span></button>
        <button type="button" :class="{ active: currentTab === 'processed' }" @click="switchTab('processed')">已审核 <span>{{ dashboard.processed }}</span></button>
        <button type="button" :class="{ active: currentTab === 'returned' }" @click="switchTab('returned')">退回补交 <span>{{ dashboard.returned }}</span></button>
      </div>

      <div class="standard-filter-grid">
        <div class="filter-field"><label>批次类型</label><el-select v-model="filters.batchType" clearable placeholder="全部类型"><el-option label="绿色通道" value="GREEN_CHANNEL" /><el-option label="新生补助" value="SUBSIDY" /></el-select></div>
        <div class="filter-field"><label>申请批次</label><el-select v-model="filters.batchId" clearable filterable :disabled="!filters.batchType" placeholder="请选择批次"><el-option v-for="batch in batchOptions" :key="batch.batchId" :label="formatBatchLabel(batch)" :value="batch.batchId" /></el-select></div>
        <div class="filter-field"><label>申请类型</label><el-select v-model="filters.applicationType" clearable placeholder="全部类型"><el-option v-for="(label, value) in APPLICATION_TYPE_META" :key="value" :label="label" :value="value" /></el-select></div>
        <div class="filter-field"><label>学号</label><el-input v-model="filters.studentNo" clearable placeholder="请输入学号" /></div>
        <div class="filter-field"><label>学生姓名</label><el-input v-model="filters.studentName" clearable placeholder="请输入姓名" /></div>
        <div class="filter-field"><label>申请编号</label><el-input v-model="filters.applicationNo" clearable placeholder="请输入申请编号" @keyup.enter="search" /></div>
        <div class="filter-buttons"><el-button type="primary" @click="search"><el-icon><Search /></el-icon>查询</el-button><el-button @click="resetFilters">重置</el-button></div>
      </div>

      <el-table v-loading="loading" :data="list" border class="standard-table" row-key="applicationId" empty-text="当前条件下暂无申请">
        <el-table-column label="申请编号" min-width="170"><template #default="{ row }"><strong class="primary-cell">{{ row.applicationNo }}</strong><span class="secondary-cell">v{{ row.version }}</span></template></el-table-column>
        <el-table-column label="学生信息" min-width="150"><template #default="{ row }"><strong class="primary-cell">{{ row.studentName }}</strong><span class="secondary-cell">{{ row.studentNo }}</span></template></el-table-column>
        <el-table-column label="学院 / 专业" min-width="210"><template #default="{ row }"><strong class="primary-cell">{{ row.collegeName }}</strong><span class="secondary-cell">{{ row.majorName }} · {{ row.className }}</span></template></el-table-column>
        <el-table-column prop="applicationTypeName" label="申请类型" min-width="112" />
        <el-table-column label="申请金额" width="118"><template #default="{ row }">¥{{ row.declaredAmount.toLocaleString() }}</template></el-table-column>
        <el-table-column label="提交时间" width="136"><template #default="{ row }">{{ formatDateTime(row.submitTime) }}</template></el-table-column>
        <el-table-column label="状态" min-width="142"><template #default="{ row }"><StatusBadge :status="row.status" /></template></el-table-column>
        <el-table-column label="操作" width="260" fixed="right"><template #default="{ row }"><div class="table-actions"><el-button size="small" @click="openDetail(row)"><el-icon><View /></el-icon>详情</el-button><el-button v-if="currentTab === 'pending'" size="small" type="primary" @click="openReview(row)">审核</el-button><el-button v-else-if="currentTab === 'returned' && usesBatchSubmission" size="small" type="primary" plain :loading="resubmittingId === row.applicationId" @click="resubmitReturned(row)">逐条补交</el-button><el-button v-if="canCancel(row)" size="small" type="danger" plain @click="openCancellation(row)">取消申请</el-button></div></template></el-table-column>
      </el-table>

      <div class="pagination-row"><span>共 {{ total }} 条记录</span><el-pagination v-model:current-page="filters.page" v-model:page-size="filters.size" layout="prev, pager, next" :total="total" @current-change="loadWorkspace" /></div>
    </section>

    <ApprovalDetailDrawer v-model="detailOpen" :detail="detail" :loading="detailLoading" :role="role" @review="openReview" @cancel="openCancellation" @edit="editOpen = true" />
    <ApprovalEditableFieldsDialog v-model="editOpen" :detail="detail" :submitting="editSubmitting" @submit="submitFieldEdit" />
    <ReviewDialog v-model="reviewOpen" :application="reviewTarget" :role="role" :submitting="reviewSubmitting" @submit="submitReview" />
    <BusinessConfirmDialog v-model="cancellationOpen" mode="CANCEL_APPLICATION" :business="cancellationTarget || {}" :submitting="cancellationSubmitting" @confirm="submitCancellation" />
  </div>
</template>
