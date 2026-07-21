import memberFourClient from './memberFourClient'

/**
 * 6.1.4 线下补录接口统一使用 /api/supplements。
 *
 * X-User-Id 只为当前本地联调保留；成员一登录模块合入后，Axios 拦截器会改为
 * 携带真实登录凭证，页面不再允许手工填写用户 ID。
 */
const client = memberFourClient

const debugHeaders = (userId) => ({ headers: { 'X-User-Id': userId } })

/** 按学号查询可补录学生。 */
export const findSupplementStudent = (studentNo, userId) => client.get(
  '/supplements/students',
  { params: { studentNo }, ...debugHeaders(userId) },
)

/** 创建线下补录并触发后端自动审核事务。 */
export const createSupplement = (data, userId) => client.post(
  '/supplements',
  data,
  debugHeaders(userId),
)

/** 按确定的四个筛选字段分页查询补录历史。 */
export const fetchSupplementHistory = (params, userId) => client.get(
  '/supplements',
  { params, ...debugHeaders(userId) },
)

/** 查询一条补录详情，便于后续接入详情弹窗。 */
export const fetchSupplementDetail = (applicationId, userId) => client.get(
  `/supplements/${applicationId}`,
  debugHeaders(userId),
)
