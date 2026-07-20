<template>
  <!-- 弹窗 1：通用新增/编辑弹窗，复用于用户、新生、组织结构的增改操作 -->
  <el-dialog
    :model-value="visible"
    :title="title"
    :close-on-click-modal="false"
    :close-on-press-escape="!loading"
    :show-close="!loading"
    width="560px"
    top="10vh"
    @update:model-value="$emit('update:visible', $event)"
    class="form-dialog"
  >
    <!-- 表单区：由父组件通过 slot 传入 -->
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="112px"
      label-position="right"
      class="dialog-form"
    >
      <slot />
    </el-form>

    <!-- 底部按钮区：右对齐，取消 + 确认 -->
    <template #footer>
      <div class="dialog-footer">
        <el-button
          class="btn-cancel"
          :disabled="loading"
          @click="$emit('update:visible', false)"
        >
          取消
        </el-button>
        <el-button
          type="primary"
          class="btn-submit"
          :loading="loading"
          :disabled="loading"
          @click="handleSubmit"
        >
          {{ loading ? submitLoadingText : submitText }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  /** 控制弹窗显示/隐藏 */
  visible: { type: Boolean, default: false },
  /** 弹窗标题，如"新增学生""编辑学院" */
  title: { type: String, default: '新增' },
  /** 表单数据对象，由父组件传入 */
  formData: { type: Object, default: () => ({}) },
  /** 表单校验规则 */
  rules: { type: Object, default: () => ({}) },
  /** 保存按钮加载状态 */
  loading: { type: Boolean, default: false },
  /** 确认按钮文字 */
  submitText: { type: String, default: '保存' },
  /** 保存中按钮文字 */
  submitLoadingText: { type: String, default: '保存中…' }
})

const emit = defineEmits(['update:visible', 'submit'])

const formRef = ref(null)

/** 点确认 → 先校验 → 通过才 emit submit */
async function handleSubmit() {
  if (!formRef.value) {
    emit('submit')
    return
  }
  try {
    await formRef.value.validate()
    emit('submit')
  } catch {
    // 校验失败不提交
  }
}

/** 暴露 validate 方法给父组件调用 */
defineExpose({ formRef })
</script>

<style scoped>
/* =================================================================
   弹窗样式 — 严格按照设计规范
   ================================================================= */

/* —— Element Plus Dialog 覆盖 —— */
.form-dialog :deep(.el-dialog) {
  border-radius: 4px;              /* 规范：4px */
  overflow: hidden;
}

.form-dialog :deep(.el-dialog__header) {
  padding: 20px 20px 0;
  border-bottom: none;
}

.form-dialog :deep(.el-dialog__title) {
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;               /* 规范：模块标题 */
  color: #1F2937;
}

.form-dialog :deep(.el-dialog__body) {
  padding: 20px 20px 0;
}

.form-dialog :deep(.el-dialog__footer) {
  padding: 20px;
}

/* —— 表单区 —— */
.dialog-form :deep(.el-form-item__label) {
  font-size: 14px;
  color: #374151;
  font-weight: 400;
}

/* 必填星号为红色 #EF4444 */
.dialog-form :deep(.el-form-item.is-required .el-form-item__label::before) {
  color: #EF4444;
}

/* 输入框：32px 高，4px 圆角 */
.dialog-form :deep(.el-input__wrapper),
.dialog-form :deep(.el-select__wrapper) {
  height: 32px;
  border-radius: 4px;
  box-shadow: none;
  border: 1px solid #D1D5DB;
}

.dialog-form :deep(.el-input__wrapper:hover),
.dialog-form :deep(.el-select__wrapper:hover) {
  border-color: #1677FF;
}

.dialog-form :deep(.el-input__wrapper.is-focus),
.dialog-form :deep(.el-select__wrapper.is-focus) {
  border-color: #1677FF;
  box-shadow: 0 0 0 1px rgba(22, 119, 255, 0.15);
}

.dialog-form :deep(.el-input__inner),
.dialog-form :deep(.el-select .el-input__inner) {
  font-size: 14px;
}

/* 禁用字段背景 #F3F4F6 */
.dialog-form :deep(.el-input.is-disabled .el-input__wrapper),
.dialog-form :deep(.el-select.is-disabled .el-select__wrapper) {
  background: #F3F4F6;
}

/* 只读信息区背景 */
.dialog-form :deep(.el-input.is-disabled .el-input__wrapper) {
  background: #F8FAFC;
  border-color: #E5E7EB;
}

/* ==================== 底部按钮区 ==================== */
.dialog-footer {
  display: flex;
  justify-content: flex-end;       /* 规范：右对齐 */
  gap: 12px;                       /* 规范：按钮间距 12px */
}

/* 取消按钮：次要样式（白底蓝边蓝字） */
.btn-cancel {
  height: 32px;                    /* 规范：按钮高 32px */
  min-width: 72px;                 /* 规范：最小宽 72px */
  padding: 0 16px;                 /* 规范：左右内边距 16px */
  border-radius: 4px;             /* 规范：4px */
  font-size: 14px;
  background: #FFFFFF;
  border: 1px solid #1677FF;
  color: #1677FF;
}

.btn-cancel:hover {
  background: #F0F5FF;
  border-color: #1677FF;
  color: #1677FF;
}

/* 确认按钮：主要样式（蓝底蓝边白字） */
.btn-submit {
  height: 32px;
  min-width: 72px;
  padding: 0 16px;
  border-radius: 4px;
  font-size: 14px;
  background: #1677FF;
  border: 1px solid #1677FF;
  color: #FFFFFF;
}

.btn-submit:hover {
  background: #4096FF;
  border-color: #4096FF;
}

.btn-submit:active {
  background: #0958D9;
  border-color: #0958D9;
}
</style>
