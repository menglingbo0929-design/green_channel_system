<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AllocationDialog from './AllocationDialog.vue'
import { batchAPI, resourceConfigAPI } from '../../api/application.js'

const batches = ref([]); const colleges = ref([]); const grades = ref([]); const batchId = ref(null)
const quotas = ref([]); const loading = ref(false); const dialogOpen = ref(false); const editing = ref(null)
const selectedBatch = computed(() => batches.value.find(item => item.id === batchId.value))
function errorMessage(error) { return error.response?.data?.message || error.message || '加载失败' }
function label(batch) { return `${batch.batchCode}｜${batch.batchName}` }
function typeName(type) { return type === 'LIVING_SUBSIDY' ? '生活补助' : '路费补助' }
function remaining(row) { return Number(row.quotaAmount) - Number(row.reservedAmount) - Number(row.usedAmount) }
async function load() {
  loading.value = true
  try {
    const [batchData, collegeData, gradeData] = await Promise.all([batchAPI.listSubsidy(), resourceConfigAPI.colleges(), resourceConfigAPI.grades()])
    batches.value = batchData || []; colleges.value = collegeData || []; grades.value = gradeData || []
    if (!batchId.value && batches.value.length) batchId.value = batches.value[0].id
    await loadQuotas()
  } catch (error) { ElMessage.error(errorMessage(error)) } finally { loading.value = false }
}
async function loadQuotas() {
  if (!batchId.value) return
  loading.value = true
  try {
    const [collegeRows, gradeRows] = await Promise.all([resourceConfigAPI.subsidyQuotas(batchId.value, 'COLLEGE'), resourceConfigAPI.subsidyQuotas(batchId.value, 'GRADE')])
    quotas.value = [...(collegeRows || []), ...(gradeRows || [])].map(row => ({ ...row, resource: 'SUBSIDY' }))
  } catch (error) { quotas.value = []; ElMessage.error(errorMessage(error)) } finally { loading.value = false }
}
async function remove(row) {
  try { await resourceConfigAPI.deleteSubsidyQuota(row.id, row.scope); ElMessage.success('补助额度配置已删除'); await loadQuotas() }
  catch (error) { ElMessage.error(errorMessage(error)) }
}
function open(row = null) { editing.value = row; dialogOpen.value = true }
onMounted(load)
</script>

<template>
  <section class="quota-section"><h3>补助额度配置</h3><p>每个生活补助、路费补助批次都需要分别为学院和年级配置额度，学生提交申请时会即时预占该额度。</p><div class="toolbar"><el-select v-model="batchId" style="width:360px" @change="loadQuotas"><el-option v-for="batch in batches" :key="batch.id" :label="`${typeName(batch.batchType)}｜${label(batch)}`" :value="batch.id"/></el-select><el-button @click="loadQuotas">加载额度</el-button><el-button type="primary" :disabled="!batchId" @click="open()">新增额度分配</el-button></div><el-table v-loading="loading" :data="quotas" border class="standard-table" empty-text="该补助批次暂未配置额度"><el-table-column label="补助类型" width="110"><template #default="{row}">{{ typeName(selectedBatch?.batchType) }}</template></el-table-column><el-table-column label="维度" width="90"><template #default="{row}">{{ row.scope === 'COLLEGE' ? '学院' : '年级' }}</template></el-table-column><el-table-column prop="targetName" label="对象"/><el-table-column label="总额度"><template #default="{row}">¥{{ Number(row.quotaAmount).toFixed(2) }}</template></el-table-column><el-table-column label="预占"><template #default="{row}">¥{{ Number(row.reservedAmount).toFixed(2) }}</template></el-table-column><el-table-column label="已使用"><template #default="{row}">¥{{ Number(row.usedAmount).toFixed(2) }}</template></el-table-column><el-table-column label="剩余"><template #default="{row}">¥{{ remaining(row).toFixed(2) }}</template></el-table-column><el-table-column label="操作" width="140"><template #default="{row}"><el-button link type="primary" @click="open(row)">编辑</el-button><el-button link type="danger" @click="remove(row)">删除</el-button></template></el-table-column></el-table><AllocationDialog v-model="dialogOpen" :batch-id="batchId" resource-kind="SUBSIDY" :colleges="colleges" :grades="grades" :editing="editing" @saved="loadQuotas"/></section>
</template>

<style scoped>
.quota-section { margin: 28px 20px 0; padding-top: 24px; border-top: 1px solid #e5e7eb; }.quota-section h3 { margin: 0; font-size: 16px; }.quota-section p { margin: 6px 0 16px; color: #6b7280; font-size: 12px; }.toolbar { margin-bottom: 16px; display: flex; gap: 12px; align-items: center; }
</style>
