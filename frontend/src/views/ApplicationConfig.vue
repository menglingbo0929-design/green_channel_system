<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import AllocationDialog from '../components/application/AllocationDialog.vue'
import { catalogAPI } from '../api/application.js'

const activeTab = ref('fees')
const loading = ref(false)
const fees = ref([])
const amounts = ref([])
const gifts = ref([])
const feeDialog = ref(false)
const amountDialog = ref(false)
const giftDialog = ref(false)
const allocationOpen = ref(false)
const editingId = ref(null)
const feeForm = reactive({ name: '', enabled: true })
const amountForm = reactive({ feeItemId: null, amount: null, enabled: true })
const giftForm = reactive({ name: '', enabled: true })
const feeOptions = computed(() => fees.value.filter(item => item.enabled))

function errorMessage(error, fallback) { return error.response?.data?.message || error.message || fallback }
function reset(form) { Object.keys(form).forEach(key => { form[key] = key === 'enabled' ? true : null }); if ('name' in form) form.name = '' }

async function loadData() {
  loading.value = true
  try {
    const [feeData, amountData, giftData] = await Promise.all([
      catalogAPI.listFeeItems(), catalogAPI.listFeeAmountOptions(), catalogAPI.listGiftItems(),
    ])
    fees.value = feeData || []
    amounts.value = amountData || []
    gifts.value = giftData || []
  } catch (error) { ElMessage.error(errorMessage(error, '配置数据加载失败')) } finally { loading.value = false }
}

function openFee(row) { editingId.value = row?.id ?? null; Object.assign(feeForm, row ? { name: row.name, enabled: row.enabled } : { name: '', enabled: true }); feeDialog.value = true }
function openAmount(row) { editingId.value = row?.id ?? null; Object.assign(amountForm, row ? { feeItemId: row.feeItemId, amount: row.amount, enabled: row.enabled } : { feeItemId: fees.value.find(item => item.enabled)?.id ?? null, amount: null, enabled: true }); amountDialog.value = true }
function openGift(row) { editingId.value = row?.id ?? null; Object.assign(giftForm, row ? { name: row.name, enabled: row.enabled } : { name: '', enabled: true }); giftDialog.value = true }

async function saveFee() { try { if (!feeForm.name?.trim()) return ElMessage.warning('请输入欠费项目名称'); editingId.value ? await catalogAPI.updateFeeItem(editingId.value, feeForm) : await catalogAPI.createFeeItem(feeForm); ElMessage.success('欠费项目已保存'); feeDialog.value = false; loadData() } catch (error) { ElMessage.error(errorMessage(error, '保存失败')) } }
async function saveAmount() { try { if (!amountForm.feeItemId || !amountForm.amount) return ElMessage.warning('请选择项目并填写金额'); editingId.value ? await catalogAPI.updateFeeAmountOption(editingId.value, amountForm) : await catalogAPI.createFeeAmountOption(amountForm); ElMessage.success('金额档位已保存'); amountDialog.value = false; loadData() } catch (error) { ElMessage.error(errorMessage(error, '保存失败')) } }
async function saveGift() { try { if (!giftForm.name?.trim()) return ElMessage.warning('请输入礼包物品名称'); editingId.value ? await catalogAPI.updateGiftItem(editingId.value, giftForm) : await catalogAPI.createGiftItem(giftForm); ElMessage.success('礼包物品已保存'); giftDialog.value = false; loadData() } catch (error) { ElMessage.error(errorMessage(error, '保存失败')) } }
async function remove(kind, row) {
  try { await ElMessageBox.confirm(`确认删除“${row.name || `¥${row.amount}`}”吗？`, '删除确认', { type: 'warning' }); if (kind === 'fee') await catalogAPI.deleteFeeItem(row.id); if (kind === 'amount') await catalogAPI.deleteFeeAmountOption(row.id); if (kind === 'gift') await catalogAPI.deleteGiftItem(row.id); ElMessage.success('已删除'); loadData() } catch (error) { if (error !== 'cancel') ElMessage.error(errorMessage(error, '删除失败')) }
}
function feeName(id) { return fees.value.find(item => item.id === id)?.name || `项目 #${id}` }
onMounted(loadData)
</script>

