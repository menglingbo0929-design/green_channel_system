
# 共享结构与接口变更记录

## 2026-07-19｜成员四统计功能与统计筛选接口契约

- 状态：PROPOSED
- 提出人：成员四
- 负责人：成员四（学校端统计编排）、成员一（当前用户与学校权限）、成员二（集合统计查询、申请/组织/批次/欠费/礼包数据与欠费原因字段）
- 影响模块：成员一、成员二、成员四
- 影响表：只读 `application`、学生组织、批次、`arrears_application`、`fee_item`、礼包表和 `arrears_confirmation`；提议成员二新增 `arrears_application.arrears_reason_code`
- 影响接口：`GET /api/statistics/applications/summary`、成员二 `ApplicationStatisticsQueryService`
- 影响状态：只读 `APPROVED`、`COMPLETED`；不改变申请状态
- 变更内容：固定统计口径、人数/金额计算方式、八个筛选参数、返回字段、排序、历史批次规则和欠费原因编码。
- 当前阻塞项：成员一学校统计权限、成员二统计聚合 Service、申请全量真实数据和欠费原因编码字段均未合入；成员四不得用确认表或假数据补算。
- 使用者需要执行的操作：成员一、成员二确认并实现第 16 节契约；成员四在依赖合入后完成真实联调并更新本记录状态。
- 对应提交：待提交

## 2026-07-18｜成员四欠费单据查询、学生查看与打印接口

- 状态：PROPOSED
- 提出人：成员四
- 负责人：成员四（单据查询/打印）、成员一（当前用户和学生归属校验）、成员二（申请与欠费项目批量快照）
- 影响模块：成员一、成员二、成员四
- 影响表：arrears_confirmation；只读 application、学生组织和欠费项目数据
- 影响接口：学校单据列表/详情/打印、学生本人单据查询及两个跨模块只读 Port
- 影响状态：不改变 CONFIRM_PENDING、COMPLETED 或申请状态
- 变更内容：固定路径、权限、单据字段、批量查询上限、打印时间和 JWT 前临时身份规则。
- 当前实现范围：成员四已实现确认记录读取、分页、路由、单据组装与打印触发；不含任何模拟数据、临时表或跨模块直连。
- 当前实现范围：成员二已在 `application.service.ArrearsConfirmationApplicationService` 实现 `ArrearsVoucherApplicantQueryPort.findVoucherApplicantsByApplicationIds`，一次最多读取 100 个申请的真实申请、欠费项目与金额，并通过成员一的 `StudentOrganizationSnapshotQuery` 批量补齐学生组织快照。
- 当前阻塞项：成员一尚未合入 `StudentOrganizationSnapshotQuery` 和 `ArrearsVoucherAccessPort` 的学校权限、学生归属和确认人姓名实现。因此完整单据字段、权限校验、学生本人查看和成功响应联调目前均不可完成或验证。
- 解除条件：成员一提供上述 Port 实现和最小测试数据；成员四接入成员二 Port 后完成单据联调，不新增临时数据方案。
- 使用者需要执行的操作：成员一实现并合入上述两个 Port；成员四接入 `ArrearsVoucherApplicantQueryPort` 并联调；成员二配合排查真实数据问题。
- 对应提交：本地待提交

## 2026-07-17｜成员四学校代申请接口契约

- 状态：PROPOSED
- 提出人：成员四
- 负责人：成员四（学校端编排）、成员一（学生查询）、成员二（申请/附件/提交）、成员三（提交状态与审核记录）
- 影响模块：成员一、成员二、成员三、成员四
- 影响表：application 及明细/附件表、approval_record；成员四不直接写入
- 影响接口：学校代申请学生查询、创建草稿、上传附件、正式提交
- 影响状态：SCHOOL_PROXY 草稿为 DRAFT；正式提交后进入 COUNSELOR_PENDING
- 变更内容：固定 6.1.3 的路径、请求字段、附件上传方式、requestId、事务发起方和跨模块写入边界。
- 使用者需要执行的操作：成员一、二、三按 confirmation-statistics.md 的 Port 契约提供实现；完成联调后更新状态。
- 对应提交：待提交

## 2026-07-17｜成员二申请配置模块跨模块接口提案

