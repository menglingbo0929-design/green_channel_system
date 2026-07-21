package com.example.backend.approval.integration;

import com.example.backend.approval.api.ApprovalCompletionService;
import com.example.backend.service.port.ArrearsConfirmationCompletionPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bridges the confirmation module's local port to the member-three approval
 * state machine. The confirmation module owns its confirmation row; this
 * adapter only completes the application state and writes the approval record.
 */
@Component
public class ArrearsConfirmationCompletionPortAdapter
        implements ArrearsConfirmationCompletionPort {

    private final ApprovalCompletionService approvalCompletionService;

    public ArrearsConfirmationCompletionPortAdapter(
            ApprovalCompletionService approvalCompletionService
    ) {
        this.approvalCompletionService = approvalCompletionService;
    }

    /**
     * Must join the member-four confirmation transaction, so a confirmation
     * record can never be committed without its corresponding state transition.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void completeAfterConfirmation(
            Long applicationId,
            Integer expectedVersion,
            String requestId,
            Long operatorUserId
    ) {
        approvalCompletionService.completeAfterConfirmation(
                applicationId,
                expectedVersion,
                requestId,
                operatorUserId
        );
    }
}
