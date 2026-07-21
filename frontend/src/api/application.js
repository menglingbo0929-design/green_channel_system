import api from './index.js'

function payload(response) {
  return response.data?.data ?? response.data
}

function developmentIdentity() {
  const studentId = import.meta.env.VITE_DEV_STUDENT_ID
  const userId = import.meta.env.VITE_DEV_USER_ID
  if (!studentId || !userId) {
    throw new Error('当前后端仍使用临时开发身份头；请在本地 .env 设置 VITE_DEV_STUDENT_ID 和 VITE_DEV_USER_ID，或等待可信登录上下文接入。')
  }
  return { 'X-Student-Id': studentId, 'X-User-Id': userId }
}

export const catalogAPI = {
  listFeeItems: (includeDisabled = true) => api.get('/fee-items', { params: { includeDisabled } }).then(payload),
  createFeeItem: (data) => api.post('/fee-items', data).then(payload),
  updateFeeItem: (id, data) => api.put(`/fee-items/${id}`, data).then(payload),
  deleteFeeItem: (id) => api.delete(`/fee-items/${id}`).then(payload),
  listFeeAmountOptions: (feeItemId, includeDisabled = true) => api.get('/fee-amount-options', { params: { feeItemId, includeDisabled } }).then(payload),
  createFeeAmountOption: (data) => api.post('/fee-amount-options', data).then(payload),
  updateFeeAmountOption: (id, data) => api.put(`/fee-amount-options/${id}`, data).then(payload),
  deleteFeeAmountOption: (id) => api.delete(`/fee-amount-options/${id}`).then(payload),
  listGiftItems: (includeDisabled = true) => api.get('/gift-items', { params: { includeDisabled } }).then(payload),
  createGiftItem: (data) => api.post('/gift-items', data).then(payload),
  updateGiftItem: (id, data) => api.put(`/gift-items/${id}`, data).then(payload),
  deleteGiftItem: (id) => api.delete(`/gift-items/${id}`).then(payload),
}

export const studentApplicationAPI = {
  mine: () => api.get('/applications/mine', { headers: developmentIdentity() }).then(payload),
  createDraft: (data) => api.post('/applications/drafts', data, { headers: developmentIdentity() }).then(payload),
  updateDraft: (id, data) => api.put(`/applications/${id}`, data, { headers: developmentIdentity() }).then(payload),
  deleteDraft: (id, version) => api.delete(`/applications/${id}`, { params: { version }, headers: developmentIdentity() }),
  arrears: (id) => api.get(`/applications/${id}/arrears`, { headers: developmentIdentity() }).then(payload),
  replaceArrears: (id, data) => api.put(`/applications/${id}/arrears`, data, { headers: developmentIdentity() }).then(payload),
  gifts: (id) => api.get(`/applications/${id}/gifts`, { headers: developmentIdentity() }).then(payload),
  replaceGifts: (id, data) => api.put(`/applications/${id}/gifts`, data, { headers: developmentIdentity() }).then(payload),
  subsidy: (id) => api.get(`/applications/${id}/subsidy`, { headers: developmentIdentity() }).then(payload),
  replaceSubsidy: (id, data) => api.put(`/applications/${id}/subsidy`, data, { headers: developmentIdentity() }).then(payload),
}

export function createRequestId() {
  return globalThis.crypto?.randomUUID?.() || `web-${Date.now()}-${Math.random().toString(16).slice(2)}`
}
