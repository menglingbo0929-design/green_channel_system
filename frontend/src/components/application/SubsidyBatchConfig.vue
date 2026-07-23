<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { batchAPI, resourceConfigAPI } from '../../api/application.js'
import SubsidyQuotaConfig from './SubsidyQuotaConfig.vue'

const batches = ref([])
const grades = ref([])
const loading = ref(false)
const dialogOpen = ref(false)
const editing = ref(null)
const form = reactive({ batchCode: '', batchName: '', academicYear: '', batchType: 'LIVING_SUBSIDY', startTime: '', endTime: '', status: 'DRAFT', remark: '', eligibleGradeIds: [] })

function errorMessage(error) { return error.response?.data?.message || error.message || '操作失败' }
function typeName(type) { return type === 'LIVING_SUBSIDY' ? '生活补助' : '路费补助' }
function statusName(row) { return !row.enabled ? '已停用' : ({ DRAFT: '草稿', OPEN: '开放申请', CLOSED: '已关闭' }[row.status] || row.status) }
function gradeName(id) { const grade = grades.value.find(item => item.id === id); return grade?.gradeName || grade?.name || `年级 #${id}` }
async function load() {
  loading.value = true
  try {
    const [batchData, gradeData] = await Promise.all([batchAPI.listSubsidy(), resourceConfigAPI.grades()])
    batches.value = batchData || []; grades.value = gradeData || []
  } catch (error) { ElMessage.error(errorMessage(error)) } finally { loading.value = false }
}
function open(row) {
  editing.value = row || null
  Object.assign(form, row ? { batchCode: row.batchCode, batchName: row.batchName, academicYear: row.academicYear, batchType: row.batchType, startTime: row.startTime, endTime: row.endTime, status: row.status, remark: row.remark || '', eligibleGradeIds: row.eligibleGradeIds || [] } : { batchCode: '', batchName: '', academicYear: `${new Date().getFullYear()}-${new Date().getFullYear() + 1}`, batchType: 'LIVING_SUBSIDY', startTime: '', endTime: '', status: 'DRAFT', remark: '', eligibleGradeIds: [] })
  dialogOpen.value = true
}
async function save() {
  if (!form.batchCode.trim() || !form.batchName.trim() || !form.academicYear.trim() || !form.startTime || !form.endTime || !form.eligibleGradeIds.length) return ElMessage.warning('请完整填写批次信息，并选择适用年级')
  try {
    if (editing.value) await batchAPI.updateSubsidy(editing.value.id, form)
    else await batchAPI.createSubsidy(form)
    ElMessage.success('补助批次已保存'); dialogOpen.value = false; await load()
  } catch (error) { ElMessage.error(errorMessage(error)) }
}
async function toggle(row) {
  try { await batchAPI.toggleSubsidy(row.id); ElMessage.success(row.enabled ? '批次已停用' : '批次已启用'); await load() }
  catch (error) { ElMessage.error(errorMessage(error)) }
}
onMounted(load)
</script>

<template>
  <div class="subsidy-batch-config" v-loading="loading">
    <div class="config-toolbar"><span>为生活补助、路费补助分别设置申请期和适用年级；状态设为“开放申请”且启用后，学生端才能新建申请。</span><div><el-button @click="load">刷新</el-button><el-button type="primary" @click="open()">新增补助批次</el-button></div></div>
    <el-table :data="batches" border class="standard-table" empty-text="暂无补助批次"><el-table-column prop="batchCode" label="批次编号" min-width="150"/><el-table-column prop="batchName" label="批次名称" min-width="180"/><el-table-column label="补助类型" width="120"><template #default="{row}">{{ typeName(row.batchType) }}</template></el-table-column><el-table-column prop="academicYear" label="学年" width="110"/><el-table-column label="申请时间" min-width="220"><template #default="{row}">{{ String(row.startTime).replace('T',' ') }}<br/>至 {{ String(row.endTime).replace('T',' ') }}</template></el-table-column><el-table-column label="适用年级" min-width="160"><template #default="{row}">{{ (row.eligibleGradeIds || []).map(gradeName).join('、') || '—' }}</template></el-table-column><el-table-column label="状态" width="110"><template #default="{row}"><el-tag :type="row.status === 'OPEN' && row.enabled ? 'success' : 'info'">{{ statusName(row) }}</el-tag></template></el-table-column><el-table-column label="操作" width="150"><template #default="{row}"><el-button link type="primary" @click="open(row)">编辑</el-button><el-button link :type="row.enabled ? 'warning' : 'success'" @click="toggle(row)">{{ row.enabled ? '停用' : '启用' }}</el-button></template></el-table-column></el-table>
    <SubsidyQuotaConfig/>
    <el-dialog v-model="dialogOpen" :title="editing ? '编辑补助批次' : '新增补助批次'" width="680px"><el-form label-width="120px"><el-form-item label="批次编号" required><el-input v-model="form.batchCode" :disabled="!!editing" maxlength="32"/></el-form-item><el-form-item label="批次名称" required><el-input v-model="form.batchName" maxlength="64"/></el-form-item><el-form-item label="补助类型" required><el-radio-group v-model="form.batchType"><el-radio value="LIVING_SUBSIDY">生活补助</el-radio><el-radio value="TRAVEL_SUBSIDY">路费补助</el-radio></el-radio-group></el-form-item><el-form-item label="学年" required><el-input v-model="form.academicYear" placeholder="2026-2027" maxlength="16"/></el-form-item><el-form-item label="申请开始时间" required><el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%"/></el-form-item><el-form-item label="申请截止时间" required><el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%"/></el-form-item><el-form-item label="适用年级" required><el-select v-model="form.eligibleGradeIds" multiple collapse-tags style="width:100%"><el-option v-for="grade in grades" :key="grade.id" :label="grade.gradeName || grade.name" :value="grade.id"/></el-select></el-form-item><el-form-item label="批次状态"><el-radio-group v-model="form.status"><el-radio value="DRAFT">草稿</el-radio><el-radio value="OPEN">开放申请</el-radio><el-radio value="CLOSED">已关闭</el-radio></el-radio-group></el-form-item><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" maxlength="255"/></el-form-item></el-form><template #footer><el-button @click="dialogOpen=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template></el-dialog>
  </div>
</template>

<style scoped>
.config-toolbar { margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; gap: 12px; color: #6b7280; font-size: 12px; }.config-toolbar > div { display:flex; gap:12px; }
</style>
