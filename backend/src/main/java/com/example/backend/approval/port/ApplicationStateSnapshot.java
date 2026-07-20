package com.example.backend.approval.port;

import com.example.backend.approval.domain.ApplicationStatus;
import com.example.backend.approval.domain.ApplicationType;
import com.example.backend.approval.domain.ApprovalLevel;
import com.example.backend.approval.persistence.type.BatchType;

public record ApplicationStateSnapshot(
        Long applicationId,
        Long studentId,
        BatchType batchType,
        Long batchId,
        ApplicationType applicationType,
        ApplicationStatus status,
        ApprovalLevel currentLevel,
        Integer reviewRound,
        Integer version
) {
}
