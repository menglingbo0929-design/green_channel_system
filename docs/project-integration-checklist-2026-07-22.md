# 项目集成与剩余工作清单（2026-07-22）

适用仓库：`green_channel_system`  
检查分支：`feature/approval-flow`  
代码基线：`44ef852`（成员三可信身份、数据范围与错误状态码修复）

本文供成员一、二、四和总负责人查漏补缺。任务按模块所有权划分，跨模块实现继续通过既定 Port/Service 完成，不直接修改其他成员拥有的持久层。

## 1. 成员三已完成

### 1.1 审批可信身份适配

文件：

- `backend/src/main/java/com/example/backend/approval/integration/ApprovalCurrentUserProviderAdapter.java`

作用：

- 将成员一 `security.ICurrentUserProvider` 返回的可信 JWT 身份适配为成员三 `approval.port.CurrentUserProvider`；
- 规范化 `ROLE_` 前缀；
- 拒绝缺少 `userId`、学生缺少 `studentId`、学院缺少 `collegeId` 的不完整身份；
- 在审批契约仍只能表达一个活动角色时，拒绝含多个审批角色的歧义身份，避免静默提升权限。

### 1.2 审批学生数据范围适配

文件：

- `backend/src/main/java/com/example/backend/approval/integration/ApprovalStudentScopeServiceAdapter.java`

作用：将成员一已存在的 `service.StudentScopeService` 桥接到成员三 `approval.port.StudentScopeService`，不直连成员一 Mapper。

### 1.3 审批错误 HTTP 状态码

文件：

- `backend/src/main/java/com/example/backend/approval/web/ApprovalExceptionHandler.java`

当前映射：

| 类型 | HTTP 状态 |
|---|---:|
| 数据范围无权访问 | 403 |
| 状态、版本、重复处理、批量上报、额度等业务冲突 | 409 |
| 资源回滚失败 | 500 |
| 缺少动作、意见等请求校验 | 400 |
| 跨成员 Bean 未接入 | 503（原行为保留） |

### 1.4 验证结果

- 新增 7 个针对性测试，全部通过；
- 后端全量：94 tests，0 failures，0 errors，0 skipped；
- 前端：`npm run build` 通过；
- Spring Boot 测试上下文正常启动，无新增 Bean 冲突；
- `git diff --check` 通过。

## 2. 成员一待办

### P0-1：消息收件人解析

需要实现并注册：

```java
com.example.backend.approval.port.ApprovalMessageRecipientResolver
```

要求：`getStudentUserId(studentId)` 必须通过成员一拥有的学生/用户能力返回有效登录用户 ID。不要让成员三直接读取 `student.user_id`。

影响：缺少该 Bean 时，学校取消 Service 不会创建，审核结果消息也无法完整发送。

验收：

- 学生存在且已关联账号时返回用户 ID；
- 无账号、已删除或停用用户必须明确失败；
- 取消成功只产生一条 `APPROVAL_CANCELLED` 消息。

### P0-2：审批批次查询

需要实现并注册：

```java
com.example.backend.approval.port.ApprovalBatchQueryService
```

现有 `BatchQueryServiceImpl` 只读取 `GreenChannelBatchMapper`，只能支持 `GREEN_CHANNEL`，不能直接冒充完整审批批次实现。正式实现必须同时支持：

- `GREEN_CHANNEL`；
- `SUBSIDY`；
- `batchType + batchId` 匹配校验；
- `open`；
- `applicationDeadline`；
- `collegeDeadline`。

影响：辅导员、学院首次批量上报目前仍会返回 503。

### P1-3：多角色用户的活动角色选择

成员一设计允许一个用户拥有多个角色，但成员三当前 `LoginUser` 契约只能表达一个活动角色。当前适配器选择“安全拒绝多角色”，避免静默选取高权限角色。

需要总负责人、成员一和成员三共同确定以下一种方案：

1. 登录后选择活动角色，并把该角色写入受签名 JWT；或
2. 审批 `LoginUser` 改为角色集合，每个接口按目标动作校验 `hasRole`；或
3. 请求携带活动角色，但后端必须验证该角色确实存在于 JWT 角色集合中。

在方案确认前，多角色用户访问审批接口会收到 403。

## 3. 成员二待办

### P0-1：审核工作台查询 Port

实现并注册：

```java
com.example.backend.approval.port.ApprovalApplicationQueryPort
```

必须实现 `pagePending`、`pageByApplicationIds`、`getRequiredApprovalDetail`、`getDashboard`、`listScopedApplicationIds`。查询必须在数据库层应用辅导员、学院和学校范围，不能先全量查询再由前端过滤。

影响接口：

- `GET /api/approvals/pending`
- `GET /api/approvals/processed`
- `GET /api/approvals/{applicationId}`
- `GET /api/approvals/dashboard`

### P0-2：批量上报申请查询 Port

实现并注册：

```java
com.example.backend.approval.port.ApprovalSubmissionApplicationQueryService
```

`listByBatch(batchType, batchId)` 只返回 `deleted = 0` 的申请，并携带完整状态、层级、轮次和版本快照。

### P0-3：补录自动审核幂等号

当前文件：

```text
backend/src/main/java/com/example/backend/application/service/SupplementApplicationService.java
```

当前自动审核使用：

```java
"SUPPLEMENT_COMPLETE_" + state.applicationId()
```

