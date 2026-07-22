# 成员三任务 D：跨模块完成适配

更新日期：2026-07-21
负责人：成员三
状态：成员三适配已实现；端到端联调待其他成员依赖就绪。

## 已提供的成员三能力

| 场景 | 可注入实现 | 调用的正式服务 | 事务要求 |
| --- | --- | --- | --- |
| 欠费确认完成 | `ArrearsConfirmationCompletionPortAdapter` | `ApprovalCompletionService.completeAfterConfirmation` | 成员四确认事务必须已开启；适配器使用 `MANDATORY` 加入该事务。 |
| 补录自动审核 | `SupplementCompletionPortAdapter` | `ApprovalTransitionService.completeSupplementReview` | 成员四补录事务必须已开启；适配器使用 `MANDATORY` 加入该事务。 |
| 学校代申请提交 | `ApprovalTransitionService.submitInitial` | 同左 | 成员二提交事务在附件和资源预占成功后调用；不再建立第二套审批状态机。 |

`submitInitial` 统一写入 `SUBMIT` 审核记录，并把 `DRAFT` 推进为
`COUNSELOR_PENDING`。它同时适用于学生首次提交和 `SCHOOL_PROXY` 正式提交。

## 需要其他成员提供的依赖

### 成员一

1. 提供可注入的 `CurrentUserProvider`，返回可信学校操作人。
2. 提供 `SchoolProxyStudentQueryPort` 的有效学生和组织快照查询，供学校代申请建草稿。
3. 补录历史/详情联调还需要按申请 ID 或批量 studentId 查询学生组织快照的能力。

### 成员二

1. 提供并保持可注入的申请状态读写 Service；成员三状态机只通过这些 Service 修改
   `application` 状态和版本。
2. 完成学校代申请附件存储及资源预占。二者成功后，在同一个 `@Transactional` 中调用
   `ApprovalTransitionService.submitInitial(applicationId, version, requestId, operatorUserId)`。
3. 补录创建已直接调用 `ApprovalTransitionService.completeSupplementReview` 时，必须使用
   创建请求的原始 `requestId`，不得拼接新的自动审核请求号；否则创建和审核会具有不同
   的幂等边界。
4. 提供补录详情、历史分页及所需资源写入能力后，再进行真实数据联调。

### 成员四

1. 欠费确认保存 `arrears_confirmation` 后，在同一外层 `@Transactional` 中调用
   `ArrearsConfirmationCompletionPort.completeAfterConfirmation`；任一步失败必须整体回滚。
2. 若补录外层流程使用 `SupplementCompletionPort`，在同一事务中只调用一次。当前成员二
   补录服务已直接调用正式审批 Service 时，成员四不得再次调用该 Port，避免重复推进状态。
3. 确认、补录和学校代申请页面在依赖缺失时应保留明确的 503/501 响应，不得直接写
   `application` 或成员三审核表。

## 联调验收顺序

1. 成员四欠费确认成功，申请从 `CONFIRM_PENDING` 变为 `COMPLETED`，且写入一条确认审核记录。
2. 补录创建成功，含欠费进入 `CONFIRM_PENDING/CONFIRMATION`，无欠费进入 `COMPLETED/SYSTEM`；每次仅写一条自动审核记录。
3. 学校代申请的附件和资源预占成功后，正式提交进入 `COUNSELOR_PENDING` 并写入一条 `SUBMIT` 记录。
4. 对三条链路分别验证重复 `requestId`、版本冲突及外层事务回滚。
