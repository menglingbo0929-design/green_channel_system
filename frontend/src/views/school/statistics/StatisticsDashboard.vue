<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import * as echarts from 'echarts'
import {
  exportStatisticsExcel,
  fetchApplicationStatistics,
  fetchStatisticsPrintData,
  fetchStatisticsReport,
} from '../../../api/statistics'
import { classAPI, collegeAPI, gradeAPI, majorAPI } from '../../../api/index'
import SchoolWorkspaceShell from '../../../components/school/SchoolWorkspaceShell.vue'
import { formatBatchLabel } from '../../../constants/batch.js'

/**
 * 页面 9：学校统计看板页。
 *
 * 页面只负责把后端统计结果展示为数字、ECharts 图表和动态列表，不在前端写死
 * 演示数字。页头采用与页面 8 相同的无边框格式，右侧操作按钮仍保留在同一行。
 */
const loading = ref(false)
const summary = ref(null)
const report = ref({ columns: [], records: [], total: 0, pageNo: 1, pageSize: 20, pages: 0 })
const collegeChartRef = ref(null)
const reasonChartRef = ref(null)
const giftChartRef = ref(null)
const batchSelectRef = ref(null)
const columnDialogVisible = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const selectedBatchKey = ref('')

let collegeChart
let reasonChart
let giftChart

const organizationOptions = reactive({
  colleges: [],
  majors: [],
  grades: [],
  classes: [],
})

const filters = reactive({
  batchType: '',
  batchId: '',
  collegeId: '',
  majorId: '',
  gradeId: '',
  classId: '',
  applicationType: '',
  applicationStatus: '',
  feeItemId: '',
  applicationStartTime: '',
  applicationEndTime: '',
})

const availableColumns = [
  { key: 'applicationNo', title: '申请编号' },
  { key: 'studentNo', title: '学号' },
  { key: 'studentName', title: '姓名' },
  { key: 'collegeName', title: '学院' },
  { key: 'majorName', title: '专业' },
  { key: 'gradeName', title: '年级' },
  { key: 'className', title: '班级' },
  { key: 'applicationType', title: '申请类型' },
  { key: 'applicationStatus', title: '申请状态' },
  { key: 'arrearsItemNames', title: '欠费项目' },
  { key: 'declaredAmount', title: '欠费申报金额' },
  { key: 'confirmedAmount', title: '欠费确认金额' },
  { key: 'giftItemNames', title: '礼包物品' },
  { key: 'subsidyAmount', title: '补助金额' },
  { key: 'applicationTime', title: '申请时间' },
]

const selectedColumns = ref(availableColumns.slice(0, 10).map(item => item.key))

/**
 * 后端成功返回时严格采用后端 columns 顺序；尚无记录或接口依赖未合入时，
 * 只用已确定的字段白名单保留表格结构，不生成任何业务记录。
 */
const visibleColumns = computed(() => {
  if (report.value.columns?.length) return report.value.columns
  return availableColumns.filter(column => selectedColumns.value.includes(column.key))
})

const batchOptions = computed(() => summary.value?.batchHistoryStatistics ?? [])
const collegeOptions = computed(() => organizationOptions.colleges)
const gradeOptions = computed(() => organizationOptions.grades)

const giftApplicantCount = computed(() =>
  (summary.value?.giftItemApplicationCounts ?? []).reduce(
    (total, item) => total + Number(item.applicationCount ?? 0), 0,
  ),
)

const metricCards = computed(() => [
  { label: '申请人数统计', value: numberText(summary.value?.finalApplicantCount), color: '#1677ff' },
  { label: '实报人数统计', value: numberText(summary.value?.completedStudentCount), color: '#08ad5c' },
  { label: '欠费项目人数', value: numberText(summary.value?.feeItemApplicantCount), color: '#ff7a00' },
  { label: '欠费确认金额', value: moneyText(summary.value?.confirmedArrearsAmount), color: '#1677ff' },
  { label: '礼包申请人数', value: numberText(summary.value ? giftApplicantCount.value : undefined), color: '#08ad5c' },
])

