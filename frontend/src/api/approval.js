import axios from 'axios'
import * as mock from '../mock/approval'

const client = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL || '/api', timeout: 10000 })
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('green-channel-token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

const useMock = import.meta.env.VITE_USE_MOCK !== 'false'

export async function getApprovalList(role, tab, params) {
  if (useMock) return mock.getApprovalList(role, tab, params)
  return (await client.get(tab === 'pending' ? '/approvals/pending' : '/approvals/processed', { params })).data.data
}
export async function getMyApplications(params) {
  if (useMock) return mock.getMyApplications(params)
  return (await client.get('/applications/mine', { params })).data.data
}
export async function getMyApplicationDetail(applicationId) {
  if (useMock) return mock.getMyApplicationDetail(applicationId)
  return (await client.get(`/approvals/${applicationId}`)).data.data
}
export async function getApprovalDetail(applicationId, role) {
  if (useMock) return mock.getApprovalDetail(applicationId, role)
  return (await client.get(`/approvals/${applicationId}`)).data.data
}
export async function reviewApplication(role, applicationId, payload) {
  if (useMock) return mock.reviewApplication(role, applicationId, payload)
  return (await client.post(`/approvals/${role.toLowerCase()}/${applicationId}/review`, payload)).data.data
}
export async function getApprovalDashboard(role, params) {
  if (useMock) return mock.getApprovalDashboard(role, params)
  return (await client.get('/approvals/dashboard', { params })).data.data
}
export async function getSubmissionStatus(role, params) {
  if (useMock) return mock.getSubmissionStatus(role, params)
  return (await client.get('/approval-submissions/status', { params })).data.data
}
export async function submitInitialBatch(role, payload) {
  if (useMock) return mock.submitInitialBatch(role, payload)
  return (await client.post(`/approval-submissions/${role.toLowerCase()}/initial`, payload)).data.data
}
export async function submitReturnResubmit(role, payload) {
  if (useMock) return mock.submitReturnResubmit(role, payload)
  return (await client.post('/approval-submissions/return-resubmit', payload)).data.data
}
export async function cancelApplication(applicationId, payload) {
  if (useMock) return mock.cancelApplication(applicationId, payload)
  return (await client.post(`/approvals/${applicationId}/cancel`, payload)).data.data
}