<template>
  <div class="page-container config-page">
    <section class="page-heading-row"><div><h1>批次与申请配置</h1><p>维护欠费项目、金额档位与礼包物品；数据直接读取成员二配置接口。</p></div><div class="page-actions"><el-button @click="loadData"><el-icon><Refresh /></el-icon>刷新</el-button></div></section>
    <el-alert class="business-notice" type="info" :closable="false" show-icon title="当前联调范围：基础配置接口"><template #default>批次、礼包库存/规则、名额与补助额度尚未提供可用后端接口，页面不会显示模拟业务数据。</template></el-alert>
    <section class="content-card config-card" v-loading="loading">
      <el-tabs v-model="activeTab" class="config-tabs">
        <el-tab-pane label="批次设置" name="batch"><el-empty description="批次查询与维护接口尚未接入"><el-button type="primary" disabled>新建批次</el-button></el-empty></el-tab-pane>
        <el-tab-pane label="欠费配置" name="fees">
          <div class="config-toolbar"><span>欠费项目与预设金额档位。学生端总申报金额由后端限制为不超过 ¥8,000。</span><div><el-button @click="openAmount()">新增金额档位</el-button><el-button type="primary" @click="openFee()"><el-icon><Plus /></el-icon>新增欠费项目</el-button></div></div>
          <el-table :data="fees" border class="standard-table" empty-text="暂无欠费项目"><el-table-column prop="name" label="欠费项目" min-width="220"/><el-table-column label="启用状态" width="120"><template #default="{row}"><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag></template></el-table-column><el-table-column label="操作" width="150"><template #default="{row}"><div class="table-actions"><el-button link type="primary" @click="openFee(row)">编辑</el-button><el-button link type="danger" @click="remove('fee', row)">删除</el-button></div></template></el-table-column></el-table>
          <h2 class="subsection-title">金额档位</h2>
          <el-table :data="amounts" border class="standard-table" empty-text="暂无金额档位"><el-table-column label="欠费项目" min-width="220"><template #default="{row}">{{ feeName(row.feeItemId) }}</template></el-table-column><el-table-column label="金额" width="180"><template #default="{row}">¥{{ Number(row.amount).toFixed(2) }}</template></el-table-column><el-table-column label="状态" width="110"><template #default="{row}"><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag></template></el-table-column><el-table-column label="操作" width="150"><template #default="{row}"><div class="table-actions"><el-button link type="primary" @click="openAmount(row)">编辑</el-button><el-button link type="danger" @click="remove('amount', row)">删除</el-button></div></template></el-table-column></el-table>
        </el-tab-pane>
        <el-tab-pane label="礼包配置" name="gifts"><div class="config-toolbar"><span>当前接口仅包含物品名称和启用状态；库存、单人上限及批次关联待后端实现。</span><el-button type="primary" @click="openGift()"><el-icon><Plus /></el-icon>新增礼包物品</el-button></div><el-table :data="gifts" border class="standard-table" empty-text="暂无礼包物品"><el-table-column prop="name" label="物品名称" min-width="240"/><el-table-column label="启用状态" width="120"><template #default="{row}"><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag></template></el-table-column><el-table-column label="操作" width="150"><template #default="{row}"><div class="table-actions"><el-button link type="primary" @click="openGift(row)">编辑</el-button><el-button link type="danger" @click="remove('gift', row)">删除</el-button></div></template></el-table-column></el-table></el-tab-pane>
        <el-tab-pane label="名额与补助额度" name="quota"><el-empty description="资源总量、预占量、已使用量和剩余量接口尚未接入"><el-button type="primary" @click="allocationOpen = true">打开分配弹窗</el-button></el-empty></el-tab-pane>
      </el-tabs>
    </section>
    <el-dialog v-model="feeDialog" :title="editingId ? '编辑欠费项目' : '新增欠费项目'" width="480px"><el-form label-width="90px"><el-form-item label="项目名称" required><el-input v-model="feeForm.name" maxlength="64"/></el-form-item><el-form-item label="启用"><el-switch v-model="feeForm.enabled"/></el-form-item></el-form><template #footer><el-button @click="feeDialog=false">取消</el-button><el-button type="primary" @click="saveFee">保存</el-button></template></el-dialog>
    <el-dialog v-model="amountDialog" :title="editingId ? '编辑金额档位' : '新增金额档位'" width="480px"><el-form label-width="90px"><el-form-item label="欠费项目" required><el-select v-model="amountForm.feeItemId" :disabled="!!editingId"><el-option v-for="item in feeOptions" :key="item.id" :label="item.name" :value="item.id"/></el-select></el-form-item><el-form-item label="金额" required><el-input-number v-model="amountForm.amount" :min="0.01" :precision="2" :step="100"/></el-form-item><el-form-item label="启用"><el-switch v-model="amountForm.enabled"/></el-form-item></el-form><template #footer><el-button @click="amountDialog=false">取消</el-button><el-button type="primary" @click="saveAmount">保存</el-button></template></el-dialog>
    <el-dialog v-model="giftDialog" :title="editingId ? '编辑礼包物品' : '新增礼包物品'" width="480px"><el-form label-width="90px"><el-form-item label="物品名称" required><el-input v-model="giftForm.name" maxlength="64"/></el-form-item><el-form-item label="启用"><el-switch v-model="giftForm.enabled"/></el-form-item></el-form><template #footer><el-button @click="giftDialog=false">取消</el-button><el-button type="primary" @click="saveGift">保存</el-button></template></el-dialog>
    <AllocationDialog v-model="allocationOpen" />
  </div>
</template>

<style scoped>
.config-page { padding: 0 0 24px; }.business-notice { margin-bottom: 16px; }.config-tabs :deep(.el-tabs__header) { margin: 0; padding: 0 20px; }.config-tabs :deep(.el-tabs__content) { padding: 20px; }.config-toolbar { margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; gap: 16px; color: #6b7280; font-size: 12px; }.config-toolbar > div { display: flex; gap: 12px; }.subsection-title { margin: 24px 0 12px; font-size: 16px; line-height: 24px; }
</style>
