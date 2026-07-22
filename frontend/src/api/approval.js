import axios from 'axios'
import * as mock from '../mock/approval'
import { createRequestId } from '../constants/approval'

const client = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL || '/api', timeout: 10000 })
client.interceptors.request.use((config) => {
  // Keep the approval module on the same authenticated session as the rest of
  // the application. The legacy key remains only for existing local sessions.
  const token = localStorage.getItem('token') || localStorage.getItem('green-channel-token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Real APIs are the safe default. Mock data must be enabled explicitly so a
// missing environment variable cannot hide backend integration failures.
const useMock = import.meta.env.VITE_USE_MOCK === 'true'
const unwrap = (response) => response.data?.data ?? response.data
const valueOf = (value) => value == null ? '' : String(value)

function normalizePage(payload, fallback = {}) {
  const records = payload?.records ?? payload?.list ?? payload?.content ?? []
  return {
    records: Array.isArray(records) ? records : [],
    total: Number(payload?.total ?? payload?.totalCount ?? records.length),
    page: Number(payload?.page ?? payload?.pageNo ?? fallback.page ?? 1),
    size: Number(payload?.size ?? payload?.pageSize ?? fallback.size ?? 10),
  }
}

function normalizeApplication(item) {
  return {
    ...item,
    applicationId: item.applicationId ?? item.id,
    applicationTypeName: item.applicationTypeName ?? item.applicationType ?? '—',
    batchName: item.batchName ?? (item.batchId ? `批次 ${item.batchId}` : '—'),
    declaredAmount: Number(item.declaredAmount ?? item.requestedAmount ?? item.amount ?? 0),
    currentNode: item.currentNode ?? item.currentLevelName ?? item.statusName ?? item.status ?? '—',
    submitTime: item.submitTime ?? item.createTime ?? null,
  }
}

function filterMyApplications(records, params) {
  const reviewing = ['COUNSELOR_PENDING', 'COLLEGE_PENDING', 'SCHOOL_PENDING', 'CONFIRM_PENDING']
  return records.filter((item) => {
    const category = params.category || 'ALL'
    const categoryMatch = category === 'ALL'
      || (category === 'DRAFT' && item.status === 'DRAFT')
      || (category === 'REVIEWING' && reviewing.includes(item.status))
      || (category === 'RETURNED' && valueOf(item.status).endsWith('_RETURNED'))
      || (category === 'APPROVED' && item.status === 'APPROVED')
      || (category === 'COMPLETED' && item.status === 'COMPLETED')
    const keyword = valueOf(params.keyword).trim().toUpperCase()
    return categoryMatch
      && (!params.applicationType || item.applicationType === params.applicationType)
      && (!keyword || valueOf(item.applicationNo).toUpperCase().includes(keyword)
        || valueOf(item.applicationTypeName).toUpperCase().includes(keyword))
  })
}

export async function getApprovalList(role, tab, params) {
  if (useMock) return mock.getApprovalList(role, tab, params)
  const query = { ...params }
  if (tab === 'returned') query.status = `${role}_RETURNED`
  const endpoint = tab === 'pending' ? '/approvals/pending' : '/approvals/processed'
  const page = normalizePage(unwrap(await client.get(endpoint, { params: query })), params)
  return { ...page, records: page.records.map(normalizeApplication) }
}
export async function getMyApplications(params) {
  if (useMock) return mock.getMyApplications(params)
  const payload = unwrap(await client.get('/applications/mine', { params }))
  if (!Array.isArray(payload)) {
    const page = normalizePage(payload, params)
    return { ...page, records: page.records.map(normalizeApplication) }
  }
  const filtered = filterMyApplications(payload.map(normalizeApplication), params)
  const page = Number(params.page || 1)
  const size = Number(params.size || 10)
  const offset = (page - 1) * size
  return {
    records: filtered.slice(offset, offset + size),
    total: filtered.length,
    page,
    size,
  }
}
export async function getMyApplicationDetail(applicationId) {
  if (useMock) return mock.getMyApplicationDetail(applicationId)
  return normalizeDetail(unwrap(await client.get(`/approvals/${applicationId}`)))
}
export async function getApprovalDetail(applicationId, role) {
  if (useMock) return mock.getApprovalDetail(applicationId, role)
  return normalizeDetail(unwrap(await client.get(`/approvals/${applicationId}`)))
}
export async function readApprovalAttachment(applicationId, attachmentId) {
  if (useMock) throw new Error('模拟模式不提供真实附件内容')
  const response = await client.get(
    `/approvals/${applicationId}/attachments/${attachmentId}/content`,
    { responseType: 'blob' },
  )
  return response.data
}
export async function reviewApplication(role, applicationId, payload) {
  if (useMock) return mock.reviewApplication(role, applicationId, payload)
  return unwrap(await client.post(`/approvals/${role.toLowerCase()}/${applicationId}/review`, payload))
}
export async function getApprovalDashboard(role, params) {
  if (useMock) return mock.getApprovalDashboard(role, params)
  return unwrap(await client.get('/approvals/dashboard', { params }))
}
export async function getSubmissionStatus(role, params) {
  if (useMock) return mock.getSubmissionStatus(role, params)
  return unwrap(await client.get('/approval-submissions/status', { params }))
}
export async function submitInitialBatch(role, payload) {
  if (useMock) return mock.submitInitialBatch(role, payload)
  return unwrap(await client.post(`/approval-submissions/${role.toLowerCase()}/initial`, payload))
}
export async function submitReturnResubmit(role, payload) {
  if (useMock) return mock.submitReturnResubmit(role, payload)
  return unwrap(await client.post('/approval-submissions/return-resubmit', payload))
}
export async function cancelApplication(applicationId, payload) {
  if (useMock) return mock.cancelApplication(applicationId, payload)
  return unwrap(await client.post(`/approvals/${applicationId}/cancel`, payload))
}

export async function getMessages(params) {
  if (useMock) return mock.getMessages(params)
  const page = normalizePage(unwrap(await client.get('/messages', { params })), params)
  return {
    ...page,
    records: page.records.map((item) => ({
      ...item,
      messageId: item.messageId ?? item.id,
      read: Boolean(item.read ?? item.isRead),
    })),
  }
}
export async function editApprovalFields(applicationId, payload) {
  if (useMock) return mock.editApprovalFields(applicationId, payload)
  return unwrap(await client.put(`/approvals/${applicationId}/editable-fields`, payload))
}

export async function markMessageAsRead(messageId) {
  if (useMock) return mock.markMessageAsRead(messageId)
  return unwrap(await client.post(`/messages/${messageId}/read`, { requestId: createRequestId() }))
}

function normalizeDetail(payload) {
  const arrearsDetail = payload?.arrearsDetail
  const arrearsItems = Array.isArray(arrearsDetail) ? arrearsDetail : (arrearsDetail?.items || [])
  const subsidyAmount = payload?.subsidyDetail?.expectedAmount ?? payload?.subsidyDetail?.requestedAmount
  const declaredAmount = arrearsItems.length
    ? arrearsItems.reduce((sum, item) => sum + Number(item.declaredAmount ?? item.amount ?? 0), 0)
    : Number(subsidyAmount ?? payload?.application?.declaredAmount ?? 0)
  return {
    ...payload,
    application: normalizeApplication({ ...(payload?.application ?? payload?.applicationDetail ?? {}), declaredAmount }),
    approvalRecords: payload?.approvalRecords ?? payload?.records ?? [],
    attachments: (payload?.attachments ?? []).map((item) => ({
      ...item,
      fileName: item.fileName ?? item.originalFilename,
      fileSize: item.fileSize == null ? '—' : (typeof item.fileSize === 'number' ? `${Math.ceil(item.fileSize / 1024)} KB` : item.fileSize),
    })),
    editableFields: payload?.editableFields ?? [],
    allowedActions: payload?.allowedActions ?? [],
    version: payload?.version ?? payload?.application?.version,
  }
}
