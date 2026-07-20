export const ROLE_META = {
  STUDENT: { title: '我的申请', roleName: '学生', userName: '张同学', levelName: '申请进度' },
  COUNSELOR: { title: '辅导员审核工作台', roleName: '辅导员', userName: '李老师', levelName: '辅导员审核', pendingStatus: 'COUNSELOR_PENDING', nextLevel: '学院', reviewLabel: '完成初审' },
  COLLEGE: { title: '学院审核工作台', roleName: '学院管理员', userName: '王老师', levelName: '学院审核', pendingStatus: 'COLLEGE_PENDING', nextLevel: '学校', reviewLabel: '完成复审' },
  SCHOOL: { title: '学校审核工作台', roleName: '学校管理员', userName: '陈老师', levelName: '学校终审', pendingStatus: 'SCHOOL_PENDING', nextLevel: '办结', reviewLabel: '完成终审' },
}

export const STATUS_META = {
  DRAFT: { label: '草稿', tone: 'neutral' }, COUNSELOR_PENDING: { label: '待辅导员审核', tone: 'warning' },
  COUNSELOR_RETURNED: { label: '辅导员退回', tone: 'danger' }, COLLEGE_PENDING: { label: '待学院审核', tone: 'warning' },
  COLLEGE_RETURNED: { label: '学院退回', tone: 'danger' }, SCHOOL_PENDING: { label: '待学校审核', tone: 'warning' },
  SCHOOL_RETURNED: { label: '学校退回', tone: 'danger' }, REJECTED: { label: '审核不通过', tone: 'danger' },
  APPROVED: { label: '学校审核通过', tone: 'success' }, CONFIRM_PENDING: { label: '待欠费确认', tone: 'info' },
  COMPLETED: { label: '已完成', tone: 'success' }, CANCELLED: { label: '已取消', tone: 'neutral' },
}

export const ACTION_META = {
  SUBMIT: { label: '提交申请', tone: 'info' }, APPROVE: { label: '审核通过', tone: 'success' },
  RETURN: { label: '退回修改', tone: 'warning' }, REJECT: { label: '审核不通过', tone: 'danger' }, CANCEL: { label: '取消申请', tone: 'neutral' },
}

export const APPLICATION_TYPE_META = { GREEN_CHANNEL: '绿色通道', LIVING_SUBSIDY: '生活补助', TRAVEL_SUBSIDY: '路费补助' }

export function formatDateTime(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false }).format(new Date(value))
}

export function createRequestId() { return globalThis.crypto?.randomUUID?.() || `request-${Date.now()}-${Math.random()}` }
