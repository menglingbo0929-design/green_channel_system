import client from './index'

/**
 * 6.1.4 线下补录接口统一使用 /api/supplements。
 *
 * 统一由公共 Axios 拦截器携带 JWT，页面不传递用户 ID 或角色请求头。
 */
/** 按学号查询可补录学生。 */
export const findSupplementStudent = (studentNo) => client.get(
  '/supplements/students',
  { params: { studentNo } },
)

/** 创建线下补录并触发后端自动审核事务。 */
export const createSupplement = (data) => client.post(
  '/supplements',
  data,
)

/** 按确定的四个筛选字段分页查询补录历史。 */
export const fetchSupplementHistory = (params) => client.get(
  '/supplements',
  { params },
)

/** 查询一条补录详情，便于后续接入详情弹窗。 */
export const fetchSupplementDetail = (applicationId) => client.get(
  `/supplements/${applicationId}`,
)
