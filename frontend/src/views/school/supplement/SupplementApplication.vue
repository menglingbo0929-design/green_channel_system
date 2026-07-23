<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { createSupplement, findSupplementStudent } from '../../../api/supplement'
import { batchAPI, catalogAPI } from '../../../api/application.js'
import { formatBatchLabel } from '../../../constants/batch.js'
import BusinessConfirmDialog from '../../../components/school/BusinessConfirmDialog.vue'
import SupplementHistory from './SupplementHistory.vue'

const student = ref(null)
const result = ref(null)
const loading = ref(false)
const refreshKey = ref(0)
const confirmDialogOpen = ref(false)
const pendingPayload = ref(null)
const batchOptions = ref([])
const feeItemOptions = ref([])
const giftItemOptions = ref([])

const nowForInput = () => {
  const date = new Date()
  date.setMinutes(date.getMinutes() - date.getTimezoneOffset())
  return date.toISOString().slice(0, 16)
}

const form = reactive({
  studentNo: '',
  applicationType: 'GREEN_CHANNEL',
  batchId: '',
  applicationReason: '',
  supplementReason: '',
  handledAt: nowForInput(),
  subsidyAmount: '',
  arrearsItems: [{ feeItemId: '', declaredAmount: '', arrearsReasonCode: 'OTHER' }],
  giftItems: [],
})

const newRequestId = () => crypto.randomUUID?.() ?? `supplement-${Date.now()}`

async function runRequest(action) {
  loading.value = true
  await action()
  loading.value = false
}

function addArrearsItem() {
  form.arrearsItems.push({ feeItemId: '', declaredAmount: '', arrearsReasonCode: 'OTHER' })
}

function addGiftItem() {
  form.giftItems.push({ giftItemId: '', quantity: 1 })
}

function removeItem(items, index) {
  items.splice(index, 1)
}

/** 先查学生，避免操作员把申请补录到错误学号。 */
function searchStudent() {
  runRequest(async () => {
    const response = await findSupplementStudent(form.studentNo)
    student.value = response.data.data
  })
}

/**
 * 只组装当前申请类型允许的明细：绿色通道发送欠费/礼包，补助只发送金额。
 * batchType、source、status 和 containsArrears 都继续由后端根据正式规则确定。
 */
function buildPayload() {
  const greenChannel = form.applicationType === 'GREEN_CHANNEL'
  return {
    studentNo: form.studentNo,
    applicationType: form.applicationType,
    batchId: Number(form.batchId),
    applicationReason: form.applicationReason,
    supplementReason: form.supplementReason,
    handledAt: form.handledAt.length === 16 ? `${form.handledAt}:00` : form.handledAt,
    arrearsItems: greenChannel
      ? form.arrearsItems.filter(item => item.feeItemId !== '').map(item => ({
          feeItemId: Number(item.feeItemId),
          declaredAmount: Number(item.declaredAmount),
          arrearsReasonCode: item.arrearsReasonCode,
        }))
      : [],
    giftItems: greenChannel
      ? form.giftItems.filter(item => item.giftItemId !== '').map(item => ({
          giftItemId: Number(item.giftItemId),
          quantity: Number(item.quantity),
        }))
      : [],
    subsidyAmount: greenChannel ? null : Number(form.subsidyAmount),
    requestId: newRequestId(),
  }
}

/** 填写完成后先打开页面 8 统一业务确认弹窗，不直接写库。 */
function openSupplementConfirmation() {
  pendingPayload.value = buildPayload()
  confirmDialogOpen.value = true
}

/** 操作员在弹窗中再次核对后，才调用真实补录接口。 */
function submitSupplement() {
  runRequest(async () => {
    const response = await createSupplement(pendingPayload.value)
    result.value = response.data.data
    refreshKey.value += 1
    confirmDialogOpen.value = false
  })
}

const applicationTypeNames = {
  GREEN_CHANNEL: '绿色通道',
  LIVING_SUBSIDY: '生活补助',
  TRAVEL_SUBSIDY: '路费补助',
}

