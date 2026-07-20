# 审核与上报模块设计决策

> 状态：`APPROVED`
> 批准依据：`docs/requirement.md` 的“四人并行开发第一阶段共识”以及成员二已批准的 `docs/decisions/application-config.md`。
> 实施边界：成员三可以实现本人表、审核状态机、审核接口、审核页面和消息功能；不得直接实现或绕过其他成员拥有的 Mapper。

本文件由成员三维护。涉及共享主表、公共接口或公共状态的决定，还必须同步更新对应公共文档和 `docs/change-log.md`。

## 逐条审核与首次批量上报分离

- 日期：2026-07-17
- 负责人：成员三
- 涉及表：`application`、`approval_record`、`approval_submission_record`
- 涉及接口：辅导员审核、学院审核、辅导员首次上报、学院首次上报、退回补交
- 涉及文件：`docs/status-flow.md`、`docs/api-document.md`、`docs/database-design.md`
- 具体规定：辅导员和学院逐条执行 `APPROVE` 时只写审核记录，申请保持当前待审状态；执行首次批量上报时才统一推进到下一审核节点。学院或学校退回的申请在学生重提并重新审核后，通过 `RETURN_RESUBMIT` 单条补交。
- 做出原因：同时满足逐条审核、学生申请截止后一次性上报和退回后允许补交三个业务要求，避免首次上报限制阻断退回申请。
- 对其他模块的影响：成员二需要提供申请状态写入 Service；前端列表需要展示当前层级最新审核结论和批次上报状态；成员四只能按最终状态查询确认和统计数据。
- 是否需要其他成员确认：已确认，纳入四人第一阶段共识。

## 申请状态逻辑与物理写入分离

- 日期：2026-07-17
- 负责人：成员三
- 涉及表：`application`
- 涉及接口：全部申请状态转换接口
- 涉及文件：`docs/collaboration-rules.md`、`docs/status-flow.md`、`docs/database-design.md`、`docs/member-code-contracts.md`
- 具体规定：成员三负责判断合法状态转换、审核权限和事务编排，但不得创建 `application` Mapper 或直接执行写 SQL；实际状态更新由成员二提供的 `ApplicationStateWriteService` 完成。
- 做出原因：遵守每张表只允许表负责人模块直接写入的强制规范，同时保留成员三对审核状态机的业务所有权。
- 对其他模块的影响：成员二必须提供带旧状态和版本条件的状态写入 Service；成员四改变申请状态时调用成员三状态流转 Service，不能直接调用 Mapper。
- 是否需要其他成员确认：已确认，Service 方向和事务边界以批准后的申请配置决策为准。

## 审核轮次和并发控制

- 日期：2026-07-17
- 负责人：成员三
- 涉及表：`application`、`approval_record`、`approval_submission_record`
- 涉及接口：审核、重新提交、批量上报、退回补交、取消
- 涉及文件：`docs/status-flow.md`、`docs/database-design.md`、`docs/api-document.md`
- 具体规定：`application.review_round` 区分退回重提后的审核轮次，`application.version` 用于乐观锁；所有写接口携带唯一 `requestId` 和当前 `version`，审核记录使用 `request_id` 唯一约束保证幂等。
- 做出原因：防止旧审核结论被新一轮流程误用，并避免重复点击和并发审核覆盖状态。
- 对其他模块的影响：成员二需要在 `application` DDL、Entity 和状态写入 Service 中增加相应字段；成员四调用状态完成接口时需要传递版本和请求号。
- 是否需要其他成员确认：已确认，`application` 物理字段仍由成员二维护。

## 学校最终审核状态映射

- 日期：2026-07-17
- 负责人：成员三
- 涉及表：`application`、`approval_record`
- 涉及接口：学校最终审核、欠费确认完成、学校补录
- 涉及文件：`docs/status-flow.md`、`docs/api-document.md`
- 具体规定：包含欠费的普通绿色通道申请在学校通过后进入 `CONFIRM_PENDING`；不包含欠费的绿色通道申请以及生活、路费补助进入 `APPROVED`；成员四确认欠费后通过成员三 Service 转为 `COMPLETED`；学校线下补录按是否需要欠费确认直接进入 `CONFIRM_PENDING` 或 `COMPLETED`。
- 做出原因：区分学校审核通过和欠费实际金额确认两个业务节点，并兼容不需要欠费确认的业务。
- 对其他模块的影响：成员二提供 `containsArrears` 查询能力；成员四只查询 `CONFIRM_PENDING` 进行欠费确认，并统一统计状态口径。
- 是否需要其他成员确认：已确认，纳入第一阶段状态终点共识。

## 第一阶段代码落位约定

- 日期：2026-07-20
- 负责人：成员三
- 涉及文件：`backend/src/main/java/com/example/backend`、`frontend/src`
- 具体规定：
  - 后端沿用仓库现有基础包 `com.example.backend`，成员三代码分别放入审核与消息子包；成员三不单独发起全项目包名迁移。
  - 前端实现辅导员审核、学院审核、学校审核、学生审核进度和消息中心；共用审核详情、流程时间线和审核操作组件。
  - 前端可以按照已批准接口使用 Mock 数据并行开发；后端不得用 Mock、硬编码身份或临时 Mapper 冒充跨模块真实依赖。
  - 成员一或成员二依赖未合入时，依赖该能力的后端路由必须明确返回 `503 Service Unavailable`，不得返回缺字段的成功响应。
- 做出原因：固定成员三代码边界，使前后端能够并行启动，并避免与成员一公共布局、成员二学生申请页面和成员四确认统计页面重复开发。
- 对其他模块的影响：不改变公共接口、状态规则、事务边界或表所有权。
- 是否需要其他成员确认：否，属于成员三内部落位约定。