应按既定事务契约复用创建命令原始 `command.getRequestId()`，让补录创建和自动审核共享同一幂等边界。此文件属于成员二 application Service，成员三不越界修改。

验收：相同创建 `requestId` 并发调用时只创建一份申请，并且只产生一条自动审核记录。

### P0-4：申请接口可信身份

以下接口仍相信 `X-Student-Id` / `X-User-Id`：

- `/api/applications/mine`
- 学生草稿、明细、附件和正式提交接口
- `GreenChannelEligibilityController`

应注入成员一 `ICurrentUserProvider`，用 JWT 中的 `userId/studentId` 进行归属校验；开发身份头不能保留为生产权限依据。

### P1-5：真实跨模块事务验收

需要与成员三共同验证：

- 附件、资源预占、`submitInitial` 任一步失败整体回滚；
- 审核拒绝或学校取消时资源释放幂等；
- 退回时资源保留策略与最终释放策略一致；
- 版本更新使用带旧状态和旧版本的原子条件。

## 4. 成员四待办

### P0-1：欠费确认后端主链路

当前仓库只有确认 Entity/Mapper、DTO/VO 和成员三完成回调适配，没有完整的确认 Controller 与业务编排 Service。

需要完成：

- 待确认列表、详情、保存确认；
- 写 `arrears_confirmation`；
- 在同一外层事务调用 `ArrearsConfirmationCompletionPort.completeAfterConfirmation`；
- 保存或状态推进任一失败时整体回滚。

### P0-2：欠费单据接口

当前只有：

```java
com.example.backend.service.port.ArrearsVoucherAccessPort
```

尚缺实际实现及列表、详情、学生本人查看、打印 Controller/Service。前端 `voucher.js` 对应路由目前没有完整后端支撑。

### P0-3：统计和报表 Controller/Service

当前已有统计 DTO/VO、查询适配器和前端页面，但缺少完整后端 HTTP 编排。需要实现：

- 统计汇总；
- 历史批次；
- 报表分页；
- Excel 导出；
- 打印数据；
- 学校角色和数据范围校验。

不得使用前端传入的 `X-User-Id` 作为最终权限依据。

### P0-4：取消单据语义

成员四已有 `ArrearsDocumentServiceImpl`，仍需真实 MySQL 验证：

- 不可逆线下处理后禁止取消；
- 单据只逻辑作废，不物理删除；
- 重复取消不重复作废；
- 作废失败触发整笔学校取消事务回滚。

## 5. 总负责人/共同待办

### P0：启动真实 MySQL 联调

检查时本机 `3306` 没有监听。本轮仅验证了 H2 自动化测试，未验证真实 MySQL 方言、约束、migration 和种子数据。

建议固定联调顺序：

1. 新库执行 `01_create_database.sql`、`02_create_tables.sql`、`03_init_data.sql`、`04_test_data.sql`；
2. 使用 JDK 21 启动后端；
3. 使用四种角色 JWT 验证权限；
4. 按完整三级流程走一条绿色通道申请；
5. 再验证补助批次、学校代申请、补录、欠费确认、取消和报表。

### P0：端到端验收矩阵

至少覆盖：

- 401 未认证、403 越权、503 依赖未注入；
- 版本冲突；
- 重复及并发 `requestId`；
- 退回原因；
- 批量失败回滚；
- 跨模块外层事务回滚；
- 消息已读及未读数刷新；
- 辅导员、学院、学校之间的数据范围隔离。

### P1：文档同步

以下文档与当前代码存在差异：

- `frontend/README.md` 仍写默认启用 Mock，实际代码默认真实 API；
- `docs/member3-session-handoff-2026-07-20.md` 的 Git 和未提交状态已是历史快照；
- `docs/member2-frontend-interaction.md` 对礼包、补助和正式提交的描述已过时；
- 根 `README.md` 的后端包路径写成 `com/greenchannel/backend`，实际是 `com/example/backend`。

### P2：仓库与构建整理

- 本地 `feature/confirmation-statistics` 有唯一提交 `46c48ee`，只修改 `backend/pom.xml`，且分支明显落后；确认是否废弃后再清理；
- Vite 报告 `Home.vue` 同时静态和动态导入；
- 两个主要前端 chunk 超过 1 MB；
- Mockito 动态加载 agent 在未来 JDK 中会被禁止。

## 6. 当前仍会出现 503 的成员三接口

| 功能 | 仍缺能力 |
|---|---|
| 审核工作台列表、详情、dashboard | 成员二 `ApprovalApplicationQueryPort` |
| 辅导员/学院首次批量上报 | 成员一 `ApprovalBatchQueryService`、成员二 `ApprovalSubmissionApplicationQueryService` |
| 学校取消 | 成员一 `ApprovalMessageRecipientResolver`（其他资源和单据 Service 已存在） |

成员三适配后，可信当前用户和学生范围本身不再是上述 503 的原因。

## 7. 合并提醒

- 成员三身份、范围与错误状态码修复已提交并推送：`44ef852`；
- 不要直接修改成员三审批表或成员二 application 持久层来绕过 Port；
- 其他成员完成 Bean 后，应先运行 `mvnw test`，再进行 MySQL 联调；
- 跨模块写入必须由外层业务事务发起，禁止通过捕获异常后返回“部分成功”。