const arrearsReasonOptions = [
  { value: 'FAMILY_FINANCIAL_DIFFICULTY', label: '家庭经济困难' },
  { value: 'FAMILY_EMERGENCY', label: '家庭突发情况' },
  { value: 'MAJOR_ILLNESS', label: '重大疾病' },
  { value: 'DISASTER_ACCIDENT', label: '灾害事故' },
  { value: 'OTHER', label: '其他' },
]

async function loadBatchOptions() {
  form.batchId = ''
  const batchType = form.applicationType === 'GREEN_CHANNEL' ? 'GREEN_CHANNEL' : 'SUBSIDY'
  batchOptions.value = await batchAPI.open(batchType) || []
}

watch(() => form.applicationType, loadBatchOptions)
onMounted(async () => {
  const [, feeItems, giftItems] = await Promise.all([
    loadBatchOptions(),
    catalogAPI.listFeeItems(false),
    catalogAPI.listGiftItems(false),
  ])
  feeItemOptions.value = feeItems ?? []
  giftItemOptions.value = giftItems ?? []
})
</script>

<template>
  <section class="supplement-page">
    <header class="module-heading">
      <div><h2>线下补录</h2><p>录入已在线下完成办理的申请，系统按业务类型进入对应后续流程。</p></div>
    </header>

    <section class="form-panel">
      <h3>学生信息</h3>
      <div class="row">
        <label>学生学号<input v-model.trim="form.studentNo" placeholder="例如 20260001" /></label>
        <button type="button" :disabled="loading" @click="searchStudent">按学号查询</button>
      </div>
      <article v-if="student" class="student-card">
        <strong>{{ student.studentName }}（{{ student.studentNo }}）</strong>
        <span>{{ student.collegeName }} / {{ student.majorName }} / {{ student.gradeName }} / {{ student.className }}</span>
      </article>

      <h3>补录内容</h3>
      <div class="grid">
        <label>申请类型
          <select v-model="form.applicationType">
            <option value="GREEN_CHANNEL">绿色通道</option>
            <option value="LIVING_SUBSIDY">生活补助</option>
            <option value="TRAVEL_SUBSIDY">路费补助</option>
          </select>
        </label>
        <label>申请批次<select v-model="form.batchId"><option value="">请选择批次</option><option v-for="batch in batchOptions" :key="batch.batchId" :value="batch.batchId">{{ formatBatchLabel(batch) }}</option></select></label>
        <label>线下办理时间<input v-model="form.handledAt" type="datetime-local" /></label>
        <label class="wide">申请原因<textarea v-model="form.applicationReason" rows="2" /></label>
        <label class="wide">补录原因<textarea v-model="form.supplementReason" rows="2" placeholder="说明为何未走线上流程" /></label>
      </div>

      <template v-if="form.applicationType === 'GREEN_CHANNEL'">
        <div class="detail-header"><h3>欠费明细</h3><button type="button" @click="addArrearsItem">增加欠费项</button></div>
        <div v-for="(item, index) in form.arrearsItems" :key="`arrears-${index}`" class="detail-row">
          <label>欠费项目<select v-model="item.feeItemId"><option value="">请选择欠费项目</option><option v-for="option in feeItemOptions" :key="option.id" :value="option.id">{{ option.name }}</option></select></label>
          <label>申报金额<input v-model="item.declaredAmount" type="number" min="0.01" step="0.01" /></label>
          <label>欠费原因<select v-model="item.arrearsReasonCode"><option v-for="option in arrearsReasonOptions" :key="option.value" :value="option.value">{{ option.label }}</option></select></label>
          <button type="button" class="remove" @click="removeItem(form.arrearsItems, index)">删除</button>
        </div>

        <div class="detail-header"><h3>礼包明细</h3><button type="button" @click="addGiftItem">增加礼包项</button></div>
        <div v-for="(item, index) in form.giftItems" :key="`gift-${index}`" class="detail-row">
          <label>礼包物品<select v-model="item.giftItemId"><option value="">请选择礼包物品</option><option v-for="option in giftItemOptions" :key="option.id" :value="option.id">{{ option.name }}</option></select></label>
          <label>数量<input v-model="item.quantity" type="number" min="1" /></label>
          <button type="button" class="remove" @click="removeItem(form.giftItems, index)">删除</button>
        </div>
      </template>

      <label v-else>补助金额<input v-model="form.subsidyAmount" type="number" min="0.01" step="0.01" /></label>

      <button type="button" class="submit" :disabled="loading" @click="openSupplementConfirmation">
        {{ loading ? '处理中…' : '提交线下补录' }}
      </button>
    </section>

    <section v-if="result" class="result-card">
      <h3>补录成功</h3>
      <p>申请编号：{{ result.applicationNo }}；来源：{{ result.source }}</p>
      <p>最终状态：{{ result.status }}；当前层级：{{ result.currentLevel }}；版本：{{ result.version }}</p>
    </section>

    <SupplementHistory :refresh-key="refreshKey" />

    <!-- 线下补录属于成员四执行型业务确认，不使用成员三的通过/退回/不通过弹窗。 -->
    <BusinessConfirmDialog
      v-model="confirmDialogOpen"
      mode="SUPPLEMENT"
      :business="{
        ...student,
        applicationType: form.applicationType,
        applicationTypeName: applicationTypeNames[form.applicationType],
        declaredAmount: form.applicationType === 'GREEN_CHANNEL'
          ? form.arrearsItems.reduce((sum, item) => sum + Number(item.declaredAmount || 0), 0)
          : Number(form.subsidyAmount || 0),
        containsArrears: form.applicationType === 'GREEN_CHANNEL' && form.arrearsItems.some(item => item.feeItemId !== ''),
        supplementReason: form.supplementReason,
        handledAt: form.handledAt,
        statusName: '待提交',
      }"
      :submitting="loading"
      @confirm="submitSupplement"
    />
  </section>
