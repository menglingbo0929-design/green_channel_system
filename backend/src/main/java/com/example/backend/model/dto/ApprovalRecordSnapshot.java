package com.example.backend.model.dto;

import com.example.backend.model.domain.ApplicationStatus;
import com.example.backend.model.domain.ApprovalAction;
import com.example.backend.model.domain.ApprovalRecordLevel;

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
