# 申请审核状态流转

> 当前状态：`APPROVED`。四人第一阶段共识已在 `docs/requirement.md` 固化，本状态机可作为成员三实现依据；后续改变公共状态、事务边界或表所有权时仍须重新走共享变更流程。

本文档是成员三提出的审核模块状态机方案。前端只提交审核动作，不得提交目标状态；目标状态必须由后端 Service 根据当前状态、登录角色和申请类型计算。

成员三负责状态机判断、审核权限和事务编排；`application` 的 DDL、Entity、Mapper 及直接数据库写入由成员二负责。成员三计算出合法目标状态后，必须调用成员二提供的 `ApplicationStateWriteService` 完成条件更新，不得创建 `application` 写 Mapper。

## 1. 状态定义

| 状态 | 含义 | `current_level` | 是否允许学生编辑 |
|---|---|---|---|
| `DRAFT` | 草稿 | `STUDENT` | 是 |
| `COUNSELOR_PENDING` | 待辅导员审核或等待辅导员补交 | `COUNSELOR` | 否 |
| `COUNSELOR_RETURNED` | 辅导员退回学生修改 | `STUDENT` | 是 |
| `COLLEGE_PENDING` | 待学院审核或等待学院补交 | `COLLEGE` | 否 |
| `COLLEGE_RETURNED` | 学院退回学生修改 | `STUDENT` | 是 |
| `SCHOOL_PENDING` | 待学校审核 | `SCHOOL` | 否 |
| `SCHOOL_RETURNED` | 学校退回学生修改 | `STUDENT` | 是 |
| `REJECTED` | 审核不通过，流程终止 | `FINISHED` | 否 |
| `APPROVED` | 学校审核通过，不需要欠费确认或尚待线下办结 | `FINISHED` | 否 |
| `CONFIRM_PENDING` | 学校审核通过，等待欠费最终确认 | `CONFIRMATION` | 否 |
| `COMPLETED` | 业务已办结 | `FINISHED` | 否 |
| `CANCELLED` | 学校取消申请 | `FINISHED` | 否 |

`current_level` 只允许使用：

```text
STUDENT
COUNSELOR
COLLEGE
SCHOOL
CONFIRMATION
FINISHED
```

## 2. 审核动作

| 动作 | 含义 | 意见要求 |
|---|---|---|
| `APPROVE` | 当前审核人逐条审核通过 | 可选；补助申请必须填写最终补助金额 |
| `RETURN` | 退回学生修改 | 必填 |
| `REJECT` | 审核不通过并终止流程 | 必填 |
| `MODIFY` | 审核人修改允许编辑的非学生基本信息 | 修改说明必填 |
| `SUBMIT` | 学生重新提交、首次批量上报或退回补交 | 系统生成说明 |
| `CANCEL` | 学校取消已通过或已完成申请 | 必填 |

每次动作都必须写入 `approval_record`。即使动作不改变 `application.status`，也要保存相同的原状态和新状态。

## 3. 逐条审核与批量上报

### 3.1 统一规则

辅导员和学院采用“先逐条审核，再整体上报”：

1. 逐条执行 `APPROVE` 时，只写审核记录，申请仍停留在当前 `*_PENDING` 状态。
2. 执行 `RETURN` 或 `REJECT` 时立即改变状态，不进入本次批量上报。
3. 首次批量上报时，后端查询当前数据范围内所有仍为待审核且最新审核结论为 `APPROVE` 的申请，统一推进到下一节点。
4. 首次上报后，本批次、当前审核范围被锁定，不能再次执行首次上报。
5. 学院或学校退回的单条申请，学生重新提交后回到辅导员节点，再通过 `RETURN_RESUBMIT` 逐级补交，不受首次上报“一次”限制。
6. 批量上报不能由前端任意选择部分数据；前端只提交批次 ID，提交范围由后端权限和最新审核结论计算。

### 3.2 辅导员首次上报

前置条件：

- 当前时间晚于学生申请截止时间；
- 当前辅导员在该批次尚未执行过 `INITIAL_BATCH`；
- 数据范围内不存在尚未给出审核结论的 `COUNSELOR_PENDING` 申请；
- 上报申请的最新辅导员审核结论均为 `APPROVE`。