const queryParams = computed(() => {
  const params = {}
  Object.entries(filters).forEach(([key, value]) => {
    if (value === '') return
    if (key === 'applicationStartTime') params[key] = `${value}T00:00:00`
    else if (key === 'applicationEndTime') params[key] = `${value}T23:59:59`
    else if (['batchId', 'collegeId', 'majorId', 'gradeId', 'classId', 'feeItemId'].includes(key)) {
      params[key] = Number(value)
    } else params[key] = value
  })
  return params
})

const reportParams = computed(() => ({
  ...queryParams.value,
  pageNo: currentPage.value,
  pageSize: pageSize.value,
  columns: selectedColumns.value.join(','),
  sortBy: 'applicationTime',
  sortDirection: 'DESC',
}))

function numberText(value) {
  return value === undefined || value === null ? '--' : Number(value).toLocaleString('zh-CN')
}

function moneyText(value) {
  return value === undefined || value === null
    ? '--'
    : `¥${Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`
}

function displayCell(value, key) {
  if (value === undefined || value === null || value === '') return '—'
  if (['declaredAmount', 'confirmedAmount', 'subsidyAmount'].includes(key)) return moneyText(value)
  return value
}

function updateBatch(value) {
  if (!value) {
    filters.batchType = ''
    filters.batchId = ''
    return
  }
  const [batchType, batchId] = value.split(':')
  filters.batchType = batchType
  filters.batchId = batchId
}

async function loadStatistics() {
  const response = await fetchApplicationStatistics(queryParams.value)
  summary.value = response.data.data
  await nextTick()
  renderCharts()
}

async function loadReport() {
  const response = await fetchStatisticsReport(reportParams.value)
  report.value = response.data.data
}

/**
 * 统计筛选直接复用成员一的组织结构接口，不从统计结果反推下拉选项。
 * 这样即使某个学院当前没有最终状态申请，也仍能作为合法筛选条件出现。
 */
async function loadOrganizationOptions() {
  const [collegeResponse, gradeResponse] = await Promise.all([
    collegeAPI.list(),
    gradeAPI.list(),
  ])
  organizationOptions.colleges = collegeResponse.data.data ?? []
  organizationOptions.grades = gradeResponse.data.data ?? []
}

async function loadMajorOptions() {
  filters.majorId = ''
  filters.classId = ''
  organizationOptions.classes = []
  if (!filters.collegeId) {
    organizationOptions.majors = []
    return
  }
  const response = await majorAPI.list(Number(filters.collegeId))
  organizationOptions.majors = response.data.data ?? []
}

async function loadClassOptions() {
  filters.classId = ''
  const response = await classAPI.list({
    collegeId: filters.collegeId || undefined,
    majorId: filters.majorId || undefined,
    gradeId: filters.gradeId || undefined,
  })
  organizationOptions.classes = response.data.data ?? []
}

async function queryDashboard() {
  loading.value = true
  currentPage.value = 1
  await Promise.all([loadStatistics(), loadReport()])
  loading.value = false
}

async function resetFilters() {
  Object.keys(filters).forEach(key => { filters[key] = '' })
  selectedBatchKey.value = ''
  currentPage.value = 1
  await queryDashboard()
}

