<template>
  <el-dialog v-model="visible" title="完善个人信息" width="480px" :close-on-click-modal="false" :close-on-press-escape="false" :show-close="false">
    <el-form :model="form" label-width="120px">
      <el-form-item label="联系电话">
        <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="20" />
      </el-form-item>
      <el-form-item label="生源地贷款">
        <el-switch v-model="form.originLoan" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item label="校园地贷款">
        <el-switch v-model="form.campusLoan" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item label="困难等级">
        <el-select v-model="form.difficultyLevel" placeholder="请选择" clearable>
          <el-option label="特别困难" value="特别困难" />
          <el-option label="困难" value="困难" />
          <el-option label="一般困难" value="一般困难" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleSkip">暂不填写</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'done'])
const visible = ref(false)
const saving = ref(false)

const form = reactive({ phone: '', originLoan: 0, campusLoan: 0, difficultyLevel: '' })

watch(() => props.modelValue, async (val) => {
  if (val) {
    try {
      const res = await axios.get('/api/student/profile')
      const d = res.data.data
      if (d) {
        form.phone = d.phone || ''
        form.originLoan = d.originLoan || 0
        form.campusLoan = d.campusLoan || 0
        form.difficultyLevel = d.difficultyLevel || ''
      }
    } catch { /* ignore */ }
    visible.value = true
  } else {
    visible.value = false
  }
})

watch(visible, (v) => { if (!v) emit('update:modelValue', false) })

async function handleSave() {
  saving.value = true
  try {
    await axios.put('/api/student/profile', { ...form })
    ElMessage.success('保存成功')
    visible.value = false
    emit('done')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  } finally { saving.value = false }
}

function handleSkip() {
  visible.value = false
  emit('done')
}
</script>