状态变化：

```text
COUNSELOR_PENDING -> COLLEGE_PENDING
current_level: COUNSELOR -> COLLEGE
```

### 3.3 学院首次上报

前置条件：

- 当前时间不晚于学院提交截止时间；
- 当前学院在该批次尚未执行过 `INITIAL_BATCH`；
- 学院范围内不存在尚未给出审核结论的 `COLLEGE_PENDING` 申请；
- 上报申请的最新学院审核结论均为 `APPROVE`；
- 礼包名额、学院补助额度等业务校验通过。

状态变化：

```text
COLLEGE_PENDING -> SCHOOL_PENDING
current_level: COLLEGE -> SCHOOL
```

## 4. 完整状态流转矩阵

| 当前状态 | 登录角色 | 动作 | 新状态 | 说明 |
|---|---|---|---|---|
| `DRAFT` | 学生 | 正式提交 | `COUNSELOR_PENDING` | 由申请模块执行，成员三提供状态约定 |
| `COUNSELOR_PENDING` | 辅导员 | `APPROVE` | `COUNSELOR_PENDING` | 记录逐条审核结论，等待上报 |
| `COUNSELOR_PENDING` | 辅导员 | `RETURN` | `COUNSELOR_RETURNED` | 退回原因必填 |
| `COUNSELOR_PENDING` | 辅导员 | `REJECT` | `REJECTED` | 释放预占资源 |
| `COUNSELOR_PENDING` | 辅导员 | 首次上报/补交 | `COLLEGE_PENDING` | 批次上报或退回补交 |
| `COLLEGE_PENDING` | 学院 | `APPROVE` | `COLLEGE_PENDING` | 记录逐条审核结论，等待上报 |
| `COLLEGE_PENDING` | 学院 | `RETURN` | `COLLEGE_RETURNED` | 退回原因必填 |
| `COLLEGE_PENDING` | 学院 | `REJECT` | `REJECTED` | 释放预占资源 |
| `COLLEGE_PENDING` | 学院 | 首次上报/补交 | `SCHOOL_PENDING` | 截止时间校验 |
| `SCHOOL_PENDING` | 学校 | `RETURN` | `SCHOOL_RETURNED` | 退回原因必填 |
| `SCHOOL_PENDING` | 学校 | `REJECT` | `REJECTED` | 释放预占资源 |
| `SCHOOL_PENDING` | 学校 | `APPROVE` | `APPROVED` 或 `CONFIRM_PENDING` | 由申请类型和是否包含欠费决定 |
| 任一 `*_RETURNED` | 学生 | 重新提交 | `COUNSELOR_PENDING` | 只能修改原申请，历史记录保留 |
| `APPROVED`/`CONFIRM_PENDING`/`COMPLETED` | 学校 | `CANCEL` | `CANCELLED` | 必须完成资源回滚及关联单据处理 |
| `CONFIRM_PENDING` | 学校确认模块 | 确认完成 | `COMPLETED` | 由成员四负责 |
| `APPROVED` | 后续办结模块 | 办结 | `COMPLETED` | 如项目不区分线下办结，可保留 `APPROVED` 为终态 |

## 5. 学校最终审核状态

学校审核通过时按以下规则计算：

| 申请情况 | 审核后状态 |
|---|---|
| `GREEN_CHANNEL` 且包含欠费申请 | `CONFIRM_PENDING` |
| `GREEN_CHANNEL` 且不包含欠费申请 | `APPROVED` |
| `LIVING_SUBSIDY` | `APPROVED` |
| `TRAVEL_SUBSIDY` | `APPROVED` |

学校审核通过必须在同一事务内确认礼包、名额或补助额度占用。任何资源确认失败都必须回滚审核记录和状态变化。

## 6. 退回与重新提交

```text
COUNSELOR_PENDING -> COUNSELOR_RETURNED
COLLEGE_PENDING   -> COLLEGE_RETURNED
SCHOOL_PENDING    -> SCHOOL_RETURNED

任一 RETURNED -> 学生修改原申请 -> COUNSELOR_PENDING
```

规则：

