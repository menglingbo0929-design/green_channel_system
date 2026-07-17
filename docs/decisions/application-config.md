# 成员二申请配置模块接口与协作决策

> 状态：`APPROVED`
> 负责人：成员二
> 适用分支：`feature/application-config`
> 说明：本文整理成员二完成申请配置与学生申请模块所依赖的跨模块能力、需要对外提供的 Service，以及本模块计划提供的 REST 接口。四人第一阶段共识已写入 `docs/requirement.md`；实现以该共识和本文接口方向为依据。

## 1. 成员二职责和边界

成员二负责以下表的 DDL、Entity、Mapper 和直接写入：

```text
fee_item
fee_amount_option
gift_item
batch_gift_item
college_gift_quota
grade_gift_quota
application
arrears_application
gift_application
gift_application_item
application_attachment
application_operation_record
college_subsidy_quota
grade_subsidy_quota
subsidy_application
student_recommendation
```

成员二不得直接写入成员一、三、四负责的表。跨模块写入统一调用表负责人提供的 Service。

`application` 是唯一流程主表：

- 成员二维护物理结构、Entity、Mapper 和条件更新 Service；
- 成员三负责状态机、审核权限和审核事务编排；
- 成员四通过成员二的只读 Service 获取待确认申请，通过成员三 Service 完成状态流转；
- 共享字段增加、删除、改名或改类型，必须由成员二、三、四共同确认。

## 2. 成员二需要成员一提供的能力

### 2.1 当前登录用户

```java
public interface CurrentUserProvider {
    LoginUser getRequiredUser();
}
```

成员二需要的最小数据：

```text
userId
role
studentId
collegeId
```

申请接口不得接受前端传入的 `userId`、`studentId`、`collegeId` 或角色作为权限依据。

### 2.2 学生申请资格快照

```java
public interface StudentProfileQueryService {
    StudentApplicationProfile getRequiredProfile(Long studentId);
}
```

最小返回字段：

```text
studentId
studentNo
studentName
collegeId
majorId
gradeId
classId
profileCompleted
originStudentLoan
campusStudentLoanPlanned
aidRecognitionStatus
```

该接口用于入口资格判断和申请详情快照。成员二不直接建立 `student` Mapper。

### 2.3 数据范围校验

```java
public interface StudentScopeService {
    boolean isStudentSelf(Long userId, Long studentId);
    boolean isStudentInCollege(Long studentId, Long collegeId);
}
```

### 2.4 批次查询

```java
public interface BatchQueryService {
    BatchSnapshot getRequiredBatch(Long batchId);
    BatchSnapshot getCurrentOpenGreenChannelBatch();
    boolean isGradeEligible(Long batchId, Long gradeId);
}
```

成员二需要的最小批次字段：

```text
batchId
batchType
status
applicationStartTime
applicationEndTime
collegeSubmitDeadline
policyDescription
pageNotice
giftQuotaTotal
subsidyAmountTotal
returnReservationHours
```

### 2.5 基础组织查询

```java
public interface OrganizationQueryService {
    List<CollegeOption> listColleges();
    List<GradeOption> listGradesByCollege(Long collegeId);
}
```

用于学校分配学院名额、学院分配年级名额及前端下拉选择。成员二不复制学院、年级基础表 Mapper。

### 2.6 政策规则查询

```java
public interface PolicyRuleQueryService {
    List<PolicyRuleSnapshot> listEnabledRules(Long batchId);
}
```

成员二根据成员一维护的政策规则生成推荐入口和 `student_recommendation`，不直接修改 `policy_rule`。

## 3. 成员二需要成员三提供的能力

### 3.1 申请提交与状态流转

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

原因：首次提交和退回重提既要改变 `application`，又要向成员三负责的 `approval_record` 写入 `SUBMIT` 记录。成员二负责保存申请内容和资源预占，成员三负责状态机及审核记录编排。

`completeAfterConfirmation` 不属于成员二调用范围，由成员四调用成员三提供的 `ApprovalCompletionService`。

`resubmitReturned` 必须使用新的 `requestId`，增加 `reviewRound`，更新本轮 `submitTime`、操作人和更新时间，新增一条 `SUBMIT` 审核记录，并保留全部历史审核记录。

### 3.2 审核状态与记录查询

```java
public interface ApprovalFlowQueryService {
    ApprovalFlowSnapshot getFlow(Long applicationId);
}
```

学生申请详情页需要展示当前状态、退回原因和审核时间轴，但成员二不直接查询 `approval_record`。成员三在实现内部通过可信登录上下文获取用户、角色、学院和数据范围。

