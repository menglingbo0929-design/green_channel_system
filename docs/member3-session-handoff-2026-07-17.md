# 成员三会话工作交接记录

> 用途：供下一次新会话快速恢复上下文。本文记录截至 2026-07-17 的职责边界、关键决策、已完成工作、当前 Git 状态、未完成事项和建议执行顺序。

## 1. 项目与成员身份

- 项目：高校绿色通道系统
- 当前身份：成员三
- 负责模块：三级审核、批量上报、状态机、审核消息
- 工作分支：`feature/approval-flow`
- 成员三直接维护的表：
  - `approval_record`
  - `approval_submission_record`
  - `system_message`
  - `message_read_record`
- 成员三不得直接修改成员二维护的 `application` DDL、Entity、Mapper 或写 SQL。
- `application.status`、`application.current_level` 等状态变化由成员三负责业务判断，但物理更新必须调用成员二提供的 Service。

## 2. 协作规范与权限边界

开展跨模块工作前，按以下顺序读取权威文档：

1. `docs/collaboration-rules.md`
2. `docs/change-log.md`
3. `docs/database-design.md`
4. `docs/api-document.md`
5. `docs/status-flow.md`
6. 对应的 `docs/decisions/<模块>.md`
7. 代码和 SQL

执行原则：

- 只直接修改成员三负责的代码、表和决策内容。
- 涉及其他成员表、接口或公共状态时，只提出意见并等待负责人修改。
- 共享变更按 `PROPOSED -> APPROVED -> IMPLEMENTED` 流程推进。
- Pull Request 已合并不代表提案已经获得业务确认，仍应以决策文档和 `docs/change-log.md` 的状态为准。
- 不为方便开发而复制其他成员的 Mapper，也不新建第二张申请主表。

## 3. 已完成工作

### 3.1 成员三审核模块公共设计

已在提交 `1ef8a38` 中完成审核流程与协作规范相关文档，随后通过 PR #2 合并到 `main`。主要文件包括：

- `docs/api-document.md`
- `docs/change-log.md`
- `docs/collaboration-rules.md`
- `docs/database-design.md`
- `docs/database-format-standard.md`
- `docs/decisions/approval-flow.md`
- `docs/member-code-contracts.md`
- `docs/status-flow.md`

完成的主要设计包括：

- 三级审核状态机；
- 逐条审核与首次批量上报分离；
- 退回后补交和重新审核轮次；
- `review_round`、`version`、`requestId` 的并发与幂等约定；
- 成员三与成员二、四之间的 Service 边界；
- 成员三拥有表的数据库设计建议；
- 数据库格式与 migration 候选规范。

### 3.2 数据库格式规范审核与完善

已审核并完善 `docs/database-format-standard.md`，目标是统一：

- MySQL 与 Java 类型；
- 公共审计字段；
- 逻辑删除和乐观锁；
- 主外键、索引和命名；
- 基线 SQL 与 migration 职责；
- 各成员表结构所有权。

当前该规范在 `docs/change-log.md` 中仍是 `PROPOSED`，需要总负责人和其他成员确认后才能作为正式实现依据，不能据此直接修改其他成员表。

### 3.3 成员二 `application-config` 初次评审

已阅读成员二 `feature/application-config` 分支的 `docs/decisions/application-config.md`，并以成员三身份完成附条件评审。

评审文档：

```text
docs/application-config-member3-review.md
```

对应提交：

```text
8dc667c Create application-config-member3-review.md
```

该提交已推送到：

```text
origin/feature/approval-flow
```

评审文档只存在于成员三分支，尚未合并进 `main`。

### 3.4 成员二已吸收评审意见

成员二在提交 `1281061` 中吸收了成员三的评审意见，主要完成：

- 统一为 `ApprovalTransitionService`；
- 将学生确认完成独立为 `ApprovalCompletionService`；
- 将审核流查询改为 `getFlow(Long applicationId)`，由成员三内部读取可信登录上下文；
- 确认批量上报在成员三外层事务中循环调用单条条件更新，任一失败整批回滚；
- 补充 `applyCounselorSubsidyAmount`；
- 补充资源操作 `requestId` 和 `application_operation_record` 幂等表；
- 补充 `completeSupplementReview`；
- 明确主要跨模块事务发起方；
- 明确第一阶段 `APPROVED` 与 `COMPLETED` 的边界；
- 将数据库脚本更名为规范要求的 `database/02_create_tables.sql`。

该提交已通过 PR #6 合并到 `main`：

```text
65a5dc8 Merge pull request #6 from .../feature/application-config
```

## 4. 当前关键决策

### 4.1 审核流基本规则

