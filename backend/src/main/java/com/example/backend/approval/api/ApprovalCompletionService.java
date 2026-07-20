package com.example.backend.approval.api;

public interface ApprovalCompletionService {

    ApplicationStatusResult completeAfterConfirmation(
            Long applicationId,
            Integer expectedVersion,
            String requestId,
            Long operatorId
    );
}
