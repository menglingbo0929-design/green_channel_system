<script setup>
import { computed, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({ modelValue: Boolean, detail: Object, submitting: Boolean })
const emit = defineEmits(['update:modelValue', 'submit'])
const application = computed(() => props.detail?.application || {})
const editable = computed(() => new Set(props.detail?.editableFields || []))
const form = reactive({ applicationReason: '', arrearsItems: [], giftItems: [], expectedSubsidyAmount: null, comment: '' })
let baseline = null

const arrearsSource = () => {
  const detail = props.detail?.arrearsDetail
  return Array.isArray(detail) ? detail : (detail?.items || [])
}
const giftSource = () => {
  const detail = props.detail?.giftDetail
  return Array.isArray(detail) ? detail : (detail?.items || [])
}

function snapshot() {
  return {
    applicationReason: application.value.applicationReason || '',
    arrearsItems: arrearsSource().map((item) => ({
      feeItemId: item.feeItemId ?? null,
      feeItemName: item.feeItemName || item.name || '欠费项目',
      declaredAmount: Number(item.declaredAmount ?? item.amount ?? 0),
      arrearsReasonCode: item.arrearsReasonCode ?? null,
    })),
    giftItems: giftSource().filter((item) => typeof item === 'object').map((item) => ({
      batchGiftItemId: item.batchGiftItemId ?? null,
      itemName: item.itemName || item.name || '礼包物品',
      quantity: Number(item.quantity || 1),
    })),
    expectedSubsidyAmount: Number(props.detail?.subsidyDetail?.expectedAmount
      ?? props.detail?.subsidyDetail?.requestedAmount ?? 0) || null,
  }
}

watch(() => props.modelValue, (open) => {
  if (!open) return
  baseline = snapshot()
  Object.assign(form, {
    applicationReason: baseline.applicationReason,
    arrearsItems: baseline.arrearsItems.map((item) => ({ ...item })),
    giftItems: baseline.giftItems.map((item) => ({ ...item })),
    expectedSubsidyAmount: baseline.expectedSubsidyAmount,
    comment: '',
  })
})

function changed(left, right) {
  return JSON.stringify(left) !== JSON.stringify(right)
}

function submit() {
  const fields = {}
  if (editable.value.has('applicationReason') && form.applicationReason.trim() !== baseline.applicationReason) {
    fields.applicationReason = form.applicationReason.trim()
  }
  const arrearsItems = form.arrearsItems.map(({ feeItemId, declaredAmount, arrearsReasonCode }) => ({ feeItemId, declaredAmount, arrearsReasonCode }))
  const baselineArrears = baseline.arrearsItems.map(({ feeItemId, declaredAmount, arrearsReasonCode }) => ({ feeItemId, declaredAmount, arrearsReasonCode }))
  if (editable.value.has('arrearsItems') && changed(arrearsItems, baselineArrears)) fields.arrearsItems = arrearsItems
  const giftItems = form.giftItems.map(({ batchGiftItemId, quantity }) => ({ batchGiftItemId, quantity }))
  const baselineGifts = baseline.giftItems.map(({ batchGiftItemId, quantity }) => ({ batchGiftItemId, quantity }))
  if (editable.value.has('giftItems') && changed(giftItems, baselineGifts)) fields.giftItems = giftItems
  if (editable.value.has('expectedSubsidyAmount')
    && Number(form.expectedSubsidyAmount) !== Number(baseline.expectedSubsidyAmount)) {
    fields.expectedSubsidyAmount = Number(form.expectedSubsidyAmount)
  }
  if (!Object.keys(fields).length) return ElMessage.warning('请先修改至少一个允许字段')
  if (!form.comment.trim()) return ElMessage.warning('请填写修改说明')
  emit('submit', { fields, comment: form.comment.trim() })
}
</script>

<template>
  <el-dialog :model-value="modelValue" title="修改审核允许字段" width="min(680px, 94vw)" destroy-on-close @update:model-value="emit('update:modelValue', $event)">
    <el-alert title="学生身份、组织、批次、申请来源和主键不可修改；所有变更都会写入审核记录。" type="info" :closable="false" show-icon />
    <el-form label-position="top" class="editable-fields-form">
      <el-form-item v-if="editable.has('applicationReason')" label="申请说明">
        <el-input v-model="form.applicationReason" type="textarea" :rows="3" maxlength="1000" show-word-limit />
      </el-form-item>
      <template v-if="editable.has('arrearsItems') && form.arrearsItems.length">
        <div class="field-group-title">欠费明细</div>
        <el-form-item v-for="item in form.arrearsItems" :key="item.feeItemId" :label="item.feeItemName">
          <el-input-number v-model="item.declaredAmount" :min="0.01" :max="8000" :precision="2" controls-position="right" />
        </el-form-item>
      </template>
      <template v-if="editable.has('giftItems') && form.giftItems.length">
        <div class="field-group-title">礼包明细</div>
        <el-form-item v-for="item in form.giftItems" :key="item.batchGiftItemId" :label="item.itemName">
          <el-input-number v-model="item.quantity" :min="1" :precision="0" controls-position="right" />
        </el-form-item>
      </template>
      <el-form-item v-if="editable.has('expectedSubsidyAmount')" label="申请补助金额">
        <el-input-number v-model="form.expectedSubsidyAmount" :min="0.01" :precision="2" controls-position="right" />
      </el-form-item>
      <el-form-item label="修改说明" required>
        <el-input v-model="form.comment" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="说明核对依据和修改原因" />
      </el-form-item>
    </el-form>
    <template #footer><el-button @click="emit('update:modelValue', false)">取消</el-button><el-button type="primary" :loading="submitting" @click="submit">保存并记录审计</el-button></template>
  </el-dialog>
</template>

<style scoped>
.editable-fields-form { margin-top: 18px; }
.field-group-title { margin: 8px 0 12px; color: #303133; font-weight: 600; }
</style>
