<script setup>
import { computed, onMounted, ref } from 'vue'
import { fetchPendingArrears } from '../../api/confirmation'
import ArrearsConfirmationList from './confirmation/ArrearsConfirmationList.vue'
import ArrearsVoucher from './confirmation/ArrearsVoucher.vue'
import SchoolProxyApplication from './supplement/SchoolProxyApplication.vue'
import SupplementApplication from './supplement/SupplementApplication.vue'
import pendingReviewIcon from '../../figures/pending-review.png'
import arrearsConfirmationIcon from '../../figures/arrears-confirmation.png'
import supplementRecordIcon from '../../figures/supplement-record.png'
import voidedApplicationIcon from '../../figures/voided-application.png'

/**
 * 页面 8：学校业务处理页。
 *
 * 本页面只连接成员四负责的欠费确认、学校代申请、线下补录和欠费单据接口。
 * “最终审核”区域属于成员三，本轮只保留页面位置，不请求成员三 Controller，
 * 也不在成员四页面中提交审核动作。
 */
const activeTab = ref('arrears')
const loading = ref(false)
const ownSummary = ref({ pendingArrearsCount: null })

const tabs = [
  { key: 'review', label: '最终审核', owner: '成员三' },
  { key: 'arrears', label: '欠费确认' },
  { key: 'proxy', label: '代申请' },
  { key: 'supplement', label: '线下补录' },
  { key: 'voucher', label: '单据' },
]

const sidebarItems = [
  { icon: '⌂', label: '首页' },
  { icon: '○', label: '个人中心' },
  { icon: '▦', label: '新生信息管理' },
  { icon: '▤', label: '绿色通道申请' },
  { icon: '▥', label: '我的申请' },
  { icon: '□', label: '审核管理' },
  { icon: '▧', label: '欠费确认', active: true },
  { icon: '▣', label: '申请补录' },
  { icon: '▥', label: '统计报表' },
  { icon: '▦', label: '基础数据' },
  { icon: '▤', label: '政策与说明' },
]

const summaryCards = computed(() => [
  {
    label: '待学校审核',
    value: '—',
    color: '#1677ff',
    icon: pendingReviewIcon,
  },
  {
    label: '欠费待确认',
    value: ownSummary.value.pendingArrearsCount ?? '—',
    color: '#ff7a00',
    icon: arrearsConfirmationIcon,
  },
  {
    label: '今日补录',
    value: '—',
    color: '#00b96b',
    icon: supplementRecordIcon,
  },
  {
    label: '作废申请',
    value: '—',
    color: '#ff4d4f',
    icon: voidedApplicationIcon,
  },
])

/** 页面 8 顶部只读取成员四待确认列表的 total，不请求成员三审核看板。 */
async function loadOwnSummary() {
  loading.value = true
  const response = await fetchPendingArrears({ pageNo: 1, pageSize: 1 })
  ownSummary.value.pendingArrearsCount = Number(response.data.data?.total ?? 0)
  loading.value = false
}

onMounted(loadOwnSummary)
</script>

<template>
  <div class="school-business-page">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">学</span>
        <span>高校绿色通道系统</span>
      </div>

      <nav class="side-nav">
        <button
          v-for="item in sidebarItems"
          :key="item.label"
          type="button"
          class="side-item"
          :class="{ active: item.active }"
        >
          <span class="side-icon">{{ item.icon }}</span>
          <span>{{ item.label }}</span>
        </button>
      </nav>
    </aside>

    <section class="workspace">
      <header class="topbar">
        <div class="breadcrumb">
          <button type="button" class="menu-button" aria-label="展开菜单">☰</button>
          <span>首页</span>
          <span class="slash">/</span>
          <strong>学校业务处理页</strong>
        </div>
        <div class="top-actions">
          <button type="button" class="notice-button" aria-label="消息通知">
            ♧<span class="notice-count">3</span>
          </button>
          <span class="avatar">校</span>
          <div class="account">
            <strong>王磊</strong>
            <span>学校管理员⌄</span>
          </div>
        </div>
      </header>

      <main class="page-content">
        <div class="page-title">
          <h1>学校业务处理页</h1>
          <p>统一处理学校最终审核、欠费确认、学校代申请、线下补录等业务管理。</p>
        </div>

        <section class="summary-grid">
          <article v-for="card in summaryCards" :key="card.label" class="summary-card">
            <img :src="card.icon" :alt="card.label" />
            <div>
              <span>{{ card.label }}</span>
              <strong :style="{ color: card.color }">{{ card.value }}</strong>
            </div>
          </article>
        </section>

        <section class="business-panel">
          <div class="tab-bar">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              type="button"
              :class="{ active: activeTab === tab.key }"
              @click="activeTab = tab.key"
            >
              {{ tab.label }}
            </button>
          </div>

          <div v-if="activeTab === 'review'" class="review-content">
            <section class="review-boundary">
              <h2>学校最终审核</h2>
              <p>该区域由成员三维护。本次成员四联调不请求审核列表、详情或提交接口，也不挂载成员三审核弹窗。</p>
            </section>
          </div>

          <div v-else class="embedded-business">
            <ArrearsConfirmationList v-if="activeTab === 'arrears'" />
            <SchoolProxyApplication v-else-if="activeTab === 'proxy'" />
            <SupplementApplication v-else-if="activeTab === 'supplement'" />
            <ArrearsVoucher v-else-if="activeTab === 'voucher'" />
          </div>
        </section>
      </main>
    </section>
  </div>