- 首次提交：`DRAFT -> COUNSELOR_PENDING`。
- 退回重提：任一 `RETURNED` 状态重新进入 `COUNSELOR_PENDING`，`review_round + 1`。
- 辅导员和学院逐条执行 `APPROVE` 时，只记录本层审核结论，不立即推进到下一层。
- 首次批量上报时，统一推进已通过申请到下一审核层。
- 退回重提后的申请使用补交通道重新流转，不受“首次批量上报只能一次”的限制。
- 状态更新必须携带当前 `version`；所有写操作必须携带唯一 `requestId`。

### 4.2 校级审核后的状态

- 包含欠费的普通申请：校级通过后进入 `CONFIRM_PENDING`。
- 学生欠费确认完成后：`CONFIRM_PENDING -> COMPLETED`。
- 不包含欠费的普通申请及生活、路费补助：校级通过后进入 `APPROVED`，第一阶段将其视为审核终态。
- 无欠费的线下补录申请：可以自动进入 `COMPLETED`。
- 礼包领取、补助发放暂不强制执行 `APPROVED -> COMPLETED`，除非后续另行明确履约模块。

### 4.3 已约定的成员三对外接口

```java
public interface ApprovalTransitionService {
    ApplicationStatusResult submitInitial(
        Long applicationId,
        Integer expectedVersion,
        String requestId,
        Long operatorId
    );

    ApplicationStatusResult resubmitReturned(
        Long applicationId,
        Integer expectedVersion,
        String requestId,
        Long operatorId
    );

    ApplicationStatusResult completeSupplementReview(
        Long applicationId,
        boolean containsArrears,
        Integer expectedVersion,
        String requestId,
        Long operatorId
    );
}
```

```java
public interface ApprovalCompletionService {
    ApplicationStatusResult completeAfterConfirmation(
        Long applicationId,
        Integer expectedVersion,
        String requestId,
        Long operatorId
    );
}
```

```java
public interface ApprovalFlowQueryService {
    ApprovalFlowSnapshot getFlow(Long applicationId);
}
```

查询接口不接受前端提供的 `currentUserId`，成员三应从可信登录上下文中获取用户、角色、学院和数据范围。

### 4.4 成员二向成员三提供的接口

成员三依赖以下接口，不直接访问成员二的 Mapper：

- `ApplicationStateQueryService#getRequiredState`
- `ApplicationStateWriteService#updateState`
- `ApplicationStateWriteService#incrementReviewRoundAndUpdateState`
- `ApplicationDetailService#getApprovalDetail`
- `ApplicationDetailService#containsArrears`
- `ApplicationResourceService#reserveOnSubmit`
- `ApplicationResourceService#applyCounselorSubsidyAmount`
- `ApplicationResourceService#validateCollegeApproval`
- `ApplicationResourceService#confirmOnSchoolApproval`
- `ApplicationResourceService#handleReturn`
- `ApplicationResourceService#releaseOnReject`
- `ApplicationResourceService#releaseOnCancel`

`ApplicationStateSnapshot` 的最小字段已经调整为：

```text
applicationId
studentId
batchType
batchId
applicationType
status
currentLevel
reviewRound
version
```

### 4.5 事务边界

| 场景 | 外层事务发起方 |
| --- | --- |
| 首次提交、退回重提 | 成员二 |
| 审核、退回、拒绝、取消、批量上报 | 成员三 |
| 欠费确认完成 | 成员四 |
| 线下补录自动审核 | 成员四 |

所有跨模块 Service 使用同一数据源并加入外层事务，默认 `Propagation.REQUIRED`。申请、资源、审核记录和确认记录必须整体成功或整体回滚。

### 4.6 资源与幂等规则

- 正式提交时按学生期望补助金额预占额度。
- 辅导员确定最终金额时，持久化最终金额并原子调整预占差额。
- 额度不足时不得审核通过。
- 学院再次校验，校级通过后确认占用。
- 拒绝或取消时释放；退回时按统一保留时长处理。
- 成员二使用不可变的 `application_operation_record` 管理资源操作幂等。
- 唯一约束为 `application_id + operation_type + request_id`。
- `approval_record.request_id` 只保证成员三审核操作幂等，不能代替资源幂等记录。

### 4.7 批次关联方案

成员二已采纳“两列可空外键加类型约束”方案：

```text
batch_type
green_channel_batch_id
subsidy_batch_id
```

通过 CHECK 约束保证两个批次外键有且仅有一个非空，并与 `batch_type` 一致。对外快照统一为 `batchType + batchId`。

成员三初步同意该方案，但它仍需成员一和成员四确认，并且需要同步成员三拥有的 `approval_submission_record`、批量上报和统计契约。

## 5. 当前 Git 状态

记录本文前的状态：

