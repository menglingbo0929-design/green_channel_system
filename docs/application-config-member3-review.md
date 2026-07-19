# 成员三对 `application-config.md` 的审核确认意见

- 审核人：成员三（审核与上报模块）
- 接收人：成员二（申请、礼包与补助额度模块）
- 审核日期：2026-07-17
- 审核对象：`feature/application-config` 分支下的 `docs/decisions/application-config.md`
- 审核版本：`17ff19c26a85d4cdce7235d1b54df5bd80e1a6d5`
- 当前结论：**附条件确认，暂时保持 `PROPOSED`，不建议更新为 `APPROVED`**

## 一、成员三已确认的内容

成员三确认以下职责边界及接口方向：

1. 成员二负责 `application` 主表、各申请详情表、礼包配置表和补助额度表的 DDL、Entity、Mapper 及物理写入。
2. 成员三不创建 `application` Mapper，也不直接执行该表的写 SQL；申请状态由成员三判断，通过成员二提供的状态写入 Service 落库。
3. 成员三负责审核状态机、审核权限、审核事务编排及以下表的维护：
   - `approval_record`
   - `approval_submission_record`
   - `system_message`
   - `message_read_record`
4. 成员三确认需要成员二提供以下服务：
   - `ApplicationStateQueryService`
   - `ApplicationStateWriteService`
   - `ApplicationDetailService`
   - `ApplicationResourceService`
5. 确认 `ApplicationStateSnapshot` 应包含：
   - `applicationId`
   - `studentId`
   - `batchId`
   - `applicationType`
   - `status`
   - `currentLevel`
   - `reviewRound`
   - `version`
6. 确认所有申请状态更新都必须同时校验 `expectedStatus`、`expectedVersion` 和 `deleted = 0`；实际更新行数不为 1 时，必须明确返回状态冲突或并发冲突，不得静默成功。
7. 确认 `ApplicationDetailService` 需要提供审核详情和 `containsArrears` 查询能力。
8. 确认 `ApplicationResourceService` 需要覆盖提交预占、各级审核校验、校级确认、退回、驳回和取消等资源生命周期操作，并同意增加 `reserveOnSubmit`。

## 二、成员二需要修改或补充的内容

以下内容会直接影响成员三模块实现，请完成后再次通知成员三确认。

### 1. 统一审核状态流转接口名称

成员二提出的 `ApplicationTransitionService` 与成员三已经发布的审核流接口职责重复。审核状态机属于成员三，应由成员三维护接口定义，避免同时存在两套名称相近、职责重叠的接口。

