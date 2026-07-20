# 成员三跨模块代码协作约定

本文档只记录成员三实现三级审核所需的跨模块依赖和成员三对外提供的 Service，不代替其他成员决定其 DDL、Entity、Mapper、目录或内部实现。

执行前必须先阅读：

1. [collaboration-rules.md](./collaboration-rules.md)
2. [change-log.md](./change-log.md)
3. [database-design.md](./database-design.md)
4. [api-document.md](./api-document.md)
5. [status-flow.md](./status-flow.md)
6. [decisions/approval-flow.md](./decisions/approval-flow.md)

以下第一阶段跨模块 Service 方向已经 `APPROVED`。实现必须保持表所有权、接口方向、状态规则和事务边界；其他成员的真实实现尚未合入时，不得以临时 Mapper、假数据或硬编码身份替代。

## 1. 成员三权限边界

成员三拥有并可以直接写入：

```text
approval_record
approval_submission_record
system_message
message_read_record
```

成员三负责：

- 审核状态机判断；
- 辅导员、学院、学校审核权限；
- 逐条审核、批量上报和退回补交；
- 审核记录、上报记录和审核消息；
- 审核流程查询和审核工作台接口；
- 跨模块事务编排。

成员三不得：

- 建立或修改 `application` DDL、Entity、Mapper；
- 直接写用户、学生、学院、批次、库存、名额和补助额度表；
- 直接写 `arrears_confirmation`；
- 修改其他成员的 Mapper；
- 替其他成员提交其负责表的 migration。

## 2. 成员一需要提供的只读能力

### 2.1 当前登录用户

成员三需要获得：

```java
public record LoginUser(
    Long userId,
    UserRole role,
    Long studentId,
    Long collegeId
) {}
```

只读接口提案：

```java
public interface CurrentUserProvider {
    LoginUser getRequiredUser();
}
```

审核接口不接受前端传入的 `userId`、`role`、`counselorId` 或 `collegeId` 作为权限依据。

### 2.2 数据范围

只读接口提案：

```java
public interface StudentScopeService {

    boolean isStudentSelf(
        Long userId,
        Long studentId
    );

    boolean isCounselorResponsibleFor(
        Long counselorUserId,
        Long studentId
    );

    boolean isStudentInCollege(
        Long studentId,
        Long collegeId
    );
}
```

成员一决定关联表、Mapper 和查询实现，成员三只调用接口。

### 2.3 批次快照

成员三审核上报需要以下只读数据：

```java
public record BatchSnapshot(
    BatchType batchType,
    Long batchId,
    BatchStatus status,
    LocalDateTime applicationEndTime,
    LocalDateTime collegeSubmitDeadline,
    Integer returnReservationHours
) {}
```

只读接口提案：

```java
public interface BatchQueryService {
    BatchSnapshot getRequiredBatch(BatchType batchType, Long batchId);
}
```

成员三不定义批次表字段和 Mapper。

## 3. 成员二需要提供的申请能力

### 3.1 申请状态快照

只读接口提案：

```java
public interface ApplicationStateQueryService {
    ApplicationStateSnapshot getRequiredState(Long applicationId);
}
```

成员三需要的快照字段：

```java
public record ApplicationStateSnapshot(
    Long applicationId,
    Long studentId,
    BatchType batchType,
    Long batchId,
    ApplicationType applicationType,
    ApplicationStatus status,
    ApprovalLevel currentLevel,
    Integer reviewRound,
    Integer version
) {}
```

这只是审核模块的数据需求，不是成员三对 `application` 物理字段的修改。具体 DDL、Entity 和 Mapper 由成员二决定并在公共文档中确认。

### 3.2 申请状态写入

成员三计算合法目标状态后，通过成员二 Service 执行物理写入：

```java
public interface ApplicationStateWriteService {

    ApplicationStateSnapshot updateState(
        Long applicationId,
        ApplicationStatus expectedStatus,
        ApplicationStatus targetStatus,
        ApprovalLevel targetLevel,
        Integer expectedVersion,
        Long operatorId
    );

    ApplicationStateSnapshot incrementReviewRoundAndUpdateState(
        Long applicationId,
        ApplicationStatus expectedStatus,
        ApplicationStatus targetStatus,
        ApprovalLevel targetLevel,
        Integer expectedVersion,
        Long operatorId
    );
}
```

