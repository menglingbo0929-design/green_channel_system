<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { fetchSupplementDetail, fetchSupplementHistory } from '../../../api/supplement'

const props = defineProps({
  refreshKey: { type: Number, default: 0 },
})

const filter = reactive({ studentNo: '', applicationType: '', batchId: '', status: '' })
const pageNo = ref(1)
const pageSize = ref(10)
const page = ref({ records: [], total: 0, pages: 0 })
const loading = ref(false)
const detail = ref(null)
const detailVisible = ref(false)

/** 只发送有值的筛选条件，并把 batchId 转换成后端 Long 所需的数值。 */
const requestParams = computed(() => {
  const params = { pageNo: pageNo.value, pageSize: pageSize.value }
  for (const [key, value] of Object.entries(filter)) {
    if (value === '') continue
    params[key] = key === 'batchId' ? Number(value) : value
  }
  return params
})

async function loadHistory() {
  loading.value = true
  const response = await fetchSupplementHistory(requestParams.value)
  page.value = response.data.data
  loading.value = false
}

function searchFirstPage() {
  pageNo.value = 1
  loadHistory()
}

function previousPage() {
  if (pageNo.value <= 1) return
  pageNo.value -= 1
  loadHistory()
}

function nextPage() {
  if (pageNo.value >= (page.value.pages || 1)) return
  pageNo.value += 1
  loadHistory()
}

async function openDetail(applicationId) {
  detail.value = (await fetchSupplementDetail(applicationId)).data.data
  detailVisible.value = true
}

// 创建成功后父页面增加 refreshKey，历史表自动回到第一页重新查询。
watch(() => props.refreshKey, searchFirstPage)
</script>

<template>
  <section class="history-panel">
    <header><h3>补录历史</h3><span>共 {{ page.total }} 条</span></header>

    <div class="filters">
      <label>学号<input v-model.trim="filter.studentNo" placeholder="精确学号" /></label>
      <label>申请类型
        <select v-model="filter.applicationType">
          <option value="">全部</option>
          <option value="GREEN_CHANNEL">绿色通道</option>
          <option value="LIVING_SUBSIDY">生活补助</option>
          <option value="TRAVEL_SUBSIDY">路费补助</option>
        </select>
      </label>
      <label>批次 ID<input v-model="filter.batchId" type="number" min="1" /></label>
      <label>状态
        <select v-model="filter.status">
          <option value="">全部</option>
          <option value="CONFIRM_PENDING">待欠费确认</option>
          <option value="COMPLETED">已完成</option>
        </select>
      </label>
      <button type="button" :disabled="loading" @click="searchFirstPage">
        {{ loading ? '查询中…' : '查询历史' }}
      </button>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr><th>申请编号</th><th>学生</th><th>类型</th><th>批次</th><th>状态</th><th>线下办理时间</th><th>补录原因</th><th>操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="item in page.records" :key="item.applicationId">
            <td>{{ item.applicationNo }}</td>
            <td>{{ item.studentName }}<br /><small>{{ item.studentNo }}</small></td>
            <td>{{ item.applicationType }}</td>
            <td>{{ item.batchType }} / {{ item.batchId }}</td>
            <td>{{ item.status }}</td>
            <td>{{ item.supplementedAt }}</td>
            <td>{{ item.supplementReason }}</td>
            <td><button type="button" class="detail-button" @click="openDetail(item.applicationId)">详情</button></td>
          </tr>
          <tr v-if="!loading && page.records.length === 0"><td colspan="8" class="empty">暂无补录记录</td></tr>
        </tbody>
      </table>
    </div>

    <footer class="pager">
      <span>共 {{ page.total }} 条，第 {{ pageNo }} / {{ page.pages || 1 }} 页</span>
      <button type="button" :disabled="pageNo <= 1 || loading" @click="previousPage">上一页</button>
      <button type="button" :disabled="pageNo >= (page.pages || 1) || loading" @click="nextPage">下一页</button>
    </footer>

    <el-dialog v-model="detailVisible" title="补录详情" width="620px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="申请编号">{{ detail.applicationNo }}</el-descriptions-item>
        <el-descriptions-item label="申请状态">{{ detail.status }}</el-descriptions-item>
        <el-descriptions-item label="学生">{{ detail.studentName }}（{{ detail.studentNo }}）</el-descriptions-item>
        <el-descriptions-item label="申请类型">{{ detail.applicationType }}</el-descriptions-item>
        <el-descriptions-item label="批次">{{ detail.batchType }} / {{ detail.batchId }}</el-descriptions-item>
        <el-descriptions-item label="线下办理时间">{{ detail.supplementedAt }}</el-descriptions-item>
        <el-descriptions-item label="补录原因" :span="2">{{ detail.supplementReason }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </section>
</template>

<style scoped>
.history-panel { margin-top: 22px; border: 1px solid #ebeef5; border-radius: 4px; background: #fff; }.history-panel > header { display: flex; align-items: center; justify-content: space-between; height: 54px; padding: 0 18px; border-bottom: 1px solid #ebeef5; }.history-panel h3 { margin: 0; font-size: 16px; }.history-panel header span { color: #909399; font-size: 13px; }.filters { display: flex; flex-wrap: wrap; gap: 14px; align-items: end; padding: 18px; background: #fafcff; }label { display: grid; gap: 7px; color: #606266; font-size: 13px; }input, select { min-width: 145px; height: 34px; padding: 0 10px; border: 1px solid #dcdfe6; border-radius: 4px; outline: none; }button { height: 34px; padding: 0 14px; border: 1px solid #409eff; border-radius: 4px; color: #fff; background: #409eff; cursor: pointer; }button:disabled { opacity: .5; cursor: not-allowed; }.table-wrap { overflow-x: auto; }table { width: 100%; min-width: 900px; border-collapse: collapse; }th, td { padding: 10px 14px; border-bottom: 1px solid #ebeef5; text-align: left; white-space: nowrap; font-size: 13px; }th { color: #606266; background: #f5f7fa; }.empty { height: 78px; color: #909399; text-align: center; }.pager { display: flex; gap: 8px; align-items: center; justify-content: flex-end; padding: 14px 18px; }.pager span { margin-right: auto; color: #909399; font-size: 13px; }.pager button { min-width: 70px; color: #606266; background: #fff; border-color: #dcdfe6; }.detail-button { height: auto; min-width: auto; padding: 0; border: 0; color: #409eff; background: transparent; }
</style>
