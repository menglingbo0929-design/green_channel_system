<script setup>
import { onMounted, reactive, ref } from 'vue'
import {
  confirmArrears,
  fetchPendingArrears,
  fetchPendingArrearsDetail,
} from '../../../api/confirmation'
import BusinessConfirmDialog from '../../../components/school/BusinessConfirmDialog.vue'
import { useUserStore } from '../../../stores/user'

/**
 * 这是确认模块的联调示意页，不是最终视觉稿。
 *
 * 它只验证三个真实接口：待确认列表、待确认详情、提交最终金额。最终路由、权限、
 * Element Plus 组件和统一配色会在前端整体改版时集中处理。
 */
const query = reactive({
  applicationNo: '',
  studentNo: '',
  studentName: '',
  pageNo: 1,
  pageSize: 10,
})

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const message = ref('')
const selected = ref(null)
const confirmDialogOpen = ref(false)
const confirmSubmitting = ref(false)
const userStore = useUserStore()

function newRequestId() {
  return globalThis.crypto?.randomUUID?.()
    ?? `confirm-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

/**
 * 列表只请求后端，不在前端伪造待确认记录。
 * 成员二读取 Port 合入后直接展示真实待确认记录。
 */
async function loadList() {
  loading.value = true
  message.value = ''
  const response = await fetchPendingArrears(query)
  const page = response.data.data
  rows.value = page.records ?? []
  total.value = page.total ?? 0
  loading.value = false
}

/**
 * 确认前重新读取详情，确保金额和 version 均是最新值。
 */
async function openConfirmation(row) {
  message.value = ''
  const response = await fetchPendingArrearsDetail(row.applicationId)
  selected.value = response.data.data
  confirmDialogOpen.value = true
}

/**
 * 提交时携带 UUID requestId 与详情返回的 version。
 * 页面直接提交后端所需的确认信息。
 */
async function submitConfirmation(dialogForm) {
  message.value = ''
  confirmSubmitting.value = true
  const response = await confirmArrears(
    selected.value.applicationId,
    {
      confirmedAmount: dialogForm.confirmedAmount,
      version: selected.value.version,
      requestId: newRequestId(),
    },
    userStore.userId,
  )
  message.value = `确认成功，单据号：${response.data.data.voucherNo}`
  confirmSubmitting.value = false
  confirmDialogOpen.value = false
  selected.value = null
  await loadList()
}

onMounted(loadList)
</script>

<template>
  <main class="confirmation-demo">
    <header>
      <p class="eyebrow">成员四 · 联调示意页</p>
      <h1>欠费待确认</h1>
      <p>仅验证待确认查询、详情读取和最终金额确认；不是最终 UI。</p>
    </header>

    <section class="search-panel">
      <label>申请编号<input v-model.trim="query.applicationNo" placeholder="模糊查询" /></label>
      <label>学号<input v-model.trim="query.studentNo" placeholder="模糊查询" /></label>
      <label>姓名<input v-model.trim="query.studentName" placeholder="模糊查询" /></label>
      <button type="button" :disabled="loading" @click="loadList">
        {{ loading ? '查询中…' : '查询待确认申请' }}
      </button>
    </section>

    <p v-if="message" class="notice success">{{ message }}</p>

    <section class="table-panel">
      <p>共 {{ total }} 条待确认记录</p>
      <table>
        <thead><tr><th>申请编号</th><th>学生</th><th>学院</th><th>申报金额</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="row in rows" :key="row.applicationId">
            <td>{{ row.applicationNo }}</td>
            <td>{{ row.studentNo }} / {{ row.studentName }}</td>
            <td>{{ row.collegeName }}</td>
            <td>¥ {{ row.appliedAmount }}</td>
            <td><button type="button" @click="openConfirmation(row)">查看并确认</button></td>
          </tr>
          <tr v-if="!loading && rows.length === 0"><td colspan="5">暂无待确认数据</td></tr>
        </tbody>
      </table>
    </section>

    <!-- 页面 8 的欠费确认使用成员四业务确认弹窗，不与三级审核弹窗混用。 -->
    <BusinessConfirmDialog
      v-model="confirmDialogOpen"
      mode="ARREARS_CONFIRM"
      :business="selected ?? {}"
      :submitting="confirmSubmitting"
      @confirm="submitConfirmation"
    />
  </main>
</template>

<style scoped>
.confirmation-demo { max-width: 1080px; margin: 0 auto; padding: 40px 24px; color: #233044; text-align: left; }
.eyebrow { color: #2f7d57; font-weight: 700; }
h1 { margin: 4px 0; } .search-panel { display: flex; gap: 14px; align-items: end; flex-wrap: wrap; padding: 18px; margin: 24px 0; background: #f4f8f5; border-radius: 10px; }
label { display: grid; gap: 6px; font-size: 14px; font-weight: 600; } input { min-width: 150px; padding: 8px 10px; border: 1px solid #b9c7be; border-radius: 6px; }
button { padding: 9px 14px; border: 0; border-radius: 6px; color: #fff; background: #287a55; cursor: pointer; } button:disabled { cursor: not-allowed; opacity: .55; } .secondary { background: #68747b; }
.table-panel { overflow-x: auto; } table { width: 100%; border-collapse: collapse; } th, td { padding: 12px; border-bottom: 1px solid #dbe3de; } th { background: #eef5f0; } .notice { padding: 10px 12px; border-radius: 6px; } .success { color: #17663e; background: #e5f7eb; } .actions { display: flex; gap: 8px; }
</style>
