<script setup>
defineProps({
  modelValue: { type: Boolean, default: false },
  application: { type: Object, default: null },
  submitting: { type: Boolean, default: false },
  available: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue', 'confirm'])
</script>

<template>
  <el-dialog :model-value="modelValue" title="提交申请确认" width="520px" class="business-dialog" @update:model-value="emit('update:modelValue', $event)">
    <el-alert v-if="!available" type="warning" :closable="false" show-icon title="正式提交接口尚未接入">
      当前仅支持保存草稿和维护欠费明细。提交动作将等待成员一的身份/批次服务与成员三的审核流转服务完成联调后开放。
    </el-alert>
    <template v-else>
      <p class="dialog-tip">提交后申请将进入审核流程，草稿内容不能再由学生直接修改。</p>
      <dl class="submit-summary"><div><dt>申请类型</dt><dd>{{ application?.applicationType }}</dd></div><div><dt>申请编号</dt><dd>{{ application?.applicationNo || '待生成' }}</dd></div></dl>
    </template>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :disabled="!available" :loading="submitting" @click="emit('confirm')">确认提交</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.dialog-tip { margin: 0; color: #4b5563; }
.submit-summary { margin: 16px 0 0; padding: 12px 16px; display: grid; grid-template-columns: 1fr 1fr; gap: 16px; border: 1px solid #e5e7eb; background: #f8fafc; }
dt { color: #6b7280; font-size: 12px; } dd { margin: 2px 0 0; color: #1f2937; font-weight: 600; }
</style>
