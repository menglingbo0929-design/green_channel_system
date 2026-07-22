<script setup>
import { reactive, ref } from 'vue'
import { createProxyDraft, findProxyStudent, submitProxyDraft, uploadProxyAttachment } from '../../../api/schoolProxy'
const student = ref(null)
const result = ref(null)
const loading = ref(false)
const errorMessage = ref('')
const selectedFile = ref(null)
const attachmentUploaded = ref(false)
const form = reactive({ studentNo: '', batchId: '', applicationReason: '', feeItemId: '', declaredAmount: '', arrearsReasonCode: 'OTHER', giftItemId: '', giftQuantity: 1 })
const uuid = () => crypto.randomUUID?.() ?? `proxy-${Date.now()}`
const getErrorMessage = (error, fallback) => error?.response?.data?.message || error?.message || fallback
const search = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    student.value = (await findProxyStudent(form.studentNo)).data.data
  } catch (error) {
    student.value = null
    errorMessage.value = getErrorMessage(error, '查询学生失败，请稍后重试。')
  } finally {
    loading.value = false
  }
}
const draft = async () => {
  loading.value = true
  const data={ studentNo:form.studentNo,batchType:'GREEN_CHANNEL',batchId:Number(form.batchId),applicationReason:form.applicationReason,requestId:uuid(),
    arrearsItems: form.feeItemId ? [{feeItemId:Number(form.feeItemId),declaredAmount:Number(form.declaredAmount),arrearsReasonCode:form.arrearsReasonCode}] : [],
    giftItems: form.giftItemId ? [{giftItemId:Number(form.giftItemId),quantity:Number(form.giftQuantity)}] : [] }
  result.value=(await createProxyDraft(data)).data.data
  attachmentUploaded.value = false
  loading.value = false
}
const chooseAttachment = event => { selectedFile.value = event.target.files?.[0] ?? null; attachmentUploaded.value = false }
const upload = async () => { loading.value = true; await uploadProxyAttachment(result.value.applicationId, selectedFile.value, uuid()); attachmentUploaded.value = true; loading.value = false }
const submit = async () => { loading.value = true; result.value=(await submitProxyDraft(result.value.applicationId,result.value.version,uuid())).data.data; loading.value = false }
const arrearsReasonOptions = [
  { value: 'FAMILY_FINANCIAL_DIFFICULTY', label: '家庭经济困难' },
  { value: 'FAMILY_EMERGENCY', label: '家庭突发情况' },
  { value: 'MAJOR_ILLNESS', label: '重大疾病' },
  { value: 'DISASTER_ACCIDENT', label: '灾害事故' },
  { value: 'OTHER', label: '其他' },
]
</script>
<template>
  <section class="proxy-page">
    <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
    <header class="module-heading"><div><h2>学校代申请</h2><p>为无法自行在线提交的学生建立绿色通道申请，并提交学校审核流程。</p></div></header>
    <section class="form-card">
      <h3>学生信息</h3>
      <div class="student-search"><label><span>学生学号</span><input v-model.trim="form.studentNo" placeholder="请输入学生学号" /></label><button type="button" class="primary" :disabled="loading || !form.studentNo" @click="search">查询学生</button></div>
      <div v-if="student" class="student-card"><div class="student-avatar">{{ student.studentName?.slice(0, 1) }}</div><div><strong>{{ student.studentName }} <small>{{ student.studentNo }}</small></strong><p>{{ student.collegeName || '—' }} / {{ student.majorName || '—' }} / {{ student.gradeName || '—' }} / {{ student.className || '—' }}</p></div></div>
    </section>

    <section class="form-card">
      <h3>申请内容</h3>
      <div class="form-grid">
        <label><span>申请批次 ID</span><input v-model="form.batchId" type="number" min="1" placeholder="请输入批次 ID" /></label>
        <label><span>欠费项目 ID</span><input v-model="form.feeItemId" type="number" min="1" placeholder="可选" /></label>
        <label><span>申报金额</span><input v-model="form.declaredAmount" type="number" min="0.01" step="0.01" placeholder="可选" /></label>
        <label><span>欠费原因</span><select v-model="form.arrearsReasonCode"><option v-for="item in arrearsReasonOptions" :key="item.value" :value="item.value">{{ item.label }}</option></select></label>
        <label><span>礼包物品 ID</span><input v-model="form.giftItemId" type="number" min="1" placeholder="可选" /></label>
        <label><span>礼包数量</span><input v-model="form.giftQuantity" type="number" min="1" placeholder="默认 1" /></label>
        <label class="wide"><span>申请原因</span><textarea v-model.trim="form.applicationReason" rows="3" placeholder="请输入代申请原因" /></label>
      </div>
      <div class="form-actions"><button type="button" class="primary" :disabled="loading || !student" @click="draft">创建申请草稿</button></div>
    </section>

    <section v-if="result" class="result-card">
      <div><h3>申请草稿已创建</h3><p>申请编号：{{ result.applicationNo || result.applicationId }}　当前状态：{{ result.status }}</p></div>
      <div class="submit-steps">
        <label class="upload-field"><span>{{ selectedFile?.name || '选择申请附件' }}</span><input type="file" accept=".pdf,.jpg,.jpeg,.png,.doc,.docx" @change="chooseAttachment" /></label>
        <button type="button" :disabled="loading || !selectedFile" @click="upload">{{ attachmentUploaded ? '附件已上传' : '上传附件' }}</button>
        <button type="button" class="primary" :disabled="loading || !attachmentUploaded" @click="submit">提交学校审核</button>
      </div>
    </section>
  </section>
