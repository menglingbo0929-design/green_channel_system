<script setup>
import { ref } from 'vue'
import { getVoucher, printVoucher } from '../../../api/voucher'
import { useUserStore } from '../../../stores/user'
const voucherNo = ref('')
const userStore = useUserStore()
const voucher = ref(null)
const loading = ref(false)
async function load(print = false) {
  loading.value = true
  const response = print ? await printVoucher(voucherNo.value, userStore.userId) : await getVoucher(voucherNo.value, userStore.userId)
  voucher.value = response.data.data
  loading.value = false
  if (print) setTimeout(() => window.print(), 0)
}
const money = value => value == null ? '—' : `¥${Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
</script>
<template>
  <section class="voucher-page">
    <header class="module-heading"><div><h2>欠费确认单</h2><p>查询、预览和打印已完成欠费确认的正式单据。</p></div></header>
    <section class="search-panel">
      <label><span>单据编号</span><input v-model.trim="voucherNo" placeholder="请输入单据编号" @keyup.enter="load()" /></label>
      <button type="button" class="primary" :disabled="loading || !voucherNo" @click="load()">{{ loading ? '查询中' : '查询预览' }}</button>
      <button type="button" :disabled="loading || !voucherNo" @click="load(true)">打印单据</button>
    </section>

    <section v-if="voucher" class="voucher-sheet">
      <header class="sheet-heading"><div><h3>{{ voucher.printTitle || '高校绿色通道欠费确认单' }}</h3><p>单据编号：{{ voucher.voucherNo }}</p></div><span class="completed-tag">已确认</span></header>
      <div class="info-grid">
        <div><span>学生姓名</span><strong>{{ voucher.studentName || '—' }}</strong></div><div><span>学号</span><strong>{{ voucher.studentNo || '—' }}</strong></div>
        <div><span>学院 / 专业</span><strong>{{ voucher.collegeName || '—' }} / {{ voucher.majorName || '—' }}</strong></div><div><span>年级 / 班级</span><strong>{{ voucher.gradeName || '—' }} / {{ voucher.className || '—' }}</strong></div>
      </div>
      <table><thead><tr><th>欠费项目</th><th>申报金额</th></tr></thead><tbody><tr v-for="item in voucher.arrearsItems" :key="item.feeItemName"><td>{{ item.feeItemName }}</td><td>{{ money(item.declaredAmount) }}</td></tr></tbody></table>
      <div class="amount-row"><span>申请金额：<strong>{{ money(voucher.appliedAmount) }}</strong></span><span>实际确认金额：<strong class="confirmed">{{ money(voucher.confirmedAmount) }}</strong></span></div>
      <footer><span>确认人：{{ voucher.confirmUserName || '—' }}</span><span>确认时间：{{ voucher.confirmedTime || '—' }}</span></footer>
    </section>
    <section v-else class="empty-preview"><span>请先输入单据编号查询确认单</span></section>
  </section>
</template>
<style scoped>
.voucher-page { color: #303133; }.module-heading { margin-bottom: 18px; }.module-heading h2 { margin: 0 0 7px; font-size: 18px; }.module-heading p { margin: 0; color: #909399; font-size: 13px; }.search-panel { display: flex; align-items: center; gap: 10px; padding: 18px 20px; border: 1px solid #ebeef5; border-radius: 4px; background: #fafcff; }.search-panel label { display: flex; align-items: center; gap: 10px; color: #606266; font-size: 14px; }.search-panel input { width: 250px; height: 34px; padding: 0 11px; border: 1px solid #dcdfe6; border-radius: 4px; outline: none; }.search-panel button { height: 34px; padding: 0 16px; border: 1px solid #dcdfe6; border-radius: 4px; color: #606266; background: #fff; cursor: pointer; }.search-panel .primary { border-color: #409eff; color: #fff; background: #409eff; }.search-panel button:disabled { opacity: .6; cursor: not-allowed; }
.voucher-sheet { max-width: 980px; margin: 22px auto 0; padding: 32px 40px; border: 1px solid #e4e7ed; background: #fff; box-shadow: 0 3px 14px rgba(31,45,61,.06); }.sheet-heading { display: flex; align-items: flex-start; justify-content: space-between; padding-bottom: 20px; border-bottom: 2px solid #303133; }.sheet-heading h3 { margin: 0 0 9px; color: #1f2937; font-size: 22px; }.sheet-heading p { margin: 0; color: #909399; font-size: 13px; }.completed-tag { padding: 5px 10px; border-radius: 3px; color: #67c23a; background: #f0f9eb; font-size: 13px; }.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0; margin: 22px 0; border: 1px solid #ebeef5; }.info-grid div { display: grid; gap: 7px; min-height: 66px; padding: 12px 16px; border-right: 1px solid #ebeef5; border-bottom: 1px solid #ebeef5; }.info-grid div:nth-child(2n) { border-right: 0; }.info-grid div:nth-last-child(-n+2) { border-bottom: 0; }.info-grid span { color: #909399; font-size: 12px; }.info-grid strong { font-size: 14px; }table { width: 100%; border-collapse: collapse; }th,td { height: 42px; padding: 0 14px; border: 1px solid #ebeef5; text-align: left; }th { color: #606266; background: #f5f7fa; font-size: 13px; }.amount-row { display: flex; justify-content: flex-end; gap: 36px; padding: 20px 4px; font-size: 14px; }.amount-row strong { margin-left: 8px; }.confirmed { color: #f56c6c; font-size: 18px; }footer { display: flex; justify-content: space-between; padding-top: 18px; border-top: 1px dashed #dcdfe6; color: #606266; font-size: 13px; }.empty-preview { display: grid; height: 270px; margin-top: 22px; place-items: center; border: 1px dashed #dcdfe6; border-radius: 4px; color: #909399; background: #fafafa; }@media print { .module-heading,.search-panel,.empty-preview { display: none; }.voucher-sheet { margin: 0; border: 0; box-shadow: none; } }
</style>
