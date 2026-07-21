<script setup>
import { computed, reactive, watch } from 'vue'

/**
 * 页面 8 的业务确认弹窗。
 *
 * 该组件只负责成员四拥有的“执行型业务确认”，不承担成员三的三级审核：
 * - 最终审核继续直接使用成员三维护的 ReviewDialog；
 * - 欠费确认、线下补录、取消申请和单据作废统一使用本组件。
 *
 * 当前页面已经接通欠费确认和线下补录。取消申请、单据作废在后端接口合入后，
 * 只需传入对应 mode 和 business，并监听 confirm，无需再设计另一套弹窗。
 */
const props = defineProps({
  modelValue: Boolean,
  mode: {
    type: String,
    default: 'ARREARS_CONFIRM',
  },
  business: {
    type: Object,
    default: () => ({}),
  },
  submitting: Boolean,
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const form = reactive({
  confirmedAmount: '',
  reason: '',
})

const modeConfig = computed(() => ({
  ARREARS_CONFIRM: {
    title: '欠费最终确认',
    description: '核对学生与欠费金额，确认后生成欠费单据并完成申请。',
    confirmText: '确认欠费金额',
    result: '申请变为已完成，并生成可查询、可打印的欠费单据。',
  },
  SUPPLEMENT: {
    title: '确认线下补录',
    description: '核对学生和线下办理内容，确认后写入补录记录。',
    confirmText: '确认提交补录',
    result: props.business.containsArrears
      ? '补录申请进入待欠费确认。'
      : '补录申请直接变为已完成。',
  },
  CANCEL_APPLICATION: {
    title: '确认取消申请',
    description: '确认取消后，该申请不再进入后续审核。',
    confirmText: '确认取消申请',
    result: '申请变为已取消，相关资源按后端规则释放。',
  },
  VOID_VOUCHER: {
    title: '确认作废单据',
    description: '核对单据和作废原因，确认后保留历史记录并标记作废。',
    confirmText: '确认作废单据',
    result: '单据保留历史记录并变为已作废。',
  },
})[props.mode] ?? {})

const needsAmount = computed(() => props.mode === 'ARREARS_CONFIRM')
const needsReason = computed(() => ['CANCEL_APPLICATION', 'VOID_VOUCHER'].includes(props.mode))
const displayAmount = computed(() => props.business.appliedAmount
  ?? props.business.declaredAmount
  ?? props.business.subsidyAmount)
const submitDisabled = computed(() => {
  if (needsAmount.value && !form.confirmedAmount) return true
  return needsReason.value && !form.reason.trim()
})

watch(() => props.modelValue, (open) => {
  if (!open) return
  form.confirmedAmount = displayAmount.value ?? ''
  form.reason = ''
})

function close() {
  emit('update:modelValue', false)
}

/**
 * 组件只提交页面当前动作需要的字段；applicationId、version 和 requestId
 * 继续由调用页面按各自后端接口契约组装，避免四种业务互相污染请求格式。
 */
function submit() {
  if (submitDisabled.value) return
  emit('confirm', {
    confirmedAmount: needsAmount.value ? Number(form.confirmedAmount) : null,
    reason: form.reason.trim(),
  })
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    width="600px"
    class="business-confirm-dialog"
    :close-on-click-modal="false"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <template #header>
      <div class="dialog-heading">
        <span>{{ modeConfig.title }}</span>
        <small>{{ business.studentName }}（{{ business.studentNo }}） · {{ business.applicationNo ?? business.voucherNo }}</small>
      </div>
    </template>

    <p class="dialog-description">{{ modeConfig.description }}</p>

    <div class="business-summary">
      <div>
        <span>学生</span>
        <strong>{{ business.studentName || '—' }}</strong>
      </div>
      <div>
        <span>业务类型</span>
        <strong>{{ business.applicationTypeName ?? business.applicationType ?? '绿色通道' }}</strong>
      </div>
      <div>
        <span>申报金额</span>
        <strong>{{ displayAmount == null ? '—' : `¥${Number(displayAmount).toLocaleString()}` }}</strong>
      </div>
    </div>

    <el-descriptions :column="2" border class="business-detail">
      <el-descriptions-item label="学号">{{ business.studentNo || '—' }}</el-descriptions-item>
      <el-descriptions-item label="学院">{{ business.collegeName || '—' }}</el-descriptions-item>
      <el-descriptions-item label="申请编号">{{ business.applicationNo || '—' }}</el-descriptions-item>
      <el-descriptions-item label="当前状态">{{ business.statusName ?? business.status ?? '—' }}</el-descriptions-item>
      <el-descriptions-item v-if="mode === 'SUPPLEMENT'" label="补录原因" :span="2">
        {{ business.supplementReason || '—' }}
      </el-descriptions-item>
      <el-descriptions-item v-if="mode === 'SUPPLEMENT'" label="线下办理时间" :span="2">
        {{ business.handledAt || '—' }}
      </el-descriptions-item>
    </el-descriptions>

    <el-form label-width="112px" class="business-form">
      <el-form-item v-if="needsAmount" label="实际确认金额" required>
        <el-input-number
          v-model="form.confirmedAmount"
          :min="0.01"
          :precision="2"
          controls-position="right"
        />
        <span class="field-hint">申报金额 ¥{{ Number(displayAmount ?? 0).toLocaleString() }}</span>
      </el-form-item>
      <el-form-item v-if="needsReason" :label="mode === 'VOID_VOUCHER' ? '作废原因' : '取消原因'" required>
        <el-input v-model="form.reason" type="textarea" :rows="4" maxlength="300" show-word-limit />
      </el-form-item>
    </el-form>

    <el-alert :title="modeConfig.result" type="info" :closable="false" show-icon />

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="submitting" :disabled="submitDisabled" @click="submit">
        {{ modeConfig.confirmText }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.dialog-heading { display: grid; gap: 5px; }
.dialog-heading span { color: #1f2937; font-size: 19px; font-weight: 700; }
.dialog-heading small { color: #7c8798; font-size: 13px; font-weight: 400; }
.dialog-description { margin: 0 0 16px; color: #667085; line-height: 1.7; }
.business-summary { display: grid; grid-template-columns: repeat(3, 1fr); margin-bottom: 18px; overflow: hidden; border: 1px solid #dfe5ec; border-radius: 6px; }
.business-summary div { display: grid; gap: 7px; padding: 14px 16px; border-right: 1px solid #dfe5ec; background: #f8fafc; }
.business-summary div:last-child { border-right: 0; }
.business-summary span { color: #7c8798; font-size: 12px; }
.business-summary strong { color: #253044; font-size: 15px; }
.business-detail { margin-bottom: 18px; }
.business-form { margin-top: 20px; }
.field-hint { margin-left: 12px; color: #8a94a4; font-size: 12px; }
</style>