- 学生只能修改申请业务内容，不能修改学校导入的学号、学院、专业、年级和班级等基本信息。
- 学生重新提交时写 `SUBMIT` 审核记录，保留此前全部记录。
- 重新提交后，辅导员和学院必须重新审核。
- 首次批量上报已经完成时，辅导员和学院使用 `RETURN_RESUBMIT` 逐条补交。
- `REJECTED` 不能直接修改或重新提交；如学校允许再次申请，应由申请模块按新申请规则处理。
- 退回时预占资源默认暂时保留，具体保留时长由批次配置决定；超时释放由资源模块处理。

## 7. 取消规则

只有学校管理员可以取消 `APPROVED`、`CONFIRM_PENDING` 或 `COMPLETED` 申请，取消原因必填。

取消操作必须在一个业务事务中协调完成：

1. 校验申请尚未发生不可逆线下发放；
2. 写入 `CANCEL` 审核记录和操作日志；
3. 将申请变为 `CANCELLED`；
4. 释放礼包库存、学院/年级名额和补助额度；
5. 已生成欠费单据时标记作废，不物理删除；
6. 从有效统计口径排除，但保留历史查询。

任一回滚步骤失败时，整个取消事务失败，申请状态保持不变。

## 8. 数据权限

| 角色 | 查询范围 | 可执行操作 |
|---|---|---|
| 学生 | 本人申请 | 查看流程、修改退回申请、重新提交 |
| 辅导员 | 本人负责学生 | 逐条审核、修改允许字段、批次上报、退回补交 |
| 学院管理员 | 本学院申请 | 逐条审核、修改允许字段、批次上报、退回补交 |
| 学校管理员 | 全校申请 | 最终审核、取消、查看全部流程 |

所有权限必须由后端根据 JWT 当前用户及关联数据校验，不能信任前端传入的辅导员 ID、学院 ID 或学生 ID。

## 9. 幂等与并发

- 审核、上报、重新提交和取消请求必须携带唯一 `requestId`。
- `approval_record.request_id` 建立唯一约束，重复请求直接返回首次处理结果。
- 更新 `application` 时同时校验 `status` 和 `version`，成功后 `version + 1`。
- 首次批量上报按“批次 + 层级 + 数据范围 + `INITIAL_BATCH`”建立唯一约束。
- 资源确认和释放必须使用事务、行锁、乐观锁或原子条件更新，禁止先查询余额再无条件更新。

## 10. 统一异常码

| 错误码 | 含义 |
|---|---|
| `APPROVAL_INVALID_STATUS` | 当前状态不允许执行该动作 |
| `APPROVAL_FORBIDDEN_SCOPE` | 无权访问或处理该申请 |
| `APPROVAL_ACTION_REQUIRED` | 缺少审核动作 |
| `APPROVAL_COMMENT_REQUIRED` | 退回、拒绝或取消原因未填写 |
| `APPROVAL_ALREADY_PROCESSED` | 请求已处理或当前层级已有冲突结论 |
| `APPROVAL_VERSION_CONFLICT` | 申请已被其他人更新 |
| `APPROVAL_UNREVIEWED_EXISTS` | 批次范围内仍有未审核申请 |
| `APPROVAL_BATCH_NOT_CLOSED` | 学生申请尚未截止，辅导员不能上报 |
| `APPROVAL_BATCH_ALREADY_SUBMITTED` | 当前范围已完成首次上报 |
| `APPROVAL_COLLEGE_DEADLINE_EXPIRED` | 学院提交已超过截止时间 |
| `APPROVAL_QUOTA_INSUFFICIENT` | 库存、名额或补助额度不足 |
| `APPROVAL_CANCEL_NOT_ALLOWED` | 当前申请不能直接取消 |
| `APPROVAL_RESOURCE_ROLLBACK_FAILED` | 取消时资源回滚失败 |

## 11. 审核记录展示顺序

流程查询按 `create_time ASC, id ASC` 返回审核记录，至少包含：

```text
applicationId
approvalLevel
approverId
approverName
action
comment
oldStatus
newStatus
createTime
```

前端根据记录渲染时间轴，不根据当前状态猜测历史节点。
