import api from './index'

/**
 * 欠费最终确认模块统一使用项目公共 Axios 实例。
 *
 * 请求由公共拦截器携带 JWT；页面不再传递临时用户 ID 或角色请求头。
 */
const confirmationClient = api

/**
 * 查询状态为 CONFIRM_PENDING 的欠费申请。
 *
 * @param {object} params 查询条件：pageNo、pageSize、applicationNo、studentNo、studentName。
 * @returns {Promise<object>} 后端 JsonResponse，data 为 MyBatis-Plus Page。
 */
export function fetchPendingArrears(params) {
  return confirmationClient.get('/confirm/list', { params })
}

/**
 * 读取一笔待确认申请的最新详情。
 *
 * 页面每次打开确认对话框都应调用此接口，使用最新 version，避免把过期页面
 * 上的金额或版本直接提交。
 *
 * @param {number|string} applicationId 统一申请主表 ID。
 */
export function fetchPendingArrearsDetail(applicationId) {
  return confirmationClient.get(`/confirm/app/${applicationId}`)
}

/**
 * 完成一笔欠费确认。
 *
 * @param {number|string} applicationId 统一申请主表 ID。
 * @param {object} payload confirmedAmount、version、requestId。
 */
export function confirmArrears(applicationId, payload) {
  return confirmationClient.post(`/confirm/${applicationId}`, payload)
}