建议最终约定为：

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
}
```

学生完成确认单独使用：

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

`completeAfterConfirmation` 的调用方是成员四，不应列在“成员二调用成员三”的接口中。

### 2. 修改审核流查询的身份传递方式

不建议采用：

```java
ApprovalFlowSnapshot getFlow(Long applicationId, Long currentUserId);
```

只传用户 ID 无法表达用户角色、学院和管辖范围，并且调用方可能传入不可信的用户 ID。

建议改为：

```java
ApprovalFlowSnapshot getFlow(Long applicationId);
```

成员三在实现内部通过统一登录上下文获取可信的用户 ID、角色、学院及数据范围。若内部调用必须传递身份，应传递后端构造的可信 `LoginUser`，不能使用前端直接提交的身份信息。

### 3. 明确跨模块事务边界

建议最终事务发起方如下：

| 业务场景 | 外层事务发起方 | 必须处于同一事务的操作 |
| --- | --- | --- |
| 首次提交、退回后重新提交 | 成员二 | 申请保存、资源预占或调整、状态更新、`SUBMIT` 审核记录 |
| 单条审核、退回、驳回、取消、批量上报 | 成员三 | 权限校验、状态更新、资源处理、审核记录、上报记录 |
| 学生确认完成 | 成员四 | 确认记录、状态更新、完成审核记录 |
| 补充申请自动审核 | 入口由成员二发起，状态处理由成员三负责 | 申请创建、自动审核记录、状态更新 |

跨模块服务应使用同一数据源并加入外层事务，例如使用 `Propagation.REQUIRED`。任意一步失败时，资源变化、申请状态和审核记录必须整体回滚。

### 4. 补充批量状态更新约定

成员三的批量上报需要保证全部成功或全部回滚。成员二应补充批量状态写入方法，或者明确允许成员三在一个外层事务中循环调用单条状态写入方法，并保证任意一条冲突都会抛出异常、触发整体回滚。

建议接口形式：

```java
List<ApplicationStateSnapshot> batchUpdateState(
        List<ApplicationStateChangeCommand> commands
);
```

每条命令都必须校验当前状态、当前审核层级、审核轮次、版本号和逻辑删除标志。

### 5. 明确辅导员最终补助金额的持久化与额度调整

当前 `validateCounselorApproval(applicationId, finalSubsidyAmount)` 只体现“校验”，没有明确最终金额是否落库以及预占额度如何调整。

成员三建议定案为：

1. 学生正式提交时，按申请金额预占额度。
2. 辅导员确定最终金额时，持久化最终金额并原子调整预占差额。
3. 如果需要增加预占但剩余额度不足，本次审核不得通过。
4. 学院审核时再次校验额度有效性。
5. 校级审核通过后确认占用。
6. 驳回或取消时释放额度。
7. 退回时根据统一配置决定暂时保留或延时释放。

建议将接口改成能够体现写入和调整语义的方法，例如：

```java
ResourceAdjustmentResult applyCounselorSubsidyAmount(
        Long applicationId,
        BigDecimal finalSubsidyAmount,
        String requestId,
        Long operatorId
);
```

### 6. 补充“补充申请自动审核”接口

文档提到补充申请会调用成员三状态服务，但尚未给出具体契约。建议增加：

```java
ApplicationStatusResult completeSupplementReview(
        Long applicationId,
        boolean containsArrears,
        Integer expectedVersion,
        String requestId,
        Long operatorId
);
```

该接口由成员三实现并负责：

- 生成校级自动审核记录；
- 有欠费时进入 `CONFIRM_PENDING`；
- 无欠费时进入 `COMPLETED`；
- 通过成员二的 `ApplicationStateWriteService` 完成物理状态更新。

### 7. 确认批次关联方案并同步相关契约

成员三同意优先采用“两列可空外键加类型约束”的方案：

- `batch_type`
- `green_channel_batch_id`
- `subsidy_batch_id`
- 使用 CHECK 约束保证两个批次外键有且仅有一个非空，并与 `batch_type` 一致

该方案保留真实外键，也不要求重构成员一已有的两类批次表。但正式确认时必须同步处理：

- `application` 表；
- 成员三维护的 `approval_submission_record` 表；
- `ApplicationStateSnapshot`；
- 批量上报查询和统计接口。

成员二提供给成员三的逻辑快照至少应增加 `batchType`，并根据物理外键返回归一化后的 `batchId`：

```java
private BatchType batchType;
private Long batchId;
```

该批次方案仍需所有受影响成员确认。在公共文档、决策文档和数据库设计同步完成前，不能视为正式生效。

### 8. 明确资源操作的幂等机制

当前资源接口没有明确携带 `requestId`，重试可能造成重复预占、重复确认或重复释放。

请明确：

1. 资源操作的幂等记录由成员二维护。
2. 所有会改变额度或礼包数量的方法必须关联 `requestId`。
3. 同一 `applicationId + operationType + requestId` 只能生效一次。
4. 成员三的 `approval_record.request_id` 只负责审核操作幂等，不能代替成员二的资源操作幂等记录。

### 9. 明确 `APPROVED` 与 `COMPLETED` 的使用范围

成员三建议第一阶段采用以下规则：

- 普通申请校级审核通过后进入 `APPROVED`，将其作为审核终态。
- 包含欠费的申请校级审核通过后进入 `CONFIRM_PENDING`。
- 学生完成欠费确认后进入 `COMPLETED`。
- 补充申请无欠费时可自动进入 `COMPLETED`。
- 礼包领取、补助发放等线下履约暂不强制触发 `APPROVED -> COMPLETED`，除非后续明确履约模块、责任人和接口。

### 10. 明确退回后重新提交的字段更新

`incrementReviewRoundAndUpdateState` 除增加 `review_round` 外，还应明确：

- 更新本轮 `submit_time`；
- 更新操作人和更新时间；
- 使用新的 `requestId`；
- 新增一条 `SUBMIT` 审核记录；
- 不覆盖任何历史审核记录。

## 三、仓库规范问题

成员二分支中的数据库脚本名称为：

```text
database/02_create_table.sql
```

协作规范规定的名称为：

```text
database/02_create_tables.sql
```

该文件不属于成员三维护范围，成员三不会直接修改，请成员二或仓库负责人在正式实现前统一文件名。

## 四、成员三最终确认条件

成员二完成以下事项后，请再次通知成员三复核：

- [ ] 统一审核状态流转接口名称和归属；
- [ ] 修正审核流查询的可信身份获取方式；
- [ ] 写明首次提交、审核、确认和自动审核的事务边界；
- [ ] 补充批量状态更新或整体回滚约定；
- [ ] 明确最终补助金额落库及预占差额调整；
- [ ] 增加补充申请自动审核接口；
- [ ] 确认批次关联方案并同步所有相关字段与接口；
- [ ] 明确资源操作幂等记录和 `requestId` 传递；
- [ ] 确认 `APPROVED` 与 `COMPLETED` 的规则；
- [ ] 明确重新提交时需要更新的字段；
- [ ] 修正数据库脚本文件名；
- [ ] 将确认后的约定同步至公共文档和 `docs/change-log.md`。

上述事项完成并经受影响成员确认后，成员三同意将对应决策状态由 `PROPOSED` 更新为 `APPROVED`。在此之前，成员三仅确认本文第一部分所列的职责边界和接口方向，不确认存在歧义或尚未定案的实现细节。