```text
当前分支：feature/approval-flow
当前提交：8dc667c
远程跟踪：origin/feature/approval-flow
工作区：干净
相对 origin/main：领先 1 个提交，落后 7 个提交
origin/main：65a5dc8
```

分支差异中的成员三独有提交：

```text
8dc667c Create application-config-member3-review.md
```

注意：创建本文后，工作区会新增本交接文件，需要另行提交和推送。

## 6. 尚未完成的事项

### 6.1 立即需要处理

- [ ] 以成员三身份复核成员二提交 `1281061` 后的最新版 `docs/decisions/application-config.md`。
- [ ] 对最新版逐项核对初次评审清单，给出“确认”或“仍需修改”的正式结论。
- [ ] 如果确认通过，通知成员二在得到其余成员确认后，将对应 `docs/change-log.md` 记录从 `PROPOSED` 更新为 `APPROVED`。
- [ ] 确认批次双外键方案对 `approval_submission_record` 的最终字段影响。
- [ ] 将成员三分支同步到最新 `origin/main`。

### 6.2 仍需全员或其他成员确认

- [ ] 成员一、成员四确认批次双外键方案。
- [ ] 成员四确认欠费确认、代申请、补录和统计接口。
- [ ] 统一 `application_no` 生成格式。
- [ ] 统一附件类型、大小和存储目录。
- [ ] 明确退回资源保留时长及超时释放任务负责人。
- [ ] 明确统计模块采用 Service、只读 Mapper 还是数据库视图。
- [ ] 统一后端基础包名：`com.example.backend` 或 `com.greenchannel.backend`。
- [ ] 总负责人和四名成员确认数据库格式与 migration 规范。

### 6.3 成员三后续实现工作

公共契约正式 `APPROVED` 后，成员三再进入代码实现，至少包括：

- [ ] 成员三拥有表的最终 DDL/migration、Entity、Mapper。
- [ ] 审核状态机和权限校验。
- [ ] `ApprovalTransitionService`。
- [ ] `ApprovalCompletionService`。
- [ ] `ApprovalFlowQueryService`。
- [ ] 辅导员、学院、学校审核接口。
- [ ] 首次批量上报与退回补交通道。
- [ ] 取消、退回、驳回时的状态与资源编排。
- [ ] 审核消息生成、列表和已读接口。
- [ ] 乐观锁、重复请求、批量回滚、越权访问和状态非法测试。
- [ ] 同步更新 `docs/api-document.md`、`docs/database-design.md`、`docs/status-flow.md` 和 `docs/change-log.md`。

## 7. 建议的下一步执行顺序

1. 提交并推送本交接文档，保持成员三工作可追溯。
2. 在 GitHub Desktop 中 Fetch origin。
3. 保持在 `feature/approval-flow`，执行 Update from `main`，把最新 `main` 合入成员三分支。
4. 检查并解决文档冲突，不覆盖成员三的 `docs/application-config-member3-review.md`。
5. 阅读合入后的最新文件：
   - `docs/collaboration-rules.md`
   - `docs/change-log.md`
   - `docs/database-design.md`
   - `docs/status-flow.md`
   - `docs/decisions/application-config.md`
   - `docs/decisions/approval-flow.md`
6. 对成员二最新版 `application-config.md` 做最终复核。
7. 只将成员三的确认结论写入成员三评审文档或发送给成员二，不直接替成员二修改其决策文件。
8. 等相关成员全部确认、状态更新为 `APPROVED` 后，再实现成员三代码。

## 8. 下次新会话建议开场指令

可以在新会话中直接发送：

> 我是高校绿色通道系统的成员三。请先阅读 `docs/member3-session-handoff-2026-07-17.md`，再检查当前分支和远程 `main` 状态。按照协作规范，只处理成员三有权限的内容。首先复核成员二在提交 `1281061` 中修订的 `docs/decisions/application-config.md`，逐项对照 `docs/application-config-member3-review.md`，给出是否可以由成员三正式确认的结论；不要直接修改成员二的文件。

## 9. 关键文件索引

| 文件 | 用途 |
| --- | --- |
| `docs/collaboration-rules.md` | 最高优先级协作和权限规范 |
| `docs/change-log.md` | 共享变更状态和待确认事项 |
| `docs/database-format-standard.md` | 数据库格式候选规范 |
| `docs/database-design.md` | 表、字段、约束和所有权 |
| `docs/status-flow.md` | 申请状态与合法流转 |
| `docs/member-code-contracts.md` | 四名成员代码层接口约定 |
| `docs/decisions/approval-flow.md` | 成员三审核模块决策 |
| `docs/decisions/application-config.md` | 成员二申请模块决策 |
| `docs/application-config-member3-review.md` | 成员三对成员二提案的初次评审意见 |
| `docs/member3-session-handoff-2026-07-17.md` | 本次会话交接文档 |

