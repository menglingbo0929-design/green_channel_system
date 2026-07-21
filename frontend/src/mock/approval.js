import { APPLICATION_TYPE_META, ROLE_META, STATUS_META } from '../constants/approval'

const wait = (value, delay = 180) => new Promise((resolve) => setTimeout(() => resolve(value), delay))

const seed = [
  ['1001','GC202607200018','GREEN_CHANNEL','GREEN_CHANNEL',1,'20260018','林嘉宁','女',10,'计算机科学与技术学院','软件工程','软件2601','COUNSELOR_PENDING','COUNSELOR',null,1,3860,true,'家庭收入来源单一，希望申请绿色通道缓缴学费。',[]],
  ['1002','SH202607200009','LIVING_SUBSIDY','SUBSIDY',2,'20260043','周子昂','男',10,'计算机科学与技术学院','网络工程','网络2601','COUNSELOR_PENDING','COUNSELOR',null,1,1200,false,'家庭经济困难，申请新生生活补助。',[]],
  ['1003','GC202607190076','GREEN_CHANNEL','GREEN_CHANNEL',1,'20260106','宋雨桐','女',10,'计算机科学与技术学院','人工智能','智能2602','COUNSELOR_PENDING','COUNSELOR','APPROVE',1,5200,true,'已办理生源地助学贷款，申请剩余费用缓缴。',['COUNSELOR']],
  ['2001','GC202607190051','GREEN_CHANNEL','GREEN_CHANNEL',1,'20260027','许浩然','男',10,'计算机科学与技术学院','计算机科学与技术','计科2601','COLLEGE_PENDING','COLLEGE',null,2,4600,true,'父母务农，开学费用暂时存在困难。',['COUNSELOR']],
  ['2002','SH202607190021','TRAVEL_SUBSIDY','SUBSIDY',2,'20260088','贺知遥','女',20,'外国语学院','英语','英语2602','COLLEGE_PENDING','COLLEGE',null,2,800,false,'家庭所在地偏远，申请入学路费补助。',['COUNSELOR']],
  ['2003','GC202607180038','GREEN_CHANNEL','GREEN_CHANNEL',1,'20260142','陈星野','男',30,'环境科学与工程学院','环境工程','环境2601','COLLEGE_PENDING','COLLEGE','APPROVE',2,3100,true,'家庭遭遇临时困难，申请学费缓缴。',['COUNSELOR','COLLEGE']],
  ['3001','GC202607180026','GREEN_CHANNEL','GREEN_CHANNEL',1,'20260065','叶清和','女',20,'外国语学院','翻译','翻译2601','SCHOOL_PENDING','SCHOOL',null,3,6350,true,'已通过困难生认定，申请绿色通道入学。',['COUNSELOR','COLLEGE']],
  ['3002','SH202607180013','LIVING_SUBSIDY','SUBSIDY',2,'20260117','唐沐阳','男',30,'环境科学与工程学院','环境科学','环境2603','SCHOOL_PENDING','SCHOOL',null,3,1500,false,'低保家庭，申请生活补助。',['COUNSELOR','COLLEGE']],
  ['3003','GC202607170011','GREEN_CHANNEL','GREEN_CHANNEL',1,'20260153','沈安然','女',10,'计算机科学与技术学院','数据科学与大数据技术','数据2601','CONFIRM_PENDING','CONFIRMATION','APPROVE',4,4200,true,'申请欠费缓缴并等待学校确认。',['COUNSELOR','COLLEGE','SCHOOL']],
  ['3004','SH202607160008','TRAVEL_SUBSIDY','SUBSIDY',2,'20260179','邵闻溪','男',20,'外国语学院','日语','日语2601','APPROVED','FINISHED','APPROVE',4,900,false,'家庭所在地距离学校较远，申请交通补助。',['COUNSELOR','COLLEGE','SCHOOL']],
]

