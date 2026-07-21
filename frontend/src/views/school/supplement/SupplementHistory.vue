<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { fetchSupplementHistory } from '../../../api/supplement'

/**
 * 6.1.4 补录历史子页面。
 *
 * 不内置任何模拟记录；成员二查询适配合入后直接展示真实补录记录。
 */
const props = defineProps({
  userId: { type: [String, Number], required: true },
  refreshKey: { type: Number, default: 0 },
})

const filter = reactive({ studentNo: '', applicationType: '', batchId: '', status: '' })
const pageNo = ref(1)
const pageSize = ref(10)
const page = ref({ records: [], total: 0, pages: 0 })
const loading = ref(false)

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
  const response = await fetchSupplementHistory(requestParams.value, props.userId)
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

// 创建成功后父页面增加 refreshKey，历史表自动回到第一页重新查询。
watch(() => props.refreshKey, searchFirstPage)
</script>

<template>
  <section class="history-panel">
    <header>
      <h2>补录历史</h2>
      <p>仅查询来源为 SUPPLEMENT 的真实申请。</p>
    </header>

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
          <tr><th>申请编号</th><th>学生</th><th>类型</th><th>批次</th><th>状态</th><th>线下办理时间</th><th>补录原因</th></tr>
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
          </tr>
          <tr v-if="!loading && page.records.length === 0">
            <td colspan="7" class="empty">没有可展示的真实补录记录</td>
          </tr>
        </tbody>
      </table>
    </div>

    <footer class="pager">
      <span>共 {{ page.total }} 条，第 {{ pageNo }} / {{ page.pages || 1 }} 页</span>
      <button type="button" :disabled="pageNo <= 1 || loading" @click="previousPage">上一页</button>
      <button type="button" :disabled="pageNo >= (page.pages || 1) || loading" @click="nextPage">下一页</button>
    </footer>
  </section>
</template>

<style scoped>
.history-panel { margin-top: 28px; padding: 20px; border: 1px solid #dbe3de; border-radius: 10px; background: #fff; }
.history-panel h2 { margin: 0 0 4px; }.history-panel header p { margin-top: 0; color: #647168; }
.filters { display: flex; flex-wrap: wrap; gap: 12px; align-items: end; margin: 18px 0; }
label { display: grid; gap: 6px; font-size: 13px; font-weight: 600; }
input, select { min-width: 145px; padding: 8px; border: 1px solid #b9c7be; border-radius: 6px; }
button { padding: 9px 13px; border: 0; border-radius: 6px; color: #fff; background: #287a55; cursor: pointer; }
button:disabled { opacity: .5; cursor: not-allowed; }
.table-wrap { overflow-x: auto; }table { width: 100%; border-collapse: collapse; }th, td { padding: 10px; border-bottom: 1px solid #e2e8e4; text-align: left; white-space: nowrap; }th { background: #eef5f0; }.empty { padding: 24px; color: #7a857e; text-align: center; }
.pager { display: flex; gap: 8px; align-items: center; justify-content: flex-end; margin-top: 14px; }.pager span { margin-right: auto; }
</style>