</template>
<style scoped>
.proxy-page { color: #303133; }.module-heading { margin-bottom: 18px; }.module-heading h2 { margin: 0 0 7px; font-size: 18px; }.module-heading p { margin: 0; color: #909399; font-size: 13px; }.form-card { margin-bottom: 16px; padding: 20px 22px; border: 1px solid #ebeef5; border-radius: 4px; background: #fff; }.form-card h3 { margin: 0 0 18px; color: #303133; font-size: 16px; }.student-search { display: flex; align-items: center; gap: 12px; }.student-search label, .form-grid label { display: grid; align-items: center; gap: 9px; color: #606266; font-size: 14px; }.student-search label { grid-template-columns: 70px 260px; }.student-search input, .form-grid input, .form-grid select, .form-grid textarea { height: 34px; padding: 0 11px; border: 1px solid #dcdfe6; border-radius: 4px; color: #303133; outline: none; }.form-grid textarea { height: auto; padding: 9px 11px; resize: vertical; }.student-search input:focus, .form-grid input:focus, .form-grid select:focus, .form-grid textarea:focus { border-color: #409eff; }.primary { height: 34px; padding: 0 16px; border: 1px solid #409eff; border-radius: 4px; color: #fff; background: #409eff; cursor: pointer; }.primary:disabled { opacity: .6; cursor: not-allowed; }.student-card { display: flex; align-items: center; gap: 12px; margin-top: 16px; padding: 14px 16px; border: 1px solid #d9ecff; border-radius: 4px; background: #ecf5ff; }.student-avatar { display: grid; width: 36px; height: 36px; place-items: center; border-radius: 50%; color: #409eff; background: #fff; font-weight: 700; }.student-card strong { color: #303133; }.student-card small { margin-left: 8px; color: #909399; font-weight: 400; }.student-card p { margin: 6px 0 0; color: #606266; font-size: 13px; }.form-grid { display: grid; grid-template-columns: repeat(3, minmax(180px, 1fr)); gap: 18px; }.wide { grid-column: 1 / -1; }.form-actions { display: flex; justify-content: flex-end; margin-top: 22px; }.result-card { display: flex; align-items: center; justify-content: space-between; padding: 18px 22px; border: 1px solid #e1f3d8; border-radius: 4px; background: #f0f9eb; }.result-card h3 { margin: 0 0 7px; color: #67c23a; font-size: 16px; }.result-card p { margin: 0; color: #606266; font-size: 13px; }.submit-steps { display: flex; align-items: center; gap: 10px; }.submit-steps button { height: 34px; padding: 0 14px; border: 1px solid #dcdfe6; border-radius: 4px; color: #606266; background: #fff; cursor: pointer; }.submit-steps .primary { border-color: #409eff; color: #fff; background: #409eff; }.upload-field { position: relative; display: inline-flex; align-items: center; max-width: 160px; height: 34px; padding: 0 10px; overflow: hidden; border: 1px solid #dcdfe6; border-radius: 4px; color: #606266; background: #fff; font-size: 13px; white-space: nowrap; text-overflow: ellipsis; }.upload-field input { position: absolute; inset: 0; width: 100%; opacity: 0; cursor: pointer; }@media (max-width: 900px) { .form-grid { grid-template-columns: 1fr; }.student-search { align-items: stretch; flex-direction: column; }.student-search label { grid-template-columns: 1fr; }.student-search input { width: 100%; }.result-card { align-items: flex-start; flex-direction: column; gap: 14px; }.submit-steps { flex-wrap: wrap; } }
.error-message { margin: 0 0 16px; padding: 10px 12px; border: 1px solid #fbc4c4; border-radius: 4px; color: #f56c6c; background: #fef0f0; font-size: 13px; }
</style>
