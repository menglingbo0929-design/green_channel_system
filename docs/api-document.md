# 接口文档

> 审核模块接口当前状态：`PROPOSED`。成员二、成员三、成员四确认并在 `docs/change-log.md` 更新状态后，才允许按本文档实现共享接口。

本文档当前定义成员三提出的审核模块接口方案。其他成员新增接口时必须继续使用相同的返回格式、分页结构和错误码风格。

## 1. 公共约定

### 1.1 基础路径与身份

- 基础路径：`/api`
- 身份来源：`Authorization: Bearer <token>`
- 后端必须从 JWT 获取当前用户、角色和数据范围，不接受前端传入身份范围字段。
- 时间格式：ISO 8601，例如 `2026-07-17T10:30:00+08:00`。
- 金额使用十进制定点数，JSON 中以数值返回，后端使用 `BigDecimal`。
- 审核模块只直接写入 `approval_record`、`approval_submission_record`、`system_message`、`message_read_record`。
- `application` 的状态更新调用成员二提供的 `ApplicationStateWriteService`；库存、名额和额度变化调用成员二的资源 Service；欠费单据操作调用成员四的单据 Service。

### 1.2 统一返回格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

分页数据：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 0,
    "page": 1,
    "size": 10
  }
}
```

业务校验失败时，HTTP 状态码和 `code` 均应表达失败，`message` 提供可展示提示；不能把失败包装成 `200 success`。

### 1.3 通用分页条件

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `page` | integer | 否 | 默认 1 |
| `size` | integer | 否 | 默认 10，最大 100 |
| `batchId` | long | 否 | 批次 ID |
| `applicationType` | string | 否 | `GREEN_CHANNEL/LIVING_SUBSIDY/TRAVEL_SUBSIDY` |
| `applicationNo` | string | 否 | 申请编号 |
| `studentNo` | string | 否 | 学号 |
| `studentName` | string | 否 | 学生姓名，模糊查询 |
| `collegeId` | long | 否 | 仅学校管理员可作为筛选项 |
| `status` | string | 否 | 申请状态 |

## 2. 公共响应对象

### 2.1 审核列表项 `ApprovalListItemVO`

```json
{
  "applicationId": 1001,
  "applicationNo": "GC202607170001",
  "applicationType": "GREEN_CHANNEL",
  "applicationTypeName": "绿色通道",
  "studentId": 2001,
  "studentNo": "20260001",
  "studentName": "张三",
  "collegeId": 10,
  "collegeName": "计算机学院",
  "gradeName": "2026级",
  "status": "COUNSELOR_PENDING",
  "statusName": "待辅导员审核",
  "currentLevel": "COUNSELOR",
  "latestDecision": null,
  "submitTime": "2026-07-17T09:00:00+08:00",
  "version": 1
}
```

`latestDecision` 是当前审核层级的最新逐条审核结论，值为 `APPROVE/RETURN/REJECT` 或 `null`。辅导员和学院逐条通过后状态仍保持当前待审状态，直到批量上报。

### 2.2 审核流程记录 `ApprovalRecordVO`

```json
{
  "id": 5001,
  "applicationId": 1001,
  "approvalLevel": "COUNSELOR",
  "approverId": 301,
  "approverName": "李老师",
  "action": "APPROVE",
  "comment": "材料完整",
  "oldStatus": "COUNSELOR_PENDING",
  "newStatus": "COUNSELOR_PENDING",
  "createTime": "2026-07-17T10:30:00+08:00"
}
```

### 2.3 状态结果 `ApprovalStatusVO`

```json
{
  "applicationId": 1001,
  "status": "COLLEGE_PENDING",
  "statusName": "待学院审核",
  "currentLevel": "COLLEGE",
  "version": 2
}
```

## 3. 审核查询接口

### 3.1 查询待审核列表

```http
GET /api/approvals/pending
```

使用角色：`COUNSELOR/COLLEGE/SCHOOL`

数据范围：

- 辅导员：本人负责学生且状态为 `COUNSELOR_PENDING`；
- 学院：本学院且状态为 `COLLEGE_PENDING`；
- 学校：全校且状态为 `SCHOOL_PENDING`。

请求参数：使用 1.3 中的分页条件。返回 `ApprovalListItemVO` 分页数据。

### 3.2 查询已审核列表

```http
GET /api/approvals/processed
```

使用角色：`COUNSELOR/COLLEGE/SCHOOL`

“已审核”以当前用户或当前管理层级已经产生有效审核记录为准，不只根据申请当前状态判断。返回 `ApprovalListItemVO` 分页数据。

### 3.3 查询审核详情

```http
GET /api/approvals/{applicationId}
```

使用角色：`STUDENT/COUNSELOR/COLLEGE/SCHOOL`

权限：学生本人、负责该学生的辅导员、本学院管理员或学校管理员。

返回内容：

```json
{
  "application": {},
  "arrearsDetail": null,
  "giftDetail": null,
  "subsidyDetail": null,
  "attachments": [],
  "approvalRecords": [],
  "editableFields": [],
  "allowedActions": ["APPROVE", "RETURN", "REJECT"],
  "version": 1
}
```

申请明细和附件由成员二提供，审核模块负责按角色裁剪 `editableFields` 和 `allowedActions`。

### 3.4 查询当前状态

```http
GET /api/approvals/{applicationId}/status
```

使用角色：`STUDENT/COUNSELOR/COLLEGE/SCHOOL`

返回 `ApprovalStatusVO`。

### 3.5 查询完整审核流程

```http
GET /api/approvals/{applicationId}/flow
```

使用角色：`STUDENT/COUNSELOR/COLLEGE/SCHOOL`

返回：

```json
{
  "applicationId": 1001,
  "status": "COLLEGE_PENDING",
  "statusName": "待学院审核",
  "currentLevel": "COLLEGE",
  "currentLevelName": "学院审核",
  "returnReason": null,
  "rejectReason": null,
  "records": []
}
```

记录按 `createTime ASC, id ASC` 排序。

## 4. 逐条审核接口

### 4.1 辅导员审核

```http
POST /api/approvals/counselor/{applicationId}/review
```

使用角色：`COUNSELOR`

允许当前状态：`COUNSELOR_PENDING`

请求：

```json
{
  "action": "APPROVE",
  "comment": "材料完整",
  "finalSubsidyAmount": null,
  "version": 1,
  "requestId": "3d508cc2-d0a2-4a15-94c0-3581168e4a4b"
}
```

规则：

- 只能处理本人负责学生；
- `RETURN/REJECT` 的 `comment` 必填；
- 补助申请执行 `APPROVE` 时 `finalSubsidyAmount` 必填并校验年级额度；
- `APPROVE` 只记录结论，状态仍为 `COUNSELOR_PENDING`；
- `RETURN` 变为 `COUNSELOR_RETURNED`；
- `REJECT` 变为 `REJECTED` 并触发资源释放。

返回 `ApprovalStatusVO`。

### 4.2 学院审核

```http
POST /api/approvals/college/{applicationId}/review
```

使用角色：`COLLEGE`

允许当前状态：`COLLEGE_PENDING`

请求：

```json
{
  "action": "APPROVE",
  "comment": "同意",
  "version": 2,
  "requestId": "15fd4f64-838a-45d0-8717-e19db0eb489b"
}
```

规则：

- 只能处理本学院申请；
- `APPROVE` 前检查学院礼包名额或学院补助额度；
- `APPROVE` 只记录结论，状态仍为 `COLLEGE_PENDING`；
- `RETURN` 变为 `COLLEGE_RETURNED`；
- `REJECT` 变为 `REJECTED` 并触发资源释放。

返回 `ApprovalStatusVO`。

### 4.3 学校最终审核

```http
POST /api/approvals/school/{applicationId}/review
```

使用角色：`SCHOOL`

允许当前状态：`SCHOOL_PENDING`

请求结构与学院审核相同。

规则：

- `RETURN` 变为 `SCHOOL_RETURNED`；
- `REJECT` 变为 `REJECTED` 并释放预占资源；
- `APPROVE` 时确认资源占用；
- 包含欠费的绿色通道申请变为 `CONFIRM_PENDING`；
- 其他申请变为 `APPROVED`；
- 状态、审核记录和资源确认必须在同一事务内成功或回滚。

返回 `ApprovalStatusVO`。

### 4.4 修改审核允许字段

```http
PUT /api/approvals/{applicationId}/editable-fields
```

使用角色：`COUNSELOR/COLLEGE/SCHOOL`

请求：

```json
{
  "fields": {
    "applicationReason": "补充后的申请原因"
  },
  "comment": "根据纸质材料更正",
  "version": 2,
  "requestId": "d1f5997e-fe02-47f2-a1d5-c7ea2e1f3f12"
}
```

后端必须按申请类型和审核层级使用字段白名单，禁止修改学生基本信息、申请人、批次、申请来源和主键。成功后写 `MODIFY` 记录。

## 5. 批量上报接口

### 5.1 查询批次上报状态

```http
GET /api/approval-submissions/status?batchId={batchId}
```

使用角色：`COUNSELOR/COLLEGE`

返回：

```json
{
  "batchId": 1,
  "submissionLevel": "COUNSELOR",
  "applicationDeadline": "2026-08-20T23:59:59+08:00",
  "collegeDeadline": "2026-08-25T23:59:59+08:00",
  "initialSubmitted": false,
  "submittedAt": null,
  "pendingReviewCount": 3,
  "approvedWaitingSubmitCount": 18,
  "returnedCount": 1,
  "rejectedCount": 2,
  "canSubmit": false
}
```

### 5.2 辅导员首次上报学院

```http
POST /api/approval-submissions/counselor/initial
```

使用角色：`COUNSELOR`

请求：

```json
{
  "batchId": 1,
  "requestId": "1e2067f6-2f80-4acc-82f8-462621f611f8"
}
```

后端按当前辅导员范围自动计算上报申请，不能接受前端传入申请 ID 子集。学生申请截止后才能提交，同一批次当前辅导员只能提交一次。

### 5.3 学院首次上报学校

```http
POST /api/approval-submissions/college/initial
```

使用角色：`COLLEGE`

请求结构同 5.2。后端按当前学院自动计算范围，必须在学院截止时间前提交。

### 5.4 退回后补交

```http
POST /api/approval-submissions/return-resubmit
```

使用角色：`COUNSELOR/COLLEGE`

请求：

```json
{
  "applicationId": 1001,
  "version": 5,
  "requestId": "fb52a1c4-7f7e-43b1-bc3d-e78cc7ddd381"
}
```

规则：

- 只处理曾被学院或学校退回并由学生重新提交的申请；
- 当前层级必须已重新执行 `APPROVE`；
- 辅导员补交：`COUNSELOR_PENDING -> COLLEGE_PENDING`；
- 学院补交：`COLLEGE_PENDING -> SCHOOL_PENDING`；
- 写入 `approval_submission_record`，类型为 `RETURN_RESUBMIT`。

## 6. 学生重新提交

```http
POST /api/approvals/{applicationId}/resubmit
```

使用角色：`STUDENT`

允许状态：`COUNSELOR_RETURNED/COLLEGE_RETURNED/SCHOOL_RETURNED`

请求：

```json
{
  "version": 4,
  "requestId": "f11200f0-2314-4f2a-84bb-4e677df81f4e"
}
```

申请内容修改由成员二接口完成，本接口只负责最终校验、写 `SUBMIT` 记录并转换为 `COUNSELOR_PENDING`。

## 7. 学校取消申请

```http
POST /api/approvals/{applicationId}/cancel
```

使用角色：`SCHOOL`

允许状态：`APPROVED/CONFIRM_PENDING/COMPLETED`

请求：

```json
{
  "reason": "学生主动放弃",
  "version": 8,
  "requestId": "fc2d3348-67ec-4dd4-9ce1-60c49dc3b4f1"
}
```

取消必须同步释放资源、作废关联单据、写审核记录和操作日志。任一步失败均回滚，不能只修改申请状态。

## 8. 审核工作台

```http
GET /api/approvals/dashboard?batchId={batchId}
```

使用角色：`COUNSELOR/COLLEGE/SCHOOL`

数据按登录角色自动裁剪，返回：

```json
{
  "pendingByLevel": [
    {"level": "COUNSELOR", "count": 20},
    {"level": "COLLEGE", "count": 15},
    {"level": "SCHOOL", "count": 8}
  ],
  "decisionDistribution": [
    {"action": "APPROVE", "count": 30},
    {"action": "RETURN", "count": 5},
    {"action": "REJECT", "count": 2}
  ],
  "pendingByCollege": [],
  "flowFunnel": []
}
```

## 9. 审核状态消息

`system_message` 和 `message_read_record` 由成员三负责，所有角色通过以下接口读取和标记本人消息。

### 9.1 查询本人消息

```http
GET /api/messages?page=1&size=10&read=false
```

使用角色：`STUDENT/COUNSELOR/COLLEGE/SCHOOL`

后端从当前登录用户确定接收人，不接受前端传入 `receiverUserId`。返回：

```json
{
  "records": [
    {
      "messageId": 1,
      "messageType": "APPROVAL_RETURNED",
      "businessType": "APPLICATION",
      "businessId": 1001,
      "title": "申请已退回",
      "content": "请修改材料后重新提交",
      "read": false,
      "createTime": "2026-07-17T10:30:00+08:00"
    }
  ],
  "total": 1,
  "page": 1,
  "size": 10
}
```

### 9.2 标记消息已读

```http
POST /api/messages/{messageId}/read
```

使用角色：`STUDENT/COUNSELOR/COLLEGE/SCHOOL`

请求：

```json
{
  "requestId": "060613c3-5f89-4ae2-8610-709df9aab06e"
}
```

只能标记发送给当前用户的消息。重复调用不重复写入 `message_read_record`。

## 10. 幂等与并发参数

- 所有写接口必须携带 `requestId` 和当前 `version`。
- 相同 `requestId` 重复调用时返回首次结果，不重复写审核记录。
- `version` 不一致返回 `APPROVAL_VERSION_CONFLICT`。
- 前端收到版本冲突后必须重新加载详情，不得自动重试旧请求。

## 11. 审核模块错误码

错误码以 [status-flow.md](./status-flow.md) 第 10 节为准，包括：

```text
APPROVAL_INVALID_STATUS
APPROVAL_FORBIDDEN_SCOPE
APPROVAL_ACTION_REQUIRED
APPROVAL_COMMENT_REQUIRED
APPROVAL_ALREADY_PROCESSED
APPROVAL_VERSION_CONFLICT
APPROVAL_UNREVIEWED_EXISTS
APPROVAL_BATCH_NOT_CLOSED
APPROVAL_BATCH_ALREADY_SUBMITTED
APPROVAL_COLLEGE_DEADLINE_EXPIRED
APPROVAL_QUOTA_INSUFFICIENT
APPROVAL_CANCEL_NOT_ALLOWED
APPROVAL_RESOURCE_ROLLBACK_FAILED
```

## 12. 6.1.2 欠费单据接口（PROPOSED，受外部依赖阻塞）

本节路径和响应字段已固定；成员四不得以模拟数据替代成员一、成员二未合入的真实能力。依赖未合入时服务返回 HTTP 503，不能作为成功接口联调结果。

### 12.1 学校分页查询确认单

```http
GET /api/arrears-vouchers?pageNo=1&pageSize=10&voucherNo=GC2026000001
X-User-Id: 1
```

使用角色：`SCHOOL`。`pageNo` 默认 `1`，`pageSize` 默认 `10`、最大 `100`；`voucherNo` 可选，用于精确筛选。只返回 `arrears_confirmation.deleted = 0` 的记录。

### 12.2 学校预览与打印数据

```http
GET /api/arrears-vouchers/{voucherNo}
GET /api/arrears-vouchers/{voucherNo}/print
X-User-Id: 1
```

使用角色：`SCHOOL`。预览响应的 `printTime` 为 `null`；打印接口的 `printTitle` 固定为“高校绿色通道欠费确认单”，`printTime` 为本次后端请求时间。后端只提供打印数据，浏览器打印由前端触发。

### 12.3 学生查看本人单据

```http
GET /api/student/arrears-vouchers/{voucherNo}
X-User-Id: 2001
```

使用角色：`STUDENT`。成员一的权限实现必须校验该用户实际拥有对应 `applicationId`；不得相信前端传递的学号、学生 ID、学院 ID 或角色。

### 12.4 单据响应 `ArrearsVoucherVO`

四个接口统一返回以下 `data`：

```json
{
  "voucherNo": "GC2026000001",
  "applicationId": 1,
  "studentNo": "20260001",
  "studentName": "张三",
  "collegeName": "计算机学院",
  "majorName": "软件工程",
  "gradeName": "2026级",
  "className": "软件工程1班",
  "arrearsItems": [{"feeItemName": "学费", "declaredAmount": 5000.00}],
  "appliedAmount": 5000.00,
  "confirmedAmount": 4500.00,
  "confirmedTime": "2026-07-18T10:30:00",
  "confirmUserId": 1,
  "confirmUserName": "学校管理员",
  "printTitle": "高校绿色通道欠费确认单",
  "printTime": null
}
```

`arrearsItems`、学生组织字段由成员二的申请快照能力提供；`confirmUserName` 和访问控制由成员一提供。任一依赖缺失时，返回 `503 Service Unavailable`，不返回字段缺失的 200 响应。

## 13. 6.1.5、6.1.6 学校申请统计（PROPOSED，受跨模块依赖阻塞）

```http
GET /api/statistics/applications/summary
```

使用角色：`SCHOOL`。只统计 `deleted = 0` 且状态为 `APPROVED` 或 `COMPLETED` 的申请；任何待审核、退回、拒绝、`CONFIRM_PENDING` 与已取消数据均不参与统计。成员一可信当前用户、成员二的集合聚合查询及欠费原因字段没有合入时，接口必须返回 HTTP 503。

### 13.1 查询参数

| 参数 | 类型 | 说明 |
|---|---|---|
| `batchType` | string | `GREEN_CHANNEL` / `SUBSIDY`；传 `batchId` 时必填 |
| `batchId` | long | 批次 ID，与 `batchType` 一起定位历史或当前批次 |
| `collegeId`、`majorId`、`gradeId`、`classId` | long | 组织筛选 ID，均须大于 0 |
| `applicationType` | string | `GREEN_CHANNEL` / `LIVING_SUBSIDY` / `TRAVEL_SUBSIDY` |
| `applicationStatus` | string | 仅 `APPROVED` / `COMPLETED`；不传时两种状态均统计 |
| `feeItemId` | long | 欠费项目 ID |
| `applicationStartTime`、`applicationEndTime` | ISO 8601 datetime | 按申请创建时间闭区间筛选 |

`batchType=GREEN_CHANNEL` 只能搭配 `applicationType=GREEN_CHANNEL`；`batchType=SUBSIDY` 不可搭配 `GREEN_CHANNEL`。非法组合返回 400。

### 13.2 响应数据

```json
{
  "finalApplicantCount": 0,
  "completedStudentCount": 0,
  "feeItemApplicantCount": 0,
  "confirmedArrearsAmount": 0.00,
  "collegeApplicantCounts": [{"collegeId": 1, "collegeName": "计算机学院", "applicantCount": 0}],
  "gradeApplicantCounts": [{"gradeId": 1, "gradeName": "2026级", "applicantCount": 0}],
  "arrearsReasonStatistics": [{"arrearsReasonCode": "FAMILY_FINANCIAL_DIFFICULTY", "arrearsReasonName": "家庭经济困难", "applicantCount": 0, "confirmedAmount": 0.00}],
  "giftItemApplicationCounts": [{"giftItemId": 1, "giftItemName": "爱心礼包", "applicationCount": 0}],
  "batchHistoryStatistics": [{"batchType": "GREEN_CHANNEL", "batchId": 1, "batchName": "2026 年绿色通道", "applicantCount": 0, "completedStudentCount": 0, "confirmedArrearsAmount": 0.00}]
}
```

`finalApplicantCount` 是最终状态范围内按 `studentId` 去重人数；`completedStudentCount` 只计 `COMPLETED`；`confirmedArrearsAmount` 只累计已完成申请关联的有效 `arrears_confirmation.confirmed_amount`。无真实数据时字段为零或空数组；该零值只能由真实聚合结果产生，不能用模拟数据返回。
