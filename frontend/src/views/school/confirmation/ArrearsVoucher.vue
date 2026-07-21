<script setup>
import { ref } from 'vue'
import { getVoucher, printVoucher } from '../../../api/voucher'
import { useUserStore } from '../../../stores/user'
const voucherNo = ref('')
const userStore = useUserStore()
const voucher = ref(null)
async function load(print = false) {
  const response = print ? await printVoucher(voucherNo.value, userStore.userId) : await getVoucher(voucherNo.value, userStore.userId)
  voucher.value = response.data.data
  if (print) setTimeout(() => window.print(), 0)
}
</script>
<template><main class="voucher"><h1>{{ voucher?.printTitle ?? '欠费确认单预览' }}</h1>
  <p class="dependency-note">当前页面已接入真实确认单接口；学生、组织、欠费项目快照及权限服务须在演示前完成合入。</p>
  <label>单据编号 <input v-model="voucherNo" placeholder="GC2026000001" /></label>
  <button @click="load()">查询预览</button><button @click="load(true)">获取打印数据并打印</button>
  <section v-if="voucher"><p>单据编号：{{ voucher.voucherNo }}</p><p>学生：{{ voucher.studentNo }} {{ voucher.studentName }}</p><p>学院/专业：{{ voucher.collegeName }} / {{ voucher.majorName }}</p><p>年级/班级：{{ voucher.gradeName }} / {{ voucher.className }}</p><p>申报金额：¥{{ voucher.appliedAmount }}；实际确认：¥{{ voucher.confirmedAmount }}</p><p>确认人：{{ voucher.confirmUserName }}；确认时间：{{ voucher.confirmedTime }}</p><ul><li v-for="item in voucher.arrearsItems" :key="item.feeItemName">{{ item.feeItemName }}：¥{{ item.declaredAmount }}</li></ul></section>
</main></template>
<style scoped>.voucher{max-width:760px;margin:30px auto;text-align:left}.voucher label{display:block;margin:10px 0}.voucher input{padding:8px;margin-left:8px}.voucher button{margin-right:8px;padding:8px 12px}.voucher section{margin-top:20px;padding:20px;border:1px solid #ddd}.dependency-note{padding:10px;border-left:4px solid #d97706;background:#fffbeb;color:#92400e}@media print{.voucher>label,.voucher>button,.dependency-note{display:none}}</style>