function renderCharts() {
  if (!collegeChartRef.value || !reasonChartRef.value || !giftChartRef.value) return
  collegeChart ??= echarts.init(collegeChartRef.value)
  reasonChart ??= echarts.init(reasonChartRef.value)
  giftChart ??= echarts.init(giftChartRef.value)

  const colleges = summary.value?.collegeApplicantCounts ?? []
  collegeChart.setOption({
    animationDuration: 450,
    color: ['#1677ff'],
    grid: { left: 48, right: 24, top: 24, bottom: 48 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'category',
      data: colleges.map(item => item.collegeName),
      axisLabel: { color: '#515967', interval: 0, width: 130, overflow: 'truncate' },
      axisLine: { lineStyle: { color: '#dfe5ec' } },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: '#6e7683' },
      splitLine: { lineStyle: { color: '#edf1f5' } },
    },
    series: [{
      name: '申请人数',
      type: 'bar',
      barMaxWidth: 54,
      data: colleges.map((item, index) => ({
        value: item.applicantCount,
        itemStyle: { color: ['#1677ff', '#0bb55e', '#ff7a00', '#40a9ff'][index % 4] },
      })),
      label: { show: true, position: 'top', color: '#222' },
    }],
    graphic: colleges.length ? [] : [{
      type: 'text', left: 'center', top: 'middle', style: { text: '暂无统计数据', fill: '#9aa2ad', fontSize: 14 },
    }],
  }, true)

  const reasons = summary.value?.arrearsReasonStatistics ?? []
  const reasonPeople = reasons.map(item => ({
    name: item.arrearsReasonName,
    value: Number(item.applicantCount ?? 0),
  }))
  const reasonAmounts = reasons.map(item => ({
    name: item.arrearsReasonName,
    value: Number(item.confirmedAmount ?? 0),
  }))
  reasonChart.setOption({
    animationDuration: 450,
    color: ['#1677ff', '#0bb55e', '#ff8a00', '#40a9ff', '#8b5cf6'],
    tooltip: {
      trigger: 'item',
      formatter: ({ seriesName, name, value, percent }) => seriesName === '按人数'
        ? `${name}<br/>${value} 人（${percent}%）`
        : `${name}<br/>${moneyText(value)}（${percent}%）`,
    },
    legend: { orient: 'vertical', right: 8, top: 'middle', itemWidth: 10, itemHeight: 10 },
    series: [
      {
        name: '按人数',
        type: 'pie',
        radius: ['29%', '48%'],
        center: ['23%', '55%'],
        label: { show: true, position: 'center', formatter: '人数', color: '#5f6875' },
        data: reasonPeople,
      },
      {
        name: '按确认金额',
        type: 'pie',
        radius: ['29%', '48%'],
        center: ['55%', '55%'],
        label: { show: true, position: 'center', formatter: '金额', color: '#5f6875' },
        data: reasonAmounts,
      },
    ],
    graphic: reasons.length ? [] : [{
      type: 'text', left: 'center', top: 'middle', style: { text: '暂无统计数据', fill: '#9aa2ad', fontSize: 14 },
    }],
  }, true)

  const gifts = summary.value?.giftItemApplicationCounts ?? []
  giftChart.setOption({
    animationDuration: 450,
    color: ['#08ad5c'],
    grid: { left: 44, right: 18, top: 22, bottom: 45 },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'category',
      data: gifts.map(item => item.giftItemName),
      axisLabel: { color: '#515967', interval: 0, width: 90, overflow: 'truncate' },
      axisLine: { lineStyle: { color: '#dfe5ec' } },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: '#6e7683' },
      splitLine: { lineStyle: { color: '#edf1f5' } },
    },
    series: [{
      name: '申请数量',
      type: 'bar',
      barMaxWidth: 48,
      data: gifts.map(item => item.applicationCount),
      label: { show: true, position: 'top', color: '#222' },
    }],
    graphic: gifts.length ? [] : [{
      type: 'text', left: 'center', top: 'middle', style: { text: '暂无礼包申请数据', fill: '#9aa2ad', fontSize: 14 },
    }],
  }, true)
}

function resizeCharts() {
  collegeChart?.resize()
  reasonChart?.resize()
  giftChart?.resize()
}

function openColumnDialog() {
  columnDialogVisible.value = true
}

async function applyColumns() {
  columnDialogVisible.value = false
  currentPage.value = 1
  await loadReport()
}

async function downloadExcel() {
  const response = await exportStatisticsExcel(reportParams.value)
  const url = URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = url
  link.download = `绿色通道统计报表-${new Date().toISOString().slice(0, 10)}.xlsx`
  link.click()
  URL.revokeObjectURL(url)
}

