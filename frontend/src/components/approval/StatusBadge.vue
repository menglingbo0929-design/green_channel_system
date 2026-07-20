<script setup>
import { computed } from 'vue'
import { STATUS_META } from '../../constants/approval'
const props = defineProps({ status: { type: String, required: true } })
const meta = computed(() => STATUS_META[props.status] || { label: props.status, tone: 'neutral' })
</script>
<template><span class="status-badge" :class="`tone-${meta.tone}`"><i></i>{{ meta.label }}</span></template>
