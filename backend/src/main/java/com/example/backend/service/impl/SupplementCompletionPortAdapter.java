package com.example.backend.service.impl;

import com.example.backend.model.dto.ApplicationStatusResult;
import com.example.backend.service.ApprovalTransitionService;
import com.example.backend.model.vo.supplement.SupplementCompletionResultVO;
import com.example.backend.service.port.SupplementCompletionPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bridges member four's supplement transaction to the approved member-three
 * automatic-review transition. It deliberately exposes only the status data
 * owned by the approval module.
 */
@Component
public class SupplementCompletionPortAdapter implements SupplementCompletionPort {

    private final ApprovalTransitionService approvalTransitionService;

    public SupplementCompletionPortAdapter(
            ApprovalTransitionService approvalTransitionService
    ) {
        this.approvalTransitionService = approvalTransitionService;
    }

    /**
     * The supplement creation transaction is owned by member four and must
     * include both the application data and this automatic approval record.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public SupplementCompletionResultVO completeSupplementReview(
            Long applicationId,
            boolean containsArrears,
            Integer expectedVersion,
            String requestId,
            Long operatorUserId
    ) {
        ApplicationStatusResult result = approvalTransitionService.completeSupplementReview(
                applicationId,
                containsArrears,
                expectedVersion,
                requestId,
                operatorUserId
        );
        SupplementCompletionResultVO response = new SupplementCompletionResultVO();
        response.setStatus(result.status().name());
        response.setCurrentLevel(result.currentLevel().name());
        response.setVersion(result.version());
        return response;
    }
}