let applications = seed.map((row, index) => ({
  applicationId: Number(row[0]), applicationNo: row[1], applicationType: row[2], batchType: row[3], batchId: row[4],
  studentId: 2000 + index, studentNo: row[5], studentName: row[6], gender: row[7], collegeId: row[8], collegeName: row[9],
  majorName: row[10], gradeName: '2026级', className: row[11], status: row[12], currentLevel: row[13], latestDecision: row[14],
  submitTime: `2026-07-${20 - Math.floor(index / 2)}T0${8 + (index % 2)}:25:00+08:00`, version: row[15], declaredAmount: row[16],
  hasArrears: row[17], applicationReason: row[18], processedRoles: row[19],
  returnResubmit: ['1003', '2003'].includes(row[0]),
}))
const batchSubmitted = { COUNSELOR: false, COLLEGE: false }
const messages = [
  { messageId: 1, messageType: 'APPROVAL_RETURNED', businessType: 'APPLICATION', businessId: 4103, title: '申请已退回', content: '请补充生源地贷款受理证明后重新提交。', read: false, createTime: '2026-07-20T10:20:00+08:00' },
  { messageId: 2, messageType: 'APPROVAL_APPROVED', businessType: 'APPLICATION', businessId: 4101, title: '申请审核通过', content: '学校已完成最终审核，请关注后续办理通知。', read: false, createTime: '2026-07-19T16:35:00+08:00' },
  { messageId: 3, messageType: 'APPROVAL_REJECTED', businessType: 'APPLICATION', businessId: 4102, title: '申请未通过', content: '当前申请未满足相关条件，如有疑问请联系辅导员。', read: true, createTime: '2026-07-18T09:10:00+08:00' },
]

const myApplications = [
  { applicationId: 4101, applicationNo: 'GC202607200091', applicationType: 'GREEN_CHANNEL', batchType: 'GREEN_CHANNEL', batchId: 1, batchName: '2026 年新生绿色通道', studentId: 9001, studentNo: '20260001', studentName: '张同学', gender: '男', collegeId: 10, collegeName: '计算机科学与技术学院', majorName: '软件工程', gradeName: '2026级', className: '软件2601', status: 'DRAFT', currentLevel: 'STUDENT', currentNode: '待本人提交', submitTime: null, version: 0, declaredAmount: 4800, hasArrears: true, applicationReason: '家庭经济暂时困难，申请绿色通道缓缴学费。' },
  { applicationId: 4102, applicationNo: 'SH202607190036', applicationType: 'LIVING_SUBSIDY', batchType: 'SUBSIDY', batchId: 2, batchName: '2026 年新生生活补助', studentId: 9001, studentNo: '20260001', studentName: '张同学', gender: '男', collegeId: 10, collegeName: '计算机科学与技术学院', majorName: '软件工程', gradeName: '2026级', className: '软件2601', status: 'COUNSELOR_PENDING', currentLevel: 'COUNSELOR', currentNode: '辅导员审核', submitTime: '2026-07-19T09:25:00+08:00', version: 1, declaredAmount: 1200, hasArrears: false, applicationReason: '申请新生生活补助，用于入学初期生活支出。' },
  { applicationId: 4103, applicationNo: 'GC202607180063', applicationType: 'GREEN_CHANNEL', batchType: 'GREEN_CHANNEL', batchId: 1, batchName: '2026 年新生绿色通道', studentId: 9001, studentNo: '20260001', studentName: '张同学', gender: '男', collegeId: 10, collegeName: '计算机科学与技术学院', majorName: '软件工程', gradeName: '2026级', className: '软件2601', status: 'SCHOOL_RETURNED', currentLevel: 'STUDENT', currentNode: '待本人修改', submitTime: '2026-07-18T10:10:00+08:00', version: 4, declaredAmount: 3600, hasArrears: true, applicationReason: '已申请生源地助学贷款，申请缓缴剩余学费。', returnReason: '请补充生源地贷款受理证明。' },
  { applicationId: 4104, applicationNo: 'SH202607150012', applicationType: 'TRAVEL_SUBSIDY', batchType: 'SUBSIDY', batchId: 2, batchName: '2026 年新生路费补助', studentId: 9001, studentNo: '20260001', studentName: '张同学', gender: '男', collegeId: 10, collegeName: '计算机科学与技术学院', majorName: '软件工程', gradeName: '2026级', className: '软件2601', status: 'APPROVED', currentLevel: 'FINISHED', currentNode: '审核已通过', submitTime: '2026-07-15T14:20:00+08:00', version: 4, declaredAmount: 800, hasArrears: false, applicationReason: '家庭所在地距离学校较远，申请入学路费补助。' },
  { applicationId: 4105, applicationNo: 'GC202607120007', applicationType: 'GREEN_CHANNEL', batchType: 'GREEN_CHANNEL', batchId: 1, batchName: '2026 年新生绿色通道', studentId: 9001, studentNo: '20260001', studentName: '张同学', gender: '男', collegeId: 10, collegeName: '计算机科学与技术学院', majorName: '软件工程', gradeName: '2026级', className: '软件2601', status: 'COMPLETED', currentLevel: 'FINISHED', currentNode: '业务已办结', submitTime: '2026-07-12T08:40:00+08:00', version: 5, declaredAmount: 5200, hasArrears: true, applicationReason: '家庭收入来源单一，申请绿色通道办理入学。' },
]

