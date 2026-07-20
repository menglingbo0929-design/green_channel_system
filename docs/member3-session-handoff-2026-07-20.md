# 成员三新任务窗口交接记录（2026-07-20）

## 1. 当前身份与权限

- 项目：高校绿色通道系统（`green_channel_system`）。
- 身份：成员三。
- 分支：`feature/approval-flow`。
- 职责：三级审核、批量上报、状态机、审核记录与消息、学生审核进度及各角色审核页面。
- 成员三拥有：`approval_record`、`approval_submission_record`、`system_message`、`message_read_record`。
- `application` 的 DDL、Entity、Mapper 和直接 SQL 归成员二；成员三只能调用成员二提供的 Service。

权威文档顺序：

1. `docs/collaboration-rules.md`
2. `docs/change-log.md`
3. `docs/database-design.md`
4. `docs/api-document.md`
5. `docs/status-flow.md`
6. `docs/decisions/approval-flow.md`
7. 代码和 SQL

四人第一阶段共识、申请配置决策和成员三审核契约现已 `APPROVED`。

## 2. 当前 Git 与工作区提醒

- 记录时 HEAD：`966319d`。
- 当前分支相对 `origin/feature/approval-flow` 领先 1 个提交。
- 工作区包含大量尚未提交的前后端、数据库和文档修改。
- 新窗口开始后必须先运行 `git status`；禁止重置、清理或覆盖现有并行修改。

## 3. 已完成的文档对齐

- `status-flow.md`、`api-document.md`、`change-log.md`、`approval-flow.md`、`member-code-contracts.md` 和成员三评审结论已对齐为 `APPROVED`。
- 正式跨模块 Service：
  - `ApprovalTransitionService`
  - `ApprovalCompletionService`
  - `ApprovalFlowQueryService`
- 批量上报统一使用 `batchType + batchId`。
- `approval_submission_record` 使用 `batch_type + green_channel_batch_id/subsidy_batch_id`。

## 4. 当前后端未提交实现

仓库中已经存在以下成员三实现，可能包含并行任务成果，继续前必须先审查：

- `backend/src/main/java/com/example/backend/approval/domain/`：状态枚举、状态机、Transition 和异常。
- `backend/src/main/java/com/example/backend/approval/persistence/`：四张表的 Entity、Mapper 和类型枚举。
- `backend/src/test/java/com/example/backend/approval/`：状态机和持久层测试。
- `database/migrations/V20260720_002__create_approval_and_message_tables.sql`。
- `database/02_create_tables.sql`、`backend/pom.xml` 和测试配置也存在未提交修改。

## 5. 已完成的审核前端

路由：

- `/counselor/approvals`
- `/college/approvals`
- `/school/approvals`

主要文件：

- `frontend/src/views/approval/ApprovalWorkbench.vue`
- `frontend/src/components/approval/ApprovalDetailDrawer.vue`
- `frontend/src/components/approval/ReviewDialog.vue`
- `frontend/src/components/approval/StatusBadge.vue`
- `frontend/src/api/approval.js`
- `frontend/src/mock/approval.js`
- `frontend/src/router/index.js`

已经支持：审核概览、筛选、待审/已审列表、申请详情、流程时间线、通过/退回/驳回、辅导员最终金额、辅导员/学院批量上报以及学校取消申请。

默认使用状态化 Mock；设置 `VITE_USE_MOCK=false` 后切换真实后端。`npm run build` 已通过，仅有 Element Plus 包体积提示，不阻塞运行。

## 6. 后续建议顺序

1. 审查并完成当前状态机和持久层并行改动。
2. 实现三个跨模块 Service、审核 Controller、工作台查询和消息 Service。
3. 对接成员一可信身份/数据范围，以及成员二申请状态、资源和详情 Service。
4. 补充消息中心和学生审核进度页面。
5. 完成权限、幂等、乐观锁、批量回滚和跨模块联调测试。
6. 分批审查、提交并推送当前脏工作区。

## 7. 新窗口建议开场

> 我是高校绿色通道系统成员三。请先读取 `docs/member3-session-handoff-2026-07-20.md`，再检查当前 Git 状态。保留所有现有并行修改，按权威文档和成员三权限边界继续完成审核模块。