async function printReport() {
  const response = await fetchStatisticsPrintData(reportParams.value)
  const printData = response.data.data
  const printWindow = window.open('', '_blank')
  const headers = printData.columns.map(column => `<th>${column.title}</th>`).join('')
  const rows = printData.records.map(record => `<tr>${printData.columns
    .map(column => `<td>${displayCell(record[column.key], column.key)}</td>`).join('')}</tr>`).join('')
  printWindow.document.write(`<!doctype html><html><head><title>${printData.title}</title><style>
    body{font-family:"Microsoft YaHei",sans-serif;padding:24px;color:#222}h1{text-align:center;font-size:22px}
    p{text-align:right;color:#666}table{width:100%;border-collapse:collapse;font-size:12px}th,td{border:1px solid #bbb;padding:7px;text-align:center}th{background:#eef4fa}
  </style></head><body><h1>${printData.title}</h1><p>生成时间：${printData.generatedAt}</p><table><thead><tr>${headers}</tr></thead><tbody>${rows}</tbody></table></body></html>`)
  printWindow.document.close()
  printWindow.print()
}

function changePage(target) {
  const maxPage = report.value.pages || 1
  currentPage.value = Math.min(Math.max(Number(target) || 1, 1), maxPage)
}

watch(selectedBatchKey, updateBatch)
watch(() => filters.collegeId, loadMajorOptions)
watch(
  [() => filters.collegeId, () => filters.majorId, () => filters.gradeId],
  loadClassOptions,
)
watch(currentPage, loadReport)
watch(pageSize, () => {
  currentPage.value = 1
  loadReport()
})

onMounted(async () => {
  await loadOrganizationOptions()
  await nextTick()
  renderCharts()
  window.addEventListener('resize', resizeCharts)
  loading.value = true
  await Promise.all([loadStatistics(), loadReport()])
  loading.value = false
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  collegeChart?.dispose()
  reasonChart?.dispose()
  giftChart?.dispose()
})
</script>