const toListItem = (item) => ({ ...item, applicationTypeName: APPLICATION_TYPE_META[item.applicationType], statusName: STATUS_META[item.status]?.label || item.status })
const roleRecord = (role, action, comment, status, offset = 0) => ({ id: Date.now() + offset, approvalLevel: role, approverName: role === 'STUDENT' ? '学生本人' : ROLE_META[role].userName, action, comment, newStatus: status, createTime: new Date(Date.now() - offset * 3600000).toISOString() })
const matches = (item, filters) => (!filters.batchType || item.batchType === filters.batchType)
  && (!filters.batchId || item.batchId === Number(filters.batchId))
  && (!filters.applicationType || item.applicationType === filters.applicationType)
  && (!filters.studentNo || item.studentNo.includes(filters.studentNo.trim()))
  && (!filters.studentName || item.studentName.includes(filters.studentName.trim()))
  && (!filters.applicationNo || item.applicationNo.includes(filters.applicationNo.trim().toUpperCase()))
  && (!filters.collegeId || item.collegeId === Number(filters.collegeId))

export async function getApprovalList(role, tab, params = {}) {
  const pendingStatus = ROLE_META[role].pendingStatus
  const filtered = applications.filter((item) => {
    const belongs = tab === 'pending'
      ? item.status === pendingStatus && !item.processedRoles.includes(role)
      : tab === 'returned'
        ? item.status === pendingStatus && item.processedRoles.includes(role) && item.returnResubmit
        : item.processedRoles.includes(role)
    return belongs && matches(item, params)
  })
  const page = Number(params.page || 1), size = Number(params.size || 10), start = (page - 1) * size
  return wait({ records: filtered.slice(start, start + size).map(toListItem), total: filtered.length, page, size })
}

export async function getMyApplications(params = {}) {
  const reviewingStatuses = ['COUNSELOR_PENDING', 'COLLEGE_PENDING', 'SCHOOL_PENDING', 'CONFIRM_PENDING']
  const filtered = myApplications.filter((item) => {
    const categoryMatch = params.category === 'DRAFT' ? item.status === 'DRAFT'
      : params.category === 'REVIEWING' ? reviewingStatuses.includes(item.status)
        : params.category === 'RETURNED' ? item.status.endsWith('_RETURNED')
          : params.category === 'APPROVED' ? item.status === 'APPROVED'
            : params.category === 'COMPLETED' ? item.status === 'COMPLETED'
              : true
    const keyword = params.keyword?.trim().toUpperCase()
    return categoryMatch
      && (!params.applicationType || item.applicationType === params.applicationType)
      && (!keyword || item.applicationNo.includes(keyword) || APPLICATION_TYPE_META[item.applicationType].includes(params.keyword.trim()))
  })
  const page = Number(params.page || 1), size = Number(params.size || 10), start = (page - 1) * size
  return wait({ records: filtered.slice(start, start + size).map(toListItem), total: filtered.length, page, size })
}

export async function getMessages(params = {}) {
  const page = Number(params.page || 1)
  const size = Number(params.size || 10)
  const source = params.read === undefined || params.read === null || params.read === ''
    ? messages
    : messages.filter((item) => item.read === (params.read === true || params.read === 'true'))
  return wait({ records: source.slice((page - 1) * size, page * size), total: source.length, page, size })
}

export async function markMessageAsRead(messageId) {
  const message = messages.find((item) => item.messageId === Number(messageId))
  if (!message) throw new Error('消息不存在')
  message.read = true
  return wait({ messageId: message.messageId, read: true })
}

