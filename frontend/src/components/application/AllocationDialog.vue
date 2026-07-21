<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { resourceConfigAPI } from '../../api/application.js'

const props = defineProps({
  modelValue: { type: Boolean, default: false }, batchId: { type: Number, default: null },
  colleges: { type: Array, default: () => [] }, grades: { type: Array, default: () => [] },
  editing: { type: Object, default: null },
})
const emit = defineEmits(['update:modelValue', 'saved'])
const saving = ref(false)
const form = reactive({ resource: 'GIFT', scope: 'COLLEGE', targetId: null, total: null, version: null })
const targets = computed(() => form.scope === 'COLLEGE' ? props.colleges : props.grades)
const targetLabel = (item) => item.collegeName || item.gradeName

watch(() => props.modelValue, (open) => {
  if (!open) return
  const row = props.editing
  Object.assign(form, row ? {
    resource: row.resource, scope: row.scope, targetId: row.targetId,
    total: row.resource === 'GIFT' ? row.quotaTotal : row.quotaAmount, version: row.version,
  } : { resource: 'GIFT', scope: 'COLLEGE', targetId: null, total: null, version: null })
})
watch(() => form.scope, () => { if (!props.editing) form.targetId = null })
function close() { emit('update:modelValue', false) }
function errorMessage(error) { return error.response?.data?.message || error.message || '保存失败' }
async function save() {
  if (!props.batchId || !form.targetId || !form.total) return ElMessage.warning('请填写批次 ID、分配对象和总额度')
  saving.value = true
  try {
    const api = form.resource === 'GIFT' ? resourceConfigAPI : resourceConfigAPI
    if (props.editing) {
      const data = form.resource === 'GIFT' ? { quotaTotal: form.total, version: form.version } : { quotaAmount: form.total, version: form.version }
      if (form.resource === 'GIFT') await api.updateGiftQuota(props.editing.id, form.scope, data)
      else await api.updateSubsidyQuota(props.editing.id, form.scope, data)
    } else {
      if (form.resource === 'GIFT') await api.createGiftQuota({ batchId: props.batchId, scope: form.scope, targetId: form.targetId, quotaTotal: form.total })
      else await api.createSubsidyQuota({ batchId: props.batchId, scope: form.scope, targetId: form.targetId, quotaAmount: form.total })
    }
    ElMessage.success('分配已保存'); emit('saved'); close()
  } catch (error) { ElMessage.error(errorMessage(error)) } finally { saving.value = false }
}
</script>

<template>
  <el-dialog :model-value="modelValue" :title="editing ? '编辑名额 / 额度分配' : '新增名额 / 额度分配'" width="560px" class="business-dialog" @update:model-value="emit('update:modelValue', $event)">
    <el-alert type="info" :closable="false" show-icon title="批次、学院和年级均由成员一的查询服务校验" />
    <el-form class="allocation-form" label-width="100px">
      <el-form-item label="资源类型"><el-radio-group v-model="form.resource" :disabled="!!editing"><el-radio-button value="GIFT">礼包名额</el-radio-button><el-radio-button value="SUBSIDY">补助额度</el-radio-button></el-radio-group></el-form-item>
      <el-form-item label="分配维度"><el-radio-group v-model="form.scope" :disabled="!!editing"><el-radio-button value="COLLEGE">学院</el-radio-button><el-radio-button value="GRADE">年级</el-radio-button></el-radio-group></el-form-item>
      <el-form-item :label="form.scope === 'COLLEGE' ? '学院' : '年级'" required><el-select v-model="form.targetId" filterable :disabled="!!editing" placeholder="请选择"><el-option v-for="item in targets" :key="item.id" :value="item.id" :label="targetLabel(item)"/></el-select></el-form-item>
      <el-form-item :label="form.resource === 'GIFT' ? '名额总数' : '额度总额'" required><el-input-number v-model="form.total" :min="form.resource === 'GIFT' ? 1 : 0.01" :precision="form.resource === 'GIFT' ? 0 : 2" :step="form.resource === 'GIFT' ? 1 : 100"/></el-form-item>
    </el-form>
    <template #footer><el-button @click="close">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
  </el-dialog>
</template>

<style scoped>.allocation-form { margin-top: 18px; }</style>