- 状态：APPROVED
- 提出人：成员二
- 负责人：成员二（申请、资源和附件接口）、成员一（已确认身份、学生、批次和双外键方案）、成员三（审核状态、事务和记录接口意见已吸收）、成员四（已确认欠费确认、代申请、补录和统计边界）
- 影响模块：全部模块
- 影响表：`application`、`application_operation_record`、申请详情表、礼包/库存/名额/补助额度表、`approval_record`、`arrears_confirmation` 及基础数据表
- 影响接口：学生申请、申请配置、跨模块申请状态写入、资源生命周期、欠费确认读取、代申请、补录和统计查询
- 影响状态：第一阶段状态与办结边界已经确认，使用现有状态枚举
- 变更内容：新增并修订 `docs/decisions/application-config.md`，整理成员二依赖成员一/三的能力、成员二向成员三/四提供的 Service、成员二 REST 接口、错误码及开发前阻塞项；已吸收成员三提交 `8dc667c` 的评审意见，统一状态接口、事务边界、资源幂等、补助额度调整、批量更新、补录自动审核、批次关联和办结规则。
- 使用者需要执行的操作：四名成员按照 `docs/requirement.md` 的第一阶段共识实现本人模块；不改变表所有权、接口方向、状态规则和事务边界的实现细节由模块负责人自行决定。
- 对应提交：待提交

## 2026-07-17｜新增成员四欠费最终确认记录表

- 状态：IMPLEMENTED
- 提出人：成员四
- 负责人：成员四
- 影响模块：成员二申请模块、成员三审核状态模块、成员四确认模块
- 影响表：新增 `arrears_confirmation`；只读取 `application`，不修改其结构
- 影响接口：成员二需提供待确认欠费申请读取能力；成员三需提供 `CONFIRM_PENDING -> COMPLETED` 状态流转能力
- 影响状态：使用既有 `CONFIRM_PENDING`、`COMPLETED`，不新增状态值
- 变更内容：新增确认金额快照、实际确认金额、确认人、确认时间、单据编号与逻辑删除字段；同一申请的有效确认记录唯一。
- 使用者需要执行的操作：成员二、三合入正式 Service 后，提供 `ArrearsConfirmationApplicationPort` 的实现并同步公共接口文档；成员四执行 `database/migrations/V20260717_001__create_arrears_confirmation.sql`。

# 共享结构变更记录

最新记录必须放在最上方。共享表、公共接口、公共状态或公共配置的变化，只有完成相关成员确认并按规范同步代码和文档后，才能从 `PROPOSED` 更新为 `IMPLEMENTED`。

## 2026-07-17｜数据库格式与迁移规范候选最终稿

- 状态：PROPOSED
- 提出人：成员三
- 负责人：仓库负责人（规范生效）、各表负责人（本人表实现）
- 影响模块：全部模块
- 影响表：全部业务表
- 影响接口：无直接接口变化；后续表结构变化可能影响 DTO/VO
- 影响状态：不修改现有状态枚举
- 变更内容：统一数据库字段命名、Java/MySQL 类型、公共审计字段、逻辑删除、乐观锁、申请关系、表所有权、索引、外键、SQL 基线和 migration 规则；明确历史 migration 不变但 `02_create_tables.sql` 持续更新。
- 使用者需要执行的操作：总负责人确认规范在文档权威顺序中的位置；四名成员确认类型、时间字段、外键和 migration 命名；确认前不据此修改他人表结构。
- 对应提交：待提交

## 2026-07-17｜审核模块公共契约与申请审核字段

- 状态：PROPOSED
- 提出人：成员三
- 负责人：成员二（`application` 结构）、成员三（审核状态与审核接口）
- 影响模块：申请配置、三级审核、欠费确认与统计
- 影响表：`application`、`approval_record`、`approval_submission_record`、`system_message`、`message_read_record`
- 影响接口：申请首次提交、退回重提、三级审核、批量上报、取消、欠费确认完成、审核消息查询与已读
- 影响状态：全部申请状态、审核层级和审核动作
- 变更内容：提议为 `application` 统一采用 `status`、`current_level`、`review_round`、`version` 等审核字段；逐条通过只记录结论，首次批量上报时推进到下一节点；退回申请通过补交通道重新流转；所有跨表写入通过表负责人 Service 完成。
- 使用者需要执行的操作：成员二、成员三、成员四确认字段、枚举、Service 边界和最终状态映射；确认前不得修改 `application` DDL、Entity 或 Mapper。

- 对应提交：待提交
