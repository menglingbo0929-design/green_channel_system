<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Document, EditPen, Refresh, Search, Tickets, View, Wallet } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import ApprovalDetailDrawer from '../../components/approval/ApprovalDetailDrawer.vue'
import StatusBadge from '../../components/approval/StatusBadge.vue'
import { getMyApplicationDetail, getMyApplications } from '../../api/approval'
import { APPLICATION_TYPE_META, formatDateTime } from '../../constants/approval'

const loading = ref(false)
const router = useRouter()
const records = ref([])
const total = ref(0)
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const filters = reactive({ category: 'ALL', applicationType: '', keyword: '', page: 1, size: 10 })
const errorMessage = (error, fallback) => error.response?.data?.message || error.message || fallback

const categories = [
  { value: 'ALL', label: '全部' },
  { value: 'DRAFT', label: '草稿' },
  { value: 'REVIEWING', label: '审核中' },
  { value: 'RETURNED', label: '已退回' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'COMPLETED', label: '已完成' },
]

const metrics = computed(() => {
  const source = records.value
  return [
    { label: '申请总数', value: total.value, hint: '本学年全部申请', icon: Tickets, tone: 'blue' },
    { label: '审核中', value: source.filter(item => ['COUNSELOR_PENDING', 'COLLEGE_PENDING', 'SCHOOL_PENDING', 'CONFIRM_PENDING'].includes(item.status)).length, hint: '请关注审核进度', icon: Refresh, tone: 'warning' },
    { label: '需修改', value: source.filter(item => item.status?.endsWith('_RETURNED')).length, hint: '按退回意见补充材料', icon: EditPen, tone: 'danger' },
    { label: '已办结', value: source.filter(item => ['APPROVED', 'COMPLETED'].includes(item.status)).length, hint: '可查看线下办理提示', icon: Document, tone: 'success' },
  ]
})

async function loadApplications() {
  loading.value = true
  try {
    const data = await getMyApplications({ ...filters })
    records.value = data.records
    total.value = data.total
  } catch (error) {
    ElMessage.error(errorMessage(error, '申请列表加载失败'))
  } finally {
    loading.value = false
  }
}

function changeCategory(value) {
  filters.category = value
  filters.page = 1
  loadApplications()
}

function resetFilters() {
  Object.assign(filters, { category: 'ALL', applicationType: '', keyword: '', page: 1, size: 10 })
  loadApplications()
}

async function openDetail(row) {
  detailOpen.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getMyApplicationDetail(row.applicationId)
  } catch (error) {
    ElMessage.error(errorMessage(error, '申请详情加载失败'))
    detailOpen.value = false
  } finally {
    detailLoading.value = false
  }
}

function handlePrimaryAction(row) {
  if (row.status === 'DRAFT' || row.status?.endsWith('_RETURNED')) {
    router.push({ name: 'StudentCenter', query: { applicationId: row.applicationId, applicationType: row.applicationType } })
    return
  }
  ElMessage.info('当前申请已进入后续办理阶段，请关注消息中心通知。')
}

function openVoucher() {
  ElMessage.info('欠费单据将在学校完成金额确认后开放查看')
}

onMounted(loadApplications)
</script>

<template>
  <div class="page-container my-applications-page">
    <section class="page-heading-row">
      <div>
        <h1>我的申请</h1>
        <p>查看绿色通道与新生补助申请进度，及时处理退回事项。</p>
      </div>
      <div class="page-actions">
        <el-button @click="loadApplications"><el-icon><Refresh /></el-icon>刷新</el-button>
        <el-button type="primary" @click="router.push({ name: 'StudentCenter' })"><el-icon><Document /></el-icon>发起新申请</el-button>
      </div>
    </section>

    <section class="metric-grid" aria-label="申请概览">
      <article v-for="metric in metrics" :key="metric.label" class="summary-card" :class="`summary-${metric.tone}`">
        <div class="summary-icon"><component :is="metric.icon" /></div>
        <div><span>{{ metric.label }}</span><strong>{{ metric.value }}</strong><small>{{ metric.hint }}</small></div>
      </article>
    </section>

    <section class="business-notice">
      <div class="notice-icon"><Wallet /></div>
      <div><strong>线下办理提醒</strong><p>学校审核通过后，请按通知携带相关材料办理报到手续；欠费确认完成后可在申请详情中查看单据。</p></div>
      <el-button plain type="primary" @click="openVoucher">查看欠费单据</el-button>
    </section>

    <section class="content-card records-card">
      <div class="card-tabs">
        <button v-for="item in categories" :key="item.value" type="button" :class="{ active: filters.category === item.value }" @click="changeCategory(item.value)">{{ item.label }}</button>
      </div>

      <div class="standard-filter-row">
        <div class="filter-field"><label>申请类型</label><el-select v-model="filters.applicationType" clearable placeholder="全部类型"><el-option v-for="(label, value) in APPLICATION_TYPE_META" :key="value" :label="label" :value="value" /></el-select></div>
        <div class="filter-field filter-field-wide"><label>申请信息</label><el-input v-model="filters.keyword" clearable placeholder="申请编号或申请名称" :prefix-icon="Search" @keyup.enter="loadApplications" /></div>
        <div class="filter-buttons"><el-button type="primary" @click="loadApplications">查询</el-button><el-button @click="resetFilters">重置</el-button></div>
      </div>

      <el-table v-loading="loading" :data="records" border class="standard-table" empty-text="暂无符合条件的申请">
        <el-table-column label="申请编号" min-width="168"><template #default="{ row }"><strong class="primary-cell">{{ row.applicationNo }}</strong><span class="secondary-cell">{{ row.batchName }}</span></template></el-table-column>
        <el-table-column prop="applicationTypeName" label="申请类型" min-width="112" />
        <el-table-column label="申请金额" width="120"><template #default="{ row }">¥{{ row.declaredAmount.toLocaleString() }}</template></el-table-column>
        <el-table-column label="当前审核节点" min-width="132"><template #default="{ row }">{{ row.currentNode }}</template></el-table-column>
        <el-table-column label="状态" min-width="142"><template #default="{ row }"><StatusBadge :status="row.status" /></template></el-table-column>
        <el-table-column label="提交时间" width="138"><template #default="{ row }">{{ formatDateTime(row.submitTime) }}</template></el-table-column>
        <el-table-column label="操作" width="224" fixed="right"><template #default="{ row }"><div class="table-actions"><el-button size="small" @click="openDetail(row)"><el-icon><View /></el-icon>详情</el-button><el-button v-if="row.status === 'DRAFT' || row.status.endsWith('_RETURNED')" size="small" type="primary" @click="handlePrimaryAction(row)">{{ row.status === 'DRAFT' ? '继续填写' : '修改重提' }}</el-button><el-button v-else-if="['APPROVED', 'COMPLETED'].includes(row.status)" size="small" type="primary" plain @click="handlePrimaryAction(row)">办理说明</el-button></div></template></el-table-column>
      </el-table>

      <div class="pagination-row"><span>共 {{ total }} 条记录</span><el-pagination v-model:current-page="filters.page" v-model:page-size="filters.size" layout="prev, pager, next" :total="total" @current-change="loadApplications" /></div>
    </section>

    <ApprovalDetailDrawer v-model="detailOpen" :detail="detail" :loading="detailLoading" role="STUDENT" />
  </div>
</template>
