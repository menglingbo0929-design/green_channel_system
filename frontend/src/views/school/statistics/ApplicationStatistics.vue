<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { fetchApplicationStatistics } from '../../../api/statistics'

/**
 * 6.1.5、6.1.6 的统计联调示意页。
 *
 * 不预置任何统计数字：成员一权限 Service 与成员二真实聚合 Service 合入后展示真实数据。
 */
const filter = reactive({
  batchType: '', batchId: '', collegeId: '', majorId: '', gradeId: '', classId: '',
  applicationType: '', applicationStatus: '', feeItemId: '',
  applicationStartTime: '', applicationEndTime: '',
})
const data = ref(null)
const loading = ref(false)

/** 将空条件移除，并把所有组织/项目/批次 ID 转为数值，避免把空字符串作为真实筛选条件。 */
const requestParams = computed(() => {
  const params = {}
  for (const [key, value] of Object.entries(filter)) {
    if (value === '') continue
    params[key] = ['batchId', 'collegeId', 'majorId', 'gradeId', 'classId', 'feeItemId'].includes(key)
      ? Number(value) : value
  }
  return params
})

async function loadStatistics() {
  loading.value = true
  const response = await fetchApplicationStatistics(requestParams.value)
  data.value = response.data.data
  loading.value = false
}

onMounted(loadStatistics)
</script>

<template>
  <main class="statistics-demo">
    <header>
      <p class="eyebrow">成员四 · 统计联调示意页</p>
      <h1>申请统计</h1>
      <p>只统计最终审核通过（APPROVED）或已完成（COMPLETED）的真实数据。</p>
    </header>

    <p class="dependency-note">当前须同时具备成员一学校权限与成员二集合统计服务，演示前须完成合入；页面不展示模拟统计数字。</p>

    <section class="filter-panel">
      <label>批次体系<select v-model="filter.batchType"><option value="">全部</option><option value="GREEN_CHANNEL">绿色通道</option><option value="SUBSIDY">补助</option></select></label>
      <label>批次 ID<input v-model="filter.batchId" type="number" min="1" /></label>
      <label>学院 ID<input v-model="filter.collegeId" type="number" min="1" /></label>
      <label>专业 ID<input v-model="filter.majorId" type="number" min="1" /></label>
      <label>年级 ID<input v-model="filter.gradeId" type="number" min="1" /></label>
      <label>班级 ID<input v-model="filter.classId" type="number" min="1" /></label>
      <label>申请类型<select v-model="filter.applicationType"><option value="">全部</option><option value="GREEN_CHANNEL">绿色通道</option><option value="LIVING_SUBSIDY">生活补助</option><option value="TRAVEL_SUBSIDY">路费补助</option></select></label>
      <label>最终状态<select v-model="filter.applicationStatus"><option value="">APPROVED + COMPLETED</option><option value="APPROVED">APPROVED</option><option value="COMPLETED">COMPLETED</option></select></label>
      <label>欠费项目 ID<input v-model="filter.feeItemId" type="number" min="1" /></label>
      <label>申请开始时间<input v-model="filter.applicationStartTime" type="datetime-local" /></label>
      <label>申请结束时间<input v-model="filter.applicationEndTime" type="datetime-local" /></label>
      <button type="button" :disabled="loading" @click="loadStatistics">{{ loading ? '统计中…' : '按条件统计' }}</button>
    </section>

    <template v-if="data">
      <section class="summary-grid">
        <article><span>申请总人数</span><strong>{{ data.finalApplicantCount }}</strong></article>
        <article><span>实报人数（COMPLETED）</span><strong>{{ data.completedStudentCount }}</strong></article>
        <article><span>欠费项目人数</span><strong>{{ data.feeItemApplicantCount }}</strong></article>
        <article><span>欠费总金额</span><strong>¥ {{ data.confirmedArrearsAmount }}</strong></article>
      </section>

      <section class="tables">
        <article><h2>各学院申请人数</h2><table><thead><tr><th>学院</th><th>人数</th></tr></thead><tbody><tr v-for="item in data.collegeApplicantCounts" :key="item.collegeId"><td>{{ item.collegeName }}</td><td>{{ item.applicantCount }}</td></tr></tbody></table></article>
        <article><h2>各年级申请人数</h2><table><thead><tr><th>年级</th><th>人数</th></tr></thead><tbody><tr v-for="item in data.gradeApplicantCounts" :key="item.gradeId"><td>{{ item.gradeName }}</td><td>{{ item.applicantCount }}</td></tr></tbody></table></article>
        <article><h2>欠费原因统计</h2><table><thead><tr><th>原因</th><th>人数</th><th>确认金额</th></tr></thead><tbody><tr v-for="item in data.arrearsReasonStatistics" :key="item.arrearsReasonCode"><td>{{ item.arrearsReasonName }}</td><td>{{ item.applicantCount }}</td><td>¥ {{ item.confirmedAmount }}</td></tr></tbody></table></article>
        <article><h2>爱心礼包物品申请数量</h2><table><thead><tr><th>物品</th><th>申请数</th></tr></thead><tbody><tr v-for="item in data.giftItemApplicationCounts" :key="item.giftItemId"><td>{{ item.giftItemName }}</td><td>{{ item.applicationCount }}</td></tr></tbody></table></article>
        <article><h2>历史批次数据</h2><table><thead><tr><th>批次</th><th>申请人数</th><th>实报人数</th><th>欠费总金额</th></tr></thead><tbody><tr v-for="item in data.batchHistoryStatistics" :key="item.batchType + item.batchId"><td>{{ item.batchName }}</td><td>{{ item.applicantCount }}</td><td>{{ item.completedStudentCount }}</td><td>¥ {{ item.confirmedArrearsAmount }}</td></tr></tbody></table></article>
      </section>
    </template>
  </main>
</template>

<style scoped>
.statistics-demo { max-width: 1180px; margin: 0 auto; padding: 40px 24px; color: #233044; text-align: left; }
.eyebrow { color: #2f7d57; font-weight: 700; } h1 { margin: 4px 0; }
.dependency-note { padding: 10px; border-left: 4px solid #d97706; background: #fffbeb; color: #92400e; }
.filter-panel { display: flex; gap: 12px; align-items: end; flex-wrap: wrap; padding: 18px; margin: 24px 0; background: #f4f8f5; border-radius: 10px; }
label { display: grid; gap: 6px; font-size: 13px; font-weight: 600; } input, select { min-width: 130px; padding: 8px; border: 1px solid #b9c7be; border-radius: 6px; }
button { padding: 9px 14px; border: 0; border-radius: 6px; color: #fff; background: #287a55; cursor: pointer; } button:disabled { opacity: .55; cursor: not-allowed; }
.summary-grid { display: grid; grid-template-columns: repeat(4, minmax(150px, 1fr)); gap: 14px; margin: 24px 0; }.summary-grid article { padding: 18px; border-radius: 10px; background: #eef5f0; }.summary-grid span { display: block; font-size: 13px; }.summary-grid strong { display: block; margin-top: 8px; font-size: 26px; }
.tables { display: grid; grid-template-columns: repeat(auto-fit, minmax(360px, 1fr)); gap: 16px; }.tables article { overflow-x: auto; padding: 16px; border: 1px solid #dbe3de; border-radius: 8px; }.tables h2 { margin-top: 0; font-size: 17px; } table { width: 100%; border-collapse: collapse; } th, td { padding: 9px; border-bottom: 1px solid #dbe3de; text-align: left; } th { background: #eef5f0; }
@media (max-width: 760px) { .summary-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