<template>
  <SchoolWorkspaceShell>
    <div class="statistics-page">
      <main class="page-content">
        <!-- 按页面 8 修正：页名区域不使用白色卡片、边框、圆角或阴影。 -->
        <section class="page-heading">
          <div><h1>统计看板页</h1><p>用于查看绿色通道申请统计、金额统计与明细报表，仅学校管理员可访问。</p></div>
          <div class="heading-actions">
            <button type="button" @click="batchSelectRef?.focus()">历史批次切换</button>
            <button type="button" @click="openColumnDialog">导出列选择</button>
            <button type="button" class="primary" @click="downloadExcel">Excel导出</button>
            <button type="button" @click="printReport">报表打印</button>
          </div>
        </section>

        <section class="filter-panel">
          <div class="filter-header"><h2>筛选条件</h2></div>
          <div class="filter-grid">
            <label><span>批次筛选</span><select ref="batchSelectRef" v-model="selectedBatchKey"><option value="">全部批次</option><option v-for="batch in batchOptions" :key="`${batch.batchType}:${batch.batchId}`" :value="`${batch.batchType}:${batch.batchId}`">{{ formatBatchLabel(batch) }}</option></select></label>
            <label><span>学院筛选</span><select v-model="filters.collegeId"><option value="">全部</option><option v-for="item in collegeOptions" :key="item.id" :value="item.id">{{ item.collegeName }}</option></select></label>
            <label><span>专业筛选</span><select v-model="filters.majorId" :disabled="!filters.collegeId"><option value="">全部</option><option v-for="item in organizationOptions.majors" :key="item.id" :value="item.id">{{ item.majorName }}</option></select></label>
            <label><span>年级筛选</span><select v-model="filters.gradeId"><option value="">全部</option><option v-for="item in gradeOptions" :key="item.id" :value="item.id">{{ item.gradeName }}</option></select></label>
            <label><span>班级筛选</span><select v-model="filters.classId"><option value="">全部</option><option v-for="item in organizationOptions.classes" :key="item.id" :value="item.id">{{ item.className }}</option></select></label>
            <label><span>申请类型筛选</span><select v-model="filters.applicationType"><option value="">全部</option><option value="GREEN_CHANNEL">绿色通道</option><option value="LIVING_SUBSIDY">生活补助</option><option value="TRAVEL_SUBSIDY">路费补助</option></select></label>
            <label><span>申请状态筛选</span><select v-model="filters.applicationStatus"><option value="">全部最终状态</option><option value="APPROVED">审核通过</option><option value="CONFIRM_PENDING">待欠费确认</option><option value="COMPLETED">已完成</option></select></label>
            <label><span>欠费项目筛选</span><input v-model="filters.feeItemId" type="number" min="1" placeholder="全部" /></label>
            <label class="date-filter"><span>申请时间筛选</span><div><input v-model="filters.applicationStartTime" type="date" /><i>~</i><input v-model="filters.applicationEndTime" type="date" /></div></label>
          </div>
          <div class="filter-actions"><button type="button" class="primary" :disabled="loading" @click="queryDashboard">查询</button><button type="button" :disabled="loading" @click="resetFilters">重置</button></div>
        </section>

        <section class="metric-grid">
          <article v-for="metric in metricCards" :key="metric.label"><span>{{ metric.label }}</span><strong :style="{ color: metric.color }">{{ metric.value }}</strong><small>当前筛选结果</small></article>
        </section>

        <section class="chart-grid">
          <article><h2>各学院申请人数统计</h2><div ref="collegeChartRef" class="chart"></div></article>
          <article><h2>欠费原因人数与金额占比</h2><div ref="reasonChartRef" class="chart"></div></article>
          <article><h2>爱心礼包物品申请数量</h2><div ref="giftChartRef" class="chart"></div></article>
        </section>

        <section class="detail-panel">
          <header><h2>统计明细表</h2><div><span>当前列方案：学校统计默认方案</span><span>支持按列排序 ⓘ</span></div></header>
          <div class="table-wrap">
            <table>
              <thead><tr><th v-for="column in visibleColumns" :key="column.key">{{ column.title }} ↕</th></tr></thead>
              <tbody>
                <tr v-for="(record, index) in report.records" :key="record.applicationId ?? record.applicationNo ?? index"><td v-for="column in visibleColumns" :key="column.key">{{ displayCell(record[column.key], column.key) }}</td></tr>
                <tr v-if="!loading && report.records.length === 0"><td :colspan="Math.max(visibleColumns.length, 1)" class="empty">暂无符合条件的统计明细</td></tr>
              </tbody>
            </table>
          </div>
          <footer class="pager"><span>共 {{ report.total }} 条</span><select v-model.number="pageSize"><option :value="10">10条/页</option><option :value="20">20条/页</option><option :value="50">50条/页</option></select><button :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">‹</button><button class="current">{{ currentPage }}</button><button :disabled="currentPage >= (report.pages || 1)" @click="changePage(currentPage + 1)">›</button><span>前往</span><input :value="currentPage" type="number" min="1" :max="report.pages || 1" @change="changePage($event.target.value)" /><span>页</span></footer>
        </section>
      </main>

    <div v-if="columnDialogVisible" class="dialog-mask" @click.self="columnDialogVisible = false">
      <section class="column-dialog"><header><h2>选择导出列</h2><button @click="columnDialogVisible = false">×</button></header><div class="column-options"><label v-for="column in availableColumns" :key="column.key"><input v-model="selectedColumns" type="checkbox" :value="column.key" />{{ column.title }}</label></div><footer><button @click="columnDialogVisible = false">取消</button><button class="primary" @click="applyColumns">应用</button></footer></section>
    </div>
    </div>
  </SchoolWorkspaceShell>
</template>

