package com.example.backend.model.dto;

import com.example.backend.model.domain.ApplicationStatus;
import com.example.backend.model.domain.ApplicationType;
import com.example.backend.model.domain.ApprovalLevel;
import com.example.backend.model.domain.BatchType;
import java.time.LocalDateTime;
import java.math.BigDecimal;

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
        BigDecimal declaredAmount,
        ApplicationStatus status,
        ApprovalLevel currentLevel,
        Integer reviewRound,
        LocalDateTime submitTime,
        Integer version
) {
}
