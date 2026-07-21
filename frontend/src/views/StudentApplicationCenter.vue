<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Plus, Refresh } from '@element-plus/icons-vue'
import SubmitConfirmDialog from '../components/application/SubmitConfirmDialog.vue'
import { catalogAPI, createRequestId, studentApplicationAPI } from '../api/application.js'

const activeTab = ref('green')
const loading = ref(false)
const applications = ref([])
const fees = ref([])
const selected = ref(null)
const editorOpen = ref(false)
const submitOpen = ref(false)
const saving = ref(false)
const arrearsSaving = ref(false)
const form = reactive({ applicationType: 'GREEN_CHANNEL', batchType: 'GREEN_CHANNEL', batchId: null, applicationReason: '' })
const arrearsRows = ref([])
const reasonMap = { GREEN_CHANNEL: '绿色通道', LIVING_SUBSIDY: '生活补助', TRAVEL_SUBSIDY: '路费补助' }
const selectedType = computed(() => selected.value?.applicationType || '')
const totalArrears = computed(() => arrearsRows.value.reduce((total, item) => total + Number(item.declaredAmount || 0), 0))
const canEditSelected = computed(() => selected.value?.status === 'DRAFT' || ['COUNSELOR_RETURNED', 'COLLEGE_RETURNED', 'SCHOOL_RETURNED'].includes(selected.value?.status))

function errorMessage(error, fallback) { return error.response?.data?.message || error.message || fallback }
function batchTypeFor(type) { return type === 'GREEN_CHANNEL' ? 'GREEN_CHANNEL' : 'SUBSIDY' }
function currentApplication(type) { return applications.value.find(item => item.applicationType === type) || null }
function stateText(status) { return ({ DRAFT: '草稿', COUNSELOR_PENDING: '辅导员审核中', COLLEGE_PENDING: '学院审核中', SCHOOL_PENDING: '学校审核中', APPROVED: '已通过', COMPLETED: '已完成', COUNSELOR_RETURNED: '辅导员退回', COLLEGE_RETURNED: '学院退回', SCHOOL_RETURNED: '学校退回', REJECTED: '未通过' })[status] || status }
function stateTag(status) { if (status === 'DRAFT') return 'info'; if (status?.includes('RETURN') || status === 'REJECTED') return 'danger'; if (status === 'APPROVED' || status === 'COMPLETED') return 'success'; return 'warning' }

async function loadData() {
  loading.value = true
  try {
    const [applicationData, feeData] = await Promise.all([studentApplicationAPI.mine(), catalogAPI.listFeeItems(false)])
    applications.value = applicationData || []; fees.value = feeData || []
    if (selected.value) selected.value = applications.value.find(item => item.id === selected.value.id) || null
  } catch (error) { ElMessage.error(errorMessage(error, '申请数据加载失败')) } finally { loading.value = false }
}
function openDraft(type) {
  const exists = currentApplication(type)
  if (exists) return openExisting(exists)
  selected.value = null; arrearsRows.value = []
  Object.assign(form, { applicationType: type, batchType: batchTypeFor(type), batchId: null, applicationReason: '' }); editorOpen.value = true
}
async function openExisting(application) {
  selected.value = application
  Object.assign(form, { applicationType: application.applicationType, batchType: batchTypeFor(application.applicationType), batchId: null, applicationReason: application.applicationReason || '' })
  if (application.applicationType === 'GREEN_CHANNEL') {
    try { arrearsRows.value = (await studentApplicationAPI.arrears(application.id)).map(item => ({ feeItemId: item.feeItemId, declaredAmount: Number(item.declaredAmount) })) } catch (error) { ElMessage.error(errorMessage(error, '欠费明细加载失败')) }
  }
}
async function saveDraft() {
  if (!form.batchId || !form.applicationReason?.trim()) return ElMessage.warning('请填写批次 ID 和申请理由')
  saving.value = true
  try {
    if (selected.value) await studentApplicationAPI.updateDraft(selected.value.id, { version: selected.value.version, applicationReason: form.applicationReason })
    else await studentApplicationAPI.createDraft({ ...form, requestId: createRequestId() })
    ElMessage.success('草稿已保存'); editorOpen.value = false; await loadData()
  } catch (error) { ElMessage.error(errorMessage(error, '草稿保存失败')) } finally { saving.value = false }
}
async function removeDraft(application) {
  try { await ElMessageBox.confirm('删除草稿后不可恢复。当前接口尚未使用资源预占，因此不会显示资源释放信息。', '删除草稿', { type: 'warning' }); await studentApplicationAPI.deleteDraft(application.id, application.version); ElMessage.success('草稿已删除'); selected.value = null; await loadData() } catch (error) { if (error !== 'cancel') ElMessage.error(errorMessage(error, '删除失败')) }
}
function addArrears() { arrearsRows.value.push({ feeItemId: fees.value[0]?.id ?? null, declaredAmount: null }) }
async function saveArrears() {
  if (!selected.value || !canEditSelected.value) return
  if (!arrearsRows.value.length || arrearsRows.value.some(item => !item.feeItemId || !item.declaredAmount)) return ElMessage.warning('请完整填写欠费项目与金额')
  if (totalArrears.value > 8000) return ElMessage.warning('欠费申报总额不得超过 ¥8,000')
  arrearsSaving.value = true
  try { await studentApplicationAPI.replaceArrears(selected.value.id, { version: selected.value.version, items: arrearsRows.value }); ElMessage.success('欠费明细已保存'); await loadData(); selected.value = applications.value.find(item => item.id === selected.value.id) } catch (error) { ElMessage.error(errorMessage(error, '欠费明细保存失败')) } finally { arrearsSaving.value = false }
}
onMounted(loadData)
</script>

