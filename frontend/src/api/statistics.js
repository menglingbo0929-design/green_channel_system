import memberFourClient from './memberFourClient'

/** 6.1.5、6.1.6 统计接口的临时联调客户端；生产环境将由统一 Axios/JWT 封装替换。 */
const statisticsClient = memberFourClient

/**
 * 读取学校端最终状态申请统计。
 *
 * @param {object} params 仅传入文档固定的筛选参数；空字段会在调用前移除。
 * @param {number|string} userId 本地连通性调试 ID；后端仍需真实学校权限 Service 才会成功。
 */
export function fetchApplicationStatistics(params, userId) {
  return statisticsClient.get('/statistics/applications/summary', {
    params,
    headers: { 'X-User-Id': userId },
  })
}

/** 查询统计看板下方的动态列明细。 */
export function fetchStatisticsReport(params, userId) {
  return statisticsClient.get('/statistics/reports/details', {
    params,
    headers: { 'X-User-Id': userId },
  })
}

/** 查询历史批次明细；筛选参数与普通明细保持完全一致。 */
export function fetchStatisticsHistory(params, userId) {
  return statisticsClient.get('/statistics/reports/history', {
    params,
    headers: { 'X-User-Id': userId },
  })
}

/** 下载后端生成的真实 xlsx 文件。 */
export function exportStatisticsExcel(params, userId) {
  return statisticsClient.get('/statistics/reports/export', {
    params,
    headers: { 'X-User-Id': userId },
    responseType: 'blob',
  })
}

/** 获取与 Excel 同字段、同顺序的打印数据。 */
export function fetchStatisticsPrintData(params, userId) {
  return statisticsClient.get('/statistics/reports/print', {
    params,
    headers: { 'X-User-Id': userId },
  })
}