</template>

<style scoped>
:global(*) { box-sizing: border-box; }
:global(html) { background: #f4f7fb; color-scheme: light; }
:global(body) { min-width: 1180px; margin: 0; background: #f4f7fb; color: #202938; font-family: "Microsoft YaHei", "PingFang SC", Arial, sans-serif; }
:global(button), :global(input), :global(select) { font: inherit; }
:global(#app) { width: 100%; max-width: none; min-height: 100vh; margin: 0; border: 0; text-align: initial; }

.school-business-page { display: flex; min-height: 100vh; background: #f4f7fb; }
.sidebar { position: fixed; inset: 0 auto 0 0; z-index: 20; width: 224px; overflow-y: auto; color: #fff; background: linear-gradient(180deg, #07518d 0%, #023b6c 100%); box-shadow: 3px 0 12px rgba(11, 54, 91, .12); }
.brand { display: flex; align-items: center; gap: 10px; height: 66px; padding: 0 17px; border-bottom: 1px solid rgba(255,255,255,.12); font-size: 18px; font-weight: 700; white-space: nowrap; }
.brand-mark { display: grid; width: 34px; height: 38px; place-items: center; border: 2px solid #fff; border-radius: 11px 11px 15px 15px; font-size: 14px; }
.side-nav { display: grid; gap: 4px; padding: 15px 8px 24px; }
.side-item { display: flex; align-items: center; width: 100%; height: 58px; padding: 0 16px; border: 0; border-radius: 6px; color: #fff; background: transparent; cursor: pointer; font-size: 16px; text-align: left; }
.side-item:hover { background: rgba(255,255,255,.08); }
.side-item.active { background: linear-gradient(90deg, #087ff2, #0875e9); box-shadow: 0 6px 16px rgba(0, 63, 133, .28); }
.side-icon { display: inline-grid; width: 29px; margin-right: 9px; place-items: center; font-size: 21px; }

.workspace { width: calc(100% - 224px); min-height: 100vh; margin-left: 224px; }
.topbar { display: flex; align-items: center; justify-content: space-between; height: 62px; padding: 0 28px 0 20px; border-bottom: 1px solid #dce2ea; background: #fff; }
.breadcrumb, .top-actions { display: flex; align-items: center; }
.breadcrumb { gap: 18px; font-size: 15px; }
.slash { color: #b6bdc7; }
.menu-button { margin-right: 8px; border: 0; background: transparent; color: #111; cursor: pointer; font-size: 26px; line-height: 1; }
.top-actions { gap: 12px; }
.notice-button { position: relative; border: 0; background: transparent; cursor: pointer; font-size: 25px; transform: rotate(180deg); }
.notice-count { position: absolute; top: -6px; right: -8px; display: grid; width: 18px; height: 18px; place-items: center; border-radius: 50%; color: #fff; background: #ff4d4f; font-size: 11px; transform: rotate(180deg); }
.avatar { display: grid; width: 40px; height: 40px; place-items: center; border-radius: 50%; color: #fff; background: linear-gradient(#f4d2b5 0 45%, #164778 46%); font-size: 13px; }
.account { display: grid; gap: 2px; font-size: 13px; }
.account strong { color: #111; font-size: 15px; }
.account span { color: #666; }

.page-content { padding: 20px 22px 28px; }
.page-title { display: flex; align-items: baseline; gap: 18px; margin: 0 0 20px; }
.page-title h1 { margin: 0; color: #131722; font-size: 25px; font-weight: 700; letter-spacing: 0; }
.page-title p { color: #8b919c; font-size: 12px; }
.summary-grid { display: grid; grid-template-columns: repeat(4, minmax(230px, 1fr)); gap: 18px; margin-bottom: 18px; }
.summary-card { display: flex; align-items: center; gap: 28px; min-height: 140px; padding: 24px 30px; border: 1px solid #dce2e9; border-radius: 5px; background: #fff; }
.summary-card img { width: 62px; height: 62px; object-fit: contain; }
.summary-card div { display: grid; gap: 8px; }
.summary-card span { color: #333; font-size: 15px; }
.summary-card strong { font-size: 34px; line-height: 1; }

.business-panel { min-height: 620px; border: 1px solid #dce2e9; border-radius: 5px; background: #fff; }
.tab-bar { display: flex; gap: 19px; height: 60px; padding: 0 17px; border-bottom: 1px solid #dce2e9; }
.tab-bar button { position: relative; min-width: 95px; border: 0; color: #222; background: transparent; cursor: pointer; font-size: 16px; }
.tab-bar button.active { color: #1677ff; font-weight: 700; }
.tab-bar button.active::after { position: absolute; right: 10px; bottom: -1px; left: 10px; height: 3px; background: #1677ff; content: ""; }
.review-content { padding: 34px 16px 18px; }
.review-boundary { padding: 40px; border: 1px dashed #cbd5e1; border-radius: 6px; color: #667085; background: #f8fafc; text-align: center; }
.review-boundary h2 { margin: 0 0 10px; color: #344054; font-size: 20px; }
.review-boundary p { margin: 0; }
.embedded-business { padding: 8px 20px 28px; }

@media (max-width: 1380px) {
  .summary-card { gap: 18px; padding: 20px; }
}
</style>