## 4. 成员二提供给成员三的能力

以下接口沿用 `docs/member-code-contracts.md` 的提案，成员二确认后负责实现。

### 4.1 申请状态快照

```java
public interface ApplicationStateQueryService {
    ApplicationStateSnapshot getRequiredState(Long applicationId);
}
```

最小快照：

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

### 4.2 申请状态条件写入

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

更新 SQL 必须同时校验：

```text
id
expectedStatus
expectedVersion
deleted = 0
```

受影响行数不是 1 时返回明确的状态冲突或版本冲突。

批量上报不新增第二套批量写接口。成员三在同一个外层事务中循环调用单条 `updateState`；任意一条发生状态或版本冲突时抛出异常，使整批状态更新、审核记录和上报记录全部回滚。

### 4.3 审核详情

```java
public interface ApplicationDetailService {
    ApplicationApprovalDetail getApprovalDetail(Long applicationId);
    boolean containsArrears(Long applicationId);
}
```

详情需要包含学生快照、申请主表、欠费详情、礼包详情、补助详情和附件元数据。

### 4.4 资源生命周期

```java
public interface ApplicationResourceService {
    void reserveOnSubmit(Long applicationId, String requestId, Long operatorId);
    ResourceAdjustmentResult applyCounselorSubsidyAmount(
        Long applicationId,
        BigDecimal finalSubsidyAmount,
        String requestId,
        Long operatorId
    );
    void validateCollegeApproval(Long applicationId);
    void confirmOnSchoolApproval(Long applicationId, String requestId, Long operatorId);
    void handleReturn(Long applicationId, Integer reservationHours, String requestId, Long operatorId);
    void releaseOnReject(Long applicationId, String requestId, Long operatorId);
    void releaseOnCancel(Long applicationId, String requestId, Long operatorId);
}
```

所有库存、名额和额度更新必须使用事务、条件原子更新、行锁或乐观锁，禁止先查询余额再无条件更新。

资源操作幂等记录由成员二维护在不可变的 `application_operation_record` 中，按照 `application_id + operation_type + request_id` 建立唯一约束。成员三的 `approval_record.request_id` 只保证审核操作幂等，不能代替资源操作幂等。

学生提交补助申请时按期望金额预占额度；辅导员确认最终金额时，`applyCounselorSubsidyAmount` 持久化最终金额并原子调整预占差额。额度不足时审核失败，学院再次校验，学校审核通过后确认占用，拒绝或取消时释放。

### 4.5 跨模块事务边界

| 业务场景 | 外层事务发起方 | 同一事务内完成的操作 |
|---|---|---|
| 首次提交、退回重提 | 成员二 | 保存申请、预占或调整资源、调用成员三写 `SUBMIT` 记录并推进状态 |
| 审核、退回、拒绝、取消、批量上报 | 成员三 | 权限校验、调用成员二更新状态和资源、写审核/上报记录 |
| 学校欠费确认完成 | 成员四 | 写确认记录、调用成员三完成状态流转和审核记录 |
| 线下补录自动审核 | 成员四 | 调用成员二创建申请、调用成员三写自动审核记录并推进状态 |

所有跨模块 Service 使用同一数据源并加入外层事务，默认 `Propagation.REQUIRED`。任意一步失败，申请、资源、审核记录和确认记录整体回滚。

## 5. 成员二提供给成员四的能力

### 5.1 欠费确认申请读取

```java
public interface ArrearsConfirmationApplicationPort {
    PageResult<PendingArrearsApplication> pagePending(
        PendingArrearsQuery query
    );

    ArrearsConfirmationDetail getConfirmationDetail(
        Long applicationId
    );
}
```

最小返回字段：

```text
applicationId
applicationNo
version
studentId
studentNo
studentName
collegeName
majorName
gradeName
className
declaredArrearsAmount
arrearsItems
status
```

只允许返回有效且状态为 `CONFIRM_PENDING` 的申请。成员四不直接查询 `application`、`arrears_application` 或 `student`。

成员四写入欠费确认记录后，通过成员三提供的接口完成状态和审核记录：

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

### 5.2 学校代申请与线下补录

```java
public interface ApplicationCreationService {
    ApplicationDraftResult createSchoolProxyApplication(
        SchoolProxyApplicationCommand command
    );

    ApplicationResult createSupplementApplication(
        SupplementApplicationCommand command
    );
}
```

规则：