接口由成员二实现并独占 `application` 写 Mapper。成员三不得创建第二个 `application` Mapper。

### 3.3 审核详情

只读接口提案：

```java
public interface ApplicationDetailService {

    ApplicationApprovalDetail getApprovalDetail(
        Long applicationId
    );

    boolean containsArrears(
        Long applicationId
    );
}
```

详情需要覆盖学生快照、欠费明细、礼包明细、补助明细和附件。具体 DTO 字段由成员二维护，成员三不建立申请明细 Mapper。

### 3.4 资源操作

Service 提案：

```java
public interface ApplicationResourceService {

    void reserveOnSubmit(
        Long applicationId,
        String requestId,
        Long operatorId
    );

    ResourceAdjustmentResult applyCounselorSubsidyAmount(
        Long applicationId,
        BigDecimal finalSubsidyAmount,
        String requestId,
        Long operatorId
    );

    void validateCollegeApproval(
        Long applicationId
    );

    void confirmOnSchoolApproval(
        Long applicationId,
        String requestId,
        Long operatorId
    );

    void handleReturn(
        Long applicationId,
        Integer reservationHours,
        String requestId,
        Long operatorId
    );

    void releaseOnReject(
        Long applicationId,
        String requestId,
        Long operatorId
    );

    void releaseOnCancel(
        Long applicationId,
        String requestId,
        Long operatorId
    );
}
```

库存、名额和额度的事务、锁和 Mapper 均由成员二负责。

## 4. 成员四需要提供的确认能力

### 4.1 单据检查与作废

Service 提案：

```java
public interface ArrearsDocumentService {

    boolean hasIrreversibleOfflineProcessing(
        Long applicationId
    );

    void voidDocumentForCancellation(
        Long applicationId,
        String reason,
        Long operatorId
    );
}
```

成员三取消申请时调用该接口，不直接修改欠费确认或单据数据。

### 4.2 欠费确认完成

成员四完成金额确认和单据生成后，调用成员三提供的状态流转 Service。成员三校验 `CONFIRM_PENDING` 后，再调用成员二的 `ApplicationStateWriteService` 转换为 `COMPLETED`。

## 5. 成员三对外提供的 Service

成员三向其他模块提供以下已批准的跨模块 Service：

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

public interface ApprovalCompletionService {
    ApplicationStatusResult completeAfterConfirmation(
        Long applicationId,
        Integer expectedVersion,
        String requestId,
        Long operatorId
    );
}

public interface ApprovalFlowQueryService {
    ApprovalFlowSnapshot getFlow(Long applicationId);
}
```

其中状态流转 Service 负责编排、校验和成员三审核记录，不直接写 `application`。审核 Controller、取消编排、工作台查询和消息 Service 属于成员三模块内部能力，不额外改变跨模块契约。

消息 Service 提案：

```java
public interface SystemMessageService {

    void sendApprovalReturned(
        Long receiverUserId,
        Long applicationId,
        String reason
    );

    void sendApprovalRejected(
        Long receiverUserId,
        Long applicationId,
        String reason
    );

    void sendApprovalApproved(
        Long receiverUserId,
        Long applicationId,
        String notice
    );

    void markAsRead(
        Long messageId,
        Long userId
    );
}
```

## 6. 实施流程

第一阶段契约已经批准，按以下顺序实施：

1. 各表负责人分别实现自己拥有的 DDL、Entity、Mapper 和 Service；
2. 成员三可并行实现纯状态机、本人持久层、审核与消息接口，以及使用 Mock 数据的审核页面；
3. 成员一、成员二的真实依赖合入后完成权限、状态写入、资源和详情联调；
4. 成员四通过成员三提供的 Service 联调欠费确认、学校代申请和补录；
5. 完成真实数据联调和测试后，将对应变更记录更新为 `IMPLEMENTED`。