<template>
  <div class="page-container student-center-page" v-loading="loading">
    <section class="page-heading-row"><div><h1>学生申请中心</h1><p>保存申请草稿并维护绿色通道欠费明细。正式提交、附件和资格判断将在依赖接口合入后开放。</p></div><div class="page-actions"><el-button @click="loadData"><el-icon><Refresh /></el-icon>刷新</el-button></div></section>
    <el-alert class="business-notice" type="warning" :closable="false" show-icon title="开发身份与接口边界"><template #default>当前草稿接口仅在本地开发环境使用临时身份头；生产与演示环境必须由后端 JWT 登录上下文提供身份和数据范围。</template></el-alert>
    <section class="content-card application-card"><el-tabs v-model="activeTab" class="student-tabs">
      <el-tab-pane label="个人信息" name="profile"><el-empty description="学生个人信息查询与更新接口尚未接入，暂不展示虚构个人资料。"/></el-tab-pane>
      <el-tab-pane label="绿色通道" name="green"><div class="application-tab-header"><div><h2>绿色通道申请</h2><p>同一学生、同一批次仅能保留一条有效绿色通道申请。</p></div><el-button type="primary" :disabled="!!currentApplication('GREEN_CHANNEL')" @click="openDraft('GREEN_CHANNEL')"><el-icon><Plus /></el-icon>新建申请</el-button></div><div v-if="currentApplication('GREEN_CHANNEL')" class="application-summary"><div><span>申请编号</span><strong>{{ currentApplication('GREEN_CHANNEL').applicationNo }}</strong></div><div><span>当前状态</span><el-tag :type="stateTag(currentApplication('GREEN_CHANNEL').status)">{{ stateText(currentApplication('GREEN_CHANNEL').status) }}</el-tag></div><div class="summary-actions"><el-button @click="openExisting(currentApplication('GREEN_CHANNEL'))">查看 / 编辑</el-button><el-button v-if="currentApplication('GREEN_CHANNEL').status === 'DRAFT'" type="primary" @click="selected=currentApplication('GREEN_CHANNEL'); submitOpen=true">正式提交</el-button><el-button v-if="currentApplication('GREEN_CHANNEL').status === 'DRAFT'" type="danger" plain @click="removeDraft(currentApplication('GREEN_CHANNEL'))"><el-icon><Delete /></el-icon>删除草稿</el-button></div></div><el-empty v-else description="当前没有绿色通道申请草稿"><el-button type="primary" @click="openDraft('GREEN_CHANNEL')">建立草稿</el-button></el-empty><div v-if="selected && selectedType === 'GREEN_CHANNEL'" class="detail-section"><div class="section-title"><h3>欠费申请明细</h3><span>合计 ¥{{ totalArrears.toFixed(2) }} / ¥8,000.00</span></div><el-alert v-if="!canEditSelected" type="info" :closable="false" title="当前申请不处于可编辑状态，欠费明细只读。"/><div v-else class="arrears-actions"><el-button @click="addArrears">新增项目</el-button><el-button type="primary" :loading="arrearsSaving" @click="saveArrears">保存明细</el-button></div><el-table :data="arrearsRows" border class="standard-table"><el-table-column label="欠费项目" min-width="220"><template #default="{row}"><el-select v-model="row.feeItemId" :disabled="!canEditSelected"><el-option v-for="item in fees" :key="item.id" :label="item.name" :value="item.id"/></el-select></template></el-table-column><el-table-column label="申报金额" width="220"><template #default="{row}"><el-input-number v-model="row.declaredAmount" :disabled="!canEditSelected" :min="0.01" :precision="2" :step="100"/></template></el-table-column><el-table-column v-if="canEditSelected" label="操作" width="100"><template #default="{ $index }"><el-button link type="danger" @click="arrearsRows.splice($index,1)">移除</el-button></template></el-table-column></el-table></div></el-tab-pane>
      <el-tab-pane v-for="type in ['LIVING_SUBSIDY', 'TRAVEL_SUBSIDY']" :key="type" :label="reasonMap[type]" :name="type"><div class="application-tab-header"><div><h2>{{ reasonMap[type] }}申请</h2><p>最终补助金额由辅导员审核时填写。</p></div><el-button type="primary" :disabled="!!currentApplication(type)" @click="openDraft(type)">新建申请</el-button></div><div v-if="currentApplication(type)" class="application-summary"><div><span>申请编号</span><strong>{{ currentApplication(type).applicationNo }}</strong></div><div><span>当前状态</span><el-tag :type="stateTag(currentApplication(type).status)">{{ stateText(currentApplication(type).status) }}</el-tag></div><div class="summary-actions"><el-button @click="openExisting(currentApplication(type))">查看 / 编辑</el-button></div></div><el-empty v-else :description="`尚未建立${reasonMap[type]}申请`"/></el-tab-pane>
    </el-tabs></section>
    <el-dialog v-model="editorOpen" :title="`新建${reasonMap[form.applicationType]}草稿`" width="560px"><el-alert type="info" :closable="false" title="批次接口未接入，请仅在本地联调时填写已存在的批次 ID。"/><el-form class="draft-form" label-width="100px"><el-form-item label="申请类型"><el-input :model-value="reasonMap[form.applicationType]" disabled/></el-form-item><el-form-item label="批次 ID" required><el-input-number v-model="form.batchId" :min="1" :precision="0"/></el-form-item><el-form-item label="申请理由" required><el-input v-model="form.applicationReason" type="textarea" :rows="4" maxlength="500" show-word-limit/></el-form-item></el-form><template #footer><el-button @click="editorOpen=false">取消</el-button><el-button type="primary" :loading="saving" @click="saveDraft">保存草稿</el-button></template></el-dialog>
    <SubmitConfirmDialog v-model="submitOpen" :application="selected" :available="false" />
  </div>