- `SCHOOL_PROXY` 创建后进入普通三级审核；
- `SUPPLEMENT` 根据是否包含欠费进入 `CONFIRM_PENDING` 或 `COMPLETED`；
- 防止同一学生、同一批次、同一申请类型重复创建；
- 成员四负责补录业务编排，成员二负责申请主表及明细的物理写入；
- 自动审核记录和最终状态由成员三提供的状态流转 Service 处理。

成员三提供以下补录自动审核能力，由成员四在补录外层事务中调用：

```java
public interface ApprovalTransitionService {
    ApplicationStatusResult completeSupplementReview(
        Long applicationId,
        boolean containsArrears,
        Integer expectedVersion,
        String requestId,
        Long operatorId
    );
}
```

### 5.3 统计只读能力

```java
public interface ApplicationStatisticsQueryService {
    ApplicationStatisticsSnapshot queryStatistics(
        ApplicationStatisticsQuery query
    );
}
```

第一阶段允许使用成员二提供的面向集合查询 Service，或成员二、四确认后的只读聚合 SQL；具体方式由两名负责人在实现时选择。禁止逐条跨模块调用后再汇总。

## 6. 成员二 REST 接口提案

以下接口均以 `/api` 为基础路径，统一返回格式、时间格式、分页结构和身份规则遵守 `docs/api-document.md`。

### 6.1 绿色通道入口

```http
GET /api/green-channel/eligibility
GET /api/green-channel/current-policy
```

资格结果必须返回 `allowed` 和明确原因码：

```text
NO_OPEN_BATCH
OUT_OF_APPLICATION_TIME
GRADE_NOT_ELIGIBLE
PROFILE_INCOMPLETE
LOAN_CONDITION_NOT_MET
APPLICATION_ALREADY_EXISTS
```

### 6.2 学生申请

```http
POST   /api/applications/drafts
GET    /api/applications/mine
GET    /api/applications/{applicationId}
PUT    /api/applications/{applicationId}
DELETE /api/applications/{applicationId}
POST   /api/applications/{applicationId}/submit
```

规则：

- 学生身份从 JWT 获取；
- 只有 `DRAFT` 和允许编辑的退回状态可以修改内容；
- 只有未提交草稿可以删除；
- 写接口携带 `version` 和唯一 `requestId`；
- 正式提交必须在同一业务事务中完成申请校验、资源预占和状态流转。

### 6.3 欠费申请明细

```http
GET /api/applications/{applicationId}/arrears
PUT /api/applications/{applicationId}/arrears
```

后端校验金额大于 0、合计不超过 8000 元，并使用 `BigDecimal`。

### 6.4 礼包申请明细

```http
GET /api/batches/{batchId}/gift-items
GET /api/applications/{applicationId}/gift
PUT /api/applications/{applicationId}/gift
```

后端校验必选物品、单人数量上限、库存、学院名额和年级名额。

### 6.5 补助申请明细

```http
GET /api/applications/{applicationId}/subsidy
PUT /api/applications/{applicationId}/subsidy
```

支持 `LIVING_SUBSIDY`、`TRAVEL_SUBSIDY`，学生填写期望金额，最终金额由审核流程确定。

### 6.6 申请附件

```http
POST   /api/applications/{applicationId}/attachments
GET    /api/applications/{applicationId}/attachments
GET    /api/attachments/{attachmentId}/download
DELETE /api/attachments/{attachmentId}
```

附件下载必须鉴权。上传必须校验扩展名、MIME、文件头和大小，使用随机存储标识，不暴露可猜测静态路径。

### 6.7 欠费项目与金额档位

```http
GET    /api/fee-items
POST   /api/fee-items
PUT    /api/fee-items/{feeItemId}
DELETE /api/fee-items/{feeItemId}

GET    /api/fee-amount-options
POST   /api/fee-amount-options
PUT    /api/fee-amount-options/{optionId}
DELETE /api/fee-amount-options/{optionId}
```

### 6.8 礼包物品与批次配置

```http
GET    /api/gift-items
POST   /api/gift-items
PUT    /api/gift-items/{giftItemId}
DELETE /api/gift-items/{giftItemId}

GET  /api/batches/{batchId}/gift-config
PUT  /api/batches/{batchId}/gift-config
POST /api/batches/{batchId}/gift-config/copy-previous
```

复制上一批次配置时必须创建新批次关联记录，不能修改历史批次数据。

### 6.9 礼包名额

```http
GET /api/batches/{batchId}/gift-quotas/colleges
PUT /api/batches/{batchId}/gift-quotas/colleges/{collegeId}
GET /api/batches/{batchId}/gift-quotas/grades
PUT /api/batches/{batchId}/gift-quotas/grades/{gradeId}
GET /api/batches/{batchId}/gift-quotas/usage
```

