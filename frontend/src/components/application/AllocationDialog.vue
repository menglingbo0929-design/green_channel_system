<script setup>
defineProps({ modelValue: { type: Boolean, default: false } })
const emit = defineEmits(['update:modelValue'])
</script>

<template>
  <el-dialog :model-value="modelValue" title="名额 / 额度分配" width="560px" class="business-dialog" @update:model-value="emit('update:modelValue', $event)">
    <el-alert type="warning" :closable="false" show-icon title="分配接口与真实资源数据尚未接入">
      当前后端尚未提供批次资源、库存、预占量、已使用量及上下级分配接口；本弹窗保留统一交互位置，不展示静态余额或模拟使用率。
    </el-alert>
    <template #footer><el-button @click="emit('update:modelValue', false)">关闭</el-button></template>
  </el-dialog>
</template>
