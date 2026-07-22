# 成员三：学校取消申请联调依赖

## 已由成员三实现

- `POST /api/approvals/{applicationId}/cancel`：仅允许 `SCHOOL` 角色调用；请求体为 `reason`、`version`、`requestId`。
- `ApprovalCancellationService`：校验可取消状态、乐观锁、审核请求幂等、不可逆线下处理，并写入 `CANCEL` 审核记录和学生取消通知。
- `ArrearsDocumentService`：成员四实现的取消单据协作接口。
- 取消在一个 Spring 事务内调用申请状态、资源、单据、审核记录和消息能力；任一调用抛出异常时，成员三事务会回滚。

## 前端状态

学校取消的后端接口和真实 API 方法已经存在；学校审核工作台尚未增加取消入口、原因确认弹窗和成功后刷新流程。前端不得自行释放资源、作废单据或先行显示取消成功，必须以后端事务结果为准。

## 成员一依赖

1. 提供可注入的 `com.example.backend.approval.port.CurrentUserProvider`，从可信登录上下文返回当前用户；学校用户的 `role` 必须为 `SCHOOL`。
2. 提供可注入的 `ApprovalMessageRecipientResolver`，使 `getStudentUserId(studentId)` 返回有效学生登录用户 ID。

## 成员二依赖

1. 提供 `ApplicationStateQueryService` 和 `ApplicationStateWriteService` 的 Spring Bean。状态写入必须同时匹配 `applicationId`、旧状态和 `version`，成功后返回 `CANCELLED/FINISHED` 与新版本。
2. 提供 `ApprovalResourceService.releaseOnCancel(applicationId, requestId, operatorId)` 的 Spring Bean。它必须释放礼包库存、学院/年级名额和补助额度，并使用成员二的 `application_operation_record` 记录资源操作幂等。
3. 上述两个成员二 Service 必须参与调用方事务；资源释放失败必须抛出异常，不能返回“部分成功”。成员三不会直接修改 `application`、资源表或 `application_operation_record`。

## 成员四依赖

实现并注册以下 Spring Bean：

```java
public interface ArrearsDocumentService {
    boolean hasIrreversibleOfflineProcessing(Long applicationId);
    void voidDocumentForCancellation(Long applicationId, String reason, Long operatorId);
}
```

- `hasIrreversibleOfflineProcessing` 为 `true` 时，取消必须被拒绝且不产生任何状态或资源更新。
- `voidDocumentForCancellation` 只作废关联有效单据，不物理删除；不存在单据或已经作废时应安全幂等。
- 该实现必须参与调用方事务，作废失败时抛出异常以回滚整个取消操作。

## 联调验收顺序

1. 成员一、二、四的 Bean 全部可注入后，接口从 `APPROVAL_INTEGRATION_UNAVAILABLE` 自动切换到可执行。
2. 验证 `APPROVED`、`CONFIRM_PENDING`、`COMPLETED` 三种状态均可取消；其他状态返回 `APPROVAL_CANCEL_NOT_ALLOWED`。
3. 验证资源释放、单据作废、`approval_record(CANCEL)`、`APPROVAL_CANCELLED` 消息和申请状态更新全部成功。
4. 分别让资源释放、单据作废和消息写入失败，确认申请状态、审核记录和其余成员表均回滚。
5. 用相同 `requestId` 重复提交，确认不重复释放资源、不重复作废单据、不重复写审核记录或消息。
