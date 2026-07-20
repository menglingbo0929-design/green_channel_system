package com.example.backend.approval.port;

import com.example.backend.approval.domain.ApplicationStatus;
import com.example.backend.approval.domain.ApprovalLevel;

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
