package com.example.backend.service.port;

/**
 * 成员四调用成员三“完成欠费确认”的状态流转边界。
 *
 * <p>成员四保存自身的确认记录后，不能直接 UPDATE application。成员三实现本接口，
 * 校验申请仍为 CONFIRM_PENDING、版本未变化且当前操作人具备学校权限，然后通过成员二
 * 的状态写入 Service 把申请改为 COMPLETED，并写入审核/状态历史。</p>
 *
 * <p>由成员三 {@code ArrearsConfirmationCompletionPortAdapter} 适配到正式
 * {@code ApprovalCompletionService.completeAfterConfirmation}，并强制加入成员四
 * 发起的确认事务；成员四不得自行更新申请状态。</p>
 */
public interface ArrearsConfirmationCompletionPort {

    /**
     * 在当前 Spring 事务内完成欠费申请。
     *
     * @param applicationId 统一申请主表 ID
     * @param expectedVersion 前端从待确认详情读取的申请版本
     * @param requestId 本次确认的幂等请求号
     * @param operatorUserId 当前学校管理员 ID
     */
    void completeAfterConfirmation(
            Long applicationId,
            Integer expectedVersion,
            String requestId,
            Long operatorUserId);
}
