<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchPendingArrears } from '../../api/confirmation'
import { getApprovalDashboard } from '../../api/approval'
import SchoolWorkspaceShell from '../../components/school/SchoolWorkspaceShell.vue'
import ApprovalWorkbench from '../approval/ApprovalWorkbench.vue'
import ArrearsConfirmationList from './confirmation/ArrearsConfirmationList.vue'
import ArrearsVoucher from './confirmation/ArrearsVoucher.vue'
import SchoolProxyApplication from './supplement/SchoolProxyApplication.vue'
import SupplementApplication from './supplement/SupplementApplication.vue'
import pendingReviewIcon from '../../figures/pending-review.png'
import arrearsConfirmationIcon from '../../figures/arrears-confirmation.png'
import supplementRecordIcon from '../../figures/supplement-record.png'
import voidedApplicationIcon from '../../figures/voided-application.png'

/** 页面八：学校业务处理。最终审核直接复用审核工作台内容。 */
const route = useRoute()
const router = useRouter()
const activeTab = ref('review')
const ownSummary = ref({ pendingSchoolReviewCount: 0, pendingArrearsCount: 0 })

const tabs = [
  { key: 'review', label: '最终审核' },
  { key: 'arrears', label: '欠费确认' },
  { key: 'proxy', label: '代申请' },
  { key: 'supplement', label: '线下补录' },
  { key: 'voucher', label: '单据' },
]

watch(() => route.query.tab, (tab) => {
  if (tabs.some(item => item.key === tab)) activeTab.value = tab
}, { immediate: true })

function selectTab(tab) {
  activeTab.value = tab
  router.replace({ name: 'MemberFourSchoolBusiness', query: { ...route.query, tab } })
}

const summaryCards = computed(() => [
  { label: '待学校审核', value: ownSummary.value.pendingSchoolReviewCount, hint: '在最终审核页签处理', color: '#1677ff', icon: pendingReviewIcon, action: () => selectTab('review') },
  { label: '欠费待确认', value: ownSummary.value.pendingArrearsCount, hint: '待确认欠费申请', color: '#ff7a00', icon: arrearsConfirmationIcon, action: () => selectTab('arrears') },
  { label: '今日补录', value: '—', hint: '线下业务补录记录', color: '#00b96b', icon: supplementRecordIcon, action: () => selectTab('supplement') },
  { label: '作废申请', value: '—', hint: '已作废业务记录', color: '#ff4d4f', icon: voidedApplicationIcon },
])

async function loadOwnSummary() {
  // 两张卡来自两个模块。先写入成员四的待确认数，避免审核工作台暂不可用时
  // 把已成功取得的欠费确认数量一并丢掉。
  const arrearsResponse = await fetchPendingArrears({ pageNo: 1, pageSize: 1 })
  ownSummary.value.pendingArrearsCount = Number(arrearsResponse.data.data?.total ?? 0)

  const approvalDashboard = await getApprovalDashboard('SCHOOL', { page: 1, size: 1 })
  ownSummary.value.pendingSchoolReviewCount = Number(
    approvalDashboard?.pendingByLevel?.find(item => item.level === 'SCHOOL')?.count ?? 0,
  )
}

onMounted(loadOwnSummary)
</script>

<template>
  <SchoolWorkspaceShell>
    <div class="school-business-page">
      <section class="page-heading-row">
        <div>
          <h1>学校业务处理页</h1>
          <p>统一处理学校最终审核、欠费确认、学校代申请和线下补录。</p>
        </div>
      </section>

      <section class="metric-grid" aria-label="学校业务概览">
        <button v-for="card in summaryCards" :key="card.label" type="button" class="summary-card" @click="card.action?.()">
          <img :src="card.icon" :alt="card.label" />
          <div>
            <span>{{ card.label }}</span>
            <strong :style="{ color: card.color }">{{ card.value }}</strong>
            <small>{{ card.hint }}</small>
          </div>
        </button>
      </section>

      <section class="business-panel">
        <div class="tab-bar" role="tablist">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            type="button"
            role="tab"
            :aria-selected="activeTab === tab.key"
            :class="{ active: activeTab === tab.key }"
            @click="selectTab(tab.key)"
          >{{ tab.label }}</button>
        </div>

        <div class="embedded-business">
          <ApprovalWorkbench v-if="activeTab === 'review'" embedded />
          <ArrearsConfirmationList v-else-if="activeTab === 'arrears'" />
          <SchoolProxyApplication v-else-if="activeTab === 'proxy'" />
          <SupplementApplication v-else-if="activeTab === 'supplement'" />
          <ArrearsVoucher v-else-if="activeTab === 'voucher'" />
        </div>
      </section>
    </div>
  </SchoolWorkspaceShell>
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

/* 页面八内容区：外壳由 SchoolWorkspaceShell 统一提供，此处只维护业务主体。 */
.school-business-page { display: block; min-height: 100%; background: transparent; }
.page-heading-row { display: flex; align-items: center; min-height: 58px; margin-bottom: 18px; }
.page-heading-row h1 { margin: 0 0 8px; color: #1f2937; font-size: 24px; line-height: 1; }
.page-heading-row p { margin: 0; color: #6b7280; font-size: 13px; }
.metric-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; margin-bottom: 18px; }
.metric-grid .summary-card { min-height: 120px; padding: 20px 24px; gap: 18px; border: 1px solid #e5e7eb; border-radius: 4px; box-shadow: none; cursor: pointer; text-align: left; }
.metric-grid .summary-card:hover { border-color: #b7d5ff; box-shadow: 0 4px 12px rgba(22, 119, 255, .08); }
.metric-grid .summary-card img { width: 52px; height: 52px; }.metric-grid .summary-card div { gap: 4px; }
.metric-grid .summary-card span { color: #4b5563; font-size: 14px; }.metric-grid .summary-card strong { font-size: 28px; }.metric-grid .summary-card small { color: #9ca3af; font-size: 12px; }
.business-panel { min-height: 0; overflow: hidden; border-color: #e5e7eb; border-radius: 4px; }.tab-bar { height: 58px; gap: 12px; padding: 0 18px; border-bottom-color: #e5e7eb; }
.tab-bar button { min-width: 96px; color: #4b5563; font-size: 15px; }.tab-bar button.active::after { right: 12px; left: 12px; height: 3px; }.embedded-business { padding: 22px 24px 28px; }
.embedded-business :deep(.workbench-page) { padding: 0; }
@media (max-width: 1280px) { .metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
</style>
