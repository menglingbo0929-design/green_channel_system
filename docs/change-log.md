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
- 对应提交：待提交
