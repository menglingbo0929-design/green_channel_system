package com.example.backend.service.port;

import com.example.backend.model.vo.supplement.SupplementCompletionResultVO;

/**
 * 成员三审核模块需要实现的补录自动审核边界。
 *
 * <p>实现方写校级 APPROVE 记录，并把 DRAFT 流转为 CONFIRM_PENDING/CONFIRMATION
 * 或 COMPLETED/SYSTEM。该接口不允许成员四自行更新 application.status。</p>
 *
 * <p>由成员三 {@code SupplementCompletionPortAdapter} 调用正式
 * {@code ApprovalTransitionService}，保留乐观锁和 requestId 幂等，并强制加入成员四
 * 发起的外层事务。</p>
 */
public interface SupplementCompletionPort {

    SupplementCompletionResultVO completeSupplementReview(
            Long applicationId,
            boolean containsArrears,
            Integer expectedVersion,
            String requestId,
            Long operatorUserId
    );
}