export async function getMyApplicationDetail(applicationId) {
  const item = myApplications.find(record => record.applicationId === Number(applicationId))
  if (!item) throw new Error('申请不存在')
  const records = []
  if (item.status !== 'DRAFT') records.push(roleRecord('STUDENT', 'SUBMIT', '申请材料已提交', 'COUNSELOR_PENDING', 72))
  if (['SCHOOL_RETURNED', 'APPROVED', 'COMPLETED'].includes(item.status)) records.push(roleRecord('COUNSELOR', 'APPROVE', '材料齐全，同意上报学院', 'COLLEGE_PENDING', 52))
  if (['SCHOOL_RETURNED', 'APPROVED', 'COMPLETED'].includes(item.status)) records.push(roleRecord('COLLEGE', 'APPROVE', '学院复核通过', 'SCHOOL_PENDING', 30))
  if (item.status === 'SCHOOL_RETURNED') records.push(roleRecord('SCHOOL', 'RETURN', item.returnReason, 'SCHOOL_RETURNED', 12))
  if (['APPROVED', 'COMPLETED'].includes(item.status)) records.push(roleRecord('SCHOOL', 'APPROVE', '学校审核通过', item.status === 'COMPLETED' ? 'CONFIRM_PENDING' : 'APPROVED', 12))
  if (item.status === 'COMPLETED') records.push({ id: 9999, approvalLevel: 'CONFIRMATION', approverName: '学校管理员', action: 'APPROVE', comment: '欠费金额已确认，业务办结', newStatus: 'COMPLETED', createTime: '2026-07-19T15:30:00+08:00' })
  return wait({
    application: toListItem(item),
    arrearsDetail: item.hasArrears ? { declaredAmount: item.declaredAmount, items: [{ name: '学费', amount: item.declaredAmount }] } : null,
    subsidyDetail: item.applicationType !== 'GREEN_CHANNEL' ? { requestedAmount: item.declaredAmount } : null,
    giftDetail: item.applicationType === 'GREEN_CHANNEL' ? { items: ['爱心生活礼包'] } : null,
    attachments: item.status === 'DRAFT' ? [] : [{ id: 1, fileName: '家庭经济情况说明.pdf', fileSize: '1.8 MB' }],
    approvalRecords: records,
    allowedActions: [],
    editableFields: item.status === 'DRAFT' || item.status.endsWith('_RETURNED') ? ['applicationReason'] : [],
    version: item.version,
  })
}

export async function getApprovalDetail(applicationId, role) {
  const item = applications.find((record) => record.applicationId === Number(applicationId))
  if (!item) throw new Error('申请不存在')
  const records = [roleRecord('STUDENT', 'SUBMIT', '学生完成材料提交', 'COUNSELOR_PENDING', 72)]
  if (item.processedRoles.includes('COUNSELOR')) records.push(roleRecord('COUNSELOR', 'APPROVE', '材料完整，建议通过', item.status, 48))
  if (item.processedRoles.includes('COLLEGE')) records.push(roleRecord('COLLEGE', 'APPROVE', '学院复核通过', item.status, 24))
  if (item.processedRoles.includes('SCHOOL')) records.push(roleRecord('SCHOOL', item.latestDecision || 'APPROVE', '学校完成终审', item.status, 3))
  return wait({
    application: toListItem(item),
    arrearsDetail: item.hasArrears ? { declaredAmount: item.declaredAmount, items: [{ name: '学费', amount: item.declaredAmount }] } : null,
    subsidyDetail: item.applicationType !== 'GREEN_CHANNEL' ? { requestedAmount: item.declaredAmount } : null,
    giftDetail: item.applicationType === 'GREEN_CHANNEL' ? { items: ['床上用品礼包'] } : null,
    attachments: [{ id: 1, fileName: '家庭经济情况说明.pdf', fileSize: '1.8 MB' }, { id: 2, fileName: '困难认定材料.jpg', fileSize: '860 KB' }],
    approvalRecords: records, allowedActions: item.status === ROLE_META[role]?.pendingStatus && !item.processedRoles.includes(role) ? ['APPROVE','RETURN','REJECT'] : [],
    editableFields: ['applicationReason'], version: item.version,
  })
}

