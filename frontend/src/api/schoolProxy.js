import client from './index'
/** 6.1.3 学校代申请：查询学生、建草稿、上传附件、提交审核。 */
export const findProxyStudent = (studentNo) => client.get('/school-proxy/students', { params: { studentNo } })
export const createProxyDraft = (data) => client.post('/school-proxy/applications/drafts', data)
export const uploadProxyAttachment = (id, file, requestId) => {
  const formData = new FormData()
  formData.append('file', file)
  return client.post(`/school-proxy/applications/${id}/attachments`, formData, {
    params: { requestId },
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
export const submitProxyDraft = (id, version, requestId) => client.post(`/school-proxy/applications/${id}/submit`, null, { params: { version, requestId } })
