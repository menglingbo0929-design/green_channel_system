<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Bell, Check, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getMessages, markMessageAsRead } from '../../api/approval'
import { formatDateTime } from '../../constants/approval'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const filters = reactive({ page: 1, size: 10, read: '' })
const unreadCount = computed(() => records.value.filter((item) => !item.read).length)

async function loadMessages() {
  loading.value = true
  try {
    const page = await getMessages({ ...filters, read: filters.read === '' ? undefined : filters.read })
    records.value = page.records || []
    total.value = page.total || 0
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '消息加载失败')
  } finally {
    loading.value = false
  }
}

async function markRead(message) {
  if (message.read) return
  try {
    await markMessageAsRead(message.messageId)
    message.read = true
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '消息状态更新失败')
  }
}

function changeFilter(value) {
  filters.read = value
  filters.page = 1
  loadMessages()
}

onMounted(loadMessages)
</script>

<template>
  <div class="page-container">
    <section class="page-heading-row">
      <div>
        <h1>消息中心</h1>
        <p>查看申请审核进度、退回原因和后续办理通知。</p>
      </div>
      <div class="page-actions"><el-button @click="loadMessages"><el-icon><Refresh /></el-icon>刷新</el-button></div>
    </section>

    <section class="metric-grid" aria-label="消息概览">
      <article class="summary-card summary-blue"><div class="summary-icon"><Bell /></div><div><span>当前页未读</span><strong>{{ unreadCount }}</strong><small>点击消息即可标记为已读</small></div></article>
      <article class="summary-card summary-success"><div class="summary-icon"><Check /></div><div><span>消息总数</span><strong>{{ total }}</strong><small>仅展示当前登录用户的消息</small></div></article>
    </section>

    <section class="content-card records-card">
      <div class="card-tabs">
        <button type="button" :class="{ active: filters.read === '' }" @click="changeFilter('')">全部</button>
        <button type="button" :class="{ active: filters.read === false }" @click="changeFilter(false)">未读</button>
        <button type="button" :class="{ active: filters.read === true }" @click="changeFilter(true)">已读</button>
      </div>
      <el-table v-loading="loading" :data="records" border class="standard-table" empty-text="暂无消息">
        <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.read ? 'info' : 'primary'">{{ row.read ? '已读' : '未读' }}</el-tag></template></el-table-column>
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column prop="content" label="内容" min-width="280" show-overflow-tooltip />
        <el-table-column label="时间" width="155"><template #default="{ row }">{{ formatDateTime(row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" width="110"><template #default="{ row }"><el-button v-if="!row.read" size="small" type="primary" plain @click="markRead(row)">标为已读</el-button><span v-else>—</span></template></el-table-column>
      </el-table>
      <div class="pagination-row"><span>共 {{ total }} 条消息</span><el-pagination v-model:current-page="filters.page" v-model:page-size="filters.size" layout="prev, pager, next" :total="total" @current-change="loadMessages" /></div>
    </section>
  </div>
</template>
