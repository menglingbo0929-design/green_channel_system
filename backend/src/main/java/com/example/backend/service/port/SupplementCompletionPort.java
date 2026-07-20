package com.example.backend.service.port;

import com.example.backend.model.vo.supplement.SupplementCompletionResultVO;

/**
 * 成员三审核模块需要实现的补录自动审核边界。
 *
 * <p>实现方写校级 APPROVE 记录，并把 DRAFT 流转为 CONFIRM_PENDING/CONFIRMATION
 * 或 COMPLETED/SYSTEM。该接口不允许成员四自行更新 application.status。</p>
 *
 * <p>TODO(成员三)：实现补录校级自动审核记录、乐观锁和 requestId 幂等，调用成员二
 * 状态写入 Service 后返回新状态/层级/版本，并加入成员四发起的外层事务。</p>
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
