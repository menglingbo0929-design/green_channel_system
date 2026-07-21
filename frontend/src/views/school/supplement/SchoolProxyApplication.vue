<script setup>
import { reactive, ref } from 'vue'
import { createProxyDraft, findProxyStudent, submitProxyDraft } from '../../../api/schoolProxy'
import { useUserStore } from '../../../stores/user'

/** 6.1.3 临时联调页：最终统一 UI 前，仅验证学校代申请的真实接口流程。 */
const userStore = useUserStore()
const student = ref(null)
const result = ref(null)
const form = reactive({ studentNo: '', batchId: '', applicationReason: '', feeItemId: '', declaredAmount: '', requestId: '' })
const uuid = () => crypto.randomUUID?.() ?? `proxy-${Date.now()}`
const search = async () => { student.value=(await findProxyStudent(form.studentNo)).data.data }
const draft = async () => {
  const data={ studentNo:form.studentNo,batchType:'GREEN_CHANNEL',batchId:Number(form.batchId),applicationReason:form.applicationReason,requestId:uuid(),
    arrearsItems: form.feeItemId ? [{feeItemId:Number(form.feeItemId),declaredAmount:Number(form.declaredAmount)}] : [], giftItems:[] }
  result.value=(await createProxyDraft(data,userStore.userId)).data.data
}
const submit = async () => { result.value=(await submitProxyDraft(result.value.applicationId,result.value.version,uuid(),userStore.userId)).data.data }
</script>
<template>
  <main class="proxy-demo"><h1>学校代申请（6.1.3 联调页）</h1><p>流程：查学生 → 创建草稿 → 通过成员二附件接口上传附件 → 正式提交审核。</p>
    <label>学生学号 <input v-model="form.studentNo" /></label><button @click="search">查询学生</button>
    <p v-if="student">已找到：{{ student.studentName }}（{{ student.collegeName }}）</p>
    <section><label>绿色通道批次 ID <input v-model="form.batchId" type="number" /></label><label>申请原因 <textarea v-model="form.applicationReason" /></label>
      <label>欠费项目 ID（可选）<input v-model="form.feeItemId" type="number" /></label><label>申报金额（可选）<input v-model="form.declaredAmount" type="number" /></label><button @click="draft">创建 SCHOOL_PROXY 草稿</button></section>
    <section v-if="result"><p>申请 ID：{{ result.applicationId }}；状态：{{ result.status }}；版本：{{ result.version }}</p><button @click="submit">正式提交进入审核</button></section>
  </main>
</template>
<style scoped>.proxy-demo{max-width:800px;margin:30px auto;padding:24px;text-align:left}.proxy-demo label{display:grid;gap:6px;margin:10px 0}.proxy-demo input,.proxy-demo textarea{padding:8px}.proxy-demo button{padding:9px 12px;margin:8px 8px 8px 0}.proxy-demo section{margin-top:18px;padding:16px;background:#f4f8f5}</style>
