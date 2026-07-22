# 成员三｜批量上报实现与跨成员依赖

更新日期：2026-07-21  
负责人：成员三（审批编排与上报记录）

## 已完成的成员三范围

- `GET /api/approval-submissions/status`：按当前登录角色和后端计算的数据范围返回上报状态；
- `POST /api/approval-submissions/counselor/initial`：辅导员首次批量上报；
- `POST /api/approval-submissions/college/initial`：学院首次批量上报；
- `POST /api/approval-submissions/return-resubmit`：退回申请在当前审核轮次重新审核通过后的逐条补交；
- 复用 `approval_submission_record` 的请求号幂等和首次上报唯一约束；申请状态写入始终通过成员二的 `ApplicationStateWriteService`。
- 每条被推进的申请均写入一条 `SUBMIT` 审核记录；批量请求会稳定派生每条记录的内部幂等号，以满足 `approval_record.request_id` 的全局唯一约束。

成员三不读取或写入 `application`、批次、资源表的 Mapper/SQL。

## 成员一需要提供的 Spring Bean

### 1. `ApprovalBatchQueryService`

位置：`com.example.backend.approval.port.ApprovalBatchQueryService`。

```java
ApprovalBatchSnapshot getRequiredBatch(BatchType batchType, Long batchId);
```

返回的 `ApprovalBatchSnapshot` 必须包含并保证与请求一致的：

- `batchType`、`batchId`；
- `open`：批次是否允许审核上报；
- `applicationDeadline`：辅导员首次上报仅可在此时间之后进行；
- `collegeDeadline`：学院首次上报仅可在此时间及之前进行。

现有仅面向绿色通道的 `BatchQueryService` 可以作为实现基础，但适配器必须同时支持 `GREEN_CHANNEL` 和 `SUBSIDY`，不能由成员三读取批次表补齐。

### 2. `StudentScopeService`

位置：`com.example.backend.approval.port.StudentScopeService`。

必须实现：

```java
boolean isCounselorResponsibleFor(Long counselorUserId, Long studentId);
boolean isStudentInCollege(Long studentId, Long collegeId);
```

批量范围只能由这两个后端判断得出；前端不得传递辅导员、学院或学生 ID 来指定上报范围。

## 成员二需要提供的 Spring Bean

### 1. `ApprovalSubmissionApplicationQueryService`

位置：`com.example.backend.approval.port.ApprovalSubmissionApplicationQueryService`。

```java
List<ApplicationStateSnapshot> listByBatch(BatchType batchType, Long batchId);
```

要求：只返回有效（`deleted = 0`）申请；每项必须携带 `applicationId`、`studentId`、`batchType`、`batchId`、`status`、`currentLevel`、`reviewRound` 和 `version`。成员三据此调用成员一范围服务、读取本人审核记录，并决定是否推进状态。

### 2. `ApplicationStateWriteService`

现有成员二状态写入 Port 必须以状态和版本为条件原子更新。成员三会使用：

- 辅导员：`COUNSELOR_PENDING -> COLLEGE_PENDING`；
- 学院：`COLLEGE_PENDING -> SCHOOL_PENDING`。

冲突时必须明确返回版本/状态冲突，不能无条件覆盖。

### 3. `ApprovalResourceService`

学院首次上报与学院退回补交前，成员三会逐项调用：

```java
void validateCollegeApproval(Long applicationId);
```

该方法负责礼包名额、学院补助额度等成员二资源规则；失败必须抛出可回滚异常。辅导员上报不调用资源表。

## 联调验收顺序

1. 成员一、二分别注册上述 Bean，并给出至少一条真实批次和申请数据；
2. 辅导员逐条审核通过后，验证首次上报只能发生一次、未审核申请会阻止上报；
3. 学院验证截止时间和资源校验；
4. 学校退回、学生重提、当前层级重新审核通过后，验证 `RETURN_RESUBMIT`；
5. 并发重复调用同一 `requestId`，确认只生成一条 `approval_submission_record` 且不重复推进申请状态。

## 当前可预期的 503

在成员一或成员二尚未注册上述 Bean 时，接口会明确返回 `APPROVAL_INTEGRATION_UNAVAILABLE`，不会降级为前端传参、直接 SQL 或模拟成功。
