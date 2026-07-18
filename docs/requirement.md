# 高校绿色通道系统需求说明

## 四人并行开发第一阶段共识

> 状态：`APPROVED`
> 确认范围：成员一基础数据与批次、成员二申请配置、成员三审核流程、成员四确认与统计。
> 目标：固定跨模块边界和第一阶段主流程，各成员可以据此开始实现；未列出的页面样式、提示文案和内部实现细节由模块负责人自行决定。

### 1. 统一申请模型

- 全系统只使用一张申请主表 `application`。
- 绿色通道、生活补助和路费补助通过 `application_type` 区分。
- 欠费、礼包和补助的具体内容分别保存在详情表，不再建立第二套申请主表。
- 同一学生、同一批次、同一申请类型只能存在一条有效申请。
- `application` 的 DDL、Entity、Mapper 和直接数据库写入由成员二负责。

### 2. 批次关联方案

成员一和成员二确认采用“两列可空外键加批次类型”方案：

```text
batch_type
green_channel_batch_id
subsidy_batch_id
```

- 两个批次外键有且只能有一个非空，并与 `batch_type` 保持一致。
- 对外 DTO 统一返回 `batchType + batchId`，不暴露内部两列选择逻辑。
- 审核上报和统计查询使用同一批次类型规则。

### 3. 表所有权和跨模块写入

- 成员一维护用户、角色、学生、学院、专业、年级、班级和批次基础数据。
- 成员二维护申请主表、申请详情、附件、礼包、库存、名额、补助额度和申请操作幂等记录。
- 成员三维护审核记录、审核上报记录、消息，并负责状态机和审核权限。
- 成员四维护欠费确认记录并负责确认、代申请、补录和统计业务编排。
- 只有表负责人模块可以直接写本表；其他模块必须调用负责人提供的 Service，不得建立第二个写 Mapper。

### 4. 成员一向申请模块提供的能力

成员一确认向成员二提供以下可信后端能力：

```text
CurrentUserProvider
StudentProfileQueryService
StudentScopeService
BatchQueryService
OrganizationQueryService
PolicyRuleQueryService
```

用户 ID、学生 ID、角色、学院和数据范围必须从登录上下文及基础数据中获取，不能信任前端传入的身份字段。

### 5. 成员二向审核与确认模块提供的能力

成员二负责提供：

```text
ApplicationStateQueryService
ApplicationStateWriteService
ApplicationDetailService
ApplicationResourceService
ArrearsConfirmationApplicationPort
ApplicationCreationService
ApplicationStatisticsQueryService 或双方确认的只读聚合 SQL
```

- 状态写入同时校验当前状态、`version` 和 `deleted = 0`。
- 库存、名额和额度使用事务及原子条件更新。
- 写操作使用 `requestId` 保证幂等，申请使用 `version` 保证乐观锁。
- 统计采用面向集合的查询，禁止逐条跨模块调用后再汇总。

### 6. 成员三审核能力

成员三负责提供：

```text
ApprovalTransitionService
ApprovalCompletionService
ApprovalFlowQueryService
```

- 成员三判断合法状态、角色权限和目标审核层级。
- 成员三通过成员二提供的状态写入 Service 更新 `application`，不直接建立 `application` Mapper。
- 首次提交和重新提交写入 `SUBMIT` 审核记录。
- 审核、退回、拒绝、取消和批量上报必须保留审核或上报记录。

### 7. 欠费确认接口共识

成员四现有对外路径第一阶段保持不变：

```http
GET  /api/confirm/list
GET  /api/confirm/app/{applicationId}
POST /api/confirm/{applicationId}
```

确认请求增加：

```json
{
  "confirmedAmount": 1000.00,
  "version": 1,
  "requestId": "唯一请求号"
}
```

内部接口拆分：

- 成员二的 `ArrearsConfirmationApplicationPort` 只负责读取待确认申请和欠费明细。
- 成员三的 `ApprovalCompletionService` 负责校验 `CONFIRM_PENDING`、`version` 和 `requestId`，并流转为 `COMPLETED`。
- 成员四负责外层事务：读取申请、写 `arrears_confirmation`、调用完成状态；任一步失败全部回滚。
- 成员四不得直接写 `application`、`arrears_application` 或学生表。

### 8. 学校代申请和线下补录

- 成员四发起学校代申请和线下补录。
- 成员二通过 `ApplicationCreationService` 写申请主表及详情。
- `SCHOOL_PROXY` 进入普通三级审核。
- `SUPPLEMENT` 由成员三补写自动审核记录；包含欠费时进入 `CONFIRM_PENDING`，无欠费时进入 `COMPLETED`。
- 成员四负责外层事务，成员二和成员三的 Service 加入同一事务。

### 9. 资源和补助额度

- 学生正式提交时预占礼包库存、礼包名额和补助期望金额。
- 辅导员填写最终补助金额时，由成员二原子调整预占差额。
- 学院再次校验，学校通过后确认占用。
- 拒绝或取消立即释放；退回按批次配置暂时保留或延时释放。
- 所有资源变化按 `applicationId + operationType + requestId` 保证幂等。

### 10. 第一阶段状态终点

- 包含欠费的申请学校审核通过后进入 `CONFIRM_PENDING`，确认后进入 `COMPLETED`。
- 普通无欠费申请学校审核通过后进入 `APPROVED`，第一阶段将其作为审核终态。
- 无欠费线下补录可以直接进入 `COMPLETED`。
- 礼包领取和补助线下发放暂不强制执行 `APPROVED -> COMPLETED`，后续增加履约功能时再补充。

### 11. 事务发起方

| 业务场景 | 外层事务发起方 |
|---|---|
| 首次提交、退回重提 | 成员二 |
| 审核、退回、拒绝、取消、批量上报 | 成员三 |
| 学校欠费确认 | 成员四 |
| 学校代申请、线下补录 | 成员四 |

所有跨模块 Service 使用同一数据源并加入外层事务，任一步失败时申请、资源、审核记录和确认记录整体回滚。

### 12. 后续细节处理原则

申请编号格式、附件具体大小、页面提示文案、统计索引和定时释放实现不再阻塞第一阶段开发。对应负责人在实现前按现有协作规范记录决定；只要不改变上述表所有权、接口方向、状态规则和事务边界，无需重新发起四人总体验收。
