package com.example.backend.service;

import com.example.backend.model.dto.ApplicationStatusResult;

public interface ApprovalCompletionService {

    ApplicationStatusResult completeAfterConfirmation(
            Long applicationId,
            Integer expectedVersion,
            String requestId,
            Long operatorId
    );
}
