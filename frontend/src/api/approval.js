import axios from 'axios'
import * as mock from '../mock/approval'
import { createRequestId } from '../constants/approval'

const client = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL || '/api', timeout: 10000 })
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('green-channel-token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

const useMock = import.meta.env.VITE_USE_MOCK !== 'false'
const unwrap = (response) => response.data?.data ?? response.data

export async function getApprovalList(role, tab, params) {
  if (useMock) return mock.getApprovalList(role, tab, params)
  return unwrap(await client.get(tab === 'pending' ? '/approvals/pending' : '/approvals/processed', { params }))
}
export async function getMyApplications(params) {
  if (useMock) return mock.getMyApplications(params)
  const payload = unwrap(await client.get('/applications/mine', { params }))
  if (!Array.isArray(payload)) return payload
  return {
    records: payload.map((item) => ({
      ...item,
      applicationId: item.applicationId ?? item.id,
      applicationTypeName: item.applicationTypeName ?? item.applicationType ?? '—',
      batchName: item.batchName ?? '—',
      declaredAmount: Number(item.declaredAmount ?? 0),
      currentNode: item.currentNode ?? item.status ?? '—',
      submitTime: item.submitTime ?? null,
    })),
    total: payload.length,
  }
}
export async function getMyApplicationDetail(applicationId) {
  if (useMock) return mock.getMyApplicationDetail(applicationId)
  return unwrap(await client.get(`/approvals/${applicationId}`))
}
export async function getApprovalDetail(applicationId, role) {
  if (useMock) return mock.getApprovalDetail(applicationId, role)
  return unwrap(await client.get(`/approvals/${applicationId}`))
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
  return unwrap(await client.get('/messages', { params }))
}

export async function markMessageAsRead(messageId) {
  if (useMock) return mock.markMessageAsRead(messageId)
  return unwrap(await client.post(`/messages/${messageId}/read`, { requestId: createRequestId() }))
}
