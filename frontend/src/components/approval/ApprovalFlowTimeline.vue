<script setup>
import { computed } from 'vue'
import { Check, Clock, Close } from '@element-plus/icons-vue'
import { ACTION_META, formatDateTime } from '../../constants/approval'

const props = defineProps({ application: { type: Object, default: () => ({}) }, records: { type: Array, default: () => [] } })

const definitions = [
  { level: 'STUDENT', title: '学生提交申请', waiting: '等待学生提交申请' },
  { level: 'COUNSELOR', title: '辅导员审核', waiting: '等待辅导员审核' },
  { level: 'COLLEGE', title: '学院审核', waiting: '等待学院审核' },
  { level: 'SCHOOL', title: '学校审核', waiting: '等待学校审核' },
]
const levelOrder = Object.fromEntries(definitions.map((item, index) => [item.level, index]))

const nodes = computed(() => {
  const currentIndex = levelOrder[props.application.currentLevel] ?? levelOrder[statusLevel(props.application.status)] ?? 0
  return definitions.map((definition, index) => {
    const records = props.records.filter(record => record.approvalLevel === definition.level)
    const record = records.at(-1)
    const failed = record && ['RETURN', 'REJECT', 'CANCEL'].includes(record.action)
    const completed = record && ['SUBMIT', 'APPROVE'].includes(record.action)
    return {
      ...definition,
      record,
      state: failed ? 'failed' : completed ? 'completed' : index === currentIndex ? 'current' : index < currentIndex ? 'completed' : 'waiting',
    }
  })
})

function statusLevel(status) {
  if (String(status || '').startsWith('COUNSELOR')) return 'COUNSELOR'
  if (String(status || '').startsWith('COLLEGE')) return 'COLLEGE'
  if (String(status || '').startsWith('SCHOOL')) return 'SCHOOL'
  if (['APPROVED', 'CONFIRM_PENDING', 'COMPLETED', 'CANCELLED'].includes(status)) return 'SCHOOL'
  return 'STUDENT'
}
</script>

<template>
  <div class="approval-flow-card">
    <div v-for="(node, index) in nodes" :key="node.level" class="approval-flow-node" :class="`is-${node.state}`">
      <div class="node-rail"><span class="node-icon"><Check v-if="node.state === 'completed'"/><Close v-else-if="node.state === 'failed'"/><Clock v-else/></span><i v-if="index < nodes.length - 1"/></div>
      <div class="node-content">
        <div class="node-heading"><strong>{{ node.title }}</strong><el-tag v-if="node.record" size="small" :type="node.state === 'failed' ? 'danger' : 'success'">{{ ACTION_META[node.record.action]?.label || node.record.action }}</el-tag><el-tag v-else-if="node.state === 'current'" size="small" type="warning">进行中</el-tag><span>{{ node.record ? formatDateTime(node.record.createTime) : '—' }}</span></div>
        <p v-if="node.record?.comment">{{ node.record.comment }}</p><p v-else>{{ node.waiting }}</p>
        <small v-if="node.record">经办人：{{ node.record.approverName || '系统' }}</small>
      </div>
    </div>
  </div>
</template>

<style scoped>
.approval-flow-card { padding: 8px 4px; }
.approval-flow-node { display:grid; grid-template-columns:40px 1fr; min-height:112px; }
.node-rail { position:relative; display:flex; justify-content:center; }
.node-rail i { position:absolute; top:34px; bottom:0; width:2px; background:#dbe3ef; }
.node-icon { z-index:1; display:grid; place-items:center; width:30px; height:30px; border-radius:50%; color:#8a98aa; background:#eef2f7; border:2px solid #dbe3ef; }
.node-icon svg { width:15px; }
.node-content { margin:0 0 20px 12px; padding:15px 18px; border:1px solid #e5eaf1; border-radius:8px; background:#fff; box-shadow:0 4px 14px rgba(30,55,90,.05); }
.node-heading { display:flex; align-items:center; gap:10px; }.node-heading strong { font-size:15px; color:#26364a; }.node-heading span:last-child { margin-left:auto; color:#8b97a8; font-size:12px; }
.node-content p { margin:9px 0 5px; color:#536174; line-height:1.6; }.node-content small { color:#8b97a8; }
.is-completed .node-icon { color:#fff; background:#28b482; border-color:#28b482; }.is-completed .node-rail i { background:#86d9bb; }
.is-current .node-icon { color:#fff; background:#409eff; border-color:#409eff; box-shadow:0 0 0 5px #e8f3ff; }.is-current .node-content { border-color:#b9d9ff; background:#f7fbff; }
.is-failed .node-icon { color:#fff; background:#ef6464; border-color:#ef6464; }.is-failed .node-content { border-color:#ffc7c7; background:#fff8f8; }
</style>
