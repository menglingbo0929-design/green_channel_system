import api from './index'

/** 6.1.5、6.1.6、6.1.7 统计接口客户端；登录身份由统一 JWT 请求头提供。 */
const statisticsClient = api

/**
 * 读取学校端最终状态申请统计。
 *
 * @param {object} params 仅传入文档固定的筛选参数；空字段会在调用前移除。
 */
export function fetchApplicationStatistics(params) {
  return statisticsClient.get('/statistics/applications/summary', {
    params,
  })
}

/** 查询统计看板下方的动态列明细。 */
export function fetchStatisticsReport(params) {
  return statisticsClient.get('/statistics/reports/details', {
    params,
  })
}

/** 查询历史批次明细；筛选参数与普通明细保持完全一致。 */
export function fetchStatisticsHistory(params) {
  return statisticsClient.get('/statistics/reports/history', {
    params,
  })
}

/** 下载后端生成的真实 xlsx 文件。 */
export function exportStatisticsExcel(params) {
  return statisticsClient.get('/statistics/reports/export', {
    params,
    responseType: 'blob',
  })
}

/** 获取与 Excel 同字段、同顺序的打印数据。 */
export function fetchStatisticsPrintData(params) {
  return statisticsClient.get('/statistics/reports/print', {
    params,
  })
}