<style scoped>
:global(*) { box-sizing: border-box; }
:global(html) { background: #f4f7fb; color-scheme: light; }
:global(body) { min-width: 1280px; margin: 0; background: #f4f7fb; color: #202733; font-family: "Microsoft YaHei", "PingFang SC", Arial, sans-serif; }
:global(button), :global(input), :global(select) { font: inherit; }
:global(#app) { width: 100%; max-width: none; min-height: 100vh; margin: 0; text-align: initial; }
.statistics-page { min-height: calc(100vh - 60px); margin: -24px; padding: 6px 18px 24px; background: #f4f7fb; }
.sidebar { position: fixed; inset: 0 auto 0 0; z-index: 20; width: 242px; overflow-y: auto; color: #fff; background: linear-gradient(180deg, #074d83, #023966); }
.brand { display: flex; align-items: center; gap: 10px; height: 72px; padding: 0 14px; border-bottom: 1px solid rgba(255,255,255,.12); font-size: 18px; font-weight: 700; white-space: nowrap; }
.brand-mark { display: grid; width: 42px; height: 48px; place-items: center; border: 2px solid #fff; border-radius: 9px 9px 16px 16px; }
.side-nav { display: grid; gap: 3px; padding: 17px 4px 24px; }
.side-item { display: flex; align-items: center; width: 100%; height: 58px; padding: 0 22px; border: 0; border-radius: 4px; color: #fff; background: transparent; cursor: pointer; font-size: 16px; text-align: left; }
.side-item.active { background: linear-gradient(90deg, #0b82f5, #0873e5); box-shadow: 0 6px 18px rgba(0,49,113,.28); }
.side-icon { display: inline-grid; width: 27px; margin-right: 10px; place-items: center; font-size: 20px; }
.workspace { width: calc(100% - 242px); min-height: 100vh; margin-left: 242px; }
.topbar { display: flex; align-items: center; justify-content: space-between; height: 66px; padding: 0 28px; border-bottom: 1px solid #dce2e9; background: #fff; }
.breadcrumb, .account-area { display: flex; align-items: center; gap: 15px; }
.menu-button { margin-right: 9px; border: 0; background: transparent; cursor: pointer; font-size: 24px; }
.account-area .notice { position: relative; margin-right: 7px; font-size: 25px; }.notice b { position: absolute; top: -6px; right: -12px; padding: 2px 6px; border-radius: 10px; color: #fff; background: #ff4d4f; font-size: 10px; }
.avatar { display: grid; width: 40px; height: 40px; place-items: center; border-radius: 50%; color: #fff; background: #2787ed; }.account-area div { display: grid; gap: 3px; }.account-area small { color: #666; }
.page-content { padding: 18px 18px 24px; }
.page-heading { display: flex; align-items: center; justify-content: space-between; min-height: 67px; margin-bottom: 12px; padding: 0 18px; background: transparent; }
.page-heading h1 { margin: 0 0 7px; color: #131722; font-size: 25px; line-height: 1; }.page-heading p { margin: 0; color: #6f7680; font-size: 13px; }
.heading-actions { display: flex; gap: 13px; }.heading-actions button, .filter-actions button { height: 36px; padding: 0 18px; border: 1px solid #d3d9e1; border-radius: 4px; background: #fff; cursor: pointer; }.heading-actions .primary, .filter-actions .primary, .column-dialog .primary { border-color: #1677ff; color: #fff; background: #1677ff; }
.filter-panel { position: relative; padding: 12px 18px 10px; border: 1px solid #dce2e9; border-radius: 4px; background: #fff; }
.filter-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }.filter-header h2, .chart-grid h2, .detail-panel h2 { margin: 0; font-size: 16px; }.filter-header select { width: 180px; }
.filter-grid { display: grid; grid-template-columns: repeat(5, minmax(180px, 1fr)); gap: 12px 28px; padding-right: 0; }.filter-grid label { display: grid; grid-template-columns: auto minmax(100px, 1fr); align-items: center; gap: 10px; white-space: nowrap; font-size: 13px; }
.filter-grid input, .filter-grid select, .filter-header select { width: 100%; height: 34px; padding: 0 10px; border: 1px solid #d6dce5; border-radius: 3px; color: #454c56; background: #fff; }
.date-filter { grid-column: span 2; }.date-filter div { display: flex; align-items: center; gap: 7px; }.date-filter input { min-width: 120px; }.date-filter i { color: #888; font-style: normal; }
.filter-actions { display: flex; justify-content: flex-end; gap: 14px; margin-top: 8px; }.filter-actions button { min-width: 80px; height: 32px; }.filter-actions button:disabled { opacity: .55; }
.metric-grid { display: grid; grid-template-columns: repeat(5, minmax(150px, 1fr)); gap: 7px; margin: 7px 0; }.metric-grid article { display: grid; min-height: 104px; place-items: center; padding: 13px 7px; border: 1px solid #dce2e9; border-radius: 3px; background: #fff; text-align: center; }.metric-grid span { font-size: 13px; font-weight: 700; }.metric-grid strong { font-size: 25px; line-height: 1; }.metric-grid small { color: #6d7480; }
.chart-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 7px; }.chart-grid article { height: 190px; padding: 11px 16px 5px; border: 1px solid #dce2e9; border-radius: 3px; background: #fff; }.chart { width: 100%; height: 155px; }
.detail-panel { min-height: 246px; margin-top: 7px; border: 1px solid #dce2e9; border-radius: 3px; background: #fff; }.detail-panel > header { display: flex; align-items: center; justify-content: space-between; height: 42px; padding: 0 16px; }.detail-panel header div { display: flex; gap: 28px; color: #444; font-size: 12px; }
.table-wrap { width: 100%; overflow-x: auto; padding: 0 10px; }table { width: 100%; min-width: 1200px; border-collapse: collapse; table-layout: auto; font-size: 11px; }th, td { height: 31px; padding: 5px 8px; border: 1px solid #dce3eb; text-align: center; white-space: nowrap; }th { color: #202733; background: #eef5fc; }.empty { height: 78px; color: #9098a3; }
.pager { display: flex; align-items: center; justify-content: center; gap: 9px; min-height: 54px; font-size: 12px; }.pager select, .pager input, .pager button { height: 32px; border: 1px solid #d7dde5; border-radius: 4px; background: #fff; }.pager select { padding: 0 8px; }.pager input { width: 48px; text-align: center; }.pager button { min-width: 32px; cursor: pointer; }.pager .current { border-color: #1677ff; color: #fff; background: #1677ff; }
.dialog-mask { position: fixed; inset: 0; z-index: 80; display: grid; place-items: center; background: rgba(20,31,45,.38); }.column-dialog { width: 620px; border-radius: 6px; background: #fff; box-shadow: 0 16px 45px rgba(0,0,0,.2); }.column-dialog header, .column-dialog footer { display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; border-bottom: 1px solid #e3e7ed; }.column-dialog h2 { margin: 0; font-size: 19px; }.column-dialog header button { border: 0; background: transparent; font-size: 25px; cursor: pointer; }.column-options { display: grid; grid-template-columns: repeat(3, 1fr); gap: 15px; padding: 24px 20px; }.column-options label { display: flex; gap: 8px; align-items: center; }.column-dialog footer { justify-content: flex-end; gap: 12px; border-top: 1px solid #e3e7ed; border-bottom: 0; }.column-dialog footer button { min-width: 78px; height: 34px; border: 1px solid #d3d9e1; border-radius: 4px; background: #fff; cursor: pointer; }
@media (max-width: 1450px) { .filter-grid { grid-template-columns: repeat(4, minmax(180px, 1fr)); }.metric-grid { grid-template-columns: repeat(4, 1fr); }.sidebar { width: 220px; }.workspace { width: calc(100% - 220px); margin-left: 220px; } }
</style>
