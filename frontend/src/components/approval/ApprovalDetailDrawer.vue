<script setup>
import { computed } from 'vue'
import { Document, Download, Money, Paperclip, User } from '@element-plus/icons-vue'
import { ACTION_META, formatDateTime } from '../../constants/approval'
import StatusBadge from './StatusBadge.vue'

const props = defineProps({ modelValue: Boolean, detail: Object, loading: Boolean, role: String })
const emit = defineEmits(['update:modelValue', 'review', 'cancel', 'edit'])
const application = computed(() => props.detail?.application || {})
const hasReviewActions = computed(() => Boolean(props.detail?.allowedActions?.length))
const canCancel = computed(() => props.role === 'SCHOOL'
  && ['APPROVED', 'CONFIRM_PENDING', 'COMPLETED'].includes(application.value.status))
const canEdit = computed(() => (props.detail?.editableFields || []).some((field) => field !== 'finalSubsidyAmount'))
const arrearsItems = computed(() => Array.isArray(props.detail?.arrearsDetail)
  ? props.detail.arrearsDetail
  : (props.detail?.arrearsDetail?.items || []))

function requestCancellation() {
  emit('cancel', {
    ...application.value,
    version: props.detail?.version ?? application.value.version,
  })
}
</script>

<template>
  <el-drawer :model-value="modelValue" size="min(760px, 96vw)" class="detail-drawer" @update:model-value="emit('update:modelValue', $event)">
    <template #header><div class="drawer-heading"><div><span class="drawer-kicker">申请详情</span><h2>{{ application.studentName || '加载中' }}</h2></div><StatusBadge v-if="application.status" :status="application.status" /></div></template>
    <div v-loading="loading" class="drawer-content">
      <template v-if="detail">
        <section class="detail-hero">
          <div class="student-avatar"><User /></div><div class="student-primary"><strong>{{ application.studentName }}</strong><span>{{ application.studentNo }} · {{ application.gender }}</span></div>
          <dl><div><dt>学院专业</dt><dd>{{ application.collegeName }} · {{ application.majorName }}</dd></div><div><dt>年级班级</dt><dd>{{ application.gradeName }} · {{ application.className }}</dd></div><div><dt>申请编号</dt><dd>{{ application.applicationNo }}</dd></div><div><dt>提交时间</dt><dd>{{ formatDateTime(application.submitTime) }}</dd></div></dl>
        </section>
        <section class="detail-section"><div class="section-title"><Document /><h3>申请说明</h3></div><p class="reason-copy">{{ application.applicationReason }}</p></section>
        <section v-if="detail.arrearsDetail || detail.subsidyDetail" class="detail-section"><div class="section-title"><Money /><h3>金额信息</h3></div><div class="money-card"><div><span>学生申报金额</span><strong>¥{{ application.declaredAmount?.toLocaleString() }}</strong></div><div v-if="detail.arrearsDetail"><span>欠费项目</span><strong>{{ arrearsItems.map(item => item.feeItemName || item.name).join('、') }}</strong></div><div v-else><span>补助类型</span><strong>{{ application.applicationTypeName }}</strong></div></div></section>
        <section class="detail-section"><div class="section-title"><Paperclip /><h3>申请附件</h3><span>{{ detail.attachments.length }} 份</span></div><div class="attachment-list"><button v-for="file in detail.attachments" :key="file.id" type="button"><Document /><span><strong>{{ file.fileName }}</strong><small>{{ file.fileSize }}</small></span><Download /></button></div></section>
        <section class="detail-section"><div class="section-title"><h3>审核流程</h3></div><div class="flow-timeline"><div v-for="record in detail.approvalRecords" :key="record.id" class="flow-item"><i :class="`tone-${ACTION_META[record.action]?.tone || 'info'}`"></i><div><strong>{{ ACTION_META[record.action]?.label || record.action }}</strong><p>{{ record.comment }}</p><span>{{ record.approverName }} · {{ formatDateTime(record.createTime) }}</span></div></div><div v-if="detail.allowedActions.length" class="flow-item current"><i></i><div><strong>等待当前层级审核</strong><span>请核对材料后提交结论</span></div></div></div></section>
      </template>
    </div>
    <template #footer><div class="drawer-footer"><span v-if="hasReviewActions">当前版本 v{{ detail.version }}</span><span v-else-if="canCancel">该申请已生效，可由学校执行取消</span><span v-else>该申请在当前角色下仅可查看</span><div class="table-actions"><el-button v-if="canEdit" plain type="primary" @click="emit('edit')">修改允许字段</el-button><el-button v-if="canCancel" type="danger" plain @click="requestCancellation">取消申请</el-button><el-button v-if="hasReviewActions" type="primary" @click="emit('review', application)">开始审核</el-button></div></div></template>
  </el-drawer>
</template>
