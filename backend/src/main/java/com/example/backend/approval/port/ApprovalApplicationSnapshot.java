package com.example.backend.approval.port;

import com.example.backend.approval.domain.ApplicationStatus;
import com.example.backend.approval.domain.ApplicationType;
import com.example.backend.approval.domain.ApprovalLevel;
import com.example.backend.approval.persistence.type.BatchType;
import java.time.LocalDateTime;

/** Read-only application projection supplied by the application module. */
public record ApprovalApplicationSnapshot(
        Long applicationId,
        String applicationNo,
        ApplicationType applicationType,
        BatchType batchType,
        Long batchId,
        Long studentId,
        String studentNo,
        String studentName,
        Long collegeId,
        String collegeName,
        String gradeName,
        ApplicationStatus status,
        ApprovalLevel currentLevel,
        Integer reviewRound,
        LocalDateTime submitTime,
        Integer version
) {
}
