<template>
  <div class="error-page">
    <h1 class="error-code">404</h1>
    <p class="error-msg">页面不存在</p>
    <el-button type="primary" @click="$router.push('/home')">返回首页</el-button>
  </div>
</template>
<style scoped>
.error-page{display:flex;flex-direction:column;align-items:center;justify-content:center;min-height:60vh}
.error-code{font-size:72px;font-weight:700;color:#1677FF;margin:0 0 12px;line-height:1}
.error-msg{font-size:16px;color:#6B7280;margin:0 0 24px}
</style>
