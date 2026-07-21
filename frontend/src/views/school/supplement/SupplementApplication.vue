<script setup>
import { reactive, ref } from 'vue'
import { createSupplement, findSupplementStudent } from '../../../api/supplement'
import BusinessConfirmDialog from '../../../components/school/BusinessConfirmDialog.vue'
import { useUserStore } from '../../../stores/user'
import SupplementHistory from './SupplementHistory.vue'

/**
 * 6.1.4 线下补录联调页面。
 *
 * 页面只负责构造第 17 节规定的请求体和展示真实接口结果。成员一、二、三的
 * 依赖全部合入后调用真实接口，不生成模拟申请编号。
 */
const userStore = useUserStore()
const student = ref(null)
const result = ref(null)
const loading = ref(false)
const refreshKey = ref(0)
const confirmDialogOpen = ref(false)
const pendingPayload = ref(null)

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
  arrearsItems: [{ feeItemId: '', declaredAmount: '' }],
  giftItems: [],
})

const newRequestId = () => crypto.randomUUID?.() ?? `supplement-${Date.now()}`

async function runRequest(action) {
  loading.value = true
  await action()
  loading.value = false
}

function addArrearsItem() {
  form.arrearsItems.push({ feeItemId: '', declaredAmount: '' })
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
    const response = await findSupplementStudent(form.studentNo, userStore.userId)
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
    const response = await createSupplement(pendingPayload.value, userStore.userId)
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
</script>

<template>
  <main class="supplement-demo">
    <header>
      <p class="eyebrow">成员四 · 6.1.4 联调页</p>
      <h1>绿色通道线下补录</h1>
      <p>用于把已经在线下完成办理的申请补录进系统，并自动形成待欠费确认或已完成状态。</p>
    </header>

    <p class="dependency-note">真实创建依赖成员一学生/身份、成员二申请明细/资源、成员三自动审核，演示前须同时完成合入。</p>

    <section class="form-panel">
      <h2>1. 确认学生</h2>
      <div class="row">
        <label>学生学号<input v-model.trim="form.studentNo" placeholder="例如 20260001" /></label>
        <button type="button" :disabled="loading" @click="searchStudent">按学号查询</button>
      </div>
      <article v-if="student" class="student-card">
        <strong>{{ student.studentName }}（{{ student.studentNo }}）</strong>
        <span>{{ student.collegeName }} / {{ student.majorName }} / {{ student.gradeName }} / {{ student.className }}</span>
      </article>

      <h2>2. 填写线下办理内容</h2>
      <div class="grid">
        <label>申请类型
          <select v-model="form.applicationType">
            <option value="GREEN_CHANNEL">绿色通道</option>
            <option value="LIVING_SUBSIDY">生活补助</option>
            <option value="TRAVEL_SUBSIDY">路费补助</option>
          </select>
        </label>
        <label>批次 ID<input v-model="form.batchId" type="number" min="1" /></label>
        <label>线下办理时间<input v-model="form.handledAt" type="datetime-local" /></label>
        <label class="wide">申请原因<textarea v-model="form.applicationReason" rows="2" /></label>
        <label class="wide">补录原因<textarea v-model="form.supplementReason" rows="2" placeholder="说明为何未走线上流程" /></label>
      </div>

      <template v-if="form.applicationType === 'GREEN_CHANNEL'">
        <div class="detail-header"><h3>欠费明细</h3><button type="button" @click="addArrearsItem">增加欠费项</button></div>
        <div v-for="(item, index) in form.arrearsItems" :key="`arrears-${index}`" class="detail-row">
          <label>欠费项目 ID<input v-model="item.feeItemId" type="number" min="1" /></label>
          <label>申报金额<input v-model="item.declaredAmount" type="number" min="0.01" step="0.01" /></label>
          <button type="button" class="remove" @click="removeItem(form.arrearsItems, index)">删除</button>
        </div>

        <div class="detail-header"><h3>礼包明细</h3><button type="button" @click="addGiftItem">增加礼包项</button></div>
        <div v-for="(item, index) in form.giftItems" :key="`gift-${index}`" class="detail-row">
          <label>礼包物品 ID<input v-model="item.giftItemId" type="number" min="1" /></label>
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
      <h2>补录成功</h2>
      <p>申请编号：{{ result.applicationNo }}；来源：{{ result.source }}</p>
      <p>最终状态：{{ result.status }}；当前层级：{{ result.currentLevel }}；版本：{{ result.version }}</p>
    </section>

    <SupplementHistory :user-id="userStore.userId" :refresh-key="refreshKey" />

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
  </main>
</template>

<style scoped>
.supplement-demo { max-width: 1120px; margin: 0 auto; padding: 36px 24px; color: #233044; text-align: left; }
.eyebrow { margin-bottom: 4px; color: #2f7d57; font-weight: 700; }h1 { margin: 0 0 8px; }.dependency-note { padding: 12px; border-left: 4px solid #d97706; background: #fffbeb; color: #92400e; }
.form-panel { margin-top: 24px; padding: 22px; border-radius: 10px; background: #f4f8f5; }.form-panel h2 { margin-top: 24px; }.form-panel h2:first-child { margin-top: 0; }
.row, .detail-row, .detail-header { display: flex; flex-wrap: wrap; gap: 12px; align-items: end; }.grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }.wide { grid-column: span 3; }
label { display: grid; flex: 1; gap: 6px; font-size: 13px; font-weight: 600; }input, select, textarea { padding: 9px; border: 1px solid #b9c7be; border-radius: 6px; font: inherit; }textarea { resize: vertical; }
button { padding: 9px 13px; border: 0; border-radius: 6px; color: #fff; background: #287a55; cursor: pointer; }button:disabled { opacity: .55; cursor: not-allowed; }.remove { background: #a83b3b; }.submit { margin-top: 22px; padding: 11px 18px; }
.student-card, .result-card { display: grid; gap: 5px; margin-top: 14px; padding: 14px; border: 1px solid #cddbd2; border-radius: 8px; background: #fff; }.detail-header { justify-content: space-between; margin-top: 20px; }.detail-row { margin: 10px 0; }
@media (max-width: 760px) { .grid { grid-template-columns: 1fr; }.wide { grid-column: span 1; } }
</style>