### 6.10 补助额度

```http
GET /api/subsidy-batches/{batchId}/quotas/colleges
PUT /api/subsidy-batches/{batchId}/quotas/colleges/{collegeId}
GET /api/subsidy-batches/{batchId}/quotas/grades
PUT /api/subsidy-batches/{batchId}/quotas/grades/{gradeId}
GET /api/subsidy-batches/{batchId}/quotas/usage
```

### 6.11 推荐入口

```http
GET /api/student/recommendations
POST /api/student/recommendations/{recommendationId}/read
```

成员二负责学生端展示和推荐结果，政策规则本身由成员一维护。

## 7. 成员二错误码提案

```text
APPLICATION_NOT_FOUND
APPLICATION_FORBIDDEN
APPLICATION_INVALID_STATUS
APPLICATION_ALREADY_EXISTS
APPLICATION_VERSION_CONFLICT
APPLICATION_ALREADY_SUBMITTED
APPLICATION_OUT_OF_TIME
APPLICATION_GRADE_NOT_ELIGIBLE
APPLICATION_PROFILE_INCOMPLETE
APPLICATION_LOAN_CONDITION_NOT_MET
APPLICATION_ARREARS_AMOUNT_INVALID
APPLICATION_ARREARS_AMOUNT_EXCEEDED
APPLICATION_GIFT_REQUIRED_MISSING
APPLICATION_GIFT_LIMIT_EXCEEDED
APPLICATION_GIFT_STOCK_INSUFFICIENT
APPLICATION_COLLEGE_QUOTA_INSUFFICIENT
APPLICATION_GRADE_QUOTA_INSUFFICIENT
APPLICATION_SUBSIDY_QUOTA_INSUFFICIENT
APPLICATION_ATTACHMENT_TYPE_INVALID
APPLICATION_ATTACHMENT_SIZE_EXCEEDED
APPLICATION_REQUEST_ALREADY_PROCESSED
```

## 8. 第一阶段已经确认的实现约定

### 8.1 `application.batch_id` 的父表

成员二采纳“两列可空外键加类型约束”方案：

```text
batch_type
green_channel_batch_id
subsidy_batch_id
```

使用 CHECK 约束保证两个批次外键有且仅有一个非空，并与 `batch_type` 一致。对外快照统一返回 `batchType + batchId`。成员一已确认该方案；成员三的 `approval_submission_record`、批量上报及成员四统计契约按相同规则实现。

### 8.2 首次提交事务负责人

首次提交和退回重提由成员二发起外层事务，调用成员三 `ApprovalTransitionService` 写 `SUBMIT` 记录和推进状态；接口加入现有事务，不开启独立事务。

### 8.3 补助额度预占时点

正式提交时按学生期望金额预占；辅导员确认最终金额时持久化金额并原子调整差额。额度不足时不允许审核通过。

### 8.4 幂等记录位置

成员二使用不可变的 `application_operation_record` 保存申请与资源操作幂等结果；唯一约束为 `application_id + operation_type + request_id`。创建申请时尚无 `application_id`，同时对 `request_id` 建立业务级唯一约束并在创建成功后回填申请 ID。

### 8.5 `APPROVED -> COMPLETED` 负责人

第一阶段采用以下规则：普通无欠费申请校级审核通过后进入 `APPROVED` 并作为审核终态；包含欠费的申请进入 `CONFIRM_PENDING`，学校确认后进入 `COMPLETED`；无欠费补录可直接进入 `COMPLETED`。礼包领取和补助发放暂不强制执行 `APPROVED -> COMPLETED`。

### 8.6 其他公共约定

以下细节由对应负责人在实现前补充，不再阻塞第一阶段开发：

```text
application_no 生成格式
附件允许类型、大小和存储目录
退回申请资源保留时长及超时释放任务负责人
统计模块跨表查询方式
后端基础包名 com.example.backend / com.greenchannel.backend
```

## 9. 实施流程

1. 四人第一阶段共识和本决策状态均为 `APPROVED`；
2. 成员二把 REST 接口同步到 `docs/api-document.md`，把最终表结构同步到 `docs/database-design.md`；
3. 各负责人分别实现本人拥有的 Service、DDL、Entity、Mapper 和测试；
4. 负责人在实现对应功能前补充不影响公共边界的内部细节；
5. 完成联调后将变更记录更新为 `IMPLEMENTED`。
