package com.example.backend.approval.api;

import com.example.backend.application.domain.ApplicationStatus;
import com.example.backend.approval.domain.ApprovalAction;
import com.example.backend.approval.persistence.type.ApprovalRecordLevel;

import java.time.LocalDateTime;

public record ApprovalRecordSnapshot(
        Long id,
        Long applicationId,
        Integer reviewRound,
        ApprovalRecordLevel approvalLevel,
        Long approverId,
        String approverName,
        ApprovalAction action,
        String comment,
        ApplicationStatus oldStatus,
        ApplicationStatus newStatus,
        String modifiedFields,
        String requestId,
        LocalDateTime createTime
) {
}
