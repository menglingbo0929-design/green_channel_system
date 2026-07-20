# 成员二申请配置模块进度

更新日期：2026-07-20

当前成员二可独立实施范围完成度：约 **50%**。申请主表、草稿、欠费明细和成员四确认/单据读取 Port 已具备；资源、附件、正式提交和统计仍受跨模块依赖或未批准字段限制。

## 已完成（可提交）

- 已新增成员二拥有的 16 张表 migration：申请主表、申请明细、附件、资源/名额/额度、幂等操作记录和推荐记录；其中 `application` 采用已批准的双批次列方案，并以生成列 `batch_id` 保证“同学生、同批次、同类型仅一条有效申请”。
- 已实现 `application` 的 MyBatis Mapper、领域枚举和 `ApplicationStateQueryService`、`ApplicationStateWriteService`、`ApplicationDetailService`、`ApplicationCreationService`。
- 已实现草稿创建、查询、更新、删除；状态条件更新和退回重提轮次更新均同时校验 `id + status + version + deleted=0`。
- 已实现草稿创建的 `requestId` 幂等记录；重放相同请求返回原申请状态。
- 已提供学生申请 REST 骨架：创建草稿、我的申请、详情、编辑、删除。
- 已实现绿色通道欠费明细的查询和整表替换接口；校验草稿/退回状态、乐观锁、项目去重、单项正数及总额不超过 8000 元。
- 已提供成员四所需的 `ArrearsConfirmationApplicationPort` 与 `ArrearsVoucherApplicantQueryPort`：确认待办分页、确认详情及最多 100 条申请的欠费项目批量快照查询。
- 已运行 `backend/mvnw.cmd test`，编译与 Spring 上下文测试通过。

## 进行中 / 依赖解除后完成

| 项目 | 当前状态 | 外部依赖 |
|---|---|---|
| 首次提交、退回重提的完整事务 | 待接入 | 成员一登录/学生/批次服务，成员三 `ApprovalTransitionService` |
| 礼包、补助详情 REST 与资源原子扣减 | 待实现 | 成员一批次、学院、年级基础数据和最终资源配置 |
| 欠费确认与欠费单据联调 | 成员二申请/欠费批量快照已实现 | 成员一 `StudentOrganizationSnapshotQuery`、权限/归属查询；成员四接入与联调 |
| 代申请、补录、统计 | 代申请主表创建入口已实现；其余待实现 | 代申请明细/附件、成员三提交/补录状态服务；统计的成员一组织/批次数据及已批准的欠费原因字段 |
| 文件上传与下载 | 待实现 | 允许类型、大小、对象存储目录尚未决定 |
| 管理端费用/礼包/名额/额度接口 | 待实现 | 管理员鉴权与基础组织数据服务 |

## 风险与约束

- 当前数据库通用格式规范仍为 `PROPOSED`；本 migration 遵循其推荐命名（小写下划线、`xxx_id`、`create_time`）但未把它表述为全项目已批准规范。
- `ApplicationController` 的 `X-Student-Id`、`X-User-Id` 仅是开发期占位。接入成员一的 `CurrentUserProvider` 后必须移除，不能信任客户端传入身份。
- 共享表 `application` 的完整提交/审核联调必须由成员二、三、四共同验证后，才能将整体记录改为 `IMPLEMENTED`。
