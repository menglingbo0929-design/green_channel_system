import memberFourClient from './memberFourClient'
const client = memberFourClient
/** 6.1.3 学校代申请：查询学生、建草稿、上传附件、提交审核。 */
export const findProxyStudent = (studentNo) => client.get('/school-proxy/students', { params: { studentNo } })
export const createProxyDraft = (data, userId) => client.post('/school-proxy/applications/drafts', data, { headers: { 'X-User-Id': userId } })
export const uploadProxyAttachment = (id, file, requestId, userId) => {
  const formData = new FormData()
  formData.append('file', file)
  return client.post(`/school-proxy/applications/${id}/attachments`, formData, {
    params: { requestId },
    headers: { 'Content-Type': 'multipart/form-data', 'X-User-Id': userId },
  })
}
export const submitProxyDraft = (id, version, requestId, userId) => client.post(`/school-proxy/applications/${id}/submit`, null, { params: { version, requestId }, headers: { 'X-User-Id': userId } })
