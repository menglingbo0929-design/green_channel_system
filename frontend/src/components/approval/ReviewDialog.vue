<script setup>
import { computed, reactive, watch } from 'vue'
import { CircleCheck, RefreshLeft, Warning } from '@element-plus/icons-vue'
import { createRequestId } from '../../constants/approval'

const props = defineProps({ modelValue: Boolean, application: Object, role: String, submitting: Boolean })
const emit = defineEmits(['update:modelValue', 'submit'])
const form = reactive({ action: 'APPROVE', comment: '', finalSubsidyAmount: null })
const isSubsidy = computed(() => props.application?.applicationType !== 'GREEN_CHANNEL')
const needsComment = computed(() => ['RETURN', 'REJECT'].includes(form.action))
const availableQuota = computed(() => {
  const value = props.application?.applicationType === 'GREEN_CHANNEL'
    ? props.application?.availableGiftQuota
    : props.application?.availableSubsidyAmount
  return value == null ? null : Number(value)
})
const resourceHint = computed(() => {
  if (availableQuota.value == null || Number.isNaN(availableQuota.value)) {
    return '资源名额与额度由后端在提交时进行最终校验'
  }
  return props.application?.applicationType === 'GREEN_CHANNEL'
    ? `当前可用礼包名额：${availableQuota.value} 个`
    : `当前可用额度：¥${availableQuota.value.toLocaleString()}`
})
const amountInvalid = computed(() => props.role === 'COUNSELOR' && isSubsidy.value && form.action === 'APPROVE'
  && (!form.finalSubsidyAmount
    || form.finalSubsidyAmount > props.application?.declaredAmount
    || (availableQuota.value != null && form.finalSubsidyAmount > availableQuota.value)))
const submitDisabled = computed(() => (needsComment.value && !form.comment.trim()) || amountInvalid.value)

watch(() => props.modelValue, (open) => {
  if (open) Object.assign(form, { action: 'APPROVE', comment: '', finalSubsidyAmount: props.application?.declaredAmount || null })
})

function submit() {
  if (submitDisabled.value) return
  emit('submit', {
    action: form.action,
    comment: form.comment.trim(),
    finalSubsidyAmount: props.role === 'COUNSELOR' && isSubsidy.value && form.action === 'APPROVE' ? Number(form.finalSubsidyAmount) : null,
    version: props.application.version,
    requestId: createRequestId(),
  })
}
</script>

<template>
  <el-dialog :model-value="modelValue" width="600px" class="review-dialog" :close-on-click-modal="false" @update:model-value="emit('update:modelValue', $event)">
    <template #header>
      <div class="dialog-heading"><span>统一审核</span><small>{{ application?.studentName }}（{{ application?.studentNo }}） · {{ application?.applicationNo }}</small></div>
    </template>

    <div class="review-summary">
      <div><span>申请类型</span><strong>{{ application?.applicationTypeName }}</strong></div>
      <div><span>申请金额</span><strong>¥{{ application?.declaredAmount?.toLocaleString() }}</strong></div>
      <div><span>当前审核层级</span><strong>{{ role === 'COUNSELOR' ? '辅导员审核' : role === 'COLLEGE' ? '学院审核' : '学校审核' }}</strong></div>
    </div>

    <div class="dialog-section-label"><span class="required-star">*</span>审核动作</div>
    <div class="review-action-grid">
      <button type="button" class="action-approve" :class="{ selected: form.action === 'APPROVE' }" @click="form.action = 'APPROVE'"><CircleCheck /><strong>通过</strong><span>材料符合要求</span></button>
      <button type="button" class="action-return" :class="{ selected: form.action === 'RETURN' }" @click="form.action = 'RETURN'"><RefreshLeft /><strong>退回</strong><span>允许学生修改</span></button>
      <button type="button" class="action-reject" :class="{ selected: form.action === 'REJECT' }" @click="form.action = 'REJECT'"><Warning /><strong>不通过</strong><span>终止本次申请</span></button>
    </div>

    <el-alert
      v-if="form.action === 'APPROVE'"
      class="quota-alert"
      :title="resourceHint"
      type="info"
      :closable="false"
      show-icon
    />

    <el-form label-width="112px" class="review-form">
      <el-form-item v-if="role === 'COUNSELOR' && isSubsidy && form.action === 'APPROVE'" label="最终补助金额" required :error="amountInvalid ? '金额须大于 0，且不得超过申请金额或当前可用额度' : ''">
        <el-input-number v-model="form.finalSubsidyAmount" :min="1" :max="application?.declaredAmount" :precision="2" controls-position="right" />
        <span class="field-hint">申请金额 ¥{{ application?.declaredAmount?.toLocaleString() }}</span>
      </el-form-item>
      <el-form-item :label="form.action === 'RETURN' ? '退回原因' : form.action === 'REJECT' ? '不通过原因' : '审核意见'" :required="needsComment" :error="needsComment && !form.comment.trim() ? '退回或不通过时必须填写清晰原因' : ''">
        <el-input v-model="form.comment" type="textarea" :rows="4" maxlength="300" show-word-limit :placeholder="needsComment ? '请填写学生可理解、可执行的处理原因' : '可填写审核意见（选填）'" />
      </el-form-item>
    </el-form>

    <div class="concurrency-note">提交时将校验申请版本 v{{ application?.version }}；若数据已被他人处理，请刷新详情后重新操作。</div>
    <template #footer><el-button @click="emit('update:modelValue', false)">取消</el-button><el-button type="primary" :loading="submitting" :disabled="submitDisabled" @click="submit">确认提交</el-button></template>
  </el-dialog>
</template>