</template>

<style scoped>
.student-center-page { padding: 0 0 24px; }.business-notice { margin-bottom: 16px; }.student-tabs :deep(.el-tabs__header) { margin: 0; padding: 0 20px; }.student-tabs :deep(.el-tabs__content) { padding: 20px; }.application-tab-header { margin-bottom: 16px; display: flex; align-items: center; justify-content: space-between; }.application-tab-header h2 { margin: 0; font-size: 16px; line-height: 24px; }.application-tab-header p { margin: 4px 0 0; color: #6b7280; font-size: 12px; }.application-summary { padding: 16px 20px; display: grid; grid-template-columns: 1.4fr 1fr auto; gap: 20px; align-items: center; border: 1px solid #e5e7eb; background: #f8fafc; }.application-summary span, .application-summary strong { display: block; }.application-summary span { color: #6b7280; font-size: 12px; }.application-summary strong { margin-top: 4px; }.summary-actions, .arrears-actions { display: flex; gap: 8px; justify-content: flex-end; }.detail-section { margin-top: 16px; padding: 20px; border: 1px solid #e5e7eb; background: #fff; }.section-title { margin-bottom: 12px; }.section-title h3 { font-size: 16px; }.arrears-actions { margin: 12px 0; }.draft-form { margin-top: 18px; }
</style>
