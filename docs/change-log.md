
# 共享结构与接口变更记录

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
