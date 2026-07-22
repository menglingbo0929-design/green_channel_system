package com.example.backend.approval.api;

import com.example.backend.approval.domain.ApplicationStatus;
import com.example.backend.approval.domain.ApplicationType;
import com.example.backend.approval.domain.ApprovalAction;
import com.example.backend.approval.domain.ApprovalLevel;
import com.example.backend.approval.persistence.type.BatchType;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public record ApprovalListItem(
        Long applicationId,
        String applicationNo,
        ApplicationType applicationType,
        String applicationTypeName,
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
        String statusName,
        ApprovalLevel currentLevel,
        ApprovalAction latestDecision,
        LocalDateTime submitTime,
        Integer version
) {
}