export async function reviewApplication(role, applicationId, payload) {
  const item = applications.find((record) => record.applicationId === Number(applicationId))
  if (!item || item.status !== ROLE_META[role].pendingStatus) throw new Error('申请状态已变化，请刷新后重试')
  if (item.version !== payload.version) throw new Error('版本冲突，请重新加载申请详情')
  item.processedRoles.push(role); item.latestDecision = payload.action; item.version += 1
  if (payload.action === 'RETURN') { item.status = `${role}_RETURNED`; item.currentLevel = 'STUDENT' }
  else if (payload.action === 'REJECT') { item.status = 'REJECTED'; item.currentLevel = 'FINISHED' }
  else if (role === 'SCHOOL') { item.status = item.hasArrears ? 'CONFIRM_PENDING' : 'APPROVED'; item.currentLevel = item.hasArrears ? 'CONFIRMATION' : 'FINISHED' }
  return wait({ applicationId: item.applicationId, status: item.status, statusName: STATUS_META[item.status].label, currentLevel: item.currentLevel, version: item.version }, 320)
}

export async function getApprovalDashboard(role) {
  const pending = applications.filter((item) => item.status === ROLE_META[role].pendingStatus && !item.processedRoles.includes(role)).length
  const waiting = applications.filter((item) => item.status === ROLE_META[role].pendingStatus && item.processedRoles.includes(role)).length
  return wait({ pending, approvedWaitingSubmit: waiting, returned: applications.filter((item) => item.status === ROLE_META[role].pendingStatus && item.returnResubmit).length, processed: applications.filter((item) => item.processedRoles.includes(role)).length, deadline: role === 'COLLEGE' ? '07月24日 18:00' : '07月22日 23:59' })
}

export async function getSubmissionStatus(role) {
  const waiting = applications.filter((item) => item.status === ROLE_META[role].pendingStatus && item.processedRoles.includes(role)).length
  const pending = applications.filter((item) => item.status === ROLE_META[role].pendingStatus && !item.processedRoles.includes(role)).length
  return wait({ batchType: 'GREEN_CHANNEL', batchId: 1, submissionLevel: role, initialSubmitted: batchSubmitted[role], submittedAt: batchSubmitted[role] ? new Date().toISOString() : null, pendingReviewCount: pending, approvedWaitingSubmitCount: waiting, returnedCount: 1, rejectedCount: 1, canSubmit: !batchSubmitted[role] && pending === 0 && waiting > 0 })
}

export async function submitInitialBatch(role) {
  if (!['COUNSELOR','COLLEGE'].includes(role)) throw new Error('当前角色不支持批量上报')
  const pending = applications.filter((item) => item.status === ROLE_META[role].pendingStatus && !item.processedRoles.includes(role))
  if (pending.length) throw new Error(`仍有 ${pending.length} 条申请未审核，暂不能上报`)
  const next = role === 'COUNSELOR' ? 'COLLEGE' : 'SCHOOL'
  const waiting = applications.filter((item) => item.status === ROLE_META[role].pendingStatus && item.processedRoles.includes(role))
  waiting.forEach((item) => { item.status = `${next}_PENDING`; item.currentLevel = next; item.latestDecision = null; item.version += 1 })
  batchSubmitted[role] = true
  return wait({ submittedCount: waiting.length, submittedAt: new Date().toISOString() }, 420)
}

export async function submitReturnResubmit(role, payload) {
  const item = applications.find(record => record.applicationId === Number(payload.applicationId))
  if (!item || !item.returnResubmit || item.status !== ROLE_META[role].pendingStatus || !item.processedRoles.includes(role)) {
    throw new Error('当前申请不满足退回补交条件')
  }
  const next = role === 'COUNSELOR' ? 'COLLEGE' : 'SCHOOL'
  item.status = `${next}_PENDING`
  item.currentLevel = next
  item.latestDecision = null
  item.returnResubmit = false
  item.version += 1
  return wait({ applicationId: item.applicationId, status: item.status, statusName: STATUS_META[item.status].label, version: item.version }, 320)
}

export async function cancelApplication(applicationId, payload) {
  const item = applications.find((record) => record.applicationId === Number(applicationId))
  if (!item || !['APPROVED','CONFIRM_PENDING','COMPLETED'].includes(item.status)) throw new Error('当前状态不允许取消')
  if (!payload.reason?.trim()) throw new Error('请填写取消原因')
  item.status = 'CANCELLED'; item.currentLevel = 'FINISHED'; item.version += 1
  return wait({ applicationId: item.applicationId, status: item.status, statusName: '已取消', version: item.version }, 320)
}