</template>

<style scoped>
.supplement-page { color: #303133; }.module-heading { margin-bottom: 18px; }.module-heading h2 { margin: 0 0 7px; font-size: 18px; }.module-heading p { margin: 0; color: #909399; font-size: 13px; }.form-panel { margin-top: 0; padding: 22px; border: 1px solid #ebeef5; border-radius: 4px; background: #fff; }.form-panel h3 { margin: 0 0 18px; color: #303133; font-size: 16px; }.form-panel h3:not(:first-child) { margin-top: 30px; padding-top: 24px; border-top: 1px solid #ebeef5; }
.row, .detail-row, .detail-header { display: flex; flex-wrap: wrap; gap: 12px; align-items: end; }.row label { max-width: 360px; }.grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px 20px; }.wide { grid-column: span 3; }
label { display: grid; flex: 1; gap: 7px; color: #606266; font-size: 13px; font-weight: 400; }input, select, textarea { padding: 8px 10px; border: 1px solid #dcdfe6; border-radius: 4px; font: inherit; outline: none; }input:focus,select:focus,textarea:focus { border-color: #409eff; }textarea { resize: vertical; }
button { height: 34px; padding: 0 14px; border: 1px solid #409eff; border-radius: 4px; color: #fff; background: #409eff; cursor: pointer; }button:disabled { opacity: .55; cursor: not-allowed; }.remove { border-color: #f56c6c; color: #f56c6c; background: #fff; }.submit { margin-top: 24px; padding: 0 20px; }.student-card { display: grid; gap: 5px; margin-top: 14px; padding: 14px; border: 1px solid #d9ecff; border-radius: 4px; background: #ecf5ff; }.result-card { display: grid; gap: 5px; margin-top: 18px; padding: 16px 18px; border: 1px solid #e1f3d8; border-radius: 4px; background: #f0f9eb; }.result-card h3 { margin: 0; color: #67c23a; font-size: 16px; }.result-card p { margin: 0; color: #606266; font-size: 13px; }.detail-header { justify-content: space-between; margin-top: 24px; padding-top: 20px; border-top: 1px solid #ebeef5; }.detail-header h3 { margin: 0; border: 0 !important; padding: 0 !important; }.detail-row { margin: 12px 0; padding: 12px; border-radius: 4px; background: #f8fafc; }@media (max-width: 760px) { .grid { grid-template-columns: 1fr; }.wide { grid-column: span 1; } }
</style>
